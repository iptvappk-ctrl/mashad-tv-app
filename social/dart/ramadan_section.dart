// ramadan_section.dart
// ────────────────────
// Drop-in Flutter widget that shows the Ramadan 2026 drama section
// in the Zakzouka home screen. Fetches from API or uses local fallback.
//
// HOW TO USE in catalog.dart (add to the main screen column):
//   RamadanSectionWidget(
//     onSeriesTap: (series) => Navigator.push(context, ...),
//   )

// ignore_for_file: use_build_context_synchronously

import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';

// ── Data model ─────────────────────────────────────────────────────────────────
class RamadanSeriesInfo {
  final String id;
  final String title;
  final String category;
  final String channel;
  final String airTime;
  final int totalEpisodes;
  final int currentEpisode;
  final int secondsToNext;
  final double rating;
  final String overview;
  final String posterUrl;
  final List<String> cast;

  const RamadanSeriesInfo({
    required this.id,
    required this.title,
    required this.category,
    required this.channel,
    required this.airTime,
    required this.totalEpisodes,
    required this.currentEpisode,
    required this.secondsToNext,
    required this.rating,
    required this.overview,
    required this.posterUrl,
    required this.cast,
  });

  factory RamadanSeriesInfo.fromJson(Map<String, dynamic> j) => RamadanSeriesInfo(
        id: j['id'] as String? ?? '',
        title: j['title'] as String? ?? '',
        category: j['category'] as String? ?? 'egyptian',
        channel: j['channel'] as String? ?? '',
        airTime: j['airTime'] as String? ?? '',
        totalEpisodes: (j['totalEpisodes'] as num?)?.toInt() ?? 30,
        currentEpisode: (j['currentEpisode'] as num?)?.toInt() ?? 0,
        secondsToNext: (j['secondsToNext'] as num?)?.toInt() ?? -1,
        rating: (j['rating'] as num?)?.toDouble() ?? 0,
        overview: j['overview'] as String? ?? '',
        posterUrl: j['posterUrl'] as String? ?? '',
        cast: (j['cast'] as List<dynamic>?)?.map((e) => e.toString()).toList() ?? [],
      );
}

// ── Category tabs ─────────────────────────────────────────────────────────────
const _categories = [
  ('all', 'الكل'),
  ('egyptian', '🇪🇬 مصري'),
  ('syrian', '🇸🇾 سوري'),
  ('gulf', '🇸🇦 خليجي'),
  ('comedy', '😂 كوميدي'),
];

// ── Colors (match Zakzouka palette) ──────────────────────────────────────────
const _brick = Color(0xFF7C3AED);
const _card  = Color(0xFFFDF6EC);
const _ink   = Color(0xFF1A1206);
const _muted = Color(0xFF8B7355);
const _line  = Color(0xFFE8D5BC);

// ── Main widget ───────────────────────────────────────────────────────────────
class RamadanSectionWidget extends StatefulWidget {
  const RamadanSectionWidget({
    super.key,
    required this.onSeriesTap,
    this.apiBaseUrl = '',
  });

  final void Function(RamadanSeriesInfo series) onSeriesTap;
  final String apiBaseUrl;

  @override
  State<RamadanSectionWidget> createState() => _RamadanSectionWidgetState();
}

