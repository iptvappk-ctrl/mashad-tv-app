/**
 * 🚀 Super Resolve API
 * GET /api/cinema/superresolve
 * 
 * يستخرج stream مباشر (m3u8/mp4) بدون إعلانات
 * مع Smart Fallback عبر كل السيرفرات المتاحة
 * 
 * Query Params:
 *   serverUrl  — رابط سيرفر واحد للاستخراج
 *   embedUrls  — قائمة روابط مفصولة بفاصلة (fallback chain)
 *   referer    — optional referer header
 */

import { NextRequest, NextResponse } from "next/server";
import { checkRateLimit } from "@/lib/security";
import { superAggregator } from "@/lib/providers/super-aggregator";
import { extractStreamFromEmbed, extractStreamWithFallback, toProxiedUrl } from "@/lib/providers/stream-extractor";
import { isServiceAllowed } from "@/lib/server-license";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function GET(req: NextRequest) {
  if (!await isServiceAllowed()) {
    return NextResponse.json({ success: false, error: "Service temporarily suspended." }, { status: 403 });
  }

  const ip = req.headers.get("x-forwarded-for") || "127.0.0.1";
  if (!checkRateLimit(ip, 300, 60000)) {
    return NextResponse.json({ success: false, error: "Too Many Requests" }, { status: 429 });
  }

  const { searchParams } = new URL(req.url);
  const serverUrl = searchParams.get("serverUrl");
  const embedUrlsParam = searchParams.get("embedUrls");
  const referer = searchParams.get("referer") || undefined;

  // Validate: need at least one URL
  if (!serverUrl && !embedUrlsParam) {
    return NextResponse.json(
      { success: false, error: "Missing serverUrl or embedUrls parameter" },
      { status: 400 }
    );
  }

  try {
    // Case 1: Multiple embed URLs with fallback chain
    if (embedUrlsParam) {
      const embedUrls = embedUrlsParam
        .split(",")
        .map((u) => u.trim())
        .filter((u) => u.startsWith("http"));

      if (embedUrls.length === 0) {
        return NextResponse.json({ success: false, error: "No valid URLs provided" }, { status: 400 });
      }

      const result = await extractStreamWithFallback(embedUrls, referer);

      if (result?.url) {
        const proxied = toProxiedUrl(result.url, result.referer || referer);
        return NextResponse.json({
          success: true,
          playUrl: proxied,
          rawUrl: result.url,
          type: result.type,
          quality: result.quality || "HD",
        });
      }

      // Fallback: return first URL as embed proxy
      const embedFallback = `/api/proxy/embed?url=${encodeURIComponent(embedUrls[0])}`;
      return NextResponse.json({
        success: true,
        playUrl: embedFallback,
        rawUrl: embedUrls[0],
        type: "embed",
        quality: "unknown",
      });
    }

    // Case 2: Single server URL
    if (serverUrl) {
      // If already a direct stream
      if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) {
        const proxied = toProxiedUrl(serverUrl, referer);
        return NextResponse.json({
          success: true,
          playUrl: proxied,
          rawUrl: serverUrl,
          type: serverUrl.includes(".m3u8") ? "hls" : "mp4",
          quality: "HD",
        });
      }

      // Try super aggregator resolver (provider-specific + stream extractor)
      const resolvedUrl = await superAggregator.resolveSingleServer(serverUrl);

      if (resolvedUrl) {
        // Check if it's a direct stream or embed fallback
        const isEmbed = resolvedUrl.includes("/api/proxy/embed");
        const rawUrl = isEmbed ? serverUrl : resolvedUrl.replace(/^\/api\/proxy\/stream\?url=/, "");

        return NextResponse.json({
          success: true,
          playUrl: resolvedUrl,
          rawUrl: isEmbed ? serverUrl : serverUrl,
          type: isEmbed ? "embed" : resolvedUrl.includes(".m3u8") ? "hls" : "mp4",
          quality: "HD",
        });
      }

      // Last resort: proxy embed
      const embedFallback = `/api/proxy/embed?url=${encodeURIComponent(serverUrl)}`;
      return NextResponse.json({
        success: true,
        playUrl: embedFallback,
        rawUrl: serverUrl,
        type: "embed",
        quality: "unknown",
      });
    }
  } catch (err: any) {
    console.error("[SuperResolve] Error:", err?.message);
    // Always return something playable, never crash the client
    const fallbackUrl = serverUrl || embedUrlsParam?.split(",")[0] || "";
    return NextResponse.json({
      success: true,
      playUrl: fallbackUrl ? `/api/proxy/embed?url=${encodeURIComponent(fallbackUrl)}` : "",
      rawUrl: fallbackUrl,
      type: "embed",
      quality: "unknown",
      error: err?.message,
    });
  }

  return NextResponse.json({ success: false, error: "Unexpected state" }, { status: 500 });
}
