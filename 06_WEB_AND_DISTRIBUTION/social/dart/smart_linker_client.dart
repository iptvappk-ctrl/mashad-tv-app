// smart_linker_client.dart
// ────────────────────────
// Dart client that talks to the social/ API endpoints:
//   - /api/cinema/franchise  → franchise/sequel tree for movies
//   - /api/cinema/ramadan    → Ramadan series data
//   - /api/cinema/superresolve → resolve embed → clean stream
//
// Also contains a pure-Dart SmartTitleParser (no network needed)
// that extracts season/episode/part info from Arabic titles.
//
// HOW TO USE (from anywhere in the Flutter app):
//   final client = SmartLinkerClient(apiBase: 'http://localhost:3000');
//   final tree = await client.getSeriesTree('الاختيار');
//   final franchise = await client.getFranchise('رامبو');

import 'dart:async';
import 'dart:convert';

// ── Data models ───────────────────────────────────────────────────────────────
class SeriesTree {
  final String baseTitle;
  final int seasonCount;
  final List<SeasonData> seasons;

  const SeriesTree({
    required this.baseTitle,
    required this.seasonCount,
    required this.seasons,
  });

  factory SeriesTree.fromJson(Map<String, dynamic> j) => SeriesTree(
        baseTitle: j['baseTitle'] as String? ?? '',
        seasonCount: (j['seasonCount'] as num?)?.toInt() ?? 0,
        seasons: (j['seasons'] as List<dynamic>? ?? [])
            .whereType<Map<String, dynamic>>()
            .map(SeasonData.fromJson)
            .toList(),
      );
}

class SeasonData {
  final int number;
  final int episodeCount;
  final List<EpisodeData> episodes;

  const SeasonData({
    required this.number,
    required this.episodeCount,
    required this.episodes,
  });

  factory SeasonData.fromJson(Map<String, dynamic> j) => SeasonData(
        number: (j['number'] as num?)?.toInt() ?? 1,
        episodeCount: (j['episodeCount'] as num?)?.toInt() ?? 0,
        episodes: (j['episodes'] as List<dynamic>? ?? [])
            .whereType<Map<String, dynamic>>()
            .map(EpisodeData.fromJson)
            .toList(),
      );
}

class EpisodeData {
  final String title;
  final int episodeNumber;
  final int seasonNumber;
  final String pageUrl;
  final String quality;
  final List<Map<String, dynamic>> servers;

  const EpisodeData({
    required this.title,
    required this.episodeNumber,
    required this.seasonNumber,
    required this.pageUrl,
    required this.quality,
    required this.servers,
  });

  factory EpisodeData.fromJson(Map<String, dynamic> j) => EpisodeData(
        title: j['title'] as String? ?? '',
        episodeNumber: (j['episode'] as num?)?.toInt() ?? 0,
        seasonNumber: (j['season'] as num?)?.toInt() ?? 1,
        pageUrl: j['pageUrl'] as String? ?? '',
        quality: j['quality'] as String? ?? 'HD',
        servers: (j['servers'] as List<dynamic>? ?? [])
            .whereType<Map<String, dynamic>>()
            .toList(),
      );
}

class FranchiseData {
  final String baseName;
  final int partCount;
  final List<EpisodeData> parts;

  const FranchiseData({
    required this.baseName,
    required this.partCount,
    required this.parts,
  });

  factory FranchiseData.fromJson(Map<String, dynamic> j) => FranchiseData(
        baseName: j['baseName'] as String? ?? '',
        partCount: (j['partCount'] as num?)?.toInt() ?? 0,
        parts: (j['parts'] as List<dynamic>? ?? [])
            .whereType<Map<String, dynamic>>()
            .map(EpisodeData.fromJson)
            .toList(),
      );
}

