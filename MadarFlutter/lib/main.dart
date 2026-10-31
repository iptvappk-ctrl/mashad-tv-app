import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const apiBaseUrl = String.fromEnvironment(
  'MADAR_API_BASE_URL',
  defaultValue: 'http://10.0.2.2:3000',
);

class ApiConfig {
  static String baseUrl = apiBaseUrl;
  static String normalize(String value) =>
      value.trim().replaceAll(RegExp(r'/+$'), '');
}

const _native = MethodChannel('madar/native');
const _ink = Color(0xFF342633),
    _brick = Color(0xFFB94E3C),
    _paper = Color(0xFFF5E9D8),
    _card = Color(0xFFFFFAF3),
    _muted = Color(0xFF806D78);
void main() => runApp(const MadarFlutterApp());

class MadarFlutterApp extends StatelessWidget {
  const MadarFlutterApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'زقزوقة',
    theme: ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(seedColor: _brick),
      scaffoldBackgroundColor: _paper,
      appBarTheme: const AppBarTheme(
        backgroundColor: _paper,
        foregroundColor: _ink,
        elevation: 0,
      ),
      cardTheme: CardThemeData(
        color: _card,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: _card,
        indicatorColor: _brick.withOpacity(.12),
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

class MadarApi {
  MadarApi._();
  static final instance = MadarApi._();
  final HttpClient _client = HttpClient()
    ..connectionTimeout = const Duration(seconds: 5)
    ..idleTimeout = const Duration(seconds: 20)
    ..autoUncompress = true;
  Uri _uri(String path, [Map<String, String>? query]) {
    final base = Uri.parse(ApiConfig.baseUrl);
    if (base.scheme != 'https' &&
        base.host != '10.0.2.2' &&
        base.host != 'localhost')
      throw const FormatException('استخدم عنوان HTTPS لخادم المحتوى.');
    return base.resolve(path).replace(queryParameters: query);
  }

  Future<Map<String, dynamic>> _get(
    String path, [
    Map<String, String>? query,
  ]) async {
    final uri = _uri(path, query);
    final req = await _client.getUrl(uri).timeout(const Duration(seconds: 7));
    req.headers.set(HttpHeaders.acceptHeader, 'application/json');
    req.headers.set(
      HttpHeaders.userAgentHeader,
      'Zaqzouka-Flutter/1.0 Android',
    );
    final res = await req.close().timeout(const Duration(seconds: 12));
    final body = await utf8.decoder.bind(res).join();
    if (res.statusCode < 200 || res.statusCode >= 300)
      throw HttpException('الخادم رجع ${res.statusCode}', uri: uri);
    final decoded = jsonDecode(body);
    if (decoded is! Map<String, dynamic>)
      throw const FormatException('صيغة رد الخادم غير متوقعة.');
    if (decoded['success'] == false)
      throw FormatException('${decoded['error'] ?? 'تعذر جلب البيانات'}');
    return decoded;
  }

  Future<List<MediaItem>> catalog(String kind, int page) async => _items(
    (await _get(
      kind == 'series' ? '/api/cinema/series' : '/api/cinema/movies',
      {'page': '$page'},
    ))['data'],
  );
  Future<List<MediaItem>> search(String query) async =>
      _items((await _get('/api/cinema/search', {'q': query}))['data']);
  Future<Map<String, dynamic>> detail(String url) async =>
      (await _get('/api/cinema/detail', {'url': url}))['data']
          as Map<String, dynamic>;
  Future<Map<String, dynamic>> resolve(String serverUrl) =>
      _get('/api/cinema/resolve', {'serverUrl': serverUrl});
  Future<List<Map<String, dynamic>>> events() async =>
      _maps((await _get('/api/yacine/events'))['data']);
  Future<List<Map<String, dynamic>>> eventStreams(String id) async => _maps(
    (await _get('/api/yacine/stream', {'type': 'event', 'id': id}))['data'],
  );
  String absolute(String value) {
    final uri = Uri.parse(value);
    return uri.hasScheme
        ? value
        : Uri.parse(ApiConfig.baseUrl).resolve(value).toString();
  }

  List<MediaItem> _items(dynamic raw) {
    final rows = <String, MediaItem>{};
    for (final map in _maps(raw)) {
      final item = MediaItem.fromJson(map);
      if (item.title.trim().isNotEmpty && item.pageUrl.isNotEmpty)
        rows.putIfAbsent(item.dedupeKey, () => item);
    }
    return rows.values.toList(growable: false);
  }

  List<Map<String, dynamic>> _maps(dynamic raw) => raw is List
      ? raw
            .whereType<Map>()
            .map((e) => Map<String, dynamic>.from(e))
            .toList(growable: false)
      : const [];
}

String _normalizeTitle(String value) {
  var text = value.toLowerCase();
  const digits = '٠١٢٣٤٥٦٧٨٩';
  for (var i = 0; i < digits.length; i++) {
    text = text.replaceAll(digits[i], '$i');
  }
  return text
      .replaceAll(RegExp(r'[أإآ]'), 'ا')
      .replaceAll('ة', 'ه')
      .replaceAll('ى', 'ي')
      .replaceAll(RegExp(r'\b(فيلم|مسلسل|مترجم|مدبلج|720p|1080p|4k|hd)\b'), '')
      .replaceAll(RegExp(r'[^\w\s\u0600-\u06ff]'), ' ')
      .replaceAll(RegExp(r'\s+'), ' ')
      .trim();
}

class MediaItem {
  const MediaItem({
    required this.id,
    required this.title,
    required this.poster,
    required this.pageUrl,
    required this.isSeries,
    this.year = '',
    this.quality = '',
    this.provider = '',
    this.providerId = '',
  });
  final String id, title, poster, pageUrl, year, quality, provider, providerId;
  final bool isSeries;
  String get dedupeKey =>
      '${isSeries ? 's' : 'm'}:${_normalizeTitle(title)}:${year.trim()}';
  factory MediaItem.fromJson(Map<String, dynamic> m) => MediaItem(
    id: '${m['id'] ?? m['pageUrl'] ?? ''}',
    title: '${m['title'] ?? ''}',
    poster: '${m['poster'] ?? ''}',
    pageUrl: '${m['pageUrl'] ?? ''}',
    isSeries: m['isSeries'] == true,
    year: '${m['year'] ?? ''}',
    quality: '${m['quality'] ?? ''}',
    provider: '${m['providerName'] ?? ''}',
    providerId: '${m['providerId'] ?? ''}',
  );
  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'poster': poster,
    'pageUrl': pageUrl,
    'isSeries': isSeries,
    'year': year,
    'quality': quality,
    'providerName': provider,
    'providerId': providerId,
  };
}

class RootTabs extends StatefulWidget {
  const RootTabs({super.key});
  @override
  State<RootTabs> createState() => _RootTabsState();
}

class _RootTabsState extends State<RootTabs> {
  int _tab = 0;
  final Set<String> _favorites = {};
  List<MediaItem> _catalogItems = [];
  @override
  void initState() {
    super.initState();
    NativeBridge.readCache('api:base_url').then((raw) {
      if (!mounted || raw == null || raw.trim().isEmpty) return;
      setState(() => ApiConfig.baseUrl = ApiConfig.normalize(raw));
    });
    NativeBridge.readCache('favorites').then((raw) {
      if (!mounted || raw == null) return;
      try {
        setState(
          () => _favorites.addAll((jsonDecode(raw) as List).map((e) => '$e')),
        );
      } catch (_) {}
    });
  }

