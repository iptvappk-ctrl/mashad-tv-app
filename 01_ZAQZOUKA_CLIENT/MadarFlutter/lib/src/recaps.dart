part of '../main.dart';

class RecapChannel {
  const RecapChannel({
    required this.name,
    required this.channelId,
    required this.emoji,
  });

  final String name;
  final String channelId;
  final String emoji;
}

/// The 27 dedicated movie recap channels requested by the user
const List<RecapChannel> _kRecapChannels = [
  RecapChannel(
    name: 'فيلم في الخمسينة',
    channelId: 'UCqQkYvn14_QAjheEhCD5cnA',
    emoji: '⏱️',
  ),
  RecapChannel(
    name: 'كوكلي Cokley',
    channelId: 'UCY0hnHS0P2e63w0h834YHqw',
    emoji: '🎙️',
  ),
  RecapChannel(
    name: 'كوكلي 2',
    channelId: 'UCSPGHRzefMndkpMqRHZplaA',
    emoji: '🎬',
  ),
  RecapChannel(
    name: 'الخلاصة Extra',
    channelId: 'UCZFjMYifBVsil4YLg9g4rMQ',
    emoji: '⚡',
  ),
  RecapChannel(
    name: 'الملخص Mo5alas',
    channelId: 'UCEH9vQrM8F71Ht4lqin3bog',
    emoji: '🍿',
  ),
  RecapChannel(
    name: 'Film Gdeed',
    channelId: 'UC7fYsd6YpOq8LhA9eMYy3qQ',
    emoji: '🎞️',
  ),
  RecapChannel(
    name: 'مدفع الأفلام',
    channelId: 'UC6RorEP9vIC8dNfb4jX32qg',
    emoji: '💣',
  ),
  RecapChannel(
    name: 'تلخيصة FILM',
    channelId: 'UCkC5_-qsYxF8b3Toj0rO2XQ',
    emoji: '📽️',
  ),
  RecapChannel(
    name: 'مروان أحمد',
    channelId: 'UC5gArfM8TG6jyR441LYM52A',
    emoji: '🌟',
  ),
  RecapChannel(
    name: 'البوب Cokley',
    channelId: 'UCS--g81AqlXG4ik9Xm47QPQ',
    emoji: '🍿',
  ),
  RecapChannel(
    name: 'مستر كوكلي',
    channelId: 'UCr_bGuXC-v1H4jfLXWCbOTw',
    emoji: '🎩',
  ),
  RecapChannel(
    name: 'في كلمة ونص',
    channelId: 'UCW8Xf9MFA76rvAvSwpuK4qQ',
    emoji: '⏱️',
  ),
  RecapChannel(
    name: 'فيلم في الزتونة',
    channelId: 'UCtpH2xoaAVfySd_UoVO54EA',
    emoji: '🫒',
  ),
  RecapChannel(
    name: 'أشرحلي فيلم',
    channelId: 'UCeCRMlOvq8XNjHm3Zd3QMgQ',
    emoji: '💡',
  ),
  RecapChannel(
    name: 'فيلمي فيلمك',
    channelId: 'UCzJSTqSLK1w4otc27MZS3mQ',
    emoji: '🎥',
  ),
  RecapChannel(
    name: 'ملخصات الشاشة',
    channelId: 'UCA9TC_cXejDARvl-VkX99qQ',
    emoji: '📺',
  ),
  RecapChannel(
    name: 'ملخص فيلم في البيكو',
    channelId: 'UCeBthVbKg6B6742FgYK7j3w',
    emoji: '☕',
  ),
  RecapChannel(
    name: 'MoviesLimbo',
    channelId: 'UCh3HYI_do6uwzM6mS86wFMQ',
    emoji: '🪐',
  ),
  RecapChannel(
    name: 'Wael Cokley',
    channelId: 'UC9h-Q9yhgZZO9ta7tfHNigQ',
    emoji: '🎬',
  ),
  RecapChannel(
    name: 'عرش السيما',
    channelId: 'UCfIB_5-dlRTeYax2Jh42QQA',
    emoji: '👑',
  ),
  RecapChannel(
    name: 'نايت موفي',
    channelId: 'UCHlrPATgHt19EbT5Npka78A',
    emoji: '🌙',
  ),
  RecapChannel(
    name: 'أوفردوز أفلام',
    channelId: 'UC4YnFnndBBeSOAI2BiakiXg',
    emoji: '🔥',
  ),
  RecapChannel(
    name: 'Movies Killer',
    channelId: 'UC6Ydi1nQuNMejko4q0NWEhg',
    emoji: '⚔️',
  ),
  RecapChannel(
    name: 'محترف موفيز',
    channelId: 'UCsqradczzQY7fTKpDCVP9QA',
    emoji: '🎯',
  ),
  RecapChannel(
    name: 'إكسلانس أفلام',
    channelId: 'UCGV-VN8Ui_NAJrcZcs-SSRg',
    emoji: '💎',
  ),
  RecapChannel(
    name: 'فيلم في الخلاصة',
    channelId: 'UChO6HRowsTQHmELCeGy4G_Q',
    emoji: '💡',
  ),
  RecapChannel(
    name: 'فيلمتشو',
    channelId: 'UC26gMQ-F5XJ0COgPnQfL1uQ',
    emoji: '🍿',
  ),
];

class MovieRecapItem {
  const MovieRecapItem({
    required this.id,
    required this.title,
    required this.channelName,
    required this.channelId,
    required this.publishedAt,
    required this.thumbnailUrl,
  });

