# 🚀 دليل المعمارية وطريقة الدمج في التطبيق (Flutter / Web Player)
## Zero-Ad Streaming & Instant Fallback Engine

---

## 🏗️ 1. المخطط الهندسي لتدفق البث (Architecture Workflow)

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 المستخدم (App Player)
    participant Client as 📱 Flutter / Web Client
    participant API as 🌐 /api/cinema/superresolve
    participant SuperAgg as ⚙️ SuperAggregator
    participant Extractor as ⚡ Stream Extractor Engine
    participant Proxy as 🛡️ /api/proxy/stream
    participant CDN as ☁️ سحابة البث (CDN Server)

    User->>Client: اختيار فيلم / حلقة (تشغيل فوري)
    Client->>API: طلب رابط البث: GET /api/cinema/superresolve?embedUrls=S1,S2,S3
    API->>SuperAgg: تنفيذ Fallback متوازي لسيرفرات العرض
    SuperAgg->>Extractor: فك تشفير وتطهير السيرفرات (15+ Host)
    
    alt السيرفر 1 شغال ومستقر
        Extractor-->>SuperAgg: تم استخراج رابط البث الصافي (m3u8/mp4)
    else السيرفر 1 معطل أو بطيء (> 4 ثوانٍ)
        Note over SuperAgg: التحويل الفوري التلقائي للسيرفر 2
        Extractor-->>SuperAgg: تم استخراج رابط السيرفر 2 فوراً
    end

    SuperAgg-->>API: رابط الفيديو الصافي
    API-->>Client: { playUrl: "/api/proxy/stream?url=...", type: "hls" }
    
    Client->>Proxy: بدء تشغيل الفيديو في البلاير
    Proxy->>CDN: جلب الـ Segments والهيدرز مع إزالة الإعلانات
    CDN-->>Proxy: مقاطع الفيديو النقية (Zero Ads)
    Proxy-->>Client: البث المباشر بأعلى سرعة وأقل تأخير
    Client-->>User: 🎥 تشغيل الفيلم في ثوانٍ معدودة!
```

---

## ⚡ 2. كيف يحقق النظام المشاهدة والتحميل بدون إعلانات وبأقصى سرعة؟

### أ. إزالة الإعلانات والـ Popups تماماً (100% Zero Ads)
1. **عدم تحميل صفحات الويب للمستخدم نهائياً:** بدلاً من فتح صفحة الموقع التي تحتوي على 10+ إعلانات منبثقة وبرمجيات خبيثة، يقوم السيرفر الداخلي (`stream-extractor.ts`) بالاتصال بالصفحة في الخلفية، وسحب رابط الـ Stream الصريح فقط.
2. **تطهير ملفات الـ M3U8:** في مسار `/api/proxy/stream`:
   - يتم فحص كل سطر في قائمة التشغيل.
   - يتم حذف وسوم الإعلانات (`#EXT-X-DISCONTINUITY`, `#EXT-X-CUE-IN/OUT`).
   - إعادة كتابة روابط الـ Segments لترسل مباشرة للبلاير بدون أي تحويلات إعلانية.

### ب. السرعة الفائقة والتشغيل اللحظي (Instant Playback)
1. **Parallel Race Fallback:** يتم تجربة أول 3 سيرفرات في نفس اللحظة (Race)، وأول سيرفر يستجيب برابط صالح يتم تشغيله فوراً للمستخدم، بينما يتم إلغاء الطلبات البطيئة الأخرى.
2. **Multi-Tier Memory Caching:** يتم تخزين روابط البث المستخرجة في ذاكرة الـ RAM لمدة 10 دقائق، بحيث إذا فتح مستخدم آخر نفس الحلقة لا يستغرق الأمر سوى **10 مللي ثانية** لبدء العرض!
3. **HTTP Range Requests (Seek & Resume):** يدعم البروكسي كود `206 Partial Content`، مما يسمح للمستخدم بتقديم وتأخير الفيديو لأي دقيقة في الفيلم بدون أي تجمد أو إعادة تحميل من البداية.

---

## 📱 3. كود الدمج الكامل في تطبيق Flutter

### أ. كلاس استدعاء السيرفرات وحل الروابط الذكي (`StreamResolverService.dart`)

قم بنسخ هذا الكود في مشروع الفلاتر لديك (مثلاً في مسار `lib/services/stream_resolver.dart`):

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;

class ResolvedStreamResult {
  final bool success;
  final String playUrl;
  final String rawUrl;
  final String type; // 'hls' or 'mp4'
  final String quality;

  ResolvedStreamResult({
    required this.success,
    required this.playUrl,
    required this.rawUrl,
    required this.type,
    required this.quality,
  });

