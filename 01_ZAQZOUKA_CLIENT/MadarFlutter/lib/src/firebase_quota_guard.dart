// 🛡️ ZAQZOUKA FIREBASE QUOTA GUARD & INSTANT KILLSWITCH
// ========================================================
// الغرض: حماية كوتا Firebase المجانية + استجابة فورية لأوامر التجميد والمسح
// 1. القراءة من Vercel CDN والـ Dashboard API أولاً مع كاش ذكي
// 2. مراقبة حالة التطبيق (Lifecycle) لإيقاف التطبيق فور الرجوع إليه إذا كان مجمداً
// 3. منع ظهور شاشة المحتوى نهائياً عند بدء التشغيل إذا كانت المنظومة مجمدة
// 4. استجابة سريعة جداً (Polling 8-10 ثواني) بدون استنزاف البطارية أو السيرفر

import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

// ════════════════════════════════════════
// 📌 CONFIGURATION
// ════════════════════════════════════════

class _Cfg {
  // Live Endpoints (Dashboard API is where Master Control writes directly)
  static const vercelStatusUrls = [
    'https://dashboard-api-amber-chi.vercel.app/api/system_control',
    'https://dashboard-api-amber-chi.vercel.app/api/status',
  ];

  // Static fallback JSON on Vercel CDN edge (ultimate fallback)
  static const staticFallbackUrls = [
    'https://web-nu-teal-29.vercel.app/status/system.json',
    'https://zaqzouka-vercel-hub.vercel.app/status/system.json',
  ];

  // Firebase RTDB direct (last resort only)
  static const firebaseUrl =
      'https://madartv-default-rtdb.firebaseio.com/system_control.json';

  // Cache TTL: 10s for super responsive freeze/wipe response without overloading servers
  static const localCacheTtl = Duration(seconds: 10);
  static const pollingInterval = Duration(seconds: 8);
  static const firebaseMinInterval = Duration(minutes: 5); // max 1 direct hit per 5 min
  static const networkTimeout = Duration(seconds: 4);

  // SharedPreferences keys
  static const spKeyStatus = 'zaq_system_status';
  static const spKeyTimestamp = 'zaq_status_ts';
}

// ════════════════════════════════════════
// 📦 System Status Model
// ════════════════════════════════════════

class SystemStatus {
  final String status; // "active" | "frozen" | "wiped"
  final String message;
  final int updatedAt;
  final int wipeTimestamp;
  final String source;
  final Map<String, dynamic> components;

  const SystemStatus({
    required this.status,
    required this.message,
    required this.updatedAt,
    required this.wipeTimestamp,
    required this.source,
    this.components = const {},
  });

  factory SystemStatus.fromJson(Map<String, dynamic> j, {String source = 'unknown'}) {
    final comps = (j['components'] as Map<String, dynamic>?) ?? {};
    // 🔑 KEY FIX: If app_enabled=false in components → treat as frozen
    // This is what makes the Master Control kill switch actually work
    String resolvedStatus = (j['status'] as String? ?? 'active').toLowerCase();
    String resolvedMessage = j['message'] as String? ?? 'المنظومة نشطة';

    if (resolvedStatus == 'active' && comps['app_enabled'] == false) {
      resolvedStatus = 'frozen';
      resolvedMessage = 'عفواً، التطبيق متوقف حالياً للصيانة.';
    }

    return SystemStatus(
      status: resolvedStatus,
      message: resolvedMessage,
      updatedAt: (j['updatedAt'] as num?)?.toInt() ?? 0,
      wipeTimestamp: (j['wipeTimestamp'] as num?)?.toInt() ?? 0,
      source: j['_source'] as String? ?? source,
      components: comps,
    );
  }

  factory SystemStatus.active() => const SystemStatus(
        status: 'active',
        message: 'المنظومة نشطة وتعمل بشكل طبيعي',
        updatedAt: 0,
        wipeTimestamp: 0,
        source: 'hardcoded_default',
        components: {},
      );

