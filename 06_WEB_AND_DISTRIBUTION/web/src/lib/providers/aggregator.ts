import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem } from "./types";
import { titlesStrictlyMatch } from "../title-matcher";
import { arabseedProvider } from "./arabseed";
import { akwamProvider } from "./akwam";
import { wecimaProvider } from "./wecima";
import { faselHDProvider } from "./faselhd";
import { prestigeProvider } from "./prestige";
import { esheeqProvider } from "./esheeq";
import { egybestProvider } from "./egybest";
import { lodynetProvider } from "./lodynet";

// In-Memory Multi-Tier Cache
const searchCache = new Map<string, { items: UnifiedMediaItem[]; time: number }>();
const categoryCache = new Map<string, { items: UnifiedMediaItem[]; time: number }>();
const detailCache = new Map<string, { detail: UnifiedMediaDetail; time: number }>();
const ogImageCache = new Map<string, string>();

const SEARCH_TTL = 10 * 60 * 1000;
const CATEGORY_TTL = 15 * 60 * 1000;
const DETAIL_TTL = 30 * 60 * 1000;

// Fetch og:image directly from the source website page
async function fetchOgImage(pageUrl: string): Promise<string | null> {
  try {
    const cached = ogImageCache.get(pageUrl);
    if (cached) return cached;

    const res = await fetch(`/api/proxy/page?url=${encodeURIComponent(pageUrl)}`, {
      next: { revalidate: 3600 },
    });
    if (!res.ok) return null;
    const html = await res.text();

    // Extract og:image from meta tag
    const match = html.match(/<meta[^>]+(?:property|name)=["']og:image["'][^>]+content=["']([^"']+)["']/i)
      || html.match(/<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["']og:image["']/i);

    const poster = match?.[1];
    if (poster && poster.startsWith("http")) {
      ogImageCache.set(pageUrl, poster);
      return poster;
    }
    return null;
  } catch {
    return null;
  }
}

// Enrich items missing posters by fetching og:image from source website
async function enrichFromSource(items: UnifiedMediaItem[]): Promise<UnifiedMediaItem[]> {
  const needsPoster = items.filter(
    (it) => !it.poster || it.poster.length < 5 || it.poster.includes("logo") || !it.poster.startsWith("http")
  );
  if (needsPoster.length === 0) return items;

  const enriched = new Map<string, string>();

  // Batch 6 at a time, max 12 total
  for (let i = 0; i < Math.min(needsPoster.length, 12); i += 6) {
    const batch = needsPoster.slice(i, i + 6);
    const results = await Promise.allSettled(
      batch.map(async (it) => {
        const poster = await fetchOgImage(it.pageUrl);
        return { url: it.pageUrl, poster };
      })
    );
    for (const r of results) {
      if (r.status === "fulfilled" && r.value.poster) {
        enriched.set(r.value.url, r.value.poster);
      }
    }
  }

  return items.map((it) =>
    (!it.poster || it.poster.length < 5 || it.poster.includes("logo") || !it.poster.startsWith("http")) &&
    enriched.has(it.pageUrl)
      ? { ...it, poster: enriched.get(it.pageUrl)! }
      : it
  );
}

// Normalize Arabic titles for smart deduplication
export function normalizeTitle(str: string): string {
  if (!str) return "";
  return str
    .toLowerCase()
    .replace(/[أإآ]/g, "ا")
    .replace(/ة/g, "ه")
    .replace(/ى/g, "ي")
    .replace(/[\u064B-\u0652]/g, "")
    .replace(/\b(فيلم|مسلسل|مترجم|مدبلج|كامل|اون\s*لاين|بجودة|عالية|جديد|حصري|hd|fhd|4k|1080p|720p)\b/gi, "")
    .replace(/[^\w\s\u0600-\u06FF]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export class MultiProviderAggregator {
  providers: BaseProvider[] = [
    arabseedProvider,
    akwamProvider,
    wecimaProvider,
    faselHDProvider,
    esheeqProvider,
    egybestProvider,
  ];

  getProvider(id: string): BaseProvider | undefined {
    return this.providers.find((p) => p.id === id);
  }

  getProviderForUrl(url: string): BaseProvider {
    if (url.includes("akw.cam") || url.includes("akwam")) return akwamProvider;
    if (url.includes("wecima") || url.includes("mywecima") || url.includes("myweecima")) return wecimaProvider;
    if (url.includes("fasel") || url.includes("fasel4k")) return faselHDProvider;
    if (url.includes("brstej")) return prestigeProvider;
    if (url.includes("3sk") || url.includes("q-ask") || url.includes("esheeq")) return esheeqProvider;
    if (url.includes("egybest")) return egybestProvider;
    if (url.includes("lodynet")) return lodynetProvider;
    return arabseedProvider;
  }

  async searchAll(query: string, timeoutMs = 6000): Promise<UnifiedMediaItem[]> {
    const q = query.trim();
    if (!q) return [];

    const cacheKey = `search:${q.toLowerCase()}`;
    const cached = searchCache.get(cacheKey);
    if (cached && Date.now() - cached.time < SEARCH_TTL) return cached.items;

    const searchPromises = this.providers
      .filter((p) => p.isEnabled)
      .map(async (provider) => {
        try {
          return await Promise.race([
            provider.search(q),
            new Promise<UnifiedMediaItem[]>((resolve) => setTimeout(() => resolve([]), timeoutMs)),
          ]);
        } catch {
          return [];
        }
      });

    const results = await Promise.allSettled(searchPromises);
    const rawItems: UnifiedMediaItem[] = [];
    for (const res of results) {
      if (res.status === "fulfilled" && Array.isArray(res.value)) rawItems.push(...res.value);
    }

    const seen = new Map<string, UnifiedMediaItem>();
    for (const it of rawItems) {
      const norm = normalizeTitle(it.title);
      if (!norm || norm.length < 2) continue;
      if (!seen.has(norm)) {
        seen.set(norm, it);
      } else {
        const existing = seen.get(norm)!;
        if (
          (existing.poster.includes("logo") || !existing.poster.startsWith("http")) &&
          it.poster && it.poster.startsWith("http") && !it.poster.includes("logo")
        ) {
          seen.set(norm, it);
        }
      }
    }

    const merged = Array.from(seen.values()).filter((it) => titlesStrictlyMatch(q, it.title));
    const cachedAt = Date.now();
    if (merged.length > 0) searchCache.set(cacheKey, { items: merged, time: cachedAt });
    // Poster repair is opportunistic; never hold search results while opening
    // up to twelve unrelated detail pages just to find missing artwork.
    void enrichFromSource(merged).then((enriched) => {
      const current = searchCache.get(cacheKey);
      if (enriched.length > 0 && current?.time === cachedAt) {
        searchCache.set(cacheKey, { items: enriched, time: cachedAt });
      }
    }).catch(() => {});
    return merged;
  }

  async getCategory(slug: string, page = 1, timeoutMs = 6000): Promise<UnifiedMediaItem[]> {
    const cacheKey = `cat:${slug}:${page}`;
    const cached = categoryCache.get(cacheKey);
    if (cached && Date.now() - cached.time < CATEGORY_TTL) return cached.items;

    let targetProviders: BaseProvider[] = [arabseedProvider];

    if (slug === "ramadan") {
      targetProviders = [arabseedProvider, prestigeProvider, wecimaProvider];
    } else if (slug === "turkish_series" || slug === "turkish_movies") {
      targetProviders = [arabseedProvider, esheeqProvider, wecimaProvider, lodynetProvider];
    } else if (slug === "movies" || slug === "foreign_movies") {
      targetProviders = [arabseedProvider, akwamProvider, faselHDProvider, wecimaProvider, egybestProvider];
    } else if (slug === "series" || slug === "foreign_series" || slug === "arabic_series") {
      targetProviders = [arabseedProvider, wecimaProvider, faselHDProvider, akwamProvider, lodynetProvider];
    } else if (slug === "anime") {
      targetProviders = [arabseedProvider, lodynetProvider, akwamProvider];
    } else {
      targetProviders = [arabseedProvider, akwamProvider, wecimaProvider, egybestProvider];
    }

    const promises = targetProviders.map(async (provider) => {
      try {
        return await Promise.race([
          provider.getCategory(slug, page),
          new Promise<UnifiedMediaItem[]>((resolve) => setTimeout(() => resolve([]), timeoutMs)),
        ]);
      } catch {
        return [];
      }
    });

    const results = await Promise.allSettled(promises);
    const rawItems: UnifiedMediaItem[] = [];
    for (const res of results) {
      if (res.status === "fulfilled" && Array.isArray(res.value)) rawItems.push(...res.value);
    }

    const seen = new Set<string>();
    const deduplicated: UnifiedMediaItem[] = [];
    for (const it of rawItems) {
      const norm = normalizeTitle(it.title);
      if (!norm || seen.has(norm) || seen.has(it.pageUrl)) continue;
      seen.add(norm);
      seen.add(it.pageUrl);
      deduplicated.push(it);
    }

    const cachedAt = Date.now();
    if (deduplicated.length > 0) categoryCache.set(cacheKey, { items: deduplicated, time: cachedAt });
    // Return the catalog immediately. Fetching og:image is a slow, optional
    // repair path and must not delay the first screen or the API response.
    void enrichFromSource(deduplicated).then((enriched) => {
      const current = categoryCache.get(cacheKey);
      if (enriched.length > 0 && current?.time === cachedAt) {
        categoryCache.set(cacheKey, { items: enriched, time: cachedAt });
      }
    }).catch(() => {});
    return deduplicated;
  }

  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    const cached = detailCache.get(pageUrl);
    if (cached && Date.now() - cached.time < DETAIL_TTL) return cached.detail;

    const provider = this.getProviderForUrl(pageUrl);
    try {
      const detail = await provider.getDetail(pageUrl);
      if (detail && ((detail.servers && detail.servers.length > 0) || (detail.episodes && detail.episodes.length > 0))) {
        detailCache.set(pageUrl, { detail, time: Date.now() });
      }
      return detail;
    } catch (err) {
      console.error(`[Aggregator] getDetail error for ${pageUrl}:`, err);
      return null;
    }
  }

  async resolveStream(serverUrl: string, pageUrl?: string): Promise<string | null> {
    if (!serverUrl) return null;
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4") || serverUrl.includes("/api/proxy/stream")) {
      return serverUrl;
    }
    const provider = this.getProviderForUrl(serverUrl);
    try {
      const direct = await provider.resolveStream(serverUrl, pageUrl);
      return direct || serverUrl;
    } catch {
      return serverUrl;
    }
  }
}

export const aggregator = new MultiProviderAggregator();
