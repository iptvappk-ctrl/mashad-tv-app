// 🛡️ ZAQZOUKA FIREBASE QUOTA GUARD
// ===================================
// الغرض: حماية كوتا Firebase المجانية من الاستنزاف
// بدل ما كل مستخدم يفتح connection مباشر على Firebase
// → كل المستخدمين يقرأوا من Vercel CDN cache (60 ثانية)
// → لو CDN بطّل، نقرأ من SharedPreferences (local cache)
// → لو Local cache انتهت، نقرأ من Firebase مع exponential backoff
//
// النتيجة: مليون مستخدم = connection واحد على Firebase كل 60 ثانية فقط!

import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

// ════════════════════════════════════════
// 📌 CONFIGURATION
// ════════════════════════════════════════

class _Cfg {
  // Live Endpoints (Vercel Edge CDN cached, instant status response)
  static const vercelStatusUrls = [
    'https://web-nu-teal-29.vercel.app/api/status',
    'https://zaqzouka-vercel-hub.vercel.app/api/status',
    'https://zaqzouka-dashboard-api.vercel.app/api/status',
  ];

  // Static fallback JSON on Vercel CDN edge (ultimate fallback)
  static const staticFallbackUrls = [
    'https://web-nu-teal-29.vercel.app/status/system.json',
    'https://zaqzouka-vercel-hub.vercel.app/status/system.json',
  ];

  // Firebase RTDB direct (last resort only)
  static const firebaseUrl =
      'https://madartv-default-rtdb.firebaseio.com/system_control.json';

  // Cache TTL: 15s for reactive freeze/wipe response without overloading servers
  static const localCacheTtl = Duration(seconds: 15);
  static const firebaseMinInterval = Duration(minutes: 5); // max 1 direct hit per 5 min
  static const networkTimeout = Duration(seconds: 5);

  // SharedPreferences keys
  static const spKeyStatus = 'zaq_system_status';
  static const spKeyTimestamp = 'zaq_status_ts';
}

// ════════════════════════════════════════
// 📦 System Status Model
// ════════════════════════════════════════

class SystemStatus {
  final String status;       // "active" | "frozen" | "wiped"
  final String message;
  final int updatedAt;
  final int wipeTimestamp;
  final String source;

  const SystemStatus({
    required this.status,
    required this.message,
    required this.updatedAt,
    required this.wipeTimestamp,
    required this.source,
  });

  factory SystemStatus.fromJson(Map<String, dynamic> j, {String source = 'unknown'}) {
    return SystemStatus(
      status: (j['status'] as String? ?? 'active').toLowerCase(),
      message: j['message'] as String? ?? 'المنظومة نشطة',
      updatedAt: (j['updatedAt'] as num?)?.toInt() ?? 0,
      wipeTimestamp: (j['wipeTimestamp'] as num?)?.toInt() ?? 0,
      source: j['_source'] as String? ?? source,
    );
  }

  factory SystemStatus.active() => const SystemStatus(
        status: 'active',
        message: 'المنظومة نشطة وتعمل بشكل طبيعي',
        updatedAt: 0,
        wipeTimestamp: 0,
        source: 'hardcoded_default',
      );

  bool get isActive => status == 'active';
  bool get isFrozen => status == 'frozen';
  bool get isWiped => status == 'wiped';

  @override
  String toString() => 'SystemStatus($status, source=$source)';
}

// ════════════════════════════════════════
// 🛡️ QUOTA GUARD - Main Class
// ════════════════════════════════════════

class ZaqzoukaQuotaGuard {
  static ZaqzoukaQuotaGuard? _instance;
  factory ZaqzoukaQuotaGuard() => _instance ??= ZaqzoukaQuotaGuard._internal();
  ZaqzoukaQuotaGuard._internal();

  // In-memory cache (fastest layer)
  SystemStatus? _memCache;
  DateTime? _memCacheTime;

  // Last Firebase direct access time
  DateTime? _lastFirebaseHit;

  // Status stream for reactive UI
  final _statusController = StreamController<SystemStatus>.broadcast();
  Stream<SystemStatus> get statusStream => _statusController.stream;

  // Polling timer
  Timer? _pollingTimer;

  // ─────────────────────────────────────
  // Public API
  // ─────────────────────────────────────

  /// Initialize the guard and start polling
  Future<void> initialize() async {
    debugPrint('[QuotaGuard] Initializing...');
    final status = await getStatus();
    _statusController.add(status);
    _startPolling();
  }

