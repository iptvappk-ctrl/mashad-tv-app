import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'services/native_scraper_service.dart';
import 'src/firebase_quota_guard.dart';
part 'src/app_shared.dart';
part 'src/catalog.dart';
part 'src/catalog_widgets.dart';
part 'src/details.dart';
part 'src/episodes.dart';
part 'src/recaps.dart';
part 'src/sports.dart';
part 'src/telemetry.dart';

const _native = MethodChannel('madar/native');
const _nativeScraper = NativeScraperService();
const _updateProgress = EventChannel('madar/updates/progress');
const _ink = Color(0xFF342633);
const _brick = Color(0xFFB94E3C);
const _paper = Color(0xFFF2E6D7);
const _card = Color(0xFFFFFBF5);
const _muted = Color(0xFF806D78);
const _line = Color(0xFFE2D2C0);

void main() => runApp(const ZakzoukaFlutterApp());

// Keep the default Flutter test harness entry point available during the port.
class MyApp extends ZakzoukaFlutterApp {
  const MyApp({super.key});
}

class ZakzoukaFlutterApp extends StatelessWidget {
  const ZakzoukaFlutterApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'زقزوقة',
    theme: ThemeData(
      useMaterial3: true,
      scaffoldBackgroundColor: _paper,
      colorScheme: ColorScheme.fromSeed(
        seedColor: _brick,
        brightness: Brightness.light,
        surface: _card,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: _paper,
        foregroundColor: _ink,
        elevation: 0,
        centerTitle: false,
      ),
      cardTheme: CardThemeData(
        color: _card,
        margin: EdgeInsets.zero,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
          side: const BorderSide(color: _line),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: const Color(0xFFFFF9F1),
        indicatorColor: _brick.withOpacity(.12),
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TextStyle(
            color: states.contains(WidgetState.selected) ? _brick : _muted,
            fontSize: 11,
            fontWeight: states.contains(WidgetState.selected)
                ? FontWeight.w700
                : FontWeight.w500,
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: _card,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 13,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(18),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(18),
          borderSide: const BorderSide(color: _line),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(18),
          borderSide: const BorderSide(color: _brick, width: 1.4),
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: _card,
        selectedColor: _brick,
        side: const BorderSide(color: _line),
        shape: const StadiumBorder(),
        labelStyle: const TextStyle(color: _muted),
      ),
    ),
    builder: (context, child) => Directionality(
      textDirection: TextDirection.rtl,
      child: child ?? const SizedBox.shrink(),
    ),
    home: SystemStatusGuard(
      onWipe: () {
        try {
          NativeBridge.clearAllData();
        } catch (_) {}
      },
      child: const _KillSwitchGate(child: _UpdatePromptGate(child: _StartupIntro(child: RootTabs()))),
    ),
  );
}

/// Checks the public release manifest without delaying the first screen.
class _UpdatePromptGate extends StatefulWidget {
  const _UpdatePromptGate({required this.child});

  final Widget child;

  @override
  State<_UpdatePromptGate> createState() => _UpdatePromptGateState();
}

class _UpdatePromptGateState extends State<_UpdatePromptGate> {
  Timer? _checkTimer;
  int? _shownVersionCode;
  int? _shownPlayerVersionCode;
  StreamSubscription<dynamic>? _progressSub;

  @override
  void initState() {
    super.initState();
    // Let the first screen render before making a short HTTPS request.
    _checkTimer = Timer(const Duration(seconds: 2), _checkForUpdate);
    _progressSub = _updateProgress.receiveBroadcastStream().listen((event) {
      if (event is! Map) return;
      final downloaded = (event['downloadedBytes'] as num?)?.toInt() ?? -1;
      final total = (event['totalBytes'] as num?)?.toInt() ?? -1;
      final prog = (downloaded > 0 && total > 0) ? (downloaded / total).clamp(0.0, 1.0) : null;
      appUpdateNotifier.value = appUpdateNotifier.value.copyWith(
        isDownloading: true,
        downloadProgress: prog,
      );
    });
  }

