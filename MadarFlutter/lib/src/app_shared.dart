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

  static Future<Map<String, dynamic>?> checkPlayerStatus() async {
    try {
      return await _native.invokeMapMethod<String, dynamic>('player.status');
    } catch (_) {
      return null;
    }
  }

  static Future<Map<String, dynamic>?> installPlayer({String? apkUrl, String? sha256}) async {
    try {
      return await _native.invokeMapMethod<String, dynamic>('player.install', {
        if (apkUrl != null) 'apkUrl': apkUrl,
        if (sha256 != null) 'sha256': sha256,
      });
    } catch (_) {
      return null;
    }
  }

  static Future<void> promptPlayerUpdate(
    BuildContext context, {
    String? apkUrl,
    String? sha256,
    String? versionName,
    String? releaseNotes,
  }) async {
    var busy = false;
    var hint = '';
    double? downloadProgress;
    StreamSubscription<dynamic>? progressSubscription;

    await showDialog<void>(
      context: context,
      barrierDismissible: true,
      builder: (dialogContext) => StatefulBuilder(
        builder: (ctx, setDialogState) {
          return AlertDialog(
            icon: const Icon(Icons.play_circle_fill_rounded, color: _brick, size: 40),
            title: const Text('مشغل زقزوقة (نسخة الجودات الكاملة)'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  releaseNotes?.trim().isNotEmpty == true
                      ? releaseNotes!.trim()
                      : 'يتوفر تحديث جديد لمشغل زقزوقة مع تحكم كامل بجميع الجودات (أقصى توفير، متوسط، أعلى جودة، وتلقائي). اضغط تحديث لتحميله وتثبيته مباشرة داخل التطبيق.',
                  style: const TextStyle(fontSize: 13, height: 1.5),
                ),
                if (hint.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Text(hint, style: const TextStyle(color: _brick, fontSize: 12)),
                ],
                if (busy && downloadProgress != null) ...[
                  const SizedBox(height: 14),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: LinearProgressIndicator(
                      value: downloadProgress,
                      backgroundColor: _card,
                      valueColor: const AlwaysStoppedAnimation<Color>(_brick),
                      minHeight: 8,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Center(
                    child: Text(
                      'جاري التحميل... ${(downloadProgress! * 100).toInt()}%',
                      style: const TextStyle(fontSize: 12, color: _muted),
                    ),
                  ),
                ],
              ],
            ),
            actions: [
              TextButton(
                onPressed: busy ? null : () => Navigator.of(dialogContext).pop(),
                child: const Text('إلغاء'),
              ),
              FilledButton.icon(
                onPressed: busy
                    ? null
                    : () async {
                        setDialogState(() {
                          busy = true;
                          hint = '';
                        });
                        try {
                          progressSubscription ??= _updateProgress
                              .receiveBroadcastStream()
                              .listen((event) {
                                if (event is! Map) return;
                                final downloaded = (event['downloadedBytes'] as num?)?.toInt() ?? -1;
                                final total = (event['totalBytes'] as num?)?.toInt() ?? -1;
                                if (!dialogContext.mounted) return;
                                setDialogState(() {
                                  downloadProgress = (downloaded > 0 && total > 0)
                                      ? (downloaded / total).clamp(0.0, 1.0)
                                      : null;
                                });
                              });

                          final res = await installPlayer(apkUrl: apkUrl, sha256: sha256);
                          final status = res?['status'] as String? ?? 'download_failed';
                          if (status == 'install_started') {
                            if (dialogContext.mounted) Navigator.of(dialogContext).pop();
                          } else if (status == 'permission_required') {
                            setDialogState(() {
                              busy = false;
                              hint = 'يلزم السماح بتثبيت التطبيقات من هذا المصدر في الإعدادات.';
                            });
                          } else {
                            setDialogState(() {
                              busy = false;
                              hint = 'تعذر التحميل، تأكد من الاتصال وحاول مجدداً.';
                            });
                          }
                        } catch (_) {
                          setDialogState(() {
                            busy = false;
                            hint = 'حدث خطأ أثناء تنزيل التحديث.';
                          });
                        }
                      },
                icon: const Icon(Icons.download_rounded),
                label: const Text('تحميل وتثبيت التحديث الآن ⚡'),
              ),
            ],
          );
        },
      ),
    );
    await progressSubscription?.cancel();
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

  static Future<bool> requestNotificationPermission() async {
    try {
      return await _native.invokeMethod<bool>('notifications.request') ?? false;
    } catch (_) {
      return false;
    }
  }
}

