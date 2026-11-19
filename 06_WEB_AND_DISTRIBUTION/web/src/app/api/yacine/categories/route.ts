import { NextResponse } from "next/server";
import { getYacineCategories } from "@/lib/yacine";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const categories = await getYacineCategories();
    return NextResponse.json({ success: true, data: categories });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message }, { status: 500 });
  }
}
