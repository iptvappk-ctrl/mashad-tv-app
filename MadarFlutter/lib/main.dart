import 'dart:async';
import 'dart:convert';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
part 'src/app_shared.dart';
part 'src/catalog.dart';
part 'src/catalog_widgets.dart';
part 'src/details.dart';
part 'src/episodes.dart';
part 'src/sports.dart';

const _native = MethodChannel('madar/native');
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
    home: const _StartupIntro(child: RootTabs()),
  );
}

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
                          'أفلام • مسلسلات • مباريات مباشرة',
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
