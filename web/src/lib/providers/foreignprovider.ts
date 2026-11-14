/**
 * Foreign Content Provider (Hollywood/International)
 * Sources: Cinemana (Shabakaty), fallback to foreign categories
 * Specializes in: foreign movies, foreign series, dubbed content
 */
import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";
import { extractStreamFromEmbed } from "./stream-extractor";

const CINEMANA_BASE = "https://cinemana.shabakaty.com";
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

const cache = new Map<string, { data: any; time: number }>();
const CACHE_TTL = 15 * 60 * 1000;

async function fetchJson<T = any>(url: string, referer?: string, timeoutMs = 5000): Promise<T | null> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": UA,
        Accept: "application/json, text/plain, */*",
        Referer: referer || CINEMANA_BASE,
        "X-Requested-With": "XMLHttpRequest",
      },
      signal: controller.signal,
    });
    return res.ok ? await res.json() : null;
  } catch {
    return null;
  } finally {
    clearTimeout(timeoutId);
  }
}

async function fetchHtml(url: string, referer?: string, timeoutMs = 5000): Promise<string> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": UA,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "ar,en;q=0.9",
        Referer: referer || CINEMANA_BASE,
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

function mapCinemanaItem(item: any): UnifiedMediaItem {
  const isMovie = item.type === "movie" || item.category?.includes("movie");
  const href = `${CINEMANA_BASE}/${isMovie ? "movie" : "series"}/${item.id}/${item.slug || item.title?.replace(/\s+/g, "-") || item.id}`;
  return {
    id: `foreign_${item.id || Buffer.from(href).toString("base64url").slice(0, 12)}`,
    title: item.title || item.name || item.ar_name || "بدون عنوان",
    poster: item.poster || item.poster_path
      ? (item.poster || item.poster_path || "").startsWith("http")
        ? (item.poster || item.poster_path)
        : `${CINEMANA_BASE}${item.poster || item.poster_path}`
      : "",
    pageUrl: href,
    category: isMovie ? "أفلام أجنبية" : "مسلسلات أجنبية",
    quality: item.quality || "HD",
    year: item.year?.toString() || item.release_date?.substring(0, 4) || "",
    rating: item.rating?.toString() || item.rate?.toString() || "",
    isSeries: !isMovie,
    providerId: "foreign",
    providerName: "أفلام ومسلسلات أجنبية",
  };
}

