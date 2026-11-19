/**
 * 🕷️ Real-Time Crawler API
 * GET /api/crawler
 * POST /api/crawler/trigger
 */

import { NextRequest, NextResponse } from "next/server";
import { autoCrawler } from "../../lib/crawler/auto-crawler";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const limit = parseInt(searchParams.get("limit") || "50", 10);

  const releases = autoCrawler.getLatestReleases(limit);

  return NextResponse.json({
    success: true,
    total: releases.length,
    releases,
  });
}

export async function POST() {
  // فحص يدوي فوري لكافة المواقع
  const newlyDiscovered = await autoCrawler.crawlAllFeeds();

  return NextResponse.json({
    success: true,
    discoveredCount: newlyDiscovered.length,
    newItems: newlyDiscovered,
  });
}
