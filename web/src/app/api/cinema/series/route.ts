import { NextRequest, NextResponse } from "next/server";
import { superAggregator } from "@/lib/providers/super-aggregator";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const page = parseInt(searchParams.get("page") || "1", 10);

  try {
    const series = await superAggregator.getCategoryWithFallback("series", page, 6000);
    return NextResponse.json({ success: true, data: series });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
