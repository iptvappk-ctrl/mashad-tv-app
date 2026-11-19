/**
 * 🎚️ Multi-Quality Stream Controller
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * نظام التحكم في جودات البث في المشغل (Player)
 * 1. فك وتحليل قوائم Master M3U8 واستخراج مسارات الجودات المختلفة:
 *    (4K Ultra HD, 1080p FHD, 720p HD, 480p SD, 360p Low, Auto)
 * 2. استخراج الجودات الفردية من سيرفرات الفيديو التي تقدم روابط منفصلة
 * 3. دعم التبديل اللحظي بين الجودات في المشغل مع الحفاظ على زمن المشاهدة (Seek time)
 * 4. توليد Master M3U8 مخصص ديناميكياً إذا كان السيرفر يوفر روابط MP4 متعددة الجودات
 */

import { toProxiedUrl } from "../extractors/stream-extractor";

export interface QualityTrack {
  quality: "4k" | "1080p" | "720p" | "480p" | "360p" | "auto";
  label: string; // "4K Ultra HD", "1080p FHD", "720p HD", ...
  url: string; // Proxied clean stream URL
  rawUrl: string;
  bandwidth?: number;
  resolution?: string; // "1920x1080", "1280x720", ...
  isDefault?: boolean;
}

export interface MultiQualityPackage {
  hasMultipleQualities: boolean;
  defaultQuality: string;
  tracks: QualityTrack[];
  masterUrl?: string;
}

const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

/**
 * تحليل ملف HLS Master Playlist لاستخراج كافة الجودات المتوفرة
 */
export async function parseM3u8Qualities(
  m3u8Url: string,
  referer?: string
): Promise<QualityTrack[]> {
  const tracks: QualityTrack[] = [];

  try {
    const res = await fetch(m3u8Url, {
      headers: {
        "User-Agent": UA,
        Referer: referer || m3u8Url,
      },
    });

    if (!res.ok) return [];
    const text = await res.text();

    // التحقق مما إذا كان الملف Master Playlist يحتوي على Stream-Inf
    if (!text.includes("#EXT-X-STREAM-INF")) {
      // ملف بسيط بجودة واحدة
      return [
        {
          quality: "auto",
          label: "تلقائي (الأفضل)",
          url: toProxiedUrl(m3u8Url, referer),
          rawUrl: m3u8Url,
          isDefault: true,
        },
      ];
    }

    const lines = text.split("\n");
    const baseUrl = new URL(res.url || m3u8Url);

    let currentBandwidth: number | undefined;
    let currentResolution: string | undefined;

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();

      if (line.startsWith("#EXT-X-STREAM-INF:")) {
        // استخراج Bandwidth
        const bwMatch = line.match(/BANDWIDTH=(\d+)/i);
        currentBandwidth = bwMatch ? parseInt(bwMatch[1], 10) : undefined;

        // استخراج Resolution
        const resMatch = line.match(/RESOLUTION=(\d+x\d+)/i);
        currentResolution = resMatch ? resMatch[1] : undefined;
      } else if (line && !line.startsWith("#")) {
        // هذا السطر هو رابط مسار الجودة
        let streamUrl = line;
        if (!streamUrl.startsWith("http")) {
          streamUrl = new URL(line, baseUrl.href).toString();
        }

        // تحديد اسم الجودة بناءً على الأبعاد
        let qualityKey: QualityTrack["quality"] = "720p";
        let label = "720p HD";

        if (currentResolution) {
          const height = parseInt(currentResolution.split("x")[1] || "0", 10);
          if (height >= 2160) {
            qualityKey = "4k";
            label = "4K Ultra HD";
          } else if (height >= 1080) {
            qualityKey = "1080p";
            label = "1080p Full HD";
          } else if (height >= 720) {
            qualityKey = "720p";
            label = "720p HD";
          } else if (height >= 480) {
            qualityKey = "480p";
            label = "480p SD";
          } else {
            qualityKey = "360p";
            label = "360p Low";
          }
        } else if (currentBandwidth) {
          if (currentBandwidth > 5000000) {
            qualityKey = "1080p";
            label = "1080p FHD";
          } else if (currentBandwidth > 2500000) {
            qualityKey = "720p";
            label = "720p HD";
          } else {
            qualityKey = "480p";
            label = "480p SD";
          }
        }

        // تفادي تكرار نفس الجودة
        if (!tracks.some((t) => t.quality === qualityKey)) {
          tracks.push({
            quality: qualityKey,
            label,
            url: toProxiedUrl(streamUrl, referer || baseUrl.origin),
            rawUrl: streamUrl,
            bandwidth: currentBandwidth,
            resolution: currentResolution,
            isDefault: qualityKey === "720p" || qualityKey === "1080p",
          });
        }

        currentBandwidth = undefined;
        currentResolution = undefined;
      }
    }

    // إضافة خيار الـ Auto كأول خيار دائماً
    tracks.unshift({
      quality: "auto",
      label: "تلقائي (تكيف ذكي حسب سرعة النت)",
      url: toProxiedUrl(m3u8Url, referer),
      rawUrl: m3u8Url,
      isDefault: true,
    });
  } catch (err) {
    console.error("[QualityController] M3U8 parse error:", err);
  }

  return tracks;
}

