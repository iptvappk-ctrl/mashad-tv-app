/**
 * 🎚️ Stream Quality Controller API
 * GET /api/cinema/quality?streamUrl=...&referer=...
 * 
 * يرجع كافة الجودات المتاحة للمشغل (4K, 1080p, 720p, 480p, 360p, Auto)
 * مع روابط البروكسي النظيفة الخالية من الإعلانات
 */

import { NextRequest, NextResponse } from "next/server";
import { buildQualityPackage } from "../../../lib/quality/quality-controller";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const streamUrl = searchParams.get("streamUrl");
  const referer = searchParams.get("referer") || undefined;

  if (!streamUrl) {
    return NextResponse.json(
      { success: false, error: "Missing required streamUrl parameter" },
      { status: 400 }
    );
  }

  try {
    const qualityPackage = await buildQualityPackage(streamUrl, undefined, referer);

    return NextResponse.json({
      success: true,
      data: qualityPackage,
    });
  } catch (err: any) {
    return NextResponse.json(
      { success: false, error: err.message || "Failed to parse qualities" },
      { status: 500 }
    );
  }
}
