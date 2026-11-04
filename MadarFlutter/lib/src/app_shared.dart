part of '../main.dart';

class NativeBridge {
  static Future<String?> readCache(String key) async {
    try {
      return await _native.invokeMethod<String>('cache.read', {'key': key});
    } catch (_) {
      return null;
    }
  }

  static Future<void> writeCache(String key, String value) async {
    try {
      await _native.invokeMethod<void>('cache.write', {
        'key': key,
        'value': value,
      });
    } catch (_) {}
  }

  static Future<List<Map<String, dynamic>>> list(
    String method, [
    Map<String, Object?> args = const {},
  ]) async {
    final raw =
        await _native.invokeListMethod<dynamic>(method, args) ?? const [];
    return raw
        .whereType<Map>()
        .map((row) => Map<String, dynamic>.from(row))
        .toList(growable: false);
  }

  static Future<Map<String, dynamic>?> map(
    String method, [
    Map<String, Object?> args = const {},
  ]) async {
    final raw = await _native.invokeMapMethod<dynamic, dynamic>(method, args);
    return raw == null ? null : Map<String, dynamic>.from(raw);
  }

  static Future<bool> openPlayer({
    required String url,
    required String title,
    String userAgent = '',
    String referer = '',
    Map<String, String> headers = const {},
    List<Map<String, dynamic>> backupSources = const [],
    bool isLive = false,
  }) async {
    try {
      return await _native.invokeMethod<bool>('player.open', {
            'url': url,
            'title': title,
            'userAgent': userAgent,
            'referer': referer,
            'headers': headers,
            'backupSourcesJson': jsonEncode(backupSources),
            'live': isLive,
          }) ??
          false;
    } catch (_) {
      return false;
    }
  }

  static Future<void> openPlayerStore() async {
    try {
      await _native.invokeMethod<void>('player.store');
    } catch (_) {}
  }

