import { NextResponse } from "next/server";
import { getYacineEvents } from "@/lib/yacine";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const events = await getYacineEvents();
    return NextResponse.json({ success: true, data: events });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