  void _toggle(MediaItem item) {
    setState(() {
      if (!_favorites.add(item.pageUrl)) _favorites.remove(item.pageUrl);
    });
    NativeBridge.writeCache('favorites', jsonEncode(_favorites.toList()));
  }

  void _onItems(List<MediaItem> rows) => setState(() => _catalogItems = rows);
  void _open(MediaItem item) => Navigator.of(context).push(
    PageRouteBuilder<void>(
      transitionDuration: const Duration(milliseconds: 240),
      pageBuilder: (_, animation, __) => FadeTransition(
        opacity: animation,
        child: DetailPage(item: item),
      ),
    ),
  );
  @override
  Widget build(BuildContext context) {
    final body = switch (_tab) {
      1 => SportsPage(key: ValueKey('sports:${ApiConfig.baseUrl}')),
      2 => FavoritesPage(
        key: const ValueKey('favorites'),
        items: _catalogItems,
        favoriteUrls: _favorites,
        onOpen: _open,
        onToggle: _toggle,
      ),
      _ => CatalogPage(
        key: ValueKey('catalog:${ApiConfig.baseUrl}'),
        favoriteUrls: _favorites,
        onOpen: _open,
        onToggle: _toggle,
        onItems: _onItems,
      ),
    };
    return Scaffold(
      body: SafeArea(
        child: AnimatedSwitcher(
          duration: const Duration(milliseconds: 220),
          switchInCurve: Curves.easeOutCubic,
          transitionBuilder: (child, animation) => FadeTransition(
            opacity: animation,
            child: SlideTransition(
              position: Tween<Offset>(
                begin: const Offset(0, .025),
                end: Offset.zero,
              ).animate(animation),
              child: child,
            ),
          ),
          child: body,
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (i) => setState(() => _tab = i),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.movie_creation_outlined),
            selectedIcon: Icon(Icons.movie_creation),
            label: 'المكتبة',
          ),
          NavigationDestination(
            icon: Icon(Icons.sports_soccer_outlined),
            selectedIcon: Icon(Icons.sports_soccer),
            label: 'المباشر',
          ),
          NavigationDestination(
            icon: Icon(Icons.favorite_border),
            selectedIcon: Icon(Icons.favorite),
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
  final _search = TextEditingController(), _scroll = ScrollController();
  List<MediaItem> _items = [];
  String _filter = 'all';
  String? _error;
  bool _busy = true, _moreBusy = false, _firstPaintWait = true;
  int _page = 1;
  Timer? _paintTimer, _debounce;
  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    _paintTimer = Timer(const Duration(milliseconds: 750), () {
      if (mounted && _items.isEmpty) setState(() => _firstPaintWait = false);
    });
    _load();
  }

  @override
  void dispose() {
    _paintTimer?.cancel();
    _debounce?.cancel();
    _search.dispose();
    _scroll.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 480)
      _loadMore();
  }

  Future<void> _configureApi() async {
    final controller = TextEditingController(text: ApiConfig.baseUrl);
    final value = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('عنوان خادم المحتوى'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.url,
          textDirection: TextDirection.ltr,
          decoration: const InputDecoration(
            hintText: 'https://api.example.com',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('إلغاء'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, controller.text),
            child: const Text('حفظ'),
          ),
        ],
      ),
    );
    controller.dispose();
    if (value == null || value.trim().isEmpty) return;
    try {
      final normalized = ApiConfig.normalize(value);
      final uri = Uri.parse(normalized);
      if (!uri.hasAuthority ||
          (uri.scheme != 'https' &&
              uri.host != '10.0.2.2' &&
              uri.host != 'localhost')) {
        throw const FormatException(
          'استخدم عنوان HTTPS عام، أو عنوان 10.0.2.2 للمحاكي.',
        );
      }
      ApiConfig.baseUrl = normalized;
      await NativeBridge.writeCache('api:base_url', normalized);
      if (mounted) {
        setState(() {
          _items = [];
          _page = 1;
        });
        _load(reset: true);
      }
    } catch (e) {
      if (mounted) _message(context, '$e');
    }
  }