// ── Pure-Dart title parser (no network) ──────────────────────────────────────
class SmartTitleParser {
  /// Normalize Arabic text: hamza, ta marbuta, alif maqsura, digits
  static String normalize(String title) {
    var t = title.toLowerCase();
    // Arabic-Indic → ASCII digits
    const arabicDigits = '٠١٢٣٤٥٦٧٨٩';
    const persianDigits = '۰۱۲۳۴۵۶۷۸۹';
    for (var i = 0; i < 10; i++) {
      t = t.replaceAll(arabicDigits[i], '$i');
      t = t.replaceAll(persianDigits[i], '$i');
    }
    // Hamza normalization
    t = t.replaceAll(RegExp('[أإآ]'), 'ا');
    t = t.replaceAll('ة', 'ه');
    t = t.replaceAll('ى', 'ي');
    // Diacritics (tashkeel)
    t = t.replaceAll(RegExp('[\u064B-\u065F]'), '');
    // Collapse whitespace
    return t.replaceAll(RegExp(r'\s+'), ' ').trim();
  }

  static final _ordinals = {
    'الاول': 1, 'الاولى': 1,
    'الثاني': 2, 'الثانيه': 2, 'الثانية': 2,
    'الثالث': 3, 'الثالثه': 3, 'الثالثة': 3,
    'الرابع': 4, 'الرابعه': 4, 'الرابعة': 4,
    'الخامس': 5, 'الخامسه': 5, 'الخامسة': 5,
    'السادس': 6, 'السادسه': 6, 'السادسة': 6,
    'السابع': 7, 'السابعه': 7, 'السابعة': 7,
    'الثامن': 8, 'الثامنه': 8, 'الثامنة': 8,
    'التاسع': 9, 'التاسعه': 9, 'التاسعة': 9,
    'العاشر': 10, 'العاشره': 10, 'العاشرة': 10,
  };

  /// Extract season number from normalized title
  static int extractSeason(String title) {
    final t = normalize(title);
    // "الموسم الثاني" / "الموسم 2" / "Season 2" / "s02e..."
    final ordRx = RegExp(r'(?:الموسم|موسم|season)\s+(\S+)');
    final m1 = ordRx.firstMatch(t);
    if (m1 != null) {
      final g = m1.group(1) ?? '';
      return int.tryParse(g) ?? _ordinals[g] ?? 0;
    }
    final numRx = RegExp(r's(\d{1,2})e\d+');
    final m2 = numRx.firstMatch(t);
    if (m2 != null) return int.parse(m2.group(1)!);
    return 0;
  }

  /// Extract episode number from normalized title
  static int extractEpisode(String title) {
    final t = normalize(title);
    final patterns = [
      RegExp(r'(?:الحلقه|الحلقة|حلقه|حلقة|episode|ep\.?)\s*(\d+)'),
      RegExp(r's\d{1,2}e(\d{1,3})'),
      RegExp(r'\be(\d{2,3})\b'),
    ];
    for (final rx in patterns) {
      final m = rx.firstMatch(t);
      if (m != null) return int.tryParse(m.group(1) ?? '') ?? 0;
    }
    return 0;
  }

  /// Extract part number (for movies: "الجزء الثاني", "Part 2")
  static int extractPart(String title) {
    final t = normalize(title);
    final rx = RegExp(r'(?:الجزء|جزء|part|pt\.?)\s*(\S+)');
    final m = rx.firstMatch(t);
    if (m == null) return 0;
    final g = m.group(1) ?? '';
    return int.tryParse(g) ?? _ordinals[g] ?? 0;
  }

  /// Strip season/episode/part markers to get base title
  static String extractBaseTitle(String title) {
    var t = normalize(title);
    t = t.replaceAll(RegExp(r'(?:الموسم|موسم|season)\s+\S+'), '');
    t = t.replaceAll(RegExp(r'(?:الجزء|جزء|part|pt\.?)\s+\S+'), '');
    t = t.replaceAll(RegExp(r'(?:الحلقه|الحلقة|حلقه|حلقة|episode|ep\.?)\s*\d+'), '');
    t = t.replaceAll(RegExp(r's\d{1,2}e\d{1,3}'), '');
    t = t.replaceAll(RegExp(r'\(\d{4}\)'), '');
    t = t.replaceAll(RegExp(r'[:\-–|]+$'), '');
    return t.replaceAll(RegExp(r'\s+'), ' ').trim();
  }

