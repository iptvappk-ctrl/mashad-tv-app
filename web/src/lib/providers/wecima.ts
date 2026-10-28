import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";

const BASE_URL = "https://myweecima.beauty";
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

export class WeCimaProvider implements BaseProvider {
  id = "wecima";
  name = "وي سيما";
  baseUrl = BASE_URL;
  isEnabled = true;

  parseListing(html: string): UnifiedMediaItem[] {
    const $ = cheerio.load(html);
    const items: UnifiedMediaItem[] = [];

    $('a[href*="watch.php?vid="]').each((_, el) => {
      const a = $(el);
      const href = a.attr("href") || "";
      if (!href) return;

      const title =
        a.attr("title") ||
        a.find("img").attr("alt") ||
        a.text().trim().replace(/\s+/g, " ");

      if (!title || title.length < 3) return;

      // WeCima uses background-image in .imgSer div inside the card
      let poster =
        a.find("img").attr("src") ||
        a.find("img").attr("data-src") ||
        "";

      // Extract from CSS background-image (WeCima main listing uses this)
      if (!poster || poster.includes("logo") || poster.includes("custom-logo")) {
        const imgSerEl = a.find(".imgSer, .pm-video-thumb, [style*='background-image']").first();
        const style = imgSerEl.attr("style") || "";
        const bgMatch = style.match(/url\(['"]?(https?:\/\/[^'")\s]+)['"]?\)/);
        if (bgMatch) poster = bgMatch[1];
      }

      // Fix protocol-relative URLs
      if (poster.startsWith("//")) poster = "https:" + poster;

      const isSeries = href.includes("series") || title.includes("مسلسل") || title.includes("الحلقة");

      items.push({
        id: `wecima_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
        title,
        poster,
        pageUrl: href.startsWith("http") ? href : `${BASE_URL}/${href.replace(/^\//, "")}`,
        category: isSeries ? "مسلسلات وي سيما" : "أفلام وي سيما",
        isSeries,
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
      const url = `${BASE_URL}/search.php?keywords=${encodeURIComponent(query.trim())}`;
      const html = await fetchHtml(url);
      const items = this.parseListing(html);
      cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch (err) {
      console.error("[WeCimaProvider] search error:", err);
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    try {
      let path = "/movies.php";
      if (slug === "series" || slug.includes("series")) {
        path = "/all-series.php";
      } else if (slug === "foreign_movies") {
        path = "/category.php?cat=foreign-films";
      } else if (slug === "arabic_movies") {
        path = "/category.php?cat=arabic-movies";
      } else if (slug === "turkish_movies") {
        path = "/category.php?cat=turkish-films";
      } else if (slug === "arabic_series") {
        path = "/category.php?cat=arabic-tv-series";
      } else if (slug === "turkish_series") {
        path = "/category.php?cat=turkish-series";
      } else if (slug === "anime") {
        path = "/category.php?cat=anime-and-cartoons";
      }

      const separator = path.includes("?") ? "&" : "?";
      const url = page <= 1 ? `${BASE_URL}${path}` : `${BASE_URL}${path}${separator}page=${page}`;
      const html = await fetchHtml(url);
      const items = this.parseListing(html);
      cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch (err) {
      console.error(`[WeCimaProvider] getCategory error (${slug}):`, err);
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
        "عرض وي سيما";
      const overview =
        $('meta[property="og:description"]').attr("content") ||
        $(".description, p").first().text().trim() ||
        "مشاهدة وتحميل مباشر من سيرفرات وي سيما بجودة عالية وبدون إعلانات.";
      const poster =
        $('meta[property="og:image"]').attr("content") ||
        $('img[src*="/uploads/thumbs/"]').first().attr("src") ||
        "";

      const vidMatch = pageUrl.match(/vid=([a-zA-Z0-9]+)/);
      const vid = vidMatch ? vidMatch[1] : "";
      const isSeries = pageUrl.includes("series") || title.includes("مسلسل");

      const metadata = extractMediaMetadata($, pageUrl);
      const servers: UnifiedServer[] = [];

      if (vid) {
        // 1. Official WeCima Embed Player
        servers.push({
          name: "سيرفر 1 (فائق السرعة)",
          embedUrl: `${BASE_URL}/embed.php?vid=${vid}`,
          providerId: this.id,
          isHls: true,
        });

        // 2. Fetch downloads.php to get alternative servers
        try {
          const dlUrl = `${BASE_URL}/downloads.php?vid=${vid}`;
          const dlHtml = await fetchHtml(dlUrl, pageUrl, 3000);
          const $d = cheerio.load(dlHtml);
          $d("a").each((i, el) => {
            const h = $d(el).attr("href") || "";
            const sName = $d(el).text().trim().replace(/وي سيما|wecima/gi, "").trim();
            if (
              h.startsWith("http") &&
              (h.includes("uqload") ||
                h.includes("vikingfile") ||
                h.includes("turboviplay") ||
                h.includes("usersdrive") ||
                h.includes("gofile") ||
                h.includes("vidspeed"))
            ) {
              servers.push({
                name: `سيرفر ${servers.length + 1} (${sName || "بديل"})`,
                embedUrl: h,
                providerId: this.id,
                isHls: false,
              });
            }
          });
        } catch {}
      }

      if (servers.length === 0) {
        servers.push({
          name: "سيرفر 1 (مباشر)",
          embedUrl: pageUrl,
          providerId: this.id,
          isHls: false,
        });
      }

      return {
        item: {
          id: `wecima_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
          title,
          poster,
          pageUrl,
          category: isSeries ? "مسلسلات وي سيما" : "أفلام وي سيما",
          isSeries,
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
      console.error("[WeCimaProvider] getDetail error:", err);
      return null;
    }
  }

  async resolveStream(serverUrl: string): Promise<string | null> {
    return serverUrl;
  }
}

export const wecimaProvider = new WeCimaProvider();
