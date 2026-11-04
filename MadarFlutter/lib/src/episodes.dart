part of '../main.dart';

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
        if (rows.isNotEmpty) unawaited(_prefetchFirstServer(rows));
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
      if (rows.isNotEmpty) unawaited(_prefetchFirstServer(rows));
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

Future<void> _prefetchFirstServer(List<Map<String, dynamic>> servers) async {
  final candidate = servers.firstWhere(
    (server) =>
        _text(server['directStreamUrl']).trim().isNotEmpty ||
        _text(server['embedUrl']).trim().isNotEmpty,
    orElse: () => const <String, dynamic>{},
  );
  if (candidate.isEmpty ||
      _text(candidate['directStreamUrl']).trim().isNotEmpty) {
    return;
  }
  // Warm the native resolver after displaying server choices so a tap can use
  // its in-memory result instead of waiting for the first resolver request.
  await _resolveServerStream(candidate);
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