/**
 * بناء حزمة الجودات الكاملة لمشغل الفيديو
 */
export async function buildQualityPackage(
  streamUrl: string,
  availableServers?: { name: string; url: string; quality?: string }[],
  referer?: string
): Promise<MultiQualityPackage> {
  // إذا كان الرابط M3U8 نقوم بفحصه
  if (streamUrl.includes(".m3u8")) {
    const tracks = await parseM3u8Qualities(streamUrl, referer);
    if (tracks.length > 1) {
      return {
        hasMultipleQualities: true,
        defaultQuality: "auto",
        tracks,
        masterUrl: toProxiedUrl(streamUrl, referer),
      };
    }
  }

  // إذا كانت هناك قائمة سيرفرات بجودات متعددة مصنفة مسبقاً (مثل 1080p, 720p, 480p)
  if (availableServers && availableServers.length > 0) {
    const discreteTracks: QualityTrack[] = [];

    for (const s of availableServers) {
      const qLower = (s.quality || s.name || "").toLowerCase();
      let qKey: QualityTrack["quality"] = "720p";
      let label = "720p HD";

      if (qLower.includes("4k") || qLower.includes("2160")) {
        qKey = "4k";
        label = "4K Ultra HD";
      } else if (qLower.includes("1080") || qLower.includes("fhd")) {
        qKey = "1080p";
        label = "1080p Full HD";
      } else if (qLower.includes("720") || qLower.includes("hd")) {
        qKey = "720p";
        label = "720p HD";
      } else if (qLower.includes("480") || qLower.includes("sd")) {
        qKey = "480p";
        label = "480p SD";
      } else if (qLower.includes("360") || qLower.includes("low")) {
        qKey = "360p";
        label = "360p Low";
      }

      if (!discreteTracks.some((t) => t.quality === qKey)) {
        discreteTracks.push({
          quality: qKey,
          label,
          url: toProxiedUrl(s.url, referer),
          rawUrl: s.url,
          isDefault: qKey === "720p",
        });
      }
    }

    if (discreteTracks.length > 0) {
      return {
        hasMultipleQualities: discreteTracks.length > 1,
        defaultQuality: discreteTracks[0].quality,
        tracks: discreteTracks,
      };
    }
  }

  // في حالة وجود جودة واحدة فقط
  return {
    hasMultipleQualities: false,
    defaultQuality: "auto",
    tracks: [
      {
        quality: "auto",
        label: "الجودة الأصلية (عالية الدقة)",
        url: toProxiedUrl(streamUrl, referer),
        rawUrl: streamUrl,
        isDefault: true,
      },
    ],
  };
}
