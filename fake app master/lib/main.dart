import 'dart:async';
import 'package:flutter/material.dart';

/// -----------------------------------------------------------------------------
/// ⚠️ FAKE / SIMULATED MASTER CONTROL APP (تطبيق وهمي للمعاينة والمحاكاة فقط)
/// -----------------------------------------------------------------------------
/// هذا التطبيق وهمي بالكامل (Mock / Simulation Only)
/// لا يقوم بأي اتصال حقيقي بالسحابة ولا يحذف أو يمسح أي بيانات فعلية نهائياً.
/// مخصص للإهداء والمحاكاة الآمنة دون أي صلاحيات حقيقية.
/// -----------------------------------------------------------------------------

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const FakeMasterControlApp());
}

class FakeMasterControlApp extends StatelessWidget {
  const FakeMasterControlApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Zaqzouka Master Control',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0C0E14),
        fontFamily: 'sans-serif',
      ),
      home: const FakePinScreen(),
    );
  }
}

// =============================================================================
// شاشة إدخال الـ PIN (الانترو) - مطابقة للأصلي
// =============================================================================
class FakePinScreen extends StatefulWidget {
  const FakePinScreen({super.key});

  @override
  State<FakePinScreen> createState() => _FakePinScreenState();
}

class _FakePinScreenState extends State<FakePinScreen> {
  static const String _kCorrectPin = '888999';
  final List<String> _digits = [];
  bool _hasError = false;
  bool _isAuthenticating = false;

  void _onDigitPressed(String digit) {
    if (_digits.length >= 6) return;
    setState(() {
      _digits.add(digit);
      _hasError = false;
    });
    if (_digits.length == 6) _verifyPin();
  }

  void _onDelete() {
    if (_digits.isEmpty) return;
    setState(() => _digits.removeLast());
  }

  Future<void> _verifyPin() async {
    setState(() => _isAuthenticating = true);
    await Future.delayed(const Duration(milliseconds: 700));
    if (!mounted) return;
    final pin = _digits.join();
    if (pin == _kCorrectPin) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const FakeMasterDashboardScreen()),
      );
    } else {
      setState(() {
        _hasError = true;
        _isAuthenticating = false;
        _digits.clear();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0C0E14),
      body: Directionality(
        textDirection: TextDirection.rtl,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                // ── لوجو ──
                Container(
                  width: 80,
                  height: 80,
                  decoration: BoxDecoration(
                    color: const Color(0xFF141724),
                    shape: BoxShape.circle,
                    border: Border.all(color: const Color(0xFF2D6A4F), width: 2),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFF27AE60).withOpacity(0.25),
                        blurRadius: 24,
                        spreadRadius: 4,
                      ),
                    ],
                  ),
                  child: const Icon(Icons.shield_rounded, color: Color(0xFF27AE60), size: 40),
                ),
                const SizedBox(height: 20),
                const Text(
                  'Zaqzouka Master Control',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                    letterSpacing: 0.5,
                  ),
                ),
                const SizedBox(height: 6),
                const Text(
                  'أدخل رمز الأمان للدخول',
                  style: TextStyle(fontSize: 14, color: Color(0xFF7A8094)),
                ),
                const SizedBox(height: 36),

                // ── نقاط الـ PIN ──
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: List.generate(6, (i) {
                    final filled = i < _digits.length;
                    return Container(
                      margin: const EdgeInsets.symmetric(horizontal: 8),
                      width: 18,
                      height: 18,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: filled
                            ? (_hasError ? Colors.redAccent : const Color(0xFF27AE60))
                            : const Color(0xFF2A2D3D),
                        border: Border.all(
                          color: _hasError
                              ? Colors.redAccent
                              : filled
                                  ? const Color(0xFF27AE60)
                                  : const Color(0xFF3D4257),
                        ),
                        boxShadow: filled
                            ? [
                                BoxShadow(
                                  color: (_hasError ? Colors.redAccent : const Color(0xFF27AE60)).withOpacity(0.6),
                                  blurRadius: 8,
                                )
                              ]
                            : [],
                      ),
                    );
                  }),
                ),

                if (_hasError) ...[
                  const SizedBox(height: 14),
                  const Text(
                    '❌ رمز الأمان غير صحيح. حاول مرة أخرى.',
                    style: TextStyle(color: Colors.redAccent, fontSize: 13),
                    textAlign: TextAlign.center,
                  ),
                ],
                if (_isAuthenticating) ...[
                  const SizedBox(height: 14),
                  const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                      valueColor: AlwaysStoppedAnimation<Color>(Color(0xFF27AE60)),
                    ),
                  ),
                ],

                const SizedBox(height: 36),

                // ── لوحة المفاتيح ──
                _buildKeypad(),

                const SizedBox(height: 24),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.lock_rounded, color: Color(0xFF4A5568), size: 14),
                    const SizedBox(width: 6),
                    Text(
                      'الصارمة Firebase وقواعد أمان AES-256 المنظومة محمية بتشفير\nضد أي استخراج.',
                      style: const TextStyle(fontSize: 11, color: Color(0xFF4A5568)),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildKeypad() {
    final keys = [
      ['1', '2', '3'],
      ['4', '5', '6'],
      ['7', '8', '9'],
      ['', '0', 'del'],
    ];
    return Column(
      children: keys.map((row) {
        return Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: row.map((k) {
            if (k == '') return const SizedBox(width: 80, height: 68);
            if (k == 'del') {
              return _KeypadButton(
                label: '⌫',
                onTap: _onDelete,
                isSpecial: true,
              );
            }
            return _KeypadButton(label: k, onTap: () => _onDigitPressed(k));
          }).toList(),
        );
      }).toList(),
    );
  }
}