  Future<void> _checkForUpdate() async {
    try {
      final update = await _native.invokeMapMethod<String, dynamic>(
        'updates.check',
      );
      if (!mounted || update == null) return;
      final appUpdateAvailable = update['available'] == true;
      final playerUpdateAvailable = update['playerUpdateAvailable'] == true;
      final appDownloaded = update['appDownloaded'] == true;
      final playerDownloaded = update['playerDownloaded'] == true;

      appUpdateNotifier.value = AppUpdateInfo(
        hasAppUpdate: appUpdateAvailable,
        hasPlayerUpdate: playerUpdateAvailable,
        isAppDownloaded: appDownloaded,
        isPlayerDownloaded: playerDownloaded,
        appVersion: (update['versionName'] as String? ?? '').trim(),
        playerVersion: (update['playerVersionName'] as String? ?? '1.1.3').trim(),
        appNotes: (update['releaseNotes'] as String? ?? '').trim(),
        playerNotes: (update['playerReleaseNotes'] as String? ?? '').trim(),
        playerApkUrl: update['playerApkUrl'] as String?,
        playerSha256: update['playerSha256'] as String?,
      );

      if (appUpdateAvailable) {
        final code = (update['versionCode'] as num?)?.toInt();
        if (code != null && code > 0 && code != _shownVersionCode) {
          _shownVersionCode = code;
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted) _showUpdateDialog(Map<String, dynamic>.from(update));
          });
          return;
        }
      }

      if (playerUpdateAvailable) {
        final pCode = (update['playerVersionCode'] as num?)?.toInt();
        if (pCode != null && pCode > 0 && pCode != _shownPlayerVersionCode) {
          _shownPlayerVersionCode = pCode;
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted) {
              NativeBridge.promptPlayerUpdate(
                context,
                apkUrl: update['playerApkUrl'] as String?,
                sha256: update['playerSha256'] as String?,
                versionName: update['playerVersionName'] as String?,
                releaseNotes: update['playerReleaseNotes'] as String?,
              );
            }
          });
        }
      }
    } on PlatformException catch (_) {
    } catch (_) {
    }
  }

  Future<void> _showUpdateDialog(Map<String, dynamic> update) async {
    var busy = false;
    var hint = '';
    double? downloadProgress;
    StreamSubscription<dynamic>? progressSubscription;
    await showDialog<void>(
      context: context,
      barrierDismissible: true,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) {
          return AlertDialog(
            icon: const Icon(Icons.system_update_alt_rounded, color: _brick),
            title: const Text('يوجد تحديث جديد'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('الإصدار ${update['versionName'] ?? ''}'),
                if ((update['releaseNotes'] as String? ?? '')
                    .trim()
                    .isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Text(
                    (update['releaseNotes'] as String).trim(),
                    style: const TextStyle(color: _muted),
                  ),
                ],
                if (hint.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Text(hint, style: const TextStyle(color: _brick)),
                ],
                if (busy && downloadProgress != null) ...[
                  const SizedBox(height: 14),
                  LinearProgressIndicator(value: downloadProgress),
                ],
              ],
            ),
            actions: [
              TextButton(
                onPressed: busy
                    ? null
                    : () => Navigator.of(dialogContext).pop(),
                child: const Text('لاحقاً'),
              ),
              FilledButton.icon(
                onPressed: busy
                    ? null
                    : () async {
                        setDialogState(() {
                          busy = true;
                          hint = '';
                        });
                        Map<String, dynamic>? response;
                        try {
                          progressSubscription ??= _updateProgress
                              .receiveBroadcastStream()
                              .listen((event) {
                                if (event is! Map) return;
                                final downloaded =
                                    (event['downloadedBytes'] as num?)
                                        ?.toInt() ??
                                    -1;
                                final total =
                                    (event['totalBytes'] as num?)?.toInt() ??
                                    -1;
                                if (!dialogContext.mounted) return;
                                setDialogState(() {
                                  if (total > 0 && downloaded >= 0) {
                                    downloadProgress = (downloaded / total)
                                        .clamp(0, 1);
                                  }
                                  final status = event['status'];
                                  if (status == 'downloading') {
                                    hint = 'جاري تنزيل التحديث...';
                                  }
                                });
                              });
                          response = await _native
                              .invokeMapMethod<String, dynamic>(
                                'updates.install',
                              );
                        } catch (_) {
                          response = null;
                        }
                        if (!dialogContext.mounted) return;
                        final status = response?['status'];
                        if (status == 'install_started') {
                          await progressSubscription?.cancel();
                          progressSubscription = null;
                          Navigator.of(dialogContext).pop();
                          if (mounted) {
                            ScaffoldMessenger.maybeOf(this.context)
                                ?.showSnackBar(
                                  const SnackBar(
                                    content: Text(
                                      'اكتمل تنزيل التحديث؛ أكمل التثبيت في نافذة Android.',
                                    ),
                                  ),
                                );
                          }
                        } else {
                          await progressSubscription?.cancel();
                          progressSubscription = null;
                          setDialogState(() {
                            busy = false;
                            hint = status == 'permission_required'
                                ? 'اسمح للتطبيق بتثبيت التحديث من شاشة Android التي فتحت، ثم اضغط «تحديث الآن» مرة أخرى.'
                                : status == 'permission_unavailable'
                                ? 'تعذر فتح إعداد التثبيت. افتح إعدادات التطبيق واسمح بتثبيت التطبيقات.'
                                : status == 'no_update'
                                ? 'لا يتوفر تحديث أحدث الآن.'
                                : 'تعذر تنزيل التحديث والتحقق منه. تأكد من الاتصال بالإنترنت ثم حاول مرة أخرى.';
                          });
                        }
                      },
                icon: busy
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.download_rounded),
                label: const Text('تحديث الآن'),
              ),
            ],
          );
        },
      ),
    );
    await progressSubscription?.cancel();
  }

  @override
  void dispose() {
    _checkTimer?.cancel();
    _progressSub?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}

