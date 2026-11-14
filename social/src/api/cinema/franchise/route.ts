/**
 * 🎬 Smart Media Linker & Franchise API
 * GET /api/cinema/franchise?title=...
 * 
 * يرجع:
 * 1. الشجرة الكاملة للمسلسل بجميع مواسمه وحلقاته المربوطة تلقائياً
 * 2. أو جميع أجزاء سلسلة الأفلام المرتبطة ببعضها (Part 1, 2, 3...)
 */

import { NextRequest, NextResponse } from "next/server";
import { smartMediaLinker } from "../../../lib/linker/smart-media-linker";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const title = searchParams.get("title");

  if (!title) {
    return NextResponse.json({
      success: true,
      stats: smartMediaLinker.getStats(),
      message: "Pass ?title=... to retrieve unified series or movie franchise tree",
    });
  }

  // 1. فحص إذا كان العمل يمثل سلسلة أفلام (Franchise)
  const franchise = smartMediaLinker.getFranchise(title);
  if (franchise && franchise.parts.length > 0) {
    return NextResponse.json({
      success: true,
      type: "franchise",
      data: franchise,
    });
  }

  // 2. فحص إذا كان العمل يمثل مسلسلاً بمواسم وحلقات
  const seriesTree = smartMediaLinker.getSeriesTree(title);
  if (seriesTree) {
    // تحويل الـ Maps إلى مصفوفات JSON متوافقة
    const seasonsArray = Array.from(seriesTree.seasons.entries()).map(([sNum, season]) => ({
      seasonNumber: sNum,
      title: season.title,
      episodes: Array.from(season.episodes.values()),
    }));

    return NextResponse.json({
      success: true,
      type: "series",
      data: {
        canonicalId: seriesTree.canonicalId,
        title: seriesTree.title,
        poster: seriesTree.poster,
        latestSeason: seriesTree.latestSeason,
        latestEpisode: seriesTree.latestEpisode,
        seasons: seasonsArray,
      },
    });
  }

  // 3. تحليل العنوان بالذكاء الاصطناعي إذا لم يكن مسجلاً بعد
  const parsed = smartMediaLinker.parseTitle(title);

  return NextResponse.json({
    success: true,
    type: "parsed",
    data: parsed,
  });
}