  List<MediaItem> _filtered(List<MediaItem> rows) {
    final unique = <String, MediaItem>{};
    for (final item in rows) {
      if (_filter != 'all' && ((_filter == 'series') != item.isSeries))
        continue;
      unique.putIfAbsent(item.dedupeKey, () => item);
    }
    return unique.values.toList();
  }

  Future<void> _load({bool reset = false}) async {
    if (_busy && !reset) return;
    if (reset) {
      _page = 1;
      _error = null;
    }
    setState(() {
      _busy = true;
      if (_items.isEmpty) _firstPaintWait = true;
    });
    final query = _search.text.trim();
    final cacheKey = query.isNotEmpty
        ? 'search:${query.toLowerCase()}'
        : 'catalog:$_filter:1';
    try {
      final cached = await NativeBridge.readCache(cacheKey);
      if (cached != null && mounted) {
        final raw = jsonDecode(cached);
        if (raw is List) {
          final rows = raw
              .whereType<Map>()
              .map((e) => MediaItem.fromJson(Map<String, dynamic>.from(e)))
              .toList();
          if (rows.isNotEmpty)
            setState(() {
              _items = _filtered(rows);
              _busy = false;
              _firstPaintWait = false;
            });
        }
      }
    } catch (_) {}
    try {
      final rows = query.isNotEmpty
          ? await MadarApi.instance.search(query)
          : _filter == 'all'
          ? (await Future.wait([
              MadarApi.instance.catalog('movies', 1),
              MadarApi.instance.catalog('series', 1),
            ])).expand((x) => x).toList()
          : await MadarApi.instance.catalog(_filter, 1);
      await NativeBridge.writeCache(
        cacheKey,
        jsonEncode(rows.map((e) => e.toJson()).toList()),
      );
      if (mounted)
        setState(() {
          _items = _filtered(rows);
          _error = null;
          _busy = false;
          _firstPaintWait = false;
        });
      widget.onItems(_items);
      _paintTimer?.cancel();
    } catch (e) {
      if (mounted)
        setState(() {
          _error = _items.isEmpty ? '$e' : null;
          _busy = false;
          _firstPaintWait = false;
        });
    }
  }

  Future<void> _loadMore() async {
    if (_moreBusy || _busy || _search.text.trim().isNotEmpty) return;
    _moreBusy = true;
    final next = _page + 1;
    try {
      final kinds = _filter == 'all' ? ['movies', 'series'] : [_filter];
      final rows = (await Future.wait(
        kinds.map((k) => MadarApi.instance.catalog(k, next)),
      )).expand((e) => e).toList();
      if (rows.isNotEmpty && mounted) {
        _page = next;
        setState(() => _items = _filtered([..._items, ...rows]));
        widget.onItems(_items);
      }
    } catch (_) {
    } finally {
      _moreBusy = false;
    }
  }

