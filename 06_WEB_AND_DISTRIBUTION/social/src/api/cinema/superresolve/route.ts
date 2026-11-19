/**
 * 🚀 Super Resolve API Endpoint
 * GET /api/cinema/superresolve
 * 
 * يستخرج رابط البث المباشر (HLS / m3u8 / MP4) المعقم من الإعلانات
 * مع ميزة التنقل الفوري بين السيرفرات (Smart Fallback)
 * 
 * Parameters:
 *   - serverUrl: رابط سيرفر محدد
 *   - embedUrls: قائمة سيرفرات مفصولة بفواصل (server1,server2,server3)
 *   - referer: اختياري لتخطي حماية الهيدرز
 */

import { NextRequest, NextResponse } from "next/server";
import { superAggregator } from "../../../lib/providers/super-aggregator";
import { extractStreamWithFallback, toProxiedUrl } from "../../../lib/extractors/stream-extractor";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const serverUrl = searchParams.get("serverUrl");
  const embedUrlsParam = searchParams.get("embedUrls");
  const referer = searchParams.get("referer") || undefined;

  if (!serverUrl && !embedUrlsParam) {
    return NextResponse.json(
      { success: false, error: "Missing required query parameter: serverUrl or embedUrls" },
      { status: 400 }
    );
  }

  try {
    // الحالة الأولى: استلام مصفوفة سيرفرات لتنفيذ Fallback تلقائي
    if (embedUrlsParam) {
      const urls = embedUrlsParam
        .split(",")
        .map((u) => u.trim())
        .filter((u) => u.startsWith("http"));

      if (urls.length === 0) {
        return NextResponse.json({ success: false, error: "No valid URLs provided" }, { status: 400 });
      }

      const result = await extractStreamWithFallback(urls, referer);
      if (result?.url) {
        const proxied = toProxiedUrl(result.url, result.referer || referer);
        return NextResponse.json({
          success: true,
          playUrl: proxied,
          rawUrl: result.url,
          type: result.type,
          quality: result.quality || "HD",
          fallbackUsed: urls.indexOf(result.referer || urls[0]) > 0,
        });
      }

      // إذا تعذر استخراج الرابط المباشر، يتم تحويل الأول إلى embed معقم
      return NextResponse.json({
        success: true,
        playUrl: `/api/proxy/embed?url=${encodeURIComponent(urls[0])}`,
        rawUrl: urls[0],
        type: "embed",
        quality: "unknown",
        fallbackUsed: false,
      });
    }

    // الحالة الثانية: سيرفر فردي
    if (serverUrl) {
      if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) {
        return NextResponse.json({
          success: true,
          playUrl: toProxiedUrl(serverUrl, referer),
          rawUrl: serverUrl,
          type: serverUrl.includes(".m3u8") ? "hls" : "mp4",
          quality: "HD",
        });
      }

      const resolved = await superAggregator.resolveSingleServer(serverUrl);
      const isEmbed = resolved?.includes("/api/proxy/embed");

      return NextResponse.json({
        success: true,
        playUrl: resolved || serverUrl,
        rawUrl: serverUrl,
        type: isEmbed ? "embed" : resolved?.includes(".m3u8") ? "hls" : "mp4",
        quality: "HD",
      });
    }
  } catch (err: any) {
    console.error("[SuperResolve API Error]:", err?.message);
    const fallback = serverUrl || embedUrlsParam?.split(",")[0] || "";
    return NextResponse.json({
      success: true,
      playUrl: fallback ? `/api/proxy/embed?url=${encodeURIComponent(fallback)}` : "",
      rawUrl: fallback,
      type: "embed",
      quality: "unknown",
      error: err?.message,
    });
  }

  return NextResponse.json({ success: false, error: "Internal resolver error" }, { status: 500 });
}