// ─── 🔐 KILL SWITCH GATE ─────────────────────────────────────────────────────
// Checks the owner's kill switch on every app startup.
// If kill switch is active on the cloud → shows shutdown screen (cannot be bypassed).
// If server is unreachable (no internet) → allows the app to run normally (fail-open).
// OWNER only can deactivate via: POST /api/admin/kill-switch with MASTER_SECRET header.
class _KillSwitchGate extends StatefulWidget {
  const _KillSwitchGate({required this.child});
  final Widget child;
  @override
  State<_KillSwitchGate> createState() => _KillSwitchGateState();
}

class _KillSwitchGateState extends State<_KillSwitchGate> {
  static const String _apiBase = 'https://zaqzouka-dashboard-api.vercel.app';
  // 🔑 Replace this with YOUR secret API key (set it in Vercel env as ZAQZOUKA_API_KEY)
  // Keep this key private — do NOT share with anyone.
  static const String _apiKey = 'ZQ-OWNER-2026-SECRET-14MOON';

  bool _checking = true;
  bool _killed = false;
  String _killMessage = 'التطبيق متوقف مؤقتاً.\nجاري الصيانة، سنعود قريباً.';

  @override
  void initState() {
    super.initState();
    unawaited(_checkKillSwitch());
  }

  Future<void> _checkKillSwitch() async {
    try {
      final client = HttpClient()..connectionTimeout = const Duration(seconds: 4);
      final req = await client
          .postUrl(Uri.parse('$_apiBase/api/admin/license-check'))
          .timeout(const Duration(seconds: 5));
      req.headers.set('Content-Type', 'application/json');
      req.headers.set('x-zaqzouka-key', _apiKey);
      req.write(jsonEncode({'platform': 'app', 'buildId': 'zq-14moon'}));
      final res = await req.close().timeout(const Duration(seconds: 5));
      final body = await res.transform(utf8.decoder).join();
      client.close();
      final data = jsonDecode(body) as Map<String, dynamic>;
      if (data['allowed'] == false) {
        if (data['wipe'] == true || data['reason'] == 'NUCLEAR_WIPE') {
          try {
            await NativeBridge.clearAllData();
          } catch (_) {}
          exit(0);
        }
        if (mounted) {
          setState(() {
            _killed = true;
            _checking = false;
            _killMessage = data['message']?.toString() ??
                'التطبيق متوقف مؤقتاً.\nجاري الصيانة، سنعود قريباً.';
          });
        }
        return;
      }
    } catch (_) {
      // No internet or server error → fail open (let the app run normally)
    }
    if (mounted) setState(() => _checking = false);
  }

  @override
  Widget build(BuildContext context) {
    if (_checking) {
      return const Scaffold(
        backgroundColor: _paper,
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                width: 36, height: 36,
                child: CircularProgressIndicator(color: _brick, strokeWidth: 2.5),
              ),
              SizedBox(height: 18),
              Text('جاري التحقق...', style: TextStyle(color: _muted, fontSize: 14)),
            ],
          ),
        ),
      );
    }
    if (_killed) {
      return Scaffold(
        backgroundColor: const Color(0xFF1A0E0E),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.lock_outline_rounded, color: Color(0xFFB94E3C), size: 64),
                const SizedBox(height: 24),
                const Text(
                  'زقزوقة TV',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 28,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 1.2,
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  _killMessage,
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Color(0xFFCCB0A0), fontSize: 16, height: 1.6),
                ),
                const SizedBox(height: 32),
                TextButton(
                  onPressed: () {
                    setState(() { _checking = true; _killed = false; });
                    unawaited(_checkKillSwitch());
                  },
                  child: const Text('إعادة المحاولة', style: TextStyle(color: Color(0xFFB94E3C))),
                ),
              ],
            ),
          ),
        ),
      );
    }
    return widget.child;
  }
}

// ─── END KILL SWITCH GATE ────────────────────────────────────────────────────