  final String id;
  final String title;
  final String channelName;
  final String channelId;
  final DateTime? publishedAt;
  final String thumbnailUrl;

  /// Exact original high-definition 16:9 thumbnail uploaded by creator
  String get exactThumbnailUrl =>
      'https://i.ytimg.com/vi/$id/maxresdefault.jpg';
  String get fallbackThumbnailUrl => 'https://i.ytimg.com/vi/$id/mqdefault.jpg';

  /// Direct YouTube watch URL for ZAG Player
  String get videoUrl => 'https://www.youtube.com/watch?v=$id';

  Map<String, dynamic> toMap() => {
    'id': id,
    'title': title,
    'channelName': channelName,
    'channelId': channelId,
    'publishedAt': publishedAt?.toIso8601String(),
    'thumbnailUrl': thumbnailUrl,
  };

  factory MovieRecapItem.fromMap(Map<String, dynamic> map) => MovieRecapItem(
    id: map['id']?.toString() ?? '',
    title: map['title']?.toString() ?? '',
    channelName: map['channelName']?.toString() ?? '',
    channelId: map['channelId']?.toString() ?? '',
    publishedAt: map['publishedAt'] != null
        ? DateTime.tryParse(map['publishedAt'].toString())
        : null,
    thumbnailUrl: map['thumbnailUrl']?.toString() ?? '',
  );
}

class RecapsPage extends StatefulWidget {
  const RecapsPage({super.key});

  @override
  State<RecapsPage> createState() => _RecapsPageState();
}

class _RecapsPageState extends State<RecapsPage> {
  final TextEditingController _searchCtrl = TextEditingController();
  final PageController _featuredController = PageController(
    viewportFraction: .94,
  );
  List<MovieRecapItem> _items = [];
  String? _selectedChannelId; // null = 'الكل'
  String _searchQuery = '';
  bool _loading = true;
  bool _refreshing = false;
  String? _error;
  int _featuredIndex = 0;
  int _visibleCount = 45;

  static const String _cacheKey = 'recaps:cache:v3';

  @override
  void initState() {
    super.initState();
    _loadInitial();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    _featuredController.dispose();
    super.dispose();
  }

  Future<void> _loadInitial() async {
    // 1. Try loading cached recaps immediately
    try {
      final cachedJson = await NativeBridge.readCache(_cacheKey);
      if (cachedJson != null && cachedJson.isNotEmpty) {
        final decoded = jsonDecode(cachedJson) as List<dynamic>;
        final cachedList = decoded
            .map((e) => MovieRecapItem.fromMap(e as Map<String, dynamic>))
            .where((item) => item.id.isNotEmpty && item.title.isNotEmpty)
            .toList();

        if (cachedList.isNotEmpty && mounted) {
          setState(() {
            _items = cachedList;
            _loading = false;
          });
        }
      }
    } catch (_) {}

    // 2. Fetch fresh items from YouTube RSS feeds
    await _refresh();
  }

  Future<void> _refresh() async {
    if (!mounted) return;
    setState(() {
      _refreshing = true;
      _error = null;
    });

    try {
      final freshList = await _fetchFromAllChannels(
        onProgress: (items) {
          if (!mounted) return;
          setState(() {
            _items = items;
            _loading = false;
          });
        },
      );
      if (!mounted) return;
      if (freshList.isNotEmpty) {
        setState(() {
          _items = freshList;
          _loading = false;
        });
        unawaited(
          NativeBridge.writeCache(
            _cacheKey,
            jsonEncode(freshList.map((e) => e.toMap()).toList()),
          ),
        );
      } else if (_items.isEmpty) {
        setState(() {
          _loading = false;
          _error = 'تعذر تحميل الملخصات حالياً. تحقق من اتصال الإنترنت.';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _loading = false;
          if (_items.isEmpty) {
            _error = 'حدث خطأ أثناء جلب ملخصات الأفلام.';
          }
        });
      }
    } finally {
      if (mounted) setState(() => _refreshing = false);
    }
  }

