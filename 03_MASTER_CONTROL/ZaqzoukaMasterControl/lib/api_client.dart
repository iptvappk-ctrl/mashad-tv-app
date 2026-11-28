import 'dart:convert';
import 'package:http/http.dart' as http;

class MasterApiClient {
  static const List<String> _endpoints = [
    'https://zaafilm.com/api/system_control',
    'https://dashboard-api-indol-eight.vercel.app/api/system_control',
    'https://dashboard-api-amber-chi.vercel.app/api/system_control',
    'https://zaqzouka-dashboard-api.iptvappk.workers.dev/api/system_control',
  ];
  static const String _adminSecret = 'Zq!7#mK9xPd@3wR5nT';
  static const Map<String, String> _defaultHeaders = {
    'User-Agent': 'Mozilla/5.0 (Android; ZaqzoukaMaster) Zaqzouka/1.0',
    'x-admin-secret': _adminSecret,
    'Accept': 'application/json',
  };

  /// Fetch current system state from the cloud
  static Future<Map<String, dynamic>> fetchStatus() async {
    for (final url in _endpoints) {
      try {
        final res = await http.get(Uri.parse(url), headers: _defaultHeaders).timeout(const Duration(seconds: 6));
        if (res.statusCode == 200) {
          final data = jsonDecode(res.body) as Map<String, dynamic>;
          // Ensure components map exists
          if (!data.containsKey('components') || data['components'] == null) {
            data['components'] = {
              'download_site_enabled': true,
              'web_player_enabled': true,
              'website_enabled': true,
              'app_enabled': true,
              'player_enabled': true,
              'admin_enabled': true,
            };
          }
          if (!data.containsKey('status') || data['status'] == null) {
            data['status'] = 'active';
          }
          return data;
        }
      } catch (e) {
        // ignore: avoid_print
        print('Error fetching status from $url: $e');
      }
    }
    // Fallback: direct Firebase RTDB check
    try {
      final res = await http.get(
        Uri.parse('https://madartv-default-rtdb.firebaseio.com/system_control.json'),
      ).timeout(const Duration(seconds: 5));
      if (res.statusCode == 200 && res.body.isNotEmpty && res.body != 'null') {
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        if (!data.containsKey('components') || data['components'] == null) {
          data['components'] = {
            'download_site_enabled': true,
            'web_player_enabled': true,
            'website_enabled': true,
            'app_enabled': true,
            'player_enabled': true,
            'admin_enabled': true,
          };
        }
        return data;
      }
    } catch (_) {}

    return {
      'status': 'unknown',
      'message': 'تعذر الاتصال بالسحابة، يرجى فحص الإنترنت',
      'updatedAt': 0,
      'wipeTimestamp': 0,
      'components': {
        'download_site_enabled': true,
        'web_player_enabled': true,
        'website_enabled': true,
        'app_enabled': true,
        'player_enabled': true,
        'admin_enabled': true,
      },
    };
  }

