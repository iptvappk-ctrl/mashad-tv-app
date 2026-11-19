/**
 * 📋 Providers Hub API
 * GET /api/cinema/providers
 */

import { NextResponse } from "next/server";
import { ALL_PROVIDERS } from "../../../lib/providers/super-aggregator";

export const dynamic = "force-dynamic";

export async function GET() {
  const providersList = ALL_PROVIDERS.map((p) => ({
    id: p.id,
    name: p.name,
    baseUrl: p.baseUrl,
    isEnabled: p.isEnabled,
  }));

  return NextResponse.json({
    success: true,
    total: providersList.length,
    activeProviders: providersList,
    categories: [
      { slug: "movies", name: "أحدث الأفلام" },
      { slug: "series", name: "أحدث المسلسلات" },
      { slug: "arabic_movies", name: "أفلام عربية ومصرية" },
      { slug: "arabic_series", name: "مسلسلات عربية" },
      { slug: "turkish_series", name: "مسلسلات تركية مترجمة" },
      { slug: "turkish_dubbed", name: "مسلسلات تركية مدبلجة" },
      { slug: "turkish_movies", name: "أفلام تركية" },
      { slug: "anime", name: "عالم الأنمي وسبيستون" },
      { slug: "anime_dubbed", name: "أنمي مدبلج بالعربية" },
      { slug: "anime_subbed", name: "أنمي مترجم" },
      { slug: "foreign_movies", name: "أفلام أجنبية وهوليوود" },
      { slug: "foreign_series", name: "مسلسلات أجنبية" },
      { slug: "ramadan", name: "الموسم الرمضاني" },
    ],
    features: {
      zeroAds: true,
      smartFallback: true,
      instantPlay: true,
      hlsProxy: true,
      directDownload: true,
    },
  });
}
