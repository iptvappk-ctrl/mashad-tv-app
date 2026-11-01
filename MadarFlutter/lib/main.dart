import 'dart:async';
import 'dart:convert';

import 'package:cached_network_image/cached_network_image.dart';
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
    home: const _StartupIntro(child: RootTabs()),
  );
}

class _StartupIntro extends StatefulWidget {
  const _StartupIntro({required this.child});

  final Widget child;

  @override
  State<_StartupIntro> createState() => _StartupIntroState();
}

class _StartupIntroState extends State<_StartupIntro> {
  Timer? _timer;
  bool _visible = true;
  bool _showIntro = true;

  @override
  void initState() {
    super.initState();
    _timer = Timer(const Duration(milliseconds: 950), _dismissIntro);
  }

  void _dismissIntro() {
    if (!mounted || !_showIntro || !_visible) return;
    setState(() => _visible = false);
  }

  void _removeIntro() {
    if (mounted && !_visible && _showIntro) {
      setState(() => _showIntro = false);
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Stack(
    fit: StackFit.expand,
    children: [
      widget.child,
      if (_showIntro)
        Positioned.fill(
          child: IgnorePointer(
            ignoring: !_visible,
            child: AnimatedOpacity(
              opacity: _visible ? 1 : 0,
              duration: const Duration(milliseconds: 300),
              curve: Curves.easeOutCubic,
              onEnd: _removeIntro,
              child: _ZaqzoukaIntroScreen(onSkip: _dismissIntro),
            ),
          ),
        ),
    ],
  );
}

class _ZaqzoukaIntroScreen extends StatelessWidget {
  const _ZaqzoukaIntroScreen({required this.onSkip});

  final VoidCallback onSkip;

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: _paper,
    body: DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topRight,
          end: Alignment.bottomLeft,
          colors: [_card, _paper, Color(0xFFEAD7C5)],
        ),
      ),
      child: Stack(
        fit: StackFit.expand,
        children: [
          Positioned(
            top: -95,
            right: -75,
            child: Container(
              width: 250,
              height: 250,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _brick.withOpacity(.09),
              ),
            ),
          ),
          Positioned(
            bottom: -115,
            left: -70,
            child: Container(
              width: 280,
              height: 280,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _muted.withOpacity(.08),
              ),
            ),
          ),
          SafeArea(
            child: Stack(
              children: [
                Positioned(
                  top: 8,
                  left: 12,
                  child: TextButton(
                    onPressed: onSkip,
                    child: const Text(
                      'تخطي',
                      style: TextStyle(
                        color: _muted,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
                Center(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 28),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        TweenAnimationBuilder<double>(
                          tween: Tween(begin: .78, end: 1),
                          duration: const Duration(milliseconds: 760),
                          curve: Curves.easeOutBack,
                          builder: (context, scale, child) =>
                              Transform.scale(scale: scale, child: child),
                          child: Container(
                            width: 226,
                            height: 226,
                            padding: const EdgeInsets.all(7),
                            decoration: BoxDecoration(
                              color: _card,
                              borderRadius: BorderRadius.circular(40),
                              border: Border.all(
                                color: Colors.white,
                                width: 1.5,
                              ),
                              boxShadow: [
                                BoxShadow(
                                  color: _ink.withOpacity(.14),
                                  blurRadius: 36,
                                  offset: const Offset(0, 18),
                                ),
                              ],
                            ),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(34),
                              child: Image.asset(
                                'assets/zakzouka_logo.jpg',
                                fit: BoxFit.contain,
                                filterQuality: FilterQuality.medium,
                                semanticLabel: 'شعار زقزوقة',
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 27),
                        const Text(
                          'زقزوقة',
                          style: TextStyle(
                            color: _ink,
                            fontSize: 30,
                            fontWeight: FontWeight.w900,
                            letterSpacing: .2,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'كل الحكايات في مكان واحد',
                          style: TextStyle(
                            color: _muted.withOpacity(.96),
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 7),
                        const Text(
                          'أفلام • مسلسلات • مباريات مباشرة',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: _muted, fontSize: 13),
                        ),
                        const SizedBox(height: 28),
                        TweenAnimationBuilder<double>(
                          tween: Tween(begin: 0, end: 1),
                          duration: const Duration(milliseconds: 900),
                          curve: Curves.easeOutCubic,
                          builder: (context, value, child) => SizedBox(
                            width: 100,
                            child: Align(
                              alignment: Alignment.centerRight,
                              child: Container(
                                width: 100 * value,
                                height: 4,
                                decoration: BoxDecoration(
                                  color: _brick,
                                  borderRadius: BorderRadius.circular(99),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    ),
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
    for (final episode in [
      ..._maps(old?['episodes']),
      ..._maps(freshSeason?['episodes']),
    ]) {
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
        key: const PageStorageKey<String>('catalog-tab'),
        favoriteUrls: _favorites.keys.toSet(),
        onOpen: _open,
        onToggle: _toggle,
        onItems: _remember,
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
                  offset: _tab == index ? Offset.zero : const Offset(0, .012),
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
        allProviderCategory ||
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
        final merged = reset && query.isEmpty
            ? rows
            : _unique([..._items, ...rows]);
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
        if (_items.isEmpty) {
          _error = query.isNotEmpty
              ? 'ما لقيناش نتائج مطابقة. جرّب كتابة الاسم بطريقة مختلفة.'
              : 'المصادر لم ترجع محتوى لهذه القائمة الآن.';
        }
      });
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
                if (!(_tabId == 'home' && _categoryId == 'all'))
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: _card,
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(color: _line),
                    ),
                    child: Text(
                      '${_items.length} عمل',
                      style: const TextStyle(
                        color: _muted,
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                      ),
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

class _FeaturedCarousel extends StatefulWidget {
  const _FeaturedCarousel({required this.items, required this.onOpen});
  final List<MediaItem> items;
  final ValueChanged<MediaItem> onOpen;
  @override
  State<_FeaturedCarousel> createState() => _FeaturedCarouselState();
}

class _FeaturedCarouselState extends State<_FeaturedCarousel> {
  late final PageController _controller = PageController(viewportFraction: .94);
  int _current = 0;

  @override
  void didUpdateWidget(covariant _FeaturedCarousel oldWidget) {
    super.didUpdateWidget(oldWidget);
    final changedItems =
        oldWidget.items.length != widget.items.length ||
        (oldWidget.items.isNotEmpty &&
            widget.items.isNotEmpty &&
            oldWidget.items.first.pageUrl != widget.items.first.pageUrl);
    if (!changedItems) return;
    _current = 0;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && _controller.hasClients) _controller.jumpToPage(0);
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Column(
    children: [
      SizedBox(
        height: 204,
        child: PageView.builder(
          controller: _controller,
          itemCount: widget.items.length,
          onPageChanged: (index) => setState(() => _current = index),
          itemBuilder: (_, index) => Padding(
            padding: const EdgeInsetsDirectional.only(start: 2, end: 8),
            child: _FeaturedCard(
              item: widget.items[index],
              onTap: () => widget.onOpen(widget.items[index]),
            ),
          ),
        ),
      ),
      if (widget.items.length > 1) ...[
        const SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(widget.items.length, (index) {
            final active = index == _current;
            return AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              margin: const EdgeInsets.symmetric(horizontal: 3),
              width: active ? 18 : 6,
              height: 6,
              decoration: BoxDecoration(
                color: active ? _brick : _line,
                borderRadius: BorderRadius.circular(8),
              ),
            );
          }),
        ),
      ],
    ],
  );
}

class _FeaturedCard extends StatelessWidget {
  const _FeaturedCard({required this.item, required this.onTap});
  final MediaItem item;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Material(
    color: const Color(0xFF3C2D3A),
    borderRadius: BorderRadius.circular(24),
    clipBehavior: Clip.antiAlias,
    child: InkWell(
      onTap: onTap,
      child: Container(
        height: 194,
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.centerRight,
            end: Alignment.centerLeft,
            colors: [Color(0xFF30232E), Color(0xFF594653), Color(0xFF8A777A)],
          ),
        ),
        child: Stack(
          children: [
            Positioned(
              left: 8,
              top: 9,
              bottom: 9,
              width: 118,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: _NetworkImage(url: item.poster, fit: BoxFit.contain),
              ),
            ),
            Positioned(
              left: 137,
              right: 17,
              top: 16,
              bottom: 16,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  const Text(
                    'اختيار اليوم',
                    textAlign: TextAlign.right,
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
                    textAlign: TextAlign.right,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 20,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  const SizedBox(height: 9),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: [
                      const Icon(
                        Icons.play_circle_fill,
                        size: 31,
                        color: Colors.white,
                      ),
                      const SizedBox(width: 8),
                      _SmallBadge(label: item.isSeries ? 'مسلسل' : 'فيلم'),
                      if (item.year.isNotEmpty) ...[
                        const SizedBox(width: 6),
                        _SmallBadge(label: item.year),
                      ],
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
    return CachedNetworkImage(
      imageUrl: url,
      httpHeaders: _posterHeaders(url),
      fit: fit,
      memCacheWidth: 500,
      memCacheHeight: 900,
      maxWidthDiskCache: 650,
      maxHeightDiskCache: 1100,
      filterQuality: FilterQuality.low,
      fadeInDuration: const Duration(milliseconds: 120),
      useOldImageOnUrlChange: true,
      placeholder: (_, __) => const _PosterSkeleton(),
      errorWidget: (_, __, ___) => const _PosterFallback(),
    );
  }
}

Map<String, String> _posterHeaders(String rawUrl) {
  final uri = Uri.tryParse(rawUrl);
  final host = uri?.host.toLowerCase() ?? '';
  final referer = switch (host) {
    _ when host.contains('myseed') => 'https://m.myseed.tv/',
    _ when host.contains('arabseed') => 'https://arabseed.health/',
    _ when host.contains('akw') => 'https://akw.cam/',
    _ when host.contains('wecima') => 'https://myweecima.casa/',
    _ when host.contains('fasel') => 'https://wb49584.fasel4k.site/',
    _ when host.isNotEmpty =>
      '${uri!.scheme.isEmpty ? 'https' : uri.scheme}://$host/',
    _ => '',
  };
  return {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36',
    if (referer.isNotEmpty) 'Referer': referer,
    'Accept': 'image/avif,image/webp,image/apng,image/*,*/*;q=0.8',
  };
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
  );
}

class _CatalogSkeleton extends StatelessWidget {
  const _CatalogSkeleton();
  @override
  Widget build(BuildContext context) => _ShimmerLoading(
    child: ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
      children: [
        Align(
          alignment: AlignmentDirectional.centerEnd,
          child: _SkeletonBlock(width: 142, height: 9),
        ),
        const SizedBox(height: 12),
        const _SkeletonBlock(height: 190, radius: 24),
        const SizedBox(height: 18),
        Align(
          alignment: AlignmentDirectional.centerEnd,
          child: _SkeletonBlock(width: 124, height: 9),
        ),
        const SizedBox(height: 12),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: 6,
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 3,
            crossAxisSpacing: 10,
            mainAxisSpacing: 14,
            childAspectRatio: .50,
          ),
          itemBuilder: (_, __) => const _CatalogPosterSkeleton(),
        ),
      ],
    ),
  );
}

class _CatalogPosterSkeleton extends StatelessWidget {
  const _CatalogPosterSkeleton();

  @override
  Widget build(BuildContext context) => Column(
    children: [
      const Expanded(child: _SkeletonBlock(radius: 15)),
      const SizedBox(height: 7),
      const _SkeletonBlock(width: double.infinity, height: 8, radius: 5),
      const SizedBox(height: 5),
      Align(
        alignment: AlignmentDirectional.centerEnd,
        child: _SkeletonBlock(
          width: MediaQuery.sizeOf(context).width * .16,
          height: 7,
          radius: 5,
        ),
      ),
    ],
  );
}

class _SportsMatchSkeleton extends StatelessWidget {
  const _SportsMatchSkeleton();

  @override
  Widget build(BuildContext context) => _ShimmerLoading(
    child: ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(14),
      itemCount: 3,
      itemBuilder: (_, __) => Container(
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: _card,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: _line),
        ),
        child: Column(
          children: [
            const Align(
              alignment: AlignmentDirectional.centerEnd,
              child: _SkeletonBlock(width: 112, height: 8),
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                const Expanded(child: _SkeletonTeam()),
                const _SkeletonBlock(width: 64, height: 31, radius: 9),
                const Expanded(child: _SkeletonTeam()),
              ],
            ),
            const SizedBox(height: 14),
            const _SkeletonBlock(width: double.infinity, height: 8, radius: 5),
          ],
        ),
      ),
    ),
  );
}

class _SkeletonTeam extends StatelessWidget {
  const _SkeletonTeam();

  @override
  Widget build(BuildContext context) => Column(
    children: [
      const _SkeletonBlock(width: 42, height: 42, radius: 50),
      const SizedBox(height: 7),
      _SkeletonBlock(width: MediaQuery.sizeOf(context).width * .14, height: 8),
    ],
  );
}

class _SportsChannelSkeleton extends StatelessWidget {
  const _SportsChannelSkeleton();

  @override
  Widget build(BuildContext context) => _ShimmerLoading(
    child: ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(14),
      itemCount: 5,
      separatorBuilder: (_, __) => const SizedBox(height: 9),
      itemBuilder: (_, __) => Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: _card,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: _line),
        ),
        child: Row(
          children: [
            const _SkeletonBlock(width: 42, height: 42, radius: 50),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const _SkeletonBlock(width: 112, height: 9),
                  const SizedBox(height: 8),
                  _SkeletonBlock(
                    width: MediaQuery.sizeOf(context).width * .34,
                    height: 7,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            const _SkeletonBlock(width: 22, height: 22, radius: 50),
          ],
        ),
      ),
    ),
  );
}

class _SkeletonBlock extends StatelessWidget {
  const _SkeletonBlock({this.width, this.height, this.radius = 9});

  final double? width;
  final double? height;
  final double radius;

  @override
  Widget build(BuildContext context) => Container(
    width: width,
    height: height,
    decoration: BoxDecoration(
      color: const Color(0xFFEADBCB),
      borderRadius: BorderRadius.circular(radius),
    ),
  );
}

class _ShimmerLoading extends StatefulWidget {
  const _ShimmerLoading({required this.child});
  final Widget child;

  @override
  State<_ShimmerLoading> createState() => _ShimmerLoadingState();
}

class _ShimmerLoadingState extends State<_ShimmerLoading>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1450),
  )..repeat();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: _controller,
    child: widget.child,
    builder: (context, child) => ShaderMask(
      blendMode: BlendMode.srcATop,
      shaderCallback: (bounds) {
        final shift = -1.8 + _controller.value * 3.6;
        return LinearGradient(
          begin: Alignment(shift, -.24),
          end: Alignment(shift + .82, .24),
          colors: const [
            Color(0xFFEADBCB),
            Color(0xFFF8F1E8),
            Color(0xFFD7C0BE),
            Color(0xFFEADBCB),
          ],
          stops: const [0, .40, .58, 1],
        ).createShader(bounds);
      },
      child: child,
    ),
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

  Future<void> _pollSeasonCache(
    int request,
    Map<String, Object?> args,
  ) async {
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
    return seasons.map((season) {
      final episodes = _maps(season['episodes'])
        ..sort((a, b) => _episodeNumber(a).compareTo(_episodeNumber(b)));
      return '${_integer(season['number'])}:'
          '${episodes.map(_episodeNumber).join(',')}';
    }).join('|');
  }

  Future<void> _refreshSeasonPage(Map<String, dynamic> season) async {
    final seasonUrl = _text(season['pageUrl']).trim();
    final seasonNumber = _integer(season['number']);
    if (seasonUrl.isEmpty || seasonNumber <= 0 ||
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
          _seasonIndex = _seasonIndexFor(
            merged,
            preferredNumber: seasonNumber,
          );
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
    if (_maps(selected['episodes']).length <= 1) {
      unawaited(_refreshSeasonPage(selected));
    }
  }

  Future<void> _loadServers(int request) async {
    final url = widget.item.pageUrl;
    final key = 'media:servers:${widget.item.provider}:$url';
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
                    if (seasonEpisodes.length <= 1) {
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
                    _refreshingSeasonPages.contains(_text(selected?['pageUrl'])))
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
        final cacheKey = 'media:servers:${widget.item.provider}:$pageUrl';
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
          'provider': _text(episode['providerName']).ifEmpty(widget.item.provider),
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
          provider: _text(episodes[index]['providerName']).ifEmpty(widget.item.provider),
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
  late final List<Map<String, dynamic>> _episodes;
  List<Map<String, dynamic>> _servers = [];
  bool _loading = true;
  String? _error;
  int _request = 0;
  Map<String, dynamic> get _episode => _episodes[_index];
  int get _number => _episodeNumber(_episode);
  String get _episodeTitle =>
      _text(_episode['title']).ifEmpty('الحلقة $_number');
  String get _fullTitle =>
      '${widget.seriesTitle} ${widget.seasonTitle} $_episodeTitle';

  @override
  void initState() {
    super.initState();
    _episodes = _orderedEpisodes(widget.episodes);
    _index = _episodes.isEmpty
        ? 0
        : widget.index.clamp(0, _episodes.length - 1).toInt();
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
        'provider': _text(_episode['providerName']).ifEmpty(widget.provider),
      });
      if (!mounted || request != _request) return;
      setState(() {
        if (rows.isNotEmpty) _servers = rows;
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
    final target = _index + delta;
    return target >= 0 && target < _episodes.length ? target : -1;
  }

  void _moveTo(int next) {
    if (next < 0 || next >= _episodes.length) return;
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
                            ? 'السابق · الحلقة ${_episodeNumber(_episodes[previous])}'
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
                            ? 'التالي · الحلقة ${_episodeNumber(_episodes[next])}'
                            : 'لا توجد حلقة تالية',
                      ),
                    ),
                  ),
                ],
              );
            },
          ),
          const SizedBox(height: 18),
          _downloadCard(),
          const SizedBox(height: 14),
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
          const SizedBox(height: 16),
          _SectionCard(
            title: 'حلقات هذا الجزء / الموسم',
            icon: '📺',
            trailing: Text(
              '${_episodes.length} حلقة',
              style: const TextStyle(color: _muted, fontSize: 12),
            ),
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: List.generate(_episodes.length, (index) {
                final number = _episodeNumber(_episodes[index]);
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
      (row) => _isUsablePlaybackUrl(_text(row['directStreamUrl'])),
      orElse: () => _servers.first,
    );
    final url = await _resolveServerStream(server);
    if (!mounted) return;
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
    final url = await _resolveServerStream(server);
    if (!mounted) return;
    if (url.isEmpty) {
      _message(context, 'السيرفر لم يوفر رابط فيديو مباشر. جرّب سيرفرًا آخر.');
      return;
    }
    final headers = server['headers'] is Map
        ? Map<String, String>.from(
            (server['headers'] as Map).map((k, v) => MapEntry('$k', '$v')),
          )
        : <String, String>{};
    await _launchPlayer(
      context,
      url,
      _fullTitle,
      referer: _text(_episode['pageUrl']),
      headers: headers,
    );
  }
}

