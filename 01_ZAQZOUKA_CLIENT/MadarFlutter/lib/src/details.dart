part of '../main.dart';

class FavoritesPage extends StatelessWidget {
  const FavoritesPage({
    super.key,
    required this.items,
    required this.favoriteUrls,
    required this.onOpen,
    required this.onToggle,
  });
  final List<MediaItem> items;
  final Set<String> favoriteUrls;
  final ValueChanged<MediaItem> onOpen, onToggle;
  @override
  Widget build(BuildContext context) {
    final favorites = items
        .where((item) => favoriteUrls.contains(item.pageUrl))
        .toList();
    return Column(
      children: [
        const Padding(
          padding: EdgeInsets.fromLTRB(16, 18, 16, 12),
          child: Align(
            alignment: Alignment.centerRight,
            child: Text(
              'المفضلة',
              style: TextStyle(
                color: _ink,
                fontSize: 23,
                fontWeight: FontWeight.w900,
              ),
            ),
          ),
        ),
        Expanded(
          child: favorites.isEmpty
              ? const Center(
                  child: Text(
                    'أضف الأفلام والمسلسلات إلى المفضلة لتجدها هنا',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: _muted),
                  ),
                )
              : GridView.builder(
                  padding: const EdgeInsets.fromLTRB(14, 4, 14, 20),
                  itemCount: favorites.length,
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 3,
                    crossAxisSpacing: 10,
                    mainAxisSpacing: 14,
                    childAspectRatio: .50,
                  ),
                  itemBuilder: (_, index) => MediaPoster(
                    item: favorites[index],
                    favorite: true,
                    onTap: () => onOpen(favorites[index]),
                    onFavorite: () => onToggle(favorites[index]),
                  ),
                ),
        ),
      ],
    );
  }
}

/// Identifies a search result that links directly to an episode page without a
/// canonical series page. Episode links on these pages are only a partial view
/// and must not be presented as the series' confirmed total episode count.
bool isUnlinkedEpisodeResult(MediaItem item) {
  if (item.parentSeriesPageUrl.trim().isNotEmpty) return false;
  final title = item.title.toLowerCase();
  final url = item.pageUrl.toLowerCase();
  final explicitEpisodeUrl = RegExp(
    r'/(?:episodes?)(?:/|[-_])|(?:^|[-_/])(?:eps?|ep)\d{1,4}(?:[-_/]|$)|(?:^|[-_/])s\d{1,2}[-_]e(?:p)?s?\d{1,4}(?:[-_/]|$)',
    caseSensitive: false,
  ).hasMatch(url);
  if (explicitEpisodeUrl) return true;

  final hasSeasonLabel = RegExp(
    r'(?:الموسم|موسم|season\s*\d|(?:^|\W)s\d{1,2}(?:\W|$))',
    caseSensitive: false,
  ).hasMatch(title);
  final hasEpisodeLabel = RegExp(
    r'(?:الحلق[ةه]|episode|(?:^|\W)eps?\s*\d)',
    caseSensitive: false,
  ).hasMatch(title);
  return hasSeasonLabel &&
      hasEpisodeLabel &&
      _episodeNumberFrom('${item.title} ${item.pageUrl}') != null;
}

/// Episode URLs sometimes contain an `s1` token even when the provider does
/// not identify a season in the episode title. For an unlinked episode page,
/// that URL token alone is weak evidence and must not be shown as a confirmed
/// season number.
bool isSeasonGroupingUnverified(MediaItem item) {
  if (!isUnlinkedEpisodeResult(item)) return false;
  return !RegExp(
    r'(?:الموسم|موسم|season\s*\d{1,2}|(?:^|\W)s\d{1,2}(?:\W|$))',
    caseSensitive: false,
  ).hasMatch(item.title);
}

String episodeCollectionLabel({
  required MediaItem item,
  required int? seasonNumber,
  String? sourceTitle,
}) {
  if (isSeasonGroupingUnverified(item)) {
    return seasonNumber != null && seasonNumber > 0
        ? 'التجميعة $seasonNumber من المصدر'
        : 'تجميعة من المصدر';
  }
  final title = sourceTitle?.trim() ?? '';
  if (title.isNotEmpty) return title;
  return seasonNumber != null && seasonNumber > 0
      ? 'الموسم $seasonNumber'
      : 'الموسم';
}

bool canInferEpisodeForSelectedSeason({
  required int? episodeSeasonNumber,
  required int? selectedSeasonNumber,
  required int availableSeasonCount,
}) {
  if (episodeSeasonNumber != null && selectedSeasonNumber != null) {
    return episodeSeasonNumber == selectedSeasonNumber;
  }
  return episodeSeasonNumber == null && availableSeasonCount == 1;
}

class DetailPage extends StatefulWidget {
  const DetailPage({super.key, required this.item, required this.onOpenItem});
  final MediaItem item;
  final ValueChanged<MediaItem> onOpenItem;
  @override
  State<DetailPage> createState() => _DetailPageState();
}

class _DetailPageState extends State<DetailPage> {
  Map<String, dynamic>? _detail, _bundle;
  List<Map<String, dynamic>> _serverRows = [];
  bool _loading = true;
  bool _serversLoading = true;
  bool _seasonsLoading = true;
  bool _backgroundSyncing = false;
  bool _bundleFromCache = false;
  String? _error;
  int _seasonIndex = 0, _request = 0;
  int? _manualSeasonNumber;
  String? _downloadingMediaUrl;
  Future<List<Map<String, dynamic>>>? _providerBackupFuture;
  final Set<String> _refreshingSeasonPages = <String>{};
  final Map<int, String> _episodePageCursorBySeason = <int, String>{};
  final Map<int, int> _visibleEpisodeCountBySeason = <int, int>{};
  final Set<int> _loadingMoreEpisodeSeasons = <int>{};
  final Set<int> _episodeSeasonsExhausted = <int>{};

  void _seedEpisodePageCursors(Map<String, dynamic>? bundle) {
    for (final season in _maps(bundle?['seasons'])) {
      final number = _integer(season['number']);
      if (number <= 0 ||
          _maps(season['episodes']).isEmpty ||
          _episodeSeasonsExhausted.contains(number)) {
        continue;
      }
      final pageUrl = _text(season['pageUrl']).trim();
      final currentPageIsSeasonScoped =
          pageUrl == widget.item.pageUrl &&
          RegExp(
            r'(?:season[-_/ ]*\d{1,2}|(?:^|[-_/])s\d{1,2}(?:[-_/]|$)|الموسم)',
            caseSensitive: false,
          ).hasMatch('${widget.item.title} $pageUrl');
      if (pageUrl.startsWith('http') &&
          (pageUrl != widget.item.pageUrl || currentPageIsSeasonScoped)) {
        _episodePageCursorBySeason.putIfAbsent(number, () => pageUrl);
      }
    }
  }

  Future<void> _loadMoreEpisodes(Map<String, dynamic> season) async {
    final number = _integer(season['number']);
    if (number <= 0 || !_loadingMoreEpisodeSeasons.add(number)) return;
    final seasonUrl = _text(season['pageUrl'])
        .trim()
        .ifEmpty(widget.item.pageUrl);
    if (seasonUrl.isEmpty || !seasonUrl.startsWith('http')) {
      _loadingMoreEpisodeSeasons.remove(number);
      return;
    }
    final existing = _orderedEpisodes(season['episodes']);
    var visible = _visibleEpisodeCountBySeason[number] ?? 60;
    if (visible < existing.length) {
      visible = (visible + 60).clamp(0, existing.length);
      setState(() {
        _visibleEpisodeCountBySeason[number] = visible;
      });
      final canFetchNextPage =
          (_episodePageCursorBySeason[number]?.isNotEmpty ?? false) &&
          !_episodeSeasonsExhausted.contains(number);
      if (!canFetchNextPage) {
        _loadingMoreEpisodeSeasons.remove(number);
        return;
      }
    }
    final canFetchNextPage =
        (_episodePageCursorBySeason[number]?.isNotEmpty ?? false) &&
        !_episodeSeasonsExhausted.contains(number);
    if (!canFetchNextPage) {
      _loadingMoreEpisodeSeasons.remove(number);
      return;
    }

    final cursor = _episodePageCursorBySeason.putIfAbsent(
      number,
      () => seasonUrl,
    );
    final request = _request;
    if (mounted) setState(() {});
    try {
      final fresh = await NativeBridge.map('media.season.episodes.more', {
        'seasonUrl': seasonUrl,
        'cursorUrl': cursor,
        'seasonNumber': number,
        'knownEpisodeUrls': existing
            .map((episode) => _text(episode['pageUrl']))
            .where((url) => url.startsWith('http'))
            .toList(growable: false),
      }).timeout(const Duration(seconds: 20), onTimeout: () => null);
      if (!mounted || request != _request) return;
      if (fresh == null) {
        _message(
          context,
          'انتهت مهلة تحميل بقية الحلقات. اضغط المحاولة مرة أخرى.',
        );
        return;
      }
      final newEpisodes = _maps(fresh['seasons'])
          .where((row) => _integer(row['number']) == number)
          .expand((row) => _maps(row['episodes']))
          .toList(growable: false);
      final nextCursor = _text(fresh['nextPageUrl']).trim();
      setState(() {
        _bundle = _mergeSeasonBundles(_bundle, fresh);
        if (nextCursor.isNotEmpty) {
          _episodePageCursorBySeason[number] = nextCursor;
        } else {
          _episodeSeasonsExhausted.add(number);
          _episodePageCursorBySeason.remove(number);
        }
        final loadedCount = _orderedEpisodes(
          _seasons.firstWhere(
            (row) => _integer(row['number']) == number,
            orElse: () => const <String, dynamic>{},
          )['episodes'],
        ).length;
        _visibleEpisodeCountBySeason[number] = (visible + newEpisodes.length)
            .clamp(0, loadedCount);
      });
      if (newEpisodes.isEmpty && nextCursor.isEmpty) {
        _message(context, 'وصلت إلى آخر الحلقات التي يعرضها المصدر.');
      }
    } catch (_) {
      if (mounted && request == _request) {
        _message(context, 'تعذر تحميل بقية الحلقات. اضغط المحاولة مرة أخرى.');
      }
    } finally {
      _loadingMoreEpisodeSeasons.remove(number);
      if (mounted && request == _request) setState(() {});
    }
  }

