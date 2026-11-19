import { NextRequest, NextResponse } from "next/server";
import { aggregator } from "@/lib/providers/aggregator";
import { isServiceAllowed } from "@/lib/server-license";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  if (!await isServiceAllowed()) {
    return NextResponse.json({ success: false, error: "Service temporarily suspended." }, { status: 403 });
  }

  const { searchParams } = new URL(req.url);
  const serverUrl = searchParams.get("serverUrl");
  const pageUrl = searchParams.get("pageUrl") || undefined;

  if (!serverUrl) {
    return NextResponse.json({ success: false, error: "Missing serverUrl" }, { status: 400 });
  }

  try {
    const direct = await aggregator.resolveStream(serverUrl, pageUrl);
    const finalUrl = direct || serverUrl;

    let playUrl = finalUrl;
    if (!playUrl.startsWith("/api/proxy/stream") && (finalUrl.includes(".m3u8") || finalUrl.includes("datahowa"))) {
      playUrl = `/api/proxy/stream?url=${encodeURIComponent(finalUrl)}&ref=${encodeURIComponent("https://govid.live/")}`;
    }

    return NextResponse.json({
      success: true,
      playUrl,
      rawUrl: finalUrl,
      data: {
        rawUrl: finalUrl,
        playUrl,
      },
    });
  } catch (err: any) {
    return NextResponse.json({
      success: true,
      playUrl: serverUrl,
      rawUrl: serverUrl,
      data: {
        rawUrl: serverUrl,
        playUrl: serverUrl,
      },
    });
  }
}
