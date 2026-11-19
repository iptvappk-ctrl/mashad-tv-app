/**
 * 🛡️ Stream Proxy & Ad-Stripper Engine
 * GET /api/proxy/stream?url=...&ref=...
 * 
 * الميزات الفائقة:
 * 1. فك تشفير وتعديل قوائم M3U8 لإعادة توجيه كل الـ Segments من خلال البروكسي
 * 2. إزالة أي فواصل إعلانية (Discontinuity tags / Ad cues) المدمجة في البث
 * 3. دعم Range Requests (206 Partial Content) لتقديم وتأخير الفيديو فورياً بدون لاج
 * 4. دعم تحميل مقاطع الفيديو والمسلسلات بأقصى سرعة ممكنة
 * 5. تجاوز قيود CORS وموانع Referer و Cloudflare headers
 */

import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const DEFAULT_UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const targetUrl = searchParams.get("url");

  if (!targetUrl) {
    return new NextResponse("Missing url parameter", { status: 400 });
  }

  const userAgent = searchParams.get("ua") || DEFAULT_UA;
  const referer = searchParams.get("ref") || "";

  try {
    const upstreamHeaders: Record<string, string> = {
      "User-Agent": userAgent,
      Connection: "keep-alive",
    };

    if (referer) {
      upstreamHeaders["Referer"] = referer;
    } else {
      try {
        upstreamHeaders["Referer"] = new URL(targetUrl).origin + "/";
      } catch {}
    }

    // تمرير هيدر الـ Range لتسريع التحميل والـ Seek
    const rangeHeader = req.headers.get("range");
    if (rangeHeader) {
      upstreamHeaders["Range"] = rangeHeader;
    }

    const response = await fetch(targetUrl, {
      headers: upstreamHeaders,
      redirect: "follow",
    });

    const contentType = (response.headers.get("content-type") || "").toLowerCase();
    const isM3u8 =
      targetUrl.includes(".m3u8") ||
      contentType.includes("application/vnd.apple.mpegurl") ||
      contentType.includes("application/x-mpegurl") ||
      contentType.includes("text/plain");

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // الحالة 1: ملف تشغيل HLS M3U8 (إعادة صياغة وتنظيف)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    if (isM3u8 && response.ok) {
      const playlistText = await response.text();
      const actualBaseUrl = response.url || targetUrl;
      const parsedTarget = new URL(actualBaseUrl);

      const rewritten = playlistText
        .split("\n")
        .map((line) => {
          const trimmed = line.trim();

          // تخطي أسطر الفواصل الإعلانية المزعجة
          if (trimmed.includes("#EXT-X-DISCONTINUITY") && trimmed.includes("AD")) {
            return "";
          }

          if (!trimmed || trimmed.startsWith("#")) {
            // معالجة مفاتيح التشفير AES-128
            if (trimmed.startsWith("#EXT-X-KEY:") && trimmed.includes('URI="')) {
              return trimmed.replace(/URI="([^"]+)"/, (_, uri) => {
                let fullKeyUrl = uri;
                if (!uri.startsWith("http")) {
                  try {
                    fullKeyUrl = new URL(uri, parsedTarget.href).toString();
                  } catch {
                    fullKeyUrl = uri;
                  }
                }
                const proxied = `/api/proxy/stream?url=${encodeURIComponent(fullKeyUrl)}&ua=${encodeURIComponent(userAgent)}&ref=${encodeURIComponent(referer || parsedTarget.origin)}`;
                return `URI="${proxied}"`;
              });
            }
            return line;
          }

          // هذا السطر يمثل مقطع فيديو (.ts chunk) أو Sub-playlist
          let fullSegmentUrl = trimmed;
          if (!trimmed.startsWith("http")) {
            try {
              fullSegmentUrl = new URL(trimmed, parsedTarget.href).toString();
            } catch {
              return line;
            }
          }

          const effectiveRef = referer || parsedTarget.origin + "/";
          return `/api/proxy/stream?url=${encodeURIComponent(fullSegmentUrl)}&ua=${encodeURIComponent(userAgent)}&ref=${encodeURIComponent(effectiveRef)}`;
        })
        .filter(Boolean)
        .join("\n");

      return new NextResponse(rewritten, {
        status: response.status,
        headers: {
          "Content-Type": "application/vnd.apple.mpegurl",
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
          "Cache-Control": targetUrl.includes("live") ? "no-cache" : "public, max-age=60",
        },
      });
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // الحالة 2: مقاطع الفيديو والملفات الثنائية (.ts / .mp4 / تحميل مباشر)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    const responseHeaders = new Headers();
    responseHeaders.set("Access-Control-Allow-Origin", "*");
    responseHeaders.set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
    responseHeaders.set("Access-Control-Expose-Headers", "Content-Range, Content-Length, Accept-Ranges");
    responseHeaders.set("Accept-Ranges", "bytes");

    if (contentType) {
      responseHeaders.set("Content-Type", contentType);
    } else if (targetUrl.includes(".mp4")) {
      responseHeaders.set("Content-Type", "video/mp4");
    } else {
      responseHeaders.set("Content-Type", "video/MP2T");
    }

    const contentLength = response.headers.get("content-length");
    if (contentLength) responseHeaders.set("Content-Length", contentLength);

    const contentRange = response.headers.get("content-range");
    if (contentRange) responseHeaders.set("Content-Range", contentRange);

    return new NextResponse(response.body, {
      status: response.status,
      headers: responseHeaders,
    });
  } catch (err: any) {
    return new NextResponse(`Proxy Stream Error: ${err.message}`, { status: 502 });
  }
}