  /// Toggle a single component on or off
  static Future<bool> setComponentStatus(String component, bool enabled) async {
    final body = jsonEncode({
      'action': 'set_component',
      'component': component,
      'enabled': enabled,
      'secret': _adminSecret,
    });

    for (final url in _endpoints) {
      try {
        final res = await http.post(
          Uri.parse(url),
          headers: {
            'Content-Type': 'application/json',
            'x-admin-secret': _adminSecret,
          },
          body: body,
        ).timeout(const Duration(seconds: 10));
        if (res.statusCode == 200) return true;
      } catch (e) {
        // ignore: avoid_print
        print('Error setting component status on $url: $e');
      }
    }

    // Fallback: direct Firebase RTDB REST API patch
    try {
      final res = await http.patch(
        Uri.parse('https://madartv-default-rtdb.firebaseio.com/system_control/components.json'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({component: enabled}),
      ).timeout(const Duration(seconds: 8));
      return res.statusCode == 200;
    } catch (_) {}

    return false;
  }

  /// Trigger Emergency Freeze (تجميد فوري)
  static Future<bool> triggerFreeze({required String message}) async {
    final body = jsonEncode({
      'action': 'freeze',
      'message': message,
      'secret': _adminSecret,
    });

    for (final url in _endpoints) {
      try {
        final res = await http.post(
          Uri.parse(url),
          headers: {
            'Content-Type': 'application/json',
            'x-admin-secret': _adminSecret,
          },
          body: body,
        ).timeout(const Duration(seconds: 10));
        if (res.statusCode == 200) return true;
      } catch (e) {
        // ignore: avoid_print
        print('Error triggering freeze on $url: $e');
      }
    }
    return false;
  }

  /// Trigger Nuclear Wipe
  static Future<bool> triggerNuclearWipe() async {
    final body = jsonEncode({
      'action': 'wipe',
      'wipeServers': true,
      'secret': _adminSecret,
    });

    for (final url in _endpoints) {
      try {
        final res = await http.post(
          Uri.parse(url),
          headers: {
            'Content-Type': 'application/json',
            'x-admin-secret': _adminSecret,
          },
          body: body,
        ).timeout(const Duration(seconds: 12));
        if (res.statusCode == 200) return true;
      } catch (e) {
        // ignore: avoid_print
        print('Error triggering wipe on $url: $e');
      }
    }
    return false;
  }

  /// Trigger Full Restore
  static Future<bool> triggerRestore({required Map<String, dynamic> serversData}) async {
    final body = jsonEncode({
      'action': 'restore',
      'servers': serversData['servers'] ?? {},
      'catalog': serversData['catalog'] ?? {},
      'message': 'المنظومة نشطة وتعمل بكامل طاقتها.',
      'secret': _adminSecret,
    });

    for (final url in _endpoints) {
      try {
        final res = await http.post(
          Uri.parse(url),
          headers: {
            'Content-Type': 'application/json',
            'x-admin-secret': _adminSecret,
          },
          body: body,
        ).timeout(const Duration(seconds: 12));
        if (res.statusCode == 200) return true;
      } catch (e) {
        // ignore: avoid_print
        print('Error triggering restore on $url: $e');
      }
    }
    return false;
  }

  /// Save Sovereign Owner Ad Configuration (خاص بالمالك وتجاوز كود الشريك)
  static Future<bool> saveOwnerAdConfig({
    required bool enabled,
    required bool overridePartner,
    required String adHtml,
    required String adUrl,
    required int skipDelaySeconds,
    required bool bannerEnabled,
    required String bannerHtml,
    required String bannerUrl,
  }) async {
    final payload = {
      'enabled': enabled,
      'override_partner': overridePartner,
      'ad_html': adHtml,
      'ad_url': adUrl,
      'skip_delay_seconds': skipDelaySeconds,
      'banner_enabled': bannerEnabled,
      'banner_html': bannerHtml,
      'banner_url': bannerUrl,
      'updatedAt': DateTime.now().millisecondsSinceEpoch,
    };

    final body = jsonEncode({
      'action': 'set_owner_ad',
      ...payload,
      'secret': _adminSecret,
    });

    for (final url in _endpoints) {
      try {
        final res = await http.post(
          Uri.parse(url),
          headers: {
            'Content-Type': 'application/json',
            'x-admin-secret': _adminSecret,
          },
          body: body,
        ).timeout(const Duration(seconds: 10));
        if (res.statusCode == 200) return true;
      } catch (e) {
        // ignore: avoid_print
        print('Error saving owner ad config to $url: $e');
      }
    }

    // Direct Firebase RTDB REST API fallback
    try {
      final res = await http.put(
        Uri.parse('https://madartv-default-rtdb.firebaseio.com/system_control/owner_ad_config.json'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(payload),
      ).timeout(const Duration(seconds: 8));
      return res.statusCode == 200;
    } catch (_) {}

    return false;
  }

  /// Sovereign Ad Network Ad Network Switch (تبديل فوري بين إعلانات المالك وإعلانات الشريك)
  static Future<bool> setStartioControl({
    required String activeSource,
    required String ownerAppId,
    required String partnerAppId,
    bool bannerEnabled = true,
    bool videoEnabled = true,
  }) async {
    final payload = {
      'action': 'set_startio_control',
      'active_source': activeSource,
      'owner_app_id': ownerAppId,
      'partner_app_id': partnerAppId,
      'banner_enabled': bannerEnabled,
      'video_enabled': videoEnabled,
      'secret': _adminSecret,
    };

    final body = jsonEncode(payload);

    for (final url in _endpoints) {
      try {
        final res = await http.post(
          Uri.parse(url),
          headers: {
            'Content-Type': 'application/json',
            'x-admin-secret': _adminSecret,
          },
          body: body,
        ).timeout(const Duration(seconds: 10));
        if (res.statusCode == 200) return true;
      } catch (_) {}
    }

    // Direct Firebase RTDB fallback
    try {
      final res = await http.patch(
        Uri.parse('https://madartv-default-rtdb.firebaseio.com/system_control/owner_ad_config.json'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'active_source': activeSource,
          'owner_app_id': ownerAppId,
          'partner_app_id': partnerAppId,
          'banner_enabled': bannerEnabled,
          'video_enabled': videoEnabled,
          'override_partner': activeSource == 'owner',
          'updated_at': DateTime.now().toIso8601String(),
        }),
      ).timeout(const Duration(seconds: 8));
      return res.statusCode == 200;
    } catch (_) {}

    return false;
  }

  /// Publish or cancel In-App Update (with optional or forced toggle)
  static Future<bool> publishAppUpdate({
    required bool enabled,
    required int versionCode,
    required String versionName,
    required String apkUrl,
    required String releaseNotes,
    required bool forced,
  }) async {
    final payload = {
      'enabled': enabled,
      'latestVersionCode': versionCode,
      'latestVersionName': versionName,
      'apkUrl': apkUrl,
      'releaseNotes': releaseNotes,
      'forced': forced,
      'updatedAt': DateTime.now().millisecondsSinceEpoch,
    };

    final body = jsonEncode({
      'action': 'set_app_update',
      ...payload,
      'secret': _adminSecret,
    });

    for (final url in _endpoints) {
      try {
        final res = await http.post(
          Uri.parse(url),
          headers: {
            'Content-Type': 'application/json',
            'x-admin-secret': _adminSecret,
          },
          body: body,
        ).timeout(const Duration(seconds: 10));
        if (res.statusCode == 200) return true;
      } catch (e) {
        // ignore: avoid_print
        print('Error publishing update to $url: $e');
      }
    }

    // Direct Firebase RTDB REST API fallback
    try {
      final res = await http.put(
        Uri.parse('https://madartv-default-rtdb.firebaseio.com/system_control/app_update.json'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(payload),
      ).timeout(const Duration(seconds: 8));
      return res.statusCode == 200;
    } catch (_) {}

    return false;
  }
}
