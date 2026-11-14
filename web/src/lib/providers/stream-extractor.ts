/**
 * stream-extractor.ts
 * Core engine for extracting direct video URLs (m3u8/mp4) from embed players.
 * Supports: Govid/DataHowa, Doodstream, Streamlare, Vidbam/Vidlox, Uqload,
 *           Mixdrop, Streamtape, Fembed, Mp4upload, Vidoza, Voe.sx,
 *           Filelions/Fviplions, Turboviplay, Vikingfile, Generic iframe.
 */

export interface ExtractedStream {
  url: string;
  type: 'hls' | 'mp4' | 'unknown';
  quality?: string;
  referer?: string;
}

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

// --- Regex Patterns ---
const M3U8_PATTERNS = [
  /file\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /src\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /hls\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /"file"\s*:\s*"([^"]+\.m3u8[^"]*)"/i,
  /source\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /playlist\s*:\s*["']([^"']+\.m3u8[^"']*)/i,
  /["'](https?:\/\/[^"']+\.m3u8[^"']*)/i,
  /masterPlaylist\s*:\s*["']([^"']+)/i,
  /hlsUrl\s*:\s*["']([^"']+)/i,
];

const MP4_PATTERNS = [
  /file\s*:\s*["']([^"']+\.mp4[^"']*)/i,
  /"file"\s*:\s*"([^"]+\.mp4[^"]*)"/i,
  /source\s*:\s*["']([^"']+\.mp4[^"']*)/i,
  /["'](https?:\/\/[^"']+\.mp4[^"']*)/i,
  /"src"\s*:\s*"([^"]+\.mp4[^"]*)"/i,
];

async function fetchPageHtml(url: string, referer?: string, timeoutMs = 6000): Promise<string> {
  const controller = new AbortController();
  const tid = setTimeout(() => controller.abort(), timeoutMs);
  try {
    // Try via proxy first (server-side safe)
    const proxyUrl = `/api/proxy/page?url=${encodeURIComponent(url)}`;
    const res = await fetch(proxyUrl, {
      headers: { "User-Agent": UA, Referer: referer || url },
      signal: controller.signal,
    });
    if (res.ok) return await res.text();
    // Direct fallback
    const res2 = await fetch(url, {
      headers: { "User-Agent": UA, Referer: referer || url },
    });
    return res2.ok ? await res2.text() : "";
  } catch {
    return "";
  } finally {
    clearTimeout(tid);
  }
}

function findM3u8(html: string): string | null {
  for (const pattern of M3U8_PATTERNS) {
    const m = html.match(pattern);
    if (m?.[1] && m[1].startsWith("http")) return m[1];
  }
  return null;
}

function findMp4(html: string): string | null {
  for (const pattern of MP4_PATTERNS) {
    const m = html.match(pattern);
    if (m?.[1] && m[1].startsWith("http")) return m[1];
  }
  return null;
}

// --- Per-provider extractors ---

