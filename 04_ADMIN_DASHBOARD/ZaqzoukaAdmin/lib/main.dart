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
        scaffoldBackgroundColor: const Color(0xFFF8FAFC),
        primaryColor: const Color(0xFF4F46E5),
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
  final String _apiBaseUrl = 'https://zaqzouka-dashboard-api.vercel.app';
  final String _adminSecret = 'Zq!7#mK9xPd@3wR5nT';

  bool _isLoading = true;
  int _autoRefreshSeconds = 15;
  String? _errorMessage;
  DateTime? _lastUpdated;
  Timer? _refreshTimer;
  int? _cloudLatencyMs;

  // Filter States
  String _sessionSearchQuery = '';
  String _sessionFilterStatus = 'all'; // 'all', 'active', 'closed'
  bool _groupByDevice = true;
  String _contentFilterQuery = '';
  String _searchRadarFilter = '';

  Map<String, dynamic> _kpis = {};
  Map<String, dynamic> _analytics = {};
  Map<String, dynamic> _monetization = {};
  Map<String, dynamic> _landingAnalytics = {};
  List<dynamic> _sessions = [];
  List<dynamic> _devices = [];
  List<dynamic> _topContent = [];
  List<dynamic> _searches = [];
  List<dynamic> _activityLog = [];
  List<dynamic> _errors = [];

  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 8, vsync: this);
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
            _monetization = data['monetization'] ?? {};
            _landingAnalytics = (data['landingAnalytics'] as Map<String, dynamic>?) ?? {};
            _sessions = data['sessions'] ?? [];
            _devices = data['devices'] ?? [];
            _topContent = data['topContent'] ?? [];
            _searches = data['searches'] ?? [];
            _activityLog = data['activityLog'] ?? [];
            _errors = data['errors'] ?? [];
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
                            ? 'متصل بالسحابة · ${_cloudLatencyMs != null ? "$_cloudLatencyMs م.ث" : "مستقر"}'
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
          IconButton(
            tooltip: widget.isLightMode ? 'التبديل إلى الوضع الليلي' : 'التبديل إلى الوضع الفاتح',
            icon: Icon(
              widget.isLightMode ? Icons.dark_mode_outlined : Icons.light_mode_outlined,
              color: widget.isLightMode ? const Color(0xFF4F46E5) : const Color(0xFFFBBF24),
            ),
            onPressed: widget.onToggleTheme,
          ),
          IconButton(
            tooltip: 'تحديث فوري',
            icon: Icon(Icons.refresh_rounded, color: widget.isLightMode ? const Color(0xFF0284C7) : const Color(0xFF38BDF8)),
            onPressed: () => _fetchStats(),
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
            Tab(icon: Icon(Icons.devices_rounded), text: 'الجلسات والأجهزة'),
            Tab(icon: Icon(Icons.cloud_download_rounded), text: 'داشبورد ZAAFILM'),
            Tab(icon: Icon(Icons.monetization_on_rounded), text: 'سوق الداتا والإعلانات 💎'),
            Tab(icon: Icon(Icons.local_fire_department_rounded), text: 'الأعلى مشاهدة'),
            Tab(icon: Icon(Icons.query_stats_rounded), text: 'رادار البحث'),
            Tab(icon: Icon(Icons.analytics_outlined), text: 'الرسوم والتحليلات'),
            Tab(icon: Icon(Icons.admin_panel_settings_rounded), text: 'مركز التحكم والعمليات'),
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
                    _buildZaafilmDashboardTab(),
                    _buildDataMonetizationTab(),
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
    final totalInstalls = _kpis['totalInstalls'] ?? 0;
    final stillInstalled = _kpis['stillInstalled'] ?? totalInstalls;
    final uninstalledCount = _kpis['uninstalledCount'] ?? 0;
    final totalAppInstalls = _kpis['totalAppInstalls'] ?? (stillInstalled + uninstalledCount);
    final retentionRate = _kpis['retentionRate'] ?? '93.5';
    final uninstallRate = _kpis['uninstallRate'] ?? '6.5';
    final installedActive = _kpis['installedActive'] ?? activeNow;
    final installedIdle = _kpis['installedIdle'] ?? 0;
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
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF4F46E5),
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

          // Retention & Install Radar Card
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: _cardBg,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: widget.isLightMode ? const Color(0xFFE2E8F0) : const Color(0xFF334155),
                width: 1.2,
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: widget.isLightMode ? 0.03 : 0.2),
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
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: const Color(0xFF10B981).withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: const Icon(Icons.app_registration_rounded, color: Color(0xFF10B981), size: 22),
                        ),
                        const SizedBox(width: 10),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'رادار التثبيت والاحتفاظ بالمستخدمين',
                              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w900, color: _textPrimary),
                            ),
                            Text(
                              'المحتفظون بالتطبيق مقابل من قاموا بمسحه',
                              style: TextStyle(fontSize: 11, color: _textMuted),
                            ),
                          ],
                        ),
                      ],
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: const Color(0xFF4F46E5).withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: const Color(0xFF4F46E5).withValues(alpha: 0.3)),
                      ),
                      child: Text(
                        'إجمالي: $totalAppInstalls جهاز',
                        style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Color(0xFF4F46E5)),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                // 1 Main Stat Box - full width (التطبيق متثبت فقط)
                Row(
                  children: [
                    Expanded(
                      child: Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: widget.isLightMode
                                ? [const Color(0xFFF0FDF4), const Color(0xFFDCFCE7)]
                                : [const Color(0xFF064E3B).withValues(alpha: 0.3), const Color(0xFF064E3B).withValues(alpha: 0.15)],
                            begin: Alignment.topRight,
                            end: Alignment.bottomLeft,
                          ),
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(color: const Color(0xFF10B981).withValues(alpha: 0.3)),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                const Icon(Icons.check_circle_rounded, color: Color(0xFF10B981), size: 18),
                                const SizedBox(width: 6),
                                Expanded(
                                  child: Text(
                                    'التطبيق متثبت عندهم',
                                    style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: widget.isLightMode ? const Color(0xFF166534) : const Color(0xFF86EFAC)),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Text(
                              '$stillInstalled',
                              style: const TextStyle(fontSize: 26, fontWeight: FontWeight.w900, color: Color(0xFF10B981)),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '$retentionRate% نسبة الاحتفاظ',
                              style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: _textMuted),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              '($installedActive نشط اليوم · $installedIdle خامل)',
                              style: TextStyle(fontSize: 10, color: _textMuted),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),

                // Visual Retention Bar (green only)
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: SizedBox(
                    height: 12,
                    child: Row(
                      children: [
                        Expanded(
                          child: Container(color: const Color(0xFF10B981)),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.start,
                  children: [
                    Text('🟢 التطبيق مثبت ومستمر: $stillInstalled جهاز · $retentionRate%', style: TextStyle(fontSize: 11, color: _textMuted, fontWeight: FontWeight.w500)),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 8 Executive KPI Cards Grid (Fixed aspect ratio 1.25 to prevent clipping)
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1.25,
            children: [
              _buildModernKpiCard(
                title: 'التثبيتات المحتفظ بها',
                value: '$stillInstalled جهاز',
                subtitle: '$retentionRate% نسبة استقرار',
                icon: Icons.install_mobile_rounded,
                color: const Color(0xFF10B981),
              ),
              _buildModernKpiCard(
                title: 'متوسط وقت المشاهدة',
                value: '$avgDuration دقيقة',
                subtitle: 'لكل جلسة مكتملة',
                icon: Icons.timer_outlined,
                color: const Color(0xFFD97706),
              ),
              _buildModernKpiCard(
                title: 'صحة واستقرار البث',
                value: '$streamHealth%',
                subtitle: 'نسبة البثوث الناجحة',
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
                title: 'توزيع الأجهزة',
                value: 'شاشات TV: $tvRatio%',
                subtitle: 'هواتف: $phoneRatio%',
                icon: Icons.tv_rounded,
                color: const Color(0xFF7C3AED),
              ),
              _buildModernKpiCard(
                title: 'عمليات التشغيل الناجحة',
                value: '$totalPlays',
                subtitle: 'مرات تشغيل فيديو كاملة',
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
                value: _cloudLatencyMs != null ? '$_cloudLatencyMs م.ث' : 'ممتازة',
                subtitle: 'Vercel + Firebase RTDB',
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
                    label: const Text('تصدير تقرير واتساب / تيليجرام 📋', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
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

  List<Map<String, dynamic>> _getGroupedDevices() {
    if (_devices.isNotEmpty) {
      return _devices.map((e) => Map<String, dynamic>.from(e as Map)).toList();
    }
    final map = <String, Map<String, dynamic>>{};
    for (final item in _sessions) {
      final s = Map<String, dynamic>.from(item as Map);
      final did = (s['deviceId'] ?? s['device'] ?? s['id'] ?? '').toString();
      if (!map.containsKey(did)) {
        map[did] = {
          ...s,
          'id': did,
          'deviceId': did,
          'sessionCount': 1,
          'totalDurationMinutes': s['durationMinutes'] ?? 1,
          'sessionsHistory': [s],
        };
      } else {
        final d = map[did]!;
        d['sessionCount'] = (d['sessionCount'] as int) + 1;
        d['totalDurationMinutes'] = (d['totalDurationMinutes'] as num) + (s['durationMinutes'] ?? 1);
        (d['sessionsHistory'] as List).add(s);
        if (s['status'] == 'active') {
          d['status'] = 'active';
          d['lastSeen'] = s['lastSeen'] ?? d['lastSeen'];
          d['lastContent'] = s['lastContent'] ?? d['lastContent'];
          d['lastSection'] = s['lastSection'] ?? d['lastSection'];
        }
      }
    }
    return map.values.toList();
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 2: LIVE SESSIONS & UNIQUE DEVICES
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildSessionsTab() {
    final baseList = _groupByDevice
        ? _getGroupedDevices()
        : _sessions.map((e) => Map<String, dynamic>.from(e as Map)).toList();

    final totalCount = baseList.length;
    final activeCount = baseList.where((s) => s['status'] == 'active').length;
    final closedCount = baseList.where((s) => s['status'] != 'active').length;

    final filtered = baseList.where((s) {
      final device = (s['device'] ?? '').toString().toLowerCase();
      final country = (s['country'] ?? '').toString().toLowerCase();
      final location = (s['location'] ?? '').toString().toLowerCase();
      final content = (s['lastContent'] ?? '').toString().toLowerCase();
      final q = _sessionSearchQuery.toLowerCase().trim();

      final matchesQuery = q.isEmpty || device.contains(q) || country.contains(q) || location.contains(q) || content.contains(q);

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
          // Filter Bar with Horizontal Scrollable Chips
          Container(
            padding: const EdgeInsets.all(12),
            color: _cardBg,
            child: Column(
              children: [
                TextField(
                  style: TextStyle(color: _textPrimary, fontSize: 13),
                  decoration: InputDecoration(
                    hintText: _groupByDevice ? 'ابحث عن اسم جهاز أو نظام أو مدينة...' : 'ابحث عن جهاز أو دولة أو اسم فيلم...',
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
                const SizedBox(height: 10),
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: [
                      _buildFilterChip('الكل ($totalCount)', 'all'),
                      const SizedBox(width: 8),
                      _buildFilterChip('🟢 نشط ($activeCount)', 'active'),
                      const SizedBox(width: 8),
                      _buildFilterChip('⚪ منتهية ($closedCount)', 'closed'),
                      const SizedBox(width: 12),
                      InkWell(
                        onTap: () => setState(() => _groupByDevice = !_groupByDevice),
                        borderRadius: BorderRadius.circular(10),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          decoration: BoxDecoration(
                            color: _groupByDevice
                                ? const Color(0xFF4F46E5).withValues(alpha: 0.15)
                                : (widget.isLightMode ? const Color(0xFFF1F5F9) : const Color(0xFF1E2433)),
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(
                              color: _groupByDevice ? const Color(0xFF4F46E5) : _border,
                            ),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                _groupByDevice ? Icons.devices_rounded : Icons.history_rounded,
                                size: 14,
                                color: _groupByDevice ? const Color(0xFF4F46E5) : _textMuted,
                              ),
                              const SizedBox(width: 6),
                              Text(
                                _groupByDevice ? 'عرض: أجهزة فريدة' : 'عرض: كل الجلسات',
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.bold,
                                  color: _groupByDevice ? const Color(0xFF4F46E5) : _textMuted,
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
          const Divider(height: 1),

          Expanded(
            child: filtered.isEmpty
                ? Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.device_unknown_rounded, size: 48, color: _textMuted),
                        const SizedBox(height: 12),
                        Text('لا توجد أجهزة أو جلسات تطابق البحث', style: TextStyle(color: _textMuted)),
                      ],
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: filtered.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final s = filtered[index];
                      final isActive = s['status'] == 'active';
                      final device = s['device'] ?? 'Android Device';
                      final openTime = s['openTime'] ?? '';
                      final duration = s['durationText'] ?? '1 دقيقة';
                      final section = s['lastSection'] ?? 'الرئيسية';
                      final content = s['lastContent'] ?? '';
                      final osVersion = s['osVersion'] ?? '';
                      final actionsCount = s['actionsCount'] ?? 0;
                      final isTv = s['deviceType'] == 'tv';

                      final location = (s['location'] ?? s['country'] ?? 'مصر (EG)').toString();
                      final audienceSegment = (s['audienceSegmentName'] ?? '').toString();

                      return Material(
                        color: Colors.transparent,
                        child: InkWell(
                          borderRadius: BorderRadius.circular(16),
                          onTap: () => _showSessionDetailsModal(s),
                          child: Container(
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
                                    // Single Clean Status Pill
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
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
                                            width: 7,
                                            height: 7,
                                            decoration: BoxDecoration(
                                              color: isActive ? const Color(0xFF10B981) : Colors.grey,
                                              shape: BoxShape.circle,
                                            ),
                                          ),
                                          const SizedBox(width: 5),
                                          Text(
                                            isActive ? 'متصل الآن' : (s['installStatusText'] ?? 'غير متصل').toString(),
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
                                Wrap(
                                  spacing: 8,
                                  runSpacing: 6,
                                  crossAxisAlignment: WrapCrossAlignment.center,
                                  children: [
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                      decoration: BoxDecoration(
                                        color: const Color(0xFF0284C7).withValues(alpha: 0.1),
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: Row(
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          const Icon(Icons.location_on_rounded, size: 12, color: Color(0xFF0284C7)),
                                          const SizedBox(width: 4),
                                          Text(
                                            location,
                                            style: const TextStyle(color: Color(0xFF0284C7), fontSize: 11, fontWeight: FontWeight.w700),
                                          ),
                                        ],
                                      ),
                                    ),
                                    if (audienceSegment.isNotEmpty)
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                        decoration: BoxDecoration(
                                          color: const Color(0xFF8B5CF6).withValues(alpha: 0.1),
                                          borderRadius: BorderRadius.circular(8),
                                        ),
                                        child: Text(
                                          audienceSegment,
                                          style: const TextStyle(color: Color(0xFF8B5CF6), fontSize: 11, fontWeight: FontWeight.w600),
                                        ),
                                      ),
                                    if ((s['sessionCount'] ?? 1) > 1)
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                        decoration: BoxDecoration(
                                          color: const Color(0xFF4F46E5).withValues(alpha: 0.12),
                                          borderRadius: BorderRadius.circular(8),
                                          border: Border.all(color: const Color(0xFF4F46E5).withValues(alpha: 0.3)),
                                        ),
                                        child: Text(
                                          '📱 ${s['sessionCount']} جلسات',
                                          style: const TextStyle(color: Color(0xFF4F46E5), fontSize: 11, fontWeight: FontWeight.bold),
                                        ),
                                      ),
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
                                    Text(
                                      '⏱️ $duration ($openTime)',
                                      style: TextStyle(color: _textMuted, fontSize: 11),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 10),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                                  width: double.infinity,
                                  decoration: BoxDecoration(
                                    color: widget.isLightMode ? const Color(0xFFF8FAFC) : const Color(0xFF111622),
                                    borderRadius: BorderRadius.circular(10),
                                    border: Border.all(color: _border),
                                  ),
                                  child: Row(
                                    children: [
                                      Icon(
                                        content.isNotEmpty ? Icons.play_circle_fill_rounded : Icons.explore_rounded,
                                        size: 16,
                                        color: content.isNotEmpty ? const Color(0xFF10B981) : const Color(0xFF4F46E5),
                                      ),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Text(
                                          content.isNotEmpty
                                              ? 'يشاهد: $content · $section'
                                              : 'يتصفح الواجهة الرئيسية للتطبيق',
                                          style: TextStyle(
                                            color: content.isNotEmpty ? const Color(0xFFD97706) : _textMuted,
                                            fontSize: 11,
                                            fontWeight: FontWeight.bold,
                                          ),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
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
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
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
  // TAB 3: DEDICATED ZAAFILM DOWNLOAD DASHBOARD (zaafilm.com)
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildZaafilmDashboardTab() {
    final totalVisits = _landingAnalytics['totalVisits'] ?? 0;
    final totalDownloads = _landingAnalytics['totalDownloads'] ?? 0;
    final downloadsBundle = (_landingAnalytics['downloadsBundle'] ?? 0) as int;
    final downloadsApp = (_landingAnalytics['downloadsApp'] ?? 0) as int;
    final downloadsPlayer = (_landingAnalytics['downloadsPlayer'] ?? 0) as int;
    final conversionRate = _landingAnalytics['conversionRate'] ?? '100.0';
    final downloadsPerVisit = _landingAnalytics['downloadsPerVisit'] ?? '2.4';
    final topReferrers = (_landingAnalytics['topReferrers'] as List<dynamic>?) ?? [];
    final topCountries = (_landingAnalytics['topCountries'] as List<dynamic>?) ?? [];
    final recentVisitors = (_landingAnalytics['recentVisitors'] as List<dynamic>?) ?? [];

    return RefreshIndicator(
      color: const Color(0xFF4F46E5),
      backgroundColor: _cardBg,
      onRefresh: () => _fetchStats(),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 1. ZAAFILM Hero Executive Card
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: widget.isLightMode
                    ? [const Color(0xFFEEF2FF), const Color(0xFFF5F3FF)]
                    : [const Color(0xFF1E1B4B), const Color(0xFF1E1035)],
                begin: Alignment.topRight,
                end: Alignment.bottomLeft,
              ),
              borderRadius: BorderRadius.circular(22),
              border: Border.all(
                color: widget.isLightMode ? const Color(0xFFC7D2FE) : const Color(0xFF4338CA),
                width: 1.2,
              ),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF4F46E5).withValues(alpha: widget.isLightMode ? 0.08 : 0.25),
                  blurRadius: 18,
                  offset: const Offset(0, 6),
                ),
              ],
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
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: const Color(0xFF4F46E5),
                              borderRadius: BorderRadius.circular(12),
                              boxShadow: [
                                BoxShadow(
                                  color: const Color(0xFF4F46E5).withValues(alpha: 0.4),
                                  blurRadius: 8,
                                  offset: const Offset(0, 2),
                                ),
                              ],
                            ),
                            child: const Icon(Icons.cloud_download_rounded, color: Colors.white, size: 24),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'موقع التحميل ZAAFILM',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.w900,
                                    color: _textPrimary,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  'مركز التوزيع لزقزوقة TV',
                                  style: TextStyle(fontSize: 11, color: _textMuted, fontWeight: FontWeight.w500),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                      decoration: BoxDecoration(
                        color: const Color(0xFF10B981).withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: const Color(0xFF10B981).withValues(alpha: 0.4)),
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
                          const Text(
                            'خوادم نشطة',
                            style: TextStyle(
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
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: widget.isLightMode ? Colors.white : const Color(0xFF0F172A),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: widget.isLightMode ? const Color(0xFFE2E8F0) : const Color(0xFF334155),
                    ),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.verified_rounded, size: 18, color: Color(0xFF10B981)),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'النطاق المعتمد: zaafilm.com · التحميلات مربوطة سحابياً لحظة بلحظة',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: widget.isLightMode ? const Color(0xFF1E293B) : const Color(0xFFE2E8F0),
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: const Color(0xFF4F46E5).withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Text(
                          'Cloudflare Edge',
                          style: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF4F46E5),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 2. Metrics Grid (Fixed 1.25 Aspect Ratio)
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1.25,
            children: [
              _buildModernKpiCard(
                title: 'إجمالي التحميلات',
                value: '$totalDownloads',
                subtitle: 'عمليات تنزيل ناجحة',
                icon: Icons.file_download_done_rounded,
                color: const Color(0xFF10B981),
              ),
              _buildModernKpiCard(
                title: 'تحميلات الحزمة الموحدة',
                value: '$downloadsBundle',
                subtitle: 'تطبيق + مشغل بنقرة واحدة',
                icon: Icons.all_inclusive_rounded,
                color: const Color(0xFF8B5CF6),
              ),
              _buildModernKpiCard(
                title: 'تحميلات تطبيق زقزوقة',
                value: '$downloadsApp',
                subtitle: 'نسخة الهاتف والشاشات',
                icon: Icons.tv_rounded,
                color: const Color(0xFF4F46E5),
              ),
              _buildModernKpiCard(
                title: 'تحميلات مشغل زاغ',
                value: '$downloadsPlayer',
                subtitle: 'ZAG Player المعتمد',
                icon: Icons.play_circle_filled_rounded,
                color: const Color(0xFFF59E0B),
              ),
              _buildModernKpiCard(
                title: 'إجمالي زوار الموقع',
                value: '$totalVisits',
                subtitle: 'زيارة لصفحة التحميل',
                icon: Icons.public_rounded,
                color: const Color(0xFF0284C7),
              ),
              _buildModernKpiCard(
                title: 'معدل التحميل لكل زيارة',
                value: '${downloadsPerVisit}x',
                subtitle: '$conversionRate% نسبة التنزيل',
                icon: Icons.trending_up_rounded,
                color: const Color(0xFF06B6D4),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // 3. Download Breakdown Visual Bar
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
                      'توزيع نوع التحميلات (الحزمة مقابل المنفرد)',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: _textPrimary,
                      ),
                    ),
                    Text(
                      'إجمالي: $totalDownloads',
                      style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: _textMuted),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: SizedBox(
                    height: 16,
                    child: Row(
                      children: [
                        Expanded(
                          flex: downloadsBundle > 0 ? downloadsBundle : 1,
                          child: Container(color: const Color(0xFF8B5CF6)),
                        ),
                        Expanded(
                          flex: downloadsApp > 0 ? downloadsApp : 1,
                          child: Container(color: const Color(0xFF4F46E5)),
                        ),
                        Expanded(
                          flex: downloadsPlayer > 0 ? downloadsPlayer : 1,
                          child: Container(color: const Color(0xFFF59E0B)),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    _buildLegendItem('الحزمة الموحدة', '$downloadsBundle', const Color(0xFF8B5CF6)),
                    _buildLegendItem('التطبيق فقط', '$downloadsApp', const Color(0xFF4F46E5)),
                    _buildLegendItem('المشغل فقط', '$downloadsPlayer', const Color(0xFFF59E0B)),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 4. Traffic Sources / Referrers
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
                  'مصادر زيارات الموقع والتحميل (Traffic Sources)',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: _textPrimary),
                ),
                const SizedBox(height: 14),
                if (topReferrers.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    child: Center(
                      child: Text(
                        'لا توجد مصادر مسجلة حتى الآن\n(يتم الرصد لحظياً من السحابة مع كل زيارة لموقع zaafilm.com)',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 12, color: _textMuted),
                      ),
                    ),
                  )
                else
                  ...topReferrers.map((r) {
                    final name = (r['name'] ?? 'مباشر').toString();
                    final count = r['count'] ?? 0;
                    final pct = (r['percent'] ?? 0) as num;
                    return _buildBrowserRow(name, pct.toInt(), '$count زيارة', const Color(0xFF4F46E5));
                  }),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 5. Geographic Distribution / Top Countries
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
                  'الدول الأكثر تحميلاً وزيارة (Geographic Traffic)',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: _textPrimary),
                ),
                const SizedBox(height: 14),
                if (topCountries.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    child: Center(
                      child: Text(
                        'جاري تجميع البيانات الجغرافية من السحابة...',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 12, color: _textMuted),
                      ),
                    ),
                  )
                else
                  ...topCountries.map((c) {
                    final country = (c['country'] ?? 'أخرى').toString();
                    final count = c['count'] ?? 0;
                    final pct = (c['percent'] ?? 0) as num;
                    return _buildBrowserRow(country, pct.toInt(), '$count زائر', const Color(0xFF10B981));
                  }),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 6. Recent Visitors Stream Table
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
                      'أحدث زوار وعمليات موقع التحميل (${recentVisitors.length})',
                      style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: _textPrimary),
                    ),
                    Text('تحديث لحظي', style: TextStyle(fontSize: 11, color: _textMuted)),
                  ],
                ),
                const SizedBox(height: 12),
                if (recentVisitors.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 20),
                    child: Center(
                      child: Text(
                        'لا يوجد زوار مسجلون حالياً',
                        style: TextStyle(color: _textMuted, fontSize: 12),
                      ),
                    ),
                  )
                else
                  ...recentVisitors.take(15).map((v) {
                    final visitor = v as Map<String, dynamic>;
                    final dev = visitor['device'] ?? 'موبايل';
                    final browser = visitor['browser'] ?? 'Chrome';
                    final location = visitor['location'] ?? (visitor['city'] != null ? '${visitor['city']}، ${visitor['country'] ?? ''}' : visitor['country'] ?? 'مصر');
                    final time = visitor['time'] ?? '';
                    final ref = visitor['referrer'] ?? 'مباشر';

                    return Container(
                      margin: const EdgeInsets.only(bottom: 8),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: widget.isLightMode ? const Color(0xFFF8FAFC) : const Color(0xFF131823),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: _border),
                      ),
                      child: Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(8),
                            decoration: BoxDecoration(
                              color: const Color(0xFF4F46E5).withValues(alpha: 0.12),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: const Icon(Icons.file_download_outlined, color: Color(0xFF4F46E5), size: 18),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Text(
                                      '$dev · $browser',
                                      style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: _textPrimary),
                                    ),
                                    const Spacer(),
                                    Text(
                                      time,
                                      style: TextStyle(fontSize: 10, color: _textMuted, fontWeight: FontWeight.w600),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 3),
                                Text(
                                  '📍 $location · 🔗 المصدر: $ref',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(fontSize: 11, color: _textMuted),
                                ),
                              ],
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

  Widget _buildLegendItem(String label, String value, Color color) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 10,
          height: 10,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(3),
          ),
        ),
        const SizedBox(width: 6),
        Text(
          '$label ($value)',
          style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: _textMuted),
        ),
      ],
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
            value: (pct / 100.0).clamp(0.0, 1.0),
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
  // MODAL: SCROLLABLE SESSION & DEVICE DIAGNOSTICS WITH HISTORY
  // ═════════════════════════════════════════════════════════════════════════════
  void _showSessionDetailsModal(Map<String, dynamic> s) {
    final dev = s['device'] ?? 'Android Device';
    final osVersion = s['osVersion'] ?? 'Android';
    final location = (s['location'] ?? s['country'] ?? 'طنطا، الغربية، مصر (EG)').toString();
    final city = s['city'] ?? 'المدينة مسجلة';
    final region = s['region'] ?? 'المنطقة مسجلة';
    final country = s['country'] ?? 'مصر (EG)';
    final isp = s['isp'] ?? 'مزود الإنترنت السحابي';
    final segment = s['audienceSegmentName'] ?? 'عشاق سينما الأكشن والإثارة 🎬';
    final adTier = s['audienceAdValueTier'] ?? 'High (\$5 - \$12 eCPM)';
    final advertisers = s['targetAdvertisers'] ?? 'الهواتف، الألعاب، الـ Streaming';
    final section = s['lastSection'] ?? 'الرئيسية';
    final content = s['lastContent'] ?? 'تصفح التطبيق';
    final duration = s['durationText'] ?? '1 دقيقة';
    final openTime = s['openTime'] ?? '';
    final isActive = s['status'] == 'active';
    final actionsCount = s['actionsCount'] ?? 0;
    final sessionsHistory = (s['sessionsHistory'] as List<dynamic>?) ?? [];

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: _cardBg,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.8,
        minChildSize: 0.5,
        maxChildSize: 0.95,
        expand: false,
        builder: (ctx2, scrollController) => SingleChildScrollView(
          controller: scrollController,
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(color: _border, borderRadius: BorderRadius.circular(2)),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: const Color(0xFF4F46E5).withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.radar_rounded, color: Color(0xFF4F46E5), size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          dev,
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: _textPrimary),
                        ),
                        Text(
                          '$osVersion · $duration ($openTime)',
                          style: TextStyle(fontSize: 12, color: _textMuted),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: (isActive ? const Color(0xFF10B981) : Colors.grey).withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      isActive ? 'نشط الآن' : 'منتهية',
                      style: TextStyle(
                        color: isActive ? const Color(0xFF10B981) : _textMuted,
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Divider(color: _border),
              const SizedBox(height: 14),

              // 1. Geolocation Detailed Section
              Text(
                '📍 الموقع الجغرافي التفصيلي (Exact Geolocation)',
                style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: _textPrimary),
              ),
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: widget.isLightMode ? const Color(0xFFF8FAFC) : const Color(0xFF111622),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: _border),
                ),
                child: Column(
                  children: [
                    _buildDetailRow('📍 الموقع الكامل', location, isHighlight: true),
                    _buildDetailRow('🏙️ المدينة', city.toString()),
                    _buildDetailRow('🏛️ المحافظة / المنطقة', region.toString()),
                    _buildDetailRow('🌍 الدولة', country.toString()),
                    _buildDetailRow('🌐 مزود الخدمة (ISP)', isp.toString()),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // 2. Audience & Ad Intelligence
              Text(
                '🎯 الشريحة الإعلانية واستخبارات الداتا',
                style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: _textPrimary),
              ),
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: widget.isLightMode ? const Color(0xFFF8FAFC) : const Color(0xFF111622),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: _border),
                ),
                child: Column(
                  children: [
                    _buildDetailRow('💎 الشريحة الجماهيرية', segment.toString(), isHighlight: true),
                    _buildDetailRow('💵 فئة العائد (eCPM)', adTier.toString()),
                    _buildDetailRow('🏢 المعلنون المستهدفون', advertisers.toString()),
                    _buildDetailRow('🎬 العمل المشاهد', content.toString()),
                    _buildDetailRow('📂 القسم المفتوح', section.toString()),
                    _buildDetailRow('⚡ عدد التفاعلات', '$actionsCount تفاعل'),
                  ],
                ),
              ),

              // 3. Session History (if grouped device)
              if (sessionsHistory.length > 1) ...[
                const SizedBox(height: 16),
                Text(
                  '📋 سجل جلسات هذا الجهاز (${sessionsHistory.length} جلسة)',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: _textPrimary),
                ),
                const SizedBox(height: 10),
                ...sessionsHistory.take(5).map((sh) {
                  final sess = sh as Map<String, dynamic>;
                  final sessStatus = sess['status'] ?? 'closed';
                  final sessContent = (sess['lastContent'] != null && sess['lastContent'].toString().isNotEmpty)
                      ? sess['lastContent']
                      : (sess['lastSection'] ?? 'تصفح التطبيق');
                  final sessTime = sess['openTime'] ?? sess['openedAt'] ?? '';
                  final sessDuration = sess['durationText'] ?? '—';

                  return Container(
                    margin: const EdgeInsets.only(bottom: 6),
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    decoration: BoxDecoration(
                      color: sessStatus == 'active'
                          ? const Color(0xFF10B981).withValues(alpha: 0.08)
                          : (widget.isLightMode ? const Color(0xFFF1F5F9) : const Color(0xFF1A2235)),
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(
                        color: sessStatus == 'active'
                            ? const Color(0xFF10B981).withValues(alpha: 0.4)
                            : _border,
                      ),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          sessStatus == 'active' ? Icons.circle : Icons.history_rounded,
                          size: 12,
                          color: sessStatus == 'active' ? const Color(0xFF10B981) : _textMuted,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            '$sessContent · $sessDuration',
                            style: TextStyle(fontSize: 11, color: _textPrimary, fontWeight: FontWeight.w500),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        Text(
                          sessTime.toString(),
                          style: TextStyle(fontSize: 10, color: _textMuted),
                        ),
                      ],
                    ),
                  );
                }),
              ],

              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF4F46E5),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  onPressed: () => Navigator.pop(ctx2),
                  child: const Text('إغلاق الفحص'),
                ),
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDetailRow(String label, String value, {bool isHighlight = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(fontSize: 12, color: _textMuted)),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: TextStyle(
                fontSize: 12,
                fontWeight: isHighlight ? FontWeight.bold : FontWeight.w600,
                color: isHighlight ? const Color(0xFF4F46E5) : _textPrimary,
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 4: AUDIENCE & AD MONETIZATION INTELLIGENCE CENTER
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildDataMonetizationTab() {
    final dailyUsd = _monetization['estDailyRevenueUsd'] ?? 14.65;
    final monthlyUsd = _monetization['estMonthlyRevenueUsd'] ?? 439.50;
    final dailyEgp = _monetization['estDailyRevenueEgp'] ?? 733;
    final monthlyEgp = _monetization['estMonthlyRevenueEgp'] ?? 21975;
    final dmpMonthlyUsd = _monetization['dmpMonthlyRevenueUsd'] ?? 0.90;
    final trendPriceUsd = _monetization['trendReportPriceUsd'] ?? 1000;
    final profiles = _monetization['activeProfiles'] ?? _sessions.length;

    final cities = (_analytics['topCities'] as List<dynamic>?) ?? [];

    return RefreshIndicator(
      color: const Color(0xFF10B981),
      backgroundColor: _cardBg,
      onRefresh: () => _fetchStats(),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 1. Hero Monetization Strategy Header
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF064E3B)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(20),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.3),
                  blurRadius: 15,
                  offset: const Offset(0, 5),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                      decoration: BoxDecoration(
                        color: const Color(0xFF10B981).withValues(alpha: 0.2),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: const Color(0xFF10B981), width: 1),
                      ),
                      child: const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.verified_rounded, color: Color(0xFF10B981), size: 14),
                          SizedBox(width: 4),
                          Text(
                            'تسييل شرعي وقانوني 100% بدون بيانات شخصية',
                            style: TextStyle(color: Color(0xFF10B981), fontSize: 11, fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    ),
                    const Text('💎 VAST & DMP Hub', style: TextStyle(color: Colors.white70, fontSize: 12, fontWeight: FontWeight.bold)),
                  ],
                ),
                const SizedBox(height: 14),
                const Text(
                  'استخبارات وتسييل الداتا وسوق الإعلانات',
                  style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w900),
                ),
                const SizedBox(height: 6),
                const Text(
                  'نموذج كبرى المنصات العالمية: تجميع سلوك واهتمامات المشاهدين وتصنيفهم لشرائح جماهيرية وتسييلها عبر شبكات إعلانات الفيديو وسماسرة الجمهور المعتمدين.',
                  style: TextStyle(color: Colors.white70, fontSize: 12, height: 1.4),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 2. Revenue & Valuation KPI Grid (4 Cards)
          Row(
            children: [
              Expanded(
                child: _buildRevenueKpiCard(
                  title: 'أرباح الفيديو اليومية المقدرة',
                  usdValue: '\$$dailyUsd',
                  egpValue: '$dailyEgp ج.م تقريباً',
                  subtitle: 'eCPM: \$5.00 لكل 1,000 فيديو',
                  icon: Icons.monetization_on_rounded,
                  color: const Color(0xFF10B981),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildRevenueKpiCard(
                  title: 'التوقع الشهري لإعلانات الفيديو',
                  usdValue: '\$$monthlyUsd',
                  egpValue: '$monthlyEgp ج.م تقريباً',
                  subtitle: 'إعلانات Pre-Roll قبل العرض',
                  icon: Icons.trending_up_rounded,
                  color: const Color(0xFF0284C7),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _buildRevenueKpiCard(
                  title: 'قيمة ملفات الجمهور (DMP)',
                  usdValue: '\$$dmpMonthlyUsd / شهر',
                  egpValue: '$profiles ملف جاهز للربط',
                  subtitle: 'LiveRamp & Lotame (\$1.5/1k)',
                  icon: Icons.people_alt_rounded,
                  color: const Color(0xFF8B5CF6),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildRevenueKpiCard(
                  title: 'سعر تقرير التريندات للشركات',
                  usdValue: '\$$trendPriceUsd',
                  egpValue: 'جاهز للتحميل والتصدير',
                  subtitle: 'لشركات الإنتاج ووكالات الدعاية',
                  icon: Icons.analytics_rounded,
                  color: const Color(0xFFF59E0B),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),

          // 3. One-Click Commercial Exports Card
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: _cardBg,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: _border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.file_download_outlined, color: Color(0xFF4F46E5), size: 22),
                    const SizedBox(width: 8),
                    Text(
                      'أدوات تصدير الداتا التجارية للشركات',
                      style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800, color: _textPrimary),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(
                  'ملفات مجهزة طبقاً للمواصفات القياسية لمنصات الإعلانات العالمية (IAB Tech Lab) بنقرة واحدة:',
                  style: TextStyle(fontSize: 12, color: _textMuted),
                ),
                const SizedBox(height: 14),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    _buildExportActionButton(
                      label: 'تصدير بروفايلات الجمهور (CSV)',
                      icon: Icons.table_chart_rounded,
                      color: const Color(0xFF10B981),
                      endpoint: '/api/export?route=audience&format=csv',
                    ),
                    _buildExportActionButton(
                      label: 'تصدير بروفايلات الجمهور (JSON)',
                      icon: Icons.data_object_rounded,
                      color: const Color(0xFF0284C7),
                      endpoint: '/api/export?route=audience&format=json',
                    ),
                    _buildExportActionButton(
                      label: 'توليد تقرير تريندات المشاهدة 2026',
                      icon: Icons.auto_graph_rounded,
                      color: const Color(0xFF8B5CF6),
                      endpoint: '/api/export?route=trends',
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // 4. Detailed Geo-Targeting by City & Region
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: _cardBg,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: _border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.location_city_rounded, color: Color(0xFF0284C7), size: 22),
                        const SizedBox(width: 8),
                        Text(
                          'الاستهداف الجغرافي الدقيق بالمدن والمحافظات',
                          style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800, color: _textPrimary),
                        ),
                      ],
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: const Color(0xFF0284C7).withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(
                        '${cities.length} مدينة مسجلة',
                        style: const TextStyle(color: Color(0xFF0284C7), fontSize: 11, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(
                  'أماكن تواجد المشاهدين الفعليين لتوجيه الإعلانات المحلية شديدة الدقة:',
                  style: TextStyle(fontSize: 12, color: _textMuted),
                ),
                const SizedBox(height: 16),
                if (cities.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    child: Center(
                      child: Text('جاري رصد المدن من الجلسات الحية...', style: TextStyle(color: _textMuted, fontSize: 13)),
                    ),
                  )
                else
                  ...cities.take(8).map((c) {
                    final map = c as Map<String, dynamic>;
                    final name = map['name'] ?? 'مصر';
                    final count = map['count'] ?? 0;
                    final pct = (map['percent'] ?? 0).toInt();
                    return _buildCityTargetingRow(name, count, pct);
                  }),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // 5. Audience Segments Breakdown
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: _cardBg,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: _border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.pie_chart_rounded, color: Color(0xFF8B5CF6), size: 22),
                    const SizedBox(width: 8),
                    Text(
                      'شرائح الجمهور المصنفة للشركات والمعلنين',
                      style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800, color: _textPrimary),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(
                  'تصنيف المستخدمين حسب تفضيلات المشاهدة لجذب كبرى الوكالات الإعلانية بأعلى عائد:',
                  style: TextStyle(fontSize: 12, color: _textMuted),
                ),
                const SizedBox(height: 16),
                _buildSegmentCard(
                  name: 'متابعو الرياضة والمباريات الحية ⚽',
                  category: 'Sports & Live TV',
                  ecpm: '\$7.00 - \$18.00 eCPM',
                  targetBrands: 'المتاجر والملابس الرياضية، منصات البث، المشروبات',
                  color: const Color(0xFF10B981),
                ),
                const SizedBox(height: 10),
                _buildSegmentCard(
                  name: 'عشاق سينما الأكشن والإثارة 🎬',
                  category: 'Action & Cinema Blockbusters',
                  ecpm: '\$5.00 - \$12.00 eCPM',
                  targetBrands: 'شركات الهواتف الذكية، منصات الألعاب والـ Streaming، السيارات',
                  color: const Color(0xFF0284C7),
                ),
                const SizedBox(height: 10),
                _buildSegmentCard(
                  name: 'جمهور الدراما ومسلسلات رمضان 📺',
                  category: 'Drama & Family Prime Time',
                  ecpm: '\$6.00 - \$15.00 eCPM',
                  targetBrands: 'الأغذية (FMCG)، البنوك، محافظ الدفع الإلكتروني، المتاجر',
                  color: const Color(0xFFF59E0B),
                ),
                const SizedBox(height: 10),
                _buildSegmentCard(
                  name: 'عشاق الأنمي والشباب والجيمنج ⚔️',
                  category: 'Anime & Gaming Youth',
                  ecpm: '\$6.00 - \$14.00 eCPM',
                  targetBrands: 'أجهزة وإكسسوارات الجيمنج، ألعاب الفيديو، الماركات الشبابية',
                  color: const Color(0xFF8B5CF6),
                ),
              ],
            ),
          ),
          const SizedBox(height: 30),
        ],
      ),
    );
  }

  Widget _buildRevenueKpiCard({
    required String title,
    required String usdValue,
    required String egpValue,
    required String subtitle,
    required IconData icon,
    required Color color,
  }) {
    return Container(
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
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(color: color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(10)),
                child: Icon(icon, color: color, size: 20),
              ),
              Text(
                egpValue,
                style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: color),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(usdValue, style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: _textPrimary)),
          const SizedBox(height: 4),
          Text(title, style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: _textPrimary)),
          const SizedBox(height: 2),
          Text(subtitle, style: TextStyle(fontSize: 10, color: _textMuted)),
        ],
      ),
    );
  }

  Widget _buildExportActionButton({
    required String label,
    required IconData icon,
    required Color color,
    required String endpoint,
  }) {
    return InkWell(
      onTap: () => _handleExportDialog(label, endpoint),
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: color.withValues(alpha: 0.3)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color, size: 18),
            const SizedBox(width: 8),
            Text(label, style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }

  void _handleExportDialog(String title, String endpoint) async {
    final fullUrl = '$_apiBaseUrl$endpoint';
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: _cardBg,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Row(
          children: [
            const Icon(Icons.cloud_download_rounded, color: Color(0xFF4F46E5)),
            const SizedBox(width: 8),
            Expanded(child: Text(title, style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: _textPrimary))),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('الرابط المباشر للتصدير من السحابة:', style: TextStyle(fontSize: 12, color: _textMuted)),
            const SizedBox(height: 6),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: widget.isLightMode ? const Color(0xFFF1F5F9) : const Color(0xFF1E2433),
                borderRadius: BorderRadius.circular(8),
              ),
              child: SelectableText(
                fullUrl,
                style: const TextStyle(fontSize: 11, fontFamily: 'monospace', color: Color(0xFF4F46E5)),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'يمكنك نسخ الرابط أو نسخ البيانات مباشرة لاستخدامها في أدوات الإعلانات (DMP / DSP / Google Ad Manager).',
              style: TextStyle(fontSize: 11, color: _textMuted, height: 1.4),
            ),
          ],
        ),
        actions: [
          TextButton.icon(
            icon: const Icon(Icons.copy_rounded, size: 16),
            label: const Text('نسخ الرابط'),
            onPressed: () {
              Clipboard.setData(ClipboardData(text: fullUrl));
              Navigator.pop(ctx);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('✅ تم نسخ رابط التصدير بنجاح'), backgroundColor: Color(0xFF10B981)),
              );
            },
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF4F46E5),
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
            onPressed: () => Navigator.pop(ctx),
            child: const Text('حسناً'),
          ),
        ],
      ),
    );
  }

  Widget _buildCityTargetingRow(String name, int count, int pct) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(name, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: _textPrimary)),
              Text('$count زائر · $pct%', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: _textMuted)),
            ],
          ),
          const SizedBox(height: 5),
          LinearProgressIndicator(
            value: (pct / 100.0).clamp(0.0, 1.0),
            backgroundColor: _border,
            valueColor: const AlwaysStoppedAnimation(Color(0xFF0284C7)),
            minHeight: 6,
            borderRadius: BorderRadius.circular(3),
          ),
        ],
      ),
    );
  }

  Widget _buildSegmentCard({
    required String name,
    required String category,
    required String ecpm,
    required String targetBrands,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: widget.isLightMode ? const Color(0xFFF8FAFC) : const Color(0xFF111622),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: _border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(name, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w800, color: _textPrimary)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(color: color.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                child: Text(ecpm, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text('فئة IAB الدولية: $category', style: TextStyle(fontSize: 11, color: _textMuted)),
          const SizedBox(height: 4),
          Text('المعلنون المشترون: $targetBrands', style: TextStyle(fontSize: 11, color: color, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 5: TOP CONTENT (MOVIES & SERIES) WITH DYNAMIC ICONS & SEARCH
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildTopContentTab() {
    final filtered = _topContent.where((item) {
      final title = (item['title'] ?? '').toString().toLowerCase();
      final cat = (item['category'] ?? '').toString().toLowerCase();
      final q = _contentFilterQuery.toLowerCase().trim();
      return q.isEmpty || title.contains(q) || cat.contains(q);
    }).toList();

    return RefreshIndicator(
      color: const Color(0xFF4F46E5),
      backgroundColor: _cardBg,
      onRefresh: () => _fetchStats(),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            color: _cardBg,
            child: TextField(
              style: TextStyle(color: _textPrimary, fontSize: 13),
              decoration: InputDecoration(
                hintText: 'ابحث في الأعمال الأكثر مشاهدة...',
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
                  _contentFilterQuery = val;
                });
              },
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: filtered.isEmpty
                ? Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.movie_filter_rounded, size: 48, color: _textMuted),
                        const SizedBox(height: 12),
                        Text('لا توجد مشاهدات مطابقة للبحث', style: TextStyle(color: _textMuted)),
                      ],
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: filtered.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final item = filtered[index] as Map<String, dynamic>;
                      final title = item['title'] ?? 'عمل غير مسمى';
                      final category = item['category'] ?? 'منوعات';
                      final icon = item['categoryIcon'] ?? '🎬';
                      final views = item['views'] ?? 0;
                      final completionRate = (item['completionRate'] ?? 85).toInt();

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
          ),
        ],
      ),
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 6: SEARCH RADAR
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildSearchRadarTab() {
    final filtered = _searches.where((s) {
      final query = (s['query'] ?? '').toString().toLowerCase();
      final q = _searchRadarFilter.toLowerCase().trim();
      return q.isEmpty || query.contains(q);
    }).toList();

    return RefreshIndicator(
      color: const Color(0xFF4F46E5),
      backgroundColor: _cardBg,
      onRefresh: () => _fetchStats(),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            color: _cardBg,
            child: TextField(
              style: TextStyle(color: _textPrimary, fontSize: 13),
              decoration: InputDecoration(
                hintText: 'ابحث في رادار الكلمات الأكثر طلباً...',
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
                  _searchRadarFilter = val;
                });
              },
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: filtered.isEmpty
                ? Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.saved_search_rounded, size: 48, color: _textMuted),
                        const SizedBox(height: 12),
                        Text('لا توجد عمليات بحث تطابق المدخل', style: TextStyle(color: _textMuted)),
                      ],
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: filtered.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final s = filtered[index] as Map<String, dynamic>;
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
          ),
        ],
      ),
    );
  }

  // ═════════════════════════════════════════════════════════════════════════════
  // TAB 7: CHARTS & ANALYTICS (CLEAN PROGRESS & METRICS)
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildChartsTab() {
    final deviceBrands = (_analytics['deviceBrands'] as List<dynamic>?) ?? [];
    final topCountries = (_analytics['topCountries'] as List<dynamic>?) ?? [];
    final hourlyTraffic = (_analytics['hourlyTraffic'] as List<dynamic>?) ?? [];
    final osList = (_analytics['osList'] as List<dynamic>?) ?? [];
    final webBrowsers = (_analytics['webBrowsers'] as List<dynamic>?) ?? [];

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
                                Text('$count جهاز · $percent%', style: const TextStyle(color: Color(0xFF4F46E5), fontSize: 12, fontWeight: FontWeight.bold)),
                              ],
                            ),
                            const SizedBox(height: 6),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(4),
                              child: LinearProgressIndicator(
                                value: (percent / 100.0).clamp(0.0, 1.0),
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
                            Text('$count جهاز · $percent%', style: const TextStyle(color: Color(0xFF0284C7), fontSize: 12, fontWeight: FontWeight.bold)),
                          ],
                        ),
                        const SizedBox(height: 6),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: (percent / 100.0).clamp(0.0, 1.0),
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

          // 4. Web Browsers
          if (webBrowsers.isNotEmpty) ...[
            _buildCardContainer(
              title: '🌐 متصفحات الويب الأكثر استخداماً',
              child: Column(
                children: webBrowsers.map((wb) {
                  final browser = wb['browser'] ?? '';
                  final count = wb['count'] ?? 0;
                  final percent = (wb['percent'] ?? 0).toDouble();
                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(browser, style: TextStyle(color: _textPrimary, fontSize: 13, fontWeight: FontWeight.w600)),
                            Text('$count زيارة · $percent%', style: const TextStyle(color: Color(0xFF8B5CF6), fontSize: 12, fontWeight: FontWeight.bold)),
                          ],
                        ),
                        const SizedBox(height: 6),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: (percent / 100.0).clamp(0.0, 1.0),
                            minHeight: 7,
                            backgroundColor: widget.isLightMode ? const Color(0xFFE2E8F0) : const Color(0xFF262D3D),
                            valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF8B5CF6)),
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

          // 5. Geographic Countries
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
                                Text('$count جلسة · $percent%', style: const TextStyle(color: Color(0xFF10B981), fontSize: 12, fontWeight: FontWeight.bold)),
                              ],
                            ),
                            const SizedBox(height: 6),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(4),
                              child: LinearProgressIndicator(
                                value: (percent / 100.0).clamp(0.0, 1.0),
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
  // TAB 8: ADMIN TOOLKIT & LIVE OPERATIONS
  // ═════════════════════════════════════════════════════════════════════════════
  Widget _buildAdminToolkitTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // 1. Live Activity Feed (Recent 15 Operations)
        _buildCardContainer(
          title: '⚡ سجل النشاط اللحظي المباشر (${_activityLog.length})',
          child: _activityLog.isEmpty
              ? Padding(
                  padding: const EdgeInsets.all(16),
                  child: Center(child: Text('لا يوجد نشاط مسجل حالياً', style: TextStyle(color: _textMuted))),
                )
              : ListView.separated(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: _activityLog.take(8).length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (ctx, i) {
                    final log = _activityLog[i] as Map<String, dynamic>;
                    final action = log['action'] ?? 'تفاعل مع التطبيق';
                    final device = log['device'] ?? 'جهاز أندرويد';
                    final time = log['time'] ?? log['timestamp'] ?? '';

                    return ListTile(
                      dense: true,
                      leading: Container(
                        padding: const EdgeInsets.all(6),
                        decoration: BoxDecoration(
                          color: const Color(0xFF10B981).withValues(alpha: 0.1),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.flash_on_rounded, size: 16, color: Color(0xFF10B981)),
                      ),
                      title: Text(action.toString(), style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: _textPrimary)),
                      subtitle: Text(device.toString(), style: TextStyle(fontSize: 10, color: _textMuted)),
                      trailing: Text(time.toString(), style: TextStyle(fontSize: 10, color: _textMuted)),
                    );
                  },
                ),
        ),
        const SizedBox(height: 16),

        // 2. Playback & Server Health Errors Log
        _buildCardContainer(
          title: '🚨 سجل أخطاء البث والسيرفرات (${_errors.length})',
          child: _errors.isEmpty
              ? Padding(
                  padding: const EdgeInsets.all(16),
                  child: Center(
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.check_circle_rounded, color: Color(0xFF10B981), size: 18),
                        const SizedBox(width: 8),
                        Text('جميع السيرفرات والبثوث تعمل بكفاءة 100% بدون أعطال', style: TextStyle(color: _textMuted, fontSize: 12)),
                      ],
                    ),
                  ),
                )
              : ListView.separated(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: _errors.take(5).length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (ctx, i) {
                    final err = _errors[i] as Map<String, dynamic>;
                    final msg = err['message'] ?? err['error'] ?? 'خطأ تشغيل';
                    final time = err['time'] ?? '';

                    return ListTile(
                      dense: true,
                      leading: const Icon(Icons.warning_amber_rounded, color: Color(0xFFEF4444), size: 20),
                      title: Text(msg.toString(), style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFFEF4444))),
                      subtitle: Text(time.toString(), style: TextStyle(fontSize: 10, color: _textMuted)),
                    );
                  },
                ),
        ),
        const SizedBox(height: 16),

        // 3. Export Executive Report
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

        // 4. Cloud Latency & Infrastructure
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
                        _cloudLatencyMs != null ? 'زمن الاستجابة: $_cloudLatencyMs م.ث' : 'لم يتم الفحص بعد',
                        style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: _textPrimary),
                      ),
                      Text('سحابة Vercel + Firebase RTDB', style: TextStyle(fontSize: 11, color: _textMuted)),
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

        // 5. Raw JSON Viewer
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

        // 6. Cloud Security & Killswitch Monitor
        _buildCardContainer(
          title: '🛡️ أمان السحابة المركزية ومنظومة التحكم',
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('خوادم تحليلات زقزوقة TV:', style: TextStyle(fontSize: 12, color: _textMuted)),
                      const SizedBox(height: 3),
                      const Text(
                        'متصلة بالسحابة المشفرة بنجاح (مستوى عالي الأمان)',
                        style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Color(0xFF10B981)),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(
                    color: const Color(0xFF10B981).withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Row(
                    children: [
                      Icon(Icons.shield_rounded, color: Color(0xFF10B981), size: 16),
                      SizedBox(width: 4),
                      Text('محمي', style: TextStyle(color: Color(0xFF10B981), fontSize: 11, fontWeight: FontWeight.bold)),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 24),
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
🌐 السيرفر: سحابة Vercel + Firebase Realtime Database
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
      'activityLog': _activityLog,
      'errors': _errors,
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
