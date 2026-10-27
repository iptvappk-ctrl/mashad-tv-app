import { NextRequest, NextResponse } from "next/server";
import { getArabseedDetail } from "@/lib/arabseed";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const pageUrl = searchParams.get("url");

  if (!pageUrl) {
    return NextResponse.json({ success: false, error: "Missing url parameter" }, { status: 400 });
  }

  try {
    const detail = await getArabseedDetail(pageUrl);
    if (!detail) {
      return NextResponse.json({ success: false, error: "Not found" }, { status: 404 });
    }

    // Format directStream with proxy if present
    let proxiedStream = detail.directStream;
    if (proxiedStream) {
      proxiedStream = `/api/proxy/stream?url=${encodeURIComponent(proxiedStream)}&ref=${encodeURIComponent("https://govid.live/")}`;
    }

    return NextResponse.json({
      success: true,
      data: {
        ...detail,
        proxiedStream,
      },
    });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
