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
        itemCount: _events.length,
        itemBuilder: (_, index) => _eventCard(_events[index]),
      );
    }

    // جدول المباريات مع فلتر مباريات اليوم
    if (_footballMatches.isNotEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(14, 8, 14, 18),
        children: [
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
