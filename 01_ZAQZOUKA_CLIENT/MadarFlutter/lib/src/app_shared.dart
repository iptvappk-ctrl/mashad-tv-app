part of '../main.dart';

class AppUpdateInfo {
  final bool hasAppUpdate;
  final bool hasPlayerUpdate;
  final bool isAppDownloaded;
  final bool isPlayerDownloaded;
  final String appVersion;
  final String playerVersion;
  final String appNotes;
  final String playerNotes;
  final String? playerApkUrl;
  final String? playerSha256;
  final double? downloadProgress;
  final bool isDownloading;

  const AppUpdateInfo({
    this.hasAppUpdate = false,
    this.hasPlayerUpdate = false,
    this.isAppDownloaded = false,
    this.isPlayerDownloaded = false,
    this.appVersion = '',
    this.playerVersion = '',
    this.appNotes = '',
    this.playerNotes = '',
    this.playerApkUrl,
    this.playerSha256,
    this.downloadProgress,
    this.isDownloading = false,
  });

  bool get hasAnyUpdate => hasAppUpdate;
  bool get isReadyToInstall => hasAppUpdate && isAppDownloaded;

  AppUpdateInfo copyWith({
    bool? hasAppUpdate,
    bool? hasPlayerUpdate,
    bool? isAppDownloaded,
    bool? isPlayerDownloaded,
    String? appVersion,
    String? playerVersion,
    String? appNotes,
    String? playerNotes,
    String? playerApkUrl,
    String? playerSha256,
    double? downloadProgress,
    bool? isDownloading,
  }) {
    return AppUpdateInfo(
      hasAppUpdate: hasAppUpdate ?? this.hasAppUpdate,
      hasPlayerUpdate: hasPlayerUpdate ?? this.hasPlayerUpdate,
      isAppDownloaded: isAppDownloaded ?? this.isAppDownloaded,
      isPlayerDownloaded: isPlayerDownloaded ?? this.isPlayerDownloaded,
      appVersion: appVersion ?? this.appVersion,
      playerVersion: playerVersion ?? this.playerVersion,
      appNotes: appNotes ?? this.appNotes,
      playerNotes: playerNotes ?? this.playerNotes,
      playerApkUrl: playerApkUrl ?? this.playerApkUrl,
      playerSha256: playerSha256 ?? this.playerSha256,
      downloadProgress: downloadProgress ?? this.downloadProgress,
      isDownloading: isDownloading ?? this.isDownloading,
    );
  }
}

final appUpdateNotifier = ValueNotifier<AppUpdateInfo>(const AppUpdateInfo());