  Future<List<MovieRecapItem>> _fetchFromAllChannels({
    ValueChanged<List<MovieRecapItem>>? onProgress,
  }) async {
    final client = HttpClient()
      ..connectionTimeout = const Duration(seconds: 12);
    final channelResults = List<List<MovieRecapItem>>.generate(
      _kRecapChannels.length,
      (_) => const <MovieRecapItem>[],
    );
    var nextChannel = 0;

    List<MovieRecapItem> mergedResults() {
      final byId = <String, MovieRecapItem>{};
      for (final channelItems in channelResults) {
        for (final item in channelItems) {
          byId.putIfAbsent(item.id, () => item);
        }
      }
      final indexed = byId.values.toList().asMap().entries.toList();
      indexed.sort((a, b) {
        final left = a.value.publishedAt;
        final right = b.value.publishedAt;
        if (left != null && right != null) {
          final byDate = right.compareTo(left);
          if (byDate != 0) return byDate;
        } else if (left != null) {
          return -1;
        } else if (right != null) {
          return 1;
        }
        return a.key.compareTo(b.key);
      });
      return indexed.map((entry) => entry.value).toList(growable: false);
    }

    Future<void> fetchWorker() async {
      while (true) {
        final index = nextChannel++;
        if (index >= _kRecapChannels.length) return;
        final channel = _kRecapChannels[index];
        try {
          // YouTube moved the public Atom endpoint to /xml/feeds. Some channels
          // still return an empty feed, so fall back to the public Videos page.
          final uri = Uri.parse(
            'https://www.youtube.com/xml/feeds/videos.xml?channel_id=${channel.channelId}',
          );
          final req = await client
              .getUrl(uri)
              .timeout(const Duration(seconds: 12));
          req.headers.set(
            'User-Agent',
            'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
          );
          req.headers.set('Accept-Language', 'ar,en-US;q=0.9,en;q=0.8');
          final resp = await req.close().timeout(const Duration(seconds: 12));
          if (resp.statusCode == 200) {
            final body = await utf8
                .decodeStream(resp)
                .timeout(const Duration(seconds: 12));
            channelResults[index] = _parseRssFeed(body, channel);
          } else {
            channelResults[index] = const <MovieRecapItem>[];
          }
          if (channelResults[index].isEmpty) {
            channelResults[index] = await _fetchChannelVideosPage(
              client,
              channel,
            );
          }
        } catch (_) {
          channelResults[index] = await _fetchChannelVideosPage(
            client,
            channel,
          ).catchError((_) => const <MovieRecapItem>[]);
        }
        onProgress?.call(mergedResults());
      }
    }

    try {
      // A small fan-out avoids YouTube rate limits while the public channel page
      // is used as a fallback. Every completed channel updates the list at once.
      await Future.wait(List.generate(3, (_) => fetchWorker()));
    } finally {
      client.close();
    }
    return mergedResults();
  }

