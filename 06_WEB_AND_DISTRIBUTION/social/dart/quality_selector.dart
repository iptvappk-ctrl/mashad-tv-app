// quality_selector.dart
// ─────────────────────
// Drop-in Flutter widget for quality selection in the Zakzouka player.
// Compatible with the existing MadarFlutter episode/details screen.
//
// HOW TO USE:
//   1. Copy this file to: MadarFlutter/lib/src/quality_selector.dart
//   2. Add `part 'quality_selector.dart';` in main.dart
//   3. In episodes.dart _openServer(), after getting the stream URL:
//      final quality = await showQualitySelector(context, streamUrl: url);
//      if (quality != null) url = quality.streamUrl;
//
// DEPENDENCIES (add to pubspec.yaml):
//   shared_preferences: ^2.2.0

// ignore_for_file: use_build_context_synchronously

import 'dart:convert';
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

// ── Data model ────────────────────────────────────────────────────────────────
class QualityTrack {
  final String label;       // "1080p", "720p", "Auto", "4K"
  final String streamUrl;   // actual .m3u8 track URL
  final String resolution;  // "1920x1080"
  final int bandwidth;      // bps
  final double frameRate;

  const QualityTrack({
    required this.label,
    required this.streamUrl,
    this.resolution = '',
    this.bandwidth = 0,
    this.frameRate = 0,
  });

  factory QualityTrack.fromJson(Map<String, dynamic> j) => QualityTrack(
        label: j['label'] as String? ?? 'Auto',
        streamUrl: j['streamUrl'] as String? ?? j['url'] as String? ?? '',
        resolution: j['resolution'] as String? ?? '',
        bandwidth: (j['bandwidth'] as num?)?.toInt() ?? 0,
        frameRate: (j['frameRate'] as num?)?.toDouble() ?? 0,
      );

  Map<String, dynamic> toJson() => {
        'label': label,
        'streamUrl': streamUrl,
        'resolution': resolution,
        'bandwidth': bandwidth,
        'frameRate': frameRate,
      };

  @override
  String toString() => label;
}

// ── Prefs key ─────────────────────────────────────────────────────────────────
const _prefKeyQuality = 'zakzouka_preferred_quality';

Future<String> _loadPreferredQuality() async {
  final prefs = await SharedPreferences.getInstance();
  return prefs.getString(_prefKeyQuality) ?? 'Auto';
}

Future<void> _savePreferredQuality(String label) async {
  final prefs = await SharedPreferences.getInstance();
  await prefs.setString(_prefKeyQuality, label);
}

// ── Parse M3U8 tracks from JSON package (from API or Kotlin bridge) ───────────
List<QualityTrack> parseQualityTracksFromJson(Map<String, dynamic> json) {
  final tracksRaw = json['tracks'] as List<dynamic>? ?? [];
  final tracks = tracksRaw
      .whereType<Map<String, dynamic>>()
      .map(QualityTrack.fromJson)
      .toList();

  // Always prepend Auto
  final hasAuto = tracks.any((t) => t.label == 'Auto');
  final autoUrl = json['autoUrl'] as String? ?? (tracks.isNotEmpty ? tracks.first.streamUrl : '');
  if (!hasAuto && autoUrl.isNotEmpty) {
    tracks.insert(
        0,
        QualityTrack(
          label: 'Auto',
          streamUrl: autoUrl,
          resolution: '',
          bandwidth: 0,
        ));
  }
  return tracks;
}

// ── Main function: show quality selector bottom sheet ─────────────────────────
/// Shows a bottom sheet to pick stream quality.
/// Returns the chosen [QualityTrack] or null if user dismissed.
Future<QualityTrack?> showQualitySelector(
  BuildContext context, {
  required List<QualityTrack> tracks,
  String currentUrl = '',
}) async {
  if (tracks.isEmpty) return null;
  final preferred = await _loadPreferredQuality();

  return showModalBottomSheet<QualityTrack>(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (_) => _QualitySelectorSheet(
      tracks: tracks,
      preferredLabel: preferred,
      currentUrl: currentUrl,
    ),
  );
}

// ── Bottom sheet widget ───────────────────────────────────────────────────────
class _QualitySelectorSheet extends StatefulWidget {
  const _QualitySelectorSheet({
    required this.tracks,
    required this.preferredLabel,
    required this.currentUrl,
  });