  Widget _chip(String value, String label) => Padding(
    padding: const EdgeInsets.symmetric(horizontal: 4),
    child: ChoiceChip(
      label: Text(label),
      selected: _filter == value,
      selectedColor: _brick,
      labelStyle: TextStyle(
        color: _filter == value ? Colors.white : _muted,
        fontWeight: FontWeight.w700,
      ),
      onSelected: (_) {
        setState(() {
          _filter = value;
          _items = [];
          _page = 1;
        });
        _load(reset: true);
      },
    ),
  );
  @override
  Widget build(BuildContext context) => Column(
    children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 8),
        child: Row(
          children: [
            const Expanded(
              child: Text(
                'زقزوقة',
                style: TextStyle(
                  fontSize: 27,
                  fontWeight: FontWeight.w900,
                  color: _ink,
                ),
              ),
            ),
            IconButton(
              onPressed: _configureApi,
              icon: const Icon(Icons.dns_outlined, color: _muted),
              tooltip: 'إعداد الخادم',
            ),
            IconButton(
              onPressed: () => _load(reset: true),
              icon: const Icon(Icons.refresh_rounded, color: _brick),
            ),
          ],
        ),
      ),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: TextField(
          controller: _search,
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
            prefixIcon: const Icon(Icons.search),
            filled: true,
            fillColor: _card,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide.none,
            ),
            suffixIcon: _search.text.isEmpty
                ? null
                : IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () {
                      _search.clear();
                      _load(reset: true);
                    },
                  ),
          ),
        ),
      ),
      const SizedBox(height: 9),
      SizedBox(
        height: 42,
        child: ListView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 14),
          children: [
            _chip('all', 'الرئيسية'),
            _chip('movies', 'أفلام'),
            _chip('series', 'مسلسلات'),
          ],
        ),
      ),
      Expanded(
        child: RefreshIndicator(
          color: _brick,
          onRefresh: () => _load(reset: true),
          child: _content(),
        ),
      ),
    ],
  );
  Widget _content() {
    if (_items.isEmpty && _firstPaintWait) return const CatalogSkeleton();
    if (_items.isEmpty && _error != null)
      return ListView(
        children: [
          const SizedBox(height: 100),
          Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              'تعذر الاتصال بخادم المحتوى. تأكد من تشغيله وضبط MADAR_API_BASE_URL.\n$_error',
              textAlign: TextAlign.center,
              style: const TextStyle(color: _muted),
            ),
          ),
          Center(
            child: FilledButton.icon(
              onPressed: () => _load(reset: true),
              icon: const Icon(Icons.refresh),
              label: const Text('إعادة المحاولة'),
            ),
          ),
        ],
      );
    if (_items.isEmpty)
      return ListView(
        children: [
          const SizedBox(height: 130),
          Center(
            child: Text(
              _busy
                  ? 'بنحمّل الكتالوج في الخلفية…'
                  : 'لا يوجد محتوى لعرضه الآن',
              style: const TextStyle(color: _muted),
            ),
          ),
        ],
      );
    return GridView.builder(
      controller: _scroll,
      padding: const EdgeInsets.fromLTRB(14, 8, 14, 22),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        crossAxisSpacing: 10,
        mainAxisSpacing: 13,
        childAspectRatio: .51,
      ),
      itemCount: _items.length + (_moreBusy ? 3 : 0),
      itemBuilder: (context, i) {
        if (i >= _items.length) return const PosterPlaceholder();
        final item = _items[i];
        return MediaCard(
          item: item,
          favorite: widget.favoriteUrls.contains(item.pageUrl),
          onTap: () => widget.onOpen(item),
          onFavorite: () => widget.onToggle(item),
        );
      },
    );
  }
}

class MediaCard extends StatelessWidget {
  const MediaCard({
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
                child: item.poster.startsWith('http')
                    ? Image.network(
                        item.poster,
                        fit: BoxFit.cover,
                        errorBuilder: (_, __, ___) => const PosterPlaceholder(),
                      )
                    : const PosterPlaceholder(),
              ),
              Positioned(
                right: 6,
                top: 6,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: _ink.withOpacity(.78),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 7,
                      vertical: 4,
                    ),
                    child: Text(
                      item.isSeries ? 'مسلسل' : 'فيلم',
                      style: const TextStyle(color: Colors.white, fontSize: 10),
                    ),
                  ),
                ),
              ),
              Positioned(
                left: 1,
                top: 0,
                child: IconButton(
                  visualDensity: VisualDensity.compact,
                  onPressed: onFavorite,
                  icon: Icon(
                    favorite ? Icons.favorite : Icons.favorite_border,
                    color: favorite ? _brick : Colors.white,
                    size: 20,
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
            height: 1.3,
            fontWeight: FontWeight.w700,
          ),
        ),
        if (item.year.isNotEmpty || item.quality.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 3),
            child: Text(
              [item.year, item.quality].where((x) => x.isNotEmpty).join(' · '),
              maxLines: 1,
              textAlign: TextAlign.center,
              style: const TextStyle(color: _muted, fontSize: 10),
            ),
          ),
      ],
    ),
  );
}

class PosterPlaceholder extends StatelessWidget {
  const PosterPlaceholder({super.key});
  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(
      color: _paper,
      borderRadius: BorderRadius.circular(15),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [_paper, Color(0xFFEAD9CA)],
      ),
    ),
    child: const Center(
      child: Icon(Icons.movie_outlined, color: _muted, size: 30),
    ),
  );
}

class CatalogSkeleton extends StatelessWidget {
  const CatalogSkeleton({super.key});
  @override
  Widget build(BuildContext context) => GridView.builder(
    padding: const EdgeInsets.all(14),
    itemCount: 9,
    gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
      crossAxisCount: 3,
      crossAxisSpacing: 10,
      mainAxisSpacing: 13,
      childAspectRatio: .51,
    ),
    itemBuilder: (_, __) => const PosterPlaceholder(),
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
    final rows = items.where((e) => favoriteUrls.contains(e.pageUrl)).toList();
    return Column(
      children: [
        const Padding(
          padding: EdgeInsets.all(18),
          child: Align(
            alignment: Alignment.centerRight,
            child: Text(
              'المفضلة',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: _ink,
              ),
            ),
          ),
        ),
        Expanded(
          child: rows.isEmpty
              ? const Center(
                  child: Text(
                    'أضف أعمالًا للمفضلة من المكتبة.',
                    style: TextStyle(color: _muted),
                  ),
                )
              : GridView.builder(
                  padding: const EdgeInsets.all(14),
                  itemCount: rows.length,
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 3,
                    crossAxisSpacing: 10,
                    mainAxisSpacing: 13,
                    childAspectRatio: .51,
                  ),
                  itemBuilder: (_, i) => MediaCard(
                    item: rows[i],
                    favorite: true,
                    onTap: () => onOpen(rows[i]),
                    onFavorite: () => onToggle(rows[i]),
                  ),
                ),
        ),
      ],
    );
  }
}

