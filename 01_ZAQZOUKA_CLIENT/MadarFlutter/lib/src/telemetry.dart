part of '../main.dart';

/// Non-blocking, fail-safe live telemetry client for Zaqzouka TV.
/// Reports real mobile/TV device model, user sessions, search queries, and playback stats.
class AppTelemetry {
  AppTelemetry._();
  static final AppTelemetry instance = AppTelemetry._();

  static String? _sessionId;
  static String? _deviceId;
  static String _deviceModel = 'Android Device';
  static String _deviceType = 'phone';
  static String _osVersion = 'Android';
  static String _countryCode = 'EG';
  static String _timezone = 'Africa/Cairo';
  static String _city = 'طنطا';
  static String _region = 'الغربية';
  static String _isp = 'WE / TE Data';
  static String _location = 'طنطا، الغربية، مصر (EG)';
  static bool _isNewInstall = false;

  static String _currentSection = 'الرئيسية 🏠';
  static String _currentContent = '';
  static Timer? _heartbeatTimer;
  static DateTime? _lastSectionSyncAt;
  static bool _initialized = false;

  // Candidate telemetry endpoints:
  // 1. Production Cloud API (Vercel Serverless + Firebase RTDB)
  // 2. Local Wi-Fi IP (for local testing)
  // 3. Android Emulator loopback
  // 4. Localhost
  static const List<String> _candidateHosts = [
    'https://dashboard-api-amber-chi.vercel.app',
    'https://dashboard-api-indol-eight.vercel.app',
    'http://192.168.100.2:4040',
    'http://10.0.2.2:4040',
    'http://127.0.0.1:4040',
  ];

  static String _resolvedBaseUrl = 'https://dashboard-api-amber-chi.vercel.app';
  static String _appVersion = '1.0.83+90';

  /// Initializes device telemetry and reports session start.
  static Future<void> initialize() async {
    if (_initialized) return;
    _initialized = true;

    try {
      final vInfo = await _native.invokeMapMethod<String, dynamic>('app.versionInfo');
      if (vInfo != null) {
        final vName = vInfo['versionName']?.toString() ?? '1.0.83';
        final vCode = vInfo['versionCode']?.toString() ?? '90';
        _appVersion = '$vName+$vCode';
      }
    } catch (_) {}

    try {
      final info = await _native.invokeMapMethod<String, dynamic>(
        'telemetry.deviceInfo',
      );
      if (info != null) {
        _deviceId = info['deviceId']?.toString();
        _deviceModel = info['device']?.toString() ?? 'Android Device';
        _deviceType = info['deviceType']?.toString() ?? 'phone';
        _osVersion = info['osVersion']?.toString() ?? 'Android';
        _countryCode = info['countryCode']?.toString() ?? 'EG';
        _timezone = info['timezone']?.toString() ?? 'Africa/Cairo';
        _isNewInstall = info['isNewInstall'] == true;
      }
    } catch (_) {}

    // Detect exact real location (City, Governorate, ISP) non-blockingly
    await _detectRealLocation();

    // Find reachable telemetry host in background without blocking startup
    unawaited(_discoverHostAndStartSession());
  }

  static Future<void> _detectRealLocation() async {
    try {
      final client = HttpClient()..connectionTimeout = const Duration(seconds: 2);
      final req = await client.getUrl(Uri.parse('http://ip-api.com/json'));
      final res = await req.close().timeout(const Duration(seconds: 2));
      if (res.statusCode == 200) {
        final body = await res.transform(utf8.decoder).join();
        final data = jsonDecode(body) as Map<String, dynamic>;
        if (data['status'] == 'success') {
          final rawCity = data['city']?.toString() ?? '';
          final rawRegion = data['regionName']?.toString() ?? '';
          final code = data['countryCode']?.toString() ?? 'EG';
          final isp = data['isp']?.toString() ?? 'WE / TE Data';

          _countryCode = code;
          _city = _translateCity(rawCity);
          _region = _translateRegion(rawRegion);
          _isp = isp;
          if (_city.isNotEmpty && _region.isNotEmpty && _city != _region) {
            _location = '$_city، $_region، مصر ($code)';
          } else if (_city.isNotEmpty) {
            _location = '$_city، مصر ($code)';
          }
        }
      }
      client.close();
    } catch (_) {}
  }

