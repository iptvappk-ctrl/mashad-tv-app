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
  static bool _isNewInstall = false;

  static String _currentSection = 'الرئيسية 🏠';
  static String _currentContent = '';
  static Timer? _heartbeatTimer;
  static bool _initialized = false;

  // Candidate telemetry endpoints:
  // 1. Production Cloud API (Vercel Serverless + Firebase RTDB)
  // 2. Local Wi-Fi IP (for local testing)
  // 3. Android Emulator loopback
  // 4. Localhost
  static const List<String> _candidateHosts = [
    'https://zaqzouka-dashboard-api.vercel.app',
    'http://192.168.100.2:4040',
    'http://10.0.2.2:4040',
    'http://127.0.0.1:4040',
  ];

  static String _resolvedBaseUrl = 'https://zaqzouka-dashboard-api.vercel.app';

  /// Initializes device telemetry and reports session start.
  static Future<void> initialize() async {
    if (_initialized) return;
    _initialized = true;

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

    // Find reachable telemetry host in background without blocking startup
    unawaited(_discoverHostAndStartSession());
  }

  static Future<void> _discoverHostAndStartSession() async {
    for (final host in _candidateHosts) {
      final ok = await _pingHost(host);
      if (ok) {
        _resolvedBaseUrl = host;
        break;
      }
    }

    await _startSession();
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

  static Future<void> _startSession() async {
    try {
      final payload = {
        'deviceId': _deviceId,
        'device': _deviceModel,
        'deviceType': _deviceType,
        'osVersion': _osVersion,
        'countryCode': _countryCode,
        'timezone': _timezone,
        'currentSection': _currentSection,
        'currentContent': _currentContent,
        'isNewInstall': _isNewInstall,
      };

      final res = await _postJson('$_resolvedBaseUrl/api/telemetry/session/start', payload);
      if (res != null && res['sessionId'] != null) {
        _sessionId = res['sessionId'].toString();
      }
    } catch (_) {}
  }

  static void _startHeartbeatTimer() {
    _heartbeatTimer?.cancel();
    _heartbeatTimer = Timer.periodic(const Duration(seconds: 30), (_) {
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

  /// Call when user switches tabs or visits a new section.
  static void updateSection(String sectionName) {
    _currentSection = sectionName;
    if (_sessionId == null) return;
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