class DetailPage extends StatefulWidget {
  const DetailPage({super.key, required this.item});
  final MediaItem item;
  @override
  State<DetailPage> createState() => _DetailPageState();
}

class _DetailPageState extends State<DetailPage> {
  Map<String, dynamic>? _detail;
  List<Map<String, dynamic>> _seasons = [], _episodes = [];
  String? _error;
  bool _loading = true, _seasonLoading = false;
  int _seasonIndex = 0;
  String _activeSeasonUrl = '';
  @override
  void initState() {
    super.initState();
    _loadDetail(widget.item.pageUrl);
  }

  List<Map<String, dynamic>> _maps(dynamic v) => v is List
      ? v.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList()
      : [];
  int _number(dynamic value, String fallback) {
    var text = '$value $fallback';
    const d = '٠١٢٣٤٥٦٧٨٩';
    for (var i = 0; i < d.length; i++) {
      text = text.replaceAll(d[i], '$i');
    }
    return int.tryParse(RegExp(r'\d+').firstMatch(text)?.group(0) ?? '') ?? 0;
  }

  List<Map<String, dynamic>> _sortSeasons(List<Map<String, dynamic>> rows) {
    rows.sort(
      (a, b) => _number(
        a['seasonNumber'],
        '${a['title']}',
      ).compareTo(_number(b['seasonNumber'], '${b['title']}')),
    );
    return rows;
  }

  List<Map<String, dynamic>> _sortEpisodes(List<Map<String, dynamic>> rows) {
    final m = <int, Map<String, dynamic>>{};
    for (final row in rows) {
      final n = _number(row['episodeNumber'], '${row['title']}');
      if (n > 0) m.putIfAbsent(n, () => row);
    }
    final ns = m.keys.toList()..sort();
    return ns.map((n) => m[n]!).toList();
  }

  Future<void> _loadDetail(String url, {bool seasonOnly = false}) async {
    setState(() => seasonOnly ? _seasonLoading = true : _loading = true);
    final cached = await NativeBridge.readCache('detail:$url');
    if (cached != null) {
      try {
        final data = Map<String, dynamic>.from(jsonDecode(cached));
        if (mounted) {
          setState(() {
            if (!seasonOnly) {
              _detail = data;
              _seasons = _sortSeasons(_maps(data['seasons']));
            }
            _episodes = _sortEpisodes(_maps(data['episodes']));
            _activeSeasonUrl = url;
            _loading = false;
            _seasonLoading = false;
          });
        }
      } catch (_) {}
    }
    try {
      final result = await MadarApi.instance.detail(url);
      final seasons = _sortSeasons(_maps(result['seasons']));
      final episodes = _sortEpisodes(_maps(result['episodes']));
      if (!mounted) return;
      setState(() {
        if (!seasonOnly) {
          _detail = result;
          _seasons = seasons;
        }
        _episodes = episodes;
        _activeSeasonUrl = url;
        _loading = false;
        _seasonLoading = false;
      });
      await NativeBridge.writeCache('detail:$url', jsonEncode(result));
      if (!seasonOnly && _seasons.isEmpty && episodes.isNotEmpty)
        setState(
          () => _seasons = [
            {'title': 'الموسم الأول', 'seasonNumber': 1, 'pageUrl': url},
          ],
        );
    } catch (e) {
      if (!seasonOnly) {
        final cached = await NativeBridge.readCache('detail:$url');
        if (cached != null) {
          try {
            final result = Map<String, dynamic>.from(jsonDecode(cached));
            if (mounted)
              setState(() {
                _detail = result;
                _seasons = _sortSeasons(_maps(result['seasons']));
                _episodes = _sortEpisodes(_maps(result['episodes']));
                _loading = false;
              });
            return;
          } catch (_) {}
        }
      }
      if (mounted)
        setState(() {
          _error = '$e';
          _loading = false;
          _seasonLoading = false;
        });
    }
  }

