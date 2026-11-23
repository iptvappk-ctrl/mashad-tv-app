import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'security_vault.dart';
import 'api_client.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  runApp(const ZaqzoukaMasterApp());
}

class ZaqzoukaMasterApp extends StatelessWidget {
  const ZaqzoukaMasterApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Zaqzouka Master Control',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0A0C14),
        fontFamily: 'Cairo',
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFFB94E3C),
          secondary: Color(0xFFD4AF37),
          surface: Color(0xFF141824),
        ),
      ),
      home: const MasterAuthScreen(),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Auth Screen
// ─────────────────────────────────────────────────────────────────────────────
class MasterAuthScreen extends StatefulWidget {
  const MasterAuthScreen({super.key});

  @override
  State<MasterAuthScreen> createState() => _MasterAuthScreenState();
}

class _MasterAuthScreenState extends State<MasterAuthScreen> {
  final TextEditingController _passwordController = TextEditingController();
  bool _obscureText = true;
  bool _isAuthenticating = false;
  String? _errorMessage;

  @override
  void dispose() {
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _verifyPassword() async {
    final text = _passwordController.text.trim();
    if (text.isEmpty) {
      setState(() => _errorMessage = 'يرجى إدخال كلمة المرور');
      return;
    }
    setState(() { _isAuthenticating = true; _errorMessage = null; });
    final valid = await SecurityVault.verifyPin(text);
    if (!mounted) return;
    if (valid) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => MasterDashboardScreen(masterPin: text)),
      );
    } else {
      setState(() {
        _isAuthenticating = false;
        _errorMessage = 'كلمة المرور غير صحيحة، حاول مجدداً';
        _passwordController.clear();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Directionality(
        textDirection: TextDirection.rtl,
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 28.0, vertical: 24.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    width: 76, height: 76,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: const Color(0xFFB94E3C).withValues(alpha: 0.15),
                      border: Border.all(color: const Color(0xFFB94E3C), width: 2),
                      boxShadow: [BoxShadow(color: const Color(0xFFD4AF37).withValues(alpha: 0.2), blurRadius: 20, spreadRadius: 2)],
                    ),
                    child: const Icon(Icons.shield_rounded, size: 40, color: Color(0xFFD4AF37)),
                  ),
                  const SizedBox(height: 20),
                  const Text('لوحة التحكم الإداري الأعلى', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white)),
                  const SizedBox(height: 6),
                  const Text('أدخل كلمة المرور الرئيسية (Master Key)', style: TextStyle(fontSize: 13, color: Color(0xFF8E95AA))),
                  const SizedBox(height: 36),
                  Container(
                    decoration: BoxDecoration(
                      color: const Color(0xFF141824),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: _errorMessage != null ? Colors.redAccent : const Color(0xFF242A3D), width: 1.5),
                    ),
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                    child: TextField(
                      controller: _passwordController,
                      obscureText: _obscureText,
                      textInputAction: TextInputAction.done,
                      onSubmitted: (_) => _verifyPassword(),
                      style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold, letterSpacing: 1.5),
                      textAlign: TextAlign.center,
                      decoration: InputDecoration(
                        hintText: 'كلمة المرور...',
                        hintStyle: const TextStyle(color: Color(0xFF555E75), fontSize: 14),
                        border: InputBorder.none,
                        prefixIcon: const Icon(Icons.key_rounded, color: Color(0xFFD4AF37), size: 22),
                        suffixIcon: IconButton(
                          icon: Icon(_obscureText ? Icons.visibility_off_rounded : Icons.visibility_rounded, color: const Color(0xFF8E95AA), size: 20),
                          onPressed: () => setState(() => _obscureText = !_obscureText),
                        ),
                      ),
                    ),
                  ),
                  if (_errorMessage != null) ...[
                    const SizedBox(height: 12),
                    Text(_errorMessage!, style: const TextStyle(color: Colors.redAccent, fontSize: 13, fontWeight: FontWeight.bold), textAlign: TextAlign.center),
                  ],
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity, height: 52,
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFB94E3C), foregroundColor: Colors.white, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)), elevation: 4),
                      onPressed: _isAuthenticating ? null : _verifyPassword,
                      child: _isAuthenticating
                          ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white))
                          : const Text('تأكيد الدخول', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                    ),
                  ),
                  const SizedBox(height: 32),
                  const Text('المنظومة محمية بتشفير صارم وقواعد أمان ضد أي استخراج.', style: TextStyle(fontSize: 11, color: Color(0xFF4A5568)), textAlign: TextAlign.center),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Master Dashboard Screen
