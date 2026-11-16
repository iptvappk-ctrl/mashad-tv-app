import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:http/http.dart' as http;

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ZaqzoukaAdminApp());
}

class ZaqzoukaAdminApp extends StatefulWidget {
  const ZaqzoukaAdminApp({super.key});

  @override
  State<ZaqzoukaAdminApp> createState() => _ZaqzoukaAdminAppState();
}

class _ZaqzoukaAdminAppState extends State<ZaqzoukaAdminApp> {
  // Theme Mode: Light by default as requested by the user, with one-tap toggle!
  bool _isLightMode = true;

  void _toggleTheme() {
    setState(() {
      _isLightMode = !_isLightMode;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'تحليلات زقزوقة TV',
      debugShowCheckedModeBanner: false,
      themeMode: _isLightMode ? ThemeMode.light : ThemeMode.dark,
      theme: ThemeData(
        brightness: Brightness.light,
        scaffoldBackgroundColor: const Color(0xFFF8FAFC), // Ultra-clean executive slate white
        primaryColor: const Color(0xFF4F46E5), // Indigo
        colorScheme: const ColorScheme.light(
          primary: Color(0xFF4F46E5),
          secondary: Color(0xFF0284C7),
          surface: Colors.white,
          error: Color(0xFFEF4444),
        ),
        cardColor: Colors.white,
        fontFamily: 'Roboto',
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.white,
          foregroundColor: Color(0xFF0F172A),
          elevation: 0.5,
        ),
      ),
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0B0E14),
        primaryColor: const Color(0xFF818CF8),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF818CF8),
          secondary: Color(0xFF38BDF8),
          surface: Color(0xFF161B26),
          error: Color(0xFFF87171),
        ),
        cardColor: const Color(0xFF161B26),
        fontFamily: 'Roboto',
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF111622),
          foregroundColor: Colors.white,
          elevation: 0.5,
        ),
      ),
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [Locale('ar', 'EG')],
      locale: const Locale('ar', 'EG'),
      home: DashboardHomeScreen(
        isLightMode: _isLightMode,
        onToggleTheme: _toggleTheme,
      ),
    );
  }
}

class DashboardHomeScreen extends StatefulWidget {
  final bool isLightMode;
  final VoidCallback onToggleTheme;

  const DashboardHomeScreen({
    super.key,
    required this.isLightMode,
    required this.onToggleTheme,
  });

  @override
  State<DashboardHomeScreen> createState() => _DashboardHomeScreenState();
}

