part of '../main.dart';

class RootTabs extends StatefulWidget {
  const RootTabs({super.key});
  @override
  State<RootTabs> createState() => _RootTabsState();
}

class _RootTabsState extends State<RootTabs> with WidgetsBindingObserver {
  int _tab = 0;
  final Map<String, MediaItem> _favorites = {};
  final Map<String, MediaItem> _seenItems = {};
  bool _sportsBuilt = false;
  bool _recapsBuilt = false;
  bool _notificationStatusLoaded = false;
  bool _notificationsDisabled = false;
  bool _notificationBannerDismissed = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    unawaited(_restoreFavorites());
    unawaited(_refreshNotificationStatus());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_refreshNotificationStatus());
    }
  }

  Future<void> _refreshNotificationStatus() async {
    final bool enabled;
    try {
      enabled = await NativeBridge.notificationsEnabled();
    } catch (_) {
      // Notification status is optional; a platform-side failure must not
      // interfere with the catalog's first frame or app navigation.
      return;
    }
    if (!mounted) return;
    setState(() {
      _notificationStatusLoaded = true;
      _notificationsDisabled = !enabled;
    });
  }

  Future<void> _openNotificationSettings() async {
    await NativeBridge.openNotificationSettings();
  }

  Future<void> _requestNotificationPermission() async {
    await NativeBridge.requestNotificationPermission();
  }

  Future<void> _restoreFavorites() async {
    final String? saved;
    try {
      saved = await NativeBridge.readCache('favorites:items');
    } catch (_) {
      return;
    }
    if (saved == null) return;
    try {
      final rows = jsonDecode(saved) as List;
      for (final row in rows.whereType<Map>()) {
        final item = MediaItem.fromMap(Map<String, dynamic>.from(row));
        _favorites[item.pageUrl] = item;
        _seenItems[item.pageUrl] = item;
      }
      if (mounted) setState(() {});
    } catch (_) {}
  }

  void _toggle(MediaItem item) {
    setState(() {
      if (_favorites.remove(item.pageUrl) == null)
        _favorites[item.pageUrl] = item;
    });
    unawaited(
      NativeBridge.writeCache(
        'favorites:items',
        jsonEncode(_favorites.values.map((e) => e.toMap()).toList()),
      ),
    );
  }

  void _remember(List<MediaItem> rows) {
    for (final row in rows) {
      if (row.pageUrl.isNotEmpty) _seenItems[row.pageUrl] = row;
    }
  }

  void _open(MediaItem item) {
    Navigator.of(context).push<void>(
      PageRouteBuilder<void>(
        transitionDuration: const Duration(milliseconds: 250),
        reverseTransitionDuration: const Duration(milliseconds: 190),
        pageBuilder: (_, animation, __) => FadeTransition(
          opacity: animation,
          child: SlideTransition(
            position:
                Tween<Offset>(
                  begin: const Offset(0, .025),
                  end: Offset.zero,
                ).animate(
                  CurvedAnimation(
                    parent: animation,
                    curve: Curves.easeOutCubic,
                  ),
                ),
            child: DetailPage(item: item, onOpenItem: _open),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final all = {..._seenItems, ..._favorites}.values.toList();
    final pages = <Widget>[
      CatalogPage(
        key: const PageStorageKey<String>('catalog-tab'),
        favoriteUrls: _favorites.keys.toSet(),
        onOpen: _open,
        onToggle: _toggle,
        onItems: _remember,
      ),
      _recapsBuilt
          ? const RecapsPage(key: PageStorageKey<String>('recaps-tab'))
          : const SizedBox.shrink(
              key: PageStorageKey<String>('recaps-placeholder'),
            ),
      _sportsBuilt
          ? const SportsPage(key: PageStorageKey<String>('sports-tab'))
          : const SizedBox.shrink(
              key: PageStorageKey<String>('sports-placeholder'),
            ),
      FavoritesPage(
        key: const PageStorageKey<String>('favorites-tab'),
        items: all,
        favoriteUrls: _favorites.keys.toSet(),
        onOpen: _open,
        onToggle: _toggle,
      ),
    ];
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            if (_notificationStatusLoaded &&
                _notificationsDisabled &&
                !_notificationBannerDismissed)
              MaterialBanner(
                leading: const Icon(Icons.notifications_off_outlined),
                content: const Text(
                  'فعّل الإشعارات لتصلك الأعمال الجديدة وتنبيهات المباريات والتطبيق مغلق.',
                ),
                actions: [
                  TextButton(
                    onPressed: _requestNotificationPermission,
                    child: const Text('طلب الإذن'),
                  ),
                  TextButton(
                    onPressed: _openNotificationSettings,
                    child: const Text('الإعدادات'),
                  ),
                  TextButton(
                    onPressed: () =>
                        setState(() => _notificationBannerDismissed = true),
                    child: const Text('لاحقًا'),
                  ),
                ],
              ),
            Expanded(
              child: IndexedStack(
                index: _tab,
                sizing: StackFit.expand,
                children: List.generate(
                  pages.length,
                  (index) => TickerMode(
                    enabled: _tab == index,
                    child: AnimatedOpacity(
                      opacity: _tab == index ? 1 : 0,
                      duration: const Duration(milliseconds: 170),
                      child: AnimatedSlide(
                        offset: _tab == index
                            ? Offset.zero
                            : const Offset(0, .012),
                        duration: const Duration(milliseconds: 190),
                        curve: Curves.easeOutCubic,
                        child: ColoredBox(
                          color: _paper,
                          child: SizedBox.expand(child: pages[index]),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        height: 72,
        onDestinationSelected: (index) => setState(() {
          _tab = index;
          if (index == 1) _recapsBuilt = true;
          if (index == 2) _sportsBuilt = true;
        }),
        destinations: const [
          NavigationDestination(
            icon: Text('🎬', style: TextStyle(fontSize: 21)),
            label: 'أفلام ومسلسلات',
          ),
          NavigationDestination(
            icon: Text('🍿', style: TextStyle(fontSize: 21)),
            label: 'ملخصات',
          ),
          NavigationDestination(
            icon: Text('⚽', style: TextStyle(fontSize: 21)),
            label: 'بثات الكورة',
          ),
          NavigationDestination(
            icon: Text('❤️', style: TextStyle(fontSize: 21)),
            label: 'المفضلة',
          ),
        ],
      ),
    );
  }
}

class CatalogPage extends StatefulWidget {
  const CatalogPage({
    super.key,
    required this.favoriteUrls,
    required this.onOpen,
    required this.onToggle,
    required this.onItems,
  });
  final Set<String> favoriteUrls;
  final ValueChanged<MediaItem> onOpen, onToggle;
  final ValueChanged<List<MediaItem>> onItems;
  @override
  State<CatalogPage> createState() => _CatalogPageState();
}

class _CatalogPageState extends State<CatalogPage> {
  final _search = TextEditingController();
  final _scroll = ScrollController();
  Timer? _debounce;
  List<Map<String, dynamic>> _tabs = [];
  List<Map<String, dynamic>> _subcategories = [];
  List<MediaItem> _items = [];
  final Map<String, List<MediaItem>> _tabSnapshots =
      <String, List<MediaItem>>{};
  String _tabId = 'home', _path = 'main/', _categoryId = 'all';
  String _resultsQuery = '';
  bool _searchOpen = false,
      _busy = true,
      _providersBusy = false,
      _moreBusy = false,
      _hasMore = true;
  String? _error;
  int _page = 1, _request = 0;

  @override
  void initState() {
    super.initState();
    unawaited(_initialize());
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _scroll.dispose();
    _search.dispose();
    super.dispose();
  }

  Future<void> _initialize() async {
    try {
      final tabs = await NativeBridge.list('catalog.tabs');
      if (mounted && tabs.isNotEmpty) {
        setState(() => _tabs = tabs);
        _setSubcategories(tabs.first);
      }
    } catch (error) {
      if (mounted) setState(() => _error = '$error');
    }
    if (mounted) await _load(reset: true);
  }

  void _setSubcategories(Map<String, dynamic> tab) {
    final rows = _maps(tab['subcategories']);
    setState(() {
      _subcategories = rows;
      final first = rows.isEmpty ? <String, dynamic>{} : rows.first;
      _categoryId = _text(first['id']).ifEmpty('all');
      _path = _text(first['path']).ifEmpty(_text(tab['path']).ifEmpty('main/'));
    });
  }

  String _snapshotKey({
    String? tab,
    String? path,
    String? categoryId,
    String? query,
  }) =>
      '${tab ?? _tabId}|${path ?? _path}|${categoryId ?? _categoryId}|${(query ?? _search.text).trim().toLowerCase()}';

  void _rememberSnapshot(List<MediaItem> rows) {
    if (rows.isEmpty) return;
    final key = _snapshotKey();
    _tabSnapshots[key] = List<MediaItem>.unmodifiable(rows);
    while (_tabSnapshots.length > 24) {
      _tabSnapshots.remove(_tabSnapshots.keys.first);
    }
  }

  void _selectTab(Map<String, dynamic> tab) {
    final nextTab = _text(tab['id']);
    if (_tabId == nextTab) return;
    final rows = _maps(tab['subcategories']);
    final first = rows.isEmpty ? <String, dynamic>{} : rows.first;
    final nextCategory = _text(first['id']).ifEmpty('all');
    final nextPath = _text(first['path'])
        .ifEmpty(_text(tab['path']).ifEmpty('main/'));
    final cached =
        _tabSnapshots[_snapshotKey(
          tab: nextTab,
          path: nextPath,
          categoryId: nextCategory,
          query: '',
        )];
    setState(() {
      _tabId = nextTab;
      _subcategories = rows;
      _categoryId = nextCategory;
      _path = nextPath;
      _items = cached ?? const <MediaItem>[];
      _busy = cached == null;
      _providersBusy = false;
      _error = null;
      _page = 1;
      _hasMore = true;
    });
    unawaited(_load(reset: true));
  }

  void _selectCategory(Map<String, dynamic> category) {
    if (_categoryId == _text(category['id'])) return;
    final nextCategory = _text(category['id']);
    final nextPath = _text(category['path']);
    final cached =
        _tabSnapshots[_snapshotKey(
          path: nextPath,
          categoryId: nextCategory,
          query: '',
        )];
    setState(() {
      _categoryId = nextCategory;
      _path = nextPath;
      _items = cached ?? const <MediaItem>[];
      _busy = cached == null;
      _providersBusy = false;
      _error = null;
      _page = 1;
      _hasMore = true;
    });
    unawaited(_load(reset: true));
  }

  /// فلترة client-side للبحث — تضمن إن النتايج مطابقة للـ query
  List<MediaItem> _filterByQuery(List<MediaItem> rows, String query) {
    if (query.trim().isEmpty) return rows;
    final q = _normalizeSearchQuery(query);
    if (q.length < 2) return rows;
    return rows.where((item) {
      final title = _normalizeSearchQuery(item.title);
      final parent = _normalizeSearchQuery(item.parentSeriesTitle);
      // تطابق كلي
      if (title.contains(q) || parent.contains(q)) return true;
      // تطابق كلمات منفصلة (الكلمات الأكثر من 2 حرف)
      final words = q.split(RegExp(r'\s+')).where((w) => w.length > 2).toList();
      if (words.isEmpty) return title.contains(q);
      return words.every((w) => title.contains(w) || parent.contains(w));
    }).toList();
  }

  String _normalizeSearchQuery(String text) {
    var t = text.toLowerCase().trim();
    // تطبيع الأرقام العربية
    const arabicDigits = '٠١٢٣٤٥٦٧٨٩';
    for (var i = 0; i < arabicDigits.length; i++) {
      t = t.replaceAll(arabicDigits[i], '$i');
    }
    // تطبيع الحروف العربية
    t = t.replaceAll(RegExp('[أإآ]'), 'ا');
    t = t.replaceAll('ة', 'ه');
    t = t.replaceAll('ى', 'ي');
    // شيل التشكيل
    t = t.replaceAll(RegExp('[\u064B-\u065F]'), '');
    return t;
  }

  List<MediaItem> _unique(Iterable<MediaItem> rows) {
    final unique = <String, MediaItem>{};
    for (final row in rows) {
      if (row.title.trim().isEmpty || row.pageUrl.isEmpty) continue;
      final existing = unique[row.key];
      if (existing == null) {
        unique[row.key] = row;
        continue;
      }
      String normalizeUrl(String value) =>
          value.trim().replaceFirst(RegExp(r'/+$'), '').toLowerCase();
      final primaryUrl = normalizeUrl(existing.pageUrl);
      final alternatives =
          <String>{
                ...existing.alternatePageUrls,
                row.pageUrl,
                ...row.alternatePageUrls,
              }
              .where(
                (url) =>
                    url.startsWith('http') && normalizeUrl(url) != primaryUrl,
              )
              .toList(growable: false);
      unique[row.key] = existing.copyWith(alternatePageUrls: alternatives);
    }
    return unique.values.toList(growable: false);
  }

  Future<void> _load({bool reset = false, bool force = false}) async {
    if (_busy && !reset) return;
    if (reset) {
      _page = 1;
      _hasMore = true;
      _error = null;
    }
    final request = ++_request;
    final query = _search.text.trim();
    final queryChanged = query != _resultsQuery;
    if (queryChanged) _resultsQuery = query;
    final path = _path;
    final tab = _tabId;
    final allProviderCategory =
        (tab == 'series' &&
            _categoryId == 'all_series' &&
            path.replaceAll(RegExp(r'^/+|/+$'), '') == 'series') ||
        (tab == 'movies' &&
            _categoryId == 'all_movies' &&
            path.replaceAll(RegExp(r'^/+|/+$'), '') == 'movies');
    final categoryUsesProviderCatalog =
        tab == 'series' ||
        tab == 'movies' ||
        tab == 'anime' ||
        tab == 'ramadan';
    setState(() {
      _busy = true;
      _providersBusy = false;
      if (queryChanged) _items = [];
      if (reset && query.isEmpty && _items.isEmpty) _error = null;
    });
    try {
      final cached = query.isNotEmpty
          ? await NativeBridge.list('catalog.search.cached', {'query': query})
          : await NativeBridge.list('catalog.cached', {
              'path': path,
              'tab': tab,
              'categoryId': _categoryId,
              'page': 1,
            });
      if (!mounted || request != _request) return;
      if (cached.isNotEmpty) {
        final allCached = cached.map(MediaItem.fromMap).toList();
        // ── فلتر الكاش بالبحث أيضاً ──────────────────────────────────────
        final filteredCached = query.isNotEmpty
            ? _filterByQuery(allCached, query)
            : allCached;
        final rows = _unique(filteredCached);
        if (rows.isNotEmpty) {
          setState(() {
            _items = rows;
            _busy = false;
            _error = null;
          });
          if (query.isEmpty) _rememberSnapshot(rows);
          widget.onItems(rows);
        }
      }
    } catch (_) {}
    if (!mounted || request != _request) return;
    final hasProviderPass =
        query.isNotEmpty ||
        allProviderCategory ||
        categoryUsesProviderCatalog ||
        (tab == 'home' && (path == 'main/' || path == 'main'));
    try {
      final fetched = query.isNotEmpty
          ? await NativeBridge.list('catalog.search.primary', {
              'query': query,
              'force': force,
            })
          : await NativeBridge.list('catalog.primary.fetch', {
              'path': path,
              'tab': tab,
              'categoryId': _categoryId,
              'page': 1,
              'force': force,
            });
      if (!mounted || request != _request) return;
      // ── فلترة client-side للبحث ──────────────────────────────────────────
      final allRows = fetched.map(MediaItem.fromMap).toList();
      final filteredRows = query.isNotEmpty
          ? _filterByQuery(allRows, query)
          : allRows;
      final rows = _unique(filteredRows);
      if (rows.isNotEmpty) {
        // للبحث: استبدل النتايج بالكامل (متدمجش مع items قديمة)
        final merged = query.isNotEmpty
            ? rows
            : reset && query.isEmpty
            ? _unique([...rows, ..._items])
            : _unique([..._items, ...rows]);
        setState(() {
          _items = merged;
          _busy = false;
          _error = null;
          _hasMore = true;
          _providersBusy = hasProviderPass;
        });
        if (query.isEmpty) _rememberSnapshot(merged);
        widget.onItems(merged);
      } else {
        setState(() {
          _busy = false;
          _providersBusy = hasProviderPass;
          _error = hasProviderPass
              ? null
              : 'المصدر لم يرجع محتوى لهذه القائمة الآن.';
        });
      }
      if (hasProviderPass)
        unawaited(
          _loadProviderPass(request, path, tab, _categoryId, query, force),
        );
    } catch (error) {
      if (!mounted || request != _request) return;
      setState(() {
        _busy = false;
        _providersBusy = hasProviderPass;
        _error = _items.isEmpty && !hasProviderPass ? '$error' : null;
      });
      if (hasProviderPass)
        unawaited(
          _loadProviderPass(request, path, tab, _categoryId, query, force),
        );
    }
  }

  Future<void> _loadProviderPass(
    int request,
    String path,
    String tab,
    String categoryId,
    String query,
    bool force,
  ) async {
    try {
      final fetched = query.isNotEmpty
          ? await _nativeScraper.searchAllProviders(query, forceRefresh: force)
          : await _nativeScraper.fetchProviderCatalog(
              path: path,
              tab: tab,
              categoryId: categoryId,
              page: 1,
              forceRefresh: force,
            );
      if (!mounted || request != _request) return;
      // ── فلترة client-side: تضمن إن النتايج مطابقة للـ query ──────────────
      final allFetched = fetched.map(MediaItem.fromMap).toList();
      final filtered = query.isNotEmpty
          ? _filterByQuery(allFetched, query)
          : allFetched;
      final merged = _unique([..._items, ...filtered]);
      setState(() {
        _items = merged;
        _providersBusy = false;
        if (_items.isEmpty) {
          _error = query.isNotEmpty
              ? 'ما لقيناش نتائج مطابقة. جرّب كتابة الاسم بطريقة مختلفة.'
              : 'المصادر لم ترجع محتوى لهذه القائمة الآن.';
        }
      });
      if (query.isEmpty) _rememberSnapshot(merged);
      if (merged.isNotEmpty) widget.onItems(merged);
    } catch (error) {
      if (mounted && request == _request)
        setState(() {
          _providersBusy = false;
          if (_items.isEmpty) {
            _error = query.isNotEmpty
                ? 'تعذر البحث الآن. تأكد من الاتصال وحاول مرة أخرى.'
                : '$error';
          }
        });
    }
  }

  Future<void> _loadMore() async {
    if (!_hasMore ||
        _busy ||
        _providersBusy ||
        _moreBusy ||
        _search.text.trim().isNotEmpty ||
        _items.isEmpty)
      return;
    final request = _request;
    final next = _page + 1;
    setState(() => _moreBusy = true);
    try {
      final fetched = await NativeBridge.list('catalog.fetch', {
        'path': _path,
        'tab': _tabId,
        'categoryId': _categoryId,
        'page': next,
      });
      if (!mounted || request != _request) return;
      final rows = _unique(fetched.map(MediaItem.fromMap));
      final merged = _unique([..._items, ...rows]);
      final added = merged.length - _items.length;
      final hasPageData = fetched.isNotEmpty;
      setState(() {
        if (added > 0) {
          _items = merged;
        }
        // A page may only add duplicates from overlapping sources. Advance
        // past it, and stop only when every source returned an empty page.
        if (hasPageData) _page = next;
        _hasMore = hasPageData;
      });
      if (added > 0) widget.onItems(merged);
    } catch (_) {
      // The current page remains usable; scrolling can retry on the next threshold.
    } finally {
      if (mounted) setState(() => _moreBusy = false);
    }
  }

  void _toggleSearch() {
    setState(() {
      _searchOpen = !_searchOpen;
      if (!_searchOpen) {
        _request++;
        _search.clear();
        _resultsQuery = '';
        _items = [];
        _busy = true;
        _providersBusy = false;
        _error = null;
      }
    });
    if (!_searchOpen) unawaited(_load(reset: true));
  }

  void _onSearchChanged(String value) {
    _debounce?.cancel();
    final query = value.trim();
    if (query != _resultsQuery) {
      _request++;
      _resultsQuery = query;
      setState(() {
        _items = [];
        _busy = query.isNotEmpty;
        _providersBusy = false;
        _error = null;
        _page = 1;
        _hasMore = true;
      });
    }
    if (query.isEmpty) {
      unawaited(_load(reset: true));
      return;
    }
    _debounce = Timer(
      const Duration(milliseconds: 420),
      () => _load(reset: true),
    );
  }

  Widget _topBar() => Padding(
    padding: const EdgeInsets.fromLTRB(16, 5, 16, 3),
    child: Row(
      children: [
        if (_searchOpen)
          Expanded(
            child: TextField(
              controller: _search,
              autofocus: true,
              textInputAction: TextInputAction.search,
              onChanged: _onSearchChanged,
              onSubmitted: (_) {
                _debounce?.cancel();
                _load(reset: true);
              },
              decoration: InputDecoration(
                hintText: 'ابحث عن فيلم أو مسلسل',
                prefixIcon: const Icon(Icons.search, color: _brick),
                suffixIcon: IconButton(
                  onPressed: () {
                    _search.clear();
                    _onSearchChanged('');
                  },
                  icon: const Icon(Icons.close),
                ),
              ),
            ),
          )
        else ...[
          _LogoMark(size: 40),
          const SizedBox(width: 9),
          const Expanded(
            child: Text(
              'زقزوقة',
              style: TextStyle(
                color: _muted,
                fontSize: 26,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
        if (!_searchOpen)
          IconButton.filledTonal(
            tooltip: 'بحث',
            style: IconButton.styleFrom(
              backgroundColor: _card,
              foregroundColor: _ink,
            ),
            onPressed: _toggleSearch,
            icon: const Icon(Icons.search_rounded),
          )
        else
          IconButton.filledTonal(
            tooltip: 'إغلاق البحث',
            style: IconButton.styleFrom(
              backgroundColor: _card,
              foregroundColor: _ink,
            ),
            onPressed: _toggleSearch,
            icon: const Icon(Icons.close_rounded),
          ),
        const SizedBox(width: 5),
        IconButton.filledTonal(
          tooltip: 'تحديث المحتوى',
          style: IconButton.styleFrom(
            backgroundColor: _card,
            foregroundColor: _brick,
          ),
          onPressed: _busy ? null : () => _load(reset: true, force: true),
          icon: const Icon(Icons.sync_rounded),
        ),
      ],
    ),
  );

  Widget _mainTabs() => SizedBox(
    height: 47,
    child: ListView.separated(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 15),
      itemCount: _tabs.length,
      separatorBuilder: (_, __) => const SizedBox(width: 18),
      itemBuilder: (_, index) {
        final tab = _tabs[index];
        final selected = _tabId == _text(tab['id']);
        return InkWell(
          onTap: () => _selectTab(tab),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                _text(tab['title']),
                style: TextStyle(
                  color: selected ? _ink : _muted,
                  fontWeight: selected ? FontWeight.w800 : FontWeight.w500,
                  fontSize: 15,
                ),
              ),
              const SizedBox(height: 5),
              AnimatedContainer(
                duration: const Duration(milliseconds: 180),
                width: selected ? 29 : 0,
                height: 2.5,
                decoration: BoxDecoration(
                  color: _brick,
                  borderRadius: BorderRadius.circular(3),
                ),
              ),
            ],
          ),
        );
      },
    ),
  );

  Widget _categoryChips() => SizedBox(
    height: 47,
    child: ListView.separated(
      padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 4),
      scrollDirection: Axis.horizontal,
      itemCount: _subcategories.length,
      separatorBuilder: (_, __) => const SizedBox(width: 8),
      itemBuilder: (_, index) {
        final row = _subcategories[index];
        final selected = _categoryId == _text(row['id']);
        return ChoiceChip(
          label: Text(_text(row['title'])),
          selected: selected,
          showCheckmark: false,
          labelStyle: TextStyle(
            color: selected ? Colors.white : _muted,
            fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
            fontSize: 12,
          ),
          side: BorderSide(color: selected ? _brick : _line),
          onSelected: (_) => _selectCategory(row),
        );
      },
    ),
  );

  @override
  Widget build(BuildContext context) => Column(
    children: [
      _topBar(),
      if (!_searchOpen) _mainTabs(),
      if (!_searchOpen && _subcategories.isNotEmpty) _categoryChips(),
      Expanded(
        child: RefreshIndicator(
          color: _brick,
          onRefresh: () => _load(reset: true, force: true),
          child: _content(),
        ),
      ),
    ],
  );

  Widget _content() {
    if (_items.isEmpty && _busy) return const _CatalogSkeleton();
    if (_items.isEmpty && _providersBusy)
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: 110),
          Center(child: CircularProgressIndicator(color: _brick)),
          SizedBox(height: 12),
          Center(
            child: Text(
              'بنحمّل النتائج من باقي المصادر…',
              style: TextStyle(color: _muted),
            ),
          ),
        ],
      );
    if (_items.isEmpty)
      return ListView(
        children: [
          const SizedBox(height: 120),
          Icon(
            _error == null
                ? Icons.movie_filter_outlined
                : Icons.wifi_off_rounded,
            size: 42,
            color: _muted.withOpacity(.65),
          ),
          const SizedBox(height: 12),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 35),
            child: Text(
              _error ?? 'لا يوجد محتوى لعرضه الآن',
              textAlign: TextAlign.center,
              style: const TextStyle(color: _muted, height: 1.5),
            ),
          ),
          const SizedBox(height: 14),
          Center(
            child: OutlinedButton.icon(
              onPressed: () => _load(reset: true, force: true),
              icon: const Icon(Icons.refresh),
              label: const Text('إعادة المحاولة'),
            ),
          ),
        ],
      );

    final showFeatured =
        _tabId == 'home' && _search.text.trim().isEmpty && _items.isNotEmpty;
    final featuredItems = showFeatured
        ? _items.take(5).toList()
        : <MediaItem>[];
    final gridItems = showFeatured
        ? _items.skip(featuredItems.length).toList(growable: false)
        : _items;
    final categoryTitle = _search.text.trim().isNotEmpty
        ? 'نتائج البحث'
        : _text(
            _subcategories
                .where((row) => _text(row['id']) == _categoryId)
                .firstOrNull?['title'],
          ).ifEmpty(
            _text(
              _tabs
                  .where((row) => _text(row['id']) == _tabId)
                  .firstOrNull?['title'],
            ),
          );
    return CustomScrollView(
      controller: _scroll,
      physics: const AlwaysScrollableScrollPhysics(),
      slivers: [
        if (showFeatured)
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 5, 16, 11),
              child: _FeaturedCarousel(
                items: featuredItems,
                onOpen: widget.onOpen,
              ),
            ),
          ),
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(17, 4, 17, 12),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        categoryTitle.isEmpty ? 'اكتشف المحتوى' : categoryTitle,
                        style: const TextStyle(
                          color: _ink,
                          fontSize: 18,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 2),
                      const Text(
                        'اختيارات مرتبة من مكتبة زقزوقة',
                        style: TextStyle(color: _muted, fontSize: 11),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(14, 4, 14, 22),
          sliver: SliverGrid.builder(
            itemCount: gridItems.length + (_moreBusy ? 3 : 0),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              crossAxisSpacing: 10,
              mainAxisSpacing: 14,
              childAspectRatio: .50,
            ),
            itemBuilder: (context, index) {
              if (index >= gridItems.length) return const _PosterSkeleton();
              final item = gridItems[index];
              return MediaPoster(
                item: item,
                favorite: widget.favoriteUrls.contains(item.pageUrl),
                onTap: () => widget.onOpen(item),
                onFavorite: () => widget.onToggle(item),
              );
            },
          ),
        ),
        if (_search.text.trim().isEmpty && _hasMore)
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 0, 18, 26),
              child: Center(
                child: OutlinedButton.icon(
                  onPressed: _moreBusy ? null : _loadMore,
                  icon: _moreBusy
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
                    _moreBusy ? 'بنحمّل أعمال أكتر…' : 'تحميل المزيد',
                  ),
                ),
              ),
            ),
          )
        else if (_search.text.trim().isEmpty && _items.isNotEmpty)
          const SliverToBoxAdapter(
            child: Padding(
              padding: EdgeInsets.only(bottom: 26),
              child: Center(
                child: Text(
                  'وصلت لنهاية النتائج المتاحة حالياً',
                  style: TextStyle(color: _muted, fontSize: 12),
                ),
              ),
            ),
          ),
      ],
    );
  }
}