  factory SystemStatus.frozen({String message = 'عفواً، الخدمة متوقفة حالياً للتحديث والصيانة.'}) =>
      SystemStatus(
        status: 'frozen',
        message: message,
        updatedAt: DateTime.now().millisecondsSinceEpoch,
        wipeTimestamp: 0,
        source: 'cache_frozen',
        components: {},
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
  bool _isFetching = false;

  SystemStatus get currentStatus => _memCache ?? SystemStatus.active();

  // ─────────────────────────────────────
  // Public API
  // ─────────────────────────────────────

  /// Initialize the guard, perform immediate check, and start polling
  Future<void> initialize() async {
    _startPolling();
    final status = await getStatus(force: true);
    _statusController.add(status);
  }

  /// Trigger immediate live status check (e.g. on resume or pull-to-refresh)
  Future<void> checkNow({bool force = true}) async {
    final status = await getStatus(force: force);
    _statusController.add(status);
  }

  /// Get system status (uses tiered cache)
  Future<SystemStatus> getStatus({bool force = false}) async {
    // 🏎️ LAYER 1: In-memory (< 10s old)
    if (!force && _memCache != null && _memCacheTime != null) {
      final age = DateTime.now().difference(_memCacheTime!);
      if (age < _Cfg.localCacheTtl) {
        return _memCache!;
      }
    }

    if (_isFetching && _memCache != null) {
      return _memCache!;
    }
    _isFetching = true;

    try {
      // 🌐 LAYER 2: Vercel CDN & Dashboard API (handles millions of users)
      final vercelResult = await _fetchVercel();
      if (vercelResult != null) {
        _setMemCache(vercelResult);
        await _saveToLocal(vercelResult);
        return vercelResult;
      }

      // 💾 LAYER 3: Local SharedPreferences cache
      final localResult = await loadFromLocal();
      if (localResult != null) {
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
    } finally {
      _isFetching = false;
    }

    // 🆘 LAYER 5: Fallback to existing cache or active
    return _memCache ?? SystemStatus.active();
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
            .get(
              Uri.parse(url),
              headers: {'Accept': 'application/json', 'Cache-Control': 'no-cache'},
            )
            .timeout(_Cfg.networkTimeout);
        if (response.statusCode == 200) {
          final j = jsonDecode(response.body) as Map<String, dynamic>;
          final s = SystemStatus.fromJson(j, source: 'vercel_api');
          return s;
        }
      } catch (_) {}
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
      } catch (_) {}
    }
    return null;
  }

  Future<SystemStatus?> _fetchFirebase() async {
    try {
      final response = await http
          .get(Uri.parse(_Cfg.firebaseUrl))
          .timeout(_Cfg.networkTimeout);
      if (response.statusCode == 200) {
        final body = response.body;
        if (body == 'null' || body.isEmpty) return SystemStatus.active();
        final j = jsonDecode(body) as Map<String, dynamic>;
        return SystemStatus.fromJson(j, source: 'firebase_direct');
      }
    } catch (_) {}
    return null;
  }

  // ─────────────────────────────────────
  // Local cache (SharedPreferences)
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
    } catch (_) {}
  }

  Future<SystemStatus?> loadFromLocal() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final ts = prefs.getInt(_Cfg.spKeyTimestamp) ?? 0;
      final age = Duration(milliseconds: DateTime.now().millisecondsSinceEpoch - ts);
      // Local cache valid for 10 minutes as emergency offline fallback
      if (age > const Duration(minutes: 10)) return null;
      final raw = prefs.getString(_Cfg.spKeyStatus);
      if (raw == null) return null;
      final j = jsonDecode(raw) as Map<String, dynamic>;
      return SystemStatus.fromJson(j, source: 'local_cache');
    } catch (_) {
      return null;
    }
  }

  // ─────────────────────────────────────
  // Polling & helpers
  // ─────────────────────────────────────

  void _setMemCache(SystemStatus s) {
    _memCache = s;
    _memCacheTime = DateTime.now();
  }

  void _startPolling() {
    _pollingTimer?.cancel();
    // Fast periodic check (8s) hits Edge CDN / in-memory cache with zero Firebase quota usage
    _pollingTimer = Timer.periodic(_Cfg.pollingInterval, (_) async {
      final status = await getStatus();
      _statusController.add(status);
    });
  }
}

// ════════════════════════════════════════
// 🧩 Widget: SystemStatusGuard
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

class _SystemStatusGuardState extends State<SystemStatusGuard> with WidgetsBindingObserver {
  final _guard = ZaqzoukaQuotaGuard();
  late SystemStatus _status;
  bool _initialized = false;
  StreamSubscription<SystemStatus>? _sub;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _status = _guard.currentStatus;

    // 1. Subscribe FIRST so no status events are missed
    _sub = _guard.statusStream.listen((s) {
      if (mounted) {
        setState(() {
          _status = s;
          _initialized = true;
        });
        if (s.isWiped) {
          widget.onWipe?.call();
        }
      }
    });

