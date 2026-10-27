import { NextRequest, NextResponse } from "next/server";
import { searchArabseed } from "@/lib/arabseed";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const q = searchParams.get("q") || "";

  if (!q.trim()) {
    return NextResponse.json({ success: true, data: [] });
  }

  try {
    const results = await searchArabseed(q);
    return NextResponse.json({ success: true, data: results });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