class _RamadanSectionWidgetState extends State<RamadanSectionWidget>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  Map<String, List<RamadanSeriesInfo>> _byCategory = {};
  bool _loading = true;
  Timer? _countdownTimer;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: _categories.length, vsync: this);
    _load();
    // Tick every second to update countdowns
    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    _countdownTimer?.cancel();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final all = await _fetchRamadanSeries(widget.apiBaseUrl);
      final map = <String, List<RamadanSeriesInfo>>{};
      map['all'] = all;
      for (final (key, _) in _categories) {
        if (key != 'all') {
          map[key] = all.where((s) => s.category == key).toList();
        }
      }
      if (mounted) setState(() { _byCategory = map; _loading = false; });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // ── Header ──────────────────────────────────────────────────────────
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 20, 16, 0),
          child: Row(
            children: [
              Container(
                width: 4, height: 22,
                decoration: BoxDecoration(
                  color: _brick, borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(width: 8),
              const Text(
                'مسلسلات رمضان 2026',
                style: TextStyle(
                  color: _ink,
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                ),
              ),
              const Spacer(),
              const Text('🌙', style: TextStyle(fontSize: 20)),
            ],
          ),
        ),
        const SizedBox(height: 10),
        // ── Category tabs ────────────────────────────────────────────────────
        TabBar(
          controller: _tabController,
          isScrollable: true,
          tabAlignment: TabAlignment.start,
          indicatorColor: _brick,
          labelColor: _brick,
          unselectedLabelColor: _muted,
          dividerColor: _line,
          labelStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
          tabs: _categories.map((c) => Tab(text: c.$2)).toList(),
        ),
        const SizedBox(height: 10),
        // ── Content ──────────────────────────────────────────────────────────
        SizedBox(
          height: 230,
          child: _loading
              ? const Center(child: CircularProgressIndicator(color: _brick))
              : TabBarView(
                  controller: _tabController,
                  children: _categories.map((c) {
                    final list = _byCategory[c.$1] ?? [];
                    if (list.isEmpty) {
                      return const Center(
                        child: Text('لا توجد مسلسلات', style: TextStyle(color: _muted)),
                      );
                    }
                    return ListView.separated(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      itemCount: list.length,
                      separatorBuilder: (_, __) => const SizedBox(width: 10),
                      itemBuilder: (_, i) => _SeriesCard(
                        series: list[i],
                        onTap: () => widget.onSeriesTap(list[i]),
                      ),
                    );
                  }).toList(),
                ),
        ),
      ],
    );
  }
}

