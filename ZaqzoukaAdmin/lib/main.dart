import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:http/http.dart' as http;

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ZaqzoukaAdminApp());
}

class ZaqzoukaAdminApp extends StatelessWidget {
  const ZaqzoukaAdminApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'تحليلات زقزوقة TV',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0C0A15),
        primaryColor: const Color(0xFF8B5CF6),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF8B5CF6),
          secondary: Color(0xFF00E5FF),
          surface: Color(0xFF161226),
        ),
        cardColor: const Color(0xFF161226),
        fontFamily: 'Roboto',
      ),
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [Locale('ar', 'EG')],
      locale: const Locale('ar', 'EG'),
      home: const DashboardHomeScreen(),
    );
  }
}

class DashboardHomeScreen extends StatefulWidget {
  const DashboardHomeScreen({super.key});

  @override
  State<DashboardHomeScreen> createState() => _DashboardHomeScreenState();
}

class _DashboardHomeScreenState extends State<DashboardHomeScreen>
    with SingleTickerProviderStateMixin {
  String _apiBaseUrl = 'https://zaqzouka-dashboard-api.vercel.app';
  String _adminSecret = 'zaqzouka_secret_2026';

  bool _isLoading = true;
  final bool _isAutoRefresh = true;
  String? _errorMessage;
  DateTime? _lastUpdated;
  Timer? _refreshTimer;

  Map<String, dynamic> _kpis = {};
  Map<String, dynamic> _analytics = {};
  List<dynamic> _sessions = [];
  List<dynamic> _topContent = [];
  List<dynamic> _searches = [];
  List<dynamic> _activityLog = [];

  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    _fetchStats();
    _startAutoRefresh();
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    _tabController.dispose();
    super.dispose();
  }

  void _startAutoRefresh() {
    _refreshTimer?.cancel();
    if (_isAutoRefresh) {
      _refreshTimer = Timer.periodic(const Duration(seconds: 15), (_) {
        _fetchStats(isBackground: true);
      });
    }
  }

  Future<void> _fetchStats({bool isBackground = false}) async {
    if (!isBackground) {
      setState(() {
        _isLoading = true;
        _errorMessage = null;
      });
    }

    try {
      final url = Uri.parse('$_apiBaseUrl/api/stats');
      final res = await http.get(
        url,
        headers: {
          'x-admin-secret': _adminSecret,
          'Accept': 'application/json',
        },
      ).timeout(const Duration(seconds: 12));

      if (res.statusCode == 200) {
        final data = json.decode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
        if (mounted) {
          setState(() {
            _kpis = data['kpis'] ?? {};
            _analytics = data['analytics'] ?? {};
            _sessions = data['sessions'] ?? [];
            _topContent = data['topContent'] ?? [];
            _searches = data['searches'] ?? [];
            _activityLog = data['activityLog'] ?? [];
            _lastUpdated = DateTime.now();
            _isLoading = false;
            _errorMessage = null;
          });
        }
      } else {
        throw Exception('رمز الخطأ من السيرفر: ${res.statusCode}');
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceAll('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF130F22),
        elevation: 0,
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
              decoration: BoxDecoration(
                color: const Color(0xFF8B5CF6).withValues(alpha: 0.2),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFF8B5CF6), width: 1.2),
              ),
              child: const Text(
                'ZQ',
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF8B5CF6),
                  fontSize: 16,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text(
                    'تحليلات زقزوقة TV',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  Row(
                    children: [
                      Container(
                        width: 7,
                        height: 7,
                        decoration: BoxDecoration(
                          color: _errorMessage == null
                              ? const Color(0xFF00E676)
                              : Colors.redAccent,
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 5),
                      Text(
                        _errorMessage == null
                            ? 'متصل بالسحابة (Firebase/Vercel)'
                            : 'فشل الاتصال',
                        style: TextStyle(
                          fontSize: 11,
                          color: Colors.white.withValues(alpha: 0.7),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'تحديث فوري',
            icon: const Icon(Icons.refresh, color: Color(0xFF00E5FF)),
            onPressed: () => _fetchStats(),
          ),
          IconButton(
            tooltip: 'إعدادات الاتصال',
            icon: const Icon(Icons.tune, color: Colors.white70),
            onPressed: _showSettingsDialog,
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: const Color(0xFF00E5FF),
          labelColor: const Color(0xFF00E5FF),
          unselectedLabelColor: Colors.white60,
          isScrollable: true,
          tabAlignment: TabAlignment.start,
          tabs: const [
            Tab(icon: Icon(Icons.dashboard_outlined), text: 'نظرة عامة'),
            Tab(icon: Icon(Icons.phone_android_outlined), text: 'الجلسات الحية'),
            Tab(icon: Icon(Icons.bar_chart_outlined), text: 'الرسوم البيانية'),
            Tab(icon: Icon(Icons.history_outlined), text: 'سجل النشاط'),
          ],
        ),
      ),
      body: _isLoading && _sessions.isEmpty
          ? const Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  CircularProgressIndicator(color: Color(0xFF8B5CF6)),
                  SizedBox(height: 16),
                  Text('جاري تحميل بيانات السيرفر الحي...'),
                ],
              ),
            )
          : _errorMessage != null && _sessions.isEmpty
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.cloud_off, size: 64, color: Colors.redAccent),
                        const SizedBox(height: 16),
                        Text(
                          'تعذر جلب البيانات:\n$_errorMessage',
                          textAlign: TextAlign.center,
                          style: const TextStyle(color: Colors.white70),
                        ),
                        const SizedBox(height: 20),
                        ElevatedButton.icon(
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFF8B5CF6),
                          ),
                          onPressed: () => _fetchStats(),
                          icon: const Icon(Icons.refresh),
                          label: const Text('إعادة المحاولة'),
                        ),
                      ],
                    ),
                  ),
                )
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildOverviewTab(),
                    _buildSessionsTab(),
                    _buildChartsTab(),
                    _buildActivityLogTab(),
                  ],
                ),
    );
  }

  // ─── TAB 1: OVERVIEW ────────────────────────────────────────────────────────
  Widget _buildOverviewTab() {
    final activeNow = _kpis['activeNow'] ?? 0;
    final totalOpens = _kpis['firstOpens'] ?? 0;
    final totalInstalls = _kpis['totalInstalls'] ?? 0;
    final avgDuration = _kpis['avgDurationMinutes'] ?? '0';
    final streamHealth = _kpis['streamHealth'] ?? '100.0';
    final bounceRate = _kpis['bounceRate'] ?? 0;
    final tvRatio = _kpis['tvRatio'] ?? 0;
    final phoneRatio = _kpis['phoneRatio'] ?? 100;
    final totalSearches = _kpis['totalSearches'] ?? 0;
    final totalPlays = _kpis['totalPlays'] ?? 0;

    return RefreshIndicator(
      color: const Color(0xFF00E5FF),
      backgroundColor: const Color(0xFF161226),
      onRefresh: () => _fetchStats(),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Banner: Active Now
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF2A1550), Color(0xFF17112C)],
                begin: Alignment.topRight,
                end: Alignment.bottomLeft,
              ),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: const Color(0xFF8B5CF6).withValues(alpha: 0.4)),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF8B5CF6).withValues(alpha: 0.15),
                  blurRadius: 18,
                  offset: const Offset(0, 8),
                ),
              ],
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFF00E676).withValues(alpha: 0.15),
                    shape: BoxShape.circle,
                    border: Border.all(color: const Color(0xFF00E676), width: 2),
                  ),
                  child: const Icon(Icons.sensors, color: Color(0xFF00E676), size: 32),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'المتصلون الآن بالتطبيق',
                        style: TextStyle(fontSize: 14, color: Colors.white70),
                      ),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          Text(
                            '$activeNow',
                            style: const TextStyle(
                              fontSize: 36,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF00E676),
                            ),
                          ),
                          const SizedBox(width: 8),
                          const Text(
                            'مستخدم نشط',
                            style: TextStyle(fontSize: 14, color: Colors.white54),
                          ),
                        ],
                      ),
                      if (_lastUpdated != null)
                        Text(
                          'آخر فحص: ${_lastUpdated!.hour.toString().padLeft(2, '0')}:${_lastUpdated!.minute.toString().padLeft(2, '0')}:${_lastUpdated!.second.toString().padLeft(2, '0')}',
                          style: TextStyle(
                            fontSize: 11,
                            color: Colors.white.withValues(alpha: 0.4),
                          ),
                        ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // KPIs Grid
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1.35,
            children: [
              _buildKpiCard(
                title: 'إجمالي الفتح والزيارات',
                value: '$totalOpens',
                subtitle: 'منها $totalInstalls تثبيت جديد',
                icon: Icons.install_mobile,
                color: const Color(0xFF00E5FF),
              ),
              _buildKpiCard(
                title: 'متوسط وقت المشاهدة',
                value: '$avgDuration د',
                subtitle: 'لكل جلسة مكتملة',
                icon: Icons.timer_outlined,
                color: const Color(0xFFFFB800),
              ),
              _buildKpiCard(
                title: 'صحة واستقرار البث',
                value: '$streamHealth%',
                subtitle: 'بثوث بدون أعطال',
                icon: Icons.verified_outlined,
                color: const Color(0xFF00E676),
              ),
              _buildKpiCard(
                title: 'معدل الارتداد',
                value: '$bounceRate%',
                subtitle: 'خروج فوري دون تصفح',
                icon: Icons.warning_amber_rounded,
                color: const Color(0xFFFF5757),
              ),
              _buildKpiCard(
                title: 'شاشات TV مقابل هواتف',
                value: 'TV: $tvRatio%',
                subtitle: 'هواتف: $phoneRatio%',
                icon: Icons.tv,
                color: const Color(0xFFB388FF),
              ),
              _buildKpiCard(
                title: 'عمليات التشغيل والبحث',
                value: '$totalPlays تشغيل',
                subtitle: '$totalSearches عملية بحث',
                icon: Icons.play_circle_outline,
                color: const Color(0xFFEC407A),
              ),
            ],
          ),

          const SizedBox(height: 20),
          // Quick Top Content Preview
          _buildCardContainer(
            title: 'المحتوى الأكثر مشاهدة 🍿',
            child: _topContent.isEmpty
                ? const Padding(
                    padding: EdgeInsets.all(16.0),
                    child: Center(
                      child: Text('لا توجد مشاهدات مسجلة بعد',
                          style: TextStyle(color: Colors.white54)),
                    ),
                  )
                : Column(
                    children: _topContent.take(5).map((item) {
                      return ListTile(
                        dense: true,
                        leading: CircleAvatar(
                          backgroundColor: const Color(0xFF8B5CF6).withValues(alpha: 0.2),
                          child: Text(item['categoryIcon'] ?? '🎬',
                              style: const TextStyle(fontSize: 16)),
                        ),
                        title: Text(
                          item['title'] ?? 'محتوى غير مسمى',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        subtitle: Text(
                          item['category'] ?? '',
                          style: const TextStyle(color: Colors.white54, fontSize: 11),
                        ),
                        trailing: Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: const Color(0xFF00E5FF).withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            '${item['views'] ?? 0} مشاهدة',
                            style: const TextStyle(
                              color: Color(0xFF00E5FF),
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                  ),
          ),
        ],
      ),
    );
  }

  // ─── TAB 2: LIVE SESSIONS ───────────────────────────────────────────────────
  Widget _buildSessionsTab() {
    if (_sessions.isEmpty) {
      return const Center(
        child: Text('لا توجد جلسات مسجلة حتى الآن'),
      );
    }

    return RefreshIndicator(
      color: const Color(0xFF00E5FF),
      backgroundColor: const Color(0xFF161226),
      onRefresh: () => _fetchStats(),
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _sessions.length,
        separatorBuilder: (_, index) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final s = _sessions[index] as Map<String, dynamic>;
          final isActive = s['status'] == 'active';
          final device = s['device'] ?? 'Android Device';
          final country = s['country'] ?? 'مصر EG';
          final openTime = s['openTime'] ?? '';
          final duration = s['durationText'] ?? '1 دقيقة';
          final section = s['lastSection'] ?? 'الرئيسية';
          final content = s['lastContent'] ?? '';
          final osVersion = s['osVersion'] ?? '';

          return Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: const Color(0xFF161226),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(
                color: isActive
                    ? const Color(0xFF00E676).withValues(alpha: 0.6)
                    : const Color(0xFF2A2346),
                width: isActive ? 1.5 : 1.0,
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Row(
                        children: [
                          Icon(
                            s['deviceType'] == 'tv' ? Icons.tv : Icons.phone_android,
                            color: const Color(0xFF8B5CF6),
                            size: 20,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              device,
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                                color: Colors.white,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: isActive
                            ? const Color(0xFF00E676).withValues(alpha: 0.15)
                            : Colors.white.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: isActive
                              ? const Color(0xFF00E676)
                              : Colors.white24,
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Container(
                            width: 6,
                            height: 6,
                            decoration: BoxDecoration(
                              color: isActive ? const Color(0xFF00E676) : Colors.white38,
                              shape: BoxShape.circle,
                            ),
                          ),
                          const SizedBox(width: 5),
                          Text(
                            isActive ? 'نشط الآن' : 'منتهية',
                            style: TextStyle(
                              color: isActive ? const Color(0xFF00E676) : Colors.white60,
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: const Color(0xFF00E5FF).withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        '🌍 $country',
                        style: const TextStyle(color: Color(0xFF00E5FF), fontSize: 11),
                      ),
                    ),
                    const SizedBox(width: 8),
                    if (osVersion.isNotEmpty)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: Colors.white.withValues(alpha: 0.08),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          osVersion,
                          style: const TextStyle(color: Colors.white70, fontSize: 11),
                        ),
                      ),
                    const Spacer(),
                    Text(
                      '⏱️ $duration ($openTime)',
                      style: const TextStyle(color: Colors.white54, fontSize: 11),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.all(8),
                  width: double.infinity,
                  decoration: BoxDecoration(
                    color: const Color(0xFF0E0B1A),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.explore_outlined,
                          size: 14, color: Colors.white38),
                      const SizedBox(width: 6),
                      Text(
                        'القسم: $section',
                        style: const TextStyle(color: Colors.white70, fontSize: 11),
                      ),
                      if (content.isNotEmpty) ...[
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            '• $content',
                            style: const TextStyle(
                                color: Color(0xFFFFB800), fontSize: 11),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  // ─── TAB 3: CHARTS & ANALYTICS ──────────────────────────────────────────────
  Widget _buildChartsTab() {
    final deviceBrands = (_analytics['deviceBrands'] as List<dynamic>?) ?? [];
    final topCountries = (_analytics['topCountries'] as List<dynamic>?) ?? [];
    final hourlyTraffic = (_analytics['hourlyTraffic'] as List<dynamic>?) ?? [];

    return RefreshIndicator(
      color: const Color(0xFF00E5FF),
      backgroundColor: const Color(0xFF161226),
      onRefresh: () => _fetchStats(),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 1. Countries Breakdown
          _buildCardContainer(
            title: '🌍 التوزيع الجغرافي (أعلى الدول مشاهدة)',
            child: topCountries.isEmpty
                ? const Padding(
                    padding: EdgeInsets.all(16),
                    child: Center(
                      child: Text('لا توجد بيانات دول بعد',
                          style: TextStyle(color: Colors.white54)),
                    ),
                  )
                : Column(
                    children: topCountries.map((c) {
                      final name = c['name'] ?? '';
                      final count = c['count'] ?? 0;
                      final percent = (c['percent'] ?? 0).toDouble();
                      return Padding(
                        padding: const EdgeInsets.symmetric(
                            vertical: 6, horizontal: 12),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(name,
                                    style: const TextStyle(
                                        color: Colors.white, fontSize: 13)),
                                Text('$count جلسة ($percent%)',
                                    style: const TextStyle(
                                        color: Color(0xFF00E5FF),
                                        fontSize: 12,
                                        fontWeight: FontWeight.bold)),
                              ],
                            ),
                            const SizedBox(height: 4),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(4),
                              child: LinearProgressIndicator(
                                value: percent / 100.0,
                                minHeight: 6,
                                backgroundColor: const Color(0xFF2A2346),
                                valueColor: const AlwaysStoppedAnimation<Color>(
                                    Color(0xFF00E5FF)),
                              ),
                            ),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
          ),
          const SizedBox(height: 16),

          // 2. Device Brands
          _buildCardContainer(
            title: '📱 ماركات وموديلات الأجهزة الفعلية',
            child: deviceBrands.isEmpty
                ? const Padding(
                    padding: EdgeInsets.all(16),
                    child: Center(
                      child: Text('لا توجد أجهزة مسجلة',
                          style: TextStyle(color: Colors.white54)),
                    ),
                  )
                : Column(
                    children: deviceBrands.map((b) {
                      final brand = b['brand'] ?? '';
                      final count = b['count'] ?? 0;
                      final percent = (b['percent'] ?? 0).toDouble();
                      return Padding(
                        padding: const EdgeInsets.symmetric(
                            vertical: 6, horizontal: 12),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(brand,
                                    style: const TextStyle(
                                        color: Colors.white, fontSize: 13)),
                                Text('$count جهاز ($percent%)',
                                    style: const TextStyle(
                                        color: Color(0xFF8B5CF6),
                                        fontSize: 12,
                                        fontWeight: FontWeight.bold)),
                              ],
                            ),
                            const SizedBox(height: 4),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(4),
                              child: LinearProgressIndicator(
                                value: percent / 100.0,
                                minHeight: 6,
                                backgroundColor: const Color(0xFF2A2346),
                                valueColor: const AlwaysStoppedAnimation<Color>(
                                    Color(0xFF8B5CF6)),
                              ),
                            ),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
          ),
          const SizedBox(height: 16),

          // 3. Hourly Activity
          _buildCardContainer(
            title: '📊 ساعات الذروة والنشاط (خلال اليوم)',
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: SizedBox(
                height: 120,
                child: CustomPaint(
                  size: const Size(double.infinity, 120),
                  painter: HourlyChartPainter(
                    hourlyTraffic: hourlyTraffic.map((e) => (e as num).toInt()).toList(),
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 4. Searches Radar
          _buildCardContainer(
            title: '🔍 رادار عمليات البحث وطلبات المستخدمين',
            child: _searches.isEmpty
                ? const Padding(
                    padding: EdgeInsets.all(16),
                    child: Center(
                      child: Text('لا توجد عمليات بحث مسجلة بعد',
                          style: TextStyle(color: Colors.white54)),
                    ),
                  )
                : Column(
                    children: _searches.take(8).map((s) {
                      final query = s['query'] ?? '';
                      final count = s['count'] ?? 1;
                      final isFound = s['status'] == 'available';

                      return ListTile(
                        dense: true,
                        leading: const Icon(Icons.search,
                            size: 18, color: Colors.white38),
                        title: Text(
                          query,
                          style: const TextStyle(
                              color: Colors.white, fontSize: 13),
                        ),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: isFound
                                    ? const Color(0xFF00E676).withValues(alpha: 0.15)
                                    : const Color(0xFFFF5757).withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Text(
                                isFound ? 'متوفر' : 'مطلوب بقوة',
                                style: TextStyle(
                                  fontSize: 10,
                                  color: isFound
                                      ? const Color(0xFF00E676)
                                      : const Color(0xFFFF5757),
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                            const SizedBox(width: 8),
                            Text(
                              '$count مرة',
                              style: const TextStyle(
                                  color: Colors.white54, fontSize: 12),
                            ),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
          ),
        ],
      ),
    );
  }

  // ─── TAB 4: ACTIVITY LOG ───────────────────────────────────────────────────
  Widget _buildActivityLogTab() {
    if (_activityLog.isEmpty) {
      return const Center(
        child: Text('لا توجد أنشطة مسجلة حتى الآن'),
      );
    }

    return RefreshIndicator(
      color: const Color(0xFF00E5FF),
      backgroundColor: const Color(0xFF161226),
      onRefresh: () => _fetchStats(),
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _activityLog.length,
        separatorBuilder: (_, index) => const SizedBox(height: 8),
        itemBuilder: (context, index) {
          final act = _activityLog[index] as Map<String, dynamic>;
          final icon = act['icon'] ?? '📱';
          final text = act['text'] ?? '';
          final time = act['time'] ?? '';
          final type = act['type'] ?? 'info';

          Color typeColor = Colors.white54;
          if (type == 'open') typeColor = const Color(0xFF00E676);
          if (type == 'play') typeColor = const Color(0xFF00E5FF);
          if (type == 'bounce') typeColor = const Color(0xFFFF5757);
          if (type == 'search') typeColor = const Color(0xFFFFB800);

          return Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: const Color(0xFF161226),
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: const Color(0xFF2A2346)),
            ),
            child: Row(
              children: [
                Text(icon, style: const TextStyle(fontSize: 18)),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    text,
                    style: const TextStyle(color: Colors.white, fontSize: 12.5),
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  time,
                  style: TextStyle(color: typeColor, fontSize: 11),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  // ─── Helper Widgets ────────────────────────────────────────────────────────
  Widget _buildKpiCard({
    required String title,
    required String value,
    required String subtitle,
    required IconData icon,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFF161226),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                title,
                style: const TextStyle(fontSize: 11, color: Colors.white70),
              ),
              Icon(icon, color: color, size: 18),
            ],
          ),
          Text(
            value,
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: color,
            ),
          ),
          Text(
            subtitle,
            style: TextStyle(fontSize: 10, color: Colors.white.withValues(alpha: 0.4)),
          ),
        ],
      ),
    );
  }

  Widget _buildCardContainer({required String title, required Widget child}) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF161226),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFF2A2346)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(14),
            child: Text(
              title,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
          ),
          const Divider(height: 1, color: Color(0xFF2A2346)),
          child,
        ],
      ),
    );
  }

  void _showSettingsDialog() {
    final urlController = TextEditingController(text: _apiBaseUrl);
    final secretController = TextEditingController(text: _adminSecret);

    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: const Color(0xFF17122C),
          title: const Text('إعدادات الاتصال بالسيرفر'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: urlController,
                decoration: const InputDecoration(
                  labelText: 'رابط الـ API الأساسي',
                  hintText: 'https://...',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: secretController,
                decoration: const InputDecoration(
                  labelText: 'الرمز السري (Admin Secret)',
                ),
                obscureText: true,
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('إلغاء'),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF8B5CF6),
              ),
              onPressed: () {
                setState(() {
                  _apiBaseUrl = urlController.text.trim();
                  _adminSecret = secretController.text.trim();
                });
                Navigator.pop(ctx);
                _fetchStats();
              },
              child: const Text('حفظ واتصال'),
            ),
          ],
        );
      },
    );
  }
}

// ─── Custom Hourly Chart Painter ─────────────────────────────────────────────
class HourlyChartPainter extends CustomPainter {
  final List<int> hourlyTraffic;
  HourlyChartPainter({required this.hourlyTraffic});

  @override
  void paint(Canvas canvas, Size size) {
    if (hourlyTraffic.isEmpty) return;

    final paintLine = Paint()
      ..color = const Color(0xFF00E5FF)
      ..strokeWidth = 2.5
      ..style = PaintingStyle.stroke;

    final paintFill = Paint()
      ..shader = LinearGradient(
        colors: [
          const Color(0xFF00E5FF).withValues(alpha: 0.35),
          const Color(0xFF00E5FF).withValues(alpha: 0.0),
        ],
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
      ).createShader(Rect.fromLTWH(0, 0, size.width, size.height))
      ..style = PaintingStyle.fill;

    final maxVal = (hourlyTraffic.reduce((a, b) => a > b ? a : b)).toDouble();
    final effectiveMax = maxVal < 5 ? 5.0 : maxVal;

    final path = Path();
    final fillPath = Path();

    final stepX = size.width / (hourlyTraffic.length - 1);

    for (int i = 0; i < hourlyTraffic.length; i++) {
      final x = i * stepX;
      final normalized = hourlyTraffic[i] / effectiveMax;
      final y = size.height - (normalized * (size.height - 15)) - 8;

      if (i == 0) {
        path.moveTo(x, y);
        fillPath.moveTo(x, size.height);
        fillPath.lineTo(x, y);
      } else {
        path.lineTo(x, y);
        fillPath.lineTo(x, y);
      }
    }

    fillPath.lineTo(size.width, size.height);
    fillPath.close();

    canvas.drawPath(fillPath, paintFill);
    canvas.drawPath(path, paintLine);

    // Draw points for non-zero hours
    final dotPaint = Paint()..color = const Color(0xFF00E5FF);
    for (int i = 0; i < hourlyTraffic.length; i++) {
      if (hourlyTraffic[i] > 0) {
        final x = i * stepX;
        final normalized = hourlyTraffic[i] / effectiveMax;
        final y = size.height - (normalized * (size.height - 15)) - 8;
        canvas.drawCircle(Offset(x, y), 3.5, dotPaint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant HourlyChartPainter oldDelegate) => true;
}