// ─────────────────────────────────────────────────────────────────────────────
class MasterDashboardScreen extends StatefulWidget {
  final String masterPin;
  const MasterDashboardScreen({super.key, required this.masterPin});

  @override
  State<MasterDashboardScreen> createState() => _MasterDashboardScreenState();
}

class _MasterDashboardScreenState extends State<MasterDashboardScreen> {
  Map<String, dynamic> _statusData = {};
  bool _isLoading = true;
  final Map<String, bool> _componentLoading = {};
  Timer? _pollTimer;

  // 👑 Sovereign Owner Ad States & Controllers
  bool _ownerAdEnabled = false;
  bool _overridePartnerAd = true;
  final TextEditingController _ownerAdHtmlCtrl = TextEditingController();
  final TextEditingController _ownerAdUrlCtrl = TextEditingController();
  int _ownerSkipDelay = 5;

  bool _bannerAdEnabled = false;
  final TextEditingController _bannerHtmlCtrl = TextEditingController();
  final TextEditingController _bannerUrlCtrl = TextEditingController();
  bool _isSavingOwnerAds = false;

  // 4 User-Facing Components to Control
  static const List<Map<String, dynamic>> _components = [
    {
      'key': 'download_site_enabled',
      'label': 'موقع التحميل (Download Hub)',
      'subtitle': 'zaafilm.com (الرسمي)',
      'icon': Icons.cloud_download_rounded,
      'color': Color(0xFF4A90D9),
    },
    {
      'key': 'web_player_enabled',
      'label': 'موقع زقزوقة ويب (Web Player)',
      'subtitle': 'web-nu-teal-29.vercel.app',
      'icon': Icons.public_rounded,
      'color': Color(0xFF9B51E0),
    },
    {
      'key': 'app_enabled',
      'label': 'تطبيق زقزوقة TV',
      'subtitle': 'com.madar.madar_flutter',
      'icon': Icons.tv_rounded,
      'color': Color(0xFFB94E3C),
    },
    {
      'key': 'player_enabled',
      'label': 'مشغل زاغ بلاير',
      'subtitle': 'com.madar.player',
      'icon': Icons.play_circle_rounded,
      'color': Color(0xFFE5A958),
    },
  ];

  @override
  void initState() {
    super.initState();
    SecurityVault.wipeVault();
    _refreshStatus();
    _pollTimer = Timer.periodic(const Duration(seconds: 10), (_) => _refreshStatus(silent: true));
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _ownerAdHtmlCtrl.dispose();
    _ownerAdUrlCtrl.dispose();
    _bannerHtmlCtrl.dispose();
    _bannerUrlCtrl.dispose();
    super.dispose();
  }

  Future<void> _refreshStatus({bool silent = false}) async {
    if (!silent) setState(() => _isLoading = true);
    final data = await MasterApiClient.fetchStatus();
    if (!mounted) return;
    final ownerAd = data['owner_ad_config'];
    if (ownerAd is Map) {
      _ownerAdEnabled = ownerAd['enabled'] == true;
      _overridePartnerAd = ownerAd['override_partner'] != false;
      _ownerSkipDelay = (ownerAd['skip_delay_seconds'] as num?)?.toInt() ?? 5;
      if (_ownerAdHtmlCtrl.text.isEmpty && ownerAd['ad_html'] != null) {
        _ownerAdHtmlCtrl.text = ownerAd['ad_html'].toString();
      }
      if (_ownerAdUrlCtrl.text.isEmpty && ownerAd['ad_url'] != null) {
        _ownerAdUrlCtrl.text = ownerAd['ad_url'].toString();
      }
      _bannerAdEnabled = ownerAd['banner_enabled'] == true;
      if (_bannerHtmlCtrl.text.isEmpty && ownerAd['banner_html'] != null) {
        _bannerHtmlCtrl.text = ownerAd['banner_html'].toString();
      }
      if (_bannerUrlCtrl.text.isEmpty && ownerAd['banner_url'] != null) {
        _bannerUrlCtrl.text = ownerAd['banner_url'].toString();
      }
    }
    setState(() { _statusData = data; _isLoading = false; });
  }