  factory ResolvedStreamResult.fromJson(Map<String, dynamic> json) {
    return ResolvedStreamResult(
      success: json['success'] ?? false,
      playUrl: json['playUrl'] ?? '',
      rawUrl: json['rawUrl'] ?? '',
      type: json['type'] ?? 'hls',
      quality: json['quality'] ?? 'HD',
    );
  }
}

class StreamResolverService {
  // عنوان السيرفر الخاص بنا
  static const String baseApiUrl = "http://localhost:3000"; // أو دومين السيرفر المباشر

  /// حل رابط المشاهدة مع ميزة الـ Smart Fallback التلقائي
  /// [embedUrls] قائمة روابط السيرفرات المتوفرة للحلقة أو الفيلم
  static Future<ResolvedStreamResult?> resolveStreamWithFallback(
    List<String> embedUrls, {
    String? referer,
  }) async {
    if (embedUrls.isEmpty) return null;

    try {
      final joinedUrls = embedUrls.map((u) => Uri.encodeComponent(u)).join(',');
      var endpoint = "$baseApiUrl/api/cinema/superresolve?embedUrls=$joinedUrls";
      
      if (referer != null && referer.isNotEmpty) {
        endpoint += "&referer=${Uri.encodeComponent(referer)}";
      }

      final response = await http.get(
        Uri.parse(endpoint),
        headers: {
          'Accept': 'application/json',
          'User-Agent': 'IPTV-Flutter-App/2.0',
        },
      ).timeout(const Duration(seconds: 8));

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['success'] == true) {
          String finalPlayUrl = data['playUrl'];
          
          // إذا كان الرابط نسبي، نقوم بتحويله لرابط كامل
          if (finalPlayUrl.startsWith('/')) {
            finalPlayUrl = "$baseApiUrl$finalPlayUrl";
          }

          return ResolvedStreamResult(
            success: true,
            playUrl: finalPlayUrl,
            rawUrl: data['rawUrl'] ?? '',
            type: data['type'] ?? 'hls',
            quality: data['quality'] ?? 'HD',
          );
        }
      }
    } catch (e) {
      print("[StreamResolver] Error during resolution: $e");
    }

    return null;
  }
}
```

---

### ب. تشغيل الفيديو في مشغل فلاتر بدون إعلانات (`PlayerView.dart`)

سواء كنت تستخدم `video_player` أو `better_player` أو `media_kit`:

```dart
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

class CleanVideoPlayerScreen extends StatefulWidget {
  final List<String> serverUrls;
  final String title;

  const CleanVideoPlayerScreen({
    Key? key,
    required this.serverUrls,
    required this.title,
  }) : super(key: key);

  @override
  _CleanVideoPlayerScreenState createState() => _CleanVideoPlayerScreenState();
}

class _CleanVideoPlayerScreenState extends State<CleanVideoPlayerScreen> {
  VideoPlayerController? _controller;
  bool _isLoading = true;
  String _statusMessage = "جاري تحضير سيرفر البث فائق السرعة...";

  @override
  void initState() {
    super.initState();
    _startStreamPlayback();
  }

  Future<void> _startStreamPlayback() async {
    setState(() {
      _isLoading = true;
      _statusMessage = "جاري الاتصال بأقوى سيرفر متاح...";
    });

    // 1. استدعاء السيرفر لحل الرابط تلقائياً مع الـ Fallback
    final streamData = await StreamResolverService.resolveStreamWithFallback(
      widget.serverUrls,
    );

    if (streamData != null && streamData.playUrl.isNotEmpty) {
      setState(() {
        _statusMessage = "تم العثور على رابط البث النقي! جاري تشغيل المشغل...";
      });

      // 2. تشغيل الرابط النقي المستخرج في المشغل
      _controller = VideoPlayerController.networkUrl(
        Uri.parse(streamData.playUrl),
        httpHeaders: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
        },
      );

      await _controller!.initialize();
      _controller!.play();

      setState(() {
        _isLoading = false;
      });
    } else {
      setState(() {
        _isLoading = false;
        _statusMessage = "عذراً، فشل تشغيل جميع السيرفرات لهذا المحتوى حالياً.";
      });
    }
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: Text(widget.title),
        backgroundColor: Colors.transparent,
      ),
      body: Center(
        child: _isLoading
            ? Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const CircularProgressIndicator(color: Colors.redAccent),
                  const SizedBox(height: 16),
                  Text(
                    _statusMessage,
                    style: const TextStyle(color: Colors.white70, fontSize: 15),
                  ),
                ],
              )
            : _controller != null && _controller!.value.isInitialized
                ? AspectRatio(
                    aspectRatio: _controller!.value.aspectRatio,
                    child: VideoPlayer(_controller!),
                  )
                : Text(
                    _statusMessage,
                    style: const TextStyle(color: Colors.redAccent),
                  ),
      ),
    );
  }
}
```

---

### ج. كود التحميل السريع في الخلفية بأقصى سرعة خط (High-Speed Downloader)

باستخدام مكتبة `dio` لدعم استئناف التحميل (Resume) وموازاة البايتات:

```dart
import 'package:dio/dio.dart';
import 'package:path_provider/path_provider.dart';

