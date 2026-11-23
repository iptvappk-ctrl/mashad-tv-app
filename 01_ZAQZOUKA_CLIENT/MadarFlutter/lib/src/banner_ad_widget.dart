import 'dart:async';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'firebase_quota_guard.dart';

/// 📌 شريط إعلاني ثابت غير مزعج (Non-intrusive Banner Strip)
/// يظهر في أسفل الشاشة فوق شريط التنقل السفلي مباشرة، ولا يظهر داخل المشغل ملء الشاشة.
class AppBannerAdStrip extends StatefulWidget {
  const AppBannerAdStrip({super.key});

  @override
  State<AppBannerAdStrip> createState() => _AppBannerAdStripState();
}

class _AppBannerAdStripState extends State<AppBannerAdStrip> {
  static const _native = MethodChannel('madar/native');
  bool _dismissed = false;
  StreamSubscription<SystemStatus>? _sub;
  Timer? _rotationTimer;
  int _activeAdIndex = 0;

  // 🎯 باقة إعلانات متنوعة وتجارية تتبدل دورياً (ألعاب، تسوق، تداول، ترفيه)
  static const List<Map<String, String>> _diverseAds = [
    {
      'title': 'PUBG Mobile 3.5 🎮',
      'subtitle': 'تحدي الشتاء والجليد الأسطوري - حمّل والعب مجاناً!',
      'tag': 'ألعاب • مجاناً',
      'cta': 'تثبيت ⬇',
      'url': 'https://uplcm.com/4/12335194',
      'tagColor': '0xFF10B981',
      'ctaColor': '0xFF10B981',
    },
    {
      'title': 'نون للتسوق الإلكتروني 🛍️',
      'subtitle': 'خصومات كبرى تصل 70% + توصيل فوري بكود VIP10',
      'tag': 'عروض حصرية',
      'cta': 'تسوق ↗',
      'url': 'https://uplcm.com/4/12335228',
      'tagColor': '0xFFF59E0B',
      'ctaColor': '0xFFF59E0B',
    },
    {
      'title': 'منصة إكسنس للتداول 📈',
      'subtitle': 'تداول الذهب والأسهم بحساب تجريبي مجاني 10,000\$',
      'tag': 'تداول واستثمار',
      'cta': 'ابدأ الآن ↗',
      'url': 'https://uplcm.com/4/12335268',
      'tagColor': '0xFF00E5FF',
      'ctaColor': '0xFF00E5FF',
    },
    {
      'title': 'تطبيق شاهد VIP 🎬',
      'subtitle': 'أقوى العروض الحصرية والأفلام العالمية بجودة 4K',
      'tag': 'سينما وترفيه',
      'cta': 'اشترك ↗',
      'url': 'https://uplcm.com/4/12335274',
      'tagColor': '0xFFEC4899',
      'ctaColor': '0xFFEC4899',
    },
    {
      'title': 'AliExpress Super Deals 🔥',
      'subtitle': 'شحن مجاني وأسعار تبدأ من 1\$ لأول طلبية',
      'tag': 'تخفيضات 90%',
      'cta': 'اطلب ↗',
      'url': 'https://uplcm.com/4/12335194',
      'tagColor': '0xFFEF4444',
      'ctaColor': '0xFFEF4444',
    },
  ];

  @override
  void initState() {
    super.initState();
    _sub = ZaqzoukaQuotaGuard.instance.statusStream.listen((_) {
      if (mounted) setState(() {});
    });
    // تبديل نوع الإعلان كل 7 ثوانٍ لجذب المشاهدين
    _rotationTimer = Timer.periodic(const Duration(seconds: 7), (_) {
      if (mounted) {
        setState(() {
          _activeAdIndex = (_activeAdIndex + 1) % _diverseAds.length;
        });
      }
    });
  }

  @override
  void dispose() {
    _rotationTimer?.cancel();
    _sub?.cancel();
    super.dispose();
  }

  void _openAdUrl(String url) {
    if (url.trim().isEmpty) return;
    try {
      _native.invokeMethod('system.openUrl', {'url': url.trim()});
    } catch (_) {}
  }

  String? _extractImgUrl(String html) {
    final match = RegExp(r'''<img[^>]+src=["\']([^"\']+)["\']''', caseSensitive: false).firstMatch(html);
    if (match != null) return match.group(1);
    final directImgMatch = RegExp(r'''https?://[^\s<>"\']+\.(?:png|jpg|jpeg|webp|gif)''', caseSensitive: false).firstMatch(html);
    return directImgMatch?.group(0);
  }

  String _resolveBannerUrl(String directUrl, String html) {
    if (directUrl.trim().isNotEmpty) return directUrl.trim();
    final hrefMatch = RegExp(r'''href=["\']([^"\']+)["\']''', caseSensitive: false).firstMatch(html);
    if (hrefMatch != null) return hrefMatch.group(1) ?? '';
    final urlMatch = RegExp(r'''https?://[^\s<>"\']+''').firstMatch(html);
    return urlMatch?.group(0) ?? '';
  }

  String _extractBannerText(String html) {
    final clean = html.replaceAll(RegExp(r'<[^>]*>'), '').trim();
    return clean.isNotEmpty ? clean : 'برعاية شركائنا · اضغط للمشاهدة';
  }

