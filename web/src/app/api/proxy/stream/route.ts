import { NextRequest, NextResponse } from "next/server";
import { isAllowedProxyUrl, checkRateLimit } from "@/lib/security";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const ip = req.headers.get("x-forwarded-for") || "127.0.0.1";
  if (!checkRateLimit(ip, 600, 60000)) {
    return new NextResponse("Too Many Requests", { status: 429 });
  }

  const { searchParams } = new URL(req.url);
  const targetUrl = searchParams.get("url");

  if (!targetUrl) {
    return new NextResponse("Missing url parameter", { status: 400 });
  }

  if (!isAllowedProxyUrl(targetUrl)) {
    return new NextResponse("Forbidden target URL", { status: 403 });
  }

  const userAgent =
    searchParams.get("ua") ||
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
  const referer = searchParams.get("ref") || "";

  try {
    const upstreamHeaders: Record<string, string> = {
      "User-Agent": userAgent,
    };
    if (referer) {
      upstreamHeaders["Referer"] = referer;
    }

    // Forward range header if browser requests partial video bytes
    const rangeHeader = req.headers.get("range");
    if (rangeHeader) {
      upstreamHeaders["Range"] = rangeHeader;
    }

    const response = await fetch(targetUrl, {
      headers: upstreamHeaders,
      redirect: "follow",
    });

    const contentType = response.headers.get("content-type") || "";
    const isM3u8 =
      targetUrl.includes(".m3u8") ||
      contentType.includes("application/vnd.apple.mpegurl") ||
      contentType.includes("application/x-mpegurl") ||
      contentType.includes("text/plain");

    // Case 1: M3U8 Playlist -> Rewrite segment URLs to pass through proxy
    if (isM3u8 && response.ok) {
      const playlistText = await response.text();
      const baseUrl = targetUrl.substring(0, targetUrl.lastIndexOf("/") + 1);

      const rewritten = playlistText
        .split("\n")
        .map((line) => {
          const trimmed = line.trim();
          if (!trimmed || trimmed.startsWith("#")) {
            // Check for URI inside tags like #EXT-X-KEY:METHOD=AES-128,URI="..."
            if (trimmed.startsWith("#EXT-X-KEY:") && trimmed.includes('URI="')) {
              return trimmed.replace(/URI="([^"]+)"/, (_, uri) => {
                const fullUri = uri.startsWith("http") ? uri : new URL(uri, baseUrl).toString();
                const proxied = `/api/proxy/stream?url=${encodeURIComponent(fullUri)}&ua=${encodeURIComponent(userAgent)}&ref=${encodeURIComponent(referer)}`;
                return `URI="${proxied}"`;
              });
            }
            return line;
          }

          // Line is a segment or sub-playlist URL
          let fullSegmentUrl = trimmed;
          if (!trimmed.startsWith("http")) {
            try {
              fullSegmentUrl = new URL(trimmed, baseUrl).toString();
            } catch {
              return line;
            }
          }

          return `/api/proxy/stream?url=${encodeURIComponent(fullSegmentUrl)}&ua=${encodeURIComponent(userAgent)}&ref=${encodeURIComponent(referer)}`;
        })
        .join("\n");

      return new NextResponse(rewritten, {
        status: response.status,
        headers: {
          "Content-Type": "application/vnd.apple.mpegurl",
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
          "Cache-Control": "no-cache, no-store, must-revalidate",
        },
      });
    }

    // Case 2: Binary Video Chunk (.ts, .mp4, audio) -> Stream directly
    const responseHeaders = new Headers();
    responseHeaders.set("Access-Control-Allow-Origin", "*");
    responseHeaders.set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
    if (contentType) responseHeaders.set("Content-Type", contentType);

    const contentLength = response.headers.get("content-length");
    if (contentLength) responseHeaders.set("Content-Length", contentLength);

    const contentRange = response.headers.get("content-range");
    if (contentRange) responseHeaders.set("Content-Range", contentRange);

    const acceptRanges = response.headers.get("accept-ranges");
    if (acceptRanges) responseHeaders.set("Accept-Ranges", acceptRanges);

    return new NextResponse(response.body, {
      status: response.status,
      headers: responseHeaders,
    });
  } catch (err: any) {
    console.error("Stream proxy error:", err.message);
    return new NextResponse(`Proxy error: ${err.message}`, { status: 502 });
  }
}

export async function OPTIONS() {
  return new NextResponse(null, {
    status: 204,
    headers: {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
      "Access-Control-Allow-Headers": "*",
    },
  });
}
