/**
 * 👑 EgyBest Provider — إيجي بست
 * https://egybest.mx
 * عملاق الأفلام والمسلسلات العربية والأجنبية والأنمي
 */

import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { fetchHtml, extractMetadata, makeId } from "./utils";
import { extractStreamFromEmbed } from "../extractors/stream-extractor";

const BASE = "https://egybest.mx";
const cache = new Map<string, { data: any; time: number }>();
const TTL = 15 * 60 * 1000;

function parseListing(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];

  $("a.movie, div.movie-card a, .content-box a.movie, .movies-list-full a").each((_, el) => {
    const $el = $(el);
    const href = $el.attr("href") || "";
    if (!href || !href.startsWith("http")) return;

    const title =
      $el.find(".title, h2, h3, .movie-title").first().text().trim() ||
      $el.attr("title") ||
      "";
    if (!title || title.length < 2) return;

    const imgEl = $el.find("img").first();
    const rawPoster =
      imgEl.attr("data-src") ||
      imgEl.attr("src") ||
      imgEl.attr("data-lazy") ||
      "";

    let poster = "";
    if (rawPoster.startsWith("http")) poster = rawPoster;
    else if (rawPoster.startsWith("/")) poster = `${BASE}${rawPoster}`;
    else if (rawPoster) poster = `${BASE}/${rawPoster}`;

    const quality = $el.find(".ribbon, .quality, .label").first().text().trim() || "HD";
    const year = $el.find(".year, .date").first().text().trim().match(/\d{4}/)?.[0] || "";
    const isSeries = href.includes("/series/") || href.includes("/show/") || title.includes("مسلسل");

    items.push({
      id: makeId("egybest", href),
      title: title.trim(),
      poster,
      pageUrl: href,
      category: isSeries ? "مسلسلات إيجي بست" : "أفلام إيجي بست",
      quality,
      year,
      isSeries,
      providerId: "egybest",
      providerName: "إيجي بست",
    });
  });

  const seen = new Set<string>();
  return items.filter((i) => {
    if (seen.has(i.pageUrl)) return false;
    seen.add(i.pageUrl);
    return true;
  });
}

export const egybestProvider: BaseProvider = {
  id: "egybest",
  name: "إيجي بست",
  baseUrl: BASE,
  isEnabled: true,

  async search(query: string) {
    const ck = `egybest:s:${query}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const html = await fetchHtml(`${BASE}/explore/?q=${encodeURIComponent(query)}`, `${BASE}/`);
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getCategory(slug, page = 1) {
    const ck = `egybest:cat:${slug}:${page}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

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

    const html = await fetchHtml(url, `${BASE}/`);
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getDetail(pageUrl) {
    const ck = `egybest:d:${pageUrl}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    const html = await fetchHtml(pageUrl, `${BASE}/`, 8000);
    if (!html) return null;
    const $ = cheerio.load(html);

    const title = $("h1").first().text().trim() || $('meta[property="og:title"]').attr("content") || "عرض إيجي بست";
    const poster = $('meta[property="og:image"]').attr("content") || $(".movie-poster img, .poster img").first().attr("src") || "";
    const overview = $('meta[property="og:description"]').attr("content") || $(".story, .desc").first().text().trim() || "";
    const isSeries = pageUrl.includes("/series/") || pageUrl.includes("/show/");
    const meta = extractMetadata($, pageUrl);

    const servers: UnifiedServer[] = [];
    $("iframe[src]").each((i, el) => {
      const src = $(el).attr("src") || "";
      if (src.startsWith("http")) {
        servers.push({
          name: `سيرفر ${i + 1}`,
          embedUrl: src,
          providerId: "egybest",
          isHls: src.includes(".m3u8"),
        });
      }
    });

    $('a[href*="/watch/"], a[data-url], .watch-btn, .server-item').each((_, el) => {
      const u = $(el).attr("data-url") || $(el).attr("href") || "";
      if (u && u.startsWith("http") && !servers.find((s) => s.embedUrl === u)) {
        servers.push({
          name: $(el).text().trim() || `سيرفر ${servers.length + 1}`,
          embedUrl: u,
          providerId: "egybest",
        });
      }
    });

    if (servers.length === 0) {
      servers.push({ name: "سيرفر 1", embedUrl: pageUrl, providerId: "egybest" });
    }

    const detail: UnifiedMediaDetail = {
      item: {
        id: makeId("egybest", pageUrl),
        title,
        poster,
        pageUrl,
        category: isSeries ? "مسلسلات إيجي بست" : "أفلام إيجي بست",
        isSeries,
        providerId: "egybest",
        providerName: "إيجي بست",
      },
      overview,
      cast: meta.cast,
      servers,
      seasons: meta.seasons,
      episodes: meta.episodes,
    };
    cache.set(ck, { data: detail, time: Date.now() });
    return detail;
  },

  async resolveStream(serverUrl) {
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) return serverUrl;
    const r = await extractStreamFromEmbed(serverUrl, BASE);
    return r?.url || serverUrl;
  },
};