Future<void> handleUpdateAction(BuildContext context) async {
  final info = appUpdateNotifier.value;
  if (info.isReadyToInstall) {
    await showDialog<void>(
      context: context,
      builder: (dialog) => AlertDialog(
        icon: const Icon(Icons.system_update_rounded, color: Color(0xFF16856D), size: 40),
        title: const Text('تحديث زقزوقة جاهز للتثبيت ⚡'),
        content: const Text(
          'تم تنزيل حزمة التحديث بالكامل ومطابقتها. اضغط تثبيت للبدء مباشرة دون الحاجة لإعادة التنزيل.',
          style: TextStyle(height: 1.5),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialog),
            child: const Text('إلغاء'),
          ),
          FilledButton.icon(
            style: FilledButton.styleFrom(backgroundColor: const Color(0xFF16856D)),
            onPressed: () async {
              Navigator.pop(dialog);
              await NativeBridge.installDownloaded(target: 'app');
            },
            icon: const Icon(Icons.install_mobile_rounded),
            label: const Text('تثبيت التحديث الآن ⚡'),
          ),
        ],
      ),
    );
    return;
  }

  if (info.hasAppUpdate) {
    await NativeBridge.promptAppUpdate(context);
  }
}


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

  static Future<void> clearAllData() async {
    try {
      await _native.invokeMethod<void>('cache.clearAll');
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

  static Future<void> openDirectAdUrl(String url) async {
    if (url.trim().isEmpty) return;
    try {
      await _native.invokeMethod<void>('system.openUrl', {'url': url.trim()});
    } catch (_) {}
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

  static Future<bool> openYouTube({String? videoId, String? watchUrl}) async {
    try {
      return await _native.invokeMethod<bool>('youtube.open', {
            'videoId': videoId,
            'watchUrl': watchUrl,
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

  static Future<bool> openExternalPlayer({
    required String url,
    required String title,
  }) async {
    try {
      return await _native.invokeMethod<bool>('player.openExternal', {
            'url': url,
            'title': title,
          }) ??
          false;
    } catch (_) {
      return false;
    }
  }

  static Future<Map<String, dynamic>?> checkPlayerStatus() async {
    try {
      return await _native.invokeMapMethod<String, dynamic>('player.status');
    } catch (_) {
      return null;
    }
  }

  static Future<Map<String, dynamic>?> installDownloaded({String target = 'app'}) async {
    try {
      return await _native.invokeMapMethod<String, dynamic>('updates.installDownloaded', {
        'target': target,
      });
    } catch (_) {
      return null;
    }
  }

  static Future<void> promptAppUpdate(BuildContext context) async {
    var busy = false;
    var hint = '';
    double? downloadProgress;
    StreamSubscription<dynamic>? progressSubscription;
    final info = appUpdateNotifier.value;

    await showDialog<void>(
      context: context,
      barrierDismissible: true,
      builder: (dialogContext) => StatefulBuilder(
        builder: (ctx, setDialogState) {
          return AlertDialog(
            icon: const Icon(Icons.system_update_alt_rounded, color: _brick, size: 40),
            title: Text('تحديث جديد (${info.appVersion.ifEmpty('1.0.64')}) ⚡'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  info.appNotes.trim().isNotEmpty
                      ? info.appNotes.trim()
                      : 'يتوفر إصدار جديد من تطبيق زقزوقة. يدعم التحديث الاستئناف التلقائي في حال انقطاع النت.',
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
                        appUpdateNotifier.value = appUpdateNotifier.value.copyWith(isDownloading: true);
                        try {
                          progressSubscription ??= _updateProgress
                              .receiveBroadcastStream()
                              .listen((event) {
                                if (event is! Map) return;
                                final downloaded = (event['downloadedBytes'] as num?)?.toInt() ?? -1;
                                final total = (event['totalBytes'] as num?)?.toInt() ?? -1;
                                final prog = (downloaded > 0 && total > 0)
                                    ? (downloaded / total).clamp(0.0, 1.0)
                                    : null;
                                appUpdateNotifier.value = appUpdateNotifier.value.copyWith(
                                  isDownloading: true,
                                  downloadProgress: prog,
                                );
                                if (!dialogContext.mounted) return;
                                setDialogState(() {
                                  downloadProgress = prog;
                                });
                              });

                          final res = await _native.invokeMapMethod<String, dynamic>('updates.install');
                          final status = res?['status'] as String? ?? 'download_failed';
                          if (status == 'install_started') {
                            appUpdateNotifier.value = appUpdateNotifier.value.copyWith(
                              isDownloading: false,
                              isAppDownloaded: true,
                            );
                            if (dialogContext.mounted) Navigator.of(dialogContext).pop();
                          } else if (status == 'permission_required') {
                            appUpdateNotifier.value = appUpdateNotifier.value.copyWith(isDownloading: false);
                            setDialogState(() {
                              busy = false;
                              hint = 'يلزم تفعيل إذن تثبيت التطبيقات من الإعدادات.';
                            });
                          } else {
                            appUpdateNotifier.value = appUpdateNotifier.value.copyWith(isDownloading: false);
                            setDialogState(() {
                              busy = false;
                              hint = 'تعذر التنزيل، تأكد من الاتصال وحاول مجدداً.';
                            });
                          }
                        } catch (_) {
                          appUpdateNotifier.value = appUpdateNotifier.value.copyWith(isDownloading: false);
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

  static Future<Map<String, dynamic>?> installPlayer({String? apkUrl, String? sha256}) async {
    try {
      return await _native.invokeMapMethod<String, dynamic>('player.install', {
        'apkUrl': apkUrl ?? 'https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.86-93/zag-player-1.1.37.apk',
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
    // Player updates completely disabled per user requirement
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
    final resolvedRef = _text(server['resolvedReferer']).trim();
    backups.add({
      'url': url,
      'referer': resolvedRef.isNotEmpty
          ? resolvedRef
          : embed.isNotEmpty
          ? embed
          : serverPage.isNotEmpty
          ? serverPage
          : pageReferer,
    });
    if (backups.length >= 12) return backups;
  }

  // Second pass: only if zero verified direct streams resolved, pass verified
  // alive embeds (embedAlive == true) so dead/404 HTML mirrors never stall playback.
  if (backups.isEmpty) {
    final hostCounts = <String, int>{};
    final orderedFallbacks = [
      ...expandedServers.where((s) => s['embedAlive'] == true),
      ...prioritizeServerRowsForProbe(
        expandedServers.where((s) => s['embedAlive'] != true),
      ).take(3),
    ];
    for (final server in orderedFallbacks) {
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

      final hostKey = urlUri.host.toLowerCase().replaceFirst(RegExp(r'^www\.'), '');
      final usedCount = hostCounts[hostKey] ?? 0;
      if (usedCount >= 2) continue;
      hostCounts[hostKey] = usedCount + 1;

      final serverPage = _text(server['sourcePageUrl']).trim();
      final resolvedRef = _text(server['resolvedReferer']).trim();
      backups.add({
        'url': url,
        'referer': resolvedRef.isNotEmpty
            ? resolvedRef
            : embed.isNotEmpty
            ? embed
            : serverPage.isNotEmpty
            ? serverPage
            : pageReferer,
      });
      if (backups.length >= 8) return backups;
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

String _cleanSeriesTitle(String raw) {
  if (raw.trim().isEmpty) return '';
  var text = raw.trim();
  text = text.replaceFirst(RegExp(r'^(?:مشاهدة|مشاهده|تحميل|سلسلة)?\s*(?:مسلسل|فيلم|انمي|أنمي|برنامج)\s*', caseSensitive: false), '');
  text = text.replaceFirst(RegExp(r'\s+(?:الموسم|موسم|season|الجزء|جزء|part|الحلقة|حلقة|episode|ep)\s+.*$', caseSensitive: false), '');
  text = text.replaceFirst(RegExp(r'\s+(?:مترجم|مترجمة|مدبلج|مدبلجة|فيلم|مسلسل|WEB-DL|720p|1080p|HD|FHD|4K).*$', caseSensitive: false), '');
  return text.trim().isNotEmpty ? text.trim() : raw.trim();
}

Future<List<Map<String, dynamic>>> _fetchProviderBackupServerRows({
  required String title,
  required String pageUrl,
  required bool isSeries,
  String year = '',
  int? episodeNumber,
  int? seasonNumber,
}) async {
  final cleanTitle = (isSeries ? _cleanSeriesTitle(title) : title).trim();
  final cleanUrl = pageUrl.trim();
  if (cleanTitle.isEmpty || !cleanUrl.startsWith('http')) return const [];
  try {
    final rows = await _nativeScraper.findServerCandidatesAcrossProviders(
      title: cleanTitle,
      pageUrl: cleanUrl,
      isSeries: isSeries,
      year: year.trim(),
      episodeNumber: episodeNumber,
      seasonNumber: seasonNumber,
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

int _serverHostPriority(Map<String, dynamic> row) {
  final direct = _text(row['directStreamUrl']).trim().toLowerCase();
  if (direct.endsWith('.m3u8') || direct.endsWith('.mp4')) return 0;
  final u = (_text(row['embedUrl']).isNotEmpty ? _text(row['embedUrl']) : direct).toLowerCase();
  if (u.contains('vids.php') || u.contains('player-gateway')) return 1;
  if (u.contains('vidara.') || u.contains('sugbaioug.')) return 2;
  if (u.contains('mixdrop') || u.contains('mxdrop') || u.contains('vidtube')) return 3;
  if (u.contains('vidmoly') || u.contains('voe.') || u.contains('uqload')) return 4;
  if (u.contains('luluvdo') || u.contains('vidplayer') || u.contains('govid.live')) return 10;
  if (u.contains('streamtape') || u.contains('govid') || u.contains('ok.ru')) return 5;
  if (u.contains('dood') || u.contains('ds2play') || u.contains('playmogo') || u.contains('d000d')) return 9;
  if (u.contains('byse')) return 8;
  return 6;
}

/// Interleave source pages before playback probing so one site with many
/// mirrors cannot consume the probe window before other configured sources.
/// Within each source, direct/fast-extractable hosts are probed first.
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

  for (final group in groups.values) {
    group.sort((a, b) => _serverHostPriority(a).compareTo(_serverHostPriority(b)));
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

Future<Map<String, String>?> _resolveServerStreamDetail(
  Map<String, dynamic> server, {
  String referer = '',
}) async {
  final direct = _text(server['directStreamUrl']).trim();
  final embed = _text(server['embedUrl']).trim();
  final candidateUrl = _isUsablePlaybackUrl(direct) ? direct : embed;
  final candidateUri = Uri.tryParse(candidateUrl);
  if (candidateUrl.isEmpty ||
      candidateUri == null ||
      candidateUri.host.isEmpty ||
      (candidateUri.scheme != 'http' && candidateUri.scheme != 'https')) {
    return null;
  }
  final effectiveReferer = _text(server['sourcePageUrl']).ifEmpty(referer);
  try {
    final raw = await _native
        .invokeMapMethod<dynamic, dynamic>('media.resolve.detail', {
          'url': candidateUrl,
          'referer': effectiveReferer,
        })
        .timeout(const Duration(seconds: 7));
    if (raw == null) return null;
    final resolvedUrl = _text(raw['url']).trim();
    if (!_isUsablePlaybackUrl(resolvedUrl)) return null;
    return {
      'url': resolvedUrl,
      'referer': _text(raw['referer']).trim(),
    };
  } catch (_) {
    return null;
  }
}

/// Shared parallel server probe used by both Movie DetailPage and EpisodePage.
Future<Map<String, dynamic>?> _findBestWorkingServer({
  required List<Map<String, dynamic>> servers,
  required String referer,
  Future<List<Map<String, dynamic>>>? retryExtraFuture,
  bool allowRetryExtra = true,
}) async {
  final candidateRows = _mergeServerRows(servers);
  if (candidateRows.isEmpty) return null;

  final verified = <Map<String, dynamic>>[];
  Map<String, dynamic>? firstEmbedFallback;
  final firstPlayable = Completer<Map<String, dynamic>?>();
  var completed = 0;
  final probes = <Future<void>>[];
  final probeCandidates = prioritizeServerRowsForProbe(candidateRows)
      .take(20)
      .toList(growable: false);

  for (final server in probeCandidates) {
    probes.add(() async {
      try {
        final direct = _text(server['directStreamUrl']).trim();
        final embed = _text(server['embedUrl']).trim();
        final candidateUrl = _isUsablePlaybackUrl(direct) ? direct : embed;
        if (!_isUsablePlaybackUrl(candidateUrl)) return;
        final sourceRef = _text(server['sourcePageUrl']).ifEmpty(referer);
        final resolvedMeta = await _resolveServerStreamDetail(
          {...server, 'directStreamUrl': '', 'embedUrl': candidateUrl},
          referer: sourceRef,
        );
        final resolvedUrl = _text(resolvedMeta?['url']).trim();
        final resolvedRef = _text(resolvedMeta?['referer']).trim();
        final isVerifiedDirect = _isUsablePlaybackUrl(resolvedUrl);
        final streamUrl = isVerifiedDirect ? resolvedUrl : candidateUrl;
        final row = <String, dynamic>{
          ...server,
          'directStreamUrl': streamUrl,
          if (resolvedRef.isNotEmpty) 'resolvedReferer': resolvedRef,
          'playableVerified': isVerifiedDirect,
        };
        if (isVerifiedDirect) {
          verified.add(row);
          if (!firstPlayable.isCompleted) firstPlayable.complete(row);
        } else if (!firstPlayable.isCompleted &&
            _isUsablePlaybackUrl(candidateUrl)) {
          final embedAlive = await _probePlayableEmbedUrl(
            candidateUrl,
            referer: sourceRef,
          );
          if (embedAlive) {
            firstEmbedFallback ??= {...row, 'embedAlive': true};
          }
        }
      } catch (_) {
        // Other candidate servers continue probing concurrently.
      } finally {
        completed++;
        if (completed == probeCandidates.length && !firstPlayable.isCompleted) {
          firstPlayable.complete(firstEmbedFallback);
        }
      }
    }());
  }

  final selected = await firstPlayable.future.timeout(
    const Duration(seconds: 8),
    onTimeout: () => verified.isNotEmpty ? verified.first : firstEmbedFallback,
  );
  if (selected == null) {
    if (allowRetryExtra && retryExtraFuture != null) {
      try {
        final discovered = _mergeServerRows([
          ...await retryExtraFuture.timeout(const Duration(seconds: 4)),
          ...candidateRows,
        ]);
        if (discovered.length > candidateRows.length) {
          return await _findBestWorkingServer(
            servers: discovered,
            referer: referer,
            allowRetryExtra: false,
          );
        }
      } catch (_) {}
    }
    if (candidateRows.isNotEmpty) {
      final fallback = prioritizeServerRowsForProbe(candidateRows).firstWhere(
        (s) =>
            _isUsablePlaybackUrl(_text(s['directStreamUrl']).trim()) ||
            _isUsablePlaybackUrl(_text(s['embedUrl']).trim()),
        orElse: () => candidateRows.first,
      );
      return {
        ...fallback,
        'playableVerified': false,
        'fallbackServers': candidateRows,
      };
    }
    return null;
  }

  if (selected['playableVerified'] == true) {
    // Give sibling fast resolvers up to 1.5s to return additional verified streams
    // (stopping immediately once 4 backups or all probes finish) so ZAG Player has
    // instant pre-verified direct failovers on hand.
    if (verified.length < 4 && completed < probeCandidates.length) {
      await Future.any([
        Future.wait(probes),
        Future<void>(() async {
          for (var i = 0; i < 15; i++) {
            await Future<void>.delayed(const Duration(milliseconds: 100));
            if (verified.length >= 4 || completed >= probeCandidates.length) break;
          }
        }),
      ]);
    }
  }
  return {
    ...selected,
    'fallbackServers': _mergeServerRows([...verified, ...candidateRows]),
  };
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
        final existingEp = episodesByNumber[episodeNumber];
        final curUrl = _text(episode['pageUrl']).trim();
        final oldUrl = _text(existingEp?['pageUrl']).trim();
        episodesByNumber[episodeNumber] = {
          ...?existingEp,
          ...episode,
          if (curUrl.isEmpty && oldUrl.isNotEmpty) 'pageUrl': oldUrl,
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
      'pageUrl': _text(freshSeason?['pageUrl']).ifEmpty(_text(old?['pageUrl'])),
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

  // If we have no episodes at all, enrichment is needed
  if (episodeNumbers.isEmpty) return true;

  // FIX: If episodes exist (even 1 or 2), treat as complete unless there are gaps.
  // Previously: `< 3` caused infinite sync loop for newly-released series with 1-2 eps.
  // Now: only flag as needing enrichment if there's a gap in the sequence.
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
