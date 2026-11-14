import { NextRequest, NextResponse } from "next/server";

export interface RecapChannel {
  name: string;
  channelId: string;
  emoji: string;
}

export interface MovieRecapItem {
  id: string;
  title: string;
  channelName: string;
  channelId: string;
  publishedAt: string;
  thumbnailUrl: string;
  videoUrl: string;
}

export const RECAP_CHANNELS: RecapChannel[] = [
  { name: "كوكلي Cokley", channelId: "UCY0hnHS0P2e63w0h834YHqw", emoji: "🎙️" },
  { name: "كوكلي 2", channelId: "UCSPGHRzefMndkpMqRHZplaA", emoji: "🎬" },
  { name: "الخلاصة Extra", channelId: "UCZFjMYifBVsil4YLg9g4rMQ", emoji: "⚡" },
  { name: "الملخص Mo5alas", channelId: "UCEH9vQrM8F71Ht4lqin3bog", emoji: "🍿" },
  { name: "Film Gdeed", channelId: "UC7fYsd6YpOq8LhA9eMYy3qQ", emoji: "🎞️" },
  { name: "مدفع الأفلام", channelId: "UC6RorEP9vIC8dNfb4jX32qg", emoji: "💣" },
  { name: "تلخيصة FILM", channelId: "UCkC5_-qsYxF8b3Toj0rO2XQ", emoji: "📽️" },
  { name: "مروان أحمد", channelId: "UC5gArfM8TG6jyR441LYM52A", emoji: "🌟" },
  { name: "البوب Cokley", channelId: "UCS--g81AqlXG4ik9Xm47QPQ", emoji: "🍿" },
  { name: "مستر كوكلي", channelId: "UCr_bGuXC-v1H4jfLXWCbOTw", emoji: "🎩" },
  { name: "في كلمة ونص", channelId: "UCW8Xf9MFA76rvAvSwpuK4qQ", emoji: "⏱️" },
  { name: "فيلم في الزتونة", channelId: "UCtpH2xoaAVfySd_UoVO54EA", emoji: "🫒" },
  { name: "أشرحلي فيلم", channelId: "UCeCRMlOvq8XNjHm3Zd3QMgQ", emoji: "💡" },
  { name: "فيلمي فيلمك", channelId: "UCzJSTqSLK1w4otc27MZS3mQ", emoji: "🎥" },
  { name: "ملخصات الشاشة", channelId: "UCA9TC_cXejDARvl-VkX99qQ", emoji: "📺" },
  { name: "ملخص فيلم في البيكو", channelId: "UCeBthVbKg6B6742FgYK7j3w", emoji: "☕" },
  { name: "MoviesLimbo", channelId: "UCh3HYI_do6uwzM6mS86wFMQ", emoji: "🪐" },
  { name: "Wael Cokley", channelId: "UC9h-Q9yhgZZO9ta7tfHNigQ", emoji: "🎬" },
  { name: "عرش السيما", channelId: "UCfIB_5-dlRTeYax2Jh42QQA", emoji: "👑" },
  { name: "نايت موفي", channelId: "UCHlrPATgHt19EbT5Npka78A", emoji: "🌙" },
  { name: "أوفردوز أفلام", channelId: "UC4YnFnndBBeSOAI2BiakiXg", emoji: "🔥" },
  { name: "Movies Killer", channelId: "UC6Ydi1nQuNMejko4q0NWEhg", emoji: "⚔️" },
  { name: "محترف موفيز", channelId: "UCsqradczzQY7fTKpDCVP9QA", emoji: "🎯" },
  { name: "إكسلانس أفلام", channelId: "UCGV-VN8Ui_NAJrcZcs-SSRg", emoji: "💎" },
  { name: "فيلم في الخلاصة", channelId: "UChO6HRowsTQHmELCeGy4G_Q", emoji: "💡" },
];

// In-memory cache for ultra-fast response
let cachedRecaps: MovieRecapItem[] = [];
let lastFetchTime = 0;
const CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour

function parseInitialData(html: string, channel: RecapChannel): MovieRecapItem[] {
  const match = html.match(/var ytInitialData\s*=\s*(\{[\s\S]*?\});<\/script>/);
  if (!match) return [];

  try {
    const data = JSON.parse(match[1]);
    const items: MovieRecapItem[] = [];
    const seen = new Set<string>();

    function walk(obj: any) {
      if (!obj || typeof obj !== "object") return;

      // Type 1: Standard videoRenderer / gridVideoRenderer
      if (obj.videoId && typeof obj.videoId === "string" && obj.videoId.length === 11 && obj.title) {
        const id = obj.videoId;
        if (!seen.has(id)) {
          seen.add(id);
          const title = obj.title.runs
            ? obj.title.runs.map((r: any) => r.text).join("")
            : obj.title.simpleText || "";

          if (title && !title.toLowerCase().includes("#shorts")) {
            items.push({
              id,
              title: title.trim(),
              channelName: channel.name,
              channelId: channel.channelId,
              publishedAt: new Date().toISOString(),
              thumbnailUrl: `https://i.ytimg.com/vi/${id}/hqdefault.jpg`,
              videoUrl: `https://www.youtube.com/watch?v=${id}`,
            });
          }
        }
      }

      // Type 2: Modern lockupViewModel
      if (obj.lockupViewModel && obj.lockupViewModel.contentId) {
        const m = obj.lockupViewModel;
        const id = m.contentId;
        if (typeof id === "string" && id.length === 11 && !seen.has(id)) {
          seen.add(id);
          const title = m.metadata?.lockupMetadataViewModel?.title?.content || "";
          if (title && !title.toLowerCase().includes("#shorts")) {
            items.push({
              id,
              title: title.trim(),
              channelName: channel.name,
              channelId: channel.channelId,
              publishedAt: new Date().toISOString(),
              thumbnailUrl: `https://i.ytimg.com/vi/${id}/hqdefault.jpg`,
              videoUrl: `https://www.youtube.com/watch?v=${id}`,
            });
          }
        }
      }

      // Recurse into children
      for (const key of Object.keys(obj)) {
        walk(obj[key]);
      }
    }

    walk(data);
    return items;
  } catch {
    return [];
  }
}

async function fetchChannelVideos(channel: RecapChannel): Promise<MovieRecapItem[]> {
  const url = `https://www.youtube.com/channel/${channel.channelId}/videos`;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 9000);

  try {
    const res = await fetch(url, {
      signal: controller.signal,
      headers: {
        "User-Agent":
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept-Language": "ar,en-US;q=0.9,en;q=0.8",
      },
    });
    clearTimeout(timeoutId);

    if (!res.ok) return [];
    const html = await res.text();
    return parseInitialData(html, channel);
  } catch {
    clearTimeout(timeoutId);
    return [];
  }
}

async function getAllRecaps(): Promise<MovieRecapItem[]> {
  const now = Date.now();
  if (cachedRecaps.length > 0 && now - lastFetchTime < CACHE_TTL_MS) {
    return cachedRecaps;
  }

  // Crawl channels in chunks of 5
  const allResults: MovieRecapItem[] = [];
  const chunkSize = 5;

  for (let i = 0; i < RECAP_CHANNELS.length; i += chunkSize) {
    const chunk = RECAP_CHANNELS.slice(i, i + chunkSize);
    const chunkResults = await Promise.all(chunk.map((ch) => fetchChannelVideos(ch)));
    for (const res of chunkResults) {
      allResults.push(...res);
    }
  }

  if (allResults.length > 0) {
    // Deduplicate by ID
    const uniqueMap = new Map<string, MovieRecapItem>();
    for (const item of allResults) {
      if (!uniqueMap.has(item.id)) {
        uniqueMap.set(item.id, item);
      }
    }

    const items = Array.from(uniqueMap.values());
    cachedRecaps = items;
    lastFetchTime = now;
    return items;
  }

  return cachedRecaps;
}

export async function GET(request: NextRequest) {
  try {
    const searchParams = request.nextUrl.searchParams;
    const channelId = searchParams.get("channelId") || "";
    const query = searchParams.get("q") || "";
    const limit = Math.min(Number(searchParams.get("limit")) || 150, 300);

    const recaps = await getAllRecaps();

    let filtered = recaps;

    if (channelId) {
      filtered = filtered.filter((item) => item.channelId === channelId);
    }

    if (query.trim()) {
      const q = query.trim().toLowerCase();
      filtered = filtered.filter(
        (item) =>
          item.title.toLowerCase().includes(q) ||
          item.channelName.toLowerCase().includes(q)
      );
    }

    return NextResponse.json({
      success: true,
      channels: RECAP_CHANNELS,
      total: filtered.length,
      data: filtered.slice(0, limit),
    });
  } catch (error) {
    console.error("Error fetching recaps:", error);
    return NextResponse.json(
      {
        success: false,
        error: "فشل تحميل ملخصات الأفلام",
        channels: RECAP_CHANNELS,
        data: [],
      },
      { status: 500 }
    );
  }
}