    // 2. Load cached status immediately to prevent flash of content
    _loadInitial();
  }

  Future<void> _loadInitial() async {
    // Check local storage first
    final local = await _guard.loadFromLocal();
    if (local != null && mounted) {
      setState(() {
        _status = local;
        if (local.isFrozen || local.isWiped) {
          _initialized = true;
        }
      });
      if (local.isWiped) {
        widget.onWipe?.call();
      }
    }

    // Now perform live network check
    await _guard.initialize();
    if (mounted) {
      setState(() {
        _status = _guard.currentStatus;
        _initialized = true;
      });
      if (_status.isWiped) {
        widget.onWipe?.call();
      }
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      // User returned from background / Master Control -> Immediate verification
      _guard.checkNow(force: true);
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _sub?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!_initialized) {
      return const Scaffold(
        backgroundColor: Color(0xFF0F0F14),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                width: 38,
                height: 38,
                child: CircularProgressIndicator(color: Color(0xFFD4AF37), strokeWidth: 2.5),
              ),
              SizedBox(height: 18),
              Text(
                'جاري التحقق من المنظومة...',
                style: TextStyle(color: Colors.white70, fontSize: 14),
              ),
            ],
          ),
        ),
      );
    }

    if (_status.isWiped) {
      return widget.wipedBuilder?.call(_status) ?? _DefaultWipedScreen(_status);
    }
    if (_status.isFrozen) {
      return widget.frozenBuilder?.call(_status) ?? _DefaultFrozenScreen(_status);
    }
    return widget.child;
  }
}

// ════════════════════════════════════════
// 🔒 Default Frozen Screen (Matching Website UI)
// ════════════════════════════════════════

class _DefaultFrozenScreen extends StatefulWidget {
  final SystemStatus status;
  const _DefaultFrozenScreen(this.status);

  @override
  State<_DefaultFrozenScreen> createState() => _DefaultFrozenScreenState();
}

class _DefaultFrozenScreenState extends State<_DefaultFrozenScreen> {
  bool _isChecking = false;

  Future<void> _handleRefresh() async {
    if (_isChecking) return;
    setState(() => _isChecking = true);
    await ZaqzoukaQuotaGuard().checkNow(force: true);
    if (mounted) {
      setState(() => _isChecking = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF08080C),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 28),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 36),
            decoration: BoxDecoration(
              color: const Color(0xFF14141E),
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: const Color(0xFFD4AF37).withValues(alpha: 0.3), width: 1.5),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.5),
                  blurRadius: 30,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Lock Icon Container
                Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    color: const Color(0xFFD4AF37).withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                    border: Border.all(color: const Color(0xFFD4AF37).withValues(alpha: 0.3)),
                  ),
                  child: const Center(
                    child: Icon(Icons.lock_rounded, color: Color(0xFFD4AF37), size: 36),
                  ),
                ),
                const SizedBox(height: 18),

                // Pill Badge
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                  decoration: BoxDecoration(
                    color: const Color(0xFFD4AF37).withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: const Text(
                    'صيانة مؤقتة',
                    style: TextStyle(
                      color: Color(0xFFD4AF37),
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // Title
                const Text(
                  'زقزوقة TV',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 24,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 0.5,
                  ),
                ),
                const SizedBox(height: 12),

                // Message
                Text(
                  widget.status.message,
                  style: const TextStyle(
                    color: Color(0xFF9E9EB2),
                    fontSize: 14,
                    height: 1.6,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 28),

                // Action Button: "إعادة التحقق الآن"
                SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: ElevatedButton(
                    onPressed: _isChecking ? null : _handleRefresh,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFFD4AF37),
                      foregroundColor: Colors.black,
                      elevation: 4,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                    ),
                    child: _isChecking
                        ? const SizedBox(
                            width: 22,
                            height: 22,
                            child: CircularProgressIndicator(color: Colors.black, strokeWidth: 2.5),
                          )
                        : const Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.refresh_rounded, size: 20),
                              SizedBox(width: 8),
                              Text(
                                'إعادة التحقق الآن',
                                style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                              ),
                            ],
                          ),
                  ),
                ),
                const SizedBox(height: 12),
                const Text(
                  'جاري التحقق التلقائي كل 8 ثوانٍ...',
                  style: TextStyle(color: Colors.white30, fontSize: 11),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ════════════════════════════════════════
// 🚫 Default Wiped Screen
// ════════════════════════════════════════

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
              const Text(
                'الخدمة غير متاحة',
                style: TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              const Text(
                'عذراً، هذا التطبيق لم يعد متاحاً.',
                style: TextStyle(color: Colors.white54, fontSize: 15),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