  /// Get system status (uses tiered cache)
  Future<SystemStatus> getStatus() async {
    // 🏎️ LAYER 1: In-memory (< 60s old)
    if (_memCache != null && _memCacheTime != null) {
      final age = DateTime.now().difference(_memCacheTime!);
      if (age < _Cfg.localCacheTtl) {
        debugPrint('[QuotaGuard] L1 mem-cache hit (${age.inSeconds}s old)');
        return _memCache!;
      }
    }

    // 🌐 LAYER 2: Vercel CDN (handles millions of users)
    final vercelResult = await _fetchVercel();
    if (vercelResult != null) {
      _setMemCache(vercelResult);
      await _saveToLocal(vercelResult);
      return vercelResult;
    }

    // 💾 LAYER 3: Local SharedPreferences cache
    final localResult = await _loadFromLocal();
    if (localResult != null) {
      debugPrint('[QuotaGuard] L3 local cache hit');
      _setMemCache(localResult);
      return localResult;
    }

    // 🔥 LAYER 4: Firebase RTDB direct (last resort, rate-limited)
    final canHitFirebase = _lastFirebaseHit == null ||
        DateTime.now().difference(_lastFirebaseHit!) > _Cfg.firebaseMinInterval;

    if (canHitFirebase) {
      final firebaseResult = await _fetchFirebase();
      if (firebaseResult != null) {
        _lastFirebaseHit = DateTime.now();
        _setMemCache(firebaseResult);
        await _saveToLocal(firebaseResult);
        return firebaseResult;
      }
    }

    // 🆘 LAYER 5: Hardcoded "active" fallback (never crash the app)
    debugPrint('[QuotaGuard] All layers failed → using hardcoded active');
    return SystemStatus.active();
  }

  void dispose() {
    _pollingTimer?.cancel();
    _statusController.close();
    _instance = null;
  }

  // ─────────────────────────────────────
  // Private: Fetch layers
  // ─────────────────────────────────────

  Future<SystemStatus?> _fetchVercel() async {
    for (final url in _Cfg.vercelStatusUrls) {
      try {
        final response = await http
            .get(Uri.parse(url))
            .timeout(_Cfg.networkTimeout);
        if (response.statusCode == 200) {
          final j = jsonDecode(response.body) as Map<String, dynamic>;
          final s = SystemStatus.fromJson(j, source: 'vercel_cdn');
          debugPrint('[QuotaGuard] L2 Vercel OK from $url: ${s.status}');
          return s;
        }
      } catch (e) {
        debugPrint('[QuotaGuard] L2 Vercel FAIL ($url): $e');
      }
    }

    // Try static fallback JSONs
    for (final fallbackUrl in _Cfg.staticFallbackUrls) {
      try {
        final response = await http
            .get(Uri.parse(fallbackUrl))
            .timeout(_Cfg.networkTimeout);
        if (response.statusCode == 200) {
          final j = jsonDecode(response.body) as Map<String, dynamic>;
          return SystemStatus.fromJson(j, source: 'static_cdn');
        }
      } catch (e) {
        debugPrint('[QuotaGuard] L2 Static CDN FAIL ($fallbackUrl): $e');
      }
    }
    return null;
  }

  Future<SystemStatus?> _fetchFirebase() async {
    try {
      debugPrint('[QuotaGuard] L4 Firebase direct hit (rate-limited)');
      final response = await http
          .get(Uri.parse(_Cfg.firebaseUrl))
          .timeout(_Cfg.networkTimeout);
      if (response.statusCode == 200) {
        final body = response.body;
        if (body == 'null' || body.isEmpty) return SystemStatus.active();
        final j = jsonDecode(body) as Map<String, dynamic>;
        return SystemStatus.fromJson(j, source: 'firebase_direct');
      }
    } catch (e) {
      debugPrint('[QuotaGuard] L4 Firebase FAIL: $e');
    }
    return null;
  }

  // ─────────────────────────────────────
  // Private: Local cache (SharedPreferences)
  // ─────────────────────────────────────