class ForeignProvider implements BaseProvider {
  id = "foreign";
  name = "أفلام ومسلسلات أجنبية";
  baseUrl = CINEMANA_BASE;
  isEnabled = true;

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `foreign:search:${query}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;
    try {
      // Cinemana search API
      const data = await fetchJson<any[]>(`${CINEMANA_BASE}/ar/api/search?q=${encodeURIComponent(query)}&lang=ar`);
      if (Array.isArray(data) && data.length > 0) {
        const items = data.map(mapCinemanaItem);
        cache.set(cacheKey, { data: items, time: Date.now() });
        return items;
      }
      // HTML fallback
      const html = await fetchHtml(`${CINEMANA_BASE}/ar/search?q=${encodeURIComponent(query)}`);
      const $ = cheerio.load(html);
      const items: UnifiedMediaItem[] = [];
      const seen = new Set<string>();
      $("article, .movie-card, .content-item, .show-item").each((_, el) => {
        const $el = $(el);
        const a = $el.find("a").first();
        const href = a.attr("href") || "";
        if (!href || !href.startsWith("http") || seen.has(href)) return;
        seen.add(href);
        const title = $el.find("h3, h2, .title").first().text().trim() || a.attr("title") || "";
        if (!title || title.length < 2) return;
        const imgEl = $el.find("img").first();
        let poster = imgEl.attr("data-src") || imgEl.attr("src") || "";
        if (poster && !poster.startsWith("http")) poster = poster.startsWith("/") ? `${CINEMANA_BASE}${poster}` : `${CINEMANA_BASE}/${poster}`;
        items.push({
          id: `foreign_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
          title, poster, pageUrl: href,
          category: "أجنبي",
          isSeries: href.includes("/series/"),
          providerId: this.id, providerName: this.name,
        });
      });
      if (items.length > 0) cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch {
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `foreign:cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    try {
      // Cinemana JSON API for lists
      let apiUrl = `${CINEMANA_BASE}/ar/api/videos?type=movie&page=${page}&lang=ar`;
      if (slug === "foreign_series" || slug === "series") apiUrl = `${CINEMANA_BASE}/ar/api/videos?type=series&page=${page}&lang=ar`;
      else if (slug === "dubbed") apiUrl = `${CINEMANA_BASE}/ar/api/videos?type=movie&dubbed=1&page=${page}&lang=ar`;
      else if (slug === "new_foreign") apiUrl = `${CINEMANA_BASE}/ar/api/videos?type=movie&sort=new&page=${page}&lang=ar`;

      const data = await fetchJson<any[]>(apiUrl);
      if (Array.isArray(data) && data.length > 0) {
        const items = data.map(mapCinemanaItem);
        cache.set(cacheKey, { data: items, time: Date.now() });
        return items;
      }

      // HTML fallback
      let htmlUrl = `${CINEMANA_BASE}/ar/movies?page=${page}`;
      if (slug === "foreign_series" || slug === "series") htmlUrl = `${CINEMANA_BASE}/ar/series?page=${page}`;
      const html = await fetchHtml(htmlUrl);
      const $ = cheerio.load(html);
      const items: UnifiedMediaItem[] = [];
      const seen = new Set<string>();
      $("article, .movie-card, .content-item, .show-item").each((_, el) => {
        const $el = $(el);
        const a = $el.find("a").first();
        const href = a.attr("href") || "";
        if (!href || !href.startsWith("http") || seen.has(href)) return;
        seen.add(href);
        const title = $el.find("h3, h2, .title").first().text().trim() || a.attr("title") || "";
        if (!title || title.length < 2) return;
        const imgEl = $el.find("img").first();
        let poster = imgEl.attr("data-src") || imgEl.attr("src") || "";
        if (poster && !poster.startsWith("http")) poster = poster.startsWith("/") ? `${CINEMANA_BASE}${poster}` : `${CINEMANA_BASE}/${poster}`;
        const year = $el.find(".year, .date").first().text().trim().match(/\d{4}/)?.[0] || "";
        const isSeries = href.includes("/series/");
        items.push({
          id: `foreign_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
          title, poster, pageUrl: href,
          category: isSeries ? "مسلسلات أجنبية" : "أفلام أجنبية",
          year,
          isSeries,
          providerId: this.id, providerName: this.name,
        });
      });
      if (items.length > 0) cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch {
      return [];
    }
  }

  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    try {
      // Check if it's a Cinemana URL with an ID
      const idMatch = pageUrl.match(/\/(?:movie|series)\/(\d+)\//);
      if (idMatch) {
        const id = idMatch[1];
        const isMovie = pageUrl.includes("/movie/");
        const apiUrl = `${CINEMANA_BASE}/ar/api/${isMovie ? "movie" : "series"}/${id}`;
        const data = await fetchJson<any>(apiUrl);
        if (data) {
          const item = mapCinemanaItem({ ...data, type: isMovie ? "movie" : "series" });
          const servers: UnifiedServer[] = [];
          if (data.stream_url || data.hls_url) {
            servers.push({
              name: "سيرفر 1 (مباشر)",
              embedUrl: data.stream_url || data.hls_url,
              directStream: data.stream_url || data.hls_url,
              isHls: !!(data.hls_url || (data.stream_url || "").includes(".m3u8")),
              providerId: this.id,
            });
          }
          if (data.embed_url) {
            servers.push({ name: "سيرفر 2 (Embed)", embedUrl: data.embed_url, providerId: this.id });
          }
          return {
            item, overview: data.description || data.overview || "",
            cast: Array.isArray(data.cast) ? data.cast.map((c: any) => c.name || c) : [],
            servers,
            seasons: Array.isArray(data.seasons) ? data.seasons.map((s: any, i: number) => ({
              title: s.title || `الموسم ${i + 1}`,
              pageUrl: `${CINEMANA_BASE}/ar/series/${id}/season/${s.id || i + 1}`,
              seasonNumber: `${i + 1}`,
            })) : [],
            episodes: Array.isArray(data.episodes) ? data.episodes.map((e: any, i: number) => ({
              title: e.title || `الحلقة ${i + 1}`,
              pageUrl: `${CINEMANA_BASE}/ar/episode/${e.id || i}`,
              episodeNumber: `${e.episode_number || i + 1}`,
            })) : [],
            proxiedStream: servers[0]?.embedUrl,
          };
        }
      }

      // HTML fallback
      const html = await fetchHtml(pageUrl);
      if (!html) return null;
      const $ = cheerio.load(html);

      const title = $("h1, .Title, .movie-title").first().text().trim() || $('meta[property="og:title"]').attr("content") || "عرض";
      const poster = $('meta[property="og:image"]').attr("content") || $(".poster img, .movie-poster img").first().attr("src") || "";
      const overview = $(".description, .overview, .story").first().text().trim() || $('meta[property="og:description"]').attr("content") || "";
      const isSeries = pageUrl.includes("/series/");
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

      return {
        item: {
          id: `foreign_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
          title, poster, pageUrl,
          category: isSeries ? "مسلسلات أجنبية" : "أفلام أجنبية",
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

export const foreignProvider = new ForeignProvider();