List<Map<String, dynamic>> _playerFallbackSources({
  required List<Map<String, dynamic>> servers,
  required Map<String, dynamic> current,
  required String pageReferer,
}) {
  final currentDirect = _text(current['directStreamUrl']).trim();
  final currentUrl = _isUsablePlaybackUrl(currentDirect) ? currentDirect : '';
  final seen = <String>{currentUrl};
  final backups = <Map<String, dynamic>>[];

  // Rows with the same visible server label can point to different mirrors
  // (often because the source rotates query tokens). Keep one row in the UI,
  // but still pass every underlying URL to the player as an automatic backup.
  final expandedServers = <Map<String, dynamic>>[];
  for (final server in servers) {
    expandedServers.add(server);
    expandedServers.addAll(_maps(server['fallbackServers']));
  }

  // Only pass streams that were resolved and checked by the native media
  // verifier. An embed/page URL can return HTML, ads, or a 404; treating it as
  // a video backup caused the player to open a blank/error page.
  for (final server in expandedServers) {
    if (server['playableVerified'] != true) continue;
    final url = _text(server['directStreamUrl']).trim();
    final urlUri = Uri.tryParse(url);
    if (url.isEmpty ||
        urlUri == null ||
        urlUri.host.isEmpty ||
        (urlUri.scheme != 'http' && urlUri.scheme != 'https') ||
        !_isUsablePlaybackUrl(url) ||
        !seen.add(url)) {
      continue;
    }

    final embed = _text(server['embedUrl']).trim();
    final serverPage = _text(server['sourcePageUrl']).trim();
    backups.add({
      'url': url,
      'referer': embed.isNotEmpty
          ? embed
          : serverPage.isNotEmpty
          ? serverPage
          : pageReferer,
    });
    if (backups.length >= 48) return backups;
  }

  // Second pass: if verified backups are scarce, include remaining server candidates
  // so the player always has multiple fallbacks rather than a dead error screen.
  if (backups.length < 4) {
    for (final server in expandedServers) {
      final direct = _text(server['directStreamUrl']).trim();
      final embed = _text(server['embedUrl']).trim();
      final url = direct.isNotEmpty ? direct : embed;
      final urlUri = Uri.tryParse(url);
      if (url.isEmpty ||
          urlUri == null ||
          urlUri.host.isEmpty ||
          (urlUri.scheme != 'http' && urlUri.scheme != 'https') ||
          !_isUsablePlaybackUrl(url) ||
          !seen.add(url)) {
        continue;
      }

      final serverPage = _text(server['sourcePageUrl']).trim();
      backups.add({
        'url': url,
        'referer': embed.isNotEmpty
            ? embed
            : serverPage.isNotEmpty
            ? serverPage
            : pageReferer,
      });
      if (backups.length >= 48) return backups;
    }
  }

  return backups;
}

String _serverCacheKey(String pageUrl) => 'episode:servers:v6:$pageUrl';

List<Map<String, dynamic>> _tagServerRows(
  List<Map<String, dynamic>> rows,
  String sourcePageUrl,
  String provider,
) => rows
    .map(
      (row) => {
        ...row,
        'sourcePageUrl': _text(row['sourcePageUrl']).ifEmpty(sourcePageUrl),
        'sourceProviderName': _text(row['sourceProviderName'])
            .ifEmpty(provider),
      },
    )
    .toList(growable: false);

Future<List<Map<String, dynamic>>> _fetchServerRowsForPage(
  String pageUrl,
  String provider,
) async {
  final url = pageUrl.trim();
  if (!url.startsWith('http')) return const [];
  final key = _serverCacheKey(url);
  List<Map<String, dynamic>> cachedRows = const [];
  try {
    final encoded = await NativeBridge.readCache(key);
    if (encoded != null) {
      cachedRows = _tagServerRows(_maps(jsonDecode(encoded)), url, provider);
    }
  } catch (_) {}
  try {
    final rows = await _nativeScraper.fetchServerCandidates(
      pageUrl: url,
      provider: provider,
    );
    if (rows.isNotEmpty) {
      final tagged = _tagServerRows(rows, url, provider);
      unawaited(NativeBridge.writeCache(key, jsonEncode(tagged)));
      return tagged;
    }
  } catch (_) {}
  return cachedRows;
}

Future<List<Map<String, dynamic>>> _fetchAlternativeServerRows({
  required List<String> pageUrls,
  required String provider,
  int maximumPages = 24,
}) async {
  final seen = <String>{};
  final candidates = pageUrls
      .map((url) => url.trim())
      .where((url) => url.startsWith('http'))
      .where(
        (url) => seen.add(url.replaceFirst(RegExp(r'/+$'), '').toLowerCase()),
      )
      .take(maximumPages)
      .toList(growable: false);
  if (candidates.isEmpty) return const [];
  final results = await Future.wait(
    candidates.map((url) => _fetchServerRowsForPage(url, provider)),
    eagerError: false,
  );
  return _mergeServerRows(results.expand((rows) => rows));
}

Future<List<Map<String, dynamic>>> _fetchProviderBackupServerRows({
  required String title,
  required String pageUrl,
  required bool isSeries,
  String year = '',
}) async {
  final cleanTitle = title.trim();
  final cleanUrl = pageUrl.trim();
  if (cleanTitle.isEmpty || !cleanUrl.startsWith('http')) return const [];
  try {
    final rows = await _nativeScraper.findServerCandidatesAcrossProviders(
      title: cleanTitle,
      pageUrl: cleanUrl,
      isSeries: isSeries,
      year: year.trim(),
    );
    return _mergeServerRows(_tagServerRows(rows, cleanUrl, 'مصدر بديل'));
  } catch (_) {
    return const [];
  }
}

