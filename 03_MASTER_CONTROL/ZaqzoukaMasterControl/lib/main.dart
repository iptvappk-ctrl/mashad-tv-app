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

  // Component definitions
  static const List<Map<String, dynamic>> _components = [
    {
      'key': 'download_site_enabled',
      'label': 'موقع التحميل (Download Hub)',
      'subtitle': 'zaqzouka-tv.vercel.app',
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
    {
      'key': 'admin_enabled',
      'label': 'لوحة الأدمن',
      'subtitle': 'Admin Dashboard App',
      'icon': Icons.admin_panel_settings_rounded,
      'color': Color(0xFF27AE60),
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
    super.dispose();
  }

  Future<void> _refreshStatus({bool silent = false}) async {
    if (!silent) setState(() => _isLoading = true);
    final data = await MasterApiClient.fetchStatus();
    if (!mounted) return;
    setState(() { _statusData = data; _isLoading = false; });
  }

  String get _currentStatus => _statusData['status']?.toString().toLowerCase() ?? 'active';

  Map<String, bool> get _componentsState {
    final raw = _statusData['components'];
    if (raw is Map) {
      return {
        'download_site_enabled': raw['download_site_enabled'] != false && raw['website_enabled'] != false,
        'web_player_enabled': raw['web_player_enabled'] != false && raw['website_enabled'] != false,
        'website_enabled': raw['website_enabled'] != false,
        'app_enabled': raw['app_enabled'] != false,
        'player_enabled': raw['player_enabled'] != false,
        'admin_enabled': raw['admin_enabled'] != false,
      };
    }
    return {
      'download_site_enabled': true,
      'web_player_enabled': true,
      'website_enabled': true,
      'app_enabled': true,
      'player_enabled': true,
      'admin_enabled': true,
    };
  }

  Future<void> _toggleComponent(String key, bool newValue) async {
    setState(() => _componentLoading[key] = true);
    final ok = await MasterApiClient.setComponentStatus(key, newValue);
    if (!mounted) return;
    if (ok) {
      // Optimistically update local state
      final comps = Map<String, dynamic>.from(_statusData['components'] ?? {});
      comps[key] = newValue;
      setState(() { _statusData = Map<String, dynamic>.from(_statusData)..['components'] = comps; });
      _showToast(newValue ? 'تم تفعيل المكون بنجاح' : 'تم إيقاف المكون بنجاح');
    } else {
      _showToast('فشل تنفيذ الأمر، تأكد من الاتصال');
    }
    setState(() => _componentLoading.remove(key));
    // Refresh real state after a moment
    await Future.delayed(const Duration(seconds: 2));
    if (mounted) _refreshStatus(silent: true);
  }

  // ── Action 1: Emergency Freeze (all) ──
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
            const Text('سيتم إيقاف الموقع والتطبيق والمشغل فوراً عند جميع المستخدمين وعرض الرسالة التالية:', style: TextStyle(fontSize: 13, color: Color(0xFF8E95AA))),
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

  // ── Action 2: Nuclear Wipe ──
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
            Expanded(child: Text('عملية التدمير الشامل', style: TextStyle(color: Colors.redAccent, fontSize: 18, fontWeight: FontWeight.bold))),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'تحذير شديد الخطورة:\n\n'
              '1. سيتم تصفير وحذف كافة السيرفرات والقنوات من Firebase نهائياً.\n'
              '2. سيتم إيقاف الموقع الرسمي والتطبيق والمشغل ولوحة الأدمن فوراً.\n'
              '3. سيتم إرسال أمر تدمير لكل هواتف المستخدمين لمسح الكاش المحلي.\n\n'
              'لتأكيد التدمير، اكتب كلمة WIPE أدناه:',
              style: TextStyle(fontSize: 13, color: Colors.white70, height: 1.5),
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
            child: const Text('تدمير كل شيء فوراً'),
          ),
        ],
      ),
    );
    if (confirm == true) {
      setState(() => _isLoading = true);
      await SecurityVault.wipeVault();
      final ok = await MasterApiClient.triggerNuclearWipe();
      if (!mounted) return;
      _showToast(ok ? 'تم تدمير ومسح كل شيء نهائياً بلا رجعة' : 'فشل تنفيذ أمر المسح');
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
          'سيتم تفعيل الموقع الرسمي والتطبيق والمشغل ولوحة الأدمن بالكامل، وإعادة رفع السيرفرات المشفرة وتشغيل المنظومة بكامل طاقتها.',
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
    String statusSubtitle = 'جميع السيرفرات والمشغلات تعمل بشكل طبيعي';

    if (status == 'frozen') {
      statusColor = const Color(0xFFD4AF37);
      statusTitle = 'المنظومة مجمدة مؤقتاً';
      statusSubtitle = _statusData['message']?.toString() ?? 'التطبيق متوقف للصيانة';
    } else if (status == 'wiped') {
      statusColor = Colors.redAccent;
      statusTitle = 'تم المسح والتدمير بالكامل';
      statusSubtitle = 'البيانات صفرية وجميع أجهزة المستخدمين محظورة';
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
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // ── Live Status Card ──
                    Container(
                      padding: const EdgeInsets.all(20),
                      decoration: BoxDecoration(
                        color: const Color(0xFF141824),
                        borderRadius: BorderRadius.circular(18),
                        border: Border.all(color: statusColor, width: 2),
                        boxShadow: [BoxShadow(color: statusColor.withValues(alpha: 0.2), blurRadius: 15, spreadRadius: 1)],
                      ),
                      child: Column(
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Container(width: 12, height: 12, decoration: BoxDecoration(shape: BoxShape.circle, color: statusColor)),
                              const SizedBox(width: 10),
                              Flexible(child: Text(statusTitle, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: statusColor))),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Text(statusSubtitle, textAlign: TextAlign.center, style: const TextStyle(fontSize: 13, color: Color(0xFF8E95AA))),
                        ],
                      ),
                    ),

                    const SizedBox(height: 28),

                    // ── Per-Component Toggles Section ──
                    Container(
                      padding: const EdgeInsets.all(16),
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
                              Icon(Icons.toggle_on_rounded, color: Color(0xFFD4AF37), size: 22),
                              SizedBox(width: 8),
                              Text('التحكم في مكونات المنظومة', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white)),
                            ],
                          ),
                          const SizedBox(height: 4),
                          const Text('تفعيل أو إيقاف كل جزء من المشروع بشكل منفصل فوراً', style: TextStyle(fontSize: 12, color: Color(0xFF8E95AA))),
                          const SizedBox(height: 16),
                          ...(_components.map((comp) {
                            final key = comp['key'] as String;
                            final isEnabled = compState[key] ?? true;
                            final isLoading = _componentLoading[key] == true;
                            final color = comp['color'] as Color;
                            return Container(
                              margin: const EdgeInsets.only(bottom: 10),
                              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                              decoration: BoxDecoration(
                                color: const Color(0xFF0F131E),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(color: isEnabled ? color.withValues(alpha: 0.4) : const Color(0xFF2A2A3D)),
                              ),
                              child: Row(
                                children: [
                                  Container(
                                    width: 38, height: 38,
                                    decoration: BoxDecoration(shape: BoxShape.circle, color: (isEnabled ? color : const Color(0xFF555E75)).withValues(alpha: 0.15)),
                                    child: Icon(comp['icon'] as IconData, color: isEnabled ? color : const Color(0xFF555E75), size: 20),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(comp['label'] as String, style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: isEnabled ? Colors.white : const Color(0xFF8E95AA))),
                                        Text(comp['subtitle'] as String, style: const TextStyle(fontSize: 11, color: Color(0xFF555E75))),
                                      ],
                                    ),
                                  ),
                                  if (isLoading)
                                    const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFFD4AF37)))
                                  else
                                    Switch(
                                      value: isEnabled,
                                      onChanged: (val) => _toggleComponent(key, val),
                                      activeColor: color, activeTrackColor: color.withValues(alpha: 0.5),
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

                    const SizedBox(height: 24),
                    const Text('أزرار التحكم الكاملة في المنظومة:', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white)),
                    const SizedBox(height: 14),

                    // ── Freeze All Button ──
                    _buildControlCard(
                      title: 'إيقاف وتجميد مؤقت (Freeze All)',
                      subtitle: 'إيقاف الموقع والتطبيق والمشغل ولوحة الأدمن فوراً عند جميع المستخدمين مع رسالة صيانة.',
                      color: const Color(0xFFD4AF37),
                      btnText: 'تجميد كل المنظومة',
                      onTap: _confirmFreeze,
                    ),
                    const SizedBox(height: 16),

                    // ── Nuclear Wipe ──
                    _buildControlCard(
                      title: 'تدمير ومسح شامل (Nuclear Wipe)',
                      subtitle: 'مسح كل شيء من فايربيس وإيقاف الموقع والمشغل والتطبيق نهائياً وإرسال أمر مسح لكل هواتف المستخدمين.',
                      color: Colors.redAccent,
                      btnText: 'مسح وتدمير كل شيء (عليا وعلى أعدائي)',
                      isDanger: true,
                      onTap: _confirmNuclearWipe,
                    ),
                    const SizedBox(height: 16),

                    // ── Full Restore ──
                    _buildControlCard(
                      title: 'استعادة وتشغيل كامل (Full Restore)',
                      subtitle: 'تفعيل الموقع والتطبيق والمشغل ولوحة الأدمن بالكامل وإعادة رفع السيرفرات المشفرة.',
                      color: const Color(0xFF27AE60),
                      btnText: 'إعادة التشغيل الآن',
                      onTap: _confirmRestore,
                    ),

                    const SizedBox(height: 30),
                    // Footer
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: const Color(0xFF0F131E),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: const Color(0xFF242A3D)),
                      ),
                      child: const Row(
                        children: [
                          Icon(Icons.lock_rounded, size: 20, color: Color(0xFFD4AF37)),
                          SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              'المنظومة محمية بتشفير AES-256 وقواعد أمان Firebase الصارمة ضد أي استخراج.',
                              style: TextStyle(fontSize: 12, color: Color(0xFF8E95AA)),
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

  Widget _buildControlCard({
    required String title,
    required String subtitle,
    required Color color,
    required String btnText,
    required VoidCallback onTap,
    bool isDanger = false,
  }) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFF141824),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(title, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: color)),
          const SizedBox(height: 6),
          Text(subtitle, style: const TextStyle(fontSize: 12.5, color: Color(0xFF8E95AA), height: 1.4)),
          const SizedBox(height: 14),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: color,
              foregroundColor: isDanger || color == const Color(0xFFD4AF37) ? Colors.black : Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 12),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
            onPressed: onTap,
            child: Text(btnText, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}
