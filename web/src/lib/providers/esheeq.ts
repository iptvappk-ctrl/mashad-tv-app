import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";

const BASE_URL = "https://e.q-ask.video";
const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

const cache = new Map<string, { data: any; time: number }>();
const CACHE_TTL = 15 * 60 * 1000;

async function fetchHtml(url: string, referer = `${BASE_URL}/`, timeoutMs = 4000): Promise<string> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": UA,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        Referer: referer,
      },
      signal: controller.signal,
    });
    return await res.text();
  } finally {
    clearTimeout(timeoutId);
  }
}

export class EsheeqProvider implements BaseProvider {
  id = "esheeq";
  name = "قصة عشق";
  baseUrl = BASE_URL;
  isEnabled = true;

  parseListing(html: string): UnifiedMediaItem[] {
    const $ = cheerio.load(html);
    const items: UnifiedMediaItem[] = [];

    $('a[href*="/video-"]').each((_, el) => {
      const a = $(el);
      const href = a.attr("href") || "";
      if (!href) return;

      const title =
        a.find("img").attr("alt") ||
        a.attr("title") ||
        a.text().trim().replace(/\s+/g, " ");

      if (!title || title.length < 5 || title.includes("Dramatic TV")) return;

      const poster =
        a.find("img").attr("data-src") ||
        a.find("img").attr("src") ||
        "";

      items.push({
        id: `3sk_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
        title,
        poster,
        pageUrl: href.startsWith("http") ? href : `${BASE_URL}/${href.replace(/^\//, "")}`,
        category: "دراما تركية مترجمة",
        isSeries: true,
        providerId: this.id,
        providerName: this.name,
      });
    });

    const seen = new Set<string>();
    return items.filter((it) => {
      if (seen.has(it.pageUrl)) return false;
      seen.add(it.pageUrl);
      return true;
    });
  }

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `search:${query}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    try {
      const url = `${BASE_URL}/?s=${encodeURIComponent(query.trim())}`;
      const html = await fetchHtml(url);
      const items = this.parseListing(html);
      cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch (err) {
      console.error("[EsheeqProvider] search error:", err);
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    try {
      const url = page <= 1 ? `${BASE_URL}/` : `${BASE_URL}/page/${page}/`;
      const html = await fetchHtml(url);
      const items = this.parseListing(html);
      cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch (err) {
      console.error(`[EsheeqProvider] getCategory error (${slug}):`, err);
      return [];
    }
  }

  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    try {
      const html = await fetchHtml(pageUrl);
      const $ = cheerio.load(html);

      const title =
        $('meta[property="og:title"]').attr("content") ||
        $("h1").first().text().trim() ||
        "مسلسل تركي - قصة عشق";
      const overview =
        $('meta[property="og:description"]').attr("content") ||
        "مشاهدة وتحميل الحلقة مترجمة وبأعلى جودة من موقع قصة عشق الأصلي.";
      const poster =
        $('meta[property="og:image"]').attr("content") ||
        $('img[src*="/uploads/"]').first().attr("src") ||
        "";

      const metadata = extractMediaMetadata($, pageUrl);
      const servers: UnifiedServer[] = [];

      // Extract iframes
      $("iframe").each((_, el) => {
        const src = $(el).attr("src");
        if (src && src.startsWith("http")) {
          servers.push({
            name: "[قصة عشق] سيرفر مباشر",
            embedUrl: src,
            providerId: this.id,
            isHls: true,
          });
        }
      });

      if (servers.length === 0) {
        servers.push({
          name: "[قصة عشق] سيرفر المشاهدة التركية",
          embedUrl: pageUrl,
          providerId: this.id,
          isHls: false,
        });
      }

      return {
        item: {
          id: `3sk_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
          title,
          poster,
          pageUrl,
          category: "دراما تركية مترجمة",
          isSeries: true,
          providerId: this.id,
          providerName: this.name,
        },
        overview,
        cast: metadata.cast,
        servers,
        seasons: metadata.seasons,
        episodes: metadata.episodes,
      };
    } catch (err) {
      console.error("[EsheeqProvider] getDetail error:", err);
      return null;
    }
  }

  async resolveStream(serverUrl: string): Promise<string | null> {
    return serverUrl;
  }
}

export const esheeqProvider = new EsheeqProvider();
