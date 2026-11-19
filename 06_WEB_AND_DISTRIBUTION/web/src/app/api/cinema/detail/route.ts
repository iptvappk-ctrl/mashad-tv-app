import { NextRequest, NextResponse } from "next/server";
import { aggregator } from "@/lib/providers/aggregator";
import { resolveDirectStream } from "@/lib/arabseed";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const pageUrl = searchParams.get("url");

  if (!pageUrl) {
    return NextResponse.json({ success: false, error: "Missing url parameter" }, { status: 400 });
  }

  try {
    const detail = await aggregator.getDetail(pageUrl);
    if (!detail) {
      return NextResponse.json({ success: false, error: "Not found" }, { status: 404 });
    }

    // Clean server names: strip [عرب سيد] and other provider tags
    const cleanedServers = (detail.servers || []).map((s, idx) => ({
      ...s,
      name: s.name.replace(/^\[.*?\]\s*/, "") || `سيرفر ${idx + 1}`,
    }));

    // Pre-resolve 1st server for instant playback when user clicks "شاهد الآن"
    let proxiedStream = detail.directStream;
    if (!proxiedStream && cleanedServers.length > 0) {
      try {
        const resolved = await resolveDirectStream(cleanedServers[0].embedUrl);
        if (resolved) proxiedStream = resolved;
      } catch {}
    }

    if (proxiedStream && !proxiedStream.startsWith("/api/proxy/stream") && (proxiedStream.includes(".m3u8") || proxiedStream.includes("datahowa"))) {
      proxiedStream = `/api/proxy/stream?url=${encodeURIComponent(proxiedStream)}&ref=${encodeURIComponent("https://govid.live/")}`;
    }

    return NextResponse.json({
      success: true,
      data: {
        ...detail,
        servers: cleanedServers,
        proxiedStream,
      },
    });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
