import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";

const BASE_URL = "https://brsteej.casa";
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

export class PrestigeProvider implements BaseProvider {
  id = "prestige";
  name = "برستيج";
  baseUrl = BASE_URL;
  isEnabled = true;

  parseListing(html: string): UnifiedMediaItem[] {
    const $ = cheerio.load(html);
    const items: UnifiedMediaItem[] = [];

    $('a[href*="video.php?vid="]').each((_, el) => {
      const a = $(el);
      const href = a.attr("href") || "";
      if (!href) return;

      const title =
        a.find("img").attr("alt") ||
        a.attr("title") ||
        a.text().trim().replace(/\s+/g, " ");

      if (!title || title.length < 5 || title.includes("برستيج")) return;

      const poster =
        a.find("img").attr("src") ||
        a.find("img").attr("data-src") ||
        "";

      items.push({
        id: `prestige_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
        title,
        poster,
        pageUrl: href.startsWith("http") ? href : `${BASE_URL}/${href.replace(/^\//, "")}`,
        category: "مسلسلات رمضان وبرستيج",
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
      const url = `${BASE_URL}/search.php?keywords=${encodeURIComponent(query.trim())}`;
      const html = await fetchHtml(url);
      const items = this.parseListing(html);
      cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch (err) {
      console.error("[PrestigeProvider] search error:", err);
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    try {
      const url = `${BASE_URL}/home03`;
      const html = await fetchHtml(url);
      const items = this.parseListing(html);
      cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch (err) {
      console.error(`[PrestigeProvider] getCategory error (${slug}):`, err);
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
        "مسلسل برستيج";
      const overview =
        $('meta[property="og:description"]').attr("content") ||
        "مشاهدة وتحميل حلقات المسلسل بجودة عالية من سيرفرات برستيج الحصرية.";
      const poster =
        $('meta[property="og:image"]').attr("content") ||
        $('img[src*="/uploads/thumbs/"]').first().attr("src") ||
        "";

      const vidMatch = pageUrl.match(/vid=([a-zA-Z0-9]+)/);
      const vid = vidMatch ? vidMatch[1] : "";

      const metadata = extractMediaMetadata($, pageUrl);
      const servers: UnifiedServer[] = [];

      // Check iframes or video tags
      $("iframe").each((_, el) => {
        const src = $(el).attr("src");
        if (src && src.startsWith("http")) {
          servers.push({
            name: "[برستيج] سيرفر مباشر",
            embedUrl: src,
            providerId: this.id,
            isHls: true,
          });
        }
      });

      if (vid && servers.length === 0) {
        servers.push({
          name: "[برستيج] سيرفر المشاهدة السريعة",
          embedUrl: `${BASE_URL}/watch.php?vid=${vid}`,
          providerId: this.id,
          isHls: true,
        });
      }

      return {
        item: {
          id: `prestige_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
          title,
          poster,
          pageUrl,
          category: "مسلسلات رمضان وبرستيج",
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
      console.error("[PrestigeProvider] getDetail error:", err);
      return null;
    }
  }

  async resolveStream(serverUrl: string): Promise<string | null> {
    return serverUrl;
  }
}

export const prestigeProvider = new PrestigeProvider();
