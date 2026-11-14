/**
 * 🌙 Ramadan Series Super API
 * GET /api/cinema/ramadan
 * 
 * Query Params:
 *   - category: 'all' | 'egyptian' | 'syrian' | 'gulf' | 'maghreb' | 'comedy' | 'action'
 *   - seriesId: جلب حلقات وتفاصيل مسلسل رمضاني محدد
 *   - live: 'true' لجلب أحدث الحلقات المعروضة اليوم فوراً
 */

import { NextRequest, NextResponse } from "next/server";
import { ramadanEngine, RamadanCategory } from "../../../lib/ramadan/ramadan-engine";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const category = (searchParams.get("category") || "all") as RamadanCategory;
  const seriesId = searchParams.get("seriesId");
  const isLive = searchParams.get("live") === "true";

  try {
    // 1. طلب حلقات مسلسل رمضاني محدد
    if (seriesId) {
      const data = await ramadanEngine.getSeriesEpisodes(seriesId);
      return NextResponse.json({
        success: true,
        data,
      });
    }

    // 2. طلب البث الحي لجديد رمضان اليوم
    if (isLive) {
      const liveFeed = await ramadanEngine.fetchLiveRamadanFeed();
      return NextResponse.json({
        success: true,
        total: liveFeed.length,
        items: liveFeed,
      });
    }

    // 3. طلب قائمة المسلسلات حسب التصنيف
    const seriesList = await ramadanEngine.getSeriesByCategory(category);

    return NextResponse.json({
      success: true,
      category,
      total: seriesList.length,
      series: seriesList,
      categories: [
        { key: "all", name: "كل مسلسلات رمضان" },
        { key: "egyptian", name: "مسلسلات مصرية" },
        { key: "syrian", name: "مسلسلات سورية وشامية" },
        { key: "gulf", name: "مسلسلات خليجية" },
        { key: "comedy", name: "كوميديا رمضان" },
        { key: "action", name: "أكشن وتشويق" },
      ],
    });
  } catch (err: any) {
    return NextResponse.json(
      { success: false, error: err.message || "Ramadan API error" },
      { status: 500 }
    );
  }
}