int _episodeNumber(Map<String, dynamic> row) {
  final direct = int.tryParse(_text(row['number']));
  if (direct != null && direct > 0) return direct;
  final value = '${_text(row['title'])} ${_text(row['pageUrl'])}';
  final labeled = _episodeNumberFrom(value);
  if (labeled != null) return labeled;
  const digitSets = ['٠١٢٣٤٥٦٧٨٩', '۰۱۲۳۴۵۶۷۸۹'];
  var numericTitle = _text(row['title']);
  for (final digits in digitSets) {
    for (var i = 0; i < digits.length; i++) {
      numericTitle = numericTitle.replaceAll(digits[i], '$i');
    }
  }
  return int.tryParse(numericTitle.trim()) ?? 0;
}

bool _isUsablePlaybackUrl(String value) {
  final uri = Uri.tryParse(value.trim());
  if (uri == null ||
      !uri.hasAuthority ||
      (uri.scheme != 'http' && uri.scheme != 'https')) {
    return false;
  }
  final host = uri.host.toLowerCase();
  const redirectHosts = <String>[
    'google.com',
    'googleadservices.com',
    'doubleclick.net',
    'googlesyndication.com',
  ];
  return !redirectHosts.any(
    (domain) => host == domain || host.endsWith('.$domain'),
  );
}

