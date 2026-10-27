import { NextRequest, NextResponse } from "next/server";
import { resolveDirectStream } from "@/lib/arabseed";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const serverUrl = searchParams.get("serverUrl");

  if (!serverUrl) {
    return NextResponse.json({ success: false, error: "Missing serverUrl" }, { status: 400 });
  }

  try {
    const direct = await resolveDirectStream(serverUrl);
    if (!direct) {
      return NextResponse.json({
        success: false,
        error: "Could not resolve direct stream for this server",
      });
    }

    const proxied = `/api/proxy/stream?url=${encodeURIComponent(direct)}&ref=${encodeURIComponent("https://govid.live/")}`;
    return NextResponse.json({
      success: true,
      playUrl: proxied,
      rawUrl: direct,
      data: {
        rawUrl: direct,
        playUrl: proxied,
      },
    });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