class _DashboardHomeScreenState extends State<DashboardHomeScreen>
    with SingleTickerProviderStateMixin {
  String _apiBaseUrl = 'https://zaqzouka-dashboard-api.vercel.app';
  String _adminSecret = 'zaqzouka_secret_2026';

  bool _isLoading = true;
  int _autoRefreshSeconds = 15; // 0 = off, 5, 15, 30
  String? _errorMessage;
  DateTime? _lastUpdated;
  Timer? _refreshTimer;
  int? _cloudLatencyMs;

  // Filter States
  String _sessionSearchQuery = '';
  String _sessionFilterStatus = 'all'; // 'all', 'active', 'closed'
  String _logFilterType = 'all'; // 'all', 'play', 'search', 'install'

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
    _tabController = TabController(length: 7, vsync: this);
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
    if (_autoRefreshSeconds > 0) {
      _refreshTimer = Timer.periodic(Duration(seconds: _autoRefreshSeconds), (_) {
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

    final stopwatch = Stopwatch()..start();
    try {
      final url = Uri.parse('$_apiBaseUrl/api/stats');
      final res = await http.get(
        url,
        headers: {
          'x-admin-secret': _adminSecret,
          'Accept': 'application/json',
        },
      ).timeout(const Duration(seconds: 12));
      stopwatch.stop();

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
            _cloudLatencyMs = stopwatch.elapsedMilliseconds;
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

  // Color & Theme Helpers
  Color get _bg => widget.isLightMode ? const Color(0xFFF8FAFC) : const Color(0xFF0B0E14);
  Color get _cardBg => widget.isLightMode ? Colors.white : const Color(0xFF161B26);
  Color get _border => widget.isLightMode ? const Color(0xFFE2E8F0) : const Color(0xFF262D3D);
  Color get _textPrimary => widget.isLightMode ? const Color(0xFF0F172A) : const Color(0xFFF8FAFC);
  Color get _textMuted => widget.isLightMode ? const Color(0xFF64748B) : const Color(0xFF94A3B8);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bg,
      appBar: AppBar(
        titleSpacing: 16,
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF4F46E5), Color(0xFF7C3AED)],
                ),
                borderRadius: BorderRadius.circular(8),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF4F46E5).withValues(alpha: 0.3),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: const Text(
                'ZQ',
                style: TextStyle(
                  fontWeight: FontWeight.w900,
                  color: Colors.white,
                  fontSize: 16,
                  letterSpacing: 1,
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'لوحة تحليلات زقزوقة TV',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w800,
                      color: _textPrimary,
                    ),
                  ),
                  Row(
                    children: [
                      Container(
                        width: 7,
                        height: 7,
                        decoration: BoxDecoration(
                          color: _errorMessage == null
                              ? const Color(0xFF10B981)
                              : const Color(0xFFEF4444),
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 5),
                      Text(
                        _errorMessage == null
                            ? 'متصل بالسحابة · ${_cloudLatencyMs != null ? "$_cloudLatencyMs ms" : "مستقر"}'
                            : 'فشل الاتصال',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w500,
                          color: _errorMessage == null
                              ? const Color(0xFF10B981)
                              : const Color(0xFFEF4444),
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
          // Theme Toggle Button (فاتح / ليلي)
          IconButton(
            tooltip: widget.isLightMode ? 'التبديل إلى الوضع الليلي' : 'التبديل إلى الوضع الفاتح',
            icon: Icon(
              widget.isLightMode ? Icons.dark_mode_outlined : Icons.light_mode_outlined,
              color: widget.isLightMode ? const Color(0xFF4F46E5) : const Color(0xFFFBBF24),
            ),
            onPressed: widget.onToggleTheme,
          ),
          // Refresh Button
          IconButton(
            tooltip: 'تحديث فوري',
            icon: Icon(Icons.refresh_rounded, color: widget.isLightMode ? const Color(0xFF0284C7) : const Color(0xFF38BDF8)),
            onPressed: () => _fetchStats(),
          ),
          // Settings Dialog
          IconButton(
            tooltip: 'إعدادات الربط',
            icon: Icon(Icons.tune_rounded, color: _textMuted),
            onPressed: _showSettingsDialog,
          ),
          const SizedBox(width: 4),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: const Color(0xFF4F46E5),
          indicatorWeight: 3,
          labelColor: const Color(0xFF4F46E5),
          unselectedLabelColor: _textMuted,
          labelStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13),
          unselectedLabelStyle: const TextStyle(fontWeight: FontWeight.w500, fontSize: 13),
          isScrollable: true,
          tabAlignment: TabAlignment.start,
          tabs: const [
            Tab(icon: Icon(Icons.radar_rounded), text: 'الرادار والملخص'),
            Tab(icon: Icon(Icons.devices_rounded), text: 'الجلسات الحية'),
            Tab(icon: Icon(Icons.language_rounded), text: 'داشبورد الموقع'),
            Tab(icon: Icon(Icons.local_fire_department_rounded), text: 'الأعلى مشاهدة'),
            Tab(icon: Icon(Icons.query_stats_rounded), text: 'رادار البحث'),
            Tab(icon: Icon(Icons.analytics_outlined), text: 'الرسوم البيانية'),
            Tab(icon: Icon(Icons.admin_panel_settings_rounded), text: 'أدوات الإدارة'),
          ],
        ),
      ),
      body: _isLoading && _sessions.isEmpty
          ? Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const CircularProgressIndicator(color: Color(0xFF4F46E5)),
                  const SizedBox(height: 16),
                  Text(
                    'جاري تحميل تحليلات زقزوقة السحابية...',
                    style: TextStyle(color: _textMuted, fontSize: 14),
                  ),
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
                        const Icon(Icons.cloud_off_rounded, size: 64, color: Color(0xFFEF4444)),
                        const SizedBox(height: 16),
                        Text(
                          'تعذر جلب البيانات:\n$_errorMessage',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: _textPrimary, fontSize: 14),
                        ),
                        const SizedBox(height: 20),
                        ElevatedButton.icon(
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFF4F46E5),
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
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
                    _buildWebsiteDashboardTab(),
                    _buildTopContentTab(),
                    _buildSearchRadarTab(),
                    _buildChartsTab(),
                    _buildAdminToolkitTab(),
                  ],
                ),
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 1: EXECUTIVE OVERVIEW & RADAR
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildOverviewTab() {
    final activeNow = _kpis['activeNow'] ?? 0;
    final totalOpens = _kpis['firstOpens'] ?? 0;
    final totalInstalls = _kpis['totalInstalls'] ?? 0;
    final avgDuration = _kpis['avgDurationMinutes'] ?? '0';
    final streamHealth = _kpis['streamHealth'] ?? '100.0';
    final bounceRate = _kpis['bounceRate'] ?? 0;
    final tvRatio = _kpis['tvRatio'] ?? 0;
    final phoneRatio = _kpis['phoneRatio'] ?? (_sessions.isNotEmpty ? 100 : 0);
    final totalSearches = _kpis['totalSearches'] ?? 0;
    final totalPlays = _kpis['totalPlays'] ?? 0;

    return RefreshIndicator(
      color: const Color(0xFF4F46E5),
      backgroundColor: _cardBg,
      onRefresh: () => _fetchStats(),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Hero Live Pulse Radar Card
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: _cardBg,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: widget.isLightMode ? const Color(0xFFE2E8F0) : const Color(0xFF312E81).withValues(alpha: 0.5),
                width: 1.2,
              ),
              boxShadow: [
                BoxShadow(
                  color: widget.isLightMode
                      ? Colors.black.withValues(alpha: 0.04)
                      : const Color(0xFF4F46E5).withValues(alpha: 0.15),
                  blurRadius: 18,
                  offset: const Offset(0, 6),
                ),
              ],
            ),
            child: Column(
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: [
                            const Color(0xFF10B981).withValues(alpha: 0.2),
                            const Color(0xFF059669).withValues(alpha: 0.1),
                          ],
                        ),
                        shape: BoxShape.circle,
                        border: Border.all(color: const Color(0xFF10B981), width: 2),
                      ),
                      child: const Icon(Icons.sensors_rounded, color: Color(0xFF10B981), size: 30),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Text(
                                'المتصلون الآن بالتطبيق',
                                style: TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.w600,
                                  color: _textMuted,
                                ),
                              ),
                              const Spacer(),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                decoration: BoxDecoration(
                                  color: const Color(0xFF10B981).withValues(alpha: 0.12),
                                  borderRadius: BorderRadius.circular(20),
                                ),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Container(
                                      width: 6,
                                      height: 6,
                                      decoration: const BoxDecoration(
                                        color: Color(0xFF10B981),
                                        shape: BoxShape.circle,
                                      ),
                                    ),
                                    const SizedBox(width: 5),
                                    const Text(
                                      'رادار مباشر',
                                      style: TextStyle(
                                        color: Color(0xFF10B981),
                                        fontSize: 11,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.baseline,
                            textBaseline: TextBaseline.alphabetic,
                            children: [
                              Text(
                                '$activeNow',
                                style: TextStyle(
                                  fontSize: 40,
                                  fontWeight: FontWeight.w900,
                                  color: activeNow > 0 ? const Color(0xFF10B981) : _textPrimary,
                                ),
                              ),
                              const SizedBox(width: 8),
                              Text(
                                'مستخدم نشط حاليًا',
                                style: TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w500,
                                  color: _textMuted,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const Divider(height: 24),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.schedule_rounded, size: 14, color: _textMuted),
                        const SizedBox(width: 4),
                        Text(
                          _lastUpdated != null
                              ? 'آخر تحديث: ${_lastUpdated!.hour.toString().padLeft(2, '0')}:${_lastUpdated!.minute.toString().padLeft(2, '0')}:${_lastUpdated!.second.toString().padLeft(2, '0')}'
                              : 'جارٍ الفحص...',
                          style: TextStyle(fontSize: 11, color: _textMuted),
                        ),
                      ],
                    ),
                    Row(
                      children: [
                        Text(
                          'تحديث تلقائي:',
                          style: TextStyle(fontSize: 11, color: _textMuted),
                        ),
                        const SizedBox(width: 6),
                        DropdownButton<int>(
                          value: _autoRefreshSeconds,
                          isDense: true,
                          underline: const SizedBox(),
                          dropdownColor: _cardBg,
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: const Color(0xFF4F46E5),
                          ),
                          items: const [
                            DropdownMenuItem(value: 5, child: Text('5 ثوان')),
                            DropdownMenuItem(value: 15, child: Text('15 ثانية')),
                            DropdownMenuItem(value: 30, child: Text('30 ثانية')),
                            DropdownMenuItem(value: 0, child: Text('إيقاف')),
                          ],
                          onChanged: (val) {
                            if (val != null) {
                              setState(() {
                                _autoRefreshSeconds = val;
                              });
                              _startAutoRefresh();
                            }
                          },
                        ),
                      ],
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 8 Executive KPI Cards Grid
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1.35,
            children: [
              _buildModernKpiCard(
                title: 'إجمالي الزيارات والفتح',
                value: '$totalOpens',
                subtitle: 'منها $totalInstalls تثبيت محتفظ به',
                icon: Icons.install_mobile_rounded,
                color: const Color(0xFF0284C7),
              ),
              _buildModernKpiCard(
                title: 'متوسط وقت المشاهدة',
                value: '$avgDuration د',
                subtitle: 'لكل جلسة مكتملة',
                icon: Icons.timer_outlined,
                color: const Color(0xFFD97706),
              ),
              _buildModernKpiCard(
                title: 'صحة واستقرار البث',
                value: '$streamHealth%',
                subtitle: 'بثوث بدون أعطال',
                icon: Icons.verified_rounded,
                color: const Color(0xFF10B981),
              ),
              _buildModernKpiCard(
                title: 'معدل الارتداد',
                value: '$bounceRate%',
                subtitle: 'خروج فوري دون تصفح',
                icon: Icons.warning_amber_rounded,
                color: bounceRate > 50 ? const Color(0xFFEF4444) : const Color(0xFF64748B),
              ),
              _buildModernKpiCard(
                title: 'شاشات TV مقابل هواتف',
                value: 'TV: $tvRatio%',
                subtitle: 'هواتف: $phoneRatio%',
                icon: Icons.tv_rounded,
                color: const Color(0xFF7C3AED),
              ),
              _buildModernKpiCard(
                title: 'عمليات التشغيل المكتملة',
                value: '$totalPlays',
                subtitle: 'مرات تشغيل فيديو ناجحة',
                icon: Icons.play_circle_fill_rounded,
                color: const Color(0xFFEC4899),
              ),
              _buildModernKpiCard(
                title: 'رادار البحث والتنقيب',
                value: '$totalSearches',
                subtitle: 'عمليات بحث مستخدمين',
                icon: Icons.search_rounded,
                color: const Color(0xFF6366F1),
              ),
              _buildModernKpiCard(
                title: 'سرعة استجابة السحابة',
                value: _cloudLatencyMs != null ? '$_cloudLatencyMs ms' : 'سريعة',
                subtitle: 'Vercel / Firebase API',
                icon: Icons.bolt_rounded,
                color: const Color(0xFF14B8A6),
              ),
            ],
          ),

          const SizedBox(height: 20),

          // Quick Action Bar
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: _cardBg,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: _border),
            ),
            child: Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF4F46E5),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      elevation: 0,
                    ),
                    onPressed: _exportReportToClipboard,
                    icon: const Icon(Icons.share_rounded, size: 18),
                    label: const Text('تصدير تقرير واتساب / تيليجرام', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton(
                  tooltip: 'فحص اتصال السحابة',
                  icon: const Icon(Icons.network_check_rounded, color: Color(0xFF0284C7)),
                  onPressed: () => _fetchStats(),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 2: LIVE SESSIONS & DEVICE INSPECTOR
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildSessionsTab() {
    // Filter sessions based on search query and status filter
    final filtered = _sessions.where((item) {
      final s = item as Map<String, dynamic>;
      final device = (s['device'] ?? '').toString().toLowerCase();
      final country = (s['country'] ?? '').toString().toLowerCase();
      final content = (s['lastContent'] ?? '').toString().toLowerCase();
      final q = _sessionSearchQuery.toLowerCase().trim();

      final matchesQuery = q.isEmpty || device.contains(q) || country.contains(q) || content.contains(q);

      final status = s['status'] ?? '';
      if (_sessionFilterStatus == 'active' && status != 'active') return false;
      if (_sessionFilterStatus == 'closed' && status == 'active') return false;

      return matchesQuery;
    }).toList();

    return RefreshIndicator(
      color: const Color(0xFF4F46E5),
      backgroundColor: _cardBg,
      onRefresh: () => _fetchStats(),
      child: Column(
        children: [
          // Filter Bar
          Container(
            padding: const EdgeInsets.all(12),
            color: _cardBg,
            child: Column(
              children: [
                TextField(
                  style: TextStyle(color: _textPrimary, fontSize: 13),
                  decoration: InputDecoration(
                    hintText: 'ابحث عن جهاز أو دولة أو اسم فيلم...',
                    hintStyle: TextStyle(color: _textMuted, fontSize: 13),
                    prefixIcon: Icon(Icons.search, size: 20, color: _textMuted),
                    filled: true,
                    fillColor: widget.isLightMode ? const Color(0xFFF1F5F9) : const Color(0xFF1E2433),
                    contentPadding: const EdgeInsets.symmetric(vertical: 10, horizontal: 16),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide.none,
                    ),
                  ),
                  onChanged: (val) {
                    setState(() {
                      _sessionSearchQuery = val;
                    });
                  },
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    _buildFilterChip('الكل (${_sessions.length})', 'all'),
                    const SizedBox(width: 6),
                    _buildFilterChip('🟢 نشط الآن', 'active'),
                    const SizedBox(width: 6),
                    _buildFilterChip('⚪ منتهية', 'closed'),
                  ],
                ),
              ],
            ),
          ),
          const Divider(height: 1),

          Expanded(
            child: filtered.isEmpty
                ? Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.device_unknown_rounded, size: 48, color: _textMuted),
                        const SizedBox(height: 12),
                        Text('لا توجد جلسات تطابق البحث', style: TextStyle(color: _textMuted)),
                      ],
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: filtered.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final s = filtered[index] as Map<String, dynamic>;
                      final isActive = s['status'] == 'active';
                      final device = s['device'] ?? 'Android Device';
                      final country = s['country'] ?? 'مصر (EG)';
                      final openTime = s['openTime'] ?? '';
                      final duration = s['durationText'] ?? '1 دقيقة';
                      final section = s['lastSection'] ?? 'الرئيسية';
                      final content = s['lastContent'] ?? '';
                      final osVersion = s['osVersion'] ?? '';
                      final actionsCount = s['actionsCount'] ?? 0;
                      final isTv = s['deviceType'] == 'tv';

                      return Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: _cardBg,
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(
                            color: isActive ? const Color(0xFF10B981) : _border,
                            width: isActive ? 1.5 : 1.0,
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withValues(alpha: widget.isLightMode ? 0.03 : 0.2),
                              blurRadius: 10,
                              offset: const Offset(0, 3),
                            ),
                          ],
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color: (isTv ? const Color(0xFF7C3AED) : const Color(0xFF0284C7)).withValues(alpha: 0.12),
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: Icon(
                                    isTv ? Icons.tv_rounded : Icons.phone_android_rounded,
                                    color: isTv ? const Color(0xFF7C3AED) : const Color(0xFF0284C7),
                                    size: 20,
                                  ),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        device,
                                        style: TextStyle(
                                          fontWeight: FontWeight.w700,
                                          fontSize: 14,
                                          color: _textPrimary,
                                        ),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                      Text(
                                        osVersion.isNotEmpty ? osVersion : 'نظام أندرويد',
                                        style: TextStyle(fontSize: 11, color: _textMuted),
                                      ),
                                    ],
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: isActive
                                        ? const Color(0xFF10B981).withValues(alpha: 0.12)
                                        : Colors.grey.withValues(alpha: 0.12),
                                    borderRadius: BorderRadius.circular(20),
                                  ),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Container(
                                        width: 6,
                                        height: 6,
                                        decoration: BoxDecoration(
                                          color: isActive ? const Color(0xFF10B981) : Colors.grey,
                                          shape: BoxShape.circle,
                                        ),
                                      ),
                                      const SizedBox(width: 5),
                                      Text(
                                        isActive ? 'نشط الآن' : 'منتهية',
                                        style: TextStyle(
                                          color: isActive ? const Color(0xFF10B981) : _textMuted,
                                          fontSize: 11,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 12),
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFF0284C7).withValues(alpha: 0.08),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Text(
                                    '🌍 $country',
                                    style: const TextStyle(color: Color(0xFF0284C7), fontSize: 11, fontWeight: FontWeight.w600),
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                  decoration: BoxDecoration(
                                    color: widget.isLightMode ? const Color(0xFFF1F5F9) : const Color(0xFF1E2433),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Text(
                                    '⚡ $actionsCount تفاعل',
                                    style: TextStyle(color: _textMuted, fontSize: 11),
                                  ),
                                ),
                                const Spacer(),
                                Text(
                                  '⏱️ $duration ($openTime)',
                                  style: TextStyle(color: _textMuted, fontSize: 11),
                                ),
                              ],
                            ),
                            if (content.isNotEmpty || section.isNotEmpty) ...[
                              const SizedBox(height: 10),
                              Container(
                                padding: const EdgeInsets.all(10),
                                width: double.infinity,
                                decoration: BoxDecoration(
                                  color: widget.isLightMode ? const Color(0xFFF8FAFC) : const Color(0xFF111622),
                                  borderRadius: BorderRadius.circular(10),
                                  border: Border.all(color: _border),
                                ),
                                child: Row(
                                  children: [
                                    const Icon(Icons.play_circle_outline_rounded, size: 16, color: Color(0xFF4F46E5)),
                                    const SizedBox(width: 6),
                                    Text(
                                      'القسم: $section',
                                      style: TextStyle(color: _textMuted, fontSize: 11, fontWeight: FontWeight.bold),
                                    ),
                                    if (content.isNotEmpty) ...[
                                      const SizedBox(width: 6),
                                      Expanded(
                                        child: Text(
                                          '• $content',
                                          style: const TextStyle(
                                            color: Color(0xFFD97706),
                                            fontSize: 11,
                                            fontWeight: FontWeight.bold,
                                          ),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ),
                                    ],
                                  ],
                                ),
                              ),
                            ],
                          ],
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterChip(String label, String status) {
    final isSelected = _sessionFilterStatus == status;
    return InkWell(
      onTap: () {
        setState(() {
          _sessionFilterStatus = status;
        });
      },
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: isSelected
              ? const Color(0xFF4F46E5)
              : (widget.isLightMode ? const Color(0xFFF1F5F9) : const Color(0xFF1E2433)),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
            color: isSelected ? Colors.white : _textMuted,
          ),
        ),
      ),
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 3: DEDICATED WEBSITE & WEB PORTAL DASHBOARD
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildWebsiteDashboardTab() {
    final webSessions = _sessions.where((s) {
      final map = s as Map<String, dynamic>;
      return map['platform'] == 'web' || map['deviceType'] == 'web';
    }).toList();

    final webActive = webSessions.where((s) => (s as Map<String, dynamic>)['status'] == 'active').length;
    final webVisits = _kpis['webVisits'] ?? webSessions.length;
    final webRatio = _kpis['webRatio'] ?? 0;
    final appRatio = _kpis['appRatio'] ?? (_sessions.isNotEmpty ? 100 : 0);
    final webBrowsers = (_analytics['webBrowsers'] as List<dynamic>?) ?? [];

    return RefreshIndicator(
      color: const Color(0xFF0284C7),
      backgroundColor: _cardBg,
      onRefresh: () => _fetchStats(),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 1. Web Portal Status Card
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: _cardBg,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: widget.isLightMode ? const Color(0xFFE2E8F0) : const Color(0xFF0369A1).withValues(alpha: 0.4),
                width: 1.2,
              ),
              boxShadow: [
                BoxShadow(
                  color: widget.isLightMode
                      ? Colors.black.withValues(alpha: 0.04)
                      : Colors.black.withValues(alpha: 0.3),
                  blurRadius: 14,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: const Color(0xFF0284C7).withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Icon(Icons.language_rounded, color: Color(0xFF0284C7), size: 24),
                        ),
                        const SizedBox(width: 12),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'داشبورد الموقع الإلكتروني (Web Portal)',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w800,
                                color: _textPrimary,
                              ),
                            ),
                            Text(
                              'متصل بالسحابة · مراقبة حية لزوار الويب والمتصفحات',
                              style: TextStyle(fontSize: 11, color: _textMuted),
                            ),
                          ],
                        ),
                      ],
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: const Color(0xFF10B981).withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: const Color(0xFF10B981).withValues(alpha: 0.3)),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Container(
                            width: 7,
                            height: 7,
                            decoration: const BoxDecoration(
                              color: Color(0xFF10B981),
                              shape: BoxShape.circle,
                            ),
                          ),
                          const SizedBox(width: 5),
                          Text(
                            '$webActive نشط الآن',
                            style: const TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF10B981),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: widget.isLightMode ? const Color(0xFFF1F5F9) : const Color(0xFF0F172A),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.link_rounded, size: 16, color: Color(0xFF0284C7)),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          'https://madartv-updates.web.app',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: widget.isLightMode ? const Color(0xFF0369A1) : const Color(0xFF38BDF8),
                            fontFamily: 'monospace',
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: const Color(0xFF10B981).withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: const Text(
                          'Online 200 OK',
                          style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Color(0xFF10B981)),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 2. Web Metrics Grid (4 Cards)
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1.35,
            children: [
              _buildModernKpiCard(
                title: 'زوار الموقع النشطون',
                value: '$webActive',
                subtitle: 'يتصفحون الموقع الآن',
                icon: Icons.person_pin_circle_rounded,
                color: const Color(0xFF10B981),
              ),
              _buildModernKpiCard(
                title: 'إجمالي زيارات الموقع',
                value: '$webVisits',
                subtitle: 'جلسات تصفح الويب',
                icon: Icons.public_rounded,
                color: const Color(0xFF0284C7),
              ),
              _buildModernKpiCard(
                title: 'حصة حركة الموقع',
                value: '$webRatio%',
                subtitle: 'من إجمالي زوار المنظومة',
                icon: Icons.pie_chart_rounded,
                color: const Color(0xFF8B5CF6),
              ),
              _buildModernKpiCard(
                title: 'أنواع المتصفحات',
                value: '${webBrowsers.length}',
                subtitle: webBrowsers.isEmpty ? 'لا توجد متصفحات مسجلة' : 'الأنواع النشطة',
                icon: Icons.web_stories_rounded,
                color: const Color(0xFFF59E0B),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // 3. Traffic Ratio: App vs Web Progress Bar
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _cardBg,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: _border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'مقارنة توزيع الزوار: التطبيق مقابل الموقع الإلكتروني',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: _textPrimary,
                  ),
                ),
                const SizedBox(height: 14),
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: SizedBox(
                    height: 16,
                    child: Row(
                      children: [
                        Expanded(
                          flex: (appRatio as num).toInt() > 0 ? (appRatio as num).toInt() : 1,
                          child: Container(color: const Color(0xFF4F46E5)),
                        ),
                        Expanded(
                          flex: (webRatio as num).toInt() > 0 ? (webRatio as num).toInt() : (appRatio > 0 ? 0 : 1),
                          child: Container(color: const Color(0xFF0284C7)),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 10,
                          height: 10,
                          decoration: BoxDecoration(
                            color: const Color(0xFF4F46E5),
                            borderRadius: BorderRadius.circular(3),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Text('تطبيق الموبايل والشاشات ($appRatio%)', style: TextStyle(fontSize: 12, color: _textMuted)),
                      ],
                    ),
                    Row(
                      children: [
                        Container(
                          width: 10,
                          height: 10,
                          decoration: BoxDecoration(
                            color: const Color(0xFF0284C7),
                            borderRadius: BorderRadius.circular(3),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Text('الموقع الإلكتروني ($webRatio%)', style: TextStyle(fontSize: 12, color: _textMuted)),
                      ],
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 4. Browsers Breakdown
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _cardBg,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: _border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'توزيع متصفحات زوار الموقع الإلكتروني',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: _textPrimary),
                ),
                const SizedBox(height: 14),
                if (webBrowsers.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 20),
                    child: Center(
                      child: Text(
                        'لا توجد بيانات متصفحات مسجلة حتى الآن\n(يتم الرصد لحظياً من السحابة مع أول زيارة حقيقية للموقع)',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 12, color: _textMuted),
                      ),
                    ),
                  )
                else
                  ...webBrowsers.map((b) {
                    final name = (b['browser'] ?? 'متصفح غير معروف').toString();
                    final count = b['count'] ?? 0;
                    final pct = (b['percent'] ?? 0) as num;
                    return _buildBrowserRow(name, pct.toInt(), '$count زيارة', const Color(0xFF0284C7));
                  }),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 5. Recent Web Sessions List
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _cardBg,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: _border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'أحدث جلسات زوار الموقع (${webSessions.length})',
                      style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: _textPrimary),
                    ),
                    Text('تحديث لحظي', style: TextStyle(fontSize: 11, color: _textMuted)),
                  ],
                ),
                const SizedBox(height: 12),
                if (webSessions.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 24),
                    child: Center(
                      child: Column(
                        children: [
                          Icon(Icons.public_rounded, size: 40, color: _textMuted.withValues(alpha: 0.5)),
                          const SizedBox(height: 8),
                          Text(
                            'الموقع جاهز لاستقبال الزوار ومربوط بالسحابة\n(تظهر جلسات الويب هنا تلقائياً فور دخول المستخدمين)',
                            textAlign: TextAlign.center,
                            style: TextStyle(color: _textMuted, fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                  )
                else
                  ...webSessions.take(10).map((s) {
                    final session = s as Map<String, dynamic>;
                    final dev = session['device'] ?? 'Chrome (Windows)';
                    final country = session['country'] ?? 'مصر (EG)';
                    final duration = session['durationText'] ?? 'مستمر';
                    final lastContent = session['lastContent'] ?? '';
                    final isActive = session['status'] == 'active';
                    return Container(
                      margin: const EdgeInsets.only(bottom: 8),
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: widget.isLightMode ? const Color(0xFFF8FAFC) : const Color(0xFF131823),
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(color: _border),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            isActive ? Icons.language_rounded : Icons.public_off_rounded,
                            color: isActive ? const Color(0xFF10B981) : _textMuted,
                            size: 20,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  dev,
                                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: _textPrimary),
                                ),
                                Text(
                                  '🌍 $country · ⏱️ $duration${lastContent.isNotEmpty ? ' · 🎬 $lastContent' : ''}',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(fontSize: 11, color: _textMuted),
                                ),
                              ],
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                            decoration: BoxDecoration(
                              color: (isActive ? const Color(0xFF10B981) : Colors.grey).withValues(alpha: 0.12),
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(
                              isActive ? 'نشط' : 'منتهية',
                              style: TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                                color: isActive ? const Color(0xFF10B981) : _textMuted,
                              ),
                            ),
                          ),
                        ],
                      ),
                    );
                  }),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBrowserRow(String name, int pct, String countText, Color barColor) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(name, style: TextStyle(fontSize: 13, color: _textPrimary, fontWeight: FontWeight.w600)),
              Text('$countText ($pct%)', style: TextStyle(fontSize: 12, color: _textMuted, fontWeight: FontWeight.bold)),
            ],
          ),
          const SizedBox(height: 6),
          LinearProgressIndicator(
            value: pct / 100,
            backgroundColor: _border,
            valueColor: AlwaysStoppedAnimation(barColor),
            minHeight: 6,
            borderRadius: BorderRadius.circular(3),
          ),
        ],
      ),
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 4: TOP CONTENT LEADERBOARD
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildTopContentTab() {
    return RefreshIndicator(
      color: const Color(0xFF4F46E5),
      backgroundColor: _cardBg,
      onRefresh: () => _fetchStats(),
      child: _topContent.isEmpty
          ? Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.movie_filter_rounded, size: 48, color: _textMuted),
                  const SizedBox(height: 12),
                  Text('لا توجد مشاهدات مسجلة بعد', style: TextStyle(color: _textMuted)),
                ],
              ),
            )
          : ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: _topContent.length,
              separatorBuilder: (_, __) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final item = _topContent[index] as Map<String, dynamic>;
                final title = item['title'] ?? 'عمل غير مسمى';
                final category = item['category'] ?? 'منوعات';
                final icon = item['categoryIcon'] ?? '🎬';
                final views = item['views'] ?? 0;
                final completionRate = (item['completionRate'] ?? 0).toInt();

                final rankMedal = index == 0
                    ? '🥇'
                    : index == 1
                        ? '🥈'
                        : index == 2
                            ? '🥉'
                            : '#${index + 1}';

                return Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: _cardBg,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: _border),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: widget.isLightMode ? 0.03 : 0.2),
                        blurRadius: 8,
                        offset: const Offset(0, 3),
                      ),
                    ],
                  ),
                  child: Row(
                    children: [
                      Text(rankMedal, style: const TextStyle(fontSize: 22)),
                      const SizedBox(width: 12),
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: const Color(0xFF4F46E5).withValues(alpha: 0.1),
                          shape: BoxShape.circle,
                        ),
                        child: Text(icon, style: const TextStyle(fontSize: 18)),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              title,
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                                color: _textPrimary,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            const SizedBox(height: 4),
                            Row(
                              children: [
                                Text(
                                  category,
                                  style: TextStyle(fontSize: 11, color: _textMuted),
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  '• إكمال المشاهدة: $completionRate%',
                                  style: const TextStyle(fontSize: 11, color: Color(0xFF10B981), fontWeight: FontWeight.w600),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                        decoration: BoxDecoration(
                          color: const Color(0xFF4F46E5).withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          '$views مشاهدة',
                          style: const TextStyle(
                            color: Color(0xFF4F46E5),
                            fontWeight: FontWeight.w800,
                            fontSize: 12,
                          ),
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 4: SEARCH RADAR
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildSearchRadarTab() {
    return RefreshIndicator(
      color: const Color(0xFF4F46E5),
      backgroundColor: _cardBg,
      onRefresh: () => _fetchStats(),
      child: _searches.isEmpty
          ? Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.saved_search_rounded, size: 48, color: _textMuted),
                  const SizedBox(height: 12),
                  Text('لا توجد عمليات بحث مسجلة بعد', style: TextStyle(color: _textMuted)),
                ],
              ),
            )
          : ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: _searches.length,
              separatorBuilder: (_, __) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final s = _searches[index] as Map<String, dynamic>;
                final query = s['query'] ?? '';
                final count = s['count'] ?? 0;
                final isFound = s['status'] == 'available';

                return Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: _cardBg,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: _border),
                  ),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: const Color(0xFF6366F1).withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: const Icon(Icons.search_rounded, color: Color(0xFF6366F1), size: 20),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              query,
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                                color: _textPrimary,
                              ),
                            ),
                            Text(
                              'تم البحث عنها $count مرة',
                              style: TextStyle(fontSize: 11, color: _textMuted),
                            ),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: (isFound ? const Color(0xFF10B981) : const Color(0xFFEF4444)).withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Text(
                          isFound ? 'متوفر الآن' : 'مطلوب بقوة 🔥',
                          style: TextStyle(
                            color: isFound ? const Color(0xFF10B981) : const Color(0xFFEF4444),
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 5: CHARTS & ANALYTICS
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildChartsTab() {
    final deviceBrands = (_analytics['deviceBrands'] as List<dynamic>?) ?? [];
    final topCountries = (_analytics['topCountries'] as List<dynamic>?) ?? [];
    final hourlyTraffic = (_analytics['hourlyTraffic'] as List<dynamic>?) ?? [];
    final osList = (_analytics['osList'] as List<dynamic>?) ?? [];

    return RefreshIndicator(
      color: const Color(0xFF4F46E5),
      backgroundColor: _cardBg,
      onRefresh: () => _fetchStats(),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 1. Peak Hours Bar Chart
          _buildCardContainer(
            title: '📊 ساعات الذروة والنشاط (على مدار 24 ساعة)',
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: SizedBox(
                height: 140,
                child: CustomPaint(
                  size: const Size(double.infinity, 140),
                  painter: ModernHourlyChartPainter(
                    hourlyTraffic: hourlyTraffic.map((e) => (e as num).toInt()).toList(),
                    barColor: const Color(0xFF4F46E5),
                    peakColor: const Color(0xFF10B981),
                    gridColor: _border,
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 2. Device Brands Breakdown
          _buildCardContainer(
            title: '📱 توزيع موديلات وماركات الأجهزة',
            child: deviceBrands.isEmpty
                ? Padding(
                    padding: const EdgeInsets.all(16),
                    child: Center(
                      child: Text('لا توجد أجهزة مسجلة بعد', style: TextStyle(color: _textMuted)),
                    ),
                  )
                : Column(
                    children: deviceBrands.map((b) {
                      final brand = b['brand'] ?? '';
                      final count = b['count'] ?? 0;
                      final percent = (b['percent'] ?? 0).toDouble();
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(brand, style: TextStyle(color: _textPrimary, fontSize: 13, fontWeight: FontWeight.w600)),
                                Text('$count جهاز ($percent%)', style: const TextStyle(color: Color(0xFF4F46E5), fontSize: 12, fontWeight: FontWeight.bold)),
                              ],
                            ),
                            const SizedBox(height: 6),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(4),
                              child: LinearProgressIndicator(
                                value: percent / 100.0,
                                minHeight: 7,
                                backgroundColor: widget.isLightMode ? const Color(0xFFE2E8F0) : const Color(0xFF262D3D),
                                valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF4F46E5)),
                              ),
                            ),
                          ],
                        ),
                      );
                    }).toList(),
                  ),
          ),
          const SizedBox(height: 16),

          // 3. Android OS Versions
          if (osList.isNotEmpty) ...[
            _buildCardContainer(
              title: '🤖 إصدارات نظام التشغيل (Android OS)',
              child: Column(
                children: osList.map((o) {
                  final os = o['os'] ?? '';
                  final count = o['count'] ?? 0;
                  final percent = (o['percent'] ?? 0).toDouble();
                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(os, style: TextStyle(color: _textPrimary, fontSize: 13, fontWeight: FontWeight.w600)),
                            Text('$count جهاز ($percent%)', style: const TextStyle(color: Color(0xFF0284C7), fontSize: 12, fontWeight: FontWeight.bold)),
                          ],
                        ),
                        const SizedBox(height: 6),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: percent / 100.0,
                            minHeight: 7,
                            backgroundColor: widget.isLightMode ? const Color(0xFFE2E8F0) : const Color(0xFF262D3D),
                            valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF0284C7)),
                          ),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              ),
            ),
            const SizedBox(height: 16),
          ],

          // 4. Geographic Countries
          _buildCardContainer(
            title: '🌍 التوزيع الجغرافي (أعلى الدول مشاهدة)',
            child: topCountries.isEmpty
                ? Padding(
                    padding: const EdgeInsets.all(16),
                    child: Center(
                      child: Text('لا توجد بيانات دول بعد', style: TextStyle(color: _textMuted)),
                    ),
                  )
                : Column(
                    children: topCountries.map((c) {
                      final name = c['name'] ?? '';
                      final count = c['count'] ?? 0;
                      final percent = (c['percent'] ?? 0).toDouble();
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(name, style: TextStyle(color: _textPrimary, fontSize: 13, fontWeight: FontWeight.w600)),
                                Text('$count جلسة ($percent%)', style: const TextStyle(color: Color(0xFF10B981), fontSize: 12, fontWeight: FontWeight.bold)),
                              ],
                            ),
                            const SizedBox(height: 6),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(4),
                              child: LinearProgressIndicator(
                                value: percent / 100.0,
                                minHeight: 7,
                                backgroundColor: widget.isLightMode ? const Color(0xFFE2E8F0) : const Color(0xFF262D3D),
                                valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF10B981)),
                              ),
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

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 6: ADMIN TOOLKIT & MANAGEMENT
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildAdminToolkitTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // 1. Export Report
        _buildCardContainer(
          title: '📤 تصدير تقرير الإدارة التنفيذي',
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'تجهيز تقرير نصي شامل ومنسق بنقرة واحدة للمشاركة على واتساب وتيليجرام مع مسؤولي التطبيق:',
                  style: TextStyle(color: _textMuted, fontSize: 13),
                ),
                const SizedBox(height: 14),
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF4F46E5),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
                  ),
                  onPressed: _exportReportToClipboard,
                  icon: const Icon(Icons.copy_rounded, size: 18),
                  label: const Text('نسخ التقرير إلى الحافظة 📋', style: TextStyle(fontWeight: FontWeight.bold)),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),

        // 2. Cloud Ping Benchmark
        _buildCardContainer(
          title: '⚡ فحص سرعة واستجابة السيرفر السحابي',
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: const Color(0xFF10B981).withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.speed_rounded, color: Color(0xFF10B981), size: 28),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _cloudLatencyMs != null ? 'زمن الاستجابة: $_cloudLatencyMs ms' : 'لم يتم الفحص بعد',
                        style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: _textPrimary),
                      ),
                      Text('سحابة Vercel + Firebase Firestore', style: TextStyle(fontSize: 11, color: _textMuted)),
                    ],
                  ),
                ),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: widget.isLightMode ? const Color(0xFFF1F5F9) : const Color(0xFF1E2433),
                    foregroundColor: const Color(0xFF4F46E5),
                    elevation: 0,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  ),
                  onPressed: () => _fetchStats(),
                  child: const Text('فحص الآن'),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),

        // 3. Raw JSON Viewer
        _buildCardContainer(
          title: '🔍 مستعرض البيانات الخام (Raw Payload)',
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'استعراض نص الـ JSON الكامل القادم من السحابة لمراجعة أدق تفاصيل الجلسات والـ Device IDs:',
                  style: TextStyle(color: _textMuted, fontSize: 13),
                ),
                const SizedBox(height: 12),
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: widget.isLightMode ? const Color(0xFFF1F5F9) : const Color(0xFF1E2433),
                    foregroundColor: _textPrimary,
                    elevation: 0,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  ),
                  onPressed: _showRawJsonDialog,
                  icon: const Icon(Icons.code_rounded, size: 18),
                  label: const Text('فتح مستعرض الـ JSON'),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),

        // 4. Cloud Settings
        _buildCardContainer(
          title: '⚙️ إعدادات الربط والمفتاح السري',
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('رابط السيرفر السحابي:', style: TextStyle(fontSize: 12, color: _textMuted)),
                      Text(_apiBaseUrl, style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: _textPrimary)),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.edit_rounded, color: Color(0xFF4F46E5)),
                  onPressed: _showSettingsDialog,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // UI WIDGET HELPERS
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildModernKpiCard({
    required String title,
    required String value,
    required String subtitle,
    required IconData icon,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: _cardBg,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _border),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: widget.isLightMode ? 0.02 : 0.15),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.all(7),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(icon, color: color, size: 20),
              ),
              Flexible(
                child: Text(
                  title,
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: _textMuted,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          Text(
            value,
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w900,
              color: _textPrimary,
            ),
          ),
          Text(
            subtitle,
            style: TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.w500,
              color: color,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }

  Widget _buildCardContainer({required String title, required Widget child}) {
    return Container(
      decoration: BoxDecoration(
        color: _cardBg,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _border),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: widget.isLightMode ? 0.02 : 0.15),
            blurRadius: 10,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Text(
              title,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w800,
                color: _textPrimary,
              ),
            ),
          ),
          const Divider(height: 1),
          child,
        ],
      ),
    );
  }

  void _exportReportToClipboard() {
    final activeNow = _kpis['activeNow'] ?? 0;
    final totalOpens = _kpis['firstOpens'] ?? 0;
    final totalInstalls = _kpis['totalInstalls'] ?? 0;
    final avgDuration = _kpis['avgDurationMinutes'] ?? '0';
    final streamHealth = _kpis['streamHealth'] ?? '100.0';
    final totalPlays = _kpis['totalPlays'] ?? 0;
    final totalSearches = _kpis['totalSearches'] ?? 0;

    final nowStr = DateTime.now().toString().split('.')[0];

    final report = '''
📊 *تقرير منظومة تحليلات زقزوقة TV الرسمي*
⏰ التاريخ: $nowStr
🌐 السيرفر: سحابة Vercel + Firebase Firestore
━━━━━━━━━━━━━━━━━━━━
🟢 *المتصلون الآن بالتطبيق:* $activeNow مستخدم نشط
📲 *إجمالي الفتح والزيارات:* $totalOpens زيارة
📦 *التثبيتات المحتفظ بها:* $totalInstalls مستخدم
⏱️ *متوسط مدة الجلسة:* $avgDuration دقيقة
⚡ *صحة واستقرار البث:* $streamHealth% (بدون أعطال)
▶️ *عمليات التشغيل المكتملة:* $totalPlays تشغيل
🔍 *عمليات البحث المنجزة:* $totalSearches عملية بحث
━━━━━━━━━━━━━━━━━━━━
🚀 تم التوليد آلياً من تطبيق زقزوقة أدمن الرسمي
''';

    Clipboard.setData(ClipboardData(text: report));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('✅ تم نسخ التقرير التنفيذي الكامل إلى الحافظة!'),
        backgroundColor: Color(0xFF10B981),
      ),
    );
  }

  void _showRawJsonDialog() {
    final fullData = {
      'kpis': _kpis,
      'analytics': _analytics,
      'sessions': _sessions,
      'topContent': _topContent,
      'searches': _searches,
    };
    final jsonStr = const JsonEncoder.withIndent('  ').convert(fullData);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: _cardBg,
        title: Text('بيانات السحابة الخام (JSON)', style: TextStyle(color: _textPrimary)),
        content: SizedBox(
          width: double.maxFinite,
          height: 400,
          child: SingleChildScrollView(
            child: SelectableText(
              jsonStr,
              style: TextStyle(fontFamily: 'monospace', fontSize: 11, color: _textPrimary),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () {
              Clipboard.setData(ClipboardData(text: jsonStr));
              Navigator.pop(ctx);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('تم نسخ الـ JSON')),
              );
            },
            child: const Text('نسخ الـ JSON'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('إغلاق'),
          ),
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
          backgroundColor: _cardBg,
          title: Text('إعدادات الاتصال بالسحابة', style: TextStyle(color: _textPrimary)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: urlController,
                style: TextStyle(color: _textPrimary),
                decoration: const InputDecoration(
                  labelText: 'رابط السيرفر (API URL)',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: secretController,
                style: TextStyle(color: _textPrimary),
                decoration: const InputDecoration(
                  labelText: 'مفتاح الأدمن السري (Admin Secret)',
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('إلغاء'),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF4F46E5)),
              onPressed: () {
                setState(() {
                  _apiBaseUrl = urlController.text.trim();
                  _adminSecret = secretController.text.trim();
                });
                Navigator.pop(ctx);
                _fetchStats();
              },
              child: const Text('حفظ واتصال', style: TextStyle(color: Colors.white)),
            ),
          ],
        );
      },
    );
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// MODERN HOURLY CHART PAINTER
// ═════════════════════════════════════════════════════════════════════════════
class ModernHourlyChartPainter extends CustomPainter {
  final List<int> hourlyTraffic;
  final Color barColor;
  final Color peakColor;
  final Color gridColor;

