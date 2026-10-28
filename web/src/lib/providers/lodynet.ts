/**
 * Lodynet Provider — https://www.lodynet.art
 * موقع شهير لمشاهدة المسلسلات العربية والتركية
 */

import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";

const BASE = "https://lodynet.art";
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

  // Lodynet card structure
  $("article.bt, .content-box article, .movies-list article, .post-cards article").each((_, el) => {
    const $el = $(el);
    const $a = $el.find("a").first();
    const href = $a.attr("href") || $el.find("a[href]").first().attr("href") || "";
    if (!href || !href.startsWith("http")) return;

    const title =
      $el.find("h2, h3, .title, .entry-title").first().text().trim() ||
      $a.attr("title") || "";
    if (!title || title.length < 2) return;

    const imgEl = $el.find("img").first();
    const poster =
      imgEl.attr("data-src") ||
      imgEl.attr("src") ||
      "/logo.jpg";

    const quality = $el.find(".quality, .ribbon, span.qlty").first().text().trim() || "HD";
    const year = $el.find(".year, .date").text().match(/\d{4}/)?.[0] || "";
    const isSeries =
      href.includes("/series/") || href.includes("/show/") || href.includes("/مسلسل");

    items.push({
      id: `lodynet_${Buffer.from(href).toString("base64").slice(0, 20)}`,
      title,
      poster: poster.startsWith("http") ? poster : `${BASE}${poster}`,
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

const lodynetProvider: BaseProvider = {
  id: "lodynet",
  name: "لودي نت",
  baseUrl: BASE,
  isEnabled: true,

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `lodynet:cat:${slug}:${page}`;
    const cached = listCache.get(cacheKey);
    if (cached && Date.now() - cached.time < LIST_TTL) return cached.items;

    let url = `${BASE}/page/${page}/`;
    if (slug === "series" || slug === "foreign_series" || slug === "arabic_series") {
      url = `${BASE}/category/series/page/${page}/`;
    } else if (slug === "turkish_series") {
      url = `${BASE}/category/turkish-series/page/${page}/`;
    } else if (slug === "anime") {
      url = `${BASE}/category/anime/page/${page}/`;
    }

    const html = await fetchHtml(url);
    const items = parseItems(html, this.id, this.name);
    if (items.length > 0) {
      listCache.set(cacheKey, { items, time: Date.now() });
    }
    return items;
  },

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `lodynet:search:${query.toLowerCase()}`;
    const cached = listCache.get(cacheKey);
    if (cached && Date.now() - cached.time < LIST_TTL) return cached.items;

    const url = `${BASE}/?s=${encodeURIComponent(query)}`;
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

    const title = $("h1.title, h1.entry-title, h1").first().text().trim() || "بدون عنوان";
    const poster =
      $(".poster img, .movie-poster img, .entry-image img").first().attr("data-src") ||
      $(".poster img, .entry-image img").first().attr("src") ||
      "/logo.jpg";
    const overview =
      $(".story, .overview, .entry-content p").first().text().trim() ||
      "لا يوجد وصف متاح";
    const quality = $(".quality, .qlty").first().text().trim() || "HD";
    const year = $(".date, .year").text().match(/\d{4}/)?.[0] || "";
    const isSeries = pageUrl.includes("/series/") || pageUrl.includes("/مسلسل");

    const item: UnifiedMediaItem = {
      id: `lodynet_${Buffer.from(pageUrl).toString("base64").slice(0, 20)}`,
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

    const metadata = extractMediaMetadata($, pageUrl);
    const servers: UnifiedServer[] = [];
    $("iframe[src], .watch-buttons a, .serversList a, [data-embed]").each((i, el) => {
      const $el = $(el);
      const embedUrl =
        $el.attr("src") || $el.attr("href") || $el.attr("data-embed") || "";
      if (embedUrl && embedUrl.startsWith("http")) {
        servers.push({
          name: `سيرفر ${i + 1}`,
          embedUrl,
          providerId: this.id,
        });
      }
    });

    const episodes: { title: string; pageUrl: string }[] = [];
    if (isSeries) {
      $("a.ep-item, .epsList a, .episodes a").each((_, el) => {
        const href = $(el).attr("href") || "";
        const epTitle = $(el).text().trim() || `حلقة ${episodes.length + 1}`;
        if (href) {
          episodes.push({
            title: epTitle,
            pageUrl: href.startsWith("http") ? href : `${BASE}${href}`,
          });
        }
      });
    }

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

export { lodynetProvider };
