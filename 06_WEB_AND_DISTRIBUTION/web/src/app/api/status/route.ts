import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";
export const revalidate = 0;

export async function GET() {
  try {
    const res = await fetch("https://zaqzouka-dashboard-api.vercel.app/api/status", {
      headers: { Accept: "application/json" },
      cache: "no-store",
      next: { revalidate: 0 },
    });
    if (res.ok) {
      const data = await res.json();
      return NextResponse.json(data, {
        headers: {
          "Cache-Control": "no-store, no-cache, must-revalidate",
        },
      });
    }
  } catch (e) {
    // ignore
  }

  return NextResponse.json(
    { status: "active", message: "المنظومة نشطة" },
    { headers: { "Cache-Control": "no-store, no-cache, must-revalidate" } }
  );
}
