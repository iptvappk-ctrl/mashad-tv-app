import { NextRequest, NextResponse } from "next/server";
import { getArabseedSeries } from "@/lib/arabseed";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const page = parseInt(searchParams.get("page") || "1", 10);

  try {
    const series = await getArabseedSeries(page);
    return NextResponse.json({ success: true, data: series });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