  Future<void> _saveToLocal(SystemStatus s) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final json = jsonEncode({
        'status': s.status,
        'message': s.message,
        'updatedAt': s.updatedAt,
        'wipeTimestamp': s.wipeTimestamp,
        '_source': 'local_cache',
      });
      await prefs.setString(_Cfg.spKeyStatus, json);
      await prefs.setInt(_Cfg.spKeyTimestamp, DateTime.now().millisecondsSinceEpoch);
    } catch (e) {
      debugPrint('[QuotaGuard] Local save FAIL: $e');
    }
  }

  Future<SystemStatus?> _loadFromLocal() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final ts = prefs.getInt(_Cfg.spKeyTimestamp) ?? 0;
      final age = Duration(milliseconds: DateTime.now().millisecondsSinceEpoch - ts);
      // Local cache valid for 5 minutes (longer than Vercel so it's emergency fallback only)
      if (age > const Duration(minutes: 5)) return null;
      final raw = prefs.getString(_Cfg.spKeyStatus);
      if (raw == null) return null;
      final j = jsonDecode(raw) as Map<String, dynamic>;
      return SystemStatus.fromJson(j);
    } catch (e) {
      return null;
    }
  }

  // ─────────────────────────────────────
  // Private: Polling & helpers
  // ─────────────────────────────────────

  void _setMemCache(SystemStatus s) {
    _memCache = s;
    _memCacheTime = DateTime.now();
  }

  void _startPolling() {
    _pollingTimer?.cancel();
    // Poll every 15s - hits Vercel Edge CDN memory cache, zero Firebase exhaustion
    _pollingTimer = Timer.periodic(const Duration(seconds: 15), (_) async {
      final status = await getStatus();
      _statusController.add(status);
    });
  }
}

// ════════════════════════════════════════
// 🧩 Widget: SystemStatusGuard
// Wraps any widget - shows error UI if frozen/wiped
// ════════════════════════════════════════

class SystemStatusGuard extends StatefulWidget {
  final Widget child;
  final Widget Function(SystemStatus)? frozenBuilder;
  final Widget Function(SystemStatus)? wipedBuilder;
  final VoidCallback? onWipe;

  const SystemStatusGuard({
    super.key,
    required this.child,
    this.frozenBuilder,
    this.wipedBuilder,
    this.onWipe,
  });

  @override
  State<SystemStatusGuard> createState() => _SystemStatusGuardState();
}

class _SystemStatusGuardState extends State<SystemStatusGuard> {
  final _guard = ZaqzoukaQuotaGuard();
  SystemStatus _status = SystemStatus.active();

  @override
  void initState() {
    super.initState();
    _guard.initialize().then((_) {
      _guard.statusStream.listen((s) {
        if (mounted) {
          setState(() => _status = s);
          if (s.isWiped) {
            widget.onWipe?.call();
          }
        }
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_status.isWiped) {
      return widget.wipedBuilder?.call(_status) ?? _DefaultWipedScreen(_status);
    }
    if (_status.isFrozen) {
      return widget.frozenBuilder?.call(_status) ?? _DefaultFrozenScreen(_status);
    }
    return widget.child;
  }
}

class _DefaultFrozenScreen extends StatelessWidget {
  final SystemStatus status;
  const _DefaultFrozenScreen(this.status);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.pause_circle_outline, size: 80, color: Color(0xFFFFB800)),
              const SizedBox(height: 24),
              const Text('⚠️ الخدمة متوقفة مؤقتاً',
                style: TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold),
                textAlign: TextAlign.center),
              const SizedBox(height: 12),
              Text(status.message,
                style: const TextStyle(color: Colors.white70, fontSize: 16),
                textAlign: TextAlign.center),
              const SizedBox(height: 32),
              const CircularProgressIndicator(color: Color(0xFFFFB800)),
              const SizedBox(height: 12),
              const Text('جاري التحقق تلقائياً...',
                style: TextStyle(color: Colors.white38, fontSize: 13)),
            ],
          ),
        ),
      ),
    );
  }
}

class _DefaultWipedScreen extends StatelessWidget {
  final SystemStatus status;
  const _DefaultWipedScreen(this.status);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0A),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.block, size: 80, color: Color(0xFFFF3D3D)),
              const SizedBox(height: 24),
              const Text('الخدمة غير متاحة',
                style: TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold),
                textAlign: TextAlign.center),
              const SizedBox(height: 12),
              const Text('عذراً، هذا التطبيق لم يعد متاحاً.',
                style: TextStyle(color: Colors.white54, fontSize: 15),
                textAlign: TextAlign.center),
            ],
          ),
        ),
      ),
    );
  }
}
