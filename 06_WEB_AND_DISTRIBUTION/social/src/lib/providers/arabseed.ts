/**
 * 🌐 ArabSeed Provider — عرب سيد
 * https://arabseed.health
 * أفلام ومسلسلات عربية وأجنبية — سيرفرات سريعة
 */

import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { fetchHtml, extractMetadata, makeId } from "./utils";
import { extractStreamFromEmbed } from "../extractors/stream-extractor";

const BASE = "https://arabseed.health";
const cache = new Map<string, { data: any; time: number }>();
const TTL = 15 * 60 * 1000;

function parseListing(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];
  $("ul.Blocks-UL li, .GridItem, .MovieBlock").each((_, el) => {
    const a = $(el).find("a").first();
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http")) return;
    const title = a.find("h4, h3, .BlockTitle, .title").first().text().trim() || a.find("img").attr("alt") || "";
    if (!title || title.length < 2) return;
    let poster = a.find("img").attr("data-src") || a.find("img").attr("src") || "";
    if (poster.startsWith("//")) poster = "https:" + poster;
    const isSeries = href.includes("/series/") || href.includes("/مسلسل");
    items.push({
      id: makeId("arabseed", href),
      title: title.trim(),
      poster: poster.startsWith("http") ? poster : "",
      pageUrl: href,
      category: isSeries ? "مسلسلات" : "أفلام",
      isSeries,
      providerId: "arabseed",
      providerName: "عرب سيد",
    });
  });
  const seen = new Set<string>();
  return items.filter((i) => { if (seen.has(i.pageUrl)) return false; seen.add(i.pageUrl); return true; });
}

export const arabseedProvider: BaseProvider = {
  id: "arabseed",
  name: "عرب سيد",
  baseUrl: BASE,
  isEnabled: true,

  async search(query: string) {
    const ck = `arabseed:s:${query}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const html = await fetchHtml(`${BASE}/?s=${encodeURIComponent(query)}`, BASE + "/");
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getCategory(slug, page = 1) {
    const ck = `arabseed:cat:${slug}:${page}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const paths: Record<string, string> = {
      movies: "/movies", series: "/series", arabic_movies: "/category/افلام-عربية",
      arabic_series: "/category/مسلسلات-عربية", turkish_series: "/category/مسلسلات-تركية",
      turkish_movies: "/category/افلام-تركية", foreign_movies: "/category/افلام-اجنبية",
      foreign_series: "/category/مسلسلات-اجنبية", anime: "/category/انمي", ramadan: "/category/رمضان",
    };
    const path = paths[slug] || "/movies";
    const url = page <= 1 ? `${BASE}${path}/` : `${BASE}${path}/page/${page}/`;
    const html = await fetchHtml(url, BASE + "/");
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getDetail(pageUrl) {
    const ck = `arabseed:d:${pageUrl}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const html = await fetchHtml(pageUrl, BASE + "/", 8000);
    if (!html) return null;
    const $ = cheerio.load(html);
    const title = $("h1").first().text().trim() || $('meta[property="og:title"]').attr("content") || "";
    const poster = $('meta[property="og:image"]').attr("content") || $(".Poster img").first().attr("src") || "";
    const overview = $('meta[property="og:description"]').attr("content") || $(".StoryLine, .story").first().text().trim() || "";
    const isSeries = pageUrl.includes("/series/");
    const meta = extractMetadata($, pageUrl);
    const servers: UnifiedServer[] = [];
    $("iframe[src]").each((i, el) => {
      const src = $(el).attr("src") || "";
      if (src.startsWith("http")) servers.push({ name: `سيرفر ${i + 1}`, embedUrl: src, providerId: "arabseed", isHls: src.includes(".m3u8") });
    });
    $("[data-link], .WatchServers a, .server-btn").each((_, el) => {
      const eu = $(el).attr("data-link") || $(el).attr("href") || "";
      if (eu && eu.startsWith("http") && !servers.find(s => s.embedUrl === eu))
        servers.push({ name: $(el).text().trim() || `سيرفر ${servers.length + 1}`, embedUrl: eu, providerId: "arabseed" });
    });
    if (!servers.length) servers.push({ name: "سيرفر 1", embedUrl: pageUrl, providerId: "arabseed" });
    const detail: UnifiedMediaDetail = {
      item: { id: makeId("arabseed", pageUrl), title, poster, pageUrl, category: isSeries ? "مسلسلات" : "أفلام", isSeries, providerId: "arabseed", providerName: "عرب سيد" },
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
