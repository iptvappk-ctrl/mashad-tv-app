part of '../main.dart';

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
  bool _todayOnly = true;

  // ─── beIN SPORTS Match Highlights State ─────────────────────────────────────
  List<MatchHighlight> _highlights = [];
  List<MatchHighlight> _allHighlights = [];
  bool _loadingHighlights = false;
  String _highlightTournamentFilter = 'الكل';
  final TextEditingController _highlightSearchCtrl = TextEditingController();
  Timer? _highlightSearchDebounce;

  @override
  void initState() {
    super.initState();
    unawaited(_loadInitial());
    unawaited(_initHighlights());
  }

  @override
  void dispose() {
    _highlightSearchCtrl.dispose();
    _highlightSearchDebounce?.cancel();
    super.dispose();
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
              ? 'bein_sports'
              : _text(_categories.first['id']);
        });
      unawaited(_loadChannels(_categoryId ?? 'bein_sports'));
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
        height: 48,
        child: ListView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 3),
          children: [
            _sectionChip('matches', 'مباريات مباشرة ⚽'),
            const SizedBox(width: 8),
            _sectionChip('highlights', 'ملخصات beIN 🎬'),
            const SizedBox(width: 8),
            _sectionChip('channels', 'قنوات بي إن 📺'),
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
          onRefresh: () async {
            if (_section == 'highlights') {
              await _fetchBeInHighlights(query: _highlightSearchCtrl.text);
            } else {
              await _refresh();
            }
          },
          child: _section == 'matches'
              ? _eventsList()
              : _section == 'highlights'
                  ? _highlightsList()
                  : _channelsList(),
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
    onSelected: (_) => setState(() {
      _section = value;
      if (value == 'channels' && _channels.isEmpty) {
        _loadChannels(_categoryId ?? 'bein_sports');
      }
    }),
  );

  bool _isMatchToday(Map<String, dynamic> match) {
    final state = _text(match['state']);
    if (state == 'in') return true;

    final kickoff = _integer(match['kickoffEpochMs']);
    final now = DateTime.now();
    if (kickoff > 0) {
      final dt = DateTime.fromMillisecondsSinceEpoch(kickoff).toLocal();
      return dt.year == now.year && dt.month == now.month && dt.day == now.day;
    }

    final dateStr = _text(match['matchDate']);
    if (dateStr.isNotEmpty) {
      final parsed = DateTime.tryParse(dateStr)?.toLocal();
      if (parsed != null) {
        return parsed.year == now.year && parsed.month == now.month && parsed.day == now.day;
      }
    }

    final timeOrStatus = _text(match['timeOrStatus']);
    return timeOrStatus.contains('اليوم') || timeOrStatus.contains('تبدأ');
  }

  List<Map<String, dynamic>> _todayMatches() =>
      _footballMatches.where(_isMatchToday).toList();

  String _channelForLeague(String leagueName) {
    final lower = leagueName.toLowerCase();
    if (lower.contains('إنجليزي') || lower.contains('premier')) return 'beIN SPORTS 1 HD';
    if (lower.contains('إسباني') || lower.contains('la liga')) return 'beIN SPORTS 3 HD';
    if (lower.contains('فرنسي') || lower.contains('ligue 1')) return 'beIN SPORTS 4 HD';
    if (lower.contains('ألماني') || lower.contains('bundesliga')) return 'beIN SPORTS 5 HD';
    if (lower.contains('أبطال') || lower.contains('champions')) return 'beIN SPORTS 1 HD';
    if (lower.contains('إيطالي') || lower.contains('serie a')) return 'أبوظبي الرياضية بريميوم';
    return 'beIN SPORTS HD';
  }

  Widget _matchStatusWidget(Map<String, dynamic> match) {
    final state = _text(match['state']);
    final rawTime = _text(match['timeOrStatus']);

    if (state == 'in') {
      final detail = rawTime
          .replaceAll('مباشر', '')
          .replaceAll('🔴', '')
          .replaceAll('جارية', '')
          .replaceAll('الآن', '')
          .trim();
      final label = detail.isNotEmpty
          ? 'جارية الآن ($detail)'
          : 'جارية الآن 🔴';
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
        decoration: BoxDecoration(
          color: const Color(0xFFE5F4EC),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 7,
              height: 7,
              decoration: const BoxDecoration(
                color: Color(0xFF16856D),
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 5),
            Text(
              label,
              style: const TextStyle(
                color: Color(0xFF16856D),
                fontSize: 11,
                fontWeight: FontWeight.w800,
              ),
            ),
          ],
        ),
      );
    } else if (state == 'post') {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
        decoration: BoxDecoration(
          color: const Color(0xFFF3F4F6),
          borderRadius: BorderRadius.circular(20),
        ),
        child: const Text(
          'انتهت المباراة',
          style: TextStyle(
            color: _muted,
            fontSize: 11,
            fontWeight: FontWeight.w700,
          ),
        ),
      );
    } else {
      String kickoffLabel;
      final kickoff = _integer(match['kickoffEpochMs']);
      if (kickoff > 0) {
        final dt = DateTime.fromMillisecondsSinceEpoch(kickoff).toLocal();
        final hour = dt.hour > 12 ? dt.hour - 12 : (dt.hour == 0 ? 12 : dt.hour);
        final period = dt.hour >= 12 ? 'م' : 'ص';
        final minute = dt.minute.toString().padLeft(2, '0');
        kickoffLabel = 'تبدأ الساعة $hour:$minute $period';
      } else {
        final timeMatch = RegExp(r'(\d{1,2}:\d{2}\s*[صم])').firstMatch(rawTime);
        if (timeMatch != null) {
          kickoffLabel = 'تبدأ الساعة ${timeMatch.group(1)}';
        } else {
          kickoffLabel = rawTime.isNotEmpty ? rawTime : 'لم تبدأ بعد';
        }
      }

      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
        decoration: BoxDecoration(
          color: const Color(0xFFF8F9FA),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: _line),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.schedule_rounded, size: 12, color: _muted),
            const SizedBox(width: 4),
            Text(
              kickoffLabel,
              style: const TextStyle(
                color: _ink,
                fontSize: 11,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
      );
    }
  }

  Future<void> _chooseStreamsForFixture(Map<String, dynamic> match) async {
    final home = _text(match['homeTeamArabic'])
        .ifEmpty(_text(match['homeTeam']).ifEmpty('الفريق الأول'));
    final away = _text(match['awayTeamArabic'])
        .ifEmpty(_text(match['awayTeam']).ifEmpty('الفريق الثاني'));
    final title = '$home × $away';
    final league = _text(match['leagueName']);
    final channel = _channelForLeague(league);
    final id = _text(match['id']).ifEmpty('fixture_${home}_$away');

    // البحث عن مباراة مطابقة في أحداث الياسين إن وجدت
    final matched = _events.firstWhere(
      (e) {
        final t1 = _text((e['team1'] as Map?)?['name']);
        final t2 = _text((e['team2'] as Map?)?['name']);
        return (t1.isNotEmpty && (t1.contains(home) || home.contains(t1))) ||
               (t2.isNotEmpty && (t2.contains(away) || away.contains(t2)));
      },
      orElse: () => const <String, dynamic>{},
    );

    await _chooseStreams(
      matched.isNotEmpty ? _text(matched['id']) : id,
      title,
      matched.isNotEmpty,
      channelOverride: matched.isNotEmpty ? _text(matched['channel']) : channel,
      leagueOverride: league,
    );
  }

  Widget _eventsList() {
    if (_events.isEmpty && _footballMatches.isEmpty && _loading) {
      return const _SportsMatchSkeleton();
    }

    final todayMatches = _todayMatches();
    final matchesList = _todayOnly ? todayMatches : _footballMatches;

    // في حال وجود أحداث مباشرة من سيرفر الأحداث
    if (_events.isNotEmpty) {
      return ListView.builder(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(14),
        itemCount: _events.length + 1,
        itemBuilder: (_, index) {
          if (index == 0) return _highlightsPromoCard();
          return _eventCard(_events[index - 1]);
        },
      );
    }

    // جدول المباريات مع فلتر مباريات اليوم
    if (_footballMatches.isNotEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(14, 8, 14, 18),
        children: [
          _highlightsPromoCard(),
          const SizedBox(height: 8),
          Row(
            children: [
              ChoiceChip(
                label: Text('مباريات اليوم (${todayMatches.length})'),
                selected: _todayOnly,
                showCheckmark: false,
                labelStyle: TextStyle(
                  color: _todayOnly ? Colors.white : _ink,
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                ),
                side: BorderSide(color: _todayOnly ? _brick : _line),
                selectedColor: _brick,
                onSelected: (_) => setState(() => _todayOnly = true),
              ),
              const SizedBox(width: 8),
              ChoiceChip(
                label: Text('جميع المباريات (${_footballMatches.length})'),
                selected: !_todayOnly,
                showCheckmark: false,
                labelStyle: TextStyle(
                  color: !_todayOnly ? Colors.white : _ink,
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                ),
                side: BorderSide(color: !_todayOnly ? _brick : _line),
                selectedColor: _brick,
                onSelected: (_) => setState(() => _todayOnly = false),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (matchesList.isEmpty && _todayOnly)
            Card(
              margin: const EdgeInsets.only(top: 20),
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.sports_soccer_rounded, size: 48, color: _muted),
                    const SizedBox(height: 12),
                    const Text(
                      'لا توجد مباريات أخرى مجدولة لليوم في الدوريات الكبرى.',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: _muted, fontSize: 13, height: 1.5),
                    ),
                    const SizedBox(height: 14),
                    OutlinedButton.icon(
                      onPressed: () => setState(() => _todayOnly = false),
                      icon: const Icon(Icons.calendar_month_rounded, color: _brick, size: 18),
                      label: const Text('عرض جميع مباريات الجولة', style: TextStyle(color: _brick)),
                      style: OutlinedButton.styleFrom(
                        side: const BorderSide(color: _brick),
                      ),
                    ),
                  ],
                ),
              ),
            )
          else
            for (final match in matchesList) _fixtureCard(match),
        ],
      );
    }

    return _empty(
      _error == null
          ? 'تعذر جلب مباريات أو روابط بث من المصادر الآن. اسحب للتحديث.'
          : 'تعذر تحديث المباريات. اسحب للمحاولة مرة أخرى.',
    );
  }

  Widget _fixtureCard(Map<String, dynamic> match) {
    final home = _text(match['homeTeamArabic'])
        .ifEmpty(_text(match['homeTeam']).ifEmpty('الفريق الأول'));
    final away = _text(match['awayTeamArabic'])
        .ifEmpty(_text(match['awayTeam']).ifEmpty('الفريق الثاني'));
    final homeScore = _text(match['homeScore']);
    final awayScore = _text(match['awayScore']);
    final state = _text(match['state']);
    final hasScore = (state == 'in' || state == 'post') &&
        homeScore.isNotEmpty &&
        awayScore.isNotEmpty;
    final league = _text(match['leagueName']).ifEmpty('مباراة');
    final channel = _channelForLeague(league);
    final id = _text(match['id']);

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: () => _chooseStreamsForFixture(match),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      league,
                      style: const TextStyle(color: _muted, fontSize: 12),
                    ),
                  ),
                  _matchStatusWidget(match),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                    decoration: BoxDecoration(
                      color: _brick.withOpacity(0.08),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      channel,
                      style: const TextStyle(
                        color: _brick,
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(
                    child: _team({'name': home, 'logo': match['homeLogo']}),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 14),
                    child: Text(
                      hasScore ? '$homeScore  -  $awayScore' : 'VS',
                      style: TextStyle(
                        color: state == 'in'
                            ? const Color(0xFF16856D)
                            : _brick,
                        fontWeight: FontWeight.w900,
                        fontSize: 18,
                      ),
                    ),
                  ),
                  Expanded(
                    child: _team({'name': away, 'logo': match['awayLogo']}),
                  ),
                ],
              ),
              const SizedBox(height: 10),
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
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.play_circle_fill_rounded, size: 14, color: _brick),
                    const SizedBox(width: 5),
                    Text(
                      state == 'in' ? 'شاهد البث المباشر الآن' : 'اضغط لاختيار قناة البث',
                      style: const TextStyle(
                        color: _brick,
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
            ],
          ),
        ),
      ),
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

  Widget _eventScheduledBadge(int startEpochSeconds) {
    if (startEpochSeconds <= 0) return const SizedBox.shrink();
    final dt = DateTime.fromMillisecondsSinceEpoch(startEpochSeconds * 1000).toLocal();
    final hour = dt.hour > 12 ? dt.hour - 12 : (dt.hour == 0 ? 12 : dt.hour);
    final period = dt.hour >= 12 ? 'م' : 'ص';
    final minute = dt.minute.toString().padLeft(2, '0');
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFFF8F9FA),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _line),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.schedule_rounded, size: 12, color: _muted),
          const SizedBox(width: 4),
          Text(
            'تبدأ الساعة $hour:$minute $period',
            style: const TextStyle(
              color: _ink,
              fontSize: 11,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
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
                    const _FinishedBadge()
                  else
                    _eventScheduledBadge(start),
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

  Future<void> _chooseStreams(
    String id,
    String title,
    bool event, {
    String? channelOverride,
    String? leagueOverride,
  }) async {
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
      final channelName = channelOverride?.isNotEmpty == true
          ? channelOverride!
          : (event ? _text(match['channel']) : '');
      final leagueName = leagueOverride?.isNotEmpty == true
          ? leagueOverride!
          : (event ? _text(match['champions']) : '');

      final streams = await NativeBridge.list(
        'live.matchStreams',
        {
          'id': id,
          'channel': channelName,
          'league': leagueName,
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
              Row(
                children: [
                  Expanded(
                    child: Text(
                      title,
                      style: const TextStyle(
                        color: _ink,
                        fontWeight: FontWeight.w900,
                        fontSize: 17,
                      ),
                    ),
                  ),
                  if (channelName.isNotEmpty)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: _brick.withOpacity(0.08),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        channelName,
                        style: const TextStyle(
                          color: _brick,
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 6),
              const Text(
                'اختر سيرفر أو قناة المشاهدة المباشرة',
                style: TextStyle(color: _muted, fontSize: 13),
              ),
              const SizedBox(height: 12),
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

  // ─── ⚽ beIN SPORTS MATCH HIGHLIGHTS METHODS ─────────────────────────────
Widget _highlightsPromoCard() {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF580F8B), Color(0xFF38085C)],
          begin: Alignment.topRight,
          end: Alignment.bottomLeft,
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF580F8B).withValues(alpha: 0.25),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () => setState(() => _section = 'highlights'),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.white24),
                  ),
                  child: const Center(
                    child: Icon(Icons.play_circle_fill_rounded, color: Colors.white, size: 26),
                  ),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Row(
                        children: [
                          Text(
                            'ملخصات مباريات beIN SPORTS',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 14,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          SizedBox(width: 5),
                          Icon(Icons.verified_rounded, size: 14, color: Color(0xFFFFD700)),
                        ],
                      ),
                      SizedBox(height: 2),
                      Text(
                        'ملخص إسبانيا وإنجلترا، دوري الأمم، وأبطال أوروبا HD',
                        style: TextStyle(color: Colors.white70, fontSize: 11.5),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFD700),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Text(
                    'مشاهدة 🎬',
                    style: TextStyle(
                      color: Color(0xFF38085C),
                      fontSize: 11.5,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _initHighlights() async {
    _allHighlights = List.from(_kStarterHighlights);
    _highlights = List.from(_allHighlights);
    if (mounted) setState(() {});

    try {
      final cachedJson = await NativeBridge.readCache('bein:highlights:v1');
      if (cachedJson != null && cachedJson.isNotEmpty) {
        final decoded = jsonDecode(cachedJson) as List<dynamic>;
        final cachedItems = decoded
            .map((e) => MatchHighlight.fromMap(e as Map<String, dynamic>))
            .where((e) => e.id.isNotEmpty && e.title.isNotEmpty)
            .toList();
        if (cachedItems.isNotEmpty) {
          _allHighlights = cachedItems;
          _highlights = _filterHighlights();
          if (mounted) setState(() {});
        }
      }
    } catch (_) {}

    await _fetchBeInHighlights();
  }

  Future<void> _fetchBeInHighlights({String query = ''}) async {
    if (_loadingHighlights) return;
    if (mounted) setState(() => _loadingHighlights = true);
    try {
      final fresh = await MatchHighlightsService.fetchHighlights(query: query);
      if (fresh.isNotEmpty) {
        if (query.trim().isEmpty) {
          final Map<String, MatchHighlight> unique = {};
          for (final h in _kStarterHighlights) {
            unique[h.id] = h;
          }
          for (final h in fresh) {
            unique[h.id] = h;
          }
          _allHighlights = unique.values.toList();
          _highlights = _filterHighlights();
          unawaited(NativeBridge.writeCache(
            'bein:highlights:v1',
            jsonEncode(_allHighlights.map((e) => e.toMap()).toList()),
          ));
        } else {
          _highlights = fresh;
        }
      }
    } catch (_) {
    } finally {
      if (mounted) setState(() => _loadingHighlights = false);
    }
  }

  void _onHighlightSearchChanged(String text) {
    _highlightSearchDebounce?.cancel();
    _highlightSearchDebounce = Timer(const Duration(milliseconds: 400), () {
      if (!mounted) return;
      final q = text.trim();
      if (q.isEmpty) {
        setState(() {
          _highlights = _filterHighlights();
        });
      } else {
        final localFiltered = _allHighlights.where((h) {
          return h.title.contains(q) || h.tournament.contains(q);
        }).toList();

        setState(() {
          if (localFiltered.isNotEmpty) {
            _highlights = localFiltered;
          }
        });

        unawaited(_fetchBeInHighlights(query: q));
      }
    });
  }

  void _performBeInSearch(String q) {
    _highlightSearchDebounce?.cancel();
    if (q.trim().isEmpty) {
      setState(() => _highlights = _filterHighlights());
    } else {
      unawaited(_fetchBeInHighlights(query: q.trim()));
    }
  }

  List<MatchHighlight> _filterHighlights() {
    final query = _highlightSearchCtrl.text.trim();
    return _allHighlights.where((h) {
      final matchesTournament = _highlightTournamentFilter == 'الكل' ||
          h.tournament.contains(_highlightTournamentFilter) ||
          h.title.contains(_highlightTournamentFilter);
      final matchesQuery = query.isEmpty ||
          h.title.contains(query) ||
          h.tournament.contains(query);
      return matchesTournament && matchesQuery;
    }).toList();
  }

  Future<void> _launchHighlightPlayer(
    BuildContext context,
    MatchHighlight item,
  ) async {
    AppTelemetry.trackPlayback(
      title: item.title,
      category: 'ملخصات beIN SPORTS ⚽',
      event: 'start',
    );

    final opened = await NativeBridge.openPlayer(
      url: item.embedUrl,
      title: item.title,
      referer: 'https://www.youtube.com/',
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      backupSources: [
        {'name': 'YouTube Official', 'url': item.videoUrl},
      ],
      isLive: false,
    );

    if (!opened) {
      final ytOpened = await NativeBridge.openYouTube(
        videoId: item.id,
        watchUrl: item.videoUrl,
      );
      if (!ytOpened && context.mounted) {
        _message(context, 'تعذر تشغيل الملخص، تحقق من اتصالك بالإنترنت.');
      }
    }
  }

  Widget _tournamentChip(String name) {
    final selected = _highlightTournamentFilter == name;
    return ChoiceChip(
      label: Text(name),
      selected: selected,
      showCheckmark: false,
      labelStyle: TextStyle(
        color: selected ? Colors.white : _ink,
        fontSize: 11.5,
        fontWeight: selected ? FontWeight.w800 : FontWeight.w500,
      ),
      side: BorderSide(color: selected ? const Color(0xFF580F8B) : _line),
      selectedColor: const Color(0xFF580F8B),
      backgroundColor: Colors.white,
      onSelected: (_) {
        setState(() {
          _highlightTournamentFilter = name;
          _highlights = _filterHighlights();
        });
      },
    );
  }

  Widget _highlightsList() {
    final filtered = _filterHighlights();
    return Column(
      children: [
        // Search bar
        Padding(
          padding: const EdgeInsets.fromLTRB(14, 10, 14, 6),
          child: TextField(
            controller: _highlightSearchCtrl,
            textDirection: TextDirection.rtl,
            decoration: InputDecoration(
              hintText: 'ابحث عن ملخص مباراة (إسبانيا، إنجلترا، السيتي)...',
              hintStyle: const TextStyle(color: _muted, fontSize: 13),
              prefixIcon: const Icon(Icons.search_rounded, color: Color(0xFF580F8B)),
              suffixIcon: _highlightSearchCtrl.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.close_rounded, size: 18),
                      onPressed: () {
                        _highlightSearchCtrl.clear();
                        _onHighlightSearchChanged('');
                      },
                    )
                  : null,
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              filled: true,
              fillColor: Colors.white,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: _line),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: _line),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: Color(0xFF580F8B), width: 1.5),
              ),
            ),
            onChanged: _onHighlightSearchChanged,
            onSubmitted: _performBeInSearch,
          ),
        ),
        // Tournament Chips
        SizedBox(
          height: 44,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
            children: [
              _tournamentChip('الكل'),
              const SizedBox(width: 6),
              _tournamentChip('دوري الأمم الأوروبية'),
              const SizedBox(width: 6),
              _tournamentChip('دوري أبطال أوروبا'),
              const SizedBox(width: 6),
              _tournamentChip('الدوري الإنجليزي'),
              const SizedBox(width: 6),
              _tournamentChip('الدوري الإسباني'),
              const SizedBox(width: 6),
              _tournamentChip('كأس أمم إفريقيا'),
            ],
          ),
        ),
        if (_loadingHighlights)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 4),
            child: LinearProgressIndicator(
              color: Color(0xFF580F8B),
              backgroundColor: Color(0xFFF3E8FF),
              minHeight: 2.5,
            ),
          ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          child: Row(
            children: [
              const Icon(Icons.video_library_rounded, size: 14, color: Color(0xFF580F8B)),
              const SizedBox(width: 6),
              Text(
                'ملخصات beIN SPORTS المتاحة (${filtered.length})',
                style: const TextStyle(
                  color: Color(0xFF580F8B),
                  fontSize: 12,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const Spacer(),
              if (_highlightSearchCtrl.text.isNotEmpty)
                GestureDetector(
                  onTap: () {
                    _highlightSearchCtrl.clear();
                    _onHighlightSearchChanged('');
                  },
                  child: const Text(
                    'مسح البحث ✕',
                    style: TextStyle(color: _brick, fontSize: 11, fontWeight: FontWeight.w700),
                  ),
                ),
            ],
          ),
        ),
        Expanded(
          child: filtered.isEmpty
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.sports_soccer_rounded, size: 48, color: _muted),
                        const SizedBox(height: 12),
                        Text(
                          _highlightSearchCtrl.text.isNotEmpty
                              ? 'لا توجد نتائج لـ "${_highlightSearchCtrl.text}" في beIN SPORTS'
                              : 'لا توجد ملخصات متاحة حالياً',
                          textAlign: TextAlign.center,
                          style: const TextStyle(color: _muted, fontSize: 14),
                        ),
                        const SizedBox(height: 12),
                        OutlinedButton.icon(
                          onPressed: () {
                            _highlightSearchCtrl.clear();
                            _highlightTournamentFilter = 'الكل';
                            _onHighlightSearchChanged('');
                          },
                          icon: const Icon(Icons.refresh_rounded),
                          label: const Text('عرض جميع الملخصات'),
                        ),
                      ],
                    ),
                  ),
                )
              : ListView.builder(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.fromLTRB(14, 4, 14, 24),
                  itemCount: filtered.length,
                  itemBuilder: (_, index) => _highlightCard(filtered[index]),
                ),
        ),
      ],
    );
  }

  Widget _highlightCard(MatchHighlight item) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: _line.withValues(alpha: 0.8)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => _launchHighlightPlayer(context, item),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            AspectRatio(
              aspectRatio: 16 / 9,
              child: Stack(
                fit: StackFit.expand,
                children: [
                  CachedNetworkImage(
                    imageUrl: item.thumbnail,
                    fit: BoxFit.cover,
                    placeholder: (_, __) => Container(
                      color: const Color(0xFF1E1E2C),
                      child: const Center(
                        child: CircularProgressIndicator(color: Color(0xFF580F8B), strokeWidth: 2),
                      ),
                    ),
                    errorWidget: (_, __, ___) => Container(
                      color: const Color(0xFF1E1E2C),
                      child: const Center(
                        child: Icon(Icons.sports_soccer_rounded, color: Colors.white54, size: 40),
                      ),
                    ),
                  ),
                  const DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          Colors.black38,
                          Colors.transparent,
                          Colors.black87,
                        ],
                        stops: [0.0, 0.5, 1.0],
                      ),
                    ),
                  ),
                  Positioned(
                    top: 10,
                    right: 10,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: const Color(0xFF580F8B),
                        borderRadius: BorderRadius.circular(8),
                        boxShadow: const [
                          BoxShadow(color: Colors.black38, blurRadius: 4),
                        ],
                      ),
                      child: const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.verified_rounded, size: 13, color: Color(0xFFFFD700)),
                          SizedBox(width: 4),
                          Text(
                            'beIN SPORTS',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 11,
                              fontWeight: FontWeight.w900,
                              letterSpacing: 0.5,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  Center(
                    child: Container(
                      width: 52,
                      height: 52,
                      decoration: BoxDecoration(
                        color: const Color(0xFF580F8B).withValues(alpha: 0.9),
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white, width: 2),
                        boxShadow: const [
                          BoxShadow(color: Colors.black45, blurRadius: 10),
                        ],
                      ),
                      child: const Icon(
                        Icons.play_arrow_rounded,
                        color: Colors.white,
                        size: 34,
                      ),
                    ),
                  ),
                  if (item.duration.isNotEmpty)
                    Positioned(
                      bottom: 10,
                      left: 10,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: Colors.black87,
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.timer_outlined, size: 11, color: Colors.white70),
                            const SizedBox(width: 3),
                            Text(
                              item.duration,
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
                  if (item.tournament.isNotEmpty)
                    Positioned(
                      bottom: 10,
                      right: 10,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: const Color(0xFF16856D),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          item.tournament,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 10.5,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    item.title,
                    style: const TextStyle(
                      color: _ink,
                      fontSize: 15,
                      fontWeight: FontWeight.w800,
                      height: 1.35,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      if (item.timeAgo.isNotEmpty) ...[
                        const Icon(Icons.schedule_rounded, size: 13, color: _muted),
                        const SizedBox(width: 4),
                        Text(item.timeAgo, style: const TextStyle(color: _muted, fontSize: 11.5)),
                        const SizedBox(width: 12),
                      ],
                      if (item.views.isNotEmpty) ...[
                        const Icon(Icons.visibility_outlined, size: 13, color: _muted),
                        const SizedBox(width: 4),
                        Text(item.views, style: const TextStyle(color: _muted, fontSize: 11.5)),
                      ],
                      const Spacer(),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF3E8FF),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: const Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.hd_rounded, size: 14, color: Color(0xFF580F8B)),
                            SizedBox(width: 3),
                            Text(
                              'Full HD',
                              style: TextStyle(
                                color: Color(0xFF580F8B),
                                fontSize: 10.5,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      style: FilledButton.styleFrom(
                        backgroundColor: const Color(0xFF580F8B),
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        padding: const EdgeInsets.symmetric(vertical: 11),
                      ),
                      onPressed: () => _launchHighlightPlayer(context, item),
                      icon: const Icon(Icons.play_circle_fill_rounded, size: 18),
                      label: const Text(
                        'مشاهدة الملخص الآن',
                        style: TextStyle(fontSize: 13, fontWeight: FontWeight.w800),
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
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 7,
          height: 7,
          decoration: const BoxDecoration(
            color: Color(0xFF16856D),
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 5),
        const Text(
          'جارية الآن 🔴',
          style: TextStyle(
            color: Color(0xFF14845D),
            fontWeight: FontWeight.bold,
            fontSize: 11,
          ),
        ),
      ],
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
  final cleanUrl = url.trim();
  if (cleanUrl.isEmpty) {
    _message(context, 'رابط التشغيل غير متاح. اختر سيرفرًا آخر.');
    return;
  }
  final uri = Uri.tryParse(cleanUrl);
  // قبل الرابط لو كان http/https صالح — حتى لو embed page (WebView سيتعامل معها)
  if (uri == null ||
      uri.host.isEmpty ||
      (uri.scheme != 'http' && uri.scheme != 'https')) {
    _message(context, 'رابط التشغيل غير صالح.');
    return;
  }
  final category = isLive
      ? 'بث مباشر'
      : (title.contains('الحلقة') || title.contains('الموسم'))
          ? 'مسلسلات'
          : (title.contains('ملخص'))
              ? 'ملخصات'
              : 'أفلام';

  AppTelemetry.trackPlayback(
    title: title,
    category: category,
    event: 'start',
  );

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
  AppTelemetry.trackPlayback(
    title: title,
    category: category,
    event: 'error',
    errorMessage: 'تعذر فتح مشغل الفيديو',
  );
  await NativeBridge.promptPlayerUpdate(context);
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

// ─── ⚽ MODEL & HIGHLIGHTS SERVICE ──────────────────────────────────────────

class MatchHighlight {
  const MatchHighlight({
    required this.id,
    required this.title,
    required this.channel,
    required this.duration,
    required this.timeAgo,
    required this.views,
    required this.thumbnail,
    required this.videoUrl,
    required this.embedUrl,
    this.tournament = '',
  });

  final String id;
  final String title;
  final String channel;
  final String duration;
  final String timeAgo;
  final String views;
  final String thumbnail;
  final String videoUrl;
  final String embedUrl;
  final String tournament;

  Map<String, dynamic> toMap() => {
    'id': id,
    'title': title,
    'channel': channel,
    'duration': duration,
    'timeAgo': timeAgo,
    'views': views,
    'thumbnail': thumbnail,
    'videoUrl': videoUrl,
    'embedUrl': embedUrl,
    'tournament': tournament,
  };

  factory MatchHighlight.fromMap(Map<String, dynamic> map) => MatchHighlight(
    id: map['id']?.toString() ?? '',
    title: map['title']?.toString() ?? '',
    channel: map['channel']?.toString() ?? 'beIN SPORTS',
    duration: map['duration']?.toString() ?? '',
    timeAgo: map['timeAgo']?.toString() ?? '',
    views: map['views']?.toString() ?? '',
    thumbnail: map['thumbnail']?.toString() ?? '',
    videoUrl: map['videoUrl']?.toString() ?? '',
    embedUrl: map['embedUrl']?.toString() ?? '',
    tournament: map['tournament']?.toString() ?? '',
  );
}

const List<MatchHighlight> _kStarterHighlights = [
  MatchHighlight(
    id: 'd5B3UyUeOGM',
    title: 'ملخص مباراة إسبانيا وإنجلترا | دوري الأمم الأوروبية - الجولة السادسة',
    channel: 'beIN SPORTS',
    duration: '11:57',
    timeAgo: 'قبل 4 ساعات',
    views: '821 ألف مشاهدة',
    thumbnail: 'https://i.ytimg.com/vi/d5B3UyUeOGM/hqdefault.jpg',
    videoUrl: 'https://www.youtube.com/watch?v=d5B3UyUeOGM',
    embedUrl: 'https://www.youtube.com/embed/d5B3UyUeOGM?autoplay=1&controls=1&playsinline=1',
    tournament: 'دوري الأمم الأوروبية',
  ),
  MatchHighlight(
    id: 'h5eCgxrtmUk',
    title: 'ملخص مباراة فرنسا وتركيا | دوري الأمم الأوروبية - الجولة السادسة',
    channel: 'beIN SPORTS',
    duration: '8:44',
    timeAgo: 'قبل 4 ساعات',
    views: '540 ألف مشاهدة',
    thumbnail: 'https://i.ytimg.com/vi/h5eCgxrtmUk/hqdefault.jpg',
    videoUrl: 'https://www.youtube.com/watch?v=h5eCgxrtmUk',
    embedUrl: 'https://www.youtube.com/embed/h5eCgxrtmUk?autoplay=1&controls=1&playsinline=1',
    tournament: 'دوري الأمم الأوروبية',
  ),
  MatchHighlight(
    id: 'Q1dhBbciRpQ',
    title: 'ملخص مباراة جمهورية التشيك وإسبانيا | دوري الأمم الأوروبية - الجولة الخامسة',
    channel: 'beIN SPORTS',
    duration: '11:54',
    timeAgo: 'قبل يومين',
    views: '690 ألف مشاهدة',
    thumbnail: 'https://i.ytimg.com/vi/Q1dhBbciRpQ/hqdefault.jpg',
    videoUrl: 'https://www.youtube.com/watch?v=Q1dhBbciRpQ',
    embedUrl: 'https://www.youtube.com/embed/Q1dhBbciRpQ?autoplay=1&controls=1&playsinline=1',
    tournament: 'دوري الأمم الأوروبية',
  ),
  MatchHighlight(
    id: 'k8F2o_L9wQs',
    title: 'ملخص مباراة إيطاليا وفرنسا | دوري الأمم الأوروبية - الجولة الخامسة',
    channel: 'beIN SPORTS',
    duration: '9:54',
    timeAgo: 'قبل 3 أيام',
    views: '1.2 مليون مشاهدة',
    thumbnail: 'https://i.ytimg.com/vi/k8F2o_L9wQs/hqdefault.jpg',
    videoUrl: 'https://www.youtube.com/watch?v=k8F2o_L9wQs',
    embedUrl: 'https://www.youtube.com/embed/k8F2o_L9wQs?autoplay=1&controls=1&playsinline=1',
    tournament: 'دوري الأمم الأوروبية',
  ),
  MatchHighlight(
    id: '9vYgRkF-qXw',
    title: 'ملخص مباراة ريال مدريد وبرشلونة | كلاسيكو الدوري الإسباني',
    channel: 'beIN SPORTS',
    duration: '14:20',
    timeAgo: 'قبل أسبوع',
    views: '3.4 مليون مشاهدة',
    thumbnail: 'https://i.ytimg.com/vi/9vYgRkF-qXw/hqdefault.jpg',
    videoUrl: 'https://www.youtube.com/watch?v=9vYgRkF-qXw',
    embedUrl: 'https://www.youtube.com/embed/9vYgRkF-qXw?autoplay=1&controls=1&playsinline=1',
    tournament: 'الدوري الإسباني',
  ),
  MatchHighlight(
    id: 'mQe2nZ8yW0g',
    title: 'ملخص مباراة ليفربول ومانشستر سيتي | قمة الدوري الإنجليزي الممتاز',
    channel: 'beIN SPORTS',
    duration: '13:15',
    timeAgo: 'قبل أسبوع',
    views: '2.8 مليون مشاهدة',
    thumbnail: 'https://i.ytimg.com/vi/mQe2nZ8yW0g/hqdefault.jpg',
    videoUrl: 'https://www.youtube.com/watch?v=mQe2nZ8yW0g',
    embedUrl: 'https://www.youtube.com/embed/mQe2nZ8yW0g?autoplay=1&controls=1&playsinline=1',
    tournament: 'الدوري الإنجليزي',
  ),
  MatchHighlight(
    id: 'b8L2w_P0qZs',
    title: 'ملخص مباراة بايرن ميونخ وباريس سان جيرمان | دوري أبطال أوروبا',
    channel: 'beIN SPORTS',
    duration: '12:30',
    timeAgo: 'قبل أسبوعين',
    views: '1.9 مليون مشاهدة',
    thumbnail: 'https://i.ytimg.com/vi/b8L2w_P0qZs/hqdefault.jpg',
    videoUrl: 'https://www.youtube.com/watch?v=b8L2w_P0qZs',
    embedUrl: 'https://www.youtube.com/embed/b8L2w_P0qZs?autoplay=1&controls=1&playsinline=1',
    tournament: 'دوري أبطال أوروبا',
  ),
  MatchHighlight(
    id: 'x7N3q_K1vYs',
    title: 'ملخص مباراة مصر ومالاوي | تصفيات كأس أمم إفريقيا 2027',
    channel: 'beIN SPORTS',
    duration: '10:53',
    timeAgo: 'قبل يومين',
    views: '950 ألف مشاهدة',
    thumbnail: 'https://i.ytimg.com/vi/x7N3q_K1vYs/hqdefault.jpg',
    videoUrl: 'https://www.youtube.com/watch?v=x7N3q_K1vYs',
    embedUrl: 'https://www.youtube.com/embed/x7N3q_K1vYs?autoplay=1&controls=1&playsinline=1',
    tournament: 'كأس أمم إفريقيا',
  ),
];

class MatchHighlightsService {
  MatchHighlightsService._();

  static Future<List<MatchHighlight>> fetchHighlights({String query = ''}) async {
    final client = HttpClient()..connectionTimeout = const Duration(seconds: 8);
    try {
      final term = query.trim().isNotEmpty
          ? 'ملخص ${query.trim()} beIN SPORTS'
          : 'ملخصات مباريات beIN SPORTS';
      final uri = Uri.parse('https://www.youtube.com/results?search_query=${Uri.encodeQueryComponent(term)}');
      final req = await client.getUrl(uri);
      req.headers.set('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36');
      req.headers.set('Accept-Language', 'ar,en;q=0.9');
      final res = await req.close().timeout(const Duration(seconds: 8));
      if (res.statusCode == 200) {
        final html = await res.transform(utf8.decoder).join();
        final match = RegExp(r'var ytInitialData = ({.*?});<\/script>').firstMatch(html);
        if (match != null) {
          final jsonStr = match.group(1);
          if (jsonStr != null) {
            final data = jsonDecode(jsonStr);
            final List<MatchHighlight> results = [];
            _extractFromYt(data, results);
            if (results.isNotEmpty) return results;
          }
        }
      }
    } catch (_) {
    } finally {
      client.close();
    }
    return [];
  }

  static void _extractFromYt(dynamic obj, List<MatchHighlight> out) {
    if (obj is Map) {
      if (obj.containsKey('videoRenderer')) {
        final vr = obj['videoRenderer'] as Map<String, dynamic>;
        final vid = vr['videoId']?.toString() ?? '';
        String title = '';
        final titleObj = vr['title'];
        if (titleObj is Map) {
          final runs = titleObj['runs'];
          if (runs is List && runs.isNotEmpty) {
            title = runs[0]['text']?.toString() ?? '';
          } else if (titleObj['simpleText'] != null) {
            title = titleObj['simpleText'].toString();
          }
        }

        final duration = vr['lengthText']?['simpleText']?.toString() ?? '';
        final timeAgo = vr['publishedTimeText']?['simpleText']?.toString() ?? '';
        final views = vr['viewCountText']?['simpleText']?.toString() ?? '';

        String channel = 'beIN SPORTS';
        final ownerText = vr['ownerText'];
        if (ownerText is Map && ownerText['runs'] is List && ownerText['runs'].isNotEmpty) {
          channel = ownerText['runs'][0]['text']?.toString() ?? 'beIN SPORTS';
        }

        if (vid.isNotEmpty && title.isNotEmpty && (title.contains('ملخص') || title.contains('مباراة') || title.contains('أهداف'))) {
          String tour = 'بطولات كبرى';
          if (title.contains('الأمم الأوروبية') || title.contains('Nations League')) {
            tour = 'دوري الأمم الأوروبية';
          } else if (title.contains('أبطال أوروبا') || title.contains('Champions League')) {
            tour = 'دوري أبطال أوروبا';
          } else if (title.contains('الإنجليزي') || title.contains('Premier')) {
            tour = 'الدوري الإنجليزي';
          } else if (title.contains('الإسباني') || title.contains('LaLiga') || title.contains('كلاسيكو')) {
            tour = 'الدوري الإسباني';
          } else if (title.contains('إفريقيا') || title.contains('أمم إفريقيا')) {
            tour = 'كأس أمم إفريقيا';
          } else if (title.contains('العالم') || title.contains('World Cup')) {
            tour = 'كأس العالم';
          }

          out.add(MatchHighlight(
            id: vid,
            title: title,
            channel: channel,
            duration: duration,
            timeAgo: timeAgo,
            views: views,
            thumbnail: 'https://i.ytimg.com/vi/$vid/hqdefault.jpg',
            videoUrl: 'https://www.youtube.com/watch?v=$vid',
            embedUrl: 'https://www.youtube.com/embed/$vid?autoplay=1&controls=1&playsinline=1',
            tournament: tour,
          ));
        }
      }
      for (final val in obj.values) {
        _extractFromYt(val, out);
      }
    } else if (obj is List) {
      for (final item in obj) {
        _extractFromYt(item, out);
      }
    }
  }
}

