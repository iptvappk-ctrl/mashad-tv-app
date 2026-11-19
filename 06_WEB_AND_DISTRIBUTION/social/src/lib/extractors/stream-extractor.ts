/**
 * ⚡ STREAM EXTRACTOR ENGINE
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * يستخرج روابط m3u8 / mp4 مباشرة من صفحات الـ embed
 * بدون إعلانات — يتجاوز كل الـ wrappers ويجيب CDN link مباشر
 * يدعم: Doodstream, Streamtape, Uqload, Mixdrop, Voe.sx,
 *        Vidbam, Fembed, Mp4upload, Filelions, Govid, Streamlare,
 *        Vikingfile, Turboviplay + Generic regex scanner
 */

const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

export interface ExtractedStream {
  url: string;
  type: "hls" | "mp4" | "unknown";
  quality?: string;
  referer?: string;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Helpers
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

async function fetchPage(url: string, referer?: string, timeoutMs = 7000): Promise<string> {
  const ctrl = new AbortController();
  const tid = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": UA,
        Accept: "text/html,application/xhtml+xml,*/*;q=0.8",
        "Accept-Language": "ar,en;q=0.9",
        Referer: referer || url,
      },
      signal: ctrl.signal,
      redirect: "follow",
    });
    return res.ok ? await res.text() : "";
  } catch {
    return "";
  } finally {
    clearTimeout(tid);
  }
}

function detectType(url: string): "hls" | "mp4" | "unknown" {
  if (url.includes(".m3u8")) return "hls";
  if (url.includes(".mp4")) return "mp4";
  return "unknown";
}

function cleanUrl(url: string): string {
  return url
    .replace(/\\u0026/g, "&")
    .replace(/\\u003d/g, "=")
    .replace(/\\/g, "")
    .trim();
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Generic regex patterns
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const M3U8_PATTERNS = [
  /file\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /src\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /hls\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /"file"\s*:\s*"([^"]+\.m3u8[^"]*)"/i,
  /source\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /playlist\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /stream\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /"url"\s*:\s*"(https?:[^"]+\.m3u8[^"]*)"/i,
  /video\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /"masterM3U8"\s*:\s*"([^"]+)"/i,
  /hlsUrl\s*=\s*["']([^"']+\.m3u8[^"']*)/i,
];

const MP4_PATTERNS = [
  /file\s*:\s*["']([^"']+\.mp4[^"']*)/i,
  /"file"\s*:\s*"([^"]+\.mp4[^"]*)"/i,
  /src\s*:\s*["']([^"']+\.mp4[^"']*)/i,
  /"url"\s*:\s*"(https?:[^"]+\.mp4[^"]*)"/i,
  /source\s*:\s*["']([^"']+\.mp4[^"']*)/i,
];