  @override
  void initState() {
    super.initState();
    unawaited(_load());
  }

  bool _bundleHasEpisodes(Map<String, dynamic>? bundle) =>
      _maps(bundle?['seasons']).any((s) => _maps(s['episodes']).isNotEmpty);

  int _bundleEpisodeCount(Map<String, dynamic>? bundle) => _maps(
    bundle?['seasons'],
  ).fold<int>(0, (sum, s) => sum + _maps(s['episodes']).length);

  bool _hasSubstantialEpisodeList(Map<String, dynamic>? bundle) {
    for (final season in _maps(bundle?['seasons'])) {
      final eps = _orderedEpisodes(season['episodes']);
      if (eps.isEmpty) continue;
      final nums =
          eps.map(_episodeNumber).where((n) => n > 0).toSet().toList()..sort();
      if (nums.isEmpty) continue;
      if (nums.length >= 3 && nums.first == 1) return true;
      if (_bundleFromCache && nums.length >= 3) return true;
    }
    return false;
  }

  Map<String, dynamic>? _pickBestSeasonBundle(
    Map<String, dynamic>? current,
    Map<String, dynamic>? incoming,
  ) {
    final currentHasSeasons =
        current != null &&
        (_maps(current['seasons']).isNotEmpty ||
            _maps(current['relatedParts']).isNotEmpty);
    final incomingHasSeasons =
        incoming != null &&
        (_maps(incoming['seasons']).isNotEmpty ||
            _maps(incoming['relatedParts']).isNotEmpty);
    if (!currentHasSeasons) return incoming;
    if (!incomingHasSeasons) return current;
    final merged = _mergeSeasonBundles(current, incoming);
    return _bundleEpisodeCount(merged) >= _bundleEpisodeCount(current)
        ? merged
        : current;
  }

  String _seasonTabLabel(Map<String, dynamic>? season) {
    final title = _text(season?['title']).trim();
    if (title.isNotEmpty) return title;
    final number = _integer(season?['number']);
    return number > 0 ? 'الموسم $number' : 'الموسم';
  }

  Future<void> _load({bool force = false}) async {
    final request = ++_request;
    if (force) {
      _episodePageCursorBySeason.clear();
      _episodeSeasonsExhausted.clear();
      _visibleEpisodeCountBySeason.clear();
      _bundleFromCache = false;
    }
    setState(() {
      _loading = true;
      _serversLoading = true;
      _seasonsLoading = _isSeriesLookup;
      _backgroundSyncing = _isSeriesLookup;
      if (force && _isSeriesLookup) _bundle = null;
      _error = null;
    });
    final args = {'item': widget.item.toMap(), 'force': force};
    try {
      final cached = await Future.wait([
        NativeBridge.map('media.detail.cached', args),
        NativeBridge.map('media.seasons.cached', args),
      ]);
      if (!mounted || request != _request) return;
      setState(() {
        _detail = cached[0];
        final cachedHasEpisodes = _bundleHasEpisodes(cached[1]);
        if (!force || cached[1] != null) {
          _bundle = _pickBestSeasonBundle(_bundle, cached[1]);
        }
        if (cachedHasEpisodes && !force) {
          _bundleFromCache = true;
        }
        _seedEpisodePageCursors(_bundle);
        _serverRows = _maps(cached[0]?['servers']);
        _loading = _detail == null;
        _serversLoading = _serverRows.isEmpty;
        final seasonsList = _maps(_bundle?['seasons']);
        final hasSubstantialCached = _hasSubstantialEpisodeList(_bundle);
        final needsEpisodeSweep = _isSeriesLookup || seasonsList.isNotEmpty;
        // Keep shimmer loading visible until substantial real episodes are ready.
        _seasonsLoading = needsEpisodeSweep && (!hasSubstantialCached || force);
        _backgroundSyncing = needsEpisodeSweep;
        if (_bundle != null) {
          _seasonIndex = _seasonIndexFor(
            _bundle!,
            preferredNumber: _manualSeasonNumber,
          );
        }
      });
    } catch (_) {}
    if (!mounted || request != _request) return;
    unawaited(_refreshStory(request, args));
    unawaited(_loadServers(request));
    unawaited(_refreshSeasons(request, args));
  }