  @override
  Widget build(BuildContext context) {
    if (_dismissed) return const SizedBox.shrink();

    final status = ZaqzoukaQuotaGuard.instance.currentStatus;
    final currentAd = _diverseAds[_activeAdIndex];
    final tagColor = Color(int.parse(currentAd['tagColor']!));
    final ctaColor = Color(int.parse(currentAd['ctaColor']!));

    // 1. Rotating Sponsored Creatives & Custom Banners
    if (status.isBannerAdEnabled) {
      return Container(
        margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
        decoration: BoxDecoration(
          color: const Color(0xFF141923),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: const Color(0xFF2E384D), width: 1.2),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.35),
              blurRadius: 8,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Row(
          children: [
            // زر الإغلاق
            InkWell(
              borderRadius: BorderRadius.circular(14),
              onTap: () => setState(() => _dismissed = true),
              child: Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white.withValues(alpha: 0.08),
                ),
                child: const Icon(Icons.close_rounded, size: 14, color: Colors.white54),
              ),
            ),
            const SizedBox(width: 6),
            // مساحة البنر الإعلاني (ارتفاع 62dp كبرناه سيكا وتنوع إعلاني كامل)
            Expanded(
              child: SizedBox(
                height: 62,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      // خلفية الإعلان التجاري المتنوع القابل للنقر
                      InkWell(
                        onTap: () => _openAdUrl(currentAd['url']!),
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 400),
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            gradient: const LinearGradient(
                              colors: [Color(0xFF1E2638), Color(0xFF0F1420)],
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                            ),
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: tagColor.withValues(alpha: 0.3)),
                          ),
                          child: Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
                                decoration: BoxDecoration(
                                  color: tagColor.withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(6),
                                  border: Border.all(color: tagColor, width: 0.8),
                                ),
                                child: Text(
                                  currentAd['tag']!,
                                  style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: tagColor),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Text(
                                      currentAd['title']!,
                                      style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.white),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    const SizedBox(height: 2),
                                    Text(
                                      currentAd['subtitle']!,
                                      style: const TextStyle(fontSize: 10, color: Colors.white70),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 6),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                                decoration: BoxDecoration(
                                  color: ctaColor,
                                  borderRadius: BorderRadius.circular(8),
                                  boxShadow: [
                                    BoxShadow(color: ctaColor.withValues(alpha: 0.4), blurRadius: 6),
                                  ],
                                ),
                                child: Text(
                                  currentAd['cta']!,
                                  style: const TextStyle(fontSize: 11, color: Colors.white, fontWeight: FontWeight.bold),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      );
    }

    if (!status.isBannerAdEnabled) return const SizedBox.shrink();

    final bannerHtml = status.bannerHtml;
    final bannerUrl = _resolveBannerUrl(status.bannerUrl, bannerHtml);
    final bannerImg = _extractImgUrl(bannerHtml);
    final bannerText = _extractBannerText(bannerHtml);

    return AnimatedSize(
      duration: const Duration(milliseconds: 250),
      curve: Curves.easeInOut,
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
        decoration: BoxDecoration(
          color: const Color(0xFF141923),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: const Color(0xFF262D3D), width: 1),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.3),
              blurRadius: 6,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: [
            // زر الإغلاق
            InkWell(
              borderRadius: BorderRadius.circular(14),
              onTap: () => setState(() => _dismissed = true),
              child: Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white.withValues(alpha: 0.08),
                ),
                child: const Icon(Icons.close_rounded, size: 14, color: Colors.white54),
              ),
            ),
            const SizedBox(width: 8),

            // محتوى البنر
            Expanded(
              child: InkWell(
                borderRadius: BorderRadius.circular(8),
                onTap: bannerUrl.isNotEmpty ? () => _openAdUrl(bannerUrl) : null,
                child: Row(
                  children: [
                    if (bannerImg != null && bannerImg.startsWith('http'))
                      ClipRRect(
                        borderRadius: BorderRadius.circular(6),
                        child: CachedNetworkImage(
                          imageUrl: bannerImg,
                          height: 42,
                          width: 42,
                          fit: BoxFit.cover,
                          errorWidget: (_, __, ___) => const Icon(Icons.campaign_rounded, color: Color(0xFF4A90D9), size: 24),
                        ),
                      )
                    else
                      Container(
                        padding: const EdgeInsets.all(6),
                        decoration: BoxDecoration(
                          color: const Color(0xFF4A90D9).withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: const Icon(Icons.campaign_rounded, color: Color(0xFF4A90D9), size: 18),
                      ),
                    const SizedBox(width: 8),

                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFD4AF37).withValues(alpha: 0.2),
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: const Text(
                                  'إعلان مروّج',
                                  style: TextStyle(fontSize: 9, fontWeight: FontWeight.bold, color: Color(0xFFD4AF37)),
                                ),
                              ),
                              const SizedBox(width: 6),
                              Expanded(
                                child: Text(
                                  bannerText,
                                  style: const TextStyle(fontSize: 11, color: Colors.white70),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                          if (bannerUrl.isNotEmpty)
                            Text(
                              bannerUrl,
                              style: const TextStyle(fontSize: 9.5, color: Color(0xFF4A90D9)),
                              overflow: TextOverflow.ellipsis,
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),

            if (bannerUrl.isNotEmpty) ...[
              const SizedBox(width: 6),
              InkWell(
                borderRadius: BorderRadius.circular(8),
                onTap: () => _openAdUrl(bannerUrl),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: const Color(0xFF4A90D9),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Text(
                    'زيارة ↗',
                    style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