class _KeypadButton extends StatelessWidget {
  final String label;
  final VoidCallback onTap;
  final bool isSpecial;

  const _KeypadButton({required this.label, required this.onTap, this.isSpecial = false});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 80,
        height: 68,
        margin: const EdgeInsets.all(6),
        decoration: BoxDecoration(
          color: isSpecial ? const Color(0xFF181C2A) : const Color(0xFF1C1F2E),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: const Color(0xFF2D3250), width: 1),
        ),
        child: Center(
          child: Text(
            label,
            style: TextStyle(
              fontSize: isSpecial ? 22 : 24,
              fontWeight: FontWeight.w600,
              color: isSpecial ? Colors.white54 : Colors.white,
            ),
          ),
        ),
      ),
    );
  }
}

// =============================================================================
// شاشة التحكم الرئيسية (الداشبورد) - بالـ 3 أزرار
// =============================================================================
class FakeMasterDashboardScreen extends StatefulWidget {
  const FakeMasterDashboardScreen({super.key});

  @override
  State<FakeMasterDashboardScreen> createState() => _FakeMasterDashboardScreenState();
}

class _FakeMasterDashboardScreenState extends State<FakeMasterDashboardScreen> {
  // حالة المنظومة: active | frozen | wiped
  String _systemStatus = 'active';
  bool _isLoading = false;

  // للمسح الوهمي
  bool _isWipingInProgress = false;
  double _wipeProgress = 0.0;
  List<String> _terminalLogs = [];

  // للصيانة الوهمية
  bool _isFreezingInProgress = false;

  Timer? _statusPulseTimer;

