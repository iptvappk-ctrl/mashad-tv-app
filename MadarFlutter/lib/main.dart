import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const _native = MethodChannel('madar/native');
const _ink = Color(0xFF342633);
const _brick = Color(0xFFB94E3C);
const _paper = Color(0xFFF2E6D7);
const _card = Color(0xFFFFFBF5);
const _muted = Color(0xFF806D78);
const _line = Color(0xFFE2D2C0);

void main() => runApp(const ZakzoukaFlutterApp());

// Keep the default Flutter test harness entry point available during the port.
class MyApp extends ZakzoukaFlutterApp {
  const MyApp({super.key});
}

class ZakzoukaFlutterApp extends StatelessWidget {
  const ZakzoukaFlutterApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'زقزوقة',
    theme: ThemeData(
      useMaterial3: true,
      scaffoldBackgroundColor: _paper,
      colorScheme: ColorScheme.fromSeed(
        seedColor: _brick,
        brightness: Brightness.light,
        surface: _card,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: _paper,
        foregroundColor: _ink,
        elevation: 0,
        centerTitle: false,
      ),
      cardTheme: CardThemeData(
        color: _card,
        margin: EdgeInsets.zero,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
          side: const BorderSide(color: _line),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: const Color(0xFFFFF9F1),
        indicatorColor: _brick.withOpacity(.12),
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TextStyle(
            color: states.contains(WidgetState.selected) ? _brick : _muted,
            fontSize: 11,
            fontWeight: states.contains(WidgetState.selected)
                ? FontWeight.w700
                : FontWeight.w500,
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: _card,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 13,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(18),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(18),
          borderSide: const BorderSide(color: _line),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(18),
          borderSide: const BorderSide(color: _brick, width: 1.4),
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: _card,
        selectedColor: _brick,
        side: const BorderSide(color: _line),
        shape: const StadiumBorder(),
        labelStyle: const TextStyle(color: _muted),
      ),
    ),
    builder: (context, child) => Directionality(
      textDirection: TextDirection.rtl,
      child: child ?? const SizedBox.shrink(),
    ),
    home: const RootTabs(),
  );
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
  }) async {
    try {
      return await _native.invokeMethod<bool>('player.open', {
            'url': url,
            'title': title,
            'userAgent': userAgent,
            'referer': referer,
            'headers': headers,
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
}

String _text(dynamic value) => value == null ? '' : '$value';
List<Map<String, dynamic>> _maps(dynamic value) => value is List
    ? value.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList()
    : <Map<String, dynamic>>[];

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

class RootTabs extends StatefulWidget {
  const RootTabs({super.key});
  @override
  State<RootTabs> createState() => _RootTabsState();
}

class _RootTabsState extends State<RootTabs> {
  int _tab = 0;
  final Map<String, MediaItem> _favorites = {};
  final Map<String, MediaItem> _seenItems = {};
  bool _sportsBuilt = false;

  @override
  void initState() {
    super.initState();
    unawaited(_restoreFavorites());
  }

  Future<void> _restoreFavorites() async {
    final saved = await NativeBridge.readCache('favorites:items');
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
        favoriteUrls: _favorites.keys.toSet(),
        onOpen: _open,
        onToggle: _toggle,
        onItems: _remember,
      ),
      _sportsBuilt ? const SportsPage() : const SizedBox.shrink(),
      FavoritesPage(
        items: all,
        favoriteUrls: _favorites.keys.toSet(),
        onOpen: _open,
        onToggle: _toggle,
      ),
    ];
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Stack(
          children: List.generate(
            pages.length,
            (index) => Positioned.fill(
              child: IgnorePointer(
                ignoring: _tab != index,
                child: TickerMode(
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
                      child: pages[index],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        height: 72,
        onDestinationSelected: (index) => setState(() {
          _tab = index;
          if (index == 1) _sportsBuilt = true;
        }),
        destinations: const [
          NavigationDestination(
            icon: Text('🎬', style: TextStyle(fontSize: 21)),
            label: 'أفلام ومسلسلات',
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
  String _tabId = 'home', _path = 'main/', _categoryId = 'all';
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
    _scroll.addListener(_onScroll);
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

  void _onScroll() {
    if (_scroll.hasClients &&
        _scroll.position.pixels > _scroll.position.maxScrollExtent - 520) {
      unawaited(_loadMore());
    }
  }

  void _selectTab(Map<String, dynamic> tab) {
    if (_tabId == _text(tab['id'])) return;
    _tabId = _text(tab['id']);
    _items = [];
    _page = 1;
    _setSubcategories(tab);
    unawaited(_load(reset: true));
  }

  void _selectCategory(Map<String, dynamic> category) {
    if (_categoryId == _text(category['id'])) return;
    setState(() {
      _categoryId = _text(category['id']);
      _path = _text(category['path']);
      _items = [];
      _page = 1;
      _hasMore = true;
    });
    unawaited(_load(reset: true));
  }

  List<MediaItem> _unique(Iterable<MediaItem> rows) {
    final unique = <String, MediaItem>{};
    for (final row in rows) {
      if (row.title.trim().isEmpty || row.pageUrl.isEmpty) continue;
      unique.putIfAbsent(row.key, () => row);
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
    final path = _path;
    final tab = _tabId;
    setState(() {
      _busy = true;
      _providersBusy = false;
      if (reset && query.isEmpty && _items.isEmpty) _error = null;
    });
    try {
      final cached = query.isNotEmpty
          ? await NativeBridge.list('catalog.search.cached', {'query': query})
          : await NativeBridge.list('catalog.cached', {
              'path': path,
              'tab': tab,
              'page': 1,
            });
      if (!mounted || request != _request) return;
      if (cached.isNotEmpty) {
        final rows = _unique(cached.map(MediaItem.fromMap));
        setState(() {
          _items = rows;
          _busy = false;
          _error = null;
        });
        widget.onItems(rows);
      }
    } catch (_) {}
    if (!mounted || request != _request) return;
    final hasProviderPass =
        query.isNotEmpty ||
        tab == 'series' ||
        tab == 'movies' ||
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
              'page': 1,
              'force': force,
            });
      if (!mounted || request != _request) return;
      final rows = _unique(fetched.map(MediaItem.fromMap));
      if (rows.isNotEmpty) {
        final merged = _unique([..._items, ...rows]);
        setState(() {
          _items = merged;
          _busy = false;
          _error = null;
          _hasMore = true;
          _providersBusy = hasProviderPass;
        });
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
        unawaited(_loadProviderPass(request, path, tab, query, force));
    } catch (error) {
      if (!mounted || request != _request) return;
      setState(() {
        _busy = false;
        _providersBusy = hasProviderPass;
        _error = _items.isEmpty && !hasProviderPass ? '$error' : null;
      });
      if (hasProviderPass)
        unawaited(_loadProviderPass(request, path, tab, query, force));
    }
  }

  Future<void> _loadProviderPass(
    int request,
    String path,
    String tab,
    String query,
    bool force,
  ) async {
    try {
      final fetched = query.isNotEmpty
          ? await NativeBridge.list('catalog.search.providers', {
              'query': query,
              'force': force,
            })
          : await NativeBridge.list('catalog.providers.fetch', {
              'path': path,
              'tab': tab,
              'page': 1,
              'force': force,
            });
      if (!mounted || request != _request) return;
      final merged = _unique([..._items, ...fetched.map(MediaItem.fromMap)]);
      setState(() {
        _items = merged;
        _providersBusy = false;
        if (_items.isEmpty) _error = 'المصادر لم ترجع محتوى لهذه القائمة الآن.';
      });
      if (merged.isNotEmpty) widget.onItems(merged);
    } catch (error) {
      if (mounted && request == _request)
        setState(() {
          _providersBusy = false;
          if (_items.isEmpty) _error = '$error';
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
        'page': next,
      });
      if (!mounted || request != _request) return;
      final rows = _unique(fetched.map(MediaItem.fromMap));
      final merged = _unique([..._items, ...rows]);
      final added = merged.length - _items.length;
      setState(() {
        if (added > 0) {
          _items = merged;
          _page = next;
        } else {
          _hasMore = false;
        }
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
        _search.clear();
        _items = [];
      }
    });
    if (!_searchOpen) unawaited(_load(reset: true));
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
              onChanged: (_) {
                _debounce?.cancel();
                _debounce = Timer(
                  const Duration(milliseconds: 420),
                  () => _load(reset: true),
                );
              },
              onSubmitted: (_) => _load(reset: true),
              decoration: InputDecoration(
                hintText: 'ابحث عن فيلم أو مسلسل',
                prefixIcon: const Icon(Icons.search, color: _brick),
                suffixIcon: IconButton(
                  onPressed: () {
                    _search.clear();
                    _load(reset: true);
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
    final gridItems = showFeatured
        ? _items.skip(1).toList(growable: false)
        : _items;
    return CustomScrollView(
      controller: _scroll,
      physics: const AlwaysScrollableScrollPhysics(),
      slivers: [
        if (showFeatured)
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 5, 16, 14),
              child: _FeaturedCard(
                item: _items.first,
                onTap: () => widget.onOpen(_items.first),
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
        if (_moreBusy)
          const SliverToBoxAdapter(
            child: Padding(
              padding: EdgeInsets.only(bottom: 18),
              child: Center(
                child: SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: _brick,
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class _LogoMark extends StatelessWidget {
  const _LogoMark({this.size = 38});
  final double size;
  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      border: Border.all(color: _brick, width: 1.4),
      color: Colors.black,
    ),
    clipBehavior: Clip.antiAlias,
    child: Image.asset(
      'assets/zakzouka_logo.jpg',
      fit: BoxFit.cover,
      errorBuilder: (_, __, ___) =>
          const Icon(Icons.movie, color: Colors.white, size: 20),
    ),
  );
}

class _FeaturedCard extends StatelessWidget {
  const _FeaturedCard({required this.item, required this.onTap});
  final MediaItem item;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Material(
    color: _line,
    borderRadius: BorderRadius.circular(24),
    clipBehavior: Clip.antiAlias,
    child: InkWell(
      onTap: onTap,
      child: SizedBox(
        height: 194,
        child: Stack(
          fit: StackFit.expand,
          children: [
            _NetworkImage(url: item.poster, fit: BoxFit.cover),
            const DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.centerRight,
                  end: Alignment.centerLeft,
                  colors: [
                    Color(0xE82C1F2C),
                    Color(0x8A2C1F2C),
                    Color(0x12000000),
                  ],
                ),
              ),
            ),
            Positioned(
              right: 18,
              bottom: 17,
              left: 18,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  const Text(
                    'اختيار اليوم',
                    style: TextStyle(
                      color: Colors.white70,
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    item.title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 20,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  const SizedBox(height: 9),
                  Row(
                    children: [
                      _SmallBadge(label: item.isSeries ? 'مسلسل' : 'فيلم'),
                      if (item.year.isNotEmpty) ...[
                        const SizedBox(width: 6),
                        _SmallBadge(label: item.year),
                      ],
                      const Spacer(),
                      const Icon(
                        Icons.play_circle_fill,
                        size: 32,
                        color: Colors.white,
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

class _SmallBadge extends StatelessWidget {
  const _SmallBadge({required this.label});
  final String label;
  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(
      color: Colors.black.withOpacity(.58),
      borderRadius: BorderRadius.circular(8),
    ),
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Text(
        label,
        style: const TextStyle(color: Colors.white, fontSize: 10),
      ),
    ),
  );
}

class MediaPoster extends StatelessWidget {
  const MediaPoster({
    super.key,
    required this.item,
    required this.favorite,
    required this.onTap,
    required this.onFavorite,
  });
  final MediaItem item;
  final bool favorite;
  final VoidCallback onTap, onFavorite;
  @override
  Widget build(BuildContext context) => InkWell(
    borderRadius: BorderRadius.circular(15),
    onTap: onTap,
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(
          child: Stack(
            fit: StackFit.expand,
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(15),
                child: _NetworkImage(url: item.poster, fit: BoxFit.cover),
              ),
              Positioned(
                right: 6,
                top: 6,
                child: _SmallBadge(label: item.isSeries ? 'مسلسل' : 'فيلم'),
              ),
              if (item.quality.isNotEmpty)
                Positioned(
                  left: 5,
                  top: 5,
                  child: _SmallBadge(label: item.quality),
                ),
              Positioned(
                left: 0,
                bottom: 0,
                child: IconButton(
                  visualDensity: VisualDensity.compact,
                  onPressed: onFavorite,
                  icon: Icon(
                    favorite ? Icons.favorite : Icons.favorite_border,
                    color: favorite ? _brick : Colors.white,
                    size: 19,
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 7),
        Text(
          item.title,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          textAlign: TextAlign.center,
          style: const TextStyle(
            color: _ink,
            fontSize: 12,
            height: 1.35,
            fontWeight: FontWeight.w700,
          ),
        ),
        if (item.year.isNotEmpty || item.latestEpisodeLabel.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 3),
            child: Text(
              item.latestEpisodeLabel.isNotEmpty
                  ? item.latestEpisodeLabel
                  : '${item.year} · عام',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.center,
              style: const TextStyle(color: _muted, fontSize: 10),
            ),
          ),
      ],
    ),
  );
}

class _NetworkImage extends StatelessWidget {
  const _NetworkImage({required this.url, this.fit = BoxFit.cover});
  final String url;
  final BoxFit fit;
  @override
  Widget build(BuildContext context) {
    if (url.isEmpty) return const _PosterSkeleton();
    return Image.network(
      url,
      fit: fit,
      cacheWidth: 420,
      cacheHeight: 760,
      filterQuality: FilterQuality.low,
      gaplessPlayback: true,
      frameBuilder: (context, child, frame, sync) =>
          frame != null || sync ? child : const _PosterSkeleton(),
      errorBuilder: (_, __, ___) => const _PosterFallback(),
    );
  }
}

class _PosterFallback extends StatelessWidget {
  const _PosterFallback();
  @override
  Widget build(BuildContext context) => Container(
    color: const Color(0xFFE8DBCC),
    alignment: Alignment.center,
    child: const Icon(Icons.movie_outlined, color: _muted, size: 28),
  );
}

class _PosterSkeleton extends StatelessWidget {
  const _PosterSkeleton();
  @override
  Widget build(BuildContext context) => Container(
    decoration: BoxDecoration(
      color: const Color(0xFFEADBCB),
      borderRadius: BorderRadius.circular(15),
    ),
    child: const _PosterFallback(),
  );
}

class _CatalogSkeleton extends StatelessWidget {
  const _CatalogSkeleton();
  @override
  Widget build(BuildContext context) => ListView(
    physics: const AlwaysScrollableScrollPhysics(),
    padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
    children: [
      Container(
        height: 190,
        decoration: BoxDecoration(
          color: const Color(0xFFEADBCB),
          borderRadius: BorderRadius.circular(24),
        ),
      ),
      const SizedBox(height: 18),
      GridView.builder(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: 9,
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 3,
          crossAxisSpacing: 10,
          mainAxisSpacing: 14,
          childAspectRatio: .50,
        ),
        itemBuilder: (_, __) => const _PosterSkeleton(),
      ),
    ],
  );
}

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
  bool _loading = true;
  String? _error;
  int _seasonIndex = 0, _request = 0;

  @override
  void initState() {
    super.initState();
    unawaited(_load());
  }

  Future<void> _load() async {
    final request = ++_request;
    setState(() {
      _loading = true;
      _error = null;
    });
    final args = {'item': widget.item.toMap()};
    try {
      final cached = await Future.wait([
        NativeBridge.map('media.detail.cached', args),
        if (widget.item.isSeries)
          NativeBridge.map('media.seasons.cached', args)
        else
          Future<Map<String, dynamic>?>.value(null),
      ]);
      if (!mounted || request != _request) return;
      setState(() {
        _detail = cached[0];
        _bundle = cached[1];
        _loading = false;
        _seasonIndex = 0;
      });
    } catch (_) {}
    if (!mounted || request != _request) return;
    try {
      final fresh = await Future.wait([
        NativeBridge.map('media.detail.fetch', args),
        if (widget.item.isSeries)
          NativeBridge.map('media.seasons.fetch', args)
        else
          Future<Map<String, dynamic>?>.value(null),
      ]);
      if (!mounted || request != _request) return;
      setState(() {
        _detail = fresh[0] ?? _detail;
        _bundle = fresh[1] ?? _bundle;
        _loading = false;
        _error = null;
        _seasonIndex = 0;
      });
    } catch (error) {
      if (mounted && request == _request)
        setState(() {
          _loading = false;
          _error = '$error';
        });
    }
  }

  List<Map<String, dynamic>> get _seasons => _maps(_bundle?['seasons']);
  List<Map<String, dynamic>> get _parts => _maps(_bundle?['relatedParts']);
  List<Map<String, dynamic>> get _servers => _maps(_detail?['servers']);

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
    final episodes = _maps(season?['episodes'])
      ..sort((a, b) => _episodeNumber(a).compareTo(_episodeNumber(b)));
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.item.title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        actions: [
          IconButton(
            onPressed: _loading ? null : _load,
            icon: const Icon(Icons.refresh_rounded, color: _brick),
          ),
        ],
      ),
      body: RefreshIndicator(
        color: _brick,
        onRefresh: _load,
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
              const SizedBox(height: 13),
              _seasonsSection(season, episodes),
              if (_parts.isNotEmpty) ...[
                const SizedBox(height: 13),
                _SectionCard(
                  title: 'الأجزاء والمواسم الأخرى',
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
            ] else ...[
              const SizedBox(height: 13),
              _serverSection(_servers, widget.item.pageUrl, widget.item.title),
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
                  onSelected: (_) => setState(() => _seasonIndex = index),
                );
              },
            ),
          ),
          const SizedBox(height: 12),
        ],
        if (episodes.isEmpty)
          Text(
            _loading
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
              return ActionChip(
                avatar: const Icon(
                  Icons.play_arrow_rounded,
                  size: 16,
                  color: _brick,
                ),
                label: Text('الحلقة ${n > 0 ? n : index + 1}'),
                backgroundColor: _paper,
                side: const BorderSide(color: _line),
                onPressed: () => _openEpisode(episodes, index, selected),
              );
            }),
          ),
      ],
    ),
  );

  void _openEpisode(
    List<Map<String, dynamic>> episodes,
    int index,
    Map<String, dynamic>? season,
  ) {
    Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => EpisodePage(
          seriesTitle: widget.item.title,
          provider: widget.item.provider,
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
    String title,
  ) => _SectionCard(
    title: 'اختر سيرفر المشاهدة',
    icon: '▶️',
    child: servers.isEmpty
        ? Text(
            _loading
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
      final episodes = _maps(season?['episodes'])
        ..sort((a, b) => _episodeNumber(a).compareTo(_episodeNumber(b)));
      if (episodes.isEmpty) {
        _message(context, 'لا توجد حلقات جاهزة للتشغيل بعد.');
        return;
      }
      _openEpisode(episodes, 0, season);
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
    var url = _text(server['directStreamUrl']);
    final embed = _text(server['embedUrl']);
    if (url.isEmpty && embed.isNotEmpty) {
      try {
        url = _text(
          await _native.invokeMethod<String>('media.resolve', {'url': embed}),
        );
      } catch (_) {}
    }
    if (url.isEmpty) url = embed;
    await _launchPlayer(
      context,
      _absolute(url, referer),
      title,
      referer: referer,
    );
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
                item.title,
                maxLines: 3,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: _ink,
                  fontSize: 20,
                  fontWeight: FontWeight.w900,
                  height: 1.3,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                [
                  if (item.year.isNotEmpty) item.year,
                  if (item.quality.isNotEmpty) item.quality,
                  item.isSeries ? 'مسلسل' : 'فيلم',
                  item.provider,
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

class EpisodePage extends StatefulWidget {
  const EpisodePage({
    super.key,
    required this.seriesTitle,
    required this.provider,
    required this.seasonTitle,
    required this.episodes,
    required this.index,
  });
  final String seriesTitle, provider, seasonTitle;
  final List<Map<String, dynamic>> episodes;
  final int index;
  @override
  State<EpisodePage> createState() => _EpisodePageState();
}

class _EpisodePageState extends State<EpisodePage> {
  late int _index;
  List<Map<String, dynamic>> _servers = [];
  bool _loading = true;
  String? _error;
  int _request = 0;
  Map<String, dynamic> get _episode => widget.episodes[_index];
  int get _number => _episodeNumber(_episode);
  String get _episodeTitle =>
      _text(_episode['title']).ifEmpty('الحلقة $_number');
  String get _fullTitle =>
      '${widget.seriesTitle} ${widget.seasonTitle} $_episodeTitle';

  @override
  void initState() {
    super.initState();
    _index = widget.index;
    unawaited(_loadServers());
  }

  Future<void> _loadServers() async {
    final request = ++_request;
    final url = _text(_episode['pageUrl']);
    final key = 'episode:servers:$url';
    setState(() {
      _loading = true;
      _error = null;
    });
    final cached = await NativeBridge.readCache(key);
    if (cached != null && mounted && request == _request) {
      try {
        final rows = _maps(jsonDecode(cached));
        if (rows.isNotEmpty)
          setState(() {
            _servers = rows;
            _loading = false;
          });
      } catch (_) {}
    }
    if (!mounted || request != _request) return;
    try {
      final rows = await NativeBridge.list('media.servers', {
        'url': url,
        'provider': widget.provider,
      });
      if (!mounted || request != _request) return;
      setState(() {
        _servers = rows;
        _loading = false;
        _error = null;
      });
      if (rows.isNotEmpty)
        unawaited(NativeBridge.writeCache(key, jsonEncode(rows)));
    } catch (error) {
      if (mounted && request == _request)
        setState(() {
          _loading = false;
          _error = '$error';
        });
    }
  }

  int _adjacent(int delta) {
    final target = _number + delta;
    if (target <= 0) return -1;
    return widget.episodes.indexWhere(
      (episode) => _episodeNumber(episode) == target,
    );
  }

  void _moveTo(int next) {
    if (next < 0 || next >= widget.episodes.length) return;
    setState(() {
      _index = next;
      _servers = [];
    });
    unawaited(_loadServers());
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(_fullTitle, maxLines: 1, overflow: TextOverflow.ellipsis),
    ),
    body: RefreshIndicator(
      color: _brick,
      onRefresh: _loadServers,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 6, 16, 24),
        children: [
          Container(
            padding: const EdgeInsets.all(15),
            decoration: BoxDecoration(
              color: _card,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: _line),
            ),
            child: Row(
              children: [
                const _LogoMark(size: 56),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.seriesTitle,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: _ink,
                          fontWeight: FontWeight.w900,
                          fontSize: 17,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '${widget.seasonTitle} · $_episodeTitle',
                        style: const TextStyle(color: _muted),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Builder(
            builder: (context) {
              final previous = _adjacent(-1);
              final next = _adjacent(1);
              return Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: previous >= 0 ? () => _moveTo(previous) : null,
                      icon: const Icon(Icons.arrow_forward_rounded),
                      label: Text(
                        previous >= 0
                            ? 'السابق · الحلقة ${_episodeNumber(widget.episodes[previous])}'
                            : 'لا توجد حلقة سابقة',
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: FilledButton.icon(
                      onPressed: next >= 0 ? () => _moveTo(next) : null,
                      icon: const Icon(Icons.arrow_back_rounded),
                      label: Text(
                        next >= 0
                            ? 'التالي · الحلقة ${_episodeNumber(widget.episodes[next])}'
                            : 'لا توجد حلقة تالية',
                      ),
                    ),
                  ),
                ],
              );
            },
          ),
          const SizedBox(height: 18),
          if (_servers.isEmpty && _loading)
            const Padding(
              padding: EdgeInsets.all(25),
              child: Center(child: CircularProgressIndicator(color: _brick)),
            ),
          if (_servers.isEmpty && !_loading)
            Center(
              child: Padding(
                padding: const EdgeInsets.all(18),
                child: Text(
                  _error == null
                      ? 'المصدر لم يرجع سيرفرات تشغيل لهذه الحلقة.'
                      : 'تعذر تحميل السيرفرات. $_error',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: _muted),
                ),
              ),
            ),
          if (_servers.isNotEmpty)
            _SectionCard(
              title: 'اختر سيرفر المشاهدة',
              icon: '📡',
              child: Column(
                children: _servers
                    .map(
                      (server) => _ServerTile(
                        server: server,
                        onTap: () => _openServer(server),
                      ),
                    )
                    .toList(),
              ),
            ),
          const SizedBox(height: 14),
          _downloadCard(),
          const SizedBox(height: 16),
          _SectionCard(
            title: 'حلقات هذا الجزء / الموسم',
            icon: '📺',
            trailing: Text(
              '${widget.episodes.length} حلقة',
              style: const TextStyle(color: _muted, fontSize: 12),
            ),
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: List.generate(widget.episodes.length, (index) {
                final number = _episodeNumber(widget.episodes[index]);
                return ChoiceChip(
                  label: Text('الحلقة $number'),
                  selected: index == _index,
                  showCheckmark: false,
                  labelStyle: TextStyle(
                    color: index == _index ? Colors.white : _muted,
                  ),
                  side: BorderSide(color: index == _index ? _brick : _line),
                  onSelected: (_) {
                    setState(() {
                      _index = index;
                      _servers = [];
                    });
                    unawaited(_loadServers());
                  },
                );
              }),
            ),
          ),
        ],
      ),
    ),
  );

  Widget _downloadCard() => Material(
    color: const Color(0xFF078B68),
    borderRadius: BorderRadius.circular(17),
    child: InkWell(
      borderRadius: BorderRadius.circular(17),
      onTap: _servers.isEmpty ? null : _startDownload,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 15),
        child: Row(
          children: [
            const CircleAvatar(
              backgroundColor: Colors.white24,
              child: Icon(Icons.download_rounded, color: Colors.white),
            ),
            const SizedBox(width: 11),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'تحميل مباشر إلى الهاتف',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '$_episodeTitle • إلى مجلد Downloads',
                    style: const TextStyle(color: Colors.white70, fontSize: 11),
                  ),
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(9),
              ),
              child: Text(
                _servers.isEmpty ? 'تحميل الآن ↓' : 'تحميل الآن ↓',
                style: const TextStyle(
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

  Future<void> _startDownload() async {
    final server = _servers.firstWhere(
      (row) => _text(row['directStreamUrl']).isNotEmpty,
      orElse: () => _servers.first,
    );
    var url = _text(server['directStreamUrl']);
    if (url.isEmpty) {
      final embed = _text(server['embedUrl']);
      try {
        url = _text(
          await _native.invokeMethod<String>('media.resolve', {'url': embed}),
        );
      } catch (_) {}
    }
    if (url.isEmpty) {
      _message(context, 'المصدر لم يوفر رابط تنزيل مباشر لهذه الحلقة.');
      return;
    }
    bool started = false;
    try {
      started =
          await _native.invokeMethod<bool>('download.start', {
            'url': _absolute(url, _text(_episode['pageUrl'])),
            'title': _fullTitle,
            'referer': _text(_episode['pageUrl']),
          }) ??
          false;
    } catch (_) {}
    if (mounted)
      _message(
        context,
        started
            ? 'بدأ تنزيل $_episodeTitle في الخلفية.'
            : 'تعذر بدء التنزيل على هذا الجهاز.',
      );
  }

  Future<void> _openServer(Map<String, dynamic> server) async {
    var url = _text(server['directStreamUrl']);
    final embed = _text(server['embedUrl']);
    if (url.isEmpty && embed.isNotEmpty) {
      try {
        url = _text(
          await _native.invokeMethod<String>('media.resolve', {'url': embed}),
        );
      } catch (_) {}
    }
    if (url.isEmpty) url = embed;
    final headers = server['headers'] is Map
        ? Map<String, String>.from(
            (server['headers'] as Map).map((k, v) => MapEntry('$k', '$v')),
          )
        : <String, String>{};
    await _launchPlayer(
      context,
      _absolute(url, _text(_episode['pageUrl'])),
      _fullTitle,
      referer: _text(_episode['pageUrl']),
      headers: headers,
    );
  }
}

int _episodeNumber(Map<String, dynamic> row) {
  final direct = int.tryParse(_text(row['number']));
  if (direct != null && direct > 0) return direct;
  final value = _text(row['title']);
  const digits = '٠١٢٣٤٥٦٧٨٩';
  var normalized = value;
  for (var i = 0; i < digits.length; i++) {
    normalized = normalized.replaceAll(digits[i], '$i');
  }
  return int.tryParse(RegExp(r'\d+').firstMatch(normalized)?.group(0) ?? '') ??
      0;
}

class SportsPage extends StatefulWidget {
  const SportsPage({super.key});
  @override
  State<SportsPage> createState() => _SportsPageState();
}

class _SportsPageState extends State<SportsPage> {
  String _section = 'matches';
  List<Map<String, dynamic>> _events = [], _categories = [], _channels = [];
  String? _categoryId;
  String? _loadingId, _error;
  bool _loading = true, _refreshing = false;

  @override
  void initState() {
    super.initState();
    unawaited(_loadInitial());
  }

  Future<void> _loadInitial() async {
    try {
      final cached = await Future.wait([
        NativeBridge.list('live.events.cached'),
        NativeBridge.list('live.categories.cached'),
      ]);
      if (mounted)
        setState(() {
          _events = cached[0];
          _categories = cached[1];
          _loading = false;
          _categoryId ??= _categories.isEmpty
              ? null
              : _text(_categories.first['id']);
        });
      if (_categoryId != null) unawaited(_loadChannels(_categoryId!));
    } catch (_) {}
    await _refresh(force: false);
  }

  Future<void> _refresh({bool force = true}) async {
    if (_refreshing) return;
    setState(() {
      _refreshing = true;
      _error = null;
    });
    try {
      final result = await Future.wait([
        NativeBridge.list('live.events.fetch', {'force': force}),
        NativeBridge.list('live.categories.fetch', {'force': force}),
      ]);
      if (!mounted) return;
      setState(() {
        if (result[0].isNotEmpty) _events = result[0];
        if (result[1].isNotEmpty) {
          _categories = result[1];
          _categoryId ??= _text(_categories.first['id']);
        }
        _loading = false;
      });
      if (_categoryId != null) unawaited(_loadChannels(_categoryId!));
    } catch (error) {
      if (mounted)
        setState(() {
          _error = '$error';
          _loading = false;
        });
    } finally {
      if (mounted) setState(() => _refreshing = false);
    }
  }

  Future<void> _loadChannels(String id) async {
    setState(() {
      _categoryId = id;
      _channels = [];
    });
    try {
      final cached = await NativeBridge.list('live.channels.cached', {
        'categoryId': id,
      });
      if (mounted && id == _categoryId && cached.isNotEmpty)
        setState(() => _channels = cached);
    } catch (_) {}
    try {
      final rows = await NativeBridge.list('live.channels.fetch', {
        'categoryId': id,
      });
      if (mounted && id == _categoryId) setState(() => _channels = rows);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 13, 16, 6),
        child: Row(
          children: [
            const Expanded(
              child: Text(
                'بثات الكورة',
                style: TextStyle(
                  color: _ink,
                  fontSize: 23,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
            IconButton(
              onPressed: _refreshing ? null : () => _refresh(),
              icon: const Icon(Icons.refresh_rounded, color: _brick),
            ),
          ],
        ),
      ),
      SizedBox(
        height: 45,
        child: ListView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 3),
          children: [
            _sectionChip('matches', 'مباريات'),
            const SizedBox(width: 8),
            _sectionChip('channels', 'قنوات'),
          ],
        ),
      ),
      if (_section == 'channels' && _categories.isNotEmpty)
        SizedBox(
          height: 45,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
            itemCount: _categories.length,
            separatorBuilder: (_, __) => const SizedBox(width: 7),
            itemBuilder: (_, index) {
              final category = _categories[index];
              final id = _text(category['id']);
              final selected = id == _categoryId;
              return ChoiceChip(
                label: Text(_text(category['name'])),
                selected: selected,
                showCheckmark: false,
                labelStyle: TextStyle(
                  color: selected ? Colors.white : _muted,
                  fontSize: 12,
                ),
                side: BorderSide(color: selected ? _brick : _line),
                onSelected: (_) => _loadChannels(id),
              );
            },
          ),
        ),
      Expanded(
        child: RefreshIndicator(
          color: _brick,
          onRefresh: () => _refresh(),
          child: _section == 'matches' ? _eventsList() : _channelsList(),
        ),
      ),
    ],
  );

  Widget _sectionChip(String value, String label) => ChoiceChip(
    label: Text(label),
    selected: _section == value,
    showCheckmark: false,
    labelStyle: TextStyle(color: _section == value ? Colors.white : _muted),
    side: BorderSide(color: _section == value ? _brick : _line),
    onSelected: (_) => setState(() => _section = value),
  );

  Widget _eventsList() {
    if (_events.isEmpty && _loading) return const _CatalogSkeleton();
    if (_events.isEmpty) return _empty(_error ?? 'لا توجد مباريات متاحة الآن.');
    return ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(14),
      itemCount: _events.length,
      itemBuilder: (_, index) => _eventCard(_events[index]),
    );
  }

  Widget _channelsList() {
    if (_channels.isEmpty && _loading)
      return const Center(child: CircularProgressIndicator(color: _brick));
    if (_channels.isEmpty)
      return _empty(_error ?? 'اختر فئة القنوات أو اسحب للتحديث.');
    return ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(14),
      itemCount: _channels.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (_, index) {
        final channel = _channels[index];
        final id = _text(channel['id']);
        return Card(
          child: ListTile(
            onTap: () => _chooseStreams(id, _text(channel['name']), false),
            leading: _roundImage(_text(channel['logo'])),
            title: Text(
              _text(channel['name']),
              style: const TextStyle(color: _ink, fontWeight: FontWeight.w700),
            ),
            subtitle: const Text(
              'مشاهدة مباشرة',
              style: TextStyle(color: _muted),
            ),
            trailing: _loadingId == id
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: _brick,
                    ),
                  )
                : const Icon(Icons.play_circle_fill, color: _brick),
          ),
        );
      },
    );
  }

  Widget _empty(String text) => ListView(
    physics: const AlwaysScrollableScrollPhysics(),
    children: [
      const SizedBox(height: 145),
      Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            text,
            textAlign: TextAlign.center,
            style: const TextStyle(color: _muted),
          ),
        ),
      ),
    ],
  );

  Widget _roundImage(String url) => ClipOval(
    child: SizedBox(
      width: 42,
      height: 42,
      child: url.isEmpty
          ? const ColoredBox(
              color: _paper,
              child: Icon(Icons.sports_soccer, color: _muted),
            )
          : _NetworkImage(url: url),
    ),
  );

  Widget _team(dynamic raw) {
    final row = raw is Map
        ? Map<String, dynamic>.from(raw)
        : <String, dynamic>{};
    return Column(
      children: [
        _roundImage(_text(row['logo'])),
        const SizedBox(height: 5),
        Text(
          _text(row['name']).ifEmpty('فريق'),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(
            color: _ink,
            fontWeight: FontWeight.w700,
            fontSize: 12,
          ),
        ),
      ],
    );
  }

  Widget _eventCard(Map<String, dynamic> event) {
    final start = int.tryParse(_text(event['startTime'])) ?? 0;
    final end = int.tryParse(_text(event['endTime'])) ?? 0;
    final now = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    final live = start > 0 && start <= now && (end <= 0 || end >= now);
    final id = _text(event['id']);
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: () => _chooseStreams(id, _matchTitle(event), true),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      _text(event['champions']).ifEmpty('مباراة'),
                      style: const TextStyle(color: _muted, fontSize: 12),
                    ),
                  ),
                  if (live) const _LiveBadge(),
                  const SizedBox(width: 8),
                  Text(
                    _text(event['channel']),
                    style: const TextStyle(
                      color: _brick,
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(child: _team(event['team1'])),
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 15),
                    child: Text(
                      'VS',
                      style: TextStyle(
                        color: _brick,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ),
                  Expanded(child: _team(event['team2'])),
                ],
              ),
              const SizedBox(height: 8),
              if (_loadingId == id)
                const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: _brick,
                  ),
                )
              else
                const Text(
                  'اضغط لاختيار سيرفر البث',
                  style: TextStyle(color: _muted, fontSize: 11),
                ),
            ],
          ),
        ),
      ),
    );
  }

  String _matchTitle(Map<String, dynamic> e) =>
      '${_text((e['team1'] as Map?)?['name'])} × ${_text((e['team2'] as Map?)?['name'])}';

  Future<void> _chooseStreams(String id, String title, bool event) async {
    if (id.isEmpty || _loadingId == id) return;
    setState(() {
      _loadingId = id;
      _error = null;
    });
    try {
      final streams = await NativeBridge.list(
        event ? 'live.eventStreams' : 'live.channelStreams',
        {'id': id},
      );
      if (!mounted) return;
      if (streams.isEmpty) {
        _message(context, 'لا توجد روابط بث متاحة الآن.');
        return;
      }
      await showModalBottomSheet<void>(
        context: context,
        showDragHandle: true,
        backgroundColor: _card,
        builder: (sheet) => SafeArea(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 18),
            shrinkWrap: true,
            children: [
              const Text(
                'اختر سيرفر المشاهدة',
                style: TextStyle(
                  color: _ink,
                  fontWeight: FontWeight.w900,
                  fontSize: 18,
                ),
              ),
              const SizedBox(height: 10),
              ...streams.map(
                (stream) => _ServerTile(
                  server: {
                    'name': _text(stream['name']),
                    'embedUrl': _text(stream['url']),
                  },
                  onTap: () async {
                    Navigator.pop(sheet);
                    final headers = stream['headers'] is Map
                        ? Map<String, String>.from(
                            (stream['headers'] as Map).map(
                              (k, v) => MapEntry('$k', '$v'),
                            ),
                          )
                        : <String, String>{};
                    await _launchPlayer(
                      context,
                      _text(stream['url']),
                      title,
                      userAgent: _text(stream['userAgent']),
                      referer: _text(stream['referer']),
                      headers: headers,
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      );
    } catch (error) {
      if (mounted) _message(context, 'تعذر جلب سيرفرات البث: $error');
    } finally {
      if (mounted) setState(() => _loadingId = null);
    }
  }
}

class _LiveBadge extends StatelessWidget {
  const _LiveBadge();
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
    decoration: BoxDecoration(
      color: const Color(0xFFE5F4EC),
      borderRadius: BorderRadius.circular(20),
    ),
    child: const Text(
      'مباشر',
      style: TextStyle(
        color: Color(0xFF14845D),
        fontWeight: FontWeight.bold,
        fontSize: 11,
      ),
    ),
  );
}

Future<void> _launchPlayer(
  BuildContext context,
  String url,
  String title, {
  String userAgent = '',
  String referer = '',
  Map<String, String> headers = const {},
}) async {
  if (url.trim().isEmpty) {
    _message(context, 'رابط التشغيل غير متاح. اختر سيرفرًا آخر.');
    return;
  }
  final uri = Uri.tryParse(url);
  if (uri == null ||
      !uri.hasAuthority ||
      (uri.scheme != 'http' && uri.scheme != 'https')) {
    _message(context, 'رابط التشغيل غير صالح.');
    return;
  }
  final opened = await NativeBridge.openPlayer(
    url: url,
    title: title,
    userAgent: userAgent,
    referer: referer,
    headers: headers,
  );
  if (!context.mounted || opened) return;
  await showDialog<void>(
    context: context,
    builder: (dialog) => AlertDialog(
      title: const Text('مشغل زقزوقة مطلوب'),
      content: const Text(
        'ثبّت مشغل زقزوقة لفتح الفيديو. إذا كان مثبتًا بالفعل، حدّث المشغل والتطبيق معًا.',
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(dialog),
          child: const Text('إغلاق'),
        ),
        FilledButton(
          onPressed: () async {
            Navigator.pop(dialog);
            await NativeBridge.openPlayerStore();
          },
          child: const Text('تحميل المشغل'),
        ),
      ],
    ),
  );
}

String _absolute(String value, String base) {
  final uri = Uri.tryParse(value);
  if (uri == null) return value;
  if (uri.hasScheme) return value;
  final baseUri = Uri.tryParse(base);
  if (baseUri == null || !baseUri.hasScheme) return value;
  return baseUri.resolveUri(uri).toString();
}

void _message(BuildContext context, String message) =>
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), behavior: SnackBarBehavior.floating),
    );
