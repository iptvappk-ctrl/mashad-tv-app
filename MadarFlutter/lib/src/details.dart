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
  String? _error;
  int _seasonIndex = 0, _request = 0;
  int? _manualSeasonNumber;
  String? _downloadingMediaUrl;
  final Set<String> _refreshingSeasonPages = <String>{};

  @override
  void initState() {
    super.initState();
    unawaited(_load());
  }

  Future<void> _load({bool force = false}) async {
    final request = ++_request;
    setState(() {
      _loading = true;
      _serversLoading = true;
      _seasonsLoading = true;
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
        _bundle = cached[1];
        _serverRows = _maps(cached[0]?['servers']);
        _loading = _detail == null;
        _serversLoading = _serverRows.isEmpty;
        _seasonsLoading = _bundle == null;
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
        setState(() => _seasonsLoading = false);
        return;
      }
      final hasEpisodes = _maps(bundle['seasons'])
          .any((season) => _maps(season['episodes']).isNotEmpty);
      final hasParts = _maps(bundle['relatedParts']).isNotEmpty;
      final backgroundRefreshing = bundle['backgroundRefreshing'] == true;
      setState(() {
        if (hasEpisodes || hasParts) {
          // Provider enrichment is incremental. Do not let a fast but partial
          // response erase episodes already collected for an earlier season.
          _bundle = _mergeSeasonBundles(_bundle, bundle);
          _seasonIndex = _seasonIndexFor(
            _bundle!,
            preferredNumber: _manualSeasonNumber,
          );
        }
        _seasonsLoading = backgroundRefreshing;
      });
      _refreshCurrentSeasonIfSparse();
      if (backgroundRefreshing) {
        unawaited(_pollSeasonCache(request, args));
      }
    } catch (error) {
      if (mounted && request == _request) {
        setState(() {
          if (_bundle == null) _error ??= '$error';
          _seasonsLoading = false;
        });
      }
    }
  }

  Future<void> _pollSeasonCache(int request, Map<String, Object?> args) async {
    try {
      for (var attempt = 0; attempt < 20; attempt++) {
        await Future<void>.delayed(const Duration(seconds: 1));
        if (!mounted || request != _request) return;
        final cached = await NativeBridge.map('media.seasons.cached', args);
        if (cached != null) {
          final hasEpisodes = _maps(cached['seasons'])
              .any((season) => _maps(season['episodes']).isNotEmpty);
          final hasParts = _maps(cached['relatedParts']).isNotEmpty;
          if (hasEpisodes || hasParts) {
            final merged = _mergeSeasonBundles(_bundle, cached);
            if (_seasonBundleSignature(merged) !=
                _seasonBundleSignature(_bundle)) {
              setState(() {
                _bundle = merged;
                _seasonIndex = _seasonIndexFor(
                  merged,
                  preferredNumber: _manualSeasonNumber,
                );
              });
              _refreshCurrentSeasonIfSparse();
            }
          }
        }
        final status = await NativeBridge.map('media.seasons.status', args);
        if (status?['backgroundRefreshing'] != true) break;
      }
    } catch (_) {
      // Keep already loaded detail data visible if background enrichment fails.
    } finally {
      if (mounted && request == _request) {
        setState(() => _seasonsLoading = false);
      }
    }
  }

  String _seasonBundleSignature(Map<String, dynamic>? bundle) {
    if (bundle == null) return '';
    final seasons = _maps(bundle['seasons'])
      ..sort((a, b) => _integer(a['number']).compareTo(_integer(b['number'])));
    return seasons
        .map((season) {
          final episodes = _maps(season['episodes'])
            ..sort((a, b) => _episodeNumber(a).compareTo(_episodeNumber(b)));
          return '${_integer(season['number'])}:'
              '${episodes.map(_episodeNumber).join(',')}';
        })
        .join('|');
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
    if (mounted) setState(() {});
    try {
      final fresh = await NativeBridge.map('media.season.page.fetch', {
        'item': widget.item.toMap(),
        'seasonUrl': seasonUrl,
        'seasonNumber': seasonNumber,
      });
      if (!mounted || request != _request || fresh == null) return;
      final merged = _mergeSeasonBundles(_bundle, fresh);
      if (_seasonBundleSignature(merged) != _seasonBundleSignature(_bundle)) {
        setState(() {
          _bundle = merged;
          _seasonIndex = _seasonIndexFor(merged, preferredNumber: seasonNumber);
        });
      }
    } catch (_) {
      // Keep the episodes already shown if an individual season page is unavailable.
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
    final key = 'episode:servers:$url';
    try {
      final cached = await NativeBridge.readCache(key);
      if (cached != null && mounted && request == _request) {
        final rows = _maps(jsonDecode(cached));
        if (rows.isNotEmpty) {
          setState(() {
            _serverRows = rows;
            _serversLoading = false;
          });
        }
      }
    } catch (_) {}
    if (!mounted || request != _request) return;
    try {
      final rows = await NativeBridge.list('media.servers', {
        'url': url,
        'provider': widget.item.provider,
      });
      if (!mounted || request != _request) return;
      setState(() {
        if (rows.isNotEmpty) _serverRows = rows;
        _serversLoading = false;
      });
      if (rows.isNotEmpty) {
        unawaited(NativeBridge.writeCache(key, jsonEncode(rows)));
      }
    } catch (_) {
      if (mounted && request == _request) {
        setState(() => _serversLoading = false);
      }
    }
  }

  List<Map<String, dynamic>> get _seasons => _maps(_bundle?['seasons']);
  List<Map<String, dynamic>> get _parts => _maps(_bundle?['relatedParts']);
  List<Map<String, dynamic>> get _servers =>
      _serverRows.isNotEmpty ? _serverRows : _maps(_detail?['servers']);

  String get _seriesTitle => widget.item.parentSeriesTitle.trim().isNotEmpty
      ? widget.item.parentSeriesTitle.trim()
      : widget.item.title;

  bool get _isEpisodeCard => widget.item.parentSeriesPageUrl.trim().isNotEmpty;

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
    return 0;
  }

  int? _currentEpisodeIndex(List<Map<String, dynamic>> episodes) {
    final currentUrl = _normalizedMediaUrl(widget.item.pageUrl);
    final exact = episodes.indexWhere(
      (episode) => _normalizedMediaUrl(_text(episode['pageUrl'])) == currentUrl,
    );
    if (exact >= 0) return exact;
    if (!_isEpisodeCard) return null;
    final itemSeasonNumber =
        _maps(_bundle?['seasons'])
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
    final selectedSeasonNumber = _seasons.isEmpty
        ? null
        : _integer(
            _seasons[_seasonIndex.clamp(0, _seasons.length - 1)]['number'],
          );
    final sameKnownSeason =
        itemSeasonNumber != null &&
        selectedSeasonNumber != null &&
        itemSeasonNumber == selectedSeasonNumber;
    final onlySeasonCanBeInferred =
        itemSeasonNumber == null && _seasons.length == 1;
    if (!sameKnownSeason && !onlySeasonCanBeInferred) {
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
    if (!_isEpisodeCard) return episodes.isEmpty ? null : episodes.last;
    final currentUrl = _normalizedMediaUrl(widget.item.pageUrl);
    return episodes
            .where(
              (episode) =>
                  _normalizedMediaUrl(_text(episode['pageUrl'])) == currentUrl,
            )
            .firstOrNull ??
        {
          'title': widget.item.title,
          'number':
              _episodeNumberFrom(
                '${widget.item.title} ${widget.item.latestEpisodeLabel} ${widget.item.pageUrl}',
              ) ??
              0,
          'pageUrl': widget.item.pageUrl,
        };
  }

  @override
  Widget build(BuildContext context) {
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
    return Scaffold(
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
            _DetailHeader(item: widget.item),
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
                    onPressed: () => _load(),
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
            if (widget.item.isSeries) ...[
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
              const SizedBox(height: 12),
              _downloadMovieCard(),
              const SizedBox(height: 13),
              _serverSection(
                _servers,
                widget.item.pageUrl,
                widget.item.title,
                loading: _serversLoading,
              ),
            ],
            if (widget.item.isSeries &&
                !_isEpisodeCard &&
                downloadEpisode != null) ...[
              const SizedBox(height: 12),
              _downloadEpisodeCard(downloadEpisode, season),
            ],
            if (_parts.isNotEmpty) ...[
              const SizedBox(height: 13),
              _SectionCard(
                title: widget.item.isSeries
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
            if (_loading && _detail == null)
              const Padding(
                padding: EdgeInsets.all(20),
                child: Center(child: CircularProgressIndicator(color: _brick)),
              ),
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

  Widget _seasonsSection(
    Map<String, dynamic>? selected,
    List<Map<String, dynamic>> episodes,
  ) => _SectionCard(
    title: 'حلقات هذا الجزء / الموسم',
    icon: '📺',
    trailing: Text(
      '${episodes.length} حلقة',
      style: const TextStyle(color: _muted, fontSize: 12),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (_seasons.isNotEmpty) ...[
          SizedBox(
            height: 42,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: _seasons.length,
              separatorBuilder: (_, __) => const SizedBox(width: 7),
              itemBuilder: (_, index) {
                final season = _seasons[index];
                final label = _text(season['title'])
                    .ifEmpty('الموسم ${_text(season['number'])}');
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
        ],
        if ((_seasonsLoading && episodes.isNotEmpty) ||
            (_refreshingSeasonPages.contains(_text(selected?['pageUrl'])) &&
                episodes.isNotEmpty))
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Row(
              children: [
                const SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: _brick,
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    _refreshingSeasonPages.contains(_text(selected?['pageUrl']))
                        ? 'بنجلب حلقات هذا الموسم من صفحته مباشرة…'
                        : 'بنكمّل البحث عن المواسم والحلقات من المصادر الأخرى…',
                    style: const TextStyle(color: _muted, fontSize: 12),
                  ),
                ),
              ],
            ),
          ),
        if (episodes.isEmpty)
          Text(
            (_seasonsLoading ||
                    _refreshingSeasonPages.contains(
                      _text(selected?['pageUrl']),
                    ))
                ? 'بنحمّل الحلقات في الخلفية…'
                : 'المصدر لم يرجع حلقات لهذا الجزء.',
            style: const TextStyle(color: _muted),
          )
        else
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: List.generate(episodes.length, (index) {
              final episode = episodes[index];
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
      ],
    ),
  );

  Widget _downloadEpisodeCard(
    Map<String, dynamic> episode,
    Map<String, dynamic>? season,
  ) {
    final url = _text(episode['pageUrl']);
    final number = _episodeNumber(episode);
    final seasonTitle = _text(season?['title'])
        .ifEmpty('الموسم ${_text(season?['number'])}');
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
        final cacheKey = 'episode:servers:$pageUrl';
        try {
          final cached = await NativeBridge.readCache(cacheKey);
          if (cached != null) servers = _maps(jsonDecode(cached));
        } catch (_) {}
        if (servers.isEmpty) {
          servers = await NativeBridge.list('media.servers', {
            'url': pageUrl,
            'provider': widget.item.provider,
          });
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
      final server = servers.firstWhere(
        (row) => _isUsablePlaybackUrl(_text(row['directStreamUrl'])),
        orElse: () => servers.first,
      );
      final streamUrl = await _resolveServerStream(server);
      if (!mounted) return;
      if (streamUrl.isEmpty) {
        _message(context, 'المصدر لم يوفر رابط تنزيل مباشر لهذا الفيلم.');
        return;
      }
      final started =
          await _native.invokeMethod<bool>('download.start', {
            'url': _absolute(streamUrl, pageUrl),
            'title': widget.item.title,
            'referer': pageUrl,
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
      final cacheKey = 'episode:servers:$url';
      List<Map<String, dynamic>> servers = [];
      try {
        final cached = await NativeBridge.readCache(cacheKey);
        if (cached != null) servers = _maps(jsonDecode(cached));
      } catch (_) {}
      if (servers.isEmpty) {
        servers = await NativeBridge.list('media.servers', {
          'url': url,
          'provider': _text(episode['providerName'])
              .ifEmpty(widget.item.provider),
        });
        if (servers.isNotEmpty) {
          unawaited(NativeBridge.writeCache(cacheKey, jsonEncode(servers)));
        }
      }
      if (!mounted) return;
      if (servers.isEmpty) {
        _message(context, 'لا توجد روابط تنزيل متاحة لهذه الحلقة الآن.');
        return;
      }
      final server = servers.firstWhere(
        (row) => _isUsablePlaybackUrl(_text(row['directStreamUrl'])),
        orElse: () => servers.first,
      );
      final streamUrl = await _resolveServerStream(server);
      if (!mounted) return;
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
            'referer': url,
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
    Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => EpisodePage(
          seriesTitle: _seriesTitle,
          provider: _text(episodes[index]['providerName'])
              .ifEmpty(widget.item.provider),
          seasonTitle: _text(season?['title'])
              .ifEmpty('الموسم ${_text(season?['number'])}'),
          episodes: episodes,
          index: index,
        ),
      ),
    );
  }

  Widget _serverSection(
    List<Map<String, dynamic>> servers,
    String referer,
    String title, {
    bool loading = false,
  }) => _SectionCard(
    title: 'اختر سيرفر المشاهدة',
    icon: '▶️',
    child: servers.isEmpty
        ? Text(
            loading
                ? 'بنحمّل سيرفرات المشاهدة في الخلفية…'
                : 'لا توجد سيرفرات متاحة لهذا العمل الآن.',
            style: const TextStyle(color: _muted),
          )
        : Column(
            children: servers
                .map(
                  (server) => _ServerTile(
                    server: server,
                    onTap: () => _openServer(server, title, referer),
                  ),
                )
                .toList(),
          ),
  );

  Future<void> _playItem() async {
    if (widget.item.isSeries) {
      final season = _seasons.isEmpty
          ? null
          : _seasons[_seasonIndex.clamp(0, _seasons.length - 1)];
      final episodes = _orderedEpisodes(season?['episodes']);
      if (episodes.isEmpty) {
        _message(context, 'لا توجد حلقات جاهزة للتشغيل بعد.');
        return;
      }
      _openEpisode(episodes, _currentEpisodeIndex(episodes) ?? 0, season);
      return;
    }
    if (_servers.isEmpty) {
      _message(context, 'جاري تجهيز سيرفرات المشاهدة…');
      return;
    }
    await _openServer(_servers.first, widget.item.title, widget.item.pageUrl);
  }

  Future<void> _openServer(
    Map<String, dynamic> server,
    String title,
    String referer,
  ) async {
    final url = await _resolveServerStream(server);
    if (!mounted) return;
    if (url.isEmpty) {
      _message(context, 'السيرفر لم يوفر رابط فيديو مباشر. جرّب سيرفرًا آخر.');
      return;
    }
    await _launchPlayer(context, url, title, referer: referer);
  }
}

class _DetailHeader extends StatelessWidget {
  const _DetailHeader({required this.item});
  final MediaItem item;
  @override
  Widget build(BuildContext context) => Row(
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
              const SizedBox(height: 10),
              Text(
                [
                  if (item.year.isNotEmpty) item.year,
                  if (item.quality.isNotEmpty) item.quality,
                  item.isSeries ? 'مسلسل' : 'فيلم',
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
        Row(
          children: [
            Text(
              '$icon  $title',
              style: const TextStyle(
                color: _muted,
                fontSize: 16,
                fontWeight: FontWeight.w800,
              ),
            ),
            const Spacer(),
            if (trailing != null) trailing!,
          ],
        ),
        const SizedBox(height: 12),
        child,
      ],
    ),
  );
}

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