Future<String> _resolveServerStream(Map<String, dynamic> server) async {
  final direct = _text(server['directStreamUrl']).trim();
  if (_isUsablePlaybackUrl(direct)) return direct;
  final embed = _text(server['embedUrl']).trim();
  if (!_isUsablePlaybackUrl(embed)) return '';
  try {
    final resolved = _text(
      await _native.invokeMethod<String>('media.resolve', {'url': embed}),
    ).trim();
    return _isUsablePlaybackUrl(resolved) ? resolved : '';
  } catch (_) {
    return '';
  }
}

int _integer(dynamic value) =>
    value is int ? value : int.tryParse(_text(value)) ?? 0;

String _normalizedMediaUrl(String value) => (() {
  var decoded = value;
  try {
    decoded = Uri.decodeFull(value);
  } catch (_) {}
  return decoded.trim().replaceAll(RegExp(r'/+$'), '').toLowerCase();
})();

int? _seasonNumberFrom(String value) {
  var text = value
      .toLowerCase()
      .replaceAll(RegExp('[أإآ]'), 'ا')
      .replaceAll('ة', 'ه');
  for (final digits in ['٠١٢٣٤٥٦٧٨٩', '۰۱۲۳۴۵۶۷۸۹']) {
    for (var i = 0; i < digits.length; i++) {
      text = text.replaceAll(digits[i], '$i');
    }
  }
  final ordinalNumbers = <String, int>{
    'الاول': 1,
    'الاولى': 1,
    'الثاني': 2,
    'الثانيه': 2,
    'الثانية': 2,
    'الثالث': 3,
    'الثالثه': 3,
    'الثالثة': 3,
    'الرابع': 4,
    'الرابعه': 4,
    'الرابعة': 4,
    'الخامس': 5,
    'الخامسه': 5,
    'الخامسة': 5,
    'السادس': 6,
    'السادسه': 6,
    'السادسة': 6,
    'السابع': 7,
    'السابعة': 7,
    'الثامن': 8,
    'الثامنه': 8,
    'الثامنة': 8,
    'التاسع': 9,
    'التاسعه': 9,
    'التاسعة': 9,
    'العاشر': 10,
    'العاشره': 10,
    'العاشرة': 10,
  };
  final ordinal = RegExp(
    r'(?:الموسم|موسم|season)\s*(الاولى?|الثانيه?|الثاني|الثالثه?|الثالث|الرابعه?|الرابع|الخامسه?|الخامس|السادسه?|السادس|السابعة|السابع|الثامنه?|الثامن|التاسعه?|التاسع|العاشره?|العاشر)',
  ).firstMatch(text);
  if (ordinal != null) return ordinalNumbers[ordinal.group(1)];
  final numeric = RegExp(
    r'(?:الموسم|موسم|season|(?<![a-z])s)[\s._#=/:-]*(\d{1,2})(?!\d)',
  ).firstMatch(text);
  final number = int.tryParse(numeric?.group(1) ?? '');
  return number != null && number > 0 ? number : null;
}

