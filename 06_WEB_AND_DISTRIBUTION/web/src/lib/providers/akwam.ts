import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";

const BASE_URL = "https://akw.cam";
const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

const cache = new Map<string, { data: any; time: number }>();
const CACHE_TTL = 15 * 60 * 1000; // 15 mins

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

export class AkwamProvider implements BaseProvider {
  id = "akwam";
  name = "أكوام";
  baseUrl = BASE_URL;
  isEnabled = true;

  parseListing(html: string): UnifiedMediaItem[] {
    const $ = cheerio.load(html);
    const items: UnifiedMediaItem[] = [];

    $('a[href*="/akfilm-"], a[href*="/akseries-"], a[href*="/akepisode-"], a.br-card-link').each((_, el) => {
      const a = $(el);
      const href = a.attr("href") || "";
      if (!href || !href.startsWith("http")) return;
      // Skip non-content links
      if (href.includes("/category/") || href.includes("/tag/") || href.includes("/page/")) return;
      if (!href.includes("/akfilm-") && !href.includes("/akseries-") && !href.includes("/akepisode-")) return;

      const title =
        a.find("h3.br-card-title, .entry-title, h3, h2").text().trim() ||
        a.attr("title") ||
        a.find("img").attr("alt") ||
        a.text().trim().replace(/\s+/g, " ");

      if (!title || title.length < 3) return;

      // Try img first, then CSS background-image (Akwam uses background-image in .br-thumb)
      let poster =
        a.find("img").attr("data-src") ||
        a.find("img").attr("src") ||
        "";

      // Extract from background-image style if no img found
      if (!poster || poster.includes("logo")) {
        const thumbEl = a.find(".br-thumb, .ak-poster, [style*='background-image']").first();
        const style = thumbEl.attr("style") || "";
        const bgMatch = style.match(/url\(['"]?(https?:\/\/[^'")\s]+)['"]?\)/);
        if (bgMatch) poster = bgMatch[1];
      }

      // Try og:image from parent article if still no poster
      if (!poster || poster.includes("logo")) poster = "";

      const isSeries = href.includes("series") || href.includes("episode") || title.includes("مسلسل");
      const cleanTitle = title.replace(/^NEW\s*/i, "").replace(/فيلم\s+\d+$/i, "").trim();

      items.push({
        id: `akwam_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
        title: cleanTitle,
        poster,
        pageUrl: href,
        category: isSeries ? "مسلسلات أكوام" : "أفلام أكوام",
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
      const url = `${BASE_URL}/?s=${encodeURIComponent(query.trim())}`;
      const html = await fetchHtml(url);
      const items = this.parseListing(html);
      cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch (err) {
      console.error("[AkwamProvider] search error:", err);
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    try {
      let path = "/films";
      if (slug === "series" || slug.includes("series")) {
        path = "/seriestv";
      } else if (slug === "foreign_movies") {
        path = "/category/افلام-اجنبي";
      } else if (slug === "arabic_movies" || slug === "dubbed") {
        path = "/category/افلام-مدبلجة";
      } else if (slug === "anime") {
        path = "/category/انمي";
      }

      const url = page <= 1 ? `${BASE_URL}${encodeURI(path)}` : `${BASE_URL}${encodeURI(path)}/page/${page}`;
      const html = await fetchHtml(url);
      const items = this.parseListing(html);
      cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch (err) {
      console.error(`[AkwamProvider] getCategory error (${slug}):`, err);
      return [];
    }
  }

  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    try {
      const html = await fetchHtml(pageUrl);
      const $ = cheerio.load(html);

      const title =
        $("h1, .entry-title").first().text().trim() ||
        $('meta[property="og:title"]').attr("content") ||
        "عرض أكوام";
      const overview =
        $(".story, .overview, .entry-content p, p").first().text().trim() ||
        $('meta[property="og:description"]').attr("content") ||
        "مشاهدة وتحميل مباشر من سيرفرات أكوام عالية السرعة وبجودة فائقة.";
      const poster =
        $('meta[property="og:image"]').attr("content") ||
        $("img.attachment-post-thumbnail").attr("src") ||
        $("img").first().attr("src") ||
        "";

      const isSeries = pageUrl.includes("series") || title.includes("مسلسل");

      // Extract watch servers page: /watch/.../servers
      let watchUrl = $('a[href*="/watch/"]').first().attr("href") || `${pageUrl.replace(/\/$/, "")}/servers`;
      const metadata = extractMediaMetadata($, pageUrl);
      const servers: UnifiedServer[] = [];

      try {
        const watchHtml = await fetchHtml(watchUrl, pageUrl, 3500);
        const $w = cheerio.load(watchHtml);

        // 1. Check primary iframe
        const primaryIframe = $w("iframe").first().attr("src");
        if (primaryIframe && primaryIframe.startsWith("http")) {
          servers.push({
            name: "سيرفر 1 (فائق السرعة)",
            embedUrl: primaryIframe,
            providerId: this.id,
            isHls: true,
          });
        }

        // 2. Extract multiple servers from data attributes
        $w("a[data-url], [data-src], a.server, .watchServers a").each((i, el) => {
          const sUrl = $w(el).attr("data-url") || $w(el).attr("data-src") || $w(el).attr("href");
          const sName = $w(el).text().trim().replace(/أكوام|akwam/gi, "").replace(/\s+/g, " ").trim() || `سيرفر بديل ${i + 1}`;
          if (sUrl && sUrl.startsWith("http") && !servers.some((s) => s.embedUrl === sUrl)) {
            servers.push({
              name: `سيرفر ${servers.length + 1} (${sName})`,
              embedUrl: sUrl,
              providerId: this.id,
              isHls: sUrl.includes(".m3u8"),
            });
          }
        });
      } catch {}

      if (servers.length === 0) {
        servers.push({
          name: "سيرفر 1 (مباشر)",
          embedUrl: watchUrl,
          providerId: this.id,
          isHls: false,
        });
      }

      return {
        item: {
          id: `akwam_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
          title,
          poster,
          pageUrl,
          category: isSeries ? "مسلسلات أكوام" : "أفلام أكوام",
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
      console.error("[AkwamProvider] getDetail error:", err);
      return null;
    }
  }

  async resolveStream(serverUrl: string): Promise<string | null> {
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) {
      return serverUrl;
    }
    return serverUrl;
  }
}

export const akwamProvider = new AkwamProvider();
