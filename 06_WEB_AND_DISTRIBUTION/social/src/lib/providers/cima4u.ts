/**
 * 🍿 Cima4U Provider — سيما فور يو
 * https://cima4u.cc | أفلام ومسلسلات عربية وأجنبية متنوعة
 */

import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { fetchHtml, extractMetadata, makeId } from "./utils";
import { extractStreamFromEmbed } from "../extractors/stream-extractor";

const BASE = "https://cima4u.cc";
const cache = new Map<string, { data: any; time: number }>();
const TTL = 15 * 60 * 1000;

function parseListing(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];

  $(".MovieBlock a, .GridItem a, article.post a, .movies-list a").each((_, el) => {
    const a = $(el);
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http")) return;
    if (href.includes("/category/") || href.includes("/tag/") || href.includes("/page/")) return;

    const title =
      a.find("h1, h2, h3, .title, .BlockTitle").first().text().trim() ||
      a.attr("title") ||
      a.find("img").attr("alt") ||
      "";
    if (!title || title.length < 3) return;

    let poster =
      a.find("img").attr("data-src") ||
      a.find("img").attr("src") ||
      a.find("img").attr("data-lazy-src") ||
      "";

    if (poster.startsWith("//")) poster = "https:" + poster;
    const isSeries = href.includes("/series/") || href.includes("/مسلسل") || title.includes("مسلسل");

    items.push({
      id: makeId("cima4u", href),
      title: title.replace(/\s+/g, " ").trim(),
      poster: poster.startsWith("http") ? poster : "",
      pageUrl: href,
      category: isSeries ? "مسلسلات سيما فور يو" : "أفلام سيما فور يو",
      isSeries,
      providerId: "cima4u",
      providerName: "سيما فور يو",
    });
  });

  const seen = new Set<string>();
  return items.filter((i) => {
    if (seen.has(i.pageUrl)) return false;
    seen.add(i.pageUrl);
    return true;
  });
}

export const cima4uProvider: BaseProvider = {
  id: "cima4u",
  name: "سيما فور يو",
  baseUrl: BASE,
  isEnabled: true,

  async search(query: string) {
    const ck = `cima4u:s:${query}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const html = await fetchHtml(`${BASE}/?s=${encodeURIComponent(query)}`, `${BASE}/`);
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getCategory(slug, page = 1) {
    const ck = `cima4u:cat:${slug}:${page}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    const paths: Record<string, string> = {
      movies: "/category/افلام",
      series: "/category/مسلسلات",
      turkish_series: "/category/مسلسلات-تركية",
      turkish_movies: "/category/افلام-تركية",
      foreign_movies: "/category/افلام-اجنبية",
      foreign_series: "/category/مسلسلات-اجنبية",
      anime: "/category/انمي",
      arabic_movies: "/category/افلام-عربية",
    };
    const path = paths[slug] || "/category/افلام";
    const url = page <= 1 ? `${BASE}${encodeURI(path)}/` : `${BASE}${encodeURI(path)}/page/${page}/`;
    const html = await fetchHtml(url, `${BASE}/`);
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getDetail(pageUrl) {
    const ck = `cima4u:d:${pageUrl}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    const html = await fetchHtml(pageUrl, `${BASE}/`, 8000);
    if (!html) return null;
    const $ = cheerio.load(html);

    const title = $("h1").first().text().trim() || $('meta[property="og:title"]').attr("content") || "عرض سيما فور يو";
    const overview = $('meta[property="og:description"]').attr("content") || $(".story, .Description p").first().text().trim() || "";
    const poster = $('meta[property="og:image"]').attr("content") || $(".Poster img, .poster img").first().attr("src") || "";
    const isSeries = pageUrl.includes("/series/") || title.includes("مسلسل");
    const meta = extractMetadata($, pageUrl);

    const servers: UnifiedServer[] = [];
    $("iframe[src]").each((i, el) => {
      const src = $(el).attr("src") || "";
      if (src.startsWith("http")) {
        servers.push({
          name: `سيرفر ${i + 1}`,
          embedUrl: src,
          providerId: "cima4u",
          isHls: src.includes(".m3u8"),
        });
      }
    });

    $(".WatchServers a, .servers-list a, [data-embed], .server-btn").each((_, el) => {
      const embedUrl = $(el).attr("data-embed") || $(el).attr("href") || $(el).attr("data-url") || "";
      if (embedUrl && embedUrl.startsWith("http") && !servers.find((s) => s.embedUrl === embedUrl)) {
        servers.push({
          name: $(el).text().trim() || `سيرفر ${servers.length + 1}`,
          embedUrl,
          providerId: "cima4u",
        });
      }
    });

    if (servers.length === 0) {
      servers.push({ name: "سيرفر 1", embedUrl: pageUrl, providerId: "cima4u" });
    }

    const detail: UnifiedMediaDetail = {
      item: {
        id: makeId("cima4u", pageUrl),
        title,
        poster,
        pageUrl,
        category: isSeries ? "مسلسلات" : "أفلام",
        isSeries,
        providerId: "cima4u",
        providerName: "سيما فور يو",
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
