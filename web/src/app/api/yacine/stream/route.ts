import { NextRequest, NextResponse } from "next/server";
import { getStreamsForChannel, getStreamsForEvent } from "@/lib/yacine";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const type = searchParams.get("type"); // "channel" or "event"
  const id = parseInt(searchParams.get("id") || "0", 10);

  if (!id || (type !== "channel" && type !== "event")) {
    return NextResponse.json({ success: false, error: "Invalid parameters" }, { status: 400 });
  }

  try {
    const rawStreams = type === "channel" ? await getStreamsForChannel(id) : await getStreamsForEvent(id);

    // Format streams with direct and proxied playback URLs
    const formatted = rawStreams.map((s) => {
      const proxiedUrl = `/api/proxy/stream?url=${encodeURIComponent(s.url)}&ua=${encodeURIComponent(s.userAgent)}&ref=${encodeURIComponent(s.referer)}`;
      return {
        name: s.name,
        rawUrl: s.url,
        playUrl: proxiedUrl,
        userAgent: s.userAgent,
        referer: s.referer,
        urlType: s.urlType,
      };
    });

    return NextResponse.json({ success: true, data: formatted });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
