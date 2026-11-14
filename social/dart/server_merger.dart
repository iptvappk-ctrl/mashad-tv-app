// server_merger.dart
// ──────────────────
// Merges stream servers from multiple providers, removes duplicates,
// ranks by expected speed, and builds a fallback chain for the player.
//
// Compatible with the existing MadarFlutter server format:
//   { "name": "...", "embedUrl": "...", "directStreamUrl": "...", "providerName": "..." }
//
// HOW TO USE:
//   final merger = ServerMerger();
//   merger.addAll(servers1);
//   merger.addAll(servers2);
//   final ranked = merger.getRanked();     // best server first
//   final fallback = merger.buildFallbackJson(); // for backupSourcesJson

// ignore_for_file: unnecessary_null_comparison

import 'dart:convert';

// ── Server model ──────────────────────────────────────────────────────────────
class StreamServer {
  final String name;
  final String embedUrl;
  final String directStreamUrl;
  final String providerName;
  final String quality;
  final String sourcePageUrl;
  int speedScore; // 0–100, higher = faster

  StreamServer({
    required this.name,
    required this.embedUrl,
    this.directStreamUrl = '',
    this.providerName = '',
    this.quality = 'HD',
    this.sourcePageUrl = '',
    this.speedScore = 50,
  });

  factory StreamServer.fromMap(Map<String, dynamic> m) => StreamServer(
        name: _s(m['name']),
        embedUrl: _s(m['embedUrl']),
        directStreamUrl: _s(m['directStreamUrl']),
        providerName: _s(m['providerName']),
        quality: _s(m['quality']).isEmpty ? 'HD' : _s(m['quality']),
        sourcePageUrl: _s(m['sourcePageUrl']),
        speedScore: (m['speedScore'] as num?)?.toInt() ?? _defaultScore(_s(m['name'])),
      );

  Map<String, dynamic> toMap() => {
        'name': name,
        'embedUrl': embedUrl,
        'directStreamUrl': directStreamUrl,
        'providerName': providerName,
        'quality': quality,
        'sourcePageUrl': sourcePageUrl,
        'speedScore': speedScore,
      };

  static String _s(dynamic v) => v?.toString().trim() ?? '';

  /// Default speed score based on known fast hosts
  static int _defaultScore(String name) {
    final lower = name.toLowerCase();
    if (lower.contains('govid') || lower.contains('gov')) return 90;
    if (lower.contains('vidbam') || lower.contains('vidbom')) return 85;
    if (lower.contains('uqload')) return 80;
    if (lower.contains('streamtape')) return 78;
    if (lower.contains('filelions')) return 75;
    if (lower.contains('turbo')) return 73;
    if (lower.contains('mixdrop')) return 70;
    if (lower.contains('voe')) return 68;
    if (lower.contains('dood')) return 65;
    if (lower.contains('fembed')) return 60;
    if (lower.contains('mp4upload')) return 58;
    if (lower.contains('streamlare')) return 55;
    return 50;
  }

  bool get hasDirectUrl => directStreamUrl.isNotEmpty && directStreamUrl.startsWith('http');
  bool get hasEmbedUrl  => embedUrl.isNotEmpty && embedUrl.startsWith('http');
  bool get isUsable     => hasDirectUrl || hasEmbedUrl;
}

// ── Merger class ──────────────────────────────────────────────────────────────
class ServerMerger {
  final List<StreamServer> _servers = [];

  /// Add servers from a List<Map<String,dynamic>> (native bridge format)
  void addAll(List<Map<String, dynamic>> maps) {
    for (final m in maps) {
      _addSingle(StreamServer.fromMap(m));
    }
  }

  /// Add servers from a JSON string
  void addFromJson(String json) {
    try {
      final list = jsonDecode(json) as List<dynamic>;
      addAll(list.whereType<Map<String, dynamic>>().toList());
    } catch (_) {}
  }

  /// Add a single server (deduplicates by URL)
  void _addSingle(StreamServer s) {
    if (!s.isUsable) return;
    final key = _normalizeUrl(s.hasDirectUrl ? s.directStreamUrl : s.embedUrl);
    final exists = _servers.any((e) => _normalizeUrl(e.hasDirectUrl ? e.directStreamUrl : e.embedUrl) == key);
    if (!exists) _servers.add(s);
  }

  /// Add a single map
  void add(Map<String, dynamic> m) => _addSingle(StreamServer.fromMap(m));

  /// Return servers ranked best-first
  List<StreamServer> getRanked() {
    final ranked = List<StreamServer>.from(_servers);
    ranked.sort((a, b) {
      // Direct URL first
      final aDirect = a.hasDirectUrl ? 1 : 0;
      final bDirect = b.hasDirectUrl ? 1 : 0;
      if (aDirect != bDirect) return bDirect - aDirect;
      // Then speed score
      return b.speedScore.compareTo(a.speedScore);
    });
    return ranked;
  }

  /// Build the backupSourcesJson list for the Madar player
  /// Format matches what _playerFallbackSources() builds in app_shared.dart
  List<Map<String, dynamic>> buildFallbackChain() {
    return getRanked().map((s) => s.toMap()).toList();
  }

  /// JSON string of fallback chain (pass to player as backupSourcesJson)
  String buildFallbackJson() => jsonEncode(buildFallbackChain());

  /// Best server (highest ranked, usable)
  StreamServer? get best {
    final ranked = getRanked();
    return ranked.isEmpty ? null : ranked.first;
  }

  /// All servers count
  int get count => _servers.length;

  /// Clear all
  void clear() => _servers.clear();

  // ── Dedup helper ────────────────────────────────────────────────────────────
  String _normalizeUrl(String url) {
    return url
        .replaceAll(RegExp(r'https?://'), '')
        .replaceAll(RegExp(r'/+$'), '')
        .toLowerCase()
        .trim();
  }

  // ── Quality group ─────────────────────────────────────────────────────────
  /// Group servers by quality label
  Map<String, List<StreamServer>> groupByQuality() {
    final map = <String, List<StreamServer>>{};
    for (final s in getRanked()) {
      map.putIfAbsent(s.quality, () => []).add(s);
    }
    return map;
  }

  /// Get servers for a specific quality (e.g. "1080p", "720p", "HD")
  List<StreamServer> forQuality(String quality) {
    final lower = quality.toLowerCase();
    return getRanked().where((s) => s.quality.toLowerCase() == lower).toList();
  }
}

// ── Static helpers ────────────────────────────────────────────────────────────
extension ServerMergerExt on List<Map<String, dynamic>> {
  /// Merge two server lists and return ranked result
  List<Map<String, dynamic>> mergedWith(List<Map<String, dynamic>> other) {
    final merger = ServerMerger();
    merger.addAll(this);
    merger.addAll(other);
    return merger.buildFallbackChain();
  }
}