  ModernHourlyChartPainter({
    required this.hourlyTraffic,
    required this.barColor,
    required this.peakColor,
    required this.gridColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (hourlyTraffic.isEmpty) return;

    final maxVal = hourlyTraffic.reduce((a, b) => a > b ? a : b);
    final effectiveMax = maxVal == 0 ? 1 : maxVal;

    final barWidth = size.width / 24;
    final barPaint = Paint()..style = PaintingStyle.fill;

    for (int i = 0; i < 24; i++) {
      final val = i < hourlyTraffic.length ? hourlyTraffic[i] : 0;
      final heightRatio = val / effectiveMax.toDouble();
      final barHeight = heightRatio * (size.height - 24);

      final isPeak = val == maxVal && val > 0;
      barPaint.color = isPeak ? peakColor : barColor.withValues(alpha: val > 0 ? 0.75 : 0.15);

      final left = i * barWidth + (barWidth * 0.15);
      final right = (i + 1) * barWidth - (barWidth * 0.15);
      final top = size.height - 20 - barHeight;
      final bottom = size.height - 20;

      final rrect = RRect.fromRectAndRadius(
        Rect.fromLTRB(left, top, right, bottom),
        const Radius.circular(4),
      );
      canvas.drawRRect(rrect, barPaint);

      // Label hours every 4 hours
      if (i % 4 == 0) {
        final textSpan = TextSpan(
          text: '$i',
          style: TextStyle(color: gridColor, fontSize: 10, fontWeight: FontWeight.bold),
        );
        final tp = TextPainter(
          text: textSpan,
          textDirection: TextDirection.ltr,
        )..layout();
        tp.paint(canvas, Offset(left, size.height - 16));
      }
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