  static Future<bool> notificationsEnabled() async {
    try {
      final status = await map('notifications.status');
      return status?['enabled'] == true;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> openNotificationSettings() async {
    try {
      return await _native.invokeMethod<bool>('notifications.settings') ??
          false;
    } catch (_) {
      return false;
    }
  }
}

String _text(dynamic value) => value == null ? '' : '$value';
List<Map<String, dynamic>> _maps(dynamic value) => value is List
    ? value.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList()
    : <Map<String, dynamic>>[];

Map<String, dynamic> _mergeSeasonBundles(
  Map<String, dynamic>? cached,
  Map<String, dynamic> fresh,
) {
  final oldSeasons = _maps(cached?['seasons']);
  final newSeasons = _maps(fresh['seasons']);
  final merged = <int, Map<String, dynamic>>{};
  final seasonNumbers = <int>{
    ...oldSeasons.map((season) => _integer(season['number'])),
    ...newSeasons.map((season) => _integer(season['number'])),
  }..removeWhere((number) => number <= 0);
  for (final number in seasonNumbers) {
    final old = oldSeasons
        .where((season) => _integer(season['number']) == number)
        .firstOrNull;
    final freshSeason = newSeasons
        .where((season) => _integer(season['number']) == number)
        .firstOrNull;
    final episodesByNumber = <int, Map<String, dynamic>>{};
    final unnumberedByUrl = <String, Map<String, dynamic>>{};
    final oldEpisodes = _maps(old?['episodes']);
    final freshEpisodes = _maps(freshSeason?['episodes']);
    // A season page can return only a partial list (or time out midway). Keep
    // prior episode numbers and let fresh rows refresh metadata for duplicates.
    final episodesToMerge = [...oldEpisodes, ...freshEpisodes];
    for (final episode in episodesToMerge) {
      final episodeNumber = _episodeNumber(episode);
      final url = _normalizedMediaUrl(_text(episode['pageUrl']));
      if (episodeNumber > 0) {
        episodesByNumber[episodeNumber] = {
          ...?episodesByNumber[episodeNumber],
          ...episode,
        };
      } else if (url.isNotEmpty) {
        unnumberedByUrl[url] = {...?unnumberedByUrl[url], ...episode};
      }
    }
    final episodes = [...episodesByNumber.values, ...unnumberedByUrl.values]
      ..sort((a, b) {
        final numberOrder = _episodeNumber(a).compareTo(_episodeNumber(b));
        if (numberOrder != 0) return numberOrder;
        return _text(a['pageUrl']).compareTo(_text(b['pageUrl']));
      });
    merged[number] = {
      ...?old,
      ...?freshSeason,
      'number': number,
      'title': _text(freshSeason?['title']).ifEmpty(_text(old?['title'])),
      'episodes': episodes,
    };
  }
  final partsByUrl = <String, Map<String, dynamic>>{};
  for (final part in [
    ..._maps(cached?['relatedParts']),
    ..._maps(fresh['relatedParts']),
  ]) {
    final url = _normalizedMediaUrl(_text(part['pageUrl']));
    if (url.isNotEmpty) partsByUrl[url] = {...?partsByUrl[url], ...part};
  }
  final parts = partsByUrl.values.toList()
    ..sort((a, b) => _text(a['title']).compareTo(_text(b['title'])));
  return {
    ...fresh,
    'seasons': merged.values.toList()
      ..sort((a, b) => _integer(a['number']).compareTo(_integer(b['number']))),
    'relatedParts': parts,
  };
}

List<Map<String, dynamic>> _orderedEpisodes(dynamic value) {
  final byNumber = <int, Map<String, dynamic>>{};
  final unnumberedByUrl = <String, Map<String, dynamic>>{};
  for (final episode in _maps(value)) {
    final number = _episodeNumber(episode);
    final url = _normalizedMediaUrl(_text(episode['pageUrl']));
    if (number > 0) {
      byNumber[number] = {...?byNumber[number], ...episode};
    } else if (url.isNotEmpty) {
      unnumberedByUrl[url] = {...?unnumberedByUrl[url], ...episode};
    }
  }
  return [...byNumber.values, ...unnumberedByUrl.values]..sort((a, b) {
    final numberOrder = _episodeNumber(a).compareTo(_episodeNumber(b));
    if (numberOrder != 0) return numberOrder;
    return _text(a['pageUrl']).compareTo(_text(b['pageUrl']));
  });
}

/// Requests another season-page lookup only when the episode list is clearly
/// sparse or has gaps. Provider pages do not expose a reliable expected count,
/// so a continuous sequence is treated as provisionally complete.
bool _seasonNeedsEnrichment(dynamic value) {
  final episodeNumbers = _orderedEpisodes(
    value,
  ).map(_episodeNumber).where((number) => number > 0).toSet().toList()..sort();

  if (episodeNumbers.length < 3) return true;
  for (var index = 1; index < episodeNumbers.length; index++) {
    if (episodeNumbers[index] - episodeNumbers[index - 1] > 1) return true;
  }
  return false;
}

String _cleanTitle(String value) {
  const digits = '٠١٢٣٤٥٦٧٨٩';
  for (var i = 0; i < digits.length; i++) {
    value = value.replaceAll(digits[i], '$i');
  }
  return value
      .toLowerCase()
      .replaceAll(RegExp('[أإآ]'), 'ا')
      .replaceAll('ة', 'ه')
      .replaceAll('ى', 'ي')
      .replaceAll(RegExp(r'[^\w\s\u0600-\u06ff]'), ' ')
      .replaceAll(RegExp(r'\s+'), ' ')
      .trim();
}

class MediaItem {
  const MediaItem({
    required this.id,
    required this.title,
    required this.pageUrl,
    required this.isSeries,
    this.poster = '',
    this.year = '',
    this.quality = '',
    this.rating = '',
    this.provider = 'عرب سيد',
    this.overview = '',
    this.parentSeriesTitle = '',
    this.parentSeriesPageUrl = '',
    this.latestEpisodeLabel = '',
  });

  final String id,
      title,
      pageUrl,
      poster,
      year,
      quality,
      rating,
      provider,
      overview;
  final String parentSeriesTitle, parentSeriesPageUrl, latestEpisodeLabel;
  final bool isSeries;

  String get visibleProvider =>
      provider == 'عرب سيد' ? 'المصدر الأساسي' : provider;

  String get key =>
      '${isSeries ? 's' : 'm'}:${_cleanTitle(title)}:${year.trim()}';

  factory MediaItem.fromMap(Map<String, dynamic> row) => MediaItem(
    id: _text(row['id']).isNotEmpty ? _text(row['id']) : _text(row['pageUrl']),
    title: _text(row['title']).isNotEmpty
        ? _text(row['title'])
        : _text(row['arabicTitle']),
    pageUrl: _text(row['pageUrl']),
    isSeries: row['isSeries'] == true,
    poster: _text(row['poster']).isNotEmpty
        ? _text(row['poster'])
        : _text(row['posterUrl']),
    year: _text(row['year']),
    quality: _text(row['quality']),
    rating: _text(row['rating']),
    provider: _text(row['providerName']).isNotEmpty
        ? _text(row['providerName'])
        : _text(row['provider']).ifEmpty('عرب سيد'),
    overview: _text(row['overview']),
    parentSeriesTitle: _text(row['parentSeriesTitle']),
    parentSeriesPageUrl: _text(row['parentSeriesPageUrl']),
    latestEpisodeLabel: _text(row['latestEpisodeLabel']),
  );

  Map<String, dynamic> toMap() => {
    'id': id,
    'title': title,
    'arabicTitle': title,
    'originalTitle': title,
    'pageUrl': pageUrl,
    'isSeries': isSeries,
    'poster': poster,
    'posterUrl': poster,
    'year': year,
    'quality': quality,
    'rating': rating,
    'provider': provider,
    'providerName': provider,
    'overview': overview,
    'parentSeriesTitle': parentSeriesTitle,
    'parentSeriesPageUrl': parentSeriesPageUrl,
    'latestEpisodeLabel': latestEpisodeLabel,
  };
}

extension on String {
  String ifEmpty(String fallback) => isEmpty ? fallback : this;
}
