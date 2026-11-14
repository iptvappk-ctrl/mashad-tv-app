/**
 * Akoam Provider — https://akoam.one
 * Popular Arabic movies & series site
 */
import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";
import { extractStreamFromEmbed } from "./stream-extractor";

const BASE_URL = "https://akoam.one";
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
const cache = new Map<string, { data: any; time: number }>();
const CACHE_TTL = 15 * 60 * 1000;

async function fetchHtml(url: string, referer = `${BASE_URL}/`, timeoutMs = 5000): Promise<string> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": UA,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "ar,en;q=0.9",
        Referer: referer,
      },
      signal: controller.signal,
    });
    return res.ok ? await res.text() : "";
  } catch {
    return "";
  } finally {
    clearTimeout(timeoutId);
  }
}

function parseListing(html: string, providerId: string, providerName: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];
  const seen = new Set<string>();

  $(".movie-card, .small-movie, .item-movie, article, .films-card").each((_, el) => {
    const $el = $(el);
    const a = $el.find("a").first();
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http") || seen.has(href)) return;
    seen.add(href);
    if (href.includes("/category/") || href.includes("/tag/") || href.includes("/page/")) return;

    const title =
      $el.find("h3, h2, .title, .film-name, .movie-name").first().text().trim() ||
      a.attr("title") || a.find("img").attr("alt") || "";
    if (!title || title.length < 2) return;

    const imgEl = $el.find("img").first();
    let poster = imgEl.attr("data-src") || imgEl.attr("src") || imgEl.attr("data-lazy") || "";
    if (poster && !poster.startsWith("http")) poster = poster.startsWith("/") ? `${BASE_URL}${poster}` : `${BASE_URL}/${poster}`;

    const quality = $el.find(".quality, .film-quality, .label").first().text().trim() || "HD";
    const year = $el.find(".year, .film-year").first().text().trim().match(/\d{4}/)?.[0] || "";
    const isSeries = href.includes("/movies-series/") || href.includes("/series/") || title.includes("مسلسل");

    items.push({
      id: `akoam_new_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
      title,
      poster,
      pageUrl: href,
      category: isSeries ? "مسلسلات أكوام" : "أفلام أكوام",
      quality,
      year,
      isSeries,
      providerId,
      providerName,
    });
  });

  return items;
}

class AkoamNewProvider implements BaseProvider {
  id = "akoam_new";
  name = "أكوام";
  baseUrl = BASE_URL;
  isEnabled = true;

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `akoam_new:search:${query}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;
    try {
      const html = await fetchHtml(`${BASE_URL}/?s=${encodeURIComponent(query)}`);
      const items = parseListing(html, this.id, this.name);
      if (items.length > 0) cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch {
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `akoam_new:cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    let path = "/movies/";
    if (slug === "series" || slug === "arabic_series" || slug === "foreign_series") path = "/series/";
    else if (slug === "movies" || slug === "foreign_movies" || slug === "arabic_movies") path = "/movies/";
    else if (slug === "anime") path = "/anime/";
    else if (slug === "turkish_series") path = "/turkish-series/";

    const pageParam = page > 1 ? `page/${page}/` : "";
    try {
      const html = await fetchHtml(`${BASE_URL}${path}${pageParam}`);
      const items = parseListing(html, this.id, this.name);
      if (items.length > 0) cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch {
      return [];
    }
  }

  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    try {
      const html = await fetchHtml(pageUrl);
      if (!html) return null;
      const $ = cheerio.load(html);

      const title =
        $("h1.Title, h1, .title").first().text().trim() ||
        $('meta[property="og:title"]').attr("content") || "عرض";
      const poster =
        $('meta[property="og:image"]').attr("content") ||
        $(".film-poster img, .movie-poster img, .poster img").first().attr("src") || "";
      const overview =
        $(".description, .film-description, .story, p").first().text().trim() ||
        $('meta[property="og:description"]').attr("content") || "";
      const isSeries = pageUrl.includes("/series/") || title.includes("مسلسل");
      const metadata = extractMediaMetadata($, pageUrl);

      // Try to find watch links
      let watchPageUrl = pageUrl;
      const watchLink = $('a[href*="/watch/"], a[href*="/مشاهدة/"], .watch-btn').first().attr("href");
      if (watchLink && watchLink.startsWith("http")) watchPageUrl = watchLink;

      const servers: UnifiedServer[] = [];
      // Parse watch page for servers if different from detail page
      if (watchPageUrl !== pageUrl) {
        try {
          const watchHtml = await fetchHtml(watchPageUrl, pageUrl, 4000);
          const $w = cheerio.load(watchHtml);
          $w("iframe[src]").each((i, el) => {
            const src = $w(el).attr("src") || "";
            if (src.startsWith("http") || src.startsWith("//")) {
              const embedUrl = src.startsWith("//") ? `https:${src}` : src;
              if (!servers.some(s => s.embedUrl === embedUrl)) {
                servers.push({ name: `سيرفر ${i + 1}`, embedUrl, providerId: this.id });
              }
            }
          });
          $w("a[data-url], .server-btn, .server-link, .ep_link").each((i, el) => {
            const link = $w(el).attr("data-url") || $w(el).attr("href") || "";
            if (link && link.startsWith("http") && !servers.some(s => s.embedUrl === link)) {
              servers.push({ name: `سيرفر ${servers.length + 1} (بديل)`, embedUrl: link, providerId: this.id });
            }
          });
        } catch {}
      }

      // Also check detail page for iframes
      $("iframe[src]").each((i, el) => {
        const src = $(el).attr("src") || "";
        if (src.startsWith("http") || src.startsWith("//")) {
          const embedUrl = src.startsWith("//") ? `https:${src}` : src;
          if (!servers.some(s => s.embedUrl === embedUrl)) {
            servers.push({ name: `سيرفر ${servers.length + 1}`, embedUrl, providerId: this.id });
          }
        }
      });

      if (servers.length === 0) {
        servers.push({ name: "سيرفر 1 (مباشر)", embedUrl: watchPageUrl, providerId: this.id });
      }

      return {
        item: {
          id: `akoam_new_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
          title, poster, pageUrl,
          category: isSeries ? "مسلسلات" : "أفلام",
          isSeries, providerId: this.id, providerName: this.name,
        },
        overview,
        cast: metadata.cast,
        servers,
        seasons: metadata.seasons,
        episodes: metadata.episodes,
        proxiedStream: servers[0]?.embedUrl,
      };
    } catch {
      return null;
    }
  }

  async resolveStream(serverUrl: string): Promise<string | null> {
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) return serverUrl;
    try {
      const extracted = await extractStreamFromEmbed(serverUrl);
      return extracted?.url || serverUrl;
    } catch {
      return serverUrl;
    }
  }
}

export const akoamProvider = new AkoamNewProvider();