async function extractGovid(html: string, embedUrl: string): Promise<ExtractedStream | null> {
  try {
    // DataHowa/Govid: POST to /api with video id
    const idMatch = embedUrl.match(/\/(?:v|embed|e)\/([a-zA-Z0-9_-]+)/);
    if (!idMatch) return null;
    const vid = idMatch[1];
    const apiUrl = embedUrl.replace(/\/(?:v|embed|e)\/[^/?]+.*/, `/api/source/${vid}`);
    const controller = new AbortController();
    const tid = setTimeout(() => controller.abort(), 5000);
    try {
      const res = await fetch(apiUrl, {
        method: "POST",
        headers: {
          "User-Agent": UA,
          "Referer": embedUrl,
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: `r=&d=${encodeURIComponent(new URL(embedUrl).hostname)}`,
        signal: controller.signal,
      });
      if (res.ok) {
        const json = await res.json();
        if (json?.data) {
          // Sort by quality descending
          const sources: Array<{ file: string; label: string; type: string }> = Array.isArray(json.data) ? json.data : [json.data];
          const hls = sources.find(s => s.file?.includes(".m3u8"));
          const mp4 = sources.find(s => s.file?.includes(".mp4"));
          if (hls?.file) return { url: hls.file, type: "hls", referer: embedUrl };
          if (mp4?.file) return { url: mp4.file, type: "mp4", referer: embedUrl };
        }
      }
    } finally {
      clearTimeout(tid);
    }
    // Fallback: scan page source
    const m3u8 = findM3u8(html);
    if (m3u8) return { url: m3u8, type: "hls", referer: embedUrl };
    return null;
  } catch {
    return null;
  }
}

async function extractDoodstream(html: string, embedUrl: string): Promise<ExtractedStream | null> {
  try {
    // Step 1: get pass_md5 token
    const passMatch = html.match(/pass_md5[^"']*["']([^"']+)/);
    const tokenMatch = html.match(/\?token=([^&"'\s]+)/);
    if (passMatch?.[1]) {
      const passUrl = passMatch[1];
      const controller = new AbortController();
      const tid = setTimeout(() => controller.abort(), 5000);
      try {
        const res = await fetch(passUrl, {
          headers: { "User-Agent": UA, Referer: embedUrl },
          signal: controller.signal,
        });
        if (res.ok) {
          const base = await res.text();
          const token = tokenMatch?.[1] || Date.now().toString();
          const streamUrl = `${base.trim()}${Date.now().toString().slice(0, 10)}?token=${token}&expiry=${Date.now() + 86400000}`;
          if (streamUrl.startsWith("http")) {
            return { url: streamUrl, type: "mp4", referer: embedUrl };
          }
        }
      } finally {
        clearTimeout(tid);
      }
    }
    // Dood /d/ pattern
    const dMatch = html.match(/["'](https?:\/\/(?:doodstream|dood)\.[^"']+\/d\/[^"']+)/i);
    if (dMatch?.[1]) return { url: dMatch[1], type: "mp4", referer: embedUrl };
    return null;
  } catch {
    return null;
  }
}

async function extractStreamlare(html: string, embedUrl: string): Promise<ExtractedStream | null> {
  try {
    const idMatch = embedUrl.match(/\/(?:v|e)\/([a-zA-Z0-9]+)/);
    if (!idMatch) return null;
    const id = idMatch[1];
    const apiUrl = `https://streamlare.com/api/video/get`;
    const controller = new AbortController();
    const tid = setTimeout(() => controller.abort(), 5000);
    try {
      const res = await fetch(apiUrl, {
        method: "POST",
        headers: {
          "User-Agent": UA,
          "Referer": embedUrl,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ id }),
        signal: controller.signal,
      });
      if (res.ok) {
        const json = await res.json();
        const sources = json?.result;
        if (sources) {
          const keys = Object.keys(sources);
          for (const k of keys) {
            const src = sources[k]?.src;
            if (src?.includes(".m3u8")) return { url: src, type: "hls", referer: embedUrl };
          }
          for (const k of keys) {
            const src = sources[k]?.src;
            if (src?.includes(".mp4")) return { url: src, type: "mp4", referer: embedUrl };
          }
        }
      }
    } finally {
      clearTimeout(tid);
    }
    const m3u8 = findM3u8(html);
    if (m3u8) return { url: m3u8, type: "hls", referer: embedUrl };
    return null;
  } catch {
    return null;
  }
}

function extractVidbam(html: string, embedUrl: string): ExtractedStream | null {
  try {
    const m3u8 = findM3u8(html);
    if (m3u8) return { url: m3u8, type: "hls", referer: embedUrl };
    const mp4 = findMp4(html);
    if (mp4) return { url: mp4, type: "mp4", referer: embedUrl };
    // Look for sources JSON array
    const srcMatch = html.match(/sources\s*:\s*\[([^\]]+)\]/i);
    if (srcMatch) {
      const fileMatch = srcMatch[1].match(/file\s*:\s*["']([^"']+)/i);
      if (fileMatch?.[1] && fileMatch[1].startsWith("http")) {
        const url = fileMatch[1];
        return { url, type: url.includes(".m3u8") ? "hls" : "mp4", referer: embedUrl };
      }
    }
    return null;
  } catch {
    return null;
  }
}

function extractUqload(html: string, embedUrl: string): ExtractedStream | null {
  try {
    // sources:[{file:"...",type:"video/mp4"}]
    const srcMatch = html.match(/sources\s*:\s*\[\s*\{[^}]*file\s*:\s*["']([^"']+)/i);
    if (srcMatch?.[1] && srcMatch[1].startsWith("http")) {
      const url = srcMatch[1];
      return { url, type: url.includes(".m3u8") ? "hls" : "mp4", referer: embedUrl };
    }
    const mp4 = findMp4(html);
    if (mp4) return { url: mp4, type: "mp4", referer: embedUrl };
    return null;
  } catch {
    return null;
  }
}

function extractMixdrop(html: string, embedUrl: string): ExtractedStream | null {
  try {
    // Mixdrop uses obfuscated eval() - look for MDCore.ref
    const refMatch = html.match(/MDCore\.ref\s*=\s*["']([^"']+)/i);
    const vidMatch = html.match(/MDCore\.v\s*=\s*["']([^"']+)/i);
    if (refMatch?.[1]) {
      const url = refMatch[1].startsWith("http") ? refMatch[1] : `https:${refMatch[1]}`;
      return { url, type: url.includes(".m3u8") ? "hls" : "mp4", referer: embedUrl };
    }
    // Fallback: scan for direct URLs in eval blocks
    const evalMatch = html.match(/eval\(function[^)]+\)\s*\{([^}]{200,})\}/s);
    if (evalMatch) {
      const decoded = evalMatch[0];
      const m3u8 = findM3u8(decoded);
      if (m3u8) return { url: m3u8, type: "hls", referer: embedUrl };
      const mp4 = findMp4(decoded);
      if (mp4) return { url: mp4, type: "mp4", referer: embedUrl };
    }
    return null;
  } catch {
    return null;
  }
}

function extractStreamtape(html: string, embedUrl: string): ExtractedStream | null {
  try {
    // Streamtape: document.getElementById('robotlink').innerHTML = ...
    const robotMatch = html.match(/getElementById\s*\(\s*['"]robotlink['"]\s*\)\s*\.innerHTML\s*=\s*['"]([^'"]+)/i);
    if (robotMatch?.[1]) {
      let url = robotMatch[1].replace(/\\'/g, "'").replace(/\\\//g, "/");
      if (!url.startsWith("http")) url = `https:${url}`;
      return { url, type: "mp4", referer: embedUrl };
    }
    // Alternative: 'https://streamtape...'
    const stMatch = html.match(/['"]?(https?:\/\/streamtape\.[^'"?\s]+\.mp4[^'"?\s]*)/i);
    if (stMatch?.[1]) return { url: stMatch[1], type: "mp4", referer: embedUrl };
    // Look for /get_video pattern
    const getMatch = html.match(/\/get_video\?[^"'\s]+/);
    if (getMatch) {
      return { url: `https://streamtape.com${getMatch[0]}`, type: "mp4", referer: embedUrl };
    }
    return null;
  } catch {
    return null;
  }
}

async function extractFembed(html: string, embedUrl: string): Promise<ExtractedStream | null> {
  try {
    const idMatch = embedUrl.match(/\/(?:v|e)\/([a-zA-Z0-9]+)/);
    if (!idMatch) return null;
    const id = idMatch[1];
    const base = new URL(embedUrl).origin;
    const apiUrl = `${base}/api/source/${id}`;
    const controller = new AbortController();
    const tid = setTimeout(() => controller.abort(), 5000);
    try {
      const res = await fetch(apiUrl, {
        method: "POST",
        headers: { "User-Agent": UA, Referer: embedUrl, "Content-Type": "application/x-www-form-urlencoded" },
        body: "r=&d=" + encodeURIComponent(new URL(embedUrl).hostname),
        signal: controller.signal,
      });
      if (res.ok) {
        const json = await res.json();
        if (json?.success && Array.isArray(json.data)) {
          const hls = json.data.find((s: any) => s.file?.includes(".m3u8"));
          const mp4 = json.data.find((s: any) => s.file?.includes(".mp4"));
          if (hls?.file) return { url: hls.file, type: "hls", quality: hls.label, referer: embedUrl };
          if (mp4?.file) return { url: mp4.file, type: "mp4", quality: mp4.label, referer: embedUrl };
        }
      }
    } finally {
      clearTimeout(tid);
    }
    const m3u8 = findM3u8(html);
    if (m3u8) return { url: m3u8, type: "hls", referer: embedUrl };
    return null;
  } catch {
    return null;
  }
}

function extractMp4upload(html: string, embedUrl: string): ExtractedStream | null {
  try {
    // sources:[{src:"...",type:"video/mp4"}]
    const srcMatch = html.match(/src\s*:\s*["'](https?:\/\/[^"']+\.mp4[^"']*)/i);
    if (srcMatch?.[1]) return { url: srcMatch[1], type: "mp4", referer: embedUrl };
    const mp4 = findMp4(html);
    if (mp4) return { url: mp4, type: "mp4", referer: embedUrl };
    return null;
  } catch {
    return null;
  }
}

function extractVidoza(html: string, embedUrl: string): ExtractedStream | null {
  try {
    // sourcesCode: [{src:"...", res:1080, label:"1080p"}]
    const srcMatch = html.match(/sourcesCode\s*:\s*\[([^\]]+)\]/i);
    if (srcMatch) {
      const fileMatch = srcMatch[1].match(/src\s*:\s*["']([^"']+)/i);
      if (fileMatch?.[1] && fileMatch[1].startsWith("http")) {
        const url = fileMatch[1];
        return { url, type: url.includes(".m3u8") ? "hls" : "mp4", referer: embedUrl };
      }
    }
    const mp4 = findMp4(html);
    if (mp4) return { url: mp4, type: "mp4", referer: embedUrl };
    return null;
  } catch {
    return null;
  }
}

async function extractVoe(html: string, embedUrl: string): Promise<ExtractedStream | null> {
  try {
    // Voe.sx: hls: 'https://...'
    const hlsMatch = html.match(/hls\s*:\s*['"]([^'"]+\.m3u8[^'"]*)/i);
    if (hlsMatch?.[1] && hlsMatch[1].startsWith("http")) {
      return { url: hlsMatch[1], type: "hls", referer: embedUrl };
    }
    // mp4
    const mp4Match = html.match(/mp4\s*:\s*['"]([^'"]+\.mp4[^'"]*)/i);
    if (mp4Match?.[1] && mp4Match[1].startsWith("http")) {
      return { url: mp4Match[1], type: "mp4", referer: embedUrl };
    }
    // Redirect pattern
    const redirectMatch = html.match(/window\.location\.href\s*=\s*['"]([^'"]+)/i);
    if (redirectMatch?.[1] && redirectMatch[1].startsWith("http")) {
      const redirectHtml = await fetchPageHtml(redirectMatch[1], embedUrl, 5000);
      const m3u8 = findM3u8(redirectHtml);
      if (m3u8) return { url: m3u8, type: "hls", referer: embedUrl };
    }
    const m3u8 = findM3u8(html);
    if (m3u8) return { url: m3u8, type: "hls", referer: embedUrl };
    return null;
  } catch {
    return null;
  }
}

function extractFilelions(html: string, embedUrl: string): ExtractedStream | null {
  try {
    // m3u8 in sources
    const m3u8 = findM3u8(html);
    if (m3u8) return { url: m3u8, type: "hls", referer: embedUrl };
    // Look for master.m3u8 specifically
    const masterMatch = html.match(/["'](https?:\/\/[^"']+master\.m3u8[^"']*)/i);
    if (masterMatch?.[1]) return { url: masterMatch[1], type: "hls", referer: embedUrl };
    return null;
  } catch {
    return null;
  }
}

function extractTurboviplay(html: string, embedUrl: string): ExtractedStream | null {
  try {
    const m3u8 = findM3u8(html);
    if (m3u8) return { url: m3u8, type: "hls", referer: embedUrl };
    const mp4 = findMp4(html);
    if (mp4) return { url: mp4, type: "mp4", referer: embedUrl };
    // Look for stream config object
    const cfgMatch = html.match(/config\s*=\s*\{[^}]*["']?sources["']?\s*:\s*\[([^\]]+)\]/is);
    if (cfgMatch) {
      const fileMatch = cfgMatch[1].match(/file\s*:\s*["']([^"']+)/i);
      if (fileMatch?.[1] && fileMatch[1].startsWith("http")) {
        const url = fileMatch[1];
        return { url, type: url.includes(".m3u8") ? "hls" : "mp4", referer: embedUrl };
      }
    }
    return null;
  } catch {
    return null;
  }
}

function extractVikingfile(html: string, embedUrl: string): ExtractedStream | null {
  try {
    const m3u8 = findM3u8(html);
    if (m3u8) return { url: m3u8, type: "hls", referer: embedUrl };
    const mp4 = findMp4(html);
    if (mp4) return { url: mp4, type: "mp4", referer: embedUrl };
    return null;
  } catch {
    return null;
  }
}

function detectProvider(url: string): string {
  if (url.includes("govid") || url.includes("datahowa") || url.includes("4shared")) return "govid";
  if (url.includes("doodstream") || url.includes("dood.") || url.includes("ds2play") || url.includes("doods.pro")) return "dood";
  if (url.includes("streamlare")) return "streamlare";
  if (url.includes("vidbam") || url.includes("vidlox")) return "vidbam";
  if (url.includes("uqload")) return "uqload";
  if (url.includes("mixdrop")) return "mixdrop";
  if (url.includes("streamtape")) return "streamtape";
  if (url.includes("fembed") || url.includes("feurl") || url.includes("fcdn.stream")) return "fembed";
  if (url.includes("mp4upload")) return "mp4upload";
  if (url.includes("vidoza")) return "vidoza";
  if (url.includes("voe.sx")) return "voe";
  if (url.includes("filelions") || url.includes("fviplions") || url.includes("listeamed") || url.includes("vgfplay")) return "filelions";
  if (url.includes("turboviplay")) return "turboviplay";
  if (url.includes("vikingfile")) return "vikingfile";
  return "generic";
}

export async function extractStreamFromEmbed(embedUrl: string, referer?: string): Promise<ExtractedStream | null> {
  if (!embedUrl) return null;
  // Already a direct stream
  if (embedUrl.includes(".m3u8") || embedUrl.endsWith(".mp4")) {
    return { url: embedUrl, type: embedUrl.includes(".m3u8") ? "hls" : "mp4", referer };
  }

  try {
    const provider = detectProvider(embedUrl);
    const html = await fetchPageHtml(embedUrl, referer || embedUrl, 7000);
    if (!html) return null;

    let result: ExtractedStream | null = null;

    switch (provider) {
      case "govid":
        result = await extractGovid(html, embedUrl);
        break;
      case "dood":
        result = await extractDoodstream(html, embedUrl);
        break;
      case "streamlare":
        result = await extractStreamlare(html, embedUrl);
        break;
      case "vidbam":
        result = extractVidbam(html, embedUrl);
        break;
      case "uqload":
        result = extractUqload(html, embedUrl);
        break;
      case "mixdrop":
        result = extractMixdrop(html, embedUrl);
        break;
      case "streamtape":
        result = extractStreamtape(html, embedUrl);
        break;
      case "fembed":
        result = await extractFembed(html, embedUrl);
        break;
      case "mp4upload":
        result = extractMp4upload(html, embedUrl);
        break;
      case "vidoza":
        result = extractVidoza(html, embedUrl);
        break;
      case "voe":
        result = await extractVoe(html, embedUrl);
        break;
      case "filelions":
        result = extractFilelions(html, embedUrl);
        break;
      case "turboviplay":
        result = extractTurboviplay(html, embedUrl);
        break;
      case "vikingfile":
        result = extractVikingfile(html, embedUrl);
        break;
      default:
        // Generic: try all patterns
        result = findM3u8(html) ? { url: findM3u8(html)!, type: "hls", referer: embedUrl } : null;
        if (!result) result = findMp4(html) ? { url: findMp4(html)!, type: "mp4", referer: embedUrl } : null;
        // Look for iframe src and recurse once
        if (!result) {
          const iframeMatch = html.match(/<iframe[^>]+src=["']([^"']+)/i);
          if (iframeMatch?.[1] && iframeMatch[1] !== embedUrl) {
            const iframeUrl = iframeMatch[1].startsWith("//") ? `https:${iframeMatch[1]}` : iframeMatch[1];
            if (iframeUrl.startsWith("http")) {
              result = await extractStreamFromEmbed(iframeUrl, embedUrl);
            }
          }
        }
        break;
    }

    return result;
  } catch {
    return null;
  }
}

export async function extractStreamWithFallback(embedUrls: string[], referer?: string): Promise<ExtractedStream | null> {
  if (!embedUrls || embedUrls.length === 0) return null;

  for (const url of embedUrls) {
    try {
      const result = await Promise.race([
        extractStreamFromEmbed(url, referer),
        new Promise<null>((resolve) => setTimeout(() => resolve(null), 7000)),
      ]);
      if (result) return result;
    } catch {
      // continue
    }
  }
  return null;
}