  static String _translateCity(String raw) {
    final c = raw.trim().toLowerCase();
    const map = {
      'tanta': 'طنطا', 'cairo': 'القاهرة', 'giza': 'الجيزة', 'alexandria': 'الإسكندرية',
      'mansoura': 'المنصورة', 'el mansoura': 'المنصورة', 'zagazig': 'الزقازيق',
      'ismailia': 'الإسماعيلية', 'suez': 'السويس', 'port said': 'بورسعيد',
      'fayoum': 'الفيوم', 'beni suef': 'بني سويف', 'minya': 'المنيا',
      'asyut': 'أسيوط', 'sohag': 'سوهاج', 'qena': 'قنا', 'luxor': 'الأقصر',
      'aswan': 'أسوان', 'hurghada': 'الغردقة', 'sharm el sheikh': 'شرم الشيخ',
      'damanhur': 'دمنهور', 'kafr el sheikh': 'كفر الشيخ', 'banha': 'بنها',
      'damietta': 'دمياط', 'shibin el kom': 'شبين الكوم', 'arish': 'العريش',
      'marsa matruh': 'مرسى مطروح', '6th of october': '6 أكتوبر',
      'riyadh': 'الرياض', 'jeddah': 'جدة', 'dammam': 'الدمام', 'dubai': 'دبي',
    };
    return map[c] ?? raw;
  }

  static String _translateRegion(String raw) {
    final r = raw.trim().toLowerCase();
    const map = {
      'gharbia': 'الغربية', 'cairo': 'القاهرة', 'giza': 'الجيزة', 'alexandria': 'الإسكندرية',
      'dakahlia': 'الدقهلية', 'sharqia': 'الشرقية', 'monufia': 'المنوفية',
      'qalyubia': 'القليوبية', 'beheira': 'البحيرة', 'kafr el sheikh': 'كفر الشيخ',
      'damietta': 'دمياط', 'port said': 'بورسعيد', 'ismailia': 'الإسماعيلية',
      'suez': 'السويس', 'red sea': 'البحر الأحمر', 'south sinai': 'جنوب سيناء',
      'riyadh': 'منطقة الرياض', 'makkah': 'منطقة مكة', 'dubai': 'إمارة دبي',
    };
    return map[r] ?? raw;
  }

  static Future<void> _discoverHostAndStartSession() async {
    // Try primary Vercel endpoint directly to avoid extra /api/health RTDB reads on startup
    final primaryOk = await _startSession();
    if (!primaryOk && _sessionId == null) {
      for (final host in _candidateHosts) {
        if (host == _resolvedBaseUrl) continue;
        final ok = await _pingHost(host);
        if (ok) {
          _resolvedBaseUrl = host;
          await _startSession();
          break;
        }
      }
    }
    _startHeartbeatTimer();
  }