  Future<void> _selectSeason(int index) async {
    if (index < 0 || index >= _seasons.length) return;
    setState(() => _seasonIndex = index);
    final url = '${_seasons[index]['pageUrl'] ?? ''}';
    if (url.isNotEmpty && url != _activeSeasonUrl)
      await _loadDetail(url, seasonOnly: true);
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail ?? {};
    final overview = '${detail['overview'] ?? ''}';
    final cast = detail['cast'] is List
        ? (detail['cast'] as List).map((e) => '$e').toList()
        : <String>[];
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.item.title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ),
      body: _loading
          ? const CatalogSkeleton()
          : _error != null && _detail == null
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  'تعذر تحميل التفاصيل: $_error',
                  textAlign: TextAlign.center,
                ),
              ),
            )
          : ListView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(16),
                      child: SizedBox(
                        width: 128,
                        height: 184,
                        child: widget.item.poster.startsWith('http')
                            ? Image.network(
                                widget.item.poster,
                                fit: BoxFit.cover,
                                errorBuilder: (_, __, ___) =>
                                    const PosterPlaceholder(),
                              )
                            : const PosterPlaceholder(),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            widget.item.title,
                            style: const TextStyle(
                              fontSize: 21,
                              fontWeight: FontWeight.w900,
                              color: _ink,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            [
                              widget.item.year,
                              widget.item.quality,
                              widget.item.provider,
                            ].where((x) => x.isNotEmpty).join(' · '),
                            style: const TextStyle(color: _muted, height: 1.5),
                          ),
                          const SizedBox(height: 14),
                          Text(
                            widget.item.isSeries
                                ? '${_seasons.length} مواسم'
                                : 'فيلم',
                            style: const TextStyle(
                              color: _brick,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                if (overview.isNotEmpty)
                  _InfoCard(
                    title: 'القصة',
                    child: Text(
                      overview,
                      style: const TextStyle(color: _ink, height: 1.7),
                    ),
                  ),
                if (cast.isNotEmpty)
                  _InfoCard(
                    title: 'طاقم العمل',
                    child: Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: cast
                          .map(
                            (e) =>
                                Chip(label: Text(e), backgroundColor: _paper),
                          )
                          .toList(),
                    ),
                  ),
                if (_seasons.isNotEmpty) ...[
                  _InfoCard(
                    title: 'الأجزاء والمواسم',
                    child: Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: List.generate(
                        _seasons.length,
                        (i) => ChoiceChip(
                          label: Text(
                            '${_seasons[i]['title'] ?? 'الموسم ${i + 1}'}',
                          ),
                          selected: _seasonIndex == i,
                          selectedColor: _brick,
                          labelStyle: TextStyle(
                            color: _seasonIndex == i ? Colors.white : _ink,
                          ),
                          onSelected: (_) => _selectSeason(i),
                        ),
                      ),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.only(top: 12, bottom: 6),
                    child: Row(
                      children: [
                        const Expanded(
                          child: Text(
                            'حلقات الموسم المحدد',
                            style: TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.bold,
                              color: _ink,
                            ),
                          ),
                        ),
                        if (_seasonLoading)
                          const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: _brick,
                            ),
                          ),
                      ],
                    ),
                  ),
                  if (_episodes.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(16),
                      child: Text(
                        'لا توجد حلقات مرقمة لهذا الموسم في البيانات.',
                        style: TextStyle(color: _muted),
                      ),
                    ),
                  ...List.generate(_episodes.length, (index) {
                    final ep = _episodes[index];
                    final n = _number(ep['episodeNumber'], '${ep['title']}');
                    return Card(
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: _brick.withOpacity(.12),
                          child: Text(
                            '$n',
                            style: const TextStyle(
                              color: _brick,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        title: Text(
                          '${ep['title'] ?? 'الحلقة $n'}',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        trailing: const Icon(
                          Icons.play_circle_fill,
                          color: _brick,
                        ),
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute<void>(
                            builder: (_) => EpisodePage(
                              episodes: _episodes,
                              initialIndex: index,
                            ),
                          ),
                        ),
                      ),
                    );
                  }),
                ] else if (widget.item.isSeries)
                  _InfoCard(
                    title: 'المواسم والحلقات',
                    child: const Text(
                      'المصدر لم يرجع مواسم واضحة لهذا العمل.',
                      style: TextStyle(color: _muted),
                    ),
                  ),
              ],
            ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.title, required this.child});
  final String title;
  final Widget child;
  @override
  Widget build(BuildContext context) => Card(
    margin: const EdgeInsets.only(top: 14),
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              color: _brick,
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 10),
          child,
        ],
      ),
    ),
  );
}

class EpisodePage extends StatefulWidget {
  const EpisodePage({
    super.key,
    required this.episodes,
    required this.initialIndex,
  });
  final List<Map<String, dynamic>> episodes;
  final int initialIndex;
  @override
  State<EpisodePage> createState() => _EpisodePageState();
}

class _EpisodePageState extends State<EpisodePage> {
  late int _index = widget.initialIndex;
  Map<String, dynamic>? _detail;
  String? _error;
  bool _loading = true;
  Map<String, dynamic> get _episode => widget.episodes[_index];
  String get _title =>
      '${_episode['title'] ?? 'الحلقة ${_episode['episodeNumber'] ?? ''}'}';
  String get _url => '${_episode['pageUrl'] ?? ''}';
  int get _number {
    var value = '${_episode['episodeNumber'] ?? ''} ${_episode['title'] ?? ''}';
    const digits = '٠١٢٣٤٥٦٧٨٩';
    for (var i = 0; i < digits.length; i++) {
      value = value.replaceAll(digits[i], '$i');
    }
    return int.tryParse(RegExp(r'\d+').firstMatch(value)?.group(0) ?? '') ?? 0;
  }