  @override
  void initState() {
    super.initState();
    _statusPulseTimer = Timer.periodic(const Duration(seconds: 4), (_) {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _statusPulseTimer?.cancel();
    super.dispose();
  }

  // ---------------------------------------------------------------------------
  // زر الصيانة (Freeze) - يشتغل فعلاً: يجمد ثم يرجع بعد 5 ثواني
  // ---------------------------------------------------------------------------
  Future<void> _doFreeze() async {
    if (_systemStatus != 'active') return;

    final confirm = await _showSimpleConfirmDialog(
      title: '⏸ تجميد المنظومة مؤقتاً',
      message: 'سيتم إيقاف المنظومة مؤقتاً وإظهار رسالة صيانة لجميع المستخدمين.\n\nهل أنت متأكد؟',
      confirmText: 'تجميد الآن',
      confirmColor: const Color(0xFFE6A817),
    );
    if (confirm != true) return;

    setState(() {
      _isFreezingInProgress = true;
    });

    // محاكاة إرسال الأمر
    await Future.delayed(const Duration(milliseconds: 1200));
    if (!mounted) return;

    setState(() {
      _systemStatus = 'frozen';
      _isFreezingInProgress = false;
    });

    _showToast('تم تجميد المنظومة بنجاح');

    // بعد 8 ثواني يرجع تلقائياً كأنه استعادة
    await Future.delayed(const Duration(seconds: 8));
    if (!mounted) return;
    if (_systemStatus == 'frozen') {
      setState(() => _systemStatus = 'active');
      _showToast('تم استعادة المنظومة تلقائياً');
    }
  }

  // ---------------------------------------------------------------------------
  // زر الاستعادة (Restore) - فيك يظهر progress ثم يرجع active
  // ---------------------------------------------------------------------------
  Future<void> _doRestore() async {
    if (_systemStatus == 'wiped') {
      _showToast('المنظومة ممسوحة نهائياً، لا يمكن استعادتها.');
      return;
    }
    if (_systemStatus == 'active') {
      _showToast('المنظومة تعمل بالفعل بشكل طبيعي.');
      return;
    }

    setState(() => _isLoading = true);
    await Future.delayed(const Duration(milliseconds: 1500));
    if (!mounted) return;

    setState(() {
      _systemStatus = 'active';
      _isLoading = false;
    });
    _showToast('✅ تم استعادة وتشغيل المنظومة بالكامل');
  }

  // ---------------------------------------------------------------------------
  // زر التدمير (Nuclear Wipe) - المسرحية الكاملة (وهمي 100%)
  // ---------------------------------------------------------------------------
  Future<void> _startNuclearWipeSequence() async {
    if (_systemStatus == 'wiped') {
      _showToast('المنظومة بالفعل تم تدميرها ومسحها نهائياً.');
      return;
    }

    // ── خطوة 1 ──
    final pass1 = await _showPasswordDialog(
      stepTitle: 'خطوة 1 من 5 : كود الأمان الأساسي',
      subtitle: 'أدخل كود الأمان الأساسي لبدء إجراءات تدمير المنظومة (Security Key 1):',
      hintText: 'أدخل كلمة السر الأولى...',
    );
    if (pass1 == null) return;
    if (pass1.trim() != 'yourphonesave') {
      _showErrorDialog('❌ كلمة السر غير صحيحة!', 'كود الأمان الأساسي غير مطابق. تم رفض العملية وإلغاء أمر المسح حفاظاً على المنظومة.');
      return;
    }

    // ── خطوة 2 ──
    final pass2 = await _showPasswordDialog(
      stepTitle: 'خطوة 2 من 5 : كود المصادقة الثنائي',
      subtitle: 'أدخل كود المصادقة الثنائي المشفر للمشرف (Secondary Key 2):',
      hintText: 'أدخل كلمة السر الثانية...',
    );
    if (pass2 == null) return;
    if (pass2.trim() != 'myphonesave') {
      _showErrorDialog('❌ كود المصادقة خاطئ!', 'كود المصادقة الثنائي غير صحيح. تم إيقاف عملية المسح.');
      return;
    }

    // ── خطوة 3 ──
    final pass3 = await _showPasswordDialog(
      stepTitle: 'خطوة 3 من 5 : كود التفويض السحابي الأقصى',
      subtitle: 'أدخل كود التفويض الرقمي النهائي (Root Authorization Code):',
      hintText: 'أدخل الأرقام السرية...',
      isNumeric: true,
    );
    if (pass3 == null) return;
    if (pass3.trim() != '19873548652') {
      _showErrorDialog('❌ كود التفويض غير صحيح!', 'الرمز الرقمي المالي غير مطابق لكود السحابة. تم رفض الأمر.');
      return;
    }

    // ── خطوة 4: تأكيد عربي ──
    final arabicConfirm = await _showTextPromptDialog(
      stepTitle: 'خطوة 4 من 5 : تأكيد تدمير كل ما يخص زقزوقة',
      warningText: '⚠️ تحذير نهائي:\nهل أنت متأكد تماماً أنك تريد مسح كل شيء يخص زقزوقة من السيرفرات والموقع وتطبيقات الهواتف نهائياً؟\n\nلتأكيد المسح، اكتب كلمة (تأكيد) في الحقل أدناه:',
      expectedText: 'تأكيد',
    );
    if (arabicConfirm != true) {
      _showToast('تم إلغاء عملية التدمير.');
      return;
    }

    // ── خطوة 5: WIPE ──
    final wipeConfirm = await _showTextPromptDialog(
      stepTitle: 'خطوة 5 من 5 : تأكيد التنفيذ الفوري (WIPE)',
      warningText: '🔥 الإجراء الأخير لا رجعة فيه إطلاقاً:\nاكتب كلمة WIPE بأحرف كبيرة لإعطاء الأمر السحابي الفوري بالتدمير الشامل:',
      expectedText: 'WIPE',
    );
    if (wipeConfirm != true) {
      _showToast('تم إلغاء العملية.');
      return;
    }

    // ── تنفيذ المحاكاة الدرامية ──
    await _executeSimulatedWipe();
  }

  Future<void> _executeSimulatedWipe() async {
    setState(() {
      _isWipingInProgress = true;
      _wipeProgress = 0.0;
      _terminalLogs = [];
    });

    final steps = [
      '⚡ [INITIALIZE] بدء بروتوكول التدمير النهائي للمنظومة...',
      '🔗 [CONNECT] الاتصال بالسيرفرات السحابية وقواعد بيانات Firebase...',
      '🌐 [WEBSITE] إرسال أمر مسح نطاق وكود منصة زقزوقة (web-nu-teal-29)... تم التصفير',
      '🗄️ [DATABASE] تفريغ وحذف جداول الأفلام والمسلسلات والمستخدمين... [100% OK]',
      '📡 [SERVERS] إيقاف وتصفير سيرفرات البث والقنوات الرياضية (beIN / Arab)... [توقف تام]',
      '📱 [CLIENTS] إرسال أمر Nuclear Wipe إلى 14,850 هاتف متصل لمسح الكاش وحظر المشغل...',
      '🔒 [SECURITY] تدمير المفاتيح وتصفير الحسابات السحابية نهائياً...',
      '✅ [COMPLETED] تم إزالة ومسح منظومة زقزوقة بالكامل من الوجود بنجاح تام!',
    ];

    for (int i = 0; i < steps.length; i++) {
      await Future.delayed(const Duration(milliseconds: 900));
      if (!mounted) return;
      setState(() {
        _terminalLogs.add(steps[i]);
        _wipeProgress = (i + 1) / steps.length;
      });
    }

    await Future.delayed(const Duration(milliseconds: 600));
    if (!mounted) return;

    setState(() {
      _isWipingInProgress = false;
      _systemStatus = 'wiped';
    });

    _showSuccessWipeDialog();
  }

  // ---------------------------------------------------------------------------
  // نوافذ الحوارات
  // ---------------------------------------------------------------------------
  Future<bool?> _showSimpleConfirmDialog({
    required String title,
    required String message,
    required String confirmText,
    required Color confirmColor,
  }) {
    return showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF141724),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: BorderSide(color: confirmColor.withOpacity(0.5), width: 1.5),
        ),
        title: Text(title,
            style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: confirmColor),
            textDirection: TextDirection.rtl),
        content: Text(message,
            style: const TextStyle(fontSize: 13, color: Colors.white70, height: 1.5),
            textDirection: TextDirection.rtl),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('إلغاء', style: TextStyle(color: Colors.white54)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: confirmColor,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(confirmText, style: const TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  Future<String?> _showPasswordDialog({
    required String stepTitle,
    required String subtitle,
    required String hintText,
    bool isNumeric = false,
  }) async {
    final controller = TextEditingController();
    bool obscure = true;

    return showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDlgState) => AlertDialog(
          backgroundColor: const Color(0xFF141724),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
            side: const BorderSide(color: Color(0xFFE53935), width: 1.5),
          ),
          title: Row(
            children: [
              const Icon(Icons.shield_rounded, color: Color(0xFFE53935), size: 24),
              const SizedBox(width: 8),
              Expanded(
                child: Text(stepTitle,
                    style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white),
                    textDirection: TextDirection.rtl),
              ),
            ],
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(subtitle,
                  style: const TextStyle(fontSize: 12.5, color: Color(0xFFB0B7C3), height: 1.4),
                  textDirection: TextDirection.rtl),
              const SizedBox(height: 16),
              TextField(
                controller: controller,
                obscureText: obscure,
                keyboardType: isNumeric ? TextInputType.number : TextInputType.text,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold, letterSpacing: 1.5),
                decoration: InputDecoration(
                  hintText: hintText,
                  hintStyle: const TextStyle(color: Colors.white30, fontSize: 13),
                  filled: true,
                  fillColor: const Color(0xFF0B0D14),
                  suffixIcon: IconButton(
                    icon: Icon(obscure ? Icons.visibility : Icons.visibility_off, color: Colors.white54, size: 20),
                    onPressed: () => setDlgState(() => obscure = !obscure),
                  ),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: Color(0xFFE53935))),
                  focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: Color(0xFFE53935), width: 2)),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, null),
              child: const Text('إلغاء الأمر', style: TextStyle(color: Colors.white54)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFE53935),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              onPressed: () => Navigator.pop(ctx, controller.text),
              child: const Text('متابعة التحقق', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  Future<bool?> _showTextPromptDialog({
    required String stepTitle,
    required String warningText,
    required String expectedText,
  }) async {
    final controller = TextEditingController();

    return showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E0E10),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: const BorderSide(color: Color(0xFFFF5252), width: 2),
        ),
        title: Row(
          children: [
            const Icon(Icons.warning_amber_rounded, color: Color(0xFFFF5252), size: 26),
            const SizedBox(width: 8),
            Expanded(
              child: Text(stepTitle,
                  style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Color(0xFFFF5252)),
                  textDirection: TextDirection.rtl),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(warningText,
                style: const TextStyle(fontSize: 13, color: Colors.white70, height: 1.5),
                textDirection: TextDirection.rtl),
            const SizedBox(height: 16),
            TextField(
              controller: controller,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Color(0xFFFF5252), fontSize: 18, fontWeight: FontWeight.w900, letterSpacing: 2),
              decoration: InputDecoration(
                hintText: 'اكتب $expectedText هنا',
                hintStyle: const TextStyle(color: Colors.white24, fontSize: 14),
                filled: true,
                fillColor: const Color(0xFF120708),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: Color(0xFFFF5252))),
                focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: Color(0xFFFF5252), width: 2)),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('تراجع', style: TextStyle(color: Colors.white54)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFD32F2F),
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
            onPressed: () {
              if (controller.text.trim().toUpperCase() == expectedText.trim().toUpperCase()) {
                Navigator.pop(ctx, true);
              } else {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('الكلمة المدخلة غير مطابقة لـ ($expectedText)', textAlign: TextAlign.center)),
                );
              }
            },
            child: const Text('تأكيد التنفيذ', style: TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  void _showErrorDialog(String title, String message) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1F1215),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text(title,
            style: const TextStyle(color: Colors.redAccent, fontSize: 16, fontWeight: FontWeight.bold),
            textDirection: TextDirection.rtl),
        content: Text(message,
            style: const TextStyle(color: Colors.white70, fontSize: 13),
            textDirection: TextDirection.rtl),
        actions: [
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.white10),
            onPressed: () => Navigator.pop(ctx),
            child: const Text('إغلاق'),
          ),
        ],
      ),
    );
  }

  void _showSuccessWipeDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1A1215),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: const BorderSide(color: Colors.redAccent, width: 2),
        ),
        title: const Row(
          children: [
            Icon(Icons.check_circle_rounded, color: Colors.redAccent, size: 28),
            SizedBox(width: 8),
            Text('💥 تم المسح والتدمير بنجاح', style: TextStyle(color: Colors.redAccent, fontSize: 16, fontWeight: FontWeight.bold)),
          ],
        ),
        content: const Text(
          'تم تنفيذ أمر إزالة الموقع والتطبيق من كافة الخوادم وهواتف المستخدمين وقواعد البيانات نهائياً وتصفير كل شيء بنجاح 100%.\n\nالمنظومة الآن معطلة وممسوحة تماماً.',
          style: TextStyle(color: Colors.white, fontSize: 13.5, height: 1.5),
          textDirection: TextDirection.rtl,
        ),
        actions: [
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
            onPressed: () => Navigator.pop(ctx),
            child: const Text('حسناً، تم الإغلاق'),
          ),
        ],
      ),
    );
  }

  void _showToast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg, textAlign: TextAlign.center), duration: const Duration(seconds: 3)),
    );
  }

  // ---------------------------------------------------------------------------
  // الواجهة الرسومية
  // ---------------------------------------------------------------------------
  @override
  Widget build(BuildContext context) {
    final isWiped = _systemStatus == 'wiped';
    final isFrozen = _systemStatus == 'frozen';
    final isActive = _systemStatus == 'active';

    // ألوان بطاقة الحالة
    Color statusColor = isWiped
        ? Colors.redAccent
        : isFrozen
            ? const Color(0xFFE6A817)
            : const Color(0xFF27AE60);

    String statusTitle = isWiped
        ? 'تم المسح والتدمير بالكامل 🔴'
        : isFrozen
            ? 'المنظومة مجمدة مؤقتاً 🟡'
            : 'المنظومة نشطة وشغالة 🟢';

    String statusSubtitle = isWiped
        ? 'تم تصفير وإزالة الموقع والتطبيق من جميع السيرفرات والأجهزة نهائياً.'
        : isFrozen
            ? 'الخدمة متوقفة مؤقتاً للتحديث والصيانة.'
            : 'جميع السيرفرات والمشغلات تعمل بشكل طبيعي';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Zaqzouka Master Control', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
        centerTitle: true,
        backgroundColor: const Color(0xFF10131E),
        elevation: 0,
        actions: [
          IconButton(
            icon: _isLoading
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : const Icon(Icons.refresh_rounded),
            onPressed: () {
              _showToast(isWiped
                  ? 'المنظومة ممسوحة نهائياً'
                  : isFrozen
                      ? 'المنظومة مجمدة حالياً'
                      : 'تم تحديث حالة السيرفرات: متصل 100%');
            },
          ),
        ],
      ),
      body: Directionality(
        textDirection: TextDirection.rtl,
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // ── بطاقة الحالة ──
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: isWiped
                      ? const Color(0xFF1F0D10)
                      : isFrozen
                          ? const Color(0xFF1F1A0D)
                          : const Color(0xFF0F1A15),
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(color: statusColor, width: 1.5),
                  boxShadow: [
                    BoxShadow(
                      color: statusColor.withOpacity(0.12),
                      blurRadius: 16,
                      spreadRadius: 2,
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          width: 14,
                          height: 14,
                          decoration: BoxDecoration(
                            color: statusColor,
                            shape: BoxShape.circle,
                            boxShadow: [
                              BoxShadow(color: statusColor.withOpacity(0.8), blurRadius: 8, spreadRadius: 1),
                            ],
                          ),
                        ),
                        const SizedBox(width: 10),
                        Text(statusTitle,
                            style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: statusColor)),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(statusSubtitle,
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 13, color: isWiped ? Colors.white70 : const Color(0xFFA1A8BA))),
                  ],
                ),
              ),

              const SizedBox(height: 24),

              // ── شريط التقدم للمسح الوهمي ──
              if (_isWipingInProgress) ...[
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFF160A0C),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: Colors.redAccent.withOpacity(0.5)),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text('جاري تنفيذ التدمير السحابي الشامل...', style: TextStyle(color: Colors.redAccent, fontSize: 13, fontWeight: FontWeight.bold)),
                          Text('${(_wipeProgress * 100).toInt()}%', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
                        ],
                      ),
                      const SizedBox(height: 10),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(8),
                        child: LinearProgressIndicator(
                          value: _wipeProgress,
                          backgroundColor: Colors.white10,
                          valueColor: const AlwaysStoppedAnimation<Color>(Colors.redAccent),
                          minHeight: 8,
                        ),
                      ),
                      const SizedBox(height: 12),
                      Container(
                        height: 140,
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(color: const Color(0xFF0A0506), borderRadius: BorderRadius.circular(10)),
                        child: ListView.builder(
                          reverse: true,
                          itemCount: _terminalLogs.length,
                          itemBuilder: (ctx, i) {
                            final idx = _terminalLogs.length - 1 - i;
                            return Padding(
                              padding: const EdgeInsets.symmetric(vertical: 2),
                              child: Text(_terminalLogs[idx],
                                  style: const TextStyle(fontFamily: 'monospace', fontSize: 11, color: Colors.greenAccent)),
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),
              ],

              // ── عنوان القسم ──
              const Text(
                ':أزرار التحكم الفوري في المنظومة',
                style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Color(0xFFB0B7C3)),
              ),
              const SizedBox(height: 14),

              // ── زر 1: الصيانة (Freeze) ──
              _buildControlCard(
                icon: Icons.pause_circle_outline_rounded,
                iconColor: const Color(0xFFE6A817),
                title: '🔴 (Freeze) إيقاف وتجميد مؤقت',
                description: 'إيقاف المشغل والتطبيق فوراً عند جميع المستخدمين مع رسالة صيانة.',
                buttonLabel: isFrozen ? 'المنظومة مجمدة حالياً...' : 'تجميد المنظومة',
                buttonColor: const Color(0xFFE6A817),
                onPressed: (isActive && !_isFreezingInProgress && !_isWipingInProgress) ? _doFreeze : null,
                isLoading: _isFreezingInProgress,
              ),

              const SizedBox(height: 14),

              // ── زر 2: التدمير الشامل (Wipe) - وهمي ──
              _buildControlCard(
                icon: Icons.auto_awesome,
                iconColor: Colors.orangeAccent,
                title: '💥 (Nuclear Wipe) تدمير ومسح شامل',
                description: 'مسح كل شيء من فايريبيس ومسح الكاش المحلي من هواتف كل الناس تهائياً.',
                buttonLabel: isWiped ? 'تم تنفيذ المسح الشامل بنجاح (معطل)' : 'مسح وتدمير كل شيء',
                buttonColor: const Color(0xFFE53935),
                onPressed: (!isWiped && !_isWipingInProgress && !_isFreezingInProgress) ? _startNuclearWipeSequence : null,
              ),

              const SizedBox(height: 14),

              // ── زر 3: الاستعادة (Restore) - فيك ──
              _buildControlCard(
                icon: Icons.play_circle_outline_rounded,
                iconColor: const Color(0xFF27AE60),
                title: '🟢 (Full Restore) استعادة وتشغيل كامل',
                description: 'إعادة رفع السيرفرات المشفرة وتفعيل المنظومة لتعمل بكامل طاقتها مجدداً.',
                buttonLabel: 'إعادة التشغيل الآن',
                buttonColor: const Color(0xFF27AE60),
                onPressed: (isFrozen && !_isLoading) ? _doRestore : null,
              ),

              const SizedBox(height: 30),

              // ── تفاصيل سيرفرات وهمية ──
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: const Color(0xFF111420),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: Colors.white.withOpacity(0.06)),
                ),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('حالة خوادم البث المباشر', style: TextStyle(fontSize: 12, color: Colors.white60)),
                        Text(
                          isWiped ? '0 سيرفر (متوقفة)' : isFrozen ? '8 سيرفرات (مجمدة)' : '8 سيرفرات نشطة',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: isWiped ? Colors.redAccent : isFrozen ? Colors.orangeAccent : Colors.greenAccent,
                          ),
                        ),
                      ],
                    ),
                    const Divider(color: Colors.white12, height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('قاعدة بيانات الأفلام والمسلسلات', style: TextStyle(fontSize: 12, color: Colors.white60)),
                        Text(
                          isWiped ? 'ممسوحة (0 KB)' : 'Firebase RTDB (مشفرة)',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: isWiped ? Colors.redAccent : Colors.amberAccent,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 16),

              // ── شريط الأمان ──
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: const [
                  Icon(Icons.lock_rounded, color: Color(0xFF4A5568), size: 13),
                  SizedBox(width: 6),
                  Text(
                    'الصارمة Firebase وقواعد أمان AES-256 المنظومة محمية بتشفير\nضد أي استخراج.',
                    style: TextStyle(fontSize: 10.5, color: Color(0xFF4A5568)),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildControlCard({
    required IconData icon,
    required Color iconColor,
    required String title,
    required String description,
    required String buttonLabel,
    required Color buttonColor,
    required VoidCallback? onPressed,
    bool isLoading = false,
  }) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFF181014),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: buttonColor.withOpacity(0.25)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Icon(icon, color: iconColor, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(title,
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: iconColor)),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(description,
              style: const TextStyle(fontSize: 12.5, color: Color(0xFF9EA3B0), height: 1.45)),
          const SizedBox(height: 18),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: onPressed == null ? Colors.grey.shade800 : buttonColor,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              elevation: onPressed == null ? 0 : 6,
            ),
            onPressed: onPressed,
            child: isLoading
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : Text(buttonLabel, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}
