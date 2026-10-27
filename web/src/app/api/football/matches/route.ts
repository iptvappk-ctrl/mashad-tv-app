import { NextResponse } from "next/server";
import { getLiveMatches } from "@/lib/football";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const matches = await getLiveMatches();
    return NextResponse.json({ success: true, data: matches });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