function extractFromHtml(html: string): string | null {
  // M3U8 first (preferred for smooth playback)
  for (const pattern of M3U8_PATTERNS) {
    const m = html.match(pattern);
    if (m?.[1]) {
      const url = cleanUrl(m[1]);
      if (url.startsWith("http") || url.startsWith("//")) {
        return url.startsWith("//") ? "https:" + url : url;
      }
    }
  }
  // MP4 fallback
  for (const pattern of MP4_PATTERNS) {
    const m = html.match(pattern);
    if (m?.[1]) {
      const url = cleanUrl(m[1]);
      if (url.startsWith("http") || url.startsWith("//")) {
        return url.startsWith("//") ? "https:" + url : url;
      }
    }
  }
  return null;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Site-specific extractors
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

async function extractDoodstream(url: string): Promise<string | null> {
  try {
    const html = await fetchPage(url, "https://doodstream.com/");
    const passMatch = html.match(/\/pass_md5\/([^?'"]+)/);
    if (!passMatch) return null;
    const origin = new URL(url).origin;
    const passUrl = `${origin}/pass_md5/${passMatch[1]}`;
    const token = await fetchPage(passUrl, url, 5000);
    if (!token || !token.trim().startsWith("http")) return null;
    const tokenStr = html.match(/token=([^&'"]+)/)?.[1] || Date.now().toString();
    const expiry = Math.floor(Date.now() / 1000) + 3600;
    const sep = token.includes("?") ? "&" : "?";
    return `${token.trim()}${sep}token=${tokenStr}&expiry=${expiry}`;
  } catch {
    return null;
  }
}

async function extractStreamtape(url: string): Promise<string | null> {
  try {
    const html = await fetchPage(url, "https://streamtape.com/");
    const m1 = html.match(/getElementById\([^)]+\)\.innerHTML\s*=\s*["']([^"']*streamtape[^"']+)/);
    if (m1) return "https:" + m1[1].trim();
    const m2 = html.match(/"robotlink"\)\s*\.innerHTML\s*=\s*["']([^"']+)/);
    if (m2) {
      const m3 = html.match(/\+\s*["']([^"']+token[^"']*)/);
      return "https:" + m2[1] + (m3 ? m3[1] : "");
    }
    const m4 = html.match(/(https?:\/\/streamtape\.[a-z]+\/get_video[^"'<\s]+)/);
    if (m4) return m4[1];
    return null;
  } catch {
    return null;
  }
}

async function extractUqload(url: string): Promise<string | null> {
  try {
    const html = await fetchPage(url, "https://uqload.com/");
    const m = html.match(/sources\s*:\s*\[\s*\{[^}]*file\s*:\s*["']([^"']+\.mp4[^"']*)/i);
    if (m) return cleanUrl(m[1]);
    return extractFromHtml(html);
  } catch {
    return null;
  }
}

async function extractMixdrop(url: string): Promise<string | null> {
  try {
    const html = await fetchPage(url, "https://mixdrop.co/");
    const m = html.match(/wurl\s*=\s*["']([^"']+)/);
    if (m) return m[1].startsWith("//") ? "https:" + m[1] : m[1];
    const m2 = html.match(/(https?:\/\/s[0-9]+\.mixdrop[^"'<\s]+\.mp4[^"'<\s]*)/i);
    if (m2) return m2[1];
    return extractFromHtml(html);
  } catch {
    return null;
  }
}

async function extractVoe(url: string): Promise<string | null> {
  try {
    const html = await fetchPage(url, "https://voe.sx/");
    const m = html.match(/['"]hls['"]\s*:\s*['"]([^'"]+\.m3u8[^'"]*)/i);
    if (m) return cleanUrl(m[1]);
    const m2 = html.match(/sources\.hls\s*=\s*['"]([^'"]+)/i);
    if (m2) return cleanUrl(m2[1]);
    return extractFromHtml(html);
  } catch {
    return null;
  }
}

async function extractVidbam(url: string): Promise<string | null> {
  try {
    const html = await fetchPage(url, url);
    const m = html.match(/sources\s*:\s*\[([^\]]+)\]/i);
    if (m) {
      const fileMatch = m[1].match(/file\s*:\s*["']([^"']+)/i);
      if (fileMatch) return cleanUrl(fileMatch[1]);
    }
    return extractFromHtml(html);
  } catch {
    return null;
  }
}

async function extractFembed(url: string): Promise<string | null> {
  try {
    const videoId = url.split("/v/").pop()?.split("?")[0];
    if (!videoId) return null;
    const origin = new URL(url).origin;
    const ctrl = new AbortController();
    const tid = setTimeout(() => ctrl.abort(), 5000);
    try {
      const res = await fetch(`${origin}/api/source/${videoId}`, {
        method: "POST",
        headers: {
          "User-Agent": UA,
          Referer: url,
          "X-Requested-With": "XMLHttpRequest",
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: `r=&d=${encodeURIComponent(new URL(url).hostname)}`,
        signal: ctrl.signal,
      });
      if (res.ok) {
        const data = await res.json();
        if (data?.data?.length) {
          const sources: { file: string; label: string }[] = data.data;
          const best = sources.find((s) => s.label?.includes("HD")) || sources[0];
          if (best?.file) return cleanUrl(best.file);
        }
      }
    } finally {
      clearTimeout(tid);
    }
    return null;
  } catch {
    return null;
  }
}

async function extractMp4upload(url: string): Promise<string | null> {
  try {
    const html = await fetchPage(url, "https://www.mp4upload.com/");
    const m = html.match(/src\s*:\s*["'](https?:\/\/[^"']+\.mp4[^"']*)/i);
    if (m) return cleanUrl(m[1]);
    return extractFromHtml(html);
  } catch {
    return null;
  }
}

async function extractFilelions(url: string): Promise<string | null> {
  try {
    const html = await fetchPage(url, url);
    const m = html.match(/file\s*:\s*["']([^"']+\.m3u8[^"']*)/i);
    if (m) return cleanUrl(m[1]);
    return extractFromHtml(html);
  } catch {
    return null;
  }
}

async function extractGovid(url: string): Promise<string | null> {
  try {
    const html = await fetchPage(url, "https://govid.live/");
    const m = html.match(/file\s*:\s*["']([^"']+\.m3u8[^"']*)/i);
    if (m) return cleanUrl(m[1]);
    const m2 = html.match(/playlist\s*:\s*\[\s*\{\s*file\s*:\s*["']([^"']+)/i);
    if (m2) return cleanUrl(m2[1]);
    return extractFromHtml(html);
  } catch {
    return null;
  }
}

async function extractStreamlare(url: string): Promise<string | null> {
  try {
    const videoId =
      url.split("/v/").pop()?.split("?")[0] ||
      url.split("/e/").pop()?.split("?")[0];
    if (!videoId) return null;
    const ctrl = new AbortController();
    const tid = setTimeout(() => ctrl.abort(), 5000);
    try {
      const res = await fetch("https://streamlare.com/api/video/stream/get", {
        method: "POST",
        headers: {
          "User-Agent": UA,
          "Content-Type": "application/json",
          Referer: url,
        },
        body: JSON.stringify({ id: videoId }),
        signal: ctrl.signal,
      });
      if (res.ok) {
        const data = await res.json();
        const streams = data?.result;
        if (streams) {
          const hls =
            streams["720p"]?.file ||
            streams["360p"]?.file ||
            Object.values(streams as Record<string, { file: string }>)[0]?.file;
          if (hls) return hls;
        }
      }
    } finally {
      clearTimeout(tid);
    }
    return null;
  } catch {
    return null;
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Dispatcher
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

function pickExtractor(url: string): ((url: string) => Promise<string | null>) | null {
  const u = url.toLowerCase();
  if (u.includes("doodstream") || u.includes("dood.") || u.includes("ds2play") || u.includes("do-stream") || u.includes("d0stream")) return extractDoodstream;
  if (u.includes("streamtape") || u.includes("shavetape")) return extractStreamtape;
  if (u.includes("uqload")) return extractUqload;
  if (u.includes("mixdrop")) return extractMixdrop;
  if (u.includes("voe.sx") || u.includes("voe.")) return extractVoe;
  if (u.includes("vidbam") || u.includes("vidlox") || u.includes("vidoza")) return extractVidbam;
  if (u.includes("fembed") || u.includes("fplayer")) return extractFembed;
  if (u.includes("mp4upload")) return extractMp4upload;
  if (u.includes("filelions") || u.includes("fviplions") || u.includes("lionscdn")) return extractFilelions;
  if (u.includes("govid") || u.includes("datahowa")) return extractGovid;
  if (u.includes("streamlare")) return extractStreamlare;
  return null;
}

/**
 * ⚡ استخرج stream مباشر من embed URL
 * Returns: direct m3u8 or mp4 — no ads
 */
export async function extractStreamFromEmbed(
  embedUrl: string,
  referer?: string
): Promise<ExtractedStream | null> {
  if (!embedUrl) return null;

  // Already direct stream
  if (embedUrl.includes(".m3u8") || embedUrl.includes(".mp4")) {
    return { url: embedUrl, type: detectType(embedUrl), referer };
  }

  // Site-specific extractor
  const specific = pickExtractor(embedUrl);
  if (specific) {
    try {
      const url = await specific(embedUrl);
      if (url) return { url, type: detectType(url), referer: embedUrl };
    } catch {}
  }

  // Generic: scan page for video URLs
  try {
    const html = await fetchPage(embedUrl, referer || embedUrl);
    if (html) {
      const url = extractFromHtml(html);
      if (url) return { url, type: detectType(url), referer: embedUrl };

      // Try nested iframes
      const iframeMatch = html.match(/<iframe[^>]+src\s*=\s*["']([^"']+)/i);
      if (iframeMatch?.[1] && iframeMatch[1] !== embedUrl) {
        const nested = iframeMatch[1].startsWith("//")
          ? "https:" + iframeMatch[1]
          : iframeMatch[1].startsWith("http")
          ? iframeMatch[1]
          : null;
        if (nested) {
          const nestedHtml = await fetchPage(nested, embedUrl);
          const nestedUrl = extractFromHtml(nestedHtml);
          if (nestedUrl) return { url: nestedUrl, type: detectType(nestedUrl), referer: nested };
        }
      }
    }
  } catch {}

  return null;
}

/**
 * 🔄 Smart Fallback — يجرب قائمة servers واحدة واحدة
 * First 3 in parallel (race), then sequential fallback
 */
export async function extractStreamWithFallback(
  embedUrls: string[],
  referer?: string
): Promise<ExtractedStream | null> {
  if (!embedUrls?.length) return null;

  const primary = embedUrls.slice(0, 3);
  const rest = embedUrls.slice(3);

  // Race first 3
  const raceResults = await Promise.allSettled(
    primary.map((url) =>
      Promise.race([
        extractStreamFromEmbed(url, referer),
        new Promise<null>((r) => setTimeout(() => r(null), 7000)),
      ])
    )
  );

  for (const r of raceResults) {
    if (r.status === "fulfilled" && r.value?.url) return r.value;
  }

  // Sequential for the rest
  for (const url of rest) {
    try {
      const result = await Promise.race([
        extractStreamFromEmbed(url, referer),
        new Promise<null>((r) => setTimeout(() => r(null), 5000)),
      ]);
      if (result?.url) return result;
    } catch {}
  }

  return null;
}

/** يحول الـ stream URL إلى proxied URL للبلاير */
export function toProxiedUrl(streamUrl: string, referer?: string): string {
  if (!streamUrl) return streamUrl;
  if (streamUrl.startsWith("/api/proxy")) return streamUrl;
  const refParam = referer ? `&ref=${encodeURIComponent(referer)}` : "";
  return `/api/proxy/stream?url=${encodeURIComponent(streamUrl)}${refParam}`;
}
