import { NextRequest, NextResponse } from "next/server";
import { getYacineChannels } from "@/lib/yacine";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const categoryId = parseInt(searchParams.get("categoryId") || "0", 10);

  if (!categoryId) {
    return NextResponse.json({ success: false, error: "Missing categoryId" }, { status: 400 });
  }

  try {
    const channels = await getYacineChannels(categoryId);
    return NextResponse.json({ success: true, data: channels });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
