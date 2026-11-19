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
// 1. Master Security PIN Screen (حماية كاملة ببصمة ورمز)
// ─────────────────────────────────────────────────────────────────────────────
class MasterAuthScreen extends StatefulWidget {
  const MasterAuthScreen({super.key});

  @override
  State<MasterAuthScreen> createState() => _MasterAuthScreenState();
}

class _MasterAuthScreenState extends State<MasterAuthScreen> {
  String _enteredPin = '';
  bool _hasError = false;

  void _onDigit(String d) {
    if (_enteredPin.length < 6) {
      setState(() {
        _enteredPin += d;
        _hasError = false;
      });
      if (_enteredPin.length == 6) {
        _verifyPin();
      }
    }
  }

  void _onDelete() {
    if (_enteredPin.isNotEmpty) {
      setState(() {
        _enteredPin = _enteredPin.substring(0, _enteredPin.length - 1);
        _hasError = false;
      });
    }
  }

  Future<void> _verifyPin() async {
    final valid = await SecurityVault.verifyPin(_enteredPin);
    if (!mounted) return;
    if (valid) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => MasterDashboardScreen(masterPin: _enteredPin),
        ),
      );
    } else {
      setState(() {
        _hasError = true;
        _enteredPin = '';
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('الرمز السري غير صحيح! الرمز الافتراضي: 999888', textAlign: TextAlign.center),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 28.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Spacer(),
              Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: const Color(0xFFB94E3C).withValues(alpha: 0.15),
                  border: Border.all(color: const Color(0xFFB94E3C), width: 2),
                ),
                child: const Icon(Icons.shield_rounded, size: 38, color: Color(0xFFD4AF37)),
              ),
              const SizedBox(height: 18),
              const Text(
                'لوحة التحكم الإداري الأعلى',
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white),
              ),
              const SizedBox(height: 6),
              const Text(
                'أدخل الرمز السري الرئيسي (Master PIN)',
                style: TextStyle(fontSize: 13, color: Color(0xFF8E95AA)),
              ),
              const SizedBox(height: 28),
              // Dots indicator
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(6, (i) {
                  final filled = i < _enteredPin.length;
                  return AnimatedContainer(
                    duration: const Duration(milliseconds: 180),
                    margin: const EdgeInsets.symmetric(horizontal: 7),
                    width: filled ? 18 : 14,
                    height: filled ? 18 : 14,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: _hasError
                          ? Colors.redAccent
                          : filled
                              ? const Color(0xFFD4AF37)
                              : const Color(0xFF242A3D),
                      boxShadow: filled
                          ? [
                              BoxShadow(
                                color: const Color(0xFFD4AF37).withValues(alpha: 0.4),
                                blurRadius: 8,
                                spreadRadius: 1,
                              )
                            ]
                          : null,
                    ),
                  );
                }),
              ),
              const Spacer(),
              // Numpad
              _buildNumpad(),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildNumpad() {
    final rows = [
      ['1', '2', '3'],
      ['4', '5', '6'],
      ['7', '8', '9'],
      ['', '0', 'del'],
    ];

    return Column(
      children: rows.map((r) {
        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: r.map((cell) {
            if (cell.isEmpty) return const SizedBox(width: 76, height: 76);
            if (cell == 'del') {
              return SizedBox(
                width: 76,
                height: 76,
                child: IconButton(
                  icon: const Icon(Icons.backspace_outlined, color: Colors.white70),
                  onPressed: _onDelete,
                ),
              );
            }
            return Container(
              margin: const EdgeInsets.all(7),
              width: 74,
              height: 74,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF141824),
                  foregroundColor: Colors.white,
                  shape: const CircleBorder(),
                  elevation: 0,
                  side: const BorderSide(color: Color(0xFF242A3D)),
                ),
                onPressed: () => _onDigit(cell),
                child: Text(cell, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
              ),
            );
          }).toList(),
        );
      }).toList(),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Master Dashboard Screen (التحكم والتدمير الشامل)
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
  Timer? _pollTimer;

  @override
  void initState() {
    super.initState();
    _refreshStatus();
    _pollTimer = Timer.periodic(const Duration(seconds: 8), (_) => _refreshStatus(silent: true));
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
    setState(() {
      _statusData = data;
      _isLoading = false;
    });
  }

  String get _currentStatus => _statusData['status']?.toString().toLowerCase() ?? 'active';

  // ── Action 1: Emergency Freeze ──
  Future<void> _confirmFreeze() async {
    final msgCtrl = TextEditingController(text: 'عفواً، الخدمة متوقفة حالياً للتحديث والصيانة.');
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1A1F2E),
        title: const Text('🛑 إيقاف وتجميد مؤقت للمنظومة', style: TextStyle(color: Colors.white, fontSize: 18)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'سيتم قفل التطبيق والمشغل في نفس اللحظة عند جميع المستخدمين وعرض الرسالة التالية:',
              style: TextStyle(fontSize: 13, color: Color(0xFF8E95AA)),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: msgCtrl,
              maxLines: 2,
              decoration: InputDecoration(
                filled: true,
                fillColor: const Color(0xFF10131E),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
              ),
            ),
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
      _showToast(ok ? 'تم تجميد المنظومة بنجاح' : 'فشل تنفيذ الأمر، تأكد من الاتصال');
      _refreshStatus();
    }
  }

  // ── Action 2: Nuclear Zero Wipe (عليا وعلى أعدائي) ──
  Future<void> _confirmNuclearWipe() async {
    final confirmCtrl = TextEditingController();
    final confirm = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF220C0C),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
          side: const BorderSide(color: Colors.redAccent, width: 2),
        ),
        title: const Row(
          children: [
            Icon(Icons.warning_amber_rounded, color: Colors.redAccent, size: 28),
            SizedBox(width: 8),
            Text('💥 عملية التدمير الشامل', style: TextStyle(color: Colors.redAccent, fontSize: 18, fontWeight: FontWeight.bold)),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '⚠️ تحذير شديد الخطورة:\n\n'
              '1. سيتم تصفير وحذف كافة السيرفرات والقنوات من Firebase.\n'
              '2. سيتم إرسال أمر تدمير فوري لكل هواتف المستخدمين لمسح الكاش المحلي وإغلاق التطبيق.\n'
              '3. سيتم حفظ نسخة احتياطية مشفرة فقط على هاتفك هذا لضمان استعادتها لاحقاً.\n\n'
              'لتأكيد التدمير، اكتب كلمة WIPE أدناه:',
              style: TextStyle(fontSize: 13, color: Colors.white70, height: 1.5),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: confirmCtrl,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.redAccent, fontWeight: FontWeight.bold, fontSize: 18),
              decoration: InputDecoration(
                hintText: 'اكتب WIPE للتأكيد',
                filled: true,
                fillColor: const Color(0xFF140808),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('تراجع')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
            onPressed: () {
              if (confirmCtrl.text.trim().toUpperCase() == 'WIPE') {
                Navigator.pop(ctx, true);
              }
            },
            child: const Text('تدمير ومسح كل شيء فوراً'),
          ),
        ],
      ),
    );

    if (confirm == true) {
      setState(() => _isLoading = true);
      // 1. First save local encrypted backup vault of servers
      await SecurityVault.saveVault(widget.masterPin, {
        'timestamp': DateTime.now().millisecondsSinceEpoch,
        'catalog': {'verified': true, 'auto_restore': true},
        'servers': {
          'server1': 'https://primary-stream-server.com',
          'server2': 'https://secondary-stream-server.com',
        }
      });

      // 2. Dispatch Nuclear Wipe Command
      final ok = await MasterApiClient.triggerNuclearWipe();
      if (!mounted) return;
      _showToast(ok ? '💥 تم تدمير ومسح السحابة والكاش من جميع الأجهزة' : 'فشل تنفيذ أمر المسح');
      _refreshStatus();
    }
  }

  // ── Action 3: Full Restore ──
  Future<void> _confirmRestore() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF0F2418),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
          side: const BorderSide(color: Color(0xFF27AE60), width: 1.5),
        ),
        title: const Row(
          children: [
            Icon(Icons.restart_alt_rounded, color: Color(0xFF27AE60), size: 28),
            SizedBox(width: 8),
            Text('🟢 استعادة وتشغيل كل شيء', style: TextStyle(color: Color(0xFF27AE60), fontSize: 18, fontWeight: FontWeight.bold)),
          ],
        ),
        content: const Text(
          'سيتم إعادة رفع السيرفرات المشفرة من هاتفك إلى Firebase وتفعيل حالة المنظومة (ACTIVE) لتعود كل التطبيقات والمشغلات للعمل فوراً.',
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
      final vaultData = await SecurityVault.loadVault(widget.masterPin) ?? {
        'servers': {},
        'catalog': {},
      };
      final ok = await MasterApiClient.triggerRestore(serversData: vaultData);
      if (!mounted) return;
      _showToast(ok ? '🟢 تم استعادة المنظومة وتشغيلها بالكامل!' : 'فشل أمر الاستعادة');
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
    String statusTitle = 'المنظومة نشطة وشغالة 🟢';
    String statusSubtitle = 'جميع السيرفرات والمشغلات تعمل بشكل طبيعي';

    if (status == 'frozen') {
      statusColor = const Color(0xFFD4AF37);
      statusTitle = 'المنظومة مجمدة مؤقتاً 🟡';
      statusSubtitle = _statusData['message']?.toString() ?? 'التطبيق متوقف للصيانة';
    } else if (status == 'wiped') {
      statusColor = Colors.redAccent;
      statusTitle = 'تم المسح والتدمير بالكامل 🔴';
      statusSubtitle = 'البيانات صفرية وجميع أجهزة المستخدمين محظورة';
    } else if (status == 'unknown') {
      statusColor = const Color(0xFFE67E22);
      statusTitle = 'تعذر الاتصال بالسحابة 🟠';
      statusSubtitle = _statusData['message']?.toString() ?? 'تأكد من اتصالك بالإنترنت';
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Zaqzouka Master Control', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
        centerTitle: true,
        backgroundColor: const Color(0xFF10131E),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            onPressed: () => _refreshStatus(),
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFB94E3C)))
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // 1. Live Status Card
                  Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: const Color(0xFF141824),
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: statusColor, width: 2),
                      boxShadow: [
                        BoxShadow(color: statusColor.withValues(alpha: 0.2), blurRadius: 15, spreadRadius: 1),
                      ],
                    ),
                    child: Column(
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Container(width: 12, height: 12, decoration: BoxDecoration(shape: BoxShape.circle, color: statusColor)),
                            const SizedBox(width: 10),
                            Text(statusTitle, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: statusColor)),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(statusSubtitle, textAlign: TextAlign.center, style: const TextStyle(fontSize: 13, color: Color(0xFF8E95AA))),
                      ],
                    ),
                  ),

                  const SizedBox(height: 28),
                  const Text('أزرار التحكم الفوري في المنظومة:', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white)),
                  const SizedBox(height: 14),

                  // Button 1: Freeze
                  _buildControlCard(
                    title: '🛑 إيقاف وتجميد مؤقت (Freeze)',
                    subtitle: 'إيقاف المشغل والتطبيق فوراً عند جميع المستخدمين مع رسالة صيانة.',
                    color: const Color(0xFFD4AF37),
                    btnText: 'تجميد المنظومة',
                    onTap: _confirmFreeze,
                  ),

                  const SizedBox(height: 16),

                  // Button 2: Nuclear Wipe (عليا وعلى أعدائي)
                  _buildControlCard(
                    title: '💥 تدمير ومسح شامل (Nuclear Wipe)',
                    subtitle: 'مسح كل شيء من فايربيس ومسح الكاش المحلي من هواتف كل الناس نهائياً.',
                    color: Colors.redAccent,
                    btnText: 'مسح وتدمير كل شيء (عليا وعلى أعدائي)',
                    isDanger: true,
                    onTap: _confirmNuclearWipe,
                  ),

                  const SizedBox(height: 16),

                  // Button 3: Restore
                  _buildControlCard(
                    title: '🟢 استعادة وتشغيل كامل (Full Restore)',
                    subtitle: 'إعادة رفع السيرفرات المشفرة وتفعيل المنظومة لتعمل بكامل طاقتها مجدداً.',
                    color: const Color(0xFF27AE60),
                    btnText: 'إعادة التشغيل الآن',
                    onTap: _confirmRestore,
                  ),

                  const SizedBox(height: 30),
                  // Security info footer
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