  Future<void> _refreshStory(int request, Map<String, Object?> args) async {
    try {
      final story = await NativeBridge.map('media.story.fetch', args);
      if (!mounted || request != _request) return;
      if (story != null) {
        setState(() {
          _detail = {...?_detail, ...story, 'servers': _servers};
          _error = null;
        });
      }
    } catch (error) {
      if (mounted && request == _request) {
        setState(() {
          if (_detail == null) _error = '$error';
        });
      }
    } finally {
      if (mounted && request == _request) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _refreshSeasons(int request, Map<String, Object?> args) async {
    try {
      final bundle = await NativeBridge.map('media.seasons.fetch', args);
      if (!mounted || request != _request) return;
      if (bundle == null) {
        final status = (_isSeriesLookup || _seasons.isNotEmpty)
            ? await NativeBridge.map('media.seasons.status', args)
            : null;
        await _finishSeasonSweep(request, args, status ?? const {});
        return;
      }
      final backgroundRefreshing = bundle['backgroundRefreshing'] == true;
      if (bundle.containsKey('seasons') || bundle.containsKey('relatedParts')) {
        final merged = _pickBestSeasonBundle(_bundle, bundle);
        final hasEpisodesNow = _bundleHasEpisodes(merged);
        final hasSubstantialNow = _hasSubstantialEpisodeList(merged);
        if (mounted && request == _request) {
          setState(() {
            _bundle = merged;
            _seedEpisodePageCursors(_bundle);
            if (merged != null) {
              _seasonIndex = _seasonIndexFor(
                merged,
                preferredNumber: _manualSeasonNumber,
              );
            }
            if (hasEpisodesNow && (!backgroundRefreshing || hasSubstantialNow)) {
              _seasonsLoading = false;
            }
          });
        }
      }
      if (backgroundRefreshing) {
        final requiresSweep =
            _isSeriesLookup ||
            _seasons.isNotEmpty ||
            _maps(bundle['seasons']).isNotEmpty;
        if (mounted && request == _request) {
          setState(() {
            _backgroundSyncing = requiresSweep;
            _seasonsLoading =
                _seasonsLoading && !_hasSubstantialEpisodeList(_bundle);
          });
        }
        unawaited(_pollSeasonCache(request, args));
        return;
      }
      await _finishSeasonSweep(request, args, bundle);
    } catch (_) {
      if (mounted && request == _request) {
        setState(() {
          _seasonsLoading = false;
          _backgroundSyncing = false;
        });
      }
    }
  }

  Future<void> _finishSeasonSweep(
    int request,
    Map<String, Object?> args,
    Map<String, dynamic> status,
  ) async {
    if (!mounted || request != _request) return;
    final requiresSweep =
        _isSeriesLookup ||
        _seasons.isNotEmpty ||
        _maps(status['seasons']).isNotEmpty;
    final cached = requiresSweep
        ? await NativeBridge.map('media.seasons.cached', args)
        : null;
    if (!mounted || request != _request) return;
    final incoming = status.containsKey('seasons') ? status : null;
    final best = _pickBestSeasonBundle(
      _pickBestSeasonBundle(_bundle, incoming),
      cached,
    );
    final finalBundle =
        best ?? <String, dynamic>{'seasons': const [], 'relatedParts': const []};
    setState(() {
      _bundle = finalBundle;
      _seedEpisodePageCursors(_bundle);
      _seasonIndex = _seasonIndexFor(
        finalBundle,
        preferredNumber: _manualSeasonNumber,
      );
      _backgroundSyncing = false;
      _seasonsLoading = false;
    });
    _refreshCurrentSeasonIfSparse();
  }

  Future<void> _pollSeasonCache(int request, Map<String, Object?> args) async {
    final deadline = DateTime.now().add(const Duration(seconds: 20));
    try {
      while (mounted && request == _request) {
        await Future<void>.delayed(const Duration(milliseconds: 350));
        if (!mounted || request != _request) return;
        final status = await NativeBridge.map('media.seasons.status', args);
        if (!mounted || request != _request) return;
        if (status?['backgroundRefreshing'] == true &&
            DateTime.now().isBefore(deadline)) {
          final cached = await NativeBridge.map('media.seasons.cached', args);
          if (!mounted || request != _request) return;
          if (cached != null) {
            final best = _pickBestSeasonBundle(_bundle, cached);
            final grew =
                _bundleEpisodeCount(best) > _bundleEpisodeCount(_bundle) ||
                _maps(best?['seasons']).length > _seasons.length;
            if (grew) {
              setState(() {
                _bundle = best;
                _seedEpisodePageCursors(_bundle);
                if (best != null) {
                  _seasonIndex = _seasonIndexFor(
                    best,
                    preferredNumber: _manualSeasonNumber,
                  );
                }
                if (_hasSubstantialEpisodeList(_bundle)) _seasonsLoading = false;
              });
            }
          }
          continue;
        }
        await _finishSeasonSweep(request, args, status ?? const {});
        return;
      }
    } catch (_) {
      if (mounted && request == _request) {
        setState(() {
          _backgroundSyncing = false;
          _seasonsLoading = false;
        });
      }
    }
  }

  Future<void> _refreshSeasonPage(Map<String, dynamic> season) async {
    final seasonUrl = _text(season['pageUrl']).trim();
    final seasonNumber = _integer(season['number']);
    if (seasonUrl.isEmpty ||
        seasonNumber <= 0 ||
        !seasonUrl.startsWith('http') ||
        !_refreshingSeasonPages.add(seasonUrl)) {
      return;
    }
    final request = _request;
    if (mounted) {
      setState(() {
        _backgroundSyncing = true;
      });
    }
    try {
      final fresh = await NativeBridge.map('media.season.page.fetch', {
        'item': widget.item.toMap(),
        'seasonUrl': seasonUrl,
        'seasonNumber': seasonNumber,
      });
      if (!mounted || request != _request) return;
      if (fresh == null) return;
      if (fresh.containsKey('seasons')) {
        final merged = _pickBestSeasonBundle(_bundle, fresh);
        if (merged != null) {
          setState(() {
            _bundle = merged;
            _seedEpisodePageCursors(_bundle);
          });
        }
      }
      if (fresh['backgroundRefreshing'] == true) {
        unawaited(_pollSeasonCache(request, {'item': widget.item.toMap()}));
      } else {
        final status = await NativeBridge.map('media.seasons.status', {
          'item': widget.item.toMap(),
        });
        if (!mounted || request != _request) return;
        await _finishSeasonSweep(request, {
          'item': widget.item.toMap(),
        }, status ?? fresh);
      }
    } catch (_) {
      if (mounted && request == _request) {
        setState(() {
          _backgroundSyncing = false;
          _seasonsLoading = false;
        });
      }
    } finally {
      _refreshingSeasonPages.remove(seasonUrl);
      if (mounted && request == _request) setState(() {});
    }
  }

  void _refreshCurrentSeasonIfSparse() {
    if (_seasons.isEmpty) return;
    final selected = _seasons[_seasonIndex.clamp(0, _seasons.length - 1)];
    if (_seasonNeedsEnrichment(selected['episodes'])) {
      unawaited(_refreshSeasonPage(selected));
    }
  }

  Future<void> _loadServers(int request) async {
    final url = widget.item.pageUrl;
    final key = _serverCacheKey(url);
    final provider = widget.item.provider;
    final alternatives = widget.item.alternatePageUrls
        .where((page) => page.trim() != url.trim())
        .take(24)
        .toList(growable: false);
    _providerBackupFuture = null;
    try {
      final cached = await NativeBridge.readCache(key);
      if (cached != null && mounted && request == _request) {
        final rows = _tagServerRows(_maps(jsonDecode(cached)), url, provider);
        if (rows.isNotEmpty) {
          setState(() {
            _serverRows = rows;
            _serversLoading = false;
          });
          if (!widget.item.isSeries) {
            unawaited(_warmTopMovieServer(rows, url));
          }
        }
      }
    } catch (_) {}
    if (!mounted || request != _request) return;
    var pendingLookups = 1 + (alternatives.isNotEmpty ? 1 : 0);
    // Discover more configured provider pages without delaying detail rendering or
    // the first play action. The player screen can start from the current server;
    // these extra candidates are added as soon as discovery returns.
    if (!widget.item.isSeries) {
      final backupFuture = _fetchProviderBackupServerRows(
        title: widget.item.title,
        pageUrl: url,
        isSeries: false,
        year: widget.item.year,
      );
      _providerBackupFuture = backupFuture;
      pendingLookups++;
    }

    Future<void> absorb(Future<List<Map<String, dynamic>>> future) async {
      try {
        final rows = await future;
        if (!mounted || request != _request) return;
        if (rows.isNotEmpty) {
          setState(() {
            _serverRows = _mergeServerRows([...rows, ..._serverRows]);
            _serversLoading = false;
          });
          if (!widget.item.isSeries) {
            unawaited(_warmTopMovieServer(_serverRows, url));
          }
        }
      } catch (_) {
        // One unavailable provider must not block playable results from others.
      } finally {
        if (mounted && request == _request) {
          setState(() {
            pendingLookups--;
            _serversLoading = _servers.isEmpty && pendingLookups > 0;
          });
        }
      }
    }

    unawaited(absorb(_fetchServerRowsForPage(url, provider)));
    if (alternatives.isNotEmpty) {
      unawaited(
        absorb(
          _fetchAlternativeServerRows(
            pageUrls: alternatives,
            provider: provider,
          ),
        ),
      );
    }
    if (!widget.item.isSeries && _providerBackupFuture != null) {
      unawaited(absorb(_providerBackupFuture!));
    }
    if (mounted && request == _request) {
      setState(() => _serversLoading = _servers.isEmpty && pendingLookups > 0);
    }
  }

  List<Map<String, dynamic>> get _seasons => _maps(_bundle?['seasons']);
  List<Map<String, dynamic>> get _parts => _maps(_bundle?['relatedParts']);
  List<Map<String, dynamic>> get _servers =>
      _serverRows.isNotEmpty ? _serverRows : _maps(_detail?['servers']);

  String get _seriesTitle {
    final raw = widget.item.parentSeriesTitle.trim().isNotEmpty
        ? widget.item.parentSeriesTitle.trim()
        : widget.item.title;
    return _cleanSeriesTitle(raw);
  }

  bool get _isEpisodeCard =>
      widget.item.parentSeriesPageUrl.trim().isNotEmpty ||
      isUnlinkedEpisodeResult(widget.item);

  bool get _isSeriesLookup => widget.item.isSeries || _isEpisodeCard;

  int? get _currentEpisodeSeasonNumber {
    final currentUrl = _normalizedMediaUrl(widget.item.pageUrl);
    return _maps(_bundle?['seasons'])
            .where(
              (season) => _maps(season['episodes']).any(
                (episode) =>
                    _normalizedMediaUrl(_text(episode['pageUrl'])) ==
                    currentUrl,
              ),
            )
            .map((season) => _integer(season['number']))
            .where((number) => number > 0)
            .firstOrNull ??
        _seasonNumberFrom(
          '${widget.item.title} ${widget.item.latestEpisodeLabel} ${widget.item.pageUrl}',
        );
  }

  int? get _selectedSeasonNumber => _seasons.isEmpty
      ? null
      : _integer(
          _seasons[_seasonIndex.clamp(0, _seasons.length - 1)]['number'],
        );

  int _seasonIndexFor(Map<String, dynamic> bundle, {int? preferredNumber}) {
    final seasons = _maps(bundle['seasons']);
    if (seasons.isEmpty) return 0;
    if (preferredNumber != null) {
      final preferred = seasons.indexWhere(
        (season) => _integer(season['number']) == preferredNumber,
      );
      if (preferred >= 0) return preferred;
    }
    final currentUrl = _normalizedMediaUrl(widget.item.pageUrl);
    for (var index = 0; index < seasons.length; index++) {
      final containsCurrentEpisode = _maps(seasons[index]['episodes']).any(
        (episode) =>
            _normalizedMediaUrl(_text(episode['pageUrl'])) == currentUrl,
      );
      if (containsCurrentEpisode) return index;
    }
    final wantedNumber = _seasonNumberFrom(
      '${widget.item.title} ${widget.item.latestEpisodeLabel} ${widget.item.pageUrl}',
    );
    if (wantedNumber != null) {
      final matching = seasons.indexWhere(
        (season) => _integer(season['number']) == wantedNumber,
      );
      if (matching >= 0) return matching;
    }
    final firstNonEmpty = seasons.indexWhere(
      (season) => _maps(season['episodes']).isNotEmpty,
    );
    return firstNonEmpty >= 0 ? firstNonEmpty : 0;
  }

  int? _currentEpisodeIndex(List<Map<String, dynamic>> episodes) {
    final currentUrl = _normalizedMediaUrl(widget.item.pageUrl);
    final exact = episodes.indexWhere(
      (episode) => _normalizedMediaUrl(_text(episode['pageUrl'])) == currentUrl,
    );
    if (exact >= 0) return exact;
    if (!_isEpisodeCard) {
      final season = _seasons.isEmpty
          ? null
          : _seasons[_seasonIndex.clamp(0, _seasons.length - 1)];
      final latestNumber = _latestEpisodeNumberForSeason(season);
      if (latestNumber == null) return null;
      final latestIndex = episodes.indexWhere(
        (episode) => _episodeNumber(episode) == latestNumber,
      );
      return latestIndex >= 0 ? latestIndex : null;
    }
    if (!canInferEpisodeForSelectedSeason(
      episodeSeasonNumber: _currentEpisodeSeasonNumber,
      selectedSeasonNumber: _selectedSeasonNumber,
      availableSeasonCount: _seasons.length,
    )) {
      return null;
    }
    final number = _episodeNumberFrom(
      '${widget.item.title} ${widget.item.latestEpisodeLabel} ${widget.item.pageUrl}',
    );
    if (number == null) return null;
    final index = episodes.indexWhere(
      (episode) => _episodeNumber(episode) == number,
    );
    return index >= 0 ? index : null;
  }

  Map<String, dynamic>? _episodeForCurrentPage(
    List<Map<String, dynamic>> episodes,
  ) {
    if (!_isEpisodeCard) {
      if (episodes.isEmpty) return null;
      final season = _seasons.isEmpty
          ? null
          : _seasons[_seasonIndex.clamp(0, _seasons.length - 1)];
      final latestNumber = _latestEpisodeNumberForSeason(season);
      if (latestNumber != null) {
        final latest = episodes
            .where((episode) => _episodeNumber(episode) == latestNumber)
            .firstOrNull;
        if (latest != null) return latest;
      }
      return episodes.last;
    }
    final currentUrl = _normalizedMediaUrl(widget.item.pageUrl);
    final exact = episodes
        .where(
          (episode) =>
              _normalizedMediaUrl(_text(episode['pageUrl'])) == currentUrl,
        )
        .firstOrNull;
    if (exact != null) return exact;
    if (!canInferEpisodeForSelectedSeason(
      episodeSeasonNumber: _currentEpisodeSeasonNumber,
      selectedSeasonNumber: _selectedSeasonNumber,
      availableSeasonCount: _seasons.length,
    )) {
      return null;
    }
    final inferredNumber = _episodeNumberFrom(
      '${widget.item.title} ${widget.item.latestEpisodeLabel} ${widget.item.pageUrl}',
    );
    if (inferredNumber != null && inferredNumber > 0) {
      return {
        'title': widget.item.title,
        'number': inferredNumber,
        'pageUrl': widget.item.pageUrl,
      };
    }
    return null;
  }

  int? _latestEpisodeNumberForSeason(Map<String, dynamic>? season) {
    final label = widget.item.latestEpisodeLabel.trim();
    if (label.isEmpty) return null;
    final selectedSeason = _integer(season?['number']);
    final labelSeason = _seasonNumberFrom(label);
    if (labelSeason != null &&
        selectedSeason > 0 &&
        labelSeason != selectedSeason) {
      return null;
    }
    return _episodeNumberFrom(label);
  }

  bool get _isSeriesEffective =>
      widget.item.isSeries ||
      _isEpisodeCard ||
      _seasons.any((season) => _maps(season['episodes']).isNotEmpty);

  @override
  Widget build(BuildContext context) {
    final isSeriesEffective = _isSeriesEffective;
    final overview = _text(_detail?['overview']).ifEmpty(widget.item.overview);
    final cast = (_detail?['cast'] is List
        ? (_detail!['cast'] as List)
              .map(_text)
              .where((e) => e.isNotEmpty)
              .toList()
        : <String>[]);
    final season = _seasons.isNotEmpty && _seasonIndex < _seasons.length
        ? _seasons[_seasonIndex]
        : null;
    final episodes = _orderedEpisodes(season?['episodes']);
    final downloadEpisode = _episodeForCurrentPage(episodes);
    // ✅ إجمالي الحلقات من كل المواسم — مش حلقات الموسم الحالي فقط
    final totalEpisodeCount = _seasons.isEmpty
        ? episodes.length
        : _seasons.fold<int>(0, (sum, s) => sum + _maps(s['episodes']).length);
    return Scaffold(
      bottomNavigationBar: const SafeArea(child: AppBannerAdStrip()),
      appBar: AppBar(
        title: Text(_seriesTitle, maxLines: 1, overflow: TextOverflow.ellipsis),
        actions: [
          IconButton(
            onPressed: _loading ? null : () => _load(force: true),
            icon: const Icon(Icons.refresh_rounded, color: _brick),
          ),
        ],
      ),
      body: RefreshIndicator(
        color: _brick,
        onRefresh: () => _load(force: true),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 26),
          children: [
            _DetailHeader(
              item: widget.item,
              isSeries: isSeriesEffective,
              episodeCount: totalEpisodeCount,
              seasonCount: _seasons.length,
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                Expanded(
                  child: FilledButton.icon(
                    onPressed: () => _playItem(),
                    icon: const Icon(Icons.play_arrow_rounded),
                    label: const Text('تشغيل'),
                  ),
                ),
                const SizedBox(width: 9),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _load(force: true),
                    icon: const Icon(Icons.sync_rounded),
                    label: const Text('تحديث البيانات'),
                  ),
                ),
              ],
            ),
            if (overview.isNotEmpty) ...[
              const SizedBox(height: 17),
              _SectionCard(
                title: 'القصة كاملة',
                icon: '📖',
                child: Text(
                  overview,
                  style: const TextStyle(
                    color: _ink,
                    height: 1.7,
                    fontSize: 14,
                  ),
                ),
              ),
            ] else if (_loading && _detail == null) ...[
              const SizedBox(height: 17),
              _SectionCard(
                title: 'القصة كاملة',
                icon: '📖',
                child: _storyLoadingSkeleton(),
              ),
            ],
            if (cast.isNotEmpty) ...[
              const SizedBox(height: 13),
              _SectionCard(
                title: 'طاقم العمل',
                icon: '🎭',
                child: Wrap(
                  spacing: 7,
                  runSpacing: 8,
                  children: cast
                      .map(
                        (name) =>
                            Chip(label: Text(name), backgroundColor: _paper),
                      )
                      .toList(),
                ),
              ),
            ],
            if (isSeriesEffective) ...[
              if (_isEpisodeCard) ...[
                if (downloadEpisode != null) ...[
                  const SizedBox(height: 12),
                  _downloadEpisodeCard(downloadEpisode, season),
                ],
                const SizedBox(height: 13),
                _serverSection(
                  _servers,
                  widget.item.pageUrl,
                  widget.item.title,
                  loading: _serversLoading,
                ),
              ],
              const SizedBox(height: 13),
              _seasonsSection(season, episodes),
            ] else ...[
              if (_servers.isNotEmpty || !_serversLoading) ...[
                const SizedBox(height: 12),
                _downloadMovieCard(),
              ],
              const SizedBox(height: 13),
              _serverSection(
                _servers,
                widget.item.pageUrl,
                widget.item.title,
                loading: _serversLoading,
              ),
            ],
            if (isSeriesEffective &&
                !_isEpisodeCard &&
                downloadEpisode != null &&
                !_shouldShowSeasonSkeleton(season, episodes)) ...[
              const SizedBox(height: 12),
              _downloadEpisodeCard(downloadEpisode, season),
            ],
            if (_parts.isNotEmpty) ...[
              const SizedBox(height: 13),
              _SectionCard(
                title: isSeriesEffective
                    ? 'الأجزاء والمواسم الأخرى'
                    : 'أجزاء وأعمال مرتبطة',
                icon: '🎬',
                child: SizedBox(
                  height: 235,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: _parts.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 10),
                    itemBuilder: (_, index) {
                      final part = MediaItem.fromMap(_parts[index]);
                      return SizedBox(
                        width: 118,
                        child: MediaPoster(
                          item: part,
                          favorite: false,
                          onTap: () => widget.onOpenItem(part),
                          onFavorite: () {},
                        ),
                      );
                    },
                  ),
                ),
              ),
            ],
            if (_error != null && _detail == null)
              Padding(
                padding: const EdgeInsets.all(15),
                child: Text(
                  'تعذر تحميل تفاصيل العمل الآن. $_error',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: _muted),
                ),
              ),
            if (_loading && _detail != null)
              const Padding(
                padding: EdgeInsets.only(top: 10),
                child: LinearProgressIndicator(
                  color: _brick,
                  backgroundColor: _line,
                ),
              ),
          ],
        ),
      ),
    );
  }

  bool _shouldShowSeasonSkeleton(
    Map<String, dynamic>? selected,
    List<Map<String, dynamic>> episodes,
  ) {
    if (_seasonsLoading) return true;
    final selectedUrl = _text(selected?['pageUrl']).trim();
    final isRefreshingSelected = _refreshingSeasonPages.contains(selectedUrl);
    return episodes.isEmpty && (_backgroundSyncing || isRefreshingSelected);
  }

  Widget _seasonsSection(
    Map<String, dynamic>? selected,
    List<Map<String, dynamic>> episodes,
  ) {
    final seasonNumber = _integer(selected?['number']);
    final visibleCount = _visibleEpisodeCountBySeason[seasonNumber] ?? 60;
    final visibleEpisodes = episodes.take(visibleCount).toList(growable: false);
    // hasLoadedMore: هناك حلقات في الذاكرة أكثر مما هو معروض (تحتاج "عرض المزيد")
    final hasLoadedMore = episodes.length > visibleEpisodes.length;
    final hasNextPageCursor =
        (_episodePageCursorBySeason[seasonNumber]?.isNotEmpty ?? false) &&
        !_episodeSeasonsExhausted.contains(seasonNumber);
    final isRefreshingSelected = _refreshingSeasonPages.contains(
      _text(selected?['pageUrl']).trim(),
    );
    final showInitialSkeleton = _shouldShowSeasonSkeleton(selected, episodes);
    final canRequestMore =
        !showInitialSkeleton && (hasLoadedMore || hasNextPageCursor);
    return _SectionCard(
      title: 'حلقات هذا الجزء / الموسم',
      icon: '📺',
      trailing: Text(
        showInitialSkeleton
            ? 'جاري تحميل الحلقات…'
            : '${episodes.length} حلقة${(_backgroundSyncing || isRefreshingSelected) ? ' · جاري التحديث' : ''}',
        style: const TextStyle(color: _muted, fontSize: 12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (showInitialSkeleton)
            _seasonEpisodesSkeleton()
          else if (_seasons.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 12),
              child: Text(
                'لم يتم العثور على حلقات حالياً.',
                textDirection: TextDirection.rtl,
                textAlign: TextAlign.center,
                style: TextStyle(color: _muted, fontSize: 13),
              ),
            )
          else ...[
            SizedBox(
              height: 42,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: _seasons.length,
                separatorBuilder: (_, __) => const SizedBox(width: 7),
                itemBuilder: (_, index) {
                  final season = _seasons[index];
                  final label = _seasonTabLabel(season);
                  return ChoiceChip(
                    label: Text(label),
                    selected: index == _seasonIndex,
                    showCheckmark: false,
                    labelStyle: TextStyle(
                      color: index == _seasonIndex ? Colors.white : _muted,
                      fontSize: 12,
                    ),
                    side: BorderSide(
                      color: index == _seasonIndex ? _brick : _line,
                    ),
                    onSelected: (_) {
                      final seasonEpisodes = _maps(season['episodes']);
                      setState(() {
                        _seasonIndex = index;
                        _manualSeasonNumber = _integer(season['number']);
                      });
                      if (_seasonNeedsEnrichment(seasonEpisodes)) {
                        unawaited(_refreshSeasonPage(season));
                      }
                    },
                  );
                },
              ),
            ),
            const SizedBox(height: 12),
            if (episodes.isEmpty)
              (isRefreshingSelected || _backgroundSyncing)
                  ? _seasonEpisodesSkeleton()
                  : const Padding(
                      padding: EdgeInsets.symmetric(vertical: 10),
                      child: Text(
                        'لا توجد حلقات متاحة لهذا الموسم حالياً.',
                        textDirection: TextDirection.rtl,
                        textAlign: TextAlign.center,
                        style: TextStyle(color: _muted, fontSize: 13),
                      ),
                    )
            else ...[
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: List.generate(visibleEpisodes.length, (index) {
                  final episode = visibleEpisodes[index];
                  final n = _episodeNumber(episode);
                  final isCurrent = _currentEpisodeIndex(episodes) == index;
                  return ActionChip(
                    avatar: Icon(
                      Icons.play_arrow_rounded,
                      size: 16,
                      color: isCurrent ? Colors.white : _brick,
                    ),
                    label: Text('الحلقة ${n > 0 ? n : index + 1}'),
                    backgroundColor: isCurrent ? _brick : _paper,
                    labelStyle: TextStyle(
                      color: isCurrent ? Colors.white : _ink,
                      fontWeight: isCurrent ? FontWeight.w800 : FontWeight.w500,
                    ),
                    side: BorderSide(color: isCurrent ? _brick : _line),
                    onPressed: () => _openEpisode(episodes, index, selected),
                  );
                }),
              ),
              if (canRequestMore) ...[
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed:
                      seasonNumber > 0 &&
                          _loadingMoreEpisodeSeasons.contains(seasonNumber)
                      ? null
                      : () => _loadMoreEpisodes(selected!),
                  icon: _loadingMoreEpisodeSeasons.contains(seasonNumber)
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: _brick,
                          ),
                        )
                      : const Icon(Icons.expand_more_rounded),
                  label: Text(
                    hasLoadedMore
                        ? hasNextPageCursor
                              ? 'عرض ${episodes.length - visibleEpisodes.length} حلقة وجلب التالية'
                              : 'عرض ${episodes.length - visibleEpisodes.length} حلقات إضافية'
                        : 'تحميل حلقات إضافية من المصدر',
                  ),
                ),
              ],
            ],
          ],
        ],
      ),
    );
  }

  Widget _storyLoadingSkeleton() => _ShimmerLoading(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        _SkeletonBlock(
          width: MediaQuery.sizeOf(context).width * .82,
          height: 12,
        ),
        const SizedBox(height: 10),
        _SkeletonBlock(
          width: MediaQuery.sizeOf(context).width * .68,
          height: 12,
        ),
        const SizedBox(height: 10),
        _SkeletonBlock(
          width: MediaQuery.sizeOf(context).width * .48,
          height: 12,
        ),
      ],
    ),
  );

  Widget _seasonEpisodesSkeleton() => _ShimmerLoading(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: List.generate(
              4,
              (_) => Container(
                width: 92,
                height: 36,
                margin: const EdgeInsets.only(left: 7),
                decoration: BoxDecoration(
                  color: const Color(0xFFEADBCB),
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(color: _line),
                ),
              ),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          alignment: WrapAlignment.start,
          children: List.generate(
            9,
            (_) => Container(
              width: 104,
              height: 42,
              decoration: BoxDecoration(
                color: const Color(0xFFEADBCB),
                borderRadius: BorderRadius.circular(24),
                border: Border.all(color: _line),
              ),
            ),
          ),
        ),
      ],
    ),
  );

  Widget _downloadEpisodeCard(
    Map<String, dynamic> episode,
    Map<String, dynamic>? season,
  ) {
    final url = _text(episode['pageUrl']);
    final number = _episodeNumber(episode);
    final seasonTitle = _seasonTabLabel(season);
    final loading = _downloadingMediaUrl == url;
    return Material(
      color: const Color(0xFF078B68),
      borderRadius: BorderRadius.circular(17),
      child: InkWell(
        borderRadius: BorderRadius.circular(17),
        onTap: loading
            ? null
            : () => _startEpisodeDownload(episode, seasonTitle),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 14),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: Colors.white24,
                child: loading
                    ? const SizedBox(
                        width: 19,
                        height: 19,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Icon(Icons.download_rounded, color: Colors.white),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      loading
                          ? 'جاري تجهيز التحميل…'
                          : 'تحميل مباشر إلى الهاتف',
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '$seasonTitle • الحلقة $number • إلى مجلد Downloads',
                      style: const TextStyle(
                        color: Colors.white70,
                        fontSize: 11,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 11,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(9),
                ),
                child: const Text(
                  'تحميل الآن ↓',
                  style: TextStyle(
                    color: Color(0xFF08785E),
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _downloadMovieCard() {
    final url = widget.item.pageUrl;
    final loading = _downloadingMediaUrl == url;
    return Material(
      color: const Color(0xFF078B68),
      borderRadius: BorderRadius.circular(17),
      child: InkWell(
        borderRadius: BorderRadius.circular(17),
        onTap: loading ? null : _startMovieDownload,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 14),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: Colors.white24,
                child: loading
                    ? const SizedBox(
                        width: 19,
                        height: 19,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Icon(Icons.download_rounded, color: Colors.white),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      loading
                          ? 'جاري تجهيز التحميل…'
                          : 'تحميل الفيلم إلى الهاتف',
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      _servers.isEmpty && _serversLoading
                          ? 'جاري تجهيز روابط المصدر في الخلفية'
                          : 'حفظ في مجلد Downloads',
                      style: const TextStyle(
                        color: Colors.white70,
                        fontSize: 11,
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 11,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(9),
                ),
                child: const Text(
                  'تحميل الآن ↓',
                  style: TextStyle(
                    color: Color(0xFF08785E),
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _startMovieDownload() async {
    final pageUrl = widget.item.pageUrl;
    if (pageUrl.isEmpty) return;
    setState(() => _downloadingMediaUrl = pageUrl);
    try {
      var servers = _servers;
      if (servers.isEmpty) {
        final cacheKey = _serverCacheKey(pageUrl);
        try {
          final cached = await NativeBridge.readCache(cacheKey);
          if (cached != null) servers = _maps(jsonDecode(cached));
        } catch (_) {}
        if (servers.isEmpty) {
          servers = await _fetchServerRowsForPage(
            pageUrl,
            widget.item.provider,
          );
        }
        if (servers.isEmpty && widget.item.alternatePageUrls.isNotEmpty) {
          servers = await _fetchAlternativeServerRows(
            pageUrls: widget.item.alternatePageUrls,
            provider: widget.item.provider,
          );
        }
        if (servers.isNotEmpty) {
          unawaited(NativeBridge.writeCache(cacheKey, jsonEncode(servers)));
          if (mounted) setState(() => _serverRows = servers);
        }
      }
      if (!mounted) return;
      if (servers.isEmpty) {
        _message(context, 'لا توجد روابط تنزيل متاحة لهذا الفيلم الآن.');
        return;
      }
      final server = await _findWorkingServer(servers, pageUrl);
      if (!mounted) return;
      if (server == null) {
        _message(context, 'لم يستجب أي رابط تنزيل صالح لهذا الفيلم.');
        return;
      }
      final sourcePage = _text(server['sourcePageUrl']).ifEmpty(pageUrl);
      final streamUrl = _text(server['directStreamUrl']).trim();
      if (streamUrl.isEmpty) {
        _message(context, 'المصدر لم يوفر رابط تنزيل مباشر لهذا الفيلم.');
        return;
      }
      final started =
          await _native.invokeMethod<bool>('download.start', {
            'url': _absolute(streamUrl, pageUrl),
            'title': widget.item.title,
            'referer': _text(server['embedUrl']).ifEmpty(sourcePage),
          }) ??
          false;
      if (mounted) {
        _message(
          context,
          started
              ? 'بدأ تنزيل الفيلم في الخلفية إلى مجلد Downloads.'
              : 'تعذر بدء تنزيل الفيلم على هذا الجهاز.',
        );
      }
    } catch (_) {
      if (mounted) _message(context, 'تعذر تجهيز تنزيل الفيلم الآن.');
    } finally {
      if (mounted && _downloadingMediaUrl == pageUrl) {
        setState(() => _downloadingMediaUrl = null);
      }
    }
  }

  Future<void> _startEpisodeDownload(
    Map<String, dynamic> episode,
    String seasonTitle,
  ) async {
    final url = _text(episode['pageUrl']);
    if (url.isEmpty) return;
    setState(() => _downloadingMediaUrl = url);
    try {
      final cacheKey = _serverCacheKey(url);
      List<Map<String, dynamic>> servers = [];
      final provider = _text(episode['providerName'])
          .ifEmpty(widget.item.provider);
      try {
        final cached = await NativeBridge.readCache(cacheKey);
        if (cached != null) servers = _maps(jsonDecode(cached));
      } catch (_) {}
      if (servers.isEmpty) {
        servers = await _fetchServerRowsForPage(url, provider);
        if (servers.isNotEmpty) {
          unawaited(NativeBridge.writeCache(cacheKey, jsonEncode(servers)));
        }
      }
      final alternateEpisodePages =
          (episode['alternatePageUrls'] as List? ?? const [])
              .map(_text)
              .where((page) => page.trim() != url.trim())
              .take(24)
              .toList(growable: false);
      if (alternateEpisodePages.isNotEmpty) {
        final alternativeRows = await _fetchAlternativeServerRows(
          pageUrls: alternateEpisodePages,
          provider: provider,
        ).timeout(const Duration(seconds: 12));
        servers = _mergeServerRows([...servers, ...alternativeRows]);
      }
      if (!mounted) return;
      if (servers.isEmpty) {
        _message(context, 'لا توجد روابط تنزيل متاحة لهذه الحلقة الآن.');
        return;
      }
      final server = await _findWorkingServer(servers, url);
      if (!mounted) return;
      if (server == null) {
        _message(context, 'لم يستجب أي رابط تنزيل صالح لهذه الحلقة.');
        return;
      }
      final sourcePage = _text(server['sourcePageUrl']).ifEmpty(url);
      final streamUrl = _text(server['directStreamUrl']).trim();
      if (streamUrl.isEmpty) {
        _message(context, 'المصدر لم يوفر رابط تنزيل مباشر لهذه الحلقة.');
        return;
      }
      final episodeNumber = _episodeNumber(episode);
      final title = '$_seriesTitle · $seasonTitle · الحلقة $episodeNumber';
      final started =
          await _native.invokeMethod<bool>('download.start', {
            'url': _absolute(streamUrl, url),
            'title': title,
            'referer': _text(server['embedUrl']).ifEmpty(sourcePage),
          }) ??
          false;
      if (mounted) {
        _message(
          context,
          started
              ? 'بدأ تنزيل الحلقة $episodeNumber في الخلفية.'
              : 'تعذر بدء التنزيل على هذا الجهاز.',
        );
      }
    } catch (_) {
      if (mounted) _message(context, 'تعذر تجهيز تنزيل الحلقة الآن.');
    } finally {
      if (mounted && _downloadingMediaUrl == url) {
        setState(() => _downloadingMediaUrl = null);
      }
    }
  }

  void _openEpisode(
    List<Map<String, dynamic>> episodes,
    int index,
    Map<String, dynamic>? season,
  ) {
    if (index < 0 || index >= episodes.length) return;

    final episode = Map<String, dynamic>.from(episodes[index]);
    final episodeUrl = _text(episode['pageUrl']).trim();
    final currentEpisodeNumber = _episodeNumberFrom(
      '${widget.item.title} ${widget.item.latestEpisodeLabel} ${widget.item.pageUrl}',
    );
    final currentSeasonNumber = _seasonNumberFrom(
      '${widget.item.title} ${widget.item.latestEpisodeLabel} ${widget.item.pageUrl}',
    );
    final selectedSeasonNumber = _integer(season?['number']);
    final selectedEpisodeNumber = _episodeNumber(episode);
    final currentPageUrl = widget.item.pageUrl.trim();
    final currentPageMatchesSelection =
        currentPageUrl.startsWith('http') &&
        currentEpisodeNumber != null &&
        currentEpisodeNumber == selectedEpisodeNumber &&
        (selectedSeasonNumber <= 0 ||
            currentSeasonNumber == null ||
            currentSeasonNumber == selectedSeasonNumber);

    // Some catalog cards are themselves the current episode page. If a
    // provider's season bundle omitted that episode URL, reuse the known page
    // URL only when both the season and episode labels agree.
    if (episodeUrl.isEmpty && currentPageMatchesSelection) {
      episode['pageUrl'] = currentPageUrl;
      episode['seasonNumber'] = selectedSeasonNumber;
      if (_text(episode['providerName']).trim().isEmpty) {
        episode['providerName'] = widget.item.provider;
      }
    }

    if (_text(episode['pageUrl']).trim().isEmpty) {
      _message(context, 'رابط هذه الحلقة غير متاح من المصدر حاليًا.');
      return;
    }

    final episodesForPage = List<Map<String, dynamic>>.generate(
      episodes.length,
      (episodeIndex) => Map<String, dynamic>.from(episodes[episodeIndex]),
    );
    episodesForPage[index] = episode;
    Navigator.of(context).push<void>(
      PageRouteBuilder<void>(
        transitionDuration: const Duration(milliseconds: 330),
        reverseTransitionDuration: const Duration(milliseconds: 240),
        pageBuilder: (_, __, ___) => EpisodePage(
          seriesTitle: _seriesTitle,
          provider: _text(episodes[index]['providerName'])
              .ifEmpty(widget.item.provider),
          seasonTitle: _seasonTabLabel(season),
          episodes: episodesForPage,
          index: index,
        ),
        transitionsBuilder: (_, animation, __, child) {
          final eased = CurvedAnimation(
            parent: animation,
            curve: Curves.easeOutCubic,
            reverseCurve: Curves.easeInCubic,
          );
          return FadeTransition(
            opacity: eased,
            child: SlideTransition(
              position: Tween<Offset>(
                begin: const Offset(0, .035),
                end: Offset.zero,
              ).animate(eased),
              child: child,
            ),
          );
        },
      ),
    );
  }

  Future<void> _warmTopMovieServer(
    List<Map<String, dynamic>> servers,
    String pageUrl,
  ) async {
    final prioritized = prioritizeServerRowsForProbe(servers);
    final candidate = prioritized.firstWhere(
      (s) =>
          _isUsablePlaybackUrl(_text(s['directStreamUrl']).trim()) ||
          _isUsablePlaybackUrl(_text(s['embedUrl']).trim()),
      orElse: () => const <String, dynamic>{},
    );
    if (candidate.isEmpty) return;
    final direct = _text(candidate['directStreamUrl']).trim();
    if (_isUsablePlaybackUrl(direct) && candidate['playableVerified'] == true) {
      return;
    }
    final referer = _text(candidate['sourcePageUrl']).ifEmpty(pageUrl);
    final resolvedMeta = await _resolveServerStreamDetail(
      candidate,
      referer: referer,
    );
    if (resolvedMeta == null || !mounted) return;
    final resolvedUrl = _text(resolvedMeta['url']).trim();
    final resolvedRef = _text(resolvedMeta['referer']).trim();
    if (!_isUsablePlaybackUrl(resolvedUrl)) return;
    final embedKey = _text(candidate['embedUrl']).trim();
    final updated = {
      ...candidate,
      'directStreamUrl': resolvedUrl,
      if (resolvedRef.isNotEmpty) 'resolvedReferer': resolvedRef,
      'playableVerified': true,
    };
    setState(() {
      _serverRows = _mergeServerRows([
        updated,
        ..._serverRows.where((r) => _text(r['embedUrl']).trim() != embedKey),
      ]);
    });
  }

  // ── يجرّب كل السيرفرات ويرجع أول واحد شغّال بأقصى سرعة ─────
  Future<Map<String, dynamic>?> _findWorkingServer(
    List<Map<String, dynamic>> servers,
    String referer, {
    bool retryDiscoveredSources = true,
  }) => _findBestWorkingServer(
    servers: servers,
    referer: referer,
    retryExtraFuture: _providerBackupFuture,
    allowRetryExtra: retryDiscoveredSources,
  );

  Widget _serverSection(
    List<Map<String, dynamic>> servers,
    String referer,
    String title, {
    bool loading = false,
  }) => _SectionCard(
    title: 'المشاهدة',
    icon: '▶️',
    child: servers.isEmpty
        ? loading
              ? _serverLoadingSkeleton()
              : const Padding(
                  padding: EdgeInsets.symmetric(vertical: 10),
                  child: Text(
                    'لم يُعثر على رابط مشاهدة صالح لهذا العمل حاليًا.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: _muted, fontSize: 13),
                  ),
                )
        : Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // ── زر "شاهد الآن" يجرّب السيرفرات ويفتح الشغّال ─────────
              _SmartPlayButton(
                servers: servers,
                loading: loading,
                onPlay: (server) => _openServer(server, title, referer),
                findWorking: (s) => _findWorkingServer(s, referer),
              ),
            ],
          ),
  );

  Widget _serverLoadingSkeleton() => _ShimmerLoading(
    child: Column(
      children: List.generate(
        3,
        (_) => Container(
          height: 48,
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.symmetric(horizontal: 13),
          decoration: BoxDecoration(
            color: const Color(0xFFEADBCB),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: _line),
          ),
          child: const Row(
            children: [
              _SkeletonBlock(width: 68, height: 9),
              Spacer(),
              _SkeletonBlock(width: 84, height: 11),
            ],
          ),
        ),
      ),
    ),
  );

  Future<void> _playItem() async {
    if (_isSeriesEffective) {
      final season = _seasons.isEmpty
          ? null
          : _seasons[_seasonIndex.clamp(0, _seasons.length - 1)];
      final episodes = _orderedEpisodes(season?['episodes']);
      if (episodes.isNotEmpty) {
        _openEpisode(episodes, _currentEpisodeIndex(episodes) ?? 0, season);
        return;
      }
      if (_servers.isNotEmpty) {
        final selected = await _findWorkingServer(
          _servers,
          widget.item.pageUrl,
        );
        if (!mounted) return;
        if (selected != null) {
          await _openServer(selected, widget.item.title, widget.item.pageUrl);
          return;
        }
      }
      _message(context, 'لا توجد حلقات جاهزة للتشغيل بعد.');
      return;
    }

    if (_servers.isEmpty) {
      _message(
        context,
        _serversLoading
            ? 'جاري تجهيز رابط المشاهدة…'
            : 'لم يُعثر على رابط مشاهدة صالح لهذا العمل حاليًا.',
      );
      return;
    }

    final selected = await _findWorkingServer(_servers, widget.item.pageUrl);
    if (!mounted) return;
    if (selected == null) {
      _message(context, 'لم يستجب أي رابط فيديو صالح لهذا العمل.');
      return;
    }
    await _openServer(selected, widget.item.title, widget.item.pageUrl);
  }

  Future<void> _openServer(
    Map<String, dynamic> server,
    String title,
    String referer,
  ) async {
    final direct = _text(server['directStreamUrl']).trim();
    final embed = _text(server['embedUrl']).trim();
    final sourcePage = _text(server['sourcePageUrl']).ifEmpty(referer);

    final candidateUrl = _isUsablePlaybackUrl(direct) ? direct : embed;

    // إذا كان السيرفر محقَّقاً مسبقاً استخدم directStreamUrl مباشرة
    // وإلا حاول الـ resolve — لو فشل استخدم candidateUrl كـ embed في WebView
    final String url;
    String resolvedReferer = _text(server['resolvedReferer']).trim();
    final bool candidateEmbedVerified;
    if (server['playableVerified'] == true && _isUsablePlaybackUrl(direct)) {
      url = direct;
      candidateEmbedVerified = true;
    } else if (server['embedAlive'] == true &&
        _isUsablePlaybackUrl(candidateUrl)) {
      // Already verified alive inside _findWorkingServer; avoid wasting a second network roundtrip.
      url = candidateUrl;
      candidateEmbedVerified = true;
    } else {
      final resolvedMeta = await _resolveServerStreamDetail({
        ...server,
        'directStreamUrl': '',
        'embedUrl': candidateUrl,
      }, referer: sourcePage);
      final resolvedViaStream = _text(resolvedMeta?['url']).trim();
      if (resolvedRefOrEmpty(resolvedMeta).isNotEmpty) {
        resolvedReferer = resolvedRefOrEmpty(resolvedMeta);
      }
      if (_isUsablePlaybackUrl(resolvedViaStream)) {
        url = resolvedViaStream;
        candidateEmbedVerified = true;
      } else {
        url = '';
        candidateEmbedVerified = await _probePlayableEmbedUrl(
          candidateUrl,
          referer: sourcePage,
        );
      }
    }
    if (!mounted) return;

    // استخدم url المُحلَّل إذا كان صالحاً، وإلا اقبل candidateUrl فقط إذا ثبت أن صفحة السيرفر حية
    final playableUrl = _isUsablePlaybackUrl(url)
        ? url
        : (candidateEmbedVerified && _isUsablePlaybackUrl(candidateUrl)
              ? candidateUrl
              : '');
    if (!_isUsablePlaybackUrl(playableUrl)) {
      final failedUrl = candidateUrl;
      final alternatives =
          _mergeServerRows([..._servers, ..._maps(server['fallbackServers'])])
              .where((candidate) {
                final directUrl = _text(candidate['directStreamUrl']).trim();
                final candidateKey = directUrl.isNotEmpty
                    ? directUrl
                    : _text(candidate['embedUrl']).trim();
                return candidateKey.isNotEmpty && candidateKey != failedUrl;
              })
              .toList(growable: false);
      final alternate = await _findWorkingServer(alternatives, sourcePage);
      if (!mounted) return;
      if (alternate != null) {
        await _openServer(alternate, title, referer);
        return;
      }
      _message(context, 'لم يتم التحقق من رابط فيديو صالح لهذا المصدر.');
      return;
    }
    final playerReferer = resolvedReferer.isNotEmpty
        ? resolvedReferer
        : embed.isNotEmpty
        ? embed
        : sourcePage;

    if (!mounted) return;

    final verifiedServer = {
      ...server,
      'directStreamUrl': playableUrl,
      if (resolvedReferer.isNotEmpty) 'resolvedReferer': resolvedReferer,
      'playableVerified': true,
    };

    // افتح البلاير فوراً بالمصادر التي جُمعت أثناء تحميل صفحة التفاصيل.
    // البحث الشبكي الإضافي هنا كان يؤخر الفتح ثم يرمي نتائجه بعد انتهاء المهلة.
    final immediateBackups = _playerFallbackSources(
      servers: _mergeServerRows([
        ..._servers,
        ..._maps(server['fallbackServers']),
      ]),
      current: verifiedServer,
      pageReferer: sourcePage,
    );

    if (!mounted) return;
    await _launchPlayer(
      context,
      playableUrl,
      title,
      referer: playerReferer,
      backupSources: immediateBackups,
    );
  }
}

class _DetailHeader extends StatelessWidget {
  const _DetailHeader({
    required this.item,
    this.isSeries = false,
    this.episodeCount = 0,
    this.seasonCount = 0,
  });
  final MediaItem item;
  final bool isSeries;
  final int episodeCount;
  final int seasonCount;
  @override
  Widget build(BuildContext context) {
    final effectiveSeries = isSeries || item.isSeries;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(16),
          child: SizedBox(
            width: 126,
            height: 184,
            child: _NetworkImage(url: item.poster),
          ),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(top: 7),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.parentSeriesTitle.trim().isNotEmpty
                      ? item.parentSeriesTitle.trim()
                      : item.title,
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: _ink,
                    fontSize: 20,
                    fontWeight: FontWeight.w900,
                    height: 1.3,
                  ),
                ),
                if (item.parentSeriesTitle.trim().isNotEmpty &&
                    item.title != item.parentSeriesTitle) ...[
                  const SizedBox(height: 4),
                  Text(
                    item.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(color: _muted, fontSize: 12),
                  ),
                ],
                if (effectiveSeries && episodeCount > 0) ...[
                  const SizedBox(height: 6),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 9,
                      vertical: 3.5,
                    ),
                    decoration: BoxDecoration(
                      color: _brick.withOpacity(0.12),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: _brick.withOpacity(0.35)),
                    ),
                    child: Text(
                      '📺 عدد الحلقات: $episodeCount حلقة' +
                          (seasonCount > 1 ? ' · $seasonCount مواسم' : ''),
                      style: const TextStyle(
                        color: _brick,
                        fontWeight: FontWeight.w800,
                        fontSize: 12,
                      ),
                    ),
                  ),
                ],
                const SizedBox(height: 8),
                Text(
                  [
                    if (item.year.isNotEmpty) item.year,
                    if (item.quality.isNotEmpty) item.quality,
                    effectiveSeries
                        ? episodeCount > 0
                              ? 'مسلسل ($episodeCount حلقة)'
                              : 'مسلسل'
                        : 'فيلم',
                    item.visibleProvider,
                  ].join(' · '),
                  style: const TextStyle(
                    color: _muted,
                    height: 1.5,
                    fontSize: 12,
                  ),
                ),
                if (item.latestEpisodeLabel.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Text(
                    item.latestEpisodeLabel,
                    style: const TextStyle(
                      color: _brick,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
                if (item.rating.isNotEmpty) ...[
                  const SizedBox(height: 9),
                  Row(
                    children: [
                      const Icon(
                        Icons.star_rounded,
                        size: 18,
                        color: Color(0xFFD79A31),
                      ),
                      const SizedBox(width: 3),
                      Text(
                        item.rating,
                        style: const TextStyle(
                          color: _ink,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({
    required this.title,
    required this.icon,
    required this.child,
    this.trailing,
  });
  final String title, icon;
  final Widget child;
  final Widget? trailing;
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(
      color: _card,
      borderRadius: BorderRadius.circular(17),
      border: Border.all(color: _line),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        LayoutBuilder(
          builder: (context, constraints) {
            final narrow = constraints.maxWidth < 460;
            final header = Text(
              '$icon  $title',
              maxLines: narrow ? 2 : 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: _muted,
                fontSize: 16,
                fontWeight: FontWeight.w800,
              ),
            );
            if (trailing == null) return header;
            if (narrow) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  header,
                  const SizedBox(height: 4),
                  Align(
                    alignment: AlignmentDirectional.centerStart,
                    child: trailing!,
                  ),
                ],
              );
            }
            return Row(
              children: [
                Expanded(flex: 5, child: header),
                const SizedBox(width: 8),
                Expanded(
                  flex: 4,
                  child: Align(
                    alignment: AlignmentDirectional.centerStart,
                    child: trailing!,
                  ),
                ),
              ],
            );
          },
        ),
        const SizedBox(height: 12),
        child,
      ],
    ),
  );
}

// ── صف السيرفرات البديلة (مطوية تحت الزر الرئيسي) ───────────────────────────
class _AlternativeServersRow extends StatefulWidget {
  const _AlternativeServersRow({required this.servers, required this.onTap});
  final List<Map<String, dynamic>> servers;
  final void Function(Map<String, dynamic>) onTap;

  @override
  State<_AlternativeServersRow> createState() => _AlternativeServersRowState();
}

class _AlternativeServersRowState extends State<_AlternativeServersRow> {
  bool _expanded = false;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      // زر "تغيير السيرفر"
      GestureDetector(
        onTap: () => setState(() => _expanded = !_expanded),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              _expanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
              color: _muted,
              size: 18,
            ),
            const SizedBox(width: 4),
            Text(
              _expanded
                  ? 'إخفاء السيرفرات'
                  : 'تغيير السيرفر (${widget.servers.length})',
              style: const TextStyle(
                color: _muted,
                fontSize: 12,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
      // قائمة السيرفرات
      if (_expanded) ...[
        const SizedBox(height: 8),
        ...widget.servers.map(
          (server) =>
              _ServerTile(server: server, onTap: () => widget.onTap(server)),
        ),
      ],
    ],
  );
}

// ── _ServerTile (ثابت — يُستخدم في القائمة المطوية) ─────────────────────────
class _ServerTile extends StatelessWidget {
  const _ServerTile({required this.server, required this.onTap});
  final Map<String, dynamic> server;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: Material(
      color: _card,
      borderRadius: BorderRadius.circular(13),
      child: InkWell(
        borderRadius: BorderRadius.circular(13),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(13),
            border: Border.all(color: _line),
          ),
          child: Row(
            children: [
              const Icon(Icons.play_circle_fill, color: _brick),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  _text(server['name']).ifEmpty('سيرفر مشاهدة'),
                  style: const TextStyle(
                    color: _ink,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const Text(
                'سريع',
                style: TextStyle(color: Color(0xFF40937B), fontSize: 12),
              ),
              const SizedBox(width: 8),
              const Icon(Icons.chevron_left, color: _brick),
            ],
          ),
        ),
      ),
    ),
  );
}

// ── Smart Play Button — يجرّب السيرفرات ويفتح الشغّال تلقائياً ──────────────
// ── قائمة سيرفرات مطوية (تظهر بس لما المستخدم يضغط "تغيير السيرفر") ──────────
class _CollapsedServerList extends StatefulWidget {
  const _CollapsedServerList({required this.servers, required this.onTap});
  final List<Map<String, dynamic>> servers;
  final void Function(Map<String, dynamic>) onTap;

  @override
  State<_CollapsedServerList> createState() => _CollapsedServerListState();
}

class _CollapsedServerListState extends State<_CollapsedServerList> {
  bool _expanded = false;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      GestureDetector(
        onTap: () => setState(() => _expanded = !_expanded),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                _expanded ? Icons.expand_less : Icons.expand_more,
                color: _muted,
                size: 16,
              ),
              const SizedBox(width: 4),
              Text(
                _expanded
                    ? 'إخفاء السيرفرات'
                    : 'تغيير السيرفر (${widget.servers.length})',
                style: const TextStyle(
                  color: _muted,
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ),
      if (_expanded) ...[
        const SizedBox(height: 6),
        ...widget.servers.map(
          (server) =>
              _ServerTile(server: server, onTap: () => widget.onTap(server)),
        ),
      ],
    ],
  );
}

class _SmartPlayButton extends StatefulWidget {
  const _SmartPlayButton({
    required this.servers,
    required this.loading,
    required this.onPlay,
    required this.findWorking,
  });
  final List<Map<String, dynamic>> servers;
  final bool loading;
  final void Function(Map<String, dynamic> server) onPlay;
  final Future<Map<String, dynamic>?> Function(List<Map<String, dynamic>>)
  findWorking;

  @override
  State<_SmartPlayButton> createState() => _SmartPlayButtonState();
}

class _SmartPlayButtonState extends State<_SmartPlayButton> {
  bool _testing = false;
  String _status = '';

  Future<void> _play() async {
    if (_testing || widget.loading) return;
    setState(() {
      _testing = true;
      _status = 'جاري فتح المشغّل…';
    });

    Map<String, dynamic>? selected;
    try {
      selected = await widget.findWorking(widget.servers);
    } catch (_) {}
    if (!mounted) return;
    setState(() {
      _testing = false;
      _status = '';
    });
    if (widget.servers.isEmpty) return;

    if (selected != null) {
      widget.onPlay(selected);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('لم يستجب أي رابط فيديو صالح لهذا العمل.'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final busy = _testing || widget.loading;
    return Material(
      color: _brick,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: busy ? null : _play,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 15),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (busy)
                const SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.5,
                    color: Colors.white,
                  ),
                )
              else
                const Icon(
                  Icons.play_arrow_rounded,
                  color: Colors.white,
                  size: 28,
                ),
              const SizedBox(width: 10),
              Text(
                busy
                    ? (_status.isEmpty ? 'جاري التحضير…' : _status)
                    : 'شاهد الآن',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