  Future<void> _saveOwnerAdSettings() async {
    setState(() => _isSavingOwnerAds = true);
    final ok = await MasterApiClient.saveOwnerAdConfig(
      enabled: _ownerAdEnabled,
      overridePartner: _overridePartnerAd,
      adHtml: _ownerAdHtmlCtrl.text.trim(),
      adUrl: _ownerAdUrlCtrl.text.trim(),
      skipDelaySeconds: _ownerSkipDelay,
      bannerEnabled: _bannerAdEnabled,
      bannerHtml: _bannerHtmlCtrl.text.trim(),
      bannerUrl: _bannerUrlCtrl.text.trim(),
    );
    if (!mounted) return;
    setState(() => _isSavingOwnerAds = false);
    _showToast(ok ? 'تم حفظ وتفعيل إعلانات المالك بنجاح!' : 'فشل الحفظ، تأكد من الاتصال');
  }

  String get _currentStatus => _statusData['status']?.toString().toLowerCase() ?? 'active';

  Map<String, bool> get _componentsState {
    final raw = _statusData['components'];
    if (raw is Map) {
      return {
        'download_site_enabled': raw['download_site_enabled'] != false && raw['website_enabled'] != false,
        'web_player_enabled': raw['web_player_enabled'] != false && raw['website_enabled'] != false,
        'app_enabled': raw['app_enabled'] != false,
        'player_enabled': raw['player_enabled'] != false,
      };
    }
    return {
      'download_site_enabled': true,
      'web_player_enabled': true,
      'app_enabled': true,
      'player_enabled': true,
    };
  }

  Future<void> _toggleComponent(String key, bool newValue) async {
    setState(() => _componentLoading[key] = true);
    final ok = await MasterApiClient.setComponentStatus(key, newValue);
    if (!mounted) return;
    if (ok) {
      final comps = Map<String, dynamic>.from(_statusData['components'] ?? {});
      comps[key] = newValue;
      setState(() { _statusData = Map<String, dynamic>.from(_statusData)..['components'] = comps; });
      _showToast(newValue ? 'تم تفعيل المكون بنجاح' : 'تم إيقاف المكون بنجاح');
    } else {
      _showToast('فشل تنفيذ الأمر، تأكد من الاتصال');
    }
    setState(() => _componentLoading.remove(key));
    await Future.delayed(const Duration(seconds: 2));
    if (mounted) _refreshStatus(silent: true);
  }

