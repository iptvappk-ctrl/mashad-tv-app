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
  int _visibleEpisodeCount = 60;
  Future<List<Map<String, dynamic>>>? _alternateServerFuture;
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
    final key = _serverCacheKey(url);
    final provider = _text(_episode['providerName']).ifEmpty(widget.provider);
    final alternatePages = (_episode['alternatePageUrls'] as List? ?? const [])
        .map(_text)
        .where((page) => page.trim() != url.trim())
        .take(24)
        .toList(growable: false);
    _alternateServerFuture = alternatePages.isEmpty
        ? null
        : _fetchAlternativeServerRows(
            pageUrls: alternatePages,
            provider: provider,
          );
    setState(() {
      _loading = true;
      _error = null;
    });
    if (url.isEmpty || !url.startsWith('http')) {
      if (mounted && request == _request) {
        setState(() {
          _loading = false;
          _error = 'رابط صفحة الحلقة غير متاح من المصدر.';
        });
      }
      return;
    }
    final cached = await NativeBridge.readCache(key);
    if (cached != null && mounted && request == _request) {
      try {
        final rows = _tagServerRows(_maps(jsonDecode(cached)), url, provider);
        if (rows.isNotEmpty)
          setState(() {
            _servers = rows;
            _loading = false;
          });
        if (rows.isNotEmpty) unawaited(_prefetchFirstServer(rows));
      } catch (_) {}
    }
    if (!mounted || request != _request) return;
    final seasonNum = _seasonNumberFrom(widget.seasonTitle) ?? 1;
    final backupFuture = _fetchProviderBackupServerRows(
      title: widget.seriesTitle,
      pageUrl: url,
      isSeries: true,
      episodeNumber: _number,
      seasonNumber: seasonNum,
    );
    var pendingLookups = 1 + (alternatePages.isNotEmpty ? 1 : 0) + 1;
    Future<void> absorb(Future<List<Map<String, dynamic>>> future) async {
      try {
        final rows = await future;
        if (!mounted || request != _request) return;
        if (rows.isNotEmpty) {
          setState(() {
            _servers = _mergeServerRows([..._servers, ...rows]);
            _loading = false;
            _error = null;
          });
          unawaited(_prefetchFirstServer(rows));
        }
      } catch (_) {
        // Keep other sources running when one provider is unavailable.
      } finally {
        if (mounted && request == _request) {
          setState(() {
            pendingLookups--;
            if (_servers.isEmpty && pendingLookups == 0) {
              _error = 'لم نجد سيرفرًا صالحًا في المصادر المتاحة لهذه الحلقة.';
            }
            _loading = _servers.isEmpty && pendingLookups > 0;
          });
        }
      }
    }

    unawaited(absorb(_fetchServerRowsForPage(url, provider)));
    if (alternatePages.isNotEmpty) {
      unawaited(absorb(_alternateServerFuture!));
    }
    unawaited(absorb(backupFuture));
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
    bottomNavigationBar: const SafeArea(
      child: AppBannerAdStrip(),
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
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 10),
              child: _ShimmerLoading(
                child: Column(
                  children: List.generate(
                    3,
                    (_) => Container(
                      height: 50,
                      margin: const EdgeInsets.only(bottom: 8),
                      decoration: BoxDecoration(
                        color: const Color(0xFFEADBCB),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: _line),
                      ),
                    ),
                  ),
                ),
              ),
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
          if (_servers.isNotEmpty) ...[
            _SmartEpisodePlay(
              servers: _servers,
              onOpen: _openServer,
              findWorking: _findWorkingEpisodeServer,
            ),
          ],
          const SizedBox(height: 16),
          _SectionCard(
            title: 'حلقات هذا الجزء / الموسم',
            icon: '📺',
            trailing: Text(
              '${_episodes.length} حلقة',
              style: const TextStyle(color: _muted, fontSize: 12),
            ),
            child: Column(
              children: [
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: List.generate(
                    _episodes.length < _visibleEpisodeCount
                        ? _episodes.length
                        : _visibleEpisodeCount,
                    (index) {
                      final number = _episodeNumber(_episodes[index]);
                      return ChoiceChip(
                        label: Text('الحلقة $number'),
                        selected: index == _index,
                        showCheckmark: false,
                        labelStyle: TextStyle(
                          color: index == _index ? Colors.white : _muted,
                        ),
                        side: BorderSide(
                          color: index == _index ? _brick : _line,
                        ),
                        onSelected: (_) {
                          setState(() {
                            _index = index;
                            _servers = [];
                          });
                          unawaited(_loadServers());
                        },
                      );
                    },
                  ),
                ),
                if (_episodes.length > _visibleEpisodeCount) ...[
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: () => setState(() {
                      _visibleEpisodeCount = (_visibleEpisodeCount + 60)
                          .clamp(0, _episodes.length)
                          .toInt();
                    }),
                    icon: const Icon(Icons.expand_more_rounded),
                    label: const Text('عرض المزيد من الحلقات'),
                  ),
                ],
              ],
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
    final server = await _findWorkingEpisodeServer(_servers);
    if (!mounted) return;
    if (server == null) {
      _message(context, 'لم يستجب أي رابط تنزيل صالح لهذه الحلقة.');
      return;
    }
    final sourcePage = _text(server['sourcePageUrl'])
        .ifEmpty(_text(_episode['pageUrl']));
    final url = _text(server['directStreamUrl']).trim();
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
            'referer': _text(server['embedUrl']).ifEmpty(sourcePage),
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
    final direct = _text(server['directStreamUrl']).trim();
    final embed = _text(server['embedUrl']).trim();
    final sourcePage = _text(server['sourcePageUrl'])
        .ifEmpty(_text(_episode['pageUrl']));

    final candidateUrl = _isUsablePlaybackUrl(direct) ? direct : embed;

    // إذا كان السيرفر محقَّقاً مسبقاً استخدم directStreamUrl مباشرة
    // وإلا حاول الـ resolve — لو فشل استخدم candidateUrl كـ embed في WebView
    final String url;
    if (server['playableVerified'] == true && _isUsablePlaybackUrl(direct)) {
      url = direct;
    } else {
      final resolvedViaStream = await _resolveServerStream({
        ...server,
        'directStreamUrl': '',
        'embedUrl': candidateUrl,
      }, referer: sourcePage).catchError((_) => '');
      url = _isUsablePlaybackUrl(resolvedViaStream) ? resolvedViaStream : '';
    }
    if (!mounted) return;
    final playableUrl = _isUsablePlaybackUrl(url) ? url : candidateUrl;
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
      final alternate = await _findWorkingEpisodeServer(alternatives);
      if (!mounted) return;
      if (alternate != null) {
        await _openServer(alternate);
        return;
      }
      _message(context, 'لم يتم التحقق من رابط فيديو صالح لهذه الحلقة.');
      return;
    }
    final playerReferer = embed.isNotEmpty ? embed : sourcePage;

    if (!mounted) return;
    final verifiedServer = {
      ...server,
      'directStreamUrl': playableUrl,
      'playableVerified': true,
    };
    await _launchPlayer(
      context,
      playableUrl,
      _fullTitle,
      referer: playerReferer,
      backupSources: _playerFallbackSources(
        servers: _mergeServerRows([
          ..._servers,
          ..._maps(server['fallbackServers']),
        ]),
        current: verifiedServer,
        pageReferer: sourcePage,
      ),
    );
  }

  Future<Map<String, dynamic>?> _findWorkingEpisodeServer(
    List<Map<String, dynamic>> servers, {
    bool retryAlternateSources = true,
  }) async {
    var candidates = _mergeServerRows(servers);
    final alternateFuture = _alternateServerFuture;
    if (candidates.isEmpty) return null;
    final verified = <Map<String, dynamic>>[];
    final firstPlayable = Completer<Map<String, dynamic>?>();
    var completed = 0;
    final probes = <Future<void>>[];
    final probeCandidates = prioritizeServerRowsForProbe(candidates)
        .take(24)
        .toList(growable: false);
    for (final server in probeCandidates) {
      probes.add(() async {
        try {
          final direct = _text(server['directStreamUrl']).trim();
          final embed = _text(server['embedUrl']).trim();
          final candidateUrl = _isUsablePlaybackUrl(direct) ? direct : embed;
          if (!_isUsablePlaybackUrl(candidateUrl)) return;
          final stream = await _native
              .invokeMethod<String>('media.resolve', {
                'url': candidateUrl,
                'referer': _text(server['sourcePageUrl'])
                    .ifEmpty(_text(_episode['pageUrl'])),
              })
              .timeout(const Duration(seconds: 8))
              .then((v) => _text(v).trim())
              .catchError((_) => '');
          // اقبل candidateUrl كـ embed إذا فشل الـ resolve
          final streamUrl = _isUsablePlaybackUrl(stream) ? stream : candidateUrl;
          final row = {
            ...server,
            'directStreamUrl': streamUrl,
            'playableVerified': _isUsablePlaybackUrl(stream),
          };
          verified.add(row);
          if (!firstPlayable.isCompleted) firstPlayable.complete(row);
        } catch (_) {
          // Continue probing the other servers in parallel.
        } finally {
          completed++;
          if (completed == probeCandidates.length &&
              !firstPlayable.isCompleted) {
            firstPlayable.complete(null);
          }
        }
      }());
    }
    final selected = await firstPlayable.future.timeout(
      const Duration(seconds: 9),
      onTimeout: () => null,
    );
    if (selected == null) {
      if (retryAlternateSources && alternateFuture != null) {
        try {
          final expanded = _mergeServerRows([
            ...candidates,
            ...await alternateFuture.timeout(const Duration(seconds: 6)),
          ]);
          if (expanded.length > candidates.length) {
            return await _findWorkingEpisodeServer(
              expanded,
              retryAlternateSources: false,
            );
          }
        } catch (_) {}
      }
      if (candidates.isNotEmpty) {
        final fallback = candidates.firstWhere(
          (s) =>
              _isUsablePlaybackUrl(_text(s['directStreamUrl']).trim()) ||
              _isUsablePlaybackUrl(_text(s['embedUrl']).trim()),
          orElse: () => candidates.first,
        );
        return {
          ...fallback,
          'playableVerified': false,
          'fallbackServers': candidates,
        };
      }
      return null;
    }
    await Future.any([
      Future.wait(probes),
      Future<void>.delayed(const Duration(milliseconds: 3500)),
    ]);
    return {
      ...selected,
      'fallbackServers': _mergeServerRows([...verified, ...candidates]),
    };
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
  final path = uri.path.toLowerCase();
  if (RegExp(r'\.(apng|avif|bmp|gif|jpe?g|png|svg|webp)$').hasMatch(path)) {
    return false;
  }
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

Future<String> _resolveServerStream(
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
    return '';
  }
  try {
    final resolved = _text(
      await _native
          .invokeMethod<String>('media.resolve', {
            'url': candidateUrl,
            'referer': referer,
          })
          .timeout(const Duration(seconds: 7)),
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

// ── Smart Play Button للحلقات (يختار أول سيرفر شغال) ────────────────────────
class _SmartEpisodePlay extends StatefulWidget {
  const _SmartEpisodePlay({
    required this.servers,
    required this.onOpen,
    required this.findWorking,
  });
  final List<Map<String, dynamic>> servers;
  final Future<void> Function(Map<String, dynamic>) onOpen;
  final Future<Map<String, dynamic>?> Function(List<Map<String, dynamic>>)
  findWorking;

  @override
  State<_SmartEpisodePlay> createState() => _SmartEpisodePlayState();
}

class _SmartEpisodePlayState extends State<_SmartEpisodePlay> {
  bool _resolving = false;

  Future<void> _play() async {
    if (_resolving || widget.servers.isEmpty) return;
    setState(() => _resolving = true);
    Map<String, dynamic>? selected;
    try {
      selected = await widget.findWorking(widget.servers);
    } catch (_) {}
    if (!mounted) return;
    setState(() => _resolving = false);
    if (selected != null) {
      await widget.onOpen(selected);
    } else if (widget.servers.isNotEmpty) {
      await widget.onOpen(widget.servers.first);
    } else {
      _message(context, 'لم يستجب أي رابط فيديو صالح لهذه الحلقة.');
    }
  }

  @override
  Widget build(BuildContext context) => Material(
    color: _brick,
    borderRadius: BorderRadius.circular(16),
    child: InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: _resolving ? null : _play,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (_resolving)
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
            Expanded(
              child: Text(
                _resolving ? 'جاري فحص رابط المشاهدة…' : 'شاهد الآن',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 17,
                  fontWeight: FontWeight.w900,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

// ── قائمة السيرفرات البديلة (مطوية) للحلقات ─────────────────────────────────
class _EpisodeAltServers extends StatefulWidget {
  const _EpisodeAltServers({required this.servers, required this.onTap});
  final List<Map<String, dynamic>> servers;
  final void Function(Map<String, dynamic>) onTap;

  @override
  State<_EpisodeAltServers> createState() => _EpisodeAltServersState();
}

class _EpisodeAltServersState extends State<_EpisodeAltServers> {
  bool _expanded = false;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      GestureDetector(
        onTap: () => setState(() => _expanded = !_expanded),
        child: Row(
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
