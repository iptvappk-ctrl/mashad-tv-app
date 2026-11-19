/**
 * 🎬 WeCima Provider — وي سيما
 * https://myweecima.casa | مكتبة ضخمة عربية وتركية وأجنبية
 */

import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { fetchHtml, extractMetadata, makeId } from "./utils";
import { extractStreamFromEmbed } from "../extractors/stream-extractor";

const BASE = "https://myweecima.casa";
const cache = new Map<string, { data: any; time: number }>();
const TTL = 15 * 60 * 1000;

function parseListing(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];
  $(".Thumb--GridItem a, .GridItem a").each((_, el) => {
    const a = $(el);
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http")) return;
    const title = a.attr("title") || a.find("img").attr("alt") || a.find("strong").text().trim() || "";
    if (!title || title.length < 2) return;
    let poster = a.find("img").attr("data-src") || a.find("img").attr("src") || "";
    if (!poster || poster.includes("logo")) {
      const style = a.find("[style*='background']").attr("style") || "";
      const bg = style.match(/url\(['"]?(https?:\/\/[^'")\s]+)/);
      if (bg) poster = bg[1];
    }
    if (poster.startsWith("//")) poster = "https:" + poster;
    const isSeries = href.includes("series") || href.includes("مسلسل");
    items.push({
      id: makeId("wecima", href),
      title: title.trim(),
      poster: poster.startsWith("http") ? poster : "",
      pageUrl: href,
      category: isSeries ? "مسلسلات وي سيما" : "أفلام وي سيما",
      isSeries,
      providerId: "wecima",
      providerName: "وي سيما",
    });
  });
  const seen = new Set<string>();
  return items.filter((i) => { if (seen.has(i.pageUrl)) return false; seen.add(i.pageUrl); return true; });
}

export const wecimaProvider: BaseProvider = {
  id: "wecima",
  name: "وي سيما",
  baseUrl: BASE,
  isEnabled: true,

  async search(query: string) {
    const ck = `wecima:s:${query}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const html = await fetchHtml(`${BASE}/?s=${encodeURIComponent(query)}`, BASE + "/");
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getCategory(slug, page = 1) {
    const ck = `wecima:cat:${slug}:${page}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const paths: Record<string, string> = {
      movies: "/movies/", series: "/series/", arabic_movies: "/category/افلام-عربية/",
      arabic_series: "/category/مسلسلات-عربية/", turkish_series: "/category/مسلسلات-تركية/",
      turkish_movies: "/category/افلام-تركية/", foreign_movies: "/category/افلام-اجنبية/",
      anime: "/category/انمي/", ramadan: "/category/رمضان/",
    };
    const path = paths[slug] || "/movies/";
    const url = page <= 1 ? `${BASE}${path}` : `${BASE}${path}page/${page}/`;
    const html = await fetchHtml(url, BASE + "/");
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getDetail(pageUrl) {
    const ck = `wecima:d:${pageUrl}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const html = await fetchHtml(pageUrl, BASE + "/", 8000);
    if (!html) return null;
    const $ = cheerio.load(html);
    const title = $('meta[property="og:title"]').attr("content") || $("h1").first().text().trim() || "";
    const poster = $('meta[property="og:image"]').attr("content") || $(".Poster--GridItem img").first().attr("src") || "";
    const overview = $('meta[property="og:description"]').attr("content") || $(".StoryLine").first().text().trim() || "";
    const isSeries = pageUrl.includes("series") || pageUrl.includes("مسلسل");
    const meta = extractMetadata($, pageUrl);
    const servers: UnifiedServer[] = [];
    $("iframe[src]").each((i, el) => {
      const src = $(el).attr("src") || "";
      if (src.startsWith("http")) servers.push({ name: `سيرفر ${i + 1}`, embedUrl: src, providerId: "wecima", isHls: src.includes(".m3u8") });
    });
    $("[data-url], .WatchServersList a, .ServersList a").each((_, el) => {
      const eu = $(el).attr("data-url") || $(el).attr("href") || "";
      if (eu && eu.startsWith("http") && !servers.find(s => s.embedUrl === eu))
        servers.push({ name: $(el).text().trim() || `سيرفر ${servers.length + 1}`, embedUrl: eu, providerId: "wecima" });
    });
    if (!servers.length) servers.push({ name: "سيرفر 1", embedUrl: pageUrl, providerId: "wecima" });
    const detail: UnifiedMediaDetail = {
      item: { id: makeId("wecima", pageUrl), title, poster, pageUrl, category: isSeries ? "مسلسلات" : "أفلام", isSeries, providerId: "wecima", providerName: "وي سيما" },
      overview, cast: meta.cast, servers, seasons: meta.seasons, episodes: meta.episodes,
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