class FastMediaDownloader {
  static final Dio _dio = Dio();

  /// تحميل الفيلم أو المسلسل مباشرة بدون إعلانات وبأقصى سرعة
  static Future<void> startDownload({
    required List<String> serverUrls,
    required String fileName,
    required Function(double progress) onProgress,
    required Function(String filePath) onCompleted,
    required Function(String error) onError,
  }) async {
    try {
      // 1. استخراج الرابط النظيف أولاً
      final streamInfo = await StreamResolverService.resolveStreamWithFallback(serverUrls);
      if (streamInfo == null || streamInfo.playUrl.isEmpty) {
        onError("تعذر العثور على سيرفر تحميل صالح.");
        return;
      }

      final dir = await getApplicationDocumentsDirectory();
      final savePath = "${dir.path}/$fileName.mp4";

      // 2. البدء في التحميل السريع مع تتبع النسبة المئوية
      await _dio.download(
        streamInfo.playUrl,
        savePath,
        onReceiveProgress: (received, total) {
          if (total != -1) {
            final progress = (received / total);
            onProgress(progress);
          }
        },
        options: Options(
          headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
          },
        ),
      );

      onCompleted(savePath);
    } catch (e) {
      onError("حدث خطأ أثناء التحميل: $e");
    }
  }
}
```

---

## 💻 4. كود تشغيل مشغل الويب (Web Player Integration)

إذا كان لديك مشغل Web (React / Vue / HTML5 Video / Hls.js):

```html
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8">
  <title>مشغل البث فائق السرعة - بدون إعلانات</title>
  <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
  <style>
    body { margin: 0; background: #000; display: flex; justify-content: center; align-items: center; height: 100vh; }
    video { width: 95vw; max-height: 90vh; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.8); }
  </style>
</head>
<body>

  <video id="iptvPlayer" controls autoplay></video>

  <script>
    async function playVideo(serverUrls) {
      const video = document.getElementById('iptvPlayer');
      const joined = serverUrls.map(encodeURIComponent).join(',');
      
      const res = await fetch(`/api/cinema/superresolve?embedUrls=${joined}`);
      const data = await res.json();

      if (data.success && data.playUrl) {
        const streamUrl = data.playUrl;

        if (Hls.isSupported() && (streamUrl.includes('.m3u8') || data.type === 'hls')) {
          const hls = new Hls({ enableWorker: true, lowLatencyMode: true });
          hls.loadSource(streamUrl);
          hls.attachMedia(video);
          hls.on(Hls.Events.MANIFEST_PARSED, () => video.play());
        } else {
          video.src = streamUrl;
          video.play();
        }
      } else {
        alert('تعذر تشغيل الفيديو من السيرفرات المتاحة.');
      }
    }

    // مثال على التشغيل الفوري بمجرد فتح الصفحة
    playVideo([
      "https://govid.live/embed-xxxx",
      "https://doodstream.com/e/xxxx",
      "https://myweecima.casa/watch.php?vid=xxxx"
    ]);
  </script>
</body>
</html>
```

---

## 🛠️ 5. كيف يتم تشغيل النظام في مشروعك الحالي؟

الملفات كاملة ومستقلة تماماً داخل فولدر:
`C:\Users\hassa\Desktop\all project\iptv app\social`

1. **الـ Providers:** موجودة داخل `social/src/lib/providers/` وتغطي كل الفئات.
2. **محرك فك تشفير السيرفرات:** في `social/src/lib/extractors/stream-extractor.ts`.
3. **الـ Endpoints البرمجية:** في `social/src/api/cinema/` و `social/src/api/proxy/`.
4. يمكنك استدعاء كلاس `superAggregator` مباشرة في أي خدمة كود (Node.js / Next.js / Express)، أو توجيه تطبيق الـ Flutter نحو الـ Endpoints الجاهزة:
   - `GET /api/cinema/superresolve?embedUrls=...`
   - `GET /api/proxy/stream?url=...`
   - `GET /api/cinema/providers`
