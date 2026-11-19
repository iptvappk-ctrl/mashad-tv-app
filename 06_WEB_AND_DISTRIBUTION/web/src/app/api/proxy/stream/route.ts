import { NextRequest, NextResponse } from "next/server";
import { isAllowedProxyUrl, checkRateLimit } from "@/lib/security";
import { isServiceAllowed } from "@/lib/server-license";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  if (!await isServiceAllowed()) {
    return new NextResponse("Service temporarily suspended.", { status: 403 });
  }

  const ip = req.headers.get("x-forwarded-for") || "127.0.0.1";
  if (!checkRateLimit(ip, 1200, 60000)) {
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
      Connection: "keep-alive",
    };
    if (referer) {
      upstreamHeaders["Referer"] = referer;
    } else if (targetUrl.includes("datahowa.asia") || targetUrl.includes("govid.live")) {
      upstreamHeaders["Referer"] = "https://govid.live/";
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

    const contentType = (response.headers.get("content-type") || "").toLowerCase();
    const isM3u8 =
      targetUrl.includes(".m3u8") ||
      contentType.includes("application/vnd.apple.mpegurl") ||
      contentType.includes("application/x-mpegurl") ||
      contentType.includes("text/plain");

    // Case 1: M3U8 Playlist -> Rewrite segment URLs to pass through proxy
    if (isM3u8 && response.ok) {
      const playlistText = await response.text();
      const actualBaseUrl = response.url || targetUrl;
      const parsedTarget = new URL(actualBaseUrl);

      const rewritten = playlistText
        .split("\n")
        .map((line) => {
          const trimmed = line.trim();
          if (!trimmed || trimmed.startsWith("#")) {
            // Check for URI inside tags like #EXT-X-KEY:METHOD=AES-128,URI="..."
            if (trimmed.startsWith("#EXT-X-KEY:") && trimmed.includes('URI="')) {
              return trimmed.replace(/URI="([^"]+)"/, (_, uri) => {
                let fullKeyUrl = uri;
                if (!uri.startsWith("http")) {
                  try {
                    const resolvedKey = new URL(uri, parsedTarget.href);
                    if (!resolvedKey.search && parsedTarget.search) {
                      resolvedKey.search = parsedTarget.search;
                    }
                    fullKeyUrl = resolvedKey.toString();
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

          // Line is a segment or sub-playlist URL
          let fullSegmentUrl = trimmed;
          if (!trimmed.startsWith("http")) {
            try {
              const resolved = new URL(trimmed, parsedTarget.href);
              if (!resolved.search && parsedTarget.search) {
                resolved.search = parsedTarget.search;
              }
              fullSegmentUrl = resolved.toString();
            } catch {
              return line;
            }
          }

          const effectiveRef = referer || (targetUrl.includes("govid.live") ? "https://govid.live/" : "");
          return `/api/proxy/stream?url=${encodeURIComponent(fullSegmentUrl)}&ua=${encodeURIComponent(userAgent)}&ref=${encodeURIComponent(effectiveRef)}`;
        })
        .join("\n");

      return new NextResponse(rewritten, {
        status: response.status,
        headers: {
          "Content-Type": "application/vnd.apple.mpegurl",
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
          "Cache-Control": targetUrl.includes("live") ? "no-cache" : "public, max-age=30",
        },
      });
    }

    // Case 2: Binary Video Chunk (.ts, .mp4, audio, disguised TS segments)
    const responseHeaders = new Headers();
    responseHeaders.set("Access-Control-Allow-Origin", "*");
    responseHeaders.set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
    responseHeaders.set("Cache-Control", "public, max-age=86400, immutable");

    const isTsChunk =
      targetUrl.includes(".ts") ||
      targetUrl.includes(".pdf") ||
      targetUrl.includes(".js") ||
      targetUrl.includes("/0021012254/") ||
      contentType.includes("video/mp2t") ||
      contentType.includes("image/jpeg") ||
      contentType.includes("application/octet-stream");

    if (isTsChunk && response.ok) {
      responseHeaders.set("Content-Type", "video/mp2t");

      const arrayBuffer = await response.arrayBuffer();
      const buffer = Buffer.from(arrayBuffer);
      const syncByteIndex = buffer.indexOf(0x47);

      // Strip any dummy junk bytes (e.g. 149-byte fake JPEG header prepended by govid/datahowa)
      let finalBuffer = buffer;
      if (syncByteIndex > 0) {
        finalBuffer = buffer.subarray(syncByteIndex);
      }

      responseHeaders.set("Content-Length", finalBuffer.length.toString());

      return new NextResponse(finalBuffer, {
        status: response.status,
        headers: responseHeaders,
      });
    }

    // Default binary streaming
    if (contentType) {
      responseHeaders.set("Content-Type", contentType);
    }
    const contentLength = response.headers.get("content-length");
    if (contentLength) responseHeaders.set("Content-Length", contentLength);

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
