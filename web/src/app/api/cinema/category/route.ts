import { NextRequest, NextResponse } from "next/server";
import { superAggregator } from "@/lib/providers/super-aggregator";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const slug = searchParams.get("slug") || "movies";
  const page = parseInt(searchParams.get("page") || "1", 10);

  try {
    const items = await superAggregator.getCategoryWithFallback(slug, page, 6000);
    return NextResponse.json({ success: true, data: items });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
