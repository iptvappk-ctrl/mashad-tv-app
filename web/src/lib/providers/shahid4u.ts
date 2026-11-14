/**
 * Shahid4U Provider — https://shahid4u.tv
 * Arabic/Turkish series focused site
 */
import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";
import { extractStreamFromEmbed } from "./stream-extractor";

const BASE_URL = "https://shahid4u.tv";
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

  $("article, .movie-card, .series-card, .BlockMovie, .show-item").each((_, el) => {
    const $el = $(el);
    const a = $el.find("a").first();
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http") || seen.has(href)) return;
    seen.add(href);
    if (href.includes("/category/") || href.includes("/tag/") || href.includes("/page/")) return;

    const title =
      $el.find("h3, h2, .title, .Title, .series-title").first().text().trim() ||
      a.attr("title") || a.find("img").attr("alt") || "";
    if (!title || title.length < 2) return;

    const imgEl = $el.find("img").first();
    let poster = imgEl.attr("data-src") || imgEl.attr("src") || imgEl.attr("data-lazy") || "";
    if (poster && !poster.startsWith("http")) poster = poster.startsWith("/") ? `${BASE_URL}${poster}` : `${BASE_URL}/${poster}`;

    const quality = $el.find(".quality, .Quality, .label").first().text().trim() || "HD";
    const year = $el.find(".year, .Year").first().text().trim().match(/\d{4}/)?.[0] || "";
    const isSeries = href.includes("/series/") || href.includes("/مسلسل") || title.includes("مسلسل");

    items.push({
      id: `shahid4u_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
      title,
      poster,
      pageUrl: href,
      category: isSeries ? "مسلسلات شاهد" : "أفلام شاهد",
      quality,
      year,
      isSeries,
      providerId,
      providerName,
    });
  });

  return items;
}

class Shahid4uProvider implements BaseProvider {
  id = "shahid4u";
  name = "شاهد فور يو";
  baseUrl = BASE_URL;
  isEnabled = true;

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `shahid4u:search:${query}`;
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
    const cacheKey = `shahid4u:cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    let path = "/series/";
    if (slug === "movies" || slug === "arabic_movies" || slug === "foreign_movies") path = "/movies/";
    else if (slug === "arabic_series") path = "/arabic-series/";
    else if (slug === "turkish_series" || slug === "turkish") path = "/turkish-series/";
    else if (slug === "foreign_series" || slug === "foreign") path = "/foreign-series/";
    else if (slug === "anime") path = "/anime/";
    else if (slug === "ramadan") path = "/ramadan-series/";

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
        $("h1, .Title, .series-title").first().text().trim() ||
        $('meta[property="og:title"]').attr("content") || "عرض";
      const poster =
        $('meta[property="og:image"]').attr("content") ||
        $(".poster img, .series-poster img, .thumbnail img").first().attr("src") || "";
      const overview =
        $(".description, .overview, .story, p.desc").first().text().trim() ||
        $('meta[property="og:description"]').attr("content") || "";
      const isSeries = pageUrl.includes("/series/") || title.includes("مسلسل");
      const metadata = extractMediaMetadata($, pageUrl);

      const servers: UnifiedServer[] = [];
      $("iframe[src]").each((i, el) => {
        const src = $(el).attr("src") || "";
        if (!src) return;
        const embedUrl = src.startsWith("//") ? `https:${src}` : src;
        if (!embedUrl.startsWith("http")) return;
        if (!servers.some(s => s.embedUrl === embedUrl)) {
          servers.push({ name: `سيرفر ${i + 1}`, embedUrl, providerId: this.id });
        }
      });

      // Multiple servers from server buttons
      $(".watch-servers a, .server-btn, [data-iframe], a[data-src]").each((i, el) => {
        const $btn = $(el);
        const src = $btn.attr("data-iframe") || $btn.attr("data-src") || $btn.attr("href") || "";
        if (!src || !src.startsWith("http")) return;
        if (!servers.some(s => s.embedUrl === src)) {
          const name = $btn.text().trim() || `سيرفر ${servers.length + 1}`;
          servers.push({ name: `${name}`, embedUrl: src, providerId: this.id });
        }
      });

      return {
        item: {
          id: `shahid4u_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
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

export const shahid4uProvider = new Shahid4uProvider();