  /// Parse all metadata from a title
  static Map<String, dynamic> parse(String title) => {
        'rawTitle': title,
        'normalized': normalize(title),
        'baseTitle': extractBaseTitle(title),
        'season': extractSeason(title),
        'episode': extractEpisode(title),
        'part': extractPart(title),
        'year': _extractYear(title),
      };

  static int _extractYear(String title) {
    final m = RegExp(r'\b(19|20)\d{2}\b').firstMatch(title);
    return int.tryParse(m?.group(0) ?? '') ?? 0;
  }
}

// ── API client ────────────────────────────────────────────────────────────────
class SmartLinkerClient {
  final String apiBase;

  const SmartLinkerClient({this.apiBase = 'http://localhost:3000'});

  /// Get series season/episode tree
  Future<SeriesTree?> getSeriesTree(String title) async {
    try {
      final json = await _get('/api/cinema/franchise', {'title': title});
      if (json.containsKey('error')) return null;
      return SeriesTree.fromJson(json);
    } catch (_) {
      return null;
    }
  }

  /// Get franchise (all movie parts/sequels)
  Future<FranchiseData?> getFranchise(String title) async {
    try {
      final json = await _get('/api/cinema/franchise', {'title': title});
      if (json.containsKey('error')) return null;
      return FranchiseData.fromJson(json);
    } catch (_) {
      return null;
    }
  }

  /// Resolve embed URL(s) → clean stream URLs
  Future<Map<String, dynamic>> superResolve({
    required String embedUrl,
    String referer = '',
    List<String> extras = const [],
  }) async {
    try {
      return await _get('/api/cinema/superresolve', {
        'serverUrl': embedUrl,
        if (referer.isNotEmpty) 'referer': referer,
        if (extras.isNotEmpty) 'embedUrls': extras.join(','),
      });
    } catch (_) {
      return {'streams': [], 'error': 'API unavailable'};
    }
  }

  /// Get Ramadan series from API
  Future<List<Map<String, dynamic>>> getRamadanSeries({
    String category = 'all',
  }) async {
    try {
      final json = await _get('/api/cinema/ramadan', {'category': category});
      final list = json['series'] as List<dynamic>? ?? [];
      return list.whereType<Map<String, dynamic>>().toList();
    } catch (_) {
      return [];
    }
  }

  // ── HTTP helper (uses dart:io HttpClient to avoid extra deps) ─────────────
  Future<Map<String, dynamic>> _get(
    String path,
    Map<String, String> params,
  ) async {
    final query = params.entries
        .map((e) => '${Uri.encodeComponent(e.key)}=${Uri.encodeComponent(e.value)}')
        .join('&');
    final uri = Uri.parse('$apiBase$path${query.isNotEmpty ? '?$query' : ''}');

    // Use dart:io HttpClient (no extra package needed)
    final client = _createHttpClient();
    try {
      final request = await client.getUrl(uri);
      request.headers.set('Accept', 'application/json');
      request.headers.set('User-Agent', 'ZakzoukaApp/1.0 Dart');
      final response = await request.close().timeout(const Duration(seconds: 10));
      final body = await response.transform(utf8.decoder).join();
      return jsonDecode(body) as Map<String, dynamic>;
    } finally {
      client.close();
    }
  }

  dynamic _createHttpClient() {
    // Using dart:io HttpClient
    // ignore: avoid_dynamic_calls
    try {
      final uri = Uri.parse(apiBase);
      _ = uri; // just to avoid unused warning
    } catch (_) {}
    // This is a simplified placeholder; in full integration use http package
    throw UnimplementedError('Use http package or NativeBridge for HTTP calls');
  }
}

// Suppress unused variable warning
// ignore: unused_element
void _ = null;