  static Future<bool> _pingHost(String host) async {
    try {
      final client = HttpClient()..connectionTimeout = const Duration(seconds: 2);
      final req = await client.getUrl(Uri.parse('$host/api/health'));
      final res = await req.close().timeout(const Duration(seconds: 2));
      client.close();
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> _startSession() async {
    try {
      // ── Deduplication: check if we have a recent session saved locally ──
      // The server also deduplicates by deviceId, but we avoid unnecessary calls.
      String? savedTs;
      String? savedSessionId;
      try {
        savedTs = await _native.invokeMethod<String>('cache.read', {'key': '_zq_session_ts'});
      } catch (_) {}
      try {
        savedSessionId = await _native.invokeMethod<String>('cache.read', {'key': '_zq_session_id'});
      } catch (_) {}

      if (savedTs != null && savedSessionId != null && savedSessionId.isNotEmpty) {
        final savedTime = DateTime.tryParse(savedTs);
        if (savedTime != null) {
          final age = DateTime.now().difference(savedTime);
          if (age.inMinutes < 20) {
            // Reuse existing session — server already has it
            _sessionId = savedSessionId;
            return true; // skip creating a new session
          }
        }
      }

      final payload = {
        'deviceId': _deviceId,
        'device': _deviceModel,
        'deviceType': _deviceType,
        'osVersion': _osVersion,
        'appVersion': _appVersion,
        'countryCode': _countryCode,
        'city': _city,
        'region': _region,
        'location': _location,
        'isp': _isp,
        'timezone': _timezone,
        'currentSection': _currentSection,
        'currentContent': _currentContent,
        'isNewInstall': _isNewInstall,
      };

      final res = await _postJson('$_resolvedBaseUrl/api/telemetry/session/start', payload);
      if (res != null && res['sessionId'] != null) {
        _sessionId = res['sessionId'].toString();
        // Save session ID and timestamp to avoid duplicates on quick restarts
        final nowIso = DateTime.now().toIso8601String();
        await _native.invokeMethod('cache.write', {'key': '_zq_session_ts', 'value': nowIso}).catchError((_) => null);
        await _native.invokeMethod('cache.write', {'key': '_zq_session_id', 'value': _sessionId}).catchError((_) => null);
        return true;
      }
    } catch (_) {}
    return false;
  }

  static void _startHeartbeatTimer() {
    _heartbeatTimer?.cancel();
    _heartbeatTimer = Timer.periodic(const Duration(minutes: 3), (_) {
      sendHeartbeat();
    });
  }

  /// Heartbeat ping to keep session alive and update duration.
  static Future<void> sendHeartbeat() async {
    if (_sessionId == null) return;
    try {
      await _postJson('$_resolvedBaseUrl/api/telemetry/session/heartbeat', {
        'sessionId': _sessionId,
        'currentSection': _currentSection,
        'currentContent': _currentContent,
      });
    } catch (_) {}
  }

  /// Call when user switches tabs or visits a new section (throttled to reduce quota writes).
  static void updateSection(String sectionName) {
    final changed = _currentSection != sectionName;
    _currentSection = sectionName;
    if (_sessionId == null || !changed) return;
    final now = DateTime.now();
    if (_lastSectionSyncAt != null &&
        now.difference(_lastSectionSyncAt!) < const Duration(seconds: 45)) {
      return;
    }
    _lastSectionSyncAt = now;
    unawaited(_postJson('$_resolvedBaseUrl/api/telemetry/action', {
      'sessionId': _sessionId,
      'section': sectionName,
    }));
  }

  /// Call when user submits a search query in any section.
  static void trackSearch({
    required String query,
    bool found = true,
    int resultsCount = 0,
  }) {
    final trimmed = query.trim();
    if (trimmed.isEmpty) return;
    unawaited(_postJson('$_resolvedBaseUrl/api/telemetry/search', {
      'sessionId': _sessionId,
      'query': trimmed,
      'found': found,
      'resultsCount': resultsCount,
    }));
  }

  /// Call when media playback starts, progresses, or encounters an error.
  static void trackPlayback({
    required String title,
    required String category,
    String event = 'start', // 'start' | 'progress' | 'complete' | 'error'
    int? completionPercent,
    String? errorMessage,
  }) {
    final cleanTitle = title.trim();
    if (cleanTitle.isEmpty) return;

    if (event == 'start') {
      _currentContent = cleanTitle;
      _currentSection = category;
    }

    unawaited(_postJson('$_resolvedBaseUrl/api/telemetry/playback', {
      'sessionId': _sessionId,
      'title': cleanTitle,
      'category': category,
      'event': event,
      'completionPercent': completionPercent,
      'errorMessage': errorMessage,
    }));
  }

  /// Call when app goes to background / closes.
  static Future<void> endSession() async {
    _heartbeatTimer?.cancel();
    if (_sessionId == null) return;
    try {
      await _postJson('$_resolvedBaseUrl/api/telemetry/session/end', {
        'sessionId': _sessionId,
      });
      _sessionId = null;
    } catch (_) {}
  }

  /// Call when app resumes from background.
  static void onAppResumed() {
    if (_sessionId == null) {
      unawaited(_startSession());
      _startHeartbeatTimer();
    } else {
      unawaited(sendHeartbeat());
    }
  }

  // ═════════════════════════════════════════════════════════════════════
  // HTTP POST Helper using dart:io HttpClient
  // ═════════════════════════════════════════════════════════════════════

  static Future<Map<String, dynamic>?> _postJson(
    String url,
    Map<String, dynamic> body,
  ) async {
    HttpClient? client;
    try {
      final uri = Uri.parse(url);
      client = HttpClient()..connectionTimeout = const Duration(seconds: 3);
      final request = await client.postUrl(uri);
      request.headers.contentType = ContentType.json;
      request.headers.set('Accept', 'application/json');
      request.write(jsonEncode(body));
      final response = await request.close().timeout(const Duration(seconds: 3));

      if (response.statusCode >= 200 && response.statusCode < 300) {
        final str = await response.transform(utf8.decoder).join();
        return jsonDecode(str) as Map<String, dynamic>?;
      }
    } catch (_) {
      // Fail silent
    } finally {
      client?.close(force: true);
    }
    return null;
  }
}