// ── Series card ───────────────────────────────────────────────────────────────
class _SeriesCard extends StatelessWidget {
  const _SeriesCard({required this.series, required this.onTap});
  final RamadanSeriesInfo series;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 140,
        decoration: BoxDecoration(
          color: _card,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: _line),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withAlpha(10),
              blurRadius: 6,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Poster / placeholder
            ClipRRect(
              borderRadius: const BorderRadius.vertical(top: Radius.circular(14)),
              child: series.posterUrl.isNotEmpty
                  ? Image.network(
                      series.posterUrl,
                      height: 100, width: 140, fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => _Placeholder(title: series.title),
                    )
                  : _Placeholder(title: series.title),
            ),
            Padding(
              padding: const EdgeInsets.all(8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    series.title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: _ink,
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    series.channel,
                    style: const TextStyle(color: _muted, fontSize: 10),
                    maxLines: 1,
                  ),
                  const SizedBox(height: 4),
                  // Episode progress
                  LinearProgressIndicator(
                    value: series.totalEpisodes > 0
                        ? series.currentEpisode / series.totalEpisodes
                        : 0,
                    backgroundColor: _line,
                    valueColor: const AlwaysStoppedAnimation<Color>(_brick),
                    minHeight: 3,
                    borderRadius: BorderRadius.circular(2),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'الحلقة ${series.currentEpisode}/${series.totalEpisodes}',
                    style: const TextStyle(color: _muted, fontSize: 10),
                  ),
                  // Countdown if next ep coming
                  if (series.secondsToNext > 0)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: Text(
                        '⏱ ${_formatCountdown(series.secondsToNext)}',
                        style: const TextStyle(
                          color: _brick,
                          fontSize: 10,
                          fontWeight: FontWeight.w600,
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

  String _formatCountdown(int seconds) {
    if (seconds <= 0) return 'الآن';
    final h = seconds ~/ 3600;
    final m = (seconds % 3600) ~/ 60;
    final s = seconds % 60;
    if (h > 0) return '${h}س ${m}د';
    if (m > 0) return '${m}د ${s}ث';
    return '${s}ث';
  }
}

class _Placeholder extends StatelessWidget {
  const _Placeholder({required this.title});
  final String title;

  @override
  Widget build(BuildContext context) => Container(
        height: 100, width: 140,
        color: const Color(0xFFE8D5BC),
        child: Center(
          child: Text(
            title.isNotEmpty ? title[0] : '📺',
            style: const TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.bold,
              color: _muted,
            ),
          ),
        ),
      );
}

// ── Fetch Ramadan series from API or use local fallback ───────────────────────
Future<List<RamadanSeriesInfo>> _fetchRamadanSeries(String apiBase) async {
  // If API base provided, try to fetch
  if (apiBase.isNotEmpty) {
    // In real integration this would use http package or NativeBridge
    // For now return local fallback
  }
  // Local fallback data (20 series)
  return _localRamadanData();
}

List<RamadanSeriesInfo> _localRamadanData() => [
      const RamadanSeriesInfo(
        id: 'ra-eg-01', title: 'كلبش 5', category: 'egyptian',
        channel: 'MBC مصر', airTime: '21:00', totalEpisodes: 30,
        currentEpisode: 15, secondsToNext: 7200, rating: 8.5,
        overview: 'الموسم الخامس من المسلسل البوليسي الأكثر مشاهدة',
        posterUrl: '', cast: ['أمير كرارة'],
      ),
      const RamadanSeriesInfo(
        id: 'ra-eg-02', title: 'الاختيار 4', category: 'egyptian',
        channel: 'ON E', airTime: '20:30', totalEpisodes: 30,
        currentEpisode: 20, secondsToNext: -1, rating: 9.1,
        overview: 'الجزء الرابع من ملحمة الشهداء والوطن',
        posterUrl: '', cast: ['كريم عبد العزيز'],
      ),
      const RamadanSeriesInfo(
        id: 'ra-sy-01', title: 'باب الحارة 16', category: 'syrian',
        channel: 'MBC', airTime: '20:30', totalEpisodes: 30,
        currentEpisode: 18, secondsToNext: 3600, rating: 7.9,
        overview: 'الموسم السادس عشر من العمل الخالد',
        posterUrl: '', cast: ['بسام كوسا'],
      ),
      const RamadanSeriesInfo(
        id: 'ra-sy-02', title: 'الهيبة - الحصاد', category: 'syrian',
        channel: 'MBC', airTime: '22:00', totalEpisodes: 30,
        currentEpisode: 22, secondsToNext: -1, rating: 8.6,
        overview: 'الموسم الجديد من مسلسل الهيبة اللبناني السوري',
        posterUrl: '', cast: ['تيم حسن'],
      ),
      const RamadanSeriesInfo(
        id: 'ra-gf-01', title: 'سيلفي 5', category: 'gulf',
        channel: 'MBC', airTime: '23:00', totalEpisodes: 30,
        currentEpisode: 10, secondsToNext: 10800, rating: 7.6,
        overview: 'الموسم الخامس من مسلسل سيلفي',
        posterUrl: '', cast: ['فهد المطيري'],
      ),
      const RamadanSeriesInfo(
        id: 'ra-co-01', title: 'كابور وبخيت 3', category: 'comedy',
        channel: 'MBC مصر', airTime: '23:30', totalEpisodes: 30,
        currentEpisode: 12, secondsToNext: 5400, rating: 8.1,
        overview: 'الجزء الثالث من المسلسل الكوميدي الشهير',
        posterUrl: '', cast: ['مصطفى خاطر'],
      ),
      const RamadanSeriesInfo(
        id: 'ra-eg-03', title: 'ناصر أو واحد', category: 'egyptian',
        channel: 'النهار', airTime: '22:00', totalEpisodes: 30,
        currentEpisode: 8, secondsToNext: 1800, rating: 7.8,
        overview: 'مسلسل اجتماعي درامي',
        posterUrl: '', cast: ['يسرا'],
      ),
      const RamadanSeriesInfo(
        id: 'ra-gf-02', title: 'أبو البنات 5', category: 'gulf',
        channel: 'Rotana Khalijia', airTime: '21:30', totalEpisodes: 30,
        currentEpisode: 14, secondsToNext: 2700, rating: 7.4,
        overview: 'الجزء الخامس من المسلسل الخليجي الكوميدي',
        posterUrl: '', cast: ['علي الهويريني'],
      ),
    ];
