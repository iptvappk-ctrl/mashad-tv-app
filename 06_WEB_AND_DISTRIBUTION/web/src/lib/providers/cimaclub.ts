/**
 * CimaClub Provider — https://cimaclub.com
 * Arabic movies and series
 */
import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";
import { extractStreamFromEmbed } from "./stream-extractor";

const BASE_URL = "https://cimaclub.com";
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

  $(".movie-card, .small-movie, article.movie, .block, .MovieBlock").each((_, el) => {
    const $el = $(el);
    const a = $el.find("a").first();
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http") || seen.has(href)) return;
    seen.add(href);
    if (href.includes("/category/") || href.includes("/tag/") || href.includes("/page/")) return;

    const title =
      $el.find("h3, h2, .title, .BlockTitle").first().text().trim() ||
      a.attr("title") || a.find("img").attr("alt") || "";
    if (!title || title.length < 2) return;

    const imgEl = $el.find("img").first();
    let poster = imgEl.attr("data-src") || imgEl.attr("src") || "";
    if (poster && !poster.startsWith("http")) poster = poster.startsWith("/") ? `${BASE_URL}${poster}` : `${BASE_URL}/${poster}`;

    const quality = $el.find(".Quality, .label, .quality").first().text().trim() || "HD";
    const year = $el.find(".year, .Year, .date").first().text().trim().match(/\d{4}/)?.[0] || "";
    const isSeries = href.includes("/series/") || href.includes("/show/") || title.includes("مسلسل");

    items.push({
      id: `cimaclub_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
      title,
      poster,
      pageUrl: href,
      category: isSeries ? "مسلسلات سيما كلوب" : "أفلام سيما كلوب",
      quality,
      year,
      isSeries,
      providerId,
      providerName,
    });
  });

  return items;
}

class CimaclubProvider implements BaseProvider {
  id = "cimaclub";
  name = "سيما كلوب";
  baseUrl = BASE_URL;
  isEnabled = true;

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `cimaclub:search:${query}`;
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
    const cacheKey = `cimaclub:cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    let path = "/movies/";
    if (slug === "series" || slug === "arabic_series") path = "/series/";
    else if (slug === "foreign_movies" || slug === "foreign") path = "/movies-foreign/";
    else if (slug === "arabic_movies") path = "/movies-arabic/";
    else if (slug === "turkish_series") path = "/series-turkish/";
    else if (slug === "anime") path = "/anime/";
    else if (slug === "movies") path = "/movies/";

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
        $("h1, .Title, .BlockTitle").first().text().trim() ||
        $('meta[property="og:title"]').attr("content") || "عرض";
      const poster =
        $('meta[property="og:image"]').attr("content") ||
        $(".Poster img, .poster img, .MovieImag img").first().attr("src") || "";
      const overview =
        $(".Story, .description, p.overview").first().text().trim() ||
        $('meta[property="og:description"]').attr("content") || "";
      const isSeries = pageUrl.includes("/series/") || pageUrl.includes("/show/") || title.includes("مسلسل");
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
      $("a.server, .watch-server, a[data-link], a[data-server]").each((i, el) => {
        const link = $(el).attr("data-link") || $(el).attr("data-server") || $(el).attr("href") || "";
        if (link && link.startsWith("http") && !servers.some(s => s.embedUrl === link)) {
          servers.push({ name: `سيرفر ${servers.length + 1} (${$(el).text().trim() || "بديل"})`, embedUrl: link, providerId: this.id });
        }
      });

      return {
        item: {
          id: `cimaclub_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
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

export const cimaclubProvider = new CimaclubProvider();