class _StartupIntro extends StatefulWidget {
  const _StartupIntro({required this.child});

  final Widget child;

  @override
  State<_StartupIntro> createState() => _StartupIntroState();
}

class _StartupIntroState extends State<_StartupIntro> {
  Timer? _timer;
  bool _visible = true;
  bool _showIntro = true;

  @override
  void initState() {
    super.initState();
    _timer = Timer(const Duration(milliseconds: 1250), _dismissIntro);
  }

  void _dismissIntro() {
    if (!mounted || !_showIntro || !_visible) return;
    setState(() => _visible = false);
  }

  void _removeIntro() {
    if (mounted && !_visible && _showIntro) {
      setState(() => _showIntro = false);
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Stack(
    fit: StackFit.expand,
    children: [
      widget.child,
      if (_showIntro)
        Positioned.fill(
          child: IgnorePointer(
            ignoring: !_visible,
            child: AnimatedOpacity(
              opacity: _visible ? 1 : 0,
              duration: const Duration(milliseconds: 350),
              curve: Curves.easeOutCubic,
              onEnd: _removeIntro,
              child: _ZaqzoukaIntroScreen(onSkip: _dismissIntro),
            ),
          ),
        ),
    ],
  );
}

class _ZaqzoukaIntroScreen extends StatelessWidget {
  const _ZaqzoukaIntroScreen({required this.onSkip});

  final VoidCallback onSkip;

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: _paper,
    body: DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topRight,
          end: Alignment.bottomLeft,
          colors: [_card, _paper, Color(0xFFEAD7C5)],
        ),
      ),
      child: Stack(
        fit: StackFit.expand,
        children: [
          Positioned(
            top: -95,
            right: -75,
            child: Container(
              width: 250,
              height: 250,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _brick.withOpacity(.09),
              ),
            ),
          ),
          Positioned(
            bottom: -115,
            left: -70,
            child: Container(
              width: 280,
              height: 280,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _muted.withOpacity(.08),
              ),
            ),
          ),
          SafeArea(
            child: Stack(
              children: [
                Positioned(
                  top: 8,
                  left: 12,
                  child: TextButton(
                    onPressed: onSkip,
                    child: const Text(
                      'تخطي',
                      style: TextStyle(
                        color: _muted,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
                Center(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 28),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        TweenAnimationBuilder<double>(
                          tween: Tween(begin: .78, end: 1),
                          duration: const Duration(milliseconds: 760),
                          curve: Curves.easeOutBack,
                          builder: (context, scale, child) => Opacity(
                            opacity: ((scale - .78) / .22)
                                .clamp(0.0, 1.0)
                                .toDouble(),
                            child: Transform.rotate(
                              angle: (1 - scale) * .6,
                              child: Transform.scale(
                                scale: scale,
                                child: child,
                              ),
                            ),
                          ),
                          child: Container(
                            width: 226,
                            height: 226,
                            padding: const EdgeInsets.all(7),
                            decoration: BoxDecoration(
                              color: _card,
                              borderRadius: BorderRadius.circular(40),
                              border: Border.all(
                                color: Colors.white,
                                width: 1.5,
                              ),
                              boxShadow: [
                                BoxShadow(
                                  color: _ink.withOpacity(.14),
                                  blurRadius: 36,
                                  offset: const Offset(0, 18),
                                ),
                              ],
                            ),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(34),
                              child: Image.asset(
                                'assets/zakzouka_logo.jpg',
                                fit: BoxFit.contain,
                                filterQuality: FilterQuality.medium,
                                semanticLabel: 'شعار زقزوقة',
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 27),
                        const Text(
                          'زقزوقة',
                          style: TextStyle(
                            color: _ink,
                            fontSize: 30,
                            fontWeight: FontWeight.w900,
                            letterSpacing: .2,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'كل الحكايات في مكان واحد',
                          style: TextStyle(
                            color: _muted.withOpacity(.96),
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 7),
                        const Text(
                          'أفلام • مسلسلات • ملخصات • مباريات مباشرة',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: _muted, fontSize: 13),
                        ),
                        const SizedBox(height: 28),
                        TweenAnimationBuilder<double>(
                          tween: Tween(begin: 0, end: 1),
                          duration: const Duration(milliseconds: 900),
                          curve: Curves.easeOutCubic,
                          builder: (context, value, child) => SizedBox(
                            width: 100,
                            child: Align(
                              alignment: Alignment.centerRight,
                              child: Container(
                                width: 100 * value,
                                height: 4,
                                decoration: BoxDecoration(
                                  color: _brick,
                                  borderRadius: BorderRadius.circular(99),
                                ),
                              ),
                            ),
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
  );
}