List<Map<String, dynamic>> _mergeServerRows(
  Iterable<Map<String, dynamic>> rows,
) {
  final groups = <String, List<Map<String, dynamic>>>{};
  final seenPlaybackUrls = <String>{};
  final flattenedRows = <Map<String, dynamic>>[];
  for (final row in rows) {
    flattenedRows.add(row);
    flattenedRows.addAll(_maps(row['fallbackServers']));
  }

  for (final row in flattenedRows) {
    final direct = _text(row['directStreamUrl']).trim();
    final embed = _text(row['embedUrl']).trim();
    final playback = _isUsablePlaybackUrl(direct) ? direct : embed;
    // قبل أي URL صالح — سواء direct stream أو embed page (للـ WebView)
    final playbackUri = Uri.tryParse(playback);
    if (playback.isEmpty ||
        playbackUri == null ||
        playbackUri.host.isEmpty ||
        (playbackUri.scheme != 'http' && playbackUri.scheme != 'https'))
      continue;
    final urlKey = playback.replaceFirst(RegExp(r'/+$'), '').toLowerCase();
    if (!seenPlaybackUrls.add(urlKey)) continue;

    final rawName = _text(row['name']).trim();
    final normalizedName = rawName
        .toLowerCase()
        .replaceAll('⚡', ' ')
        .replaceAll(RegExp(r'سيرفر|server'), ' ')
        .replaceAll(RegExp(r'فائق\s*السرعة|سريع|سريعة|fast'), ' ')
        .replaceAll(RegExp(r'[^a-z0-9\u0600-\u06FF]+'), ' ')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
    final pageHost =
        Uri.tryParse(_text(row['sourcePageUrl']).trim())?.host
            .toLowerCase()
            .replaceFirst(RegExp(r'^www\.'), '') ??
        '';
    final playbackHost =
        Uri.tryParse(playback)?.host
            .toLowerCase()
            .replaceFirst(RegExp(r'^www\.'), '') ??
        '';
    final identity = normalizedName.isEmpty
        ? 'url:$urlKey'
        : '${pageHost.isEmpty ? playbackHost : pageHost}|$normalizedName';
    groups.putIfAbsent(identity, () => <Map<String, dynamic>>[]).add(row);
  }

  return groups.values
      .map((group) {
        final primary = Map<String, dynamic>.from(group.first);
        if (group.length > 1) {
          primary['fallbackServers'] = group.skip(1).toList(growable: false);
        } else {
          primary.remove('fallbackServers');
        }
        return primary;
      })
      .toList(growable: false);
}

/// Interleave source pages before playback probing so one site with many
/// mirrors cannot consume the probe window before other configured sources.
List<Map<String, dynamic>> prioritizeServerRowsForProbe(
  Iterable<Map<String, dynamic>> rows,
) {
  final groups = <String, List<Map<String, dynamic>>>{};
  final order = <String>[];
  for (final row in rows) {
    final source = _text(row['sourceProviderName']).trim();
    final page = _text(row['sourcePageUrl']).trim();
    final host = Uri.tryParse(page)?.host.toLowerCase() ?? '';
    final key = source.isNotEmpty ? source.toLowerCase() : host;
    final groupKey = key.isEmpty ? 'unknown' : key;
    groups
        .putIfAbsent(groupKey, () {
          order.add(groupKey);
          return <Map<String, dynamic>>[];
        })
        .add(row);
  }

  final prioritized = <Map<String, dynamic>>[];
  var index = 0;
  while (true) {
    var added = false;
    for (final key in order) {
      final group = groups[key]!;
      if (index < group.length) {
        prioritized.add(group[index]);
        added = true;
      }
    }
    if (!added) break;
    index++;
  }
  return prioritized;
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
    this.alternatePageUrls = const [],
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
  final List<String> alternatePageUrls;
  final bool isSeries;

  MediaItem copyWith({List<String>? alternatePageUrls}) => MediaItem(
    id: id,
    title: title,
    pageUrl: pageUrl,
    isSeries: isSeries,
    poster: poster,
    year: year,
    quality: quality,
    rating: rating,
    provider: provider,
    overview: overview,
    parentSeriesTitle: parentSeriesTitle,
    parentSeriesPageUrl: parentSeriesPageUrl,
    latestEpisodeLabel: latestEpisodeLabel,
    alternatePageUrls: alternatePageUrls ?? this.alternatePageUrls,
  );

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
    alternatePageUrls: row['alternatePageUrls'] is List
        ? (row['alternatePageUrls'] as List)
              .map((value) => _text(value).trim())
              .where((value) => value.startsWith('http'))
              .toSet()
              .toList(growable: false)
        : const [],
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
    'alternatePageUrls': alternatePageUrls,
  };
}

extension on String {
  String ifEmpty(String fallback) => isEmpty ? fallback : this;
}