  int _adjacentIndex(int delta) => widget.episodes.indexWhere((row) {
    final value = '${row['episodeNumber'] ?? ''} ${row['title'] ?? ''}'
        .replaceAllMapped(
          RegExp('[٠-٩]'),
          (m) => '${'٠١٢٣٤٥٦٧٨٩'.indexOf(m[0]!)}',
        );
    return (int.tryParse(RegExp(r'\d+').firstMatch(value)?.group(0) ?? '') ??
            0) ==
        _number + delta;
  });

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final key = 'detail:$_url';
    final cached = await NativeBridge.readCache(key);
    if (cached != null) {
      try {
        final data = Map<String, dynamic>.from(jsonDecode(cached));
        if (mounted)
          setState(() {
            _detail = data;
            _loading = false;
            _error = null;
          });
      } catch (_) {}
    }
    try {
      final data = await MadarApi.instance.detail(_url);
      await NativeBridge.writeCache(key, jsonEncode(data));
      if (mounted)
        setState(() {
          _detail = data;
          _loading = false;
          _error = null;
        });
    } catch (e) {
      if (mounted)
        setState(() {
          _error = _detail == null ? '$e' : null;
          _loading = false;
        });
    }
  }

  Future<void> _moveTo(int next) async {
    if (next < 0 || next >= widget.episodes.length) return;
    setState(() {
      _index = next;
      _detail = null;
      _loading = true;
      _error = null;
    });
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final servers = _detail?['servers'] is List
        ? (_detail!['servers'] as List)
              .whereType<Map>()
              .map((e) => Map<String, dynamic>.from(e))
              .toList()
        : <Map<String, dynamic>>[];
    final previous = _adjacentIndex(-1), next = _adjacentIndex(1);
    return Scaffold(
      appBar: AppBar(title: Text(_title)),
      body: _loading && _detail == null
          ? const Center(child: CircularProgressIndicator(color: _brick))
          : _error != null
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  'تعذر تحميل سيرفرات الحلقة: $_error',
                  textAlign: TextAlign.center,
                ),
              ),
            )
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Text(
                  _title,
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.bold,
                    color: _ink,
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: previous < 0
                            ? null
                            : () => _moveTo(previous),
                        child: Text(
                          previous < 0
                              ? 'لا توجد حلقة سابقة'
                              : 'السابق · ${_numberFor(widget.episodes[previous])}',
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: FilledButton(
                        onPressed: next < 0 ? null : () => _moveTo(next),
                        child: Text(
                          next < 0
                              ? 'لا توجد حلقة تالية'
                              : 'التالي · ${_numberFor(widget.episodes[next])}',
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 18),
                if (servers.isEmpty)
                  const Text(
                    'المصدر لم يرجع سيرفرات تشغيل لهذه الحلقة.',
                    style: TextStyle(color: _muted),
                  ),
                ...servers.map(
                  (server) => Card(
                    child: ListTile(
                      title: Text('${server['name'] ?? 'سيرفر مشاهدة'}'),
                      subtitle: Text('${server['quality'] ?? ''}'),
                      trailing: const Icon(
                        Icons.play_circle_fill,
                        color: _brick,
                      ),
                      onTap: () async {
                        final url = '${server['embedUrl'] ?? ''}';
                        if (url.isEmpty) return;
                        try {
                          final resolved = await MadarApi.instance.resolve(url);
                          if (context.mounted)
                            await _launch(
                              context,
                              '${resolved['playUrl'] ?? url}',
                              _title,
                            );
                        } catch (e) {
                          if (context.mounted)
                            _message(context, 'تعذر تجهيز السيرفر: $e');
                        }
                      },
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}

int _numberFor(Map<String, dynamic> episode) {
  var value = '${episode['episodeNumber'] ?? ''} ${episode['title'] ?? ''}';
  const digits = '٠١٢٣٤٥٦٧٨٩';
  for (var i = 0; i < digits.length; i++) {
    value = value.replaceAll(digits[i], '$i');
  }
  return int.tryParse(RegExp(r'\d+').firstMatch(value)?.group(0) ?? '') ?? 0;
}

class SportsPage extends StatefulWidget {
  const SportsPage({super.key});
  @override
  State<SportsPage> createState() => _SportsPageState();
}

class _SportsPageState extends State<SportsPage> {
  List<Map<String, dynamic>> _events = [];
  bool _loading = true;
  String? _error;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final cache = await NativeBridge.readCache('live:events');
      if (cache != null && mounted) {
        final rows = (jsonDecode(cache) as List)
            .whereType<Map>()
            .map((e) => Map<String, dynamic>.from(e))
            .toList();
        setState(() {
          _events = rows;
          _loading = false;
        });
      }
    } catch (_) {}
    try {
      final rows = await MadarApi.instance.events();
      await NativeBridge.writeCache('live:events', jsonEncode(rows));
      if (mounted)
        setState(() {
          _events = rows;
          _loading = false;
          _error = null;
        });
    } catch (e) {
      if (mounted)
        setState(() {
          _error = '$e';
          _loading = false;
        });
    }
  }

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(18, 16, 18, 8),
        child: Row(
          children: [
            const Expanded(
              child: Text(
                'المباريات والبث',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w900,
                  color: _ink,
                ),
              ),
            ),
            IconButton(
              onPressed: _load,
              icon: const Icon(Icons.refresh, color: _brick),
            ),
          ],
        ),
      ),
      Expanded(
        child: _loading && _events.isEmpty
            ? const Center(child: CircularProgressIndicator(color: _brick))
            : _error != null && _events.isEmpty
            ? Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Text(
                    'تعذر جلب جدول المباريات. $_error',
                    textAlign: TextAlign.center,
                  ),
                ),
              )
            : RefreshIndicator(
                color: _brick,
                onRefresh: _load,
                child: ListView.builder(
                  padding: const EdgeInsets.all(14),
                  itemCount: _events.length,
                  itemBuilder: (_, i) => _eventCard(context, _events[i]),
                ),
              ),
      ),
    ],
  );
  Widget _team(dynamic raw) {
    final t = raw is Map ? Map<String, dynamic>.from(raw) : <String, dynamic>{};
    final logo = '${t['logo'] ?? ''}';
    return Column(
      children: [
        if (logo.startsWith('http'))
          ClipOval(
            child: Image.network(
              logo,
              width: 38,
              height: 38,
              fit: BoxFit.cover,
              errorBuilder: (_, __, ___) =>
                  const Icon(Icons.shield, color: _muted),
            ),
          )
        else
          const Icon(Icons.shield, color: _muted, size: 38),
        const SizedBox(height: 6),
        Text(
          '${t['name'] ?? 'فريق'}',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontWeight: FontWeight.bold, color: _ink),
        ),
      ],
    );
  }

  Widget _eventCard(BuildContext context, Map<String, dynamic> e) {
    final start = int.tryParse('${e['startTime'] ?? 0}') ?? 0,
        end = int.tryParse('${e['endTime'] ?? 0}') ?? 0,
        now = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    final live = start <= now && (end == 0 || now <= end);
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: () => _chooseEvent(context, e),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            children: [
              Row(
                children: [
                  Text(
                    '${e['champions'] ?? 'مباراة'}',
                    style: const TextStyle(color: _muted, fontSize: 12),
                  ),
                  const Spacer(),
                  if (live) const _LiveBadge(),
                ],
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(child: _team(e['team1'])),
                  Text(
                    'VS',
                    style: TextStyle(
                      color: _brick.withOpacity(.8),
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  Expanded(child: _team(e['team2'])),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                '${e['channel'] ?? ''}',
                style: const TextStyle(color: _muted, fontSize: 12),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _chooseEvent(
    BuildContext context,
    Map<String, dynamic> e,
  ) async {
    try {
      final streams = await MadarApi.instance.eventStreams('${e['id'] ?? ''}');
      if (!context.mounted) return;
      if (streams.isEmpty) {
        _message(context, 'لا توجد روابط بث متاحة لهذه المباراة الآن.');
        return;
      }
      showModalBottomSheet<void>(
        context: context,
        showDragHandle: true,
        builder: (sheet) => SafeArea(
          child: ListView(
            shrinkWrap: true,
            padding: const EdgeInsets.all(16),
            children: [
              const Text(
                'اختر سيرفر المشاهدة',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: _ink,
                ),
              ),
              ...streams.map(
                (s) => ListTile(
                  title: Text('${s['name'] ?? 'سيرفر'}'),
                  trailing: const Icon(Icons.play_arrow, color: _brick),
                  onTap: () async {
                    Navigator.pop(sheet);
                    final headers = s['headers'] is Map
                        ? Map<String, String>.from(
                            (s['headers'] as Map).map(
                              (k, v) => MapEntry('$k', '$v'),
                            ),
                          )
                        : <String, String>{};
                    await _launch(
                      context,
                      '${s['playUrl'] ?? s['rawUrl'] ?? ''}',
                      '${e['team1']?['name'] ?? ''} × ${e['team2']?['name'] ?? ''}',
                      userAgent: '${s['userAgent'] ?? ''}',
                      referer: '${s['referer'] ?? ''}',
                      headers: headers,
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      );
    } catch (err) {
      if (context.mounted) _message(context, 'تعذر جلب سيرفرات المباراة: $err');
    }
  }
}

class _LiveBadge extends StatelessWidget {
  const _LiveBadge();
  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(
      color: const Color(0xFFE8F5EF),
      borderRadius: BorderRadius.circular(20),
    ),
    child: const Padding(
      padding: EdgeInsets.symmetric(horizontal: 9, vertical: 4),
      child: Text(
        'مباشر',
        style: TextStyle(
          color: Color(0xFF14845D),
          fontWeight: FontWeight.bold,
          fontSize: 11,
        ),
      ),
    ),
  );
}

Future<void> _launch(
  BuildContext context,
  String url,
  String title, {
  String userAgent = '',
  String referer = '',
  Map<String, String> headers = const {},
}) async {
  final absolute = MadarApi.instance.absolute(url);
  final launched = await NativeBridge.openPlayer(
    url: absolute,
    title: title,
    userAgent: userAgent,
    referer: referer,
    headers: headers,
  );
  if (!context.mounted) return;
  if (!launched) {
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
}

void _message(BuildContext context, String text) =>
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