  final List<QualityTrack> tracks;
  final String preferredLabel;
  final String currentUrl;

  @override
  State<_QualitySelectorSheet> createState() => _QualitySelectorSheetState();
}

class _QualitySelectorSheetState extends State<_QualitySelectorSheet> {
  late String _selected;

  @override
  void initState() {
    super.initState();
    _selected = widget.preferredLabel;
    // If preferred not in list, default to first
    if (!widget.tracks.any((t) => t.label == _selected)) {
      _selected = widget.tracks.first.label;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Color(0xFF1E1E2E),
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Handle
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.white24,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Text(
                'جودة الفيديو',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'اختار الجودة المناسبة لاتصالك',
                style: TextStyle(color: Colors.white54, fontSize: 13),
              ),
              const SizedBox(height: 16),
              ...widget.tracks.map((track) => _QualityTile(
                    track: track,
                    isSelected: track.label == _selected,
                    onTap: () async {
                      setState(() => _selected = track.label);
                      await _savePreferredQuality(track.label);
                      if (context.mounted) Navigator.of(context).pop(track);
                    },
                  )),
              const SizedBox(height: 8),
            ],
          ),
        ),
      ),
    );
  }
}

class _QualityTile extends StatelessWidget {
  const _QualityTile({
    required this.track,
    required this.isSelected,
    required this.onTap,
  });

  final QualityTrack track;
  final bool isSelected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final badge = _qualityBadgeColor(track.label);
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFF7C3AED).withAlpha(50) : Colors.white10,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isSelected ? const Color(0xFF7C3AED) : Colors.white12,
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            // Quality badge
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: badge,
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(
                track.label,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                ),
              ),
            ),
            const SizedBox(width: 12),
            // Resolution + bandwidth
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (track.resolution.isNotEmpty)
                    Text(
                      track.resolution,
                      style: const TextStyle(color: Colors.white70, fontSize: 12),
                    ),
                  if (track.bandwidth > 0)
                    Text(
                      _formatBandwidth(track.bandwidth),
                      style: const TextStyle(color: Colors.white38, fontSize: 11),
                    ),
                ],
              ),
            ),
            if (isSelected)
              const Icon(Icons.check_circle_rounded, color: Color(0xFF7C3AED), size: 22),
          ],
        ),
      ),
    );
  }

  Color _qualityBadgeColor(String label) {
    switch (label) {
      case '4K':    return const Color(0xFFE53935);
      case '1080p': return const Color(0xFF1565C0);
      case '720p':  return const Color(0xFF2E7D32);
      case '480p':  return const Color(0xFFF57F17);
      case '360p':  return const Color(0xFF6D4C41);
      default:      return const Color(0xFF546E7A);
    }
  }

  String _formatBandwidth(int bps) {
    if (bps >= 1_000_000) return '${(bps / 1_000_000).toStringAsFixed(1)} Mbps';
    if (bps >= 1_000) return '${(bps / 1_000).toStringAsFixed(0)} Kbps';
    return '$bps bps';
  }
}

// ── Compact inline quality row (for player overlay) ───────────────────────────
/// A compact horizontal row of quality chips — embed inside the player UI.
class QualityChipRow extends StatefulWidget {
  const QualityChipRow({
    super.key,
    required this.tracks,
    required this.onQualitySelected,
    this.initialLabel = 'Auto',
  });

  final List<QualityTrack> tracks;
  final ValueChanged<QualityTrack> onQualitySelected;
  final String initialLabel;

  @override
  State<QualityChipRow> createState() => _QualityChipRowState();
}

class _QualityChipRowState extends State<QualityChipRow> {
  late String _current;

  @override
  void initState() {
    super.initState();
    _current = widget.initialLabel;
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: widget.tracks.map((track) {
          final selected = track.label == _current;
          return Padding(
            padding: const EdgeInsets.only(left: 6),
            child: GestureDetector(
              onTap: () {
                setState(() => _current = track.label);
                _savePreferredQuality(track.label);
                widget.onQualitySelected(track);
              },
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 150),
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: selected ? Colors.white : Colors.black54,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: Colors.white30),
                ),
                child: Text(
                  track.label,
                  style: TextStyle(
                    color: selected ? Colors.black : Colors.white,
                    fontWeight: selected ? FontWeight.bold : FontWeight.normal,
                    fontSize: 12,
                  ),
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}
