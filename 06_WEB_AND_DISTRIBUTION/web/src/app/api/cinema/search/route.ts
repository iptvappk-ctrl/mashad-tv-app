import { NextRequest, NextResponse } from "next/server";
import { superAggregator } from "@/lib/providers/super-aggregator";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const q = searchParams.get("q") || "";

  if (!q.trim()) {
    return NextResponse.json({ success: true, data: [] });
  }

  try {
    const results = await superAggregator.searchAll(q, 6000);
    return NextResponse.json({ success: true, data: results });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
