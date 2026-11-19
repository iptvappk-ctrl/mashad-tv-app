/**
 * EgyBest Provider — https://egybest.mx
 * أحد أشهر مواقع مشاهدة الأفلام والمسلسلات العربية
 */

import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";

const BASE = "https://egybest.mx";
const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

const listCache = new Map<string, { items: UnifiedMediaItem[]; time: number }>();
const detailCache = new Map<string, { d: UnifiedMediaDetail; time: number }>();
const LIST_TTL = 15 * 60 * 1000;
const DETAIL_TTL = 30 * 60 * 1000;

async function fetchHtml(url: string, timeoutMs = 6000): Promise<string> {
  const ctrl = new AbortController();
  const id = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const r = await fetch(url, {
      signal: ctrl.signal,
      headers: {
        "User-Agent": UA,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "ar,en;q=0.9",
        "Referer": BASE + "/",
        "Cookie": "lang=ar",
      },
      next: { revalidate: 0 },
    });
    return r.ok ? r.text() : "";
  } catch {
    return "";
  } finally {
    clearTimeout(id);
  }
}

function parseItems(html: string, providerId: string, providerName: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];

  // EgyBest cards use: div.movies-list-full a.movie, div.movies-list a.movie
  $("a.movie, div.movie-card a, .content-box a.movie, .movies-list-full a").each((_, el) => {
    const $el = $(el);
    const href = $el.attr("href") || "";
    if (!href || !href.includes(BASE.replace("https://", ""))) return;
    if (!href.startsWith("http")) return;

    const title =
      $el.find(".title, h2, h3, .movie-title").first().text().trim() ||
      $el.attr("title") ||
      "";
    if (!title || title.length < 2) return;

    // Poster: try data-src first (lazy loaded), then src
    const imgEl = $el.find("img").first();
    const rawPoster =
      imgEl.attr("data-src") ||
      imgEl.attr("src") ||
      imgEl.attr("data-lazy") ||
      "";

    // Fix relative paths (e.g., "uploads/m/..." needs base URL with slash)
    let poster = "";
    if (!rawPoster) poster = "";
    else if (rawPoster.startsWith("http")) poster = rawPoster;
    else if (rawPoster.startsWith("/")) poster = `${BASE}${rawPoster}`;
    else poster = `${BASE}/${rawPoster}`;

    const quality =
      $el.find(".ribbon, .quality, .label").first().text().trim() || "HD";
    const year =
      $el.find(".year, .date").first().text().trim().match(/\d{4}/)?.[0] || "";
    const isSeries =
      href.includes("/series/") ||
      href.includes("/show/") ||
      $el.find(".type").text().includes("مسلسل");

    items.push({
      id: `egybest_${Buffer.from(href).toString("base64").slice(0, 20)}`,
      title,
      poster,
      pageUrl: href,
      category: isSeries ? "مسلسل" : "فيلم",
      quality,
      year,
      isSeries,
      providerId,
      providerName,
    });
  });

  return items;
}

const egybestProvider: BaseProvider = {
  id: "egybest",
  name: "إيجي بست",
  baseUrl: BASE,
  isEnabled: true,

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `egybest:cat:${slug}:${page}`;
    const cached = listCache.get(cacheKey);
    if (cached && Date.now() - cached.time < LIST_TTL) return cached.items;

    let url = `${BASE}/movies/?page=${page}`;
    if (slug === "series" || slug === "foreign_series" || slug === "arabic_series") {
      url = `${BASE}/series/?page=${page}`;
    } else if (slug === "anime") {
      url = `${BASE}/anime/?page=${page}`;
    } else if (slug === "arabic_movies") {
      url = `${BASE}/movies/arabic/?page=${page}`;
    } else if (slug === "turkish_movies" || slug === "turkish_series") {
      url = `${BASE}/movies/turkish/?page=${page}`;
    }

    const html = await fetchHtml(url);
    const items = parseItems(html, this.id, this.name);
    if (items.length > 0) {
      listCache.set(cacheKey, { items, time: Date.now() });
    }
    return items;
  },

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `egybest:search:${query.toLowerCase()}`;
    const cached = listCache.get(cacheKey);
    if (cached && Date.now() - cached.time < LIST_TTL) return cached.items;

    const url = `${BASE}/explore/?q=${encodeURIComponent(query)}`;
    const html = await fetchHtml(url);
    const items = parseItems(html, this.id, this.name);
    if (items.length > 0) {
      listCache.set(cacheKey, { items, time: Date.now() });
    }
    return items;
  },

  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    const cached = detailCache.get(pageUrl);
    if (cached && Date.now() - cached.time < DETAIL_TTL) return cached.d;

    const html = await fetchHtml(pageUrl, 8000);
    if (!html) return null;

    const $ = cheerio.load(html);

    const title =
      $("h1.movie-title, h1, .title").first().text().trim() || "بدون عنوان";
    const poster =
      $(".movie-poster img, .poster img, .cover img").first().attr("data-src") ||
      $(".movie-poster img, .poster img").first().attr("src") ||
      "/logo.jpg";
    const overview =
      $(".story, .description, .overview").first().text().trim() ||
      "لا يوجد وصف متاح";
    const quality =
      $(".quality, .ribbon, .label").first().text().trim() || "HD";
    const year =
      $(".year, .date").first().text().trim().match(/\d{4}/)?.[0] || "";
    const isSeries = pageUrl.includes("/series/") || pageUrl.includes("/show/");

    const item: UnifiedMediaItem = {
      id: `egybest_${Buffer.from(pageUrl).toString("base64").slice(0, 20)}`,
      title,
      poster: poster.startsWith("http") ? poster : `${BASE}${poster}`,
      pageUrl,
      category: isSeries ? "مسلسل" : "فيلم",
      quality,
      year,
      isSeries,
      providerId: this.id,
      providerName: this.name,
    };

    // Collect servers/iframes
    const metadata = extractMediaMetadata($, pageUrl);
    const servers: UnifiedServer[] = [];
    $("iframe[src], .servers a, a.server-btn, [data-embed]").each((i, el) => {
      const $el = $(el);
      const embedUrl =
        $el.attr("src") ||
        $el.attr("href") ||
        $el.attr("data-embed") ||
        "";
      if (embedUrl && (embedUrl.startsWith("http") || embedUrl.startsWith("//"))) {
        const fullUrl = embedUrl.startsWith("//") ? `https:${embedUrl}` : embedUrl;
        servers.push({
          name: `سيرفر ${i + 1}`,
          embedUrl: fullUrl,
          providerId: this.id,
        });
      }
    });

    // Episodes for series
    const episodes = isSeries
      ? (() => {
          const eps: { title: string; pageUrl: string }[] = [];
          $("a.ep-item, .episodes-list a, .eps a").each((_, el) => {
            const href = $(el).attr("href") || "";
            const epTitle = $(el).text().trim() || `حلقة ${eps.length + 1}`;
            if (href) {
              eps.push({
                title: epTitle,
                pageUrl: href.startsWith("http") ? href : `${BASE}${href}`,
              });
            }
          });
          return eps;
        })()
      : [];

    const detail: UnifiedMediaDetail = {
      item,
      overview,
      cast: metadata.cast,
      servers,
      seasons: metadata.seasons,
      episodes: metadata.episodes.length ? metadata.episodes : episodes,
      proxiedStream: servers[0]?.embedUrl,
    };

    detailCache.set(pageUrl, { d: detail, time: Date.now() });
    return detail;
  },

  async resolveStream(serverUrl: string): Promise<string | null> {
    return serverUrl;
  },
};

export { egybestProvider };
