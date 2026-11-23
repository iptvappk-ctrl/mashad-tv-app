import 'dart:async';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'firebase_quota_guard.dart';

/// 📌 شريط إعلاني ثابت غير مزعج (Non-intrusive Banner Strip)
/// يظهر في أسفل الشاشة فوق شريط التنقل السفلي مباشرة، ولا يظهر داخل المشغل ملء الشاشة.
class AppBannerAdStrip extends StatefulWidget {
  const AppBannerAdStrip({super.key});

  @override
  State<AppBannerAdStrip> createState() => _AppBannerAdStripState();
}

class _AppBannerAdStripState extends State<AppBannerAdStrip> {
  static const _native = MethodChannel('madar/native');
  bool _dismissed = false;
  StreamSubscription<SystemStatus>? _sub;

  @override
  void initState() {
    super.initState();
    _sub = ZaqzoukaQuotaGuard.instance.statusStream.listen((_) {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _sub?.cancel();
    super.dispose();
  }

  void _openAdUrl(String url) {
    if (url.trim().isEmpty) return;
    try {
      _native.invokeMethod('system.openUrl', {'url': url.trim()});
    } catch (_) {}
  }

  String? _extractImgUrl(String html) {
    final match = RegExp(r'''<img[^>]+src=["']([^"']+)["']''', caseSensitive: false).firstMatch(html);
    if (match != null) return match.group(1);
    // If the html string is just a direct image url
    final directImgMatch = RegExp(r'''https?://[^\s<>"']+\.(?:png|jpg|jpeg|webp|gif)''', caseSensitive: false).firstMatch(html);
    return directImgMatch?.group(0);
  }

  String _resolveBannerUrl(String directUrl, String html) {
    if (directUrl.trim().isNotEmpty) return directUrl.trim();
    final hrefMatch = RegExp(r'''href=["']([^"']+)["']''', caseSensitive: false).firstMatch(html);
    if (hrefMatch != null) return hrefMatch.group(1) ?? '';
    final urlMatch = RegExp(r'''https?://[^\s<>"']+''').firstMatch(html);
    return urlMatch?.group(0) ?? '';
  }

  String _extractBannerText(String html) {
    final clean = html.replaceAll(RegExp(r'<[^>]*>'), '').trim();
    return clean.isNotEmpty ? clean : 'برعاية شركائنا · اضغط للمشاهدة';
  }

  @override
  Widget build(BuildContext context) {
    if (_dismissed) return const SizedBox.shrink();

    final status = ZaqzoukaQuotaGuard.instance.currentStatus;
    if (!status.isBannerAdEnabled) return const SizedBox.shrink();

    final bannerHtml = status.bannerHtml;
    final bannerUrl = _resolveBannerUrl(status.bannerUrl, bannerHtml);
    final bannerImg = _extractImgUrl(bannerHtml);
    final bannerText = _extractBannerText(bannerHtml);

    return AnimatedSize(
      duration: const Duration(milliseconds: 250),
      curve: Curves.easeInOut,
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: const Color(0xFF141923),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: const Color(0xFF262D3D), width: 1),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.35),
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: [
            // Close Button
            InkWell(
              borderRadius: BorderRadius.circular(14),
              onTap: () => setState(() => _dismissed = true),
              child: Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white.withValues(alpha: 0.08),
                ),
                child: const Icon(Icons.close_rounded, size: 14, color: Colors.white54),
              ),
            ),
            const SizedBox(width: 8),

            // Banner Content / Image / Text
            Expanded(
              child: InkWell(
                borderRadius: BorderRadius.circular(8),
                onTap: bannerUrl.isNotEmpty ? () => _openAdUrl(bannerUrl) : null,
                child: Row(
                  children: [
                    if (bannerImg != null && bannerImg.startsWith('http'))
                      ClipRRect(
                        borderRadius: BorderRadius.circular(6),
                        child: CachedNetworkImage(
                          imageUrl: bannerImg,
                          height: 38,
                          width: 38,
                          fit: BoxFit.cover,
                          errorWidget: (_, __, ___) => const Icon(Icons.campaign_rounded, color: Color(0xFF4A90D9), size: 24),
                        ),
                      )
                    else
                      Container(
                        padding: const EdgeInsets.all(6),
                        decoration: BoxDecoration(
                          color: const Color(0xFF4A90D9).withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: const Icon(Icons.campaign_rounded, color: Color(0xFF4A90D9), size: 18),
                      ),
                    const SizedBox(width: 8),

                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFD4AF37).withValues(alpha: 0.2),
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: const Text(
                                  'إعلان مميز',
                                  style: TextStyle(fontSize: 9, fontWeight: FontWeight.bold, color: Color(0xFFD4AF37)),
                                ),
                              ),
                              const SizedBox(width: 6),
                              Expanded(
                                child: Text(
                                  bannerText,
                                  style: const TextStyle(fontSize: 10.5, color: Colors.white70),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                          if (bannerUrl.isNotEmpty)
                            Text(
                              bannerUrl,
                              style: const TextStyle(fontSize: 9.5, color: Color(0xFF4A90D9)),
                              overflow: TextOverflow.ellipsis,
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),

            if (bannerUrl.isNotEmpty) ...[
              const SizedBox(width: 6),
              InkWell(
                borderRadius: BorderRadius.circular(8),
                onTap: () => _openAdUrl(bannerUrl),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: const Color(0xFF4A90D9),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: const Text(
                    'زيارة ↗',
                    style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