  Future<List<MovieRecapItem>> _fetchChannelVideosPage(
    HttpClient client,
    RecapChannel channel,
  ) async {
    final uri = Uri.https(
      'www.youtube.com',
      '/channel/${channel.channelId}/videos',
    );
    final request = await client
        .getUrl(uri)
        .timeout(const Duration(seconds: 12));
    request.headers.set(
      'User-Agent',
      'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    );
    request.headers.set('Accept-Language', 'ar,en-US;q=0.9,en;q=0.8');
    final response = await request.close().timeout(const Duration(seconds: 12));
    if (response.statusCode != HttpStatus.ok) {
      await response.drain<void>();
      return const <MovieRecapItem>[];
    }
    final html = await utf8
        .decodeStream(response)
        .timeout(const Duration(seconds: 12));
    return _parseChannelVideosPage(html, channel);
  }

  List<MovieRecapItem> _parseChannelVideosPage(
    String html,
    RecapChannel channel,
  ) {
    dynamic data;
    try {
      data = _extractInitialData(html);
    } catch (_) {
      return const <MovieRecapItem>[];
    }
    if (data == null) return const <MovieRecapItem>[];

    final items = <MovieRecapItem>[];
    final seen = <String>{};
    void visit(dynamic value) {
      if (value is Map) {
        final lockup = value['lockupViewModel'];
        if (lockup is Map) {
          final item = _parseLockupRenderer(lockup, channel);
          if (item != null && seen.add(item.id)) items.add(item);
        }
        for (final key in const [
          'compactVideoRenderer',
          'videoRenderer',
          'gridVideoRenderer',
        ]) {
          final renderer = value[key];
          if (renderer is Map) {
            final item = _parseVideoRenderer(renderer, channel);
            if (item != null && seen.add(item.id)) items.add(item);
          }
        }
        for (final child in value.values) {
          visit(child);
        }
      } else if (value is List) {
        for (final child in value) {
          visit(child);
        }
      }
    }

    visit(data);
    return items;
  }

  MovieRecapItem? _parseLockupRenderer(Map renderer, RecapChannel channel) {
    if (renderer['contentType']?.toString() != 'LOCKUP_CONTENT_TYPE_VIDEO') {
      return null;
    }
    final id = renderer['contentId']?.toString().trim() ?? '';
    final metadata = renderer['metadata'];
    final lockupMetadata = metadata is Map
        ? metadata['lockupMetadataViewModel']
        : null;
    final title = lockupMetadata is Map
        ? _youtubeText(lockupMetadata['title']).trim()
        : '';
    if (id.length != 11 || title.isEmpty || _isShortVideo(title)) return null;

    final contentImage = renderer['contentImage'];
    final thumbnailViewModel = contentImage is Map
        ? contentImage['thumbnailViewModel']
        : null;
    final image = thumbnailViewModel is Map
        ? thumbnailViewModel['image']
        : null;
    final sources = image is Map ? image['sources'] : null;
    final sourceRows = sources is List
        ? sources.whereType<Map>().toList()
        : const <Map>[];
    final thumbnail = sourceRows.isNotEmpty
        ? sourceRows.last['url']?.toString() ?? ''
        : '';

    DateTime? publishedAt;
    if (lockupMetadata is Map) {
      final details = lockupMetadata['metadata'];
      final contentMetadata = details is Map
          ? details['contentMetadataViewModel']
          : null;
      final rows = contentMetadata is Map
          ? contentMetadata['metadataRows']
          : null;
      if (rows is List) {
        for (final row in rows.whereType<Map>()) {
          final parts = row['metadataParts'];
          if (parts is! List) continue;
          for (final part in parts.whereType<Map>()) {
            final label =
                part['accessibilityLabel']?.toString() ??
                _youtubeText(part['text']);
            if (label.contains('قبل') ||
                label.contains('منذ') ||
                label.toLowerCase().contains('ago')) {
              publishedAt = _youtubePublishedAt(label);
              if (publishedAt != null) break;
            }
          }
          if (publishedAt != null) break;
        }
      }
    }

    return MovieRecapItem(
      id: id,
      title: title,
      channelName: channel.name,
      channelId: channel.channelId,
      publishedAt: publishedAt,
      thumbnailUrl: thumbnail.isEmpty
          ? 'https://i.ytimg.com/vi/$id/maxresdefault.jpg'
          : thumbnail,
    );
  }

  dynamic _extractInitialData(String html) {
    final assignment = RegExp(r'ytInitialData\s*=\s*').firstMatch(html);
    if (assignment == null) return null;
    final start = assignment.end;
    if (start >= html.length) return null;

    final first = html[start];
    if (first == "'" || first == '"') {
      final quote = first;
      var escaped = false;
      for (var index = start + 1; index < html.length; index++) {
        final char = html[index];
        if (escaped) {
          escaped = false;
          continue;
        }
        if (char.codeUnitAt(0) == 92) {
          escaped = true;
          continue;
        }
        if (char == quote) {
          final encoded = html.substring(start + 1, index);
          return jsonDecode(_decodeJavaScriptString(encoded));
        }
      }
      return null;
    }

    if (first != '{') return null;
    var depth = 0;
    var inString = false;
    var escaped = false;
    for (var index = start; index < html.length; index++) {
      final char = html[index];
      if (inString) {
        if (escaped) {
          escaped = false;
        } else if (char.codeUnitAt(0) == 92) {
          escaped = true;
        } else if (char == '"') {
          inString = false;
        }
        continue;
      }
      if (char == '"') {
        inString = true;
      } else if (char == '{') {
        depth++;
      } else if (char == '}' && --depth == 0) {
        return jsonDecode(html.substring(start, index + 1));
      }
    }
    return null;
  }

  MovieRecapItem? _parseVideoRenderer(Map renderer, RecapChannel channel) {
    final id = renderer['videoId']?.toString().trim() ?? '';
    final title = _youtubeText(renderer['title']).trim();
    if (id.length != 11 || title.isEmpty || _isShortVideo(title)) return null;
    final thumbnails = renderer['thumbnail'] is Map
        ? (renderer['thumbnail'] as Map)['thumbnails']
        : null;
    final thumbRows = thumbnails is List
        ? thumbnails.whereType<Map>().toList()
        : const <Map>[];
    final thumbnail = thumbRows.isNotEmpty
        ? thumbRows.last['url']?.toString() ?? ''
        : '';
    final publishedAt = _youtubePublishedAt(
      _youtubeText(renderer['publishedTimeText']),
    );
    return MovieRecapItem(
      id: id,
      title: title,
      channelName: channel.name,
      channelId: channel.channelId,
      publishedAt: publishedAt,
      thumbnailUrl: thumbnail.isEmpty
          ? 'https://i.ytimg.com/vi/$id/maxresdefault.jpg'
          : thumbnail,
    );
  }

  String _youtubeText(dynamic value) {
    if (value is String) return value;
    if (value is! Map) return '';
    final simple = value['simpleText'];
    if (simple is String) return simple;
    final content = value['content'];
    if (content is String) return content;
    final runs = value['runs'];
    if (runs is List) {
      return runs
          .whereType<Map>()
          .map((run) => run['text']?.toString() ?? '')
          .join();
    }
    return '';
  }

  DateTime? _youtubePublishedAt(String value) {
    var text = value.toLowerCase();
    for (final digits in ['٠١٢٣٤٥٦٧٨٩', '۰۱۲۳۴۵۶۷۸۹']) {
      for (var index = 0; index < digits.length; index++) {
        text = text.replaceAll(digits[index], '$index');
      }
    }
    final amountMatch = RegExp(r'(\d+)').firstMatch(text);
    final amount = int.tryParse(amountMatch?.group(1) ?? '') ?? 1;
    final unit = text;
    Duration? age;
    if (unit.contains('دقيقة') || unit.contains('minute')) {
      age = Duration(minutes: amount);
    } else if (unit.contains('ساعة') || unit.contains('hour')) {
      age = Duration(hours: amount);
    } else if (unit.contains('يوم') || unit.contains('day')) {
      age = Duration(days: amount);
    } else if (unit.contains('أسبوع') ||
        unit.contains('اسبوع') ||
        unit.contains('week')) {
      age = Duration(days: amount * 7);
    } else if (unit.contains('شهر') || unit.contains('month')) {
      age = Duration(days: amount * 30);
    } else if (unit.contains('سنة') ||
        unit.contains('عام') ||
        unit.contains('year')) {
      age = Duration(days: amount * 365);
    }
    return age == null
        ? DateTime.tryParse(value)
        : DateTime.now().subtract(age);
  }

  bool _isShortVideo(String value) {
    final lower = value.toLowerCase();
    return lower.contains('#short') ||
        lower.contains('#reels') ||
        lower.contains('#ريلز') ||
        lower.contains('#شورتس') ||
        lower.contains('#tiktok') ||
        lower.contains('#ستوري');
  }

  String _decodeJavaScriptString(String input) {
    final result = StringBuffer();
    for (var index = 0; index < input.length; index++) {
      final char = input[index];
      if (char != r'\' || index + 1 >= input.length) {
        result.write(char);
        continue;
      }
      final escaped = input[++index];
      final hexLength = escaped == 'x'
          ? 2
          : escaped == 'u'
          ? 4
          : 0;
      if (hexLength > 0 && index + hexLength < input.length) {
        final digits = input.substring(index + 1, index + 1 + hexLength);
        final codeUnit = int.tryParse(digits, radix: 16);
        if (codeUnit != null) {
          result.writeCharCode(codeUnit);
          index += hexLength;
          continue;
        }
      }
      result.write(switch (escaped) {
        'n' => '\n',
        'r' => '\r',
        't' => '\t',
        'b' => '\b',
        'f' => '\f',
        _ => escaped,
      });
    }
    return result.toString();
  }

  List<MovieRecapItem> _parseRssFeed(String xml, RecapChannel channel) {
    final items = <MovieRecapItem>[];
    final entryMatches = RegExp(r'<entry[\s\S]*?<\/entry>').allMatches(xml);

    for (final match in entryMatches) {
      final entry = match.group(0) ?? '';

      // Video ID
      final videoIdMatch = RegExp(r'<yt:videoId>([^<]+)<\/yt:videoId>')
          .firstMatch(entry);
      final videoId = videoIdMatch?.group(1)?.trim() ?? '';
      if (videoId.isEmpty) continue;

      // Title
      final titleMatch = RegExp(r'<title[^>]*>([^<]+)<\/title>')
          .firstMatch(entry);
      var rawTitle = titleMatch?.group(1)?.trim() ?? '';
      final title = _decodeXmlEntities(rawTitle);
      if (title.isEmpty) continue;

      // Description & Strict Shorts / Reels Filter
      final descMatch = RegExp(
        r'<media:description>([\s\S]*?)<\/media:description>',
      ).firstMatch(entry);
      final rawDesc = (descMatch?.group(1) ?? '').toLowerCase();
      final lowerTitle = title.toLowerCase();
      final lowerEntry = entry.toLowerCase();

      // Exclude any video containing Shorts or Reels markers
      final isShort =
          lowerTitle.contains('#short') ||
          lowerTitle.contains('#shorts') ||
          lowerTitle.contains('#reels') ||
          lowerTitle.contains('#ريلز') ||
          lowerTitle.contains('#شورتس') ||
          lowerTitle.contains('#tiktok') ||
          lowerTitle.contains('#ستوري') ||
          lowerTitle.contains('#shortsfeed') ||
          rawDesc.contains('#short') ||
          rawDesc.contains('#shorts') ||
          rawDesc.contains('#reels') ||
          rawDesc.contains('#ريلز') ||
          rawDesc.contains('#شورتس') ||
          rawDesc.contains('#tiktok') ||
          lowerEntry.contains('#shorts') ||
          lowerEntry.contains('#short') ||
          lowerEntry.contains('#reels') ||
          lowerEntry.contains('#ريلز') ||
          lowerEntry.contains('#شورتس');

      if (isShort) {
        continue; // Completely SKIP Reels and Shorts!
      }

      // Published Date
      final publishedMatch = RegExp(r'<published>([^<]+)<\/published>')
          .firstMatch(entry);
      DateTime? publishedAt;
      if (publishedMatch != null) {
        publishedAt = DateTime.tryParse(publishedMatch.group(1)?.trim() ?? '');
      }

      // Thumbnail: Use the EXACT full-resolution 16:9 thumbnail uploaded by the creator
      final thumbMatch = RegExp(r'<media:thumbnail[^>]+url="([^"]+)"')
          .firstMatch(entry);
      final rawThumb = thumbMatch?.group(1)?.trim();
      final thumb = (rawThumb != null && rawThumb.isNotEmpty)
          ? rawThumb.replaceAll('hqdefault.jpg', 'maxresdefault.jpg')
          : 'https://i.ytimg.com/vi/$videoId/maxresdefault.jpg';

      items.add(
        MovieRecapItem(
          id: videoId,
          title: title,
          channelName: channel.name,
          channelId: channel.channelId,
          publishedAt: publishedAt,
          thumbnailUrl: thumb,
        ),
      );
    }

    return items;
  }

  String _decodeXmlEntities(String input) {
    return input
        .replaceAll('&quot;', '"')
        .replaceAll('&amp;', '&')
        .replaceAll('&apos;', "'")
        .replaceAll('&#39;', "'")
        .replaceAll('&lt;', '<')
        .replaceAll('&gt;', '>')
        .replaceAll('&#x2F;', '/')
        .replaceAll('&#47;', '/');
  }

  List<MovieRecapItem> get _filteredItems {
    return _items.where((item) {
      if (_selectedChannelId != null && item.channelId != _selectedChannelId) {
        return false;
      }
      if (_searchQuery.isNotEmpty) {
        final query = _searchQuery.toLowerCase();
        final matchTitle = item.title.toLowerCase().contains(query);
        final matchChannel = item.channelName.toLowerCase().contains(query);
        if (!matchTitle && !matchChannel) return false;
      }
      return true;
    }).toList();
  }

  Future<void> _openRecap(MovieRecapItem item) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => _RecapDetailPage(
          item: item,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final list = _filteredItems;
    final featuredItems = _items.take(6).toList(growable: false);
    final visibleItems = list.take(_visibleCount).toList(growable: false);

    return RefreshIndicator(
      color: _brick,
      backgroundColor: _card,
      onRefresh: _refresh,
      child: CustomScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        slivers: [
          // ── App Bar / Header ──
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 12, 18, 6),
              child: Row(
                children: [
                  const _LogoMark(size: 42),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Text(
                              'ملخصات الأفلام',
                              style: TextStyle(
                                color: _ink,
                                fontSize: 20,
                                fontWeight: FontWeight.w900,
                                letterSpacing: -0.3,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 3),
                        const Text(
                          'أفضل قنوات السينما العربية والعالمية في مكان واحد',
                          style: TextStyle(
                            color: _muted,
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (_refreshing)
                    const SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.2,
                        color: _brick,
                      ),
                    )
                  else
                    Container(
                      decoration: BoxDecoration(
                        color: _paper,
                        shape: BoxShape.circle,
                      ),
                      child: IconButton(
                        icon: const Icon(
                          Icons.refresh_rounded,
                          color: _ink,
                          size: 20,
                        ),
                        tooltip: 'تحديث الملخصات',
                        onPressed: _refresh,
                      ),
                    ),
                ],
              ),
            ),
          ),

          if (featuredItems.isNotEmpty &&
              _searchQuery.isEmpty &&
              _selectedChannelId == null)
            SliverToBoxAdapter(
              child: _RecapFeaturedCarousel(
                items: featuredItems,
                controller: _featuredController,
                selectedIndex: _featuredIndex,
                onPageChanged: (index) =>
                    setState(() => _featuredIndex = index),
                onOpen: _openRecap,
              ),
            ),

          // ── Search Bar ──
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 10),
              child: Container(
                decoration: BoxDecoration(
                  color: _card,
                  borderRadius: BorderRadius.circular(16),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.03),
                      blurRadius: 10,
                      offset: const Offset(0, 3),
                    ),
                  ],
                  border: Border.all(color: _ink.withOpacity(0.08), width: 1),
                ),
                child: TextField(
                  controller: _searchCtrl,
                  onChanged: (val) => setState(() {
                    _searchQuery = val.trim();
                    _visibleCount = 45;
                  }),
                  onSubmitted: (val) {
                    if (val.trim().isNotEmpty) {
                      AppTelemetry.trackSearch(query: val.trim(), found: true);
                    }
                  },
                  style: const TextStyle(
                    color: _ink,
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                  ),
                  decoration: InputDecoration(
                    border: InputBorder.none,
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 12,
                    ),
                    hintText: 'ابحث في الملخصات أو اسم القناة...',
                    hintStyle: const TextStyle(color: _muted, fontSize: 13),
                    prefixIcon: const Icon(
                      Icons.search_rounded,
                      color: _brick,
                      size: 22,
                    ),
                    suffixIcon: _searchQuery.isNotEmpty
                        ? IconButton(
                            icon: const Icon(
                              Icons.close_rounded,
                              color: _muted,
                              size: 18,
                            ),
                            onPressed: () {
                              _searchCtrl.clear();
                              setState(() {
                                _searchQuery = '';
                                _visibleCount = 45;
                              });
                            },
                          )
                        : null,
                  ),
                ),
              ),
            ),
          ),

          // ── Channels Chips Bar ──
          SliverToBoxAdapter(
            child: SizedBox(
              height: 42,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                children: [
                  _ChannelChip(
                    label: 'الكل',
                    selected: _selectedChannelId == null,
                    count: _items.isNotEmpty ? '${_items.length}' : null,
                    onTap: () => setState(() {
                      _selectedChannelId = null;
                      _visibleCount = 45;
                    }),
                  ),
                  ..._kRecapChannels.map((channel) {
                    final selected = _selectedChannelId == channel.channelId;
                    return _ChannelChip(
                      label: channel.name,
                      selected: selected,
                      onTap: () => setState(() {
                        _selectedChannelId = selected
                            ? null
                            : channel.channelId;
                        _visibleCount = 45;
                      }),
                    );
                  }),
                ],
              ),
            ),
          ),

          const SliverToBoxAdapter(child: SizedBox(height: 12)),

          // ── Content State (Loading, Error, Empty, or List) ──
          if (_loading && _items.isEmpty)
            const SliverFillRemaining(
              hasScrollBody: false,
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircularProgressIndicator(color: _brick),
                    SizedBox(height: 16),
                    Text(
                      'جاري جلب أحدث ملخصات السينما من 25 قناة...',
                      style: TextStyle(
                        color: _muted,
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            )
          else if (_error != null && _items.isEmpty)
            SliverFillRemaining(
              hasScrollBody: false,
              child: Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.wifi_off_rounded,
                        size: 54,
                        color: _muted,
                      ),
                      const SizedBox(height: 12),
                      Text(
                        _error!,
                        textAlign: TextAlign.center,
                        style: const TextStyle(color: _ink, fontSize: 14),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton.icon(
                        onPressed: _refresh,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: _brick,
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                        ),
                        icon: const Icon(Icons.refresh_rounded),
                        label: const Text('إعادة المحاولة'),
                      ),
                    ],
                  ),
                ),
              ),
            )
          else if (list.isEmpty)
            const SliverFillRemaining(
              hasScrollBody: false,
              child: Center(
                child: Padding(
                  padding: EdgeInsets.all(24),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text('🍿', style: TextStyle(fontSize: 48)),
                      SizedBox(height: 12),
                      Text(
                        'لا توجد ملخصات مطابقة للبحث',
                        style: TextStyle(
                          color: _ink,
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      SizedBox(height: 6),
                      Text(
                        'جرّب تغيير كلمات البحث أو اختيار قناة أخرى',
                        style: TextStyle(color: _muted, fontSize: 13),
                      ),
                    ],
                  ),
                ),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(14, 4, 14, 12),
              sliver: SliverGrid.builder(
                itemCount: visibleItems.length,
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  crossAxisSpacing: 11,
                  mainAxisSpacing: 13,
                  childAspectRatio: .83,
                ),
                itemBuilder: (context, index) {
                  final item = visibleItems[index];
                  return _RecapVideoCard(
                    item: item,
                    onTap: () => _openRecap(item),
                    onPlay: () => _launchPlayer(context, item.videoUrl, item.title),
                  );
                },
              ),
            ),
          if (list.length > visibleItems.length)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(18, 5, 18, 28),
                child: Center(
                  child: FilledButton.icon(
                    onPressed: () => setState(() => _visibleCount += 45),
                    icon: const Icon(Icons.expand_more_rounded),
                    label: Text(
                      'عرض ${list.length - visibleItems.length < 45 ? list.length - visibleItems.length : 45} ملخص إضافي',
                    ),
                  ),
                ),
              ),
            )
          else if (_refreshing)
            const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.fromLTRB(18, 5, 18, 28),
                child: Center(
                  child: Text(
                    'بنكمّل تحميل الملخصات من بقية القنوات…',
                    style: TextStyle(color: _muted, fontSize: 12),
                  ),
                ),
              ),
            )
          else if (list.isNotEmpty)
            const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.fromLTRB(18, 5, 18, 28),
                child: Center(
                  child: Text(
                    'تم عرض كل الملخصات المتاحة من الخلاصات الحالية',
                    style: TextStyle(color: _muted, fontSize: 12),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _ChannelChip extends StatelessWidget {
  const _ChannelChip({
    required this.label,
    required this.selected,
    required this.onTap,
    this.count,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;
  final String? count;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 8),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
            decoration: BoxDecoration(
              color: selected ? _brick : _card,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: selected ? _brick : _ink.withOpacity(0.09),
                width: 1,
              ),
              boxShadow: selected
                  ? [
                      BoxShadow(
                        color: _brick.withOpacity(0.32),
                        blurRadius: 8,
                        offset: const Offset(0, 2),
                      ),
                    ]
                  : null,
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  label,
                  style: TextStyle(
                    color: selected ? Colors.white : _ink,
                    fontWeight: selected ? FontWeight.w800 : FontWeight.w600,
                    fontSize: 12,
                  ),
                ),
                if (count != null) ...[
                  const SizedBox(width: 5),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 5,
                      vertical: 1,
                    ),
                    decoration: BoxDecoration(
                      color: selected
                          ? Colors.white.withOpacity(0.24)
                          : _brick.withOpacity(0.12),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      count!,
                      style: TextStyle(
                        color: selected ? Colors.white : _brick,
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _RecapFeaturedCarousel extends StatelessWidget {
  const _RecapFeaturedCarousel({
    required this.items,
    required this.controller,
    required this.selectedIndex,
    required this.onPageChanged,
    required this.onOpen,
  });

  final List<MovieRecapItem> items;
  final PageController controller;
  final int selectedIndex;
  final ValueChanged<int> onPageChanged;
  final ValueChanged<MovieRecapItem> onOpen;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(top: 8, bottom: 7),
      child: Column(
        children: [
          SizedBox(
            height: 188,
            child: PageView.builder(
              controller: controller,
              itemCount: items.length,
              onPageChanged: onPageChanged,
              itemBuilder: (context, index) {
                final item = items[index];
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 5),
                  child: Material(
                    color: _ink,
                    borderRadius: BorderRadius.circular(23),
                    clipBehavior: Clip.antiAlias,
                    child: InkWell(
                      onTap: () => onOpen(item),
                      child: Stack(
                        fit: StackFit.expand,
                        children: [
                          CachedNetworkImage(
                            imageUrl: item.exactThumbnailUrl,
                            fit: BoxFit.cover,
                            errorWidget: (context, url, error) =>
                                CachedNetworkImage(
                                  imageUrl: item.fallbackThumbnailUrl,
                                  fit: BoxFit.cover,
                                  errorWidget: (context, url, error) =>
                                      const ColoredBox(
                                        color: _ink,
                                        child: Icon(
                                          Icons.movie_filter_rounded,
                                          color: _muted,
                                          size: 42,
                                        ),
                                      ),
                                ),
                          ),
                          const DecoratedBox(
                            decoration: BoxDecoration(
                              gradient: LinearGradient(
                                begin: Alignment.centerLeft,
                                end: Alignment.centerRight,
                                colors: [Color(0x00000000), Color(0xEA211923)],
                                stops: [0.12, 1],
                              ),
                            ),
                          ),
                          Positioned(
                            right: 18,
                            left: 18,
                            bottom: 16,
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 9,
                                    vertical: 5,
                                  ),
                                  decoration: BoxDecoration(
                                    color: _brick,
                                    borderRadius: BorderRadius.circular(20),
                                  ),
                                  child: const Text(
                                    'اختيار من الملخصات',
                                    style: TextStyle(
                                      color: Colors.white,
                                      fontSize: 11,
                                      fontWeight: FontWeight.w800,
                                    ),
                                  ),
                                ),
                                const SizedBox(height: 9),
                                Text(
                                  item.title,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  textAlign: TextAlign.right,
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontSize: 18,
                                    fontWeight: FontWeight.w900,
                                    height: 1.2,
                                  ),
                                ),
                                const SizedBox(height: 5),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.end,
                                  children: [
                                    Text(
                                      item.channelName,
                                      style: const TextStyle(
                                        color: Colors.white70,
                                        fontSize: 12,
                                      ),
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
              },
            ),
          ),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: List.generate(items.length, (index) {
              final selected = index == selectedIndex;
              return AnimatedContainer(
                duration: const Duration(milliseconds: 220),
                width: selected ? 22 : 6,
                height: 6,
                margin: const EdgeInsets.symmetric(horizontal: 3),
                decoration: BoxDecoration(
                  color: selected ? _brick : _ink.withOpacity(.18),
                  borderRadius: BorderRadius.circular(8),
                ),
              );
            }),
          ),
        ],
      ),
    );
  }
}

class _RecapDetailPage extends StatelessWidget {
  const _RecapDetailPage({required this.item});

  final MovieRecapItem item;

  Future<void> _play(BuildContext context) =>
      _launchPlayer(context, item.videoUrl, item.title);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _paper,
      appBar: AppBar(
        backgroundColor: _paper,
        foregroundColor: _ink,
        elevation: 0,
        title: const Text(
          'تفاصيل الملخص',
          style: TextStyle(fontWeight: FontWeight.w800),
        ),
        centerTitle: true,
      ),
      body: SafeArea(
        top: false,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(24),
              child: AspectRatio(
                aspectRatio: 16 / 9,
                child: CachedNetworkImage(
                  imageUrl: item.exactThumbnailUrl,
                  fit: BoxFit.cover,
                  errorWidget: (context, url, error) => CachedNetworkImage(
                    imageUrl: item.fallbackThumbnailUrl,
                    fit: BoxFit.cover,
                    errorWidget: (context, url, error) => const ColoredBox(
                      color: _ink,
                      child: Icon(
                        Icons.movie_filter_rounded,
                        color: _muted,
                        size: 48,
                      ),
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 20),
            Text(
              item.title,
              textAlign: TextAlign.right,
              style: const TextStyle(
                color: _ink,
                fontSize: 23,
                fontWeight: FontWeight.w900,
                height: 1.3,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              textDirection: TextDirection.rtl,
              children: [
                const Icon(
                  Icons.video_library_rounded,
                  color: _brick,
                  size: 19,
                ),
                const SizedBox(width: 7),
                Expanded(
                  child: Text(
                    item.channelName,
                    style: const TextStyle(
                      color: _muted,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 23),
            FilledButton(
              onPressed: () => _play(context),
              style: FilledButton.styleFrom(
                backgroundColor: _brick,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 15),
                minimumSize: const Size.fromHeight(50),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              child: const Text(
                'تشغيل',
                style: TextStyle(fontWeight: FontWeight.w800, fontSize: 15),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _RecapVideoCard extends StatelessWidget {
  const _RecapVideoCard({
    required this.item,
    required this.onTap,
    this.onPlay,
  });

  final MovieRecapItem item;
  final VoidCallback onTap;
  final VoidCallback? onPlay;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: _card,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: _ink.withOpacity(0.08), width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // ── Thumbnail with 16:9 Aspect Ratio ──
              AspectRatio(
                aspectRatio: 16 / 9,
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    CachedNetworkImage(
                      imageUrl: item.exactThumbnailUrl,
                      fit: BoxFit.cover,
                      placeholder: (context, url) => Container(
                        color: const Color(0xFF1E1720),
                        child: const Center(
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: _brick,
                          ),
                        ),
                      ),
                      errorWidget: (context, url, error) => CachedNetworkImage(
                        imageUrl: item.fallbackThumbnailUrl,
                        fit: BoxFit.cover,
                        errorWidget: (context, url2, error2) =>
                            CachedNetworkImage(
                              imageUrl: item.thumbnailUrl,
                              fit: BoxFit.cover,
                              errorWidget: (context, _, __) => Container(
                                color: const Color(0xFF1E1720),
                                child: const Icon(
                                  Icons.movie_filter_rounded,
                                  color: _muted,
                                  size: 42,
                                ),
                              ),
                            ),
                      ),
                    ),
                    // Cinematic gradient overlay
                    Positioned.fill(
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                            colors: [
                              Colors.black.withOpacity(0.35),
                              Colors.transparent,
                              Colors.black.withOpacity(0.85),
                            ],
                            stops: const [0.0, 0.45, 1.0],
                          ),
                        ),
                      ),
                    ),
                    // Channel pill on top start (right in RTL)
                    Positioned(
                      top: 10,
                      right: 10,
                      child: Container(
                        decoration: BoxDecoration(
                          color: Colors.black.withOpacity(0.72),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(
                            color: Colors.white.withOpacity(0.12),
                            width: 0.8,
                          ),
                        ),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Text('🎬', style: TextStyle(fontSize: 10)),
                            const SizedBox(width: 5),
                            Text(
                              item.channelName,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 11,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),

              // ── Card Content & Actions ──
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 8, 12, 10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      item.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: _ink,
                        fontSize: 13.5,
                        fontWeight: FontWeight.w800,
                        height: 1.3,
                      ),
                    ),
                    const SizedBox(height: 6),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton(
                        onPressed: onPlay ?? onTap,
                        style: FilledButton.styleFrom(
                          backgroundColor: _brick,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 6),
                          minimumSize: const Size.fromHeight(34),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                        ),
                        child: const Text(
                          'تشغيل',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ),
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
}