int? _episodeNumberFrom(String value) {
  var text = value.toLowerCase();
  for (final digits in ['٠١٢٣٤٥٦٧٨٩', '۰۱۲۳۴۵۶۷۸۹']) {
    for (var i = 0; i < digits.length; i++) {
      text = text.replaceAll(digits[i], '$i');
    }
  }
  final match = RegExp(
    r'(?:الحلقة|حلقة|episode|(?<![a-z])eps?|(?<![a-z])ep)[\s._#=/:-]*(\d{1,4})(?!\d)',
  ).firstMatch(text);
  final number = int.tryParse(match?.group(1) ?? '');
  return number != null && number > 0 ? number : null;
}

class SportsPage extends StatefulWidget {
  const SportsPage({super.key});
  @override
  State<SportsPage> createState() => _SportsPageState();
}

class _SportsPageState extends State<SportsPage> {
  String _section = 'matches';
  List<Map<String, dynamic>> _events = [], _categories = [], _channels = [];
  List<Map<String, dynamic>> _footballMatches = [];
  String? _categoryId;
  String? _loadingId, _error;
  bool _loading = true, _refreshing = false;
  bool _scoreRefreshing = false;

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
        NativeBridge.list('football.cached'),
      ]);
      if (mounted)
        setState(() {
          _events = cached[0];
          _categories = cached[1];
          _footballMatches = cached[2];
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
    unawaited(_refreshFootball(force));
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

  Future<void> _refreshFootball(bool force) async {
    if (_scoreRefreshing) return;
    _scoreRefreshing = true;
    try {
      final rows = await NativeBridge.list('football.fetch', {'force': force});
      if (mounted && rows.isNotEmpty) {
        setState(() => _footballMatches = rows);
      }
    } catch (_) {
      // Score and match stream listings are independent; keep the last score cache.
    } finally {
      _scoreRefreshing = false;
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
    if (_events.isEmpty && _loading) return const _SportsMatchSkeleton();
    if (_events.isEmpty) return _empty(_error ?? 'لا توجد مباريات متاحة الآن.');
    return ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(14),
      itemCount: _events.length,
      itemBuilder: (_, index) => _eventCard(_events[index]),
    );
  }

  Widget _channelsList() {
    if (_channels.isEmpty && _loading) return const _SportsChannelSkeleton();
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

  Map<String, dynamic>? _scoreFor(Map<String, dynamic> event) {
    final team1 = event['team1'] is Map
        ? Map<String, dynamic>.from(event['team1'] as Map)
        : const <String, dynamic>{};
    final team2 = event['team2'] is Map
        ? Map<String, dynamic>.from(event['team2'] as Map)
        : const <String, dynamic>{};
    final first = _text(team1['name']);
    final second = _text(team2['name']);
    final eventTime = _matchEpochMillis(event['startTime']);
    Map<String, dynamic>? best;
    var bestScore = -1;
    for (final match in _footballMatches) {
      final homeNames = [
        _text(match['homeTeamArabic']),
        _text(match['homeTeam']),
      ].where((name) => name.isNotEmpty);
      final awayNames = [
        _text(match['awayTeamArabic']),
        _text(match['awayTeam']),
      ].where((name) => name.isNotEmpty);
      final straight =
          _teamNamesMatch(first, homeNames) &&
          _teamNamesMatch(second, awayNames);
      final reversed =
          _teamNamesMatch(first, awayNames) &&
          _teamNamesMatch(second, homeNames);
      if (!straight && !reversed) continue;

      final kickoff = _integer(match['kickoffEpochMs']);
      final timeDelta = eventTime > 0 && kickoff > 0
          ? (eventTime - kickoff).abs()
          : 0;
      if (timeDelta > const Duration(hours: 30).inMilliseconds) continue;
      final score = (straight ? 4 : 2) + (kickoff > 0 ? 1 : 0);
      if (score > bestScore ||
          (score == bestScore &&
              kickoff > 0 &&
              (best == null ||
                  timeDelta <
                      (eventTime - _integer(best['kickoffEpochMs'])).abs()))) {
        best = match;
        bestScore = score;
      }
    }
    return best;
  }

  bool _teamNamesMatch(String name, Iterable<String> candidates) {
    final normalizedName = _teamKey(name);
    if (normalizedName.length < 4) return false;
    for (final candidate in candidates) {
      final normalizedCandidate = _teamKey(candidate);
      if (normalizedCandidate.length < 4) continue;
      if (normalizedName == normalizedCandidate) return true;
      final smaller = normalizedName.length <= normalizedCandidate.length
          ? normalizedName
          : normalizedCandidate;
      final larger = normalizedName.length > normalizedCandidate.length
          ? normalizedName
          : normalizedCandidate;
      if (smaller.length >= 6 && larger.contains(smaller)) return true;
    }
    return false;
  }

  String _teamKey(String value) {
    final normalized = _cleanTitle(value)
        .replaceAll('مانشستر', 'مان')
        .replaceAll(RegExp(r'\b(?:fc|cf|afc|sc|club)\b'), ' ')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
    const aliases = <String, String>{
      'مان يونايتد': 'manchester united',
      'مان سيتي': 'manchester city',
      'ليفانتي': 'levante',
      'اتلتيكو مدريد': 'atletico madrid',
      'اتليتكو مدريد': 'atletico madrid',
      'تشيلسي': 'chelsea',
      'برايتون': 'brighton',
      'ابيسويتش': 'ipswich town',
      'ايبسويتش': 'ipswich town',
      'هال سيتي': 'hull city',
      'ليفربول': 'liverpool',
      'ارسنال': 'arsenal',
      'ريال مدريد': 'real madrid',
      'برشلونه': 'barcelona',
      'توتنهام': 'tottenham',
      'نيوكاسل': 'newcastle united',
      'وست هام': 'west ham united',
    };
    return aliases[normalized] ?? normalized;
  }

  int _matchEpochMillis(dynamic raw) {
    final value = _text(raw).trim();
    final numeric = int.tryParse(value);
    if (numeric != null)
      return numeric < 1000000000000 ? numeric * 1000 : numeric;
    return DateTime.tryParse(value)?.millisecondsSinceEpoch ?? 0;
  }

  Widget _eventCard(Map<String, dynamic> event) {
    final start = int.tryParse(_text(event['startTime'])) ?? 0;
    final end = int.tryParse(_text(event['endTime'])) ?? 0;
    final now = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    final live = start > 0 && start <= now && (end <= 0 || end >= now);
    final id = _text(event['id']);
    final result = _scoreFor(event);
    final resultState = _text(result?['state']);
    final homeScore = _text(result?['homeScore']);
    final awayScore = _text(result?['awayScore']);
    final hasResult =
        (resultState == 'in' || resultState == 'post') &&
        homeScore.isNotEmpty &&
        awayScore.isNotEmpty;
    final resultIsLive = resultState == 'in';
    final resultIsFinished = resultState == 'post';
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
                  if (live || resultIsLive)
                    const _LiveBadge()
                  else if (resultIsFinished)
                    const _FinishedBadge(),
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
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 15),
                    child: hasResult
                        ? Text(
                            '$homeScore  -  $awayScore',
                            style: TextStyle(
                              color: resultIsLive
                                  ? const Color(0xFF16856D)
                                  : _brick,
                              fontWeight: FontWeight.w900,
                              fontSize: 18,
                            ),
                          )
                        : const Text(
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
              if (hasResult)
                Padding(
                  padding: const EdgeInsets.only(bottom: 7),
                  child: Text(
                    resultIsLive
                        ? _text(result?['timeOrStatus']).ifEmpty('مباشر الآن')
                        : 'انتهت المباراة',
                    style: TextStyle(
                      color: resultIsLive ? const Color(0xFF16856D) : _muted,
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
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
      final match = event
          ? _events.firstWhere(
              (row) => _text(row['id']) == id,
              orElse: () => const <String, dynamic>{},
            )
          : const <String, dynamic>{};
      final streams = await NativeBridge.list(
        event ? 'live.matchStreams' : 'live.channelStreams',
        {
          'id': id,
          if (event) 'channel': _text(match['channel']),
          if (event) 'league': _text(match['champions']),
        },
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
                    final backupSources = streams
                        .where(
                          (candidate) =>
                              _text(candidate['url']).trim().isNotEmpty &&
                              _text(candidate['url']).trim() !=
                                  _text(stream['url']).trim(),
                        )
                        .map((candidate) {
                          final candidateHeaders = candidate['headers'] is Map
                              ? Map<String, String>.from(
                                  (candidate['headers'] as Map).map(
                                    (key, value) => MapEntry('$key', '$value'),
                                  ),
                                )
                              : <String, String>{};
                          return <String, dynamic>{
                            'url': _text(candidate['url']).trim(),
                            'userAgent': _text(candidate['userAgent']),
                            'referer': _text(candidate['referer']),
                            'headers': candidateHeaders,
                          };
                        })
                        .toList(growable: false);
                    await _launchPlayer(
                      context,
                      _text(stream['url']),
                      title,
                      userAgent: _text(stream['userAgent']),
                      referer: _text(stream['referer']),
                      headers: headers,
                      backupSources: backupSources,
                      isLive: true,
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

class _FinishedBadge extends StatelessWidget {
  const _FinishedBadge();

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
    decoration: BoxDecoration(
      color: _paper,
      borderRadius: BorderRadius.circular(20),
      border: Border.all(color: _line),
    ),
    child: const Text(
      'انتهت',
      style: TextStyle(
        color: _muted,
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
  List<Map<String, dynamic>> backupSources = const [],
  bool isLive = false,
}) async {
  if (url.trim().isEmpty) {
    _message(context, 'رابط التشغيل غير متاح. اختر سيرفرًا آخر.');
    return;
  }
  final uri = Uri.tryParse(url);
  if (uri == null || !_isUsablePlaybackUrl(url)) {
    _message(context, 'رابط التشغيل غير صالح.');
    return;
  }
  final opened = await NativeBridge.openPlayer(
    url: url,
    title: title,
    userAgent: userAgent,
    referer: referer,
    headers: headers,
    backupSources: backupSources,
    isLive: isLive,
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
