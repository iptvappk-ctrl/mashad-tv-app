import { NextRequest, NextResponse } from "next/server";
import { aggregator } from "@/lib/providers/aggregator";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const slug = searchParams.get("slug") || "movies";
  const page = parseInt(searchParams.get("page") || "1", 10);

  try {
    const items = await aggregator.getCategory(slug, page);
    return NextResponse.json({ success: true, data: items });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