  // ── Action 1: Nuclear Wipe (عليا وعلى أعدائي - الحذف الكامل لكل شيء) ──
  Future<void> _confirmNuclearWipe() async {
    final confirmCtrl = TextEditingController();
    final confirm = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF220C0C),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18), side: const BorderSide(color: Colors.redAccent, width: 2)),
        title: const Row(
          children: [
            Icon(Icons.warning_amber_rounded, color: Colors.redAccent, size: 28),
            SizedBox(width: 8),
            Expanded(child: Text('💥 الحذف الكامل والتدمير الشامل', style: TextStyle(color: Colors.redAccent, fontSize: 17, fontWeight: FontWeight.bold))),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'تحذير شديد الخطورة (عليا وعلى أعدائي):\n'
              '• تصفير وحذف جميع السيرفرات والقنوات والبيانات من Firebase نهائياً.\n'
              '• قفل كلا الموقعين (التحميل والبث) فوراً وشاشات حظر.\n'
              '• إيقاف التطبيق والمشغل ومسح الكاش من كل أجهزة المستخدمين.\n'
              '• تدمير شامل للمنظومة دون ترك أي أثر.\n\n'
              'لتأكيد الحذف الكامل، اكتب كلمة WIPE أدناه:',
              style: TextStyle(fontSize: 12.5, color: Colors.white70, height: 1.5),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: confirmCtrl,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.redAccent, fontWeight: FontWeight.bold, fontSize: 18),
              decoration: InputDecoration(hintText: 'اكتب WIPE للتأكيد', filled: true, fillColor: const Color(0xFF140808), border: OutlineInputBorder(borderRadius: BorderRadius.circular(10))),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('تراجع')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
            onPressed: () { if (confirmCtrl.text.trim().toUpperCase() == 'WIPE') Navigator.pop(ctx, true); },
            child: const Text('تدمير ومسح كل شيء فوراً'),
          ),
        ],
      ),
    );
    if (confirm == true) {
      setState(() => _isLoading = true);
      await SecurityVault.wipeVault();
      final ok = await MasterApiClient.triggerNuclearWipe();
      if (!mounted) return;
      _showToast(ok ? '💥 تم تدمير ومسح كل شيء نهائياً بلا رجعة' : 'فشل تنفيذ أمر المسح');
      _refreshStatus();
    }
  }

  // ── Action 2: Emergency Freeze (all) ──
  Future<void> _confirmFreeze() async {
    final msgCtrl = TextEditingController(text: 'عفواً، الخدمة متوقفة حالياً للتحديث والصيانة.');
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1A1F2E),
        title: const Text('إيقاف وتجميد مؤقت للمنظومة', style: TextStyle(color: Colors.white, fontSize: 18)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('سيتم إيقاف الموقعين والتطبيق والمشغل فوراً عند جميع المستخدمين وعرض الرسالة:', style: TextStyle(fontSize: 13, color: Color(0xFF8E95AA))),
            const SizedBox(height: 12),
            TextField(controller: msgCtrl, maxLines: 2, decoration: InputDecoration(filled: true, fillColor: const Color(0xFF10131E), border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)))),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('إلغاء')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFD4AF37), foregroundColor: Colors.black),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('تأكيد الإيقاف الفوري'),
          ),
        ],
      ),
    );
    if (confirm == true) {
      setState(() => _isLoading = true);
      final ok = await MasterApiClient.triggerFreeze(message: msgCtrl.text.trim());
      if (!mounted) return;
      _showToast(ok ? 'تم تجميد المنظومة بالكامل بنجاح' : 'فشل تنفيذ الأمر، تأكد من الاتصال');
      _refreshStatus();
    }
  }

  // ── Action 3: Full Restore ──
  Future<void> _confirmRestore() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF0F2418),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18), side: const BorderSide(color: Color(0xFF27AE60), width: 1.5)),
        title: const Row(
          children: [
            Icon(Icons.restart_alt_rounded, color: Color(0xFF27AE60), size: 28),
            SizedBox(width: 8),
            Expanded(child: Text('استعادة وتشغيل كل شيء', style: TextStyle(color: Color(0xFF27AE60), fontSize: 18, fontWeight: FontWeight.bold))),
          ],
        ),
        content: const Text(
          'سيتم تفعيل كافة المواقع والتطبيقات والمشغل وإعادة تشغيل المنظومة بكامل طاقتها فوراً.',
          style: TextStyle(fontSize: 13.5, color: Colors.white70, height: 1.5),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('إلغاء')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF27AE60), foregroundColor: Colors.white),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('إعادة التشغيل الآن'),
          ),
        ],
      ),
    );
    if (confirm == true) {
      setState(() => _isLoading = true);
      final vaultData = await SecurityVault.loadVault(widget.masterPin) ?? {'servers': {}, 'catalog': {}};
      final ok = await MasterApiClient.triggerRestore(serversData: vaultData);
      if (!mounted) return;
      _showToast(ok ? 'تم استعادة المنظومة وتشغيلها بالكامل!' : 'فشل أمر الاستعادة');
      _refreshStatus();
    }
  }

  void _showToast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg, textAlign: TextAlign.center), duration: const Duration(seconds: 3)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final status = _currentStatus;
    Color statusColor = const Color(0xFF27AE60);
    String statusTitle = 'المنظومة نشطة وشغالة';
    String statusSubtitle = 'جميع السيرفرات والمواقع والمشغلات تعمل بشكل طبيعي';

    if (status == 'frozen') {
      statusColor = const Color(0xFFD4AF37);
      statusTitle = 'المنظومة مجمدة مؤقتاً';
      statusSubtitle = _statusData['message']?.toString() ?? 'الخدمة متوقفة للصيانة';
    } else if (status == 'wiped') {
      statusColor = Colors.redAccent;
      statusTitle = 'تم الحذف والتدمير بالكامل';
      statusSubtitle = 'البيانات صفرية وجميع أجهزة ومواقع المستخدمين مغلقة';
    } else if (status == 'unknown') {
      statusColor = const Color(0xFFE67E22);
      statusTitle = 'تعذر الاتصال بالسحابة';
      statusSubtitle = _statusData['message']?.toString() ?? 'تأكد من اتصالك بالإنترنت';
    }

    final compState = _componentsState;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Zaqzouka Master Control', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
        centerTitle: true,
        backgroundColor: const Color(0xFF10131E),
        elevation: 0,
        actions: [
          IconButton(icon: const Icon(Icons.refresh_rounded), onPressed: () => _refreshStatus()),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFB94E3C)))
          : Directionality(
              textDirection: TextDirection.rtl,
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 14.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // ── 1. Live Status Card ──
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                      decoration: BoxDecoration(
                        color: const Color(0xFF141824),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: statusColor, width: 2),
                        boxShadow: [BoxShadow(color: statusColor.withValues(alpha: 0.2), blurRadius: 12, spreadRadius: 1)],
                      ),
                      child: Column(
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Container(width: 10, height: 10, decoration: BoxDecoration(shape: BoxShape.circle, color: statusColor)),
                              const SizedBox(width: 8),
                              Flexible(child: Text(statusTitle, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: statusColor))),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(statusSubtitle, textAlign: TextAlign.center, style: const TextStyle(fontSize: 12, color: Color(0xFF8E95AA))),
                        ],
                      ),
                    ),

                    const SizedBox(height: 16),

                    // ── 2. PROMINENT NUCLEAR WIPE BUTTON (الحذف الكامل والتدمير الشامل) ──
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: const Color(0xFF220C0C),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: Colors.redAccent.withValues(alpha: 0.8), width: 1.5),
                        boxShadow: [
                          BoxShadow(color: Colors.redAccent.withValues(alpha: 0.2), blurRadius: 14, spreadRadius: 1),
                        ],
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          const Row(
                            children: [
                              Icon(Icons.warning_amber_rounded, color: Colors.redAccent, size: 22),
                              SizedBox(width: 8),
                              Text('الحذف الكامل لكل شيء (عليا وعلى أعدائي)', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Colors.redAccent)),
                            ],
                          ),
                          const SizedBox(height: 4),
                          const Text(
                            'تصفير وحذف كافة السيرفرات والقنوات من Firebase، وقفل كلا الموقعين وتدمير الكاش بكل أجهزة المستخدمين.',
                            style: TextStyle(fontSize: 11.5, color: Colors.white70, height: 1.3),
                          ),
                          const SizedBox(height: 10),
                          ElevatedButton.icon(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.redAccent,
                              foregroundColor: Colors.white,
                              padding: const EdgeInsets.symmetric(vertical: 12),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                              elevation: 3,
                            ),
                            icon: const Icon(Icons.delete_forever_rounded, size: 20),
                            label: const Text('تدمير ومسح المنظومة بالكامل فوراً', style: TextStyle(fontSize: 13.5, fontWeight: FontWeight.bold)),
                            onPressed: _confirmNuclearWipe,
                          ),
                        ],
                      ),
                    ),

                    const SizedBox(height: 18),

                    // ── 3. Granular Component Toggles (4 Essential Parts) ──
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: const Color(0xFF141824),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: const Color(0xFF242A3D)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            children: [
                              Icon(Icons.toggle_on_rounded, color: Color(0xFFD4AF37), size: 20),
                              SizedBox(width: 8),
                              Text('التحكم في أجزاء المشروع منفصلة', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Colors.white)),
                            ],
                          ),
                          const SizedBox(height: 2),
                          const Text('إيقاف أو تشغيل أي مكوّن بشكل فوري ومنفصل', style: TextStyle(fontSize: 11.5, color: Color(0xFF8E95AA))),
                          const SizedBox(height: 12),
                          ...(_components.map((comp) {
                            final key = comp['key'] as String;
                            final isEnabled = compState[key] ?? true;
                            final isLoading = _componentLoading[key] == true;
                            final color = comp['color'] as Color;
                            return Container(
                              margin: const EdgeInsets.only(bottom: 8),
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                              decoration: BoxDecoration(
                                color: const Color(0xFF0F131E),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(color: isEnabled ? color.withValues(alpha: 0.35) : const Color(0xFF2A2A3D)),
                              ),
                              child: Row(
                                children: [
                                  Container(
                                    width: 34, height: 34,
                                    decoration: BoxDecoration(shape: BoxShape.circle, color: (isEnabled ? color : const Color(0xFF555E75)).withValues(alpha: 0.15)),
                                    child: Icon(comp['icon'] as IconData, color: isEnabled ? color : const Color(0xFF555E75), size: 18),
                                  ),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(comp['label'] as String, style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: isEnabled ? Colors.white : const Color(0xFF8E95AA))),
                                        Text(comp['subtitle'] as String, style: const TextStyle(fontSize: 10.5, color: Color(0xFF555E75))),
                                      ],
                                    ),
                                  ),
                                  if (isLoading)
                                    const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFFD4AF37)))
                                  else
                                    Switch(
                                      value: isEnabled,
                                      onChanged: (val) => _toggleComponent(key, val),
                                      activeThumbColor: color, activeTrackColor: color.withValues(alpha: 0.5),
                                      inactiveThumbColor: const Color(0xFF555E75),
                                      inactiveTrackColor: const Color(0xFF2A2A3D),
                                    ),
                                ],
                              ),
                            );
                          }).toList()),
                        ],
                      ),
                    ),

                    const SizedBox(height: 16),

                    // ── 4. Freeze All & Full Restore Row ──
                    Row(
                      children: [
                        // Freeze All
                        Expanded(
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: const Color(0xFF141824),
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(color: const Color(0xFFD4AF37).withValues(alpha: 0.4)),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                const Text('تجميد مؤقت', textAlign: TextAlign.center, style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Color(0xFFD4AF37))),
                                const SizedBox(height: 4),
                                const Text('إيقاف كل شيء للصيانة', textAlign: TextAlign.center, style: TextStyle(fontSize: 10.5, color: Color(0xFF8E95AA))),
                                const SizedBox(height: 8),
                                ElevatedButton(
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: const Color(0xFFD4AF37),
                                    foregroundColor: Colors.black,
                                    padding: const EdgeInsets.symmetric(vertical: 8),
                                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                                  ),
                                  onPressed: _confirmFreeze,
                                  child: const Text('تجميد الكل', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                                ),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(width: 12),
                        // Full Restore
                        Expanded(
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: const Color(0xFF141824),
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(color: const Color(0xFF27AE60).withValues(alpha: 0.4)),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                const Text('استعادة كاملة', textAlign: TextAlign.center, style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Color(0xFF27AE60))),
                                const SizedBox(height: 4),
                                const Text('تشغيل المنظومة كاملة', textAlign: TextAlign.center, style: TextStyle(fontSize: 10.5, color: Color(0xFF8E95AA))),
                                const SizedBox(height: 8),
                                ElevatedButton(
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: const Color(0xFF27AE60),
                                    foregroundColor: Colors.white,
                                    padding: const EdgeInsets.symmetric(vertical: 8),
                                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                                  ),
                                  onPressed: _confirmRestore,
                                  child: const Text('تشغيل الكل', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),

                     const SizedBox(height: 18),

                    // ── 5. Sovereign Owner Ads & Partner Override (خاص بالمالك وتجاوز كود الشريك) ──
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: const Color(0xFF141824),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: const Color(0xFFD4AF37).withValues(alpha: 0.8), width: 1.5),
                        boxShadow: [
                          BoxShadow(color: const Color(0xFFD4AF37).withValues(alpha: 0.15), blurRadius: 14, spreadRadius: 1),
                        ],
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.all(6),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFD4AF37).withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: const Icon(Icons.shield_rounded, color: Color(0xFFD4AF37), size: 22),
                              ),
                              const SizedBox(width: 10),
                              const Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text('إدارة الإعلانات السيادية (خاص بك كمالك)', style: TextStyle(fontSize: 14.5, fontWeight: FontWeight.bold, color: Colors.white)),
                                    Text('تحكم منفصل وسري: كودك الخاص وتجاوز كود الشريك', style: TextStyle(fontSize: 11, color: Color(0xFF8E95AA))),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 14),

                          // Toggle 1: Enable Owner Ads
                          SwitchListTile(
                            contentPadding: EdgeInsets.zero,
                            title: const Text('تفعيل إعلانات المشغل الخاصة بك', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Colors.white)),
                            subtitle: const Text('تشغيل إعلان قبل الفيديو (Pre-Roll) خاص بك', style: TextStyle(fontSize: 11, color: Color(0xFF8E95AA))),
                            value: _ownerAdEnabled,
                            onChanged: (v) => setState(() => _ownerAdEnabled = v),
                            activeThumbColor: const Color(0xFFD4AF37),
                          ),

                          // Toggle 2: Override Partner Ads (تجاوز كود الشريك)
                          Container(
                            margin: const EdgeInsets.symmetric(vertical: 6),
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: _overridePartnerAd ? const Color(0xFF2A1B0A) : const Color(0xFF0F131E),
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(color: _overridePartnerAd ? const Color(0xFFD4AF37) : const Color(0xFF2A2A3D)),
                            ),
                            child: SwitchListTile(
                              contentPadding: EdgeInsets.zero,
                              title: const Text('تجاوز وحجب كود الشريك (لوحة التحليلات)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Color(0xFFE5A958))),
                              subtitle: const Text('عند التفعيل، يتم حجب إعلان الشريك نهائياً وعرض إعلانك الخاص دون أن يظهر أي شيء في لوحته', style: TextStyle(fontSize: 10.5, color: Colors.white70)),
                              value: _overridePartnerAd,
                              onChanged: (v) => setState(() => _overridePartnerAd = v),
                              activeThumbColor: const Color(0xFFD4AF37),
                            ),
                          ),

                          const SizedBox(height: 10),
                          const Text('كود إعلان المشغل (HTML / JS Script):', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.white70)),
                          const SizedBox(height: 6),
                          TextField(
                            controller: _ownerAdHtmlCtrl,
                            maxLines: 4,
                            style: const TextStyle(fontSize: 11.5, fontFamily: 'monospace', color: Colors.white),
                            decoration: InputDecoration(
                              hintText: '<!-- ضع كود إعلانك هنا (Adsterra / Monetag / Custom HTML) -->',
                              hintStyle: const TextStyle(fontSize: 11, color: Colors.white38),
                              filled: true,
                              fillColor: const Color(0xFF0B0E14),
                              border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: Color(0xFF262D3D))),
                              enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: Color(0xFF262D3D))),
                            ),
                          ),

                          const SizedBox(height: 8),
                          const Text('أو رابط إعلان المشغل المباشر (URL):', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.white70)),
                          const SizedBox(height: 6),
                          TextField(
                            controller: _ownerAdUrlCtrl,
                            style: const TextStyle(fontSize: 12, color: Colors.white),
                            decoration: InputDecoration(
                              hintText: 'https://example.com/ad',
                              hintStyle: const TextStyle(fontSize: 11, color: Colors.white38),
                              filled: true,
                              fillColor: const Color(0xFF0B0E14),
                              border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: Color(0xFF262D3D))),
                              enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: Color(0xFF262D3D))),
                            ),
                          ),

                          const SizedBox(height: 8),
                          Row(
                            children: [
                              const Text('ظهور زر التخطي بعد:', style: TextStyle(fontSize: 12, color: Colors.white70)),
                              const SizedBox(width: 8),
                              DropdownButton<int>(
                                value: _ownerSkipDelay,
                                dropdownColor: const Color(0xFF141824),
                                style: const TextStyle(color: Color(0xFFD4AF37), fontWeight: FontWeight.bold),
                                items: [3, 5, 7, 10, 15].map((s) => DropdownMenuItem(value: s, child: Text('$s ثوانٍ'))).toList(),
                                onChanged: (v) { if (v != null) setState(() => _ownerSkipDelay = v); },
                              ),
                            ],
                          ),

                          const Divider(color: Color(0xFF262D3D), height: 24),

                          // ── Banner Ad Section (بنر غير مزعج) ──
                          Row(
                            children: [
                              const Icon(Icons.view_stream_rounded, color: Color(0xFF4A90D9), size: 18),
                              const SizedBox(width: 6),
                              const Text('شريط إعلاني ثابت غير مزعج (Banner)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Colors.white)),
                            ],
                          ),
                          SwitchListTile(
                            contentPadding: EdgeInsets.zero,
                            title: const Text('تفعيل شريط البنر الإعلاني بالتطبيق', style: TextStyle(fontSize: 12.5, color: Colors.white)),
                            subtitle: const Text('شريط أنيق أسفل الصفحة الرئيسية لا يعطل التصفح ولا يظهر بالمشغل', style: TextStyle(fontSize: 10.5, color: Color(0xFF8E95AA))),
                            value: _bannerAdEnabled,
                            onChanged: (v) => setState(() => _bannerAdEnabled = v),
                            activeThumbColor: const Color(0xFF4A90D9),
                          ),

                          if (_bannerAdEnabled) ...[
                            const SizedBox(height: 6),
                            const Text('كود البنر (HTML / JS):', style: TextStyle(fontSize: 11.5, color: Colors.white70)),
                            const SizedBox(height: 4),
                            TextField(
                              controller: _bannerHtmlCtrl,
                              maxLines: 2,
                              style: const TextStyle(fontSize: 11.5, fontFamily: 'monospace', color: Colors.white),
                              decoration: InputDecoration(
                                hintText: '<script ...> أو <a href=...><img .../></a>',
                                hintStyle: const TextStyle(fontSize: 11, color: Colors.white38),
                                filled: true,
                                fillColor: const Color(0xFF0B0E14),
                                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: Color(0xFF262D3D))),
                              ),
                            ),
                            const SizedBox(height: 6),
                            const Text('أو رابط صفحة البنر (URL):', style: TextStyle(fontSize: 11.5, color: Colors.white70)),
                            const SizedBox(height: 4),
                            TextField(
                              controller: _bannerUrlCtrl,
                              style: const TextStyle(fontSize: 12, color: Colors.white),
                              decoration: InputDecoration(
                                hintText: 'https://example.com/banner',
                                hintStyle: const TextStyle(fontSize: 11, color: Colors.white38),
                                filled: true,
                                fillColor: const Color(0xFF0B0E14),
                                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: Color(0xFF262D3D))),
                              ),
                            ),
                          ],

                          const SizedBox(height: 16),
                          ElevatedButton.icon(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFFD4AF37),
                              foregroundColor: Colors.black,
                              padding: const EdgeInsets.symmetric(vertical: 12),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                              elevation: 4,
                            ),
                            icon: _isSavingOwnerAds
                                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.black))
                                : const Icon(Icons.save_rounded, size: 20),
                            label: Text(
                              _isSavingOwnerAds ? 'جارٍ الحفظ السحابي...' : 'حفظ وتفعيل إعلانات المالك فوراً',
                              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                            ),
                            onPressed: _isSavingOwnerAds ? null : _saveOwnerAdSettings,
                          ),
                        ],
                      ),
                    ),

                    const SizedBox(height: 20),
                    // Security Footer
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      decoration: BoxDecoration(
                        color: const Color(0xFF0F131E),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: const Color(0xFF242A3D)),
                      ),
                      child: const Row(
                        children: [
                          Icon(Icons.lock_rounded, size: 18, color: Color(0xFFD4AF37)),
                          SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              'المنظومة محمية بتشفير AES-256 وقواعد أمان Firebase الصارمة.',
                              style: TextStyle(fontSize: 11, color: Color(0xFF8E95AA)),
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
