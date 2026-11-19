/**
 * 🇹🇷 Turkish Provider — متخصص المسلسلات والأفلام التركية
 * مصادر متعددة: Dizimag (مترجم) + TurkFan (مدبلج) + Lodynet Turkish
 */

import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { fetchHtml, extractMetadata, makeId } from "./utils";
import { extractStreamFromEmbed } from "../extractors/stream-extractor";

const DIZIMAG_BASE = "https://www.dizimag.net";
const TURKFAN_BASE = "https://turkfan.net";
const cache = new Map<string, { data: any; time: number }>();
const TTL = 15 * 60 * 1000;

function parseDizimag(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];

  $(".film-box a, .dizi-kutu a, article a, .series-card a").each((_, el) => {
    const a = $(el);
    const href = a.attr("href") || "";
    if (!href) return;
    const fullHref = href.startsWith("http") ? href : `${DIZIMAG_BASE}${href}`;

    const title =
      a.find("h2, h3, .title, .film-name").first().text().trim() ||
      a.attr("title") ||
      a.find("img").attr("alt") ||
      "";
    if (!title || title.length < 3) return;

    let poster = a.find("img").attr("data-src") || a.find("img").attr("src") || "";
    if (poster.startsWith("//")) poster = "https:" + poster;

    const year = a.find(".year, .date").first().text().trim().match(/\d{4}/)?.[0] || "";

    items.push({
      id: makeId("turkish_dizi", fullHref),
      title: title.trim(),
      poster: poster.startsWith("http") ? poster : "",
      pageUrl: fullHref,
      category: "مسلسلات تركية مترجمة",
      year,
      isSeries: true,
      providerId: "turkish_hub",
      providerName: "الدراما التركية",
    });
  });

  const seen = new Set<string>();
  return items.filter((i) => {
    if (seen.has(i.pageUrl)) return false;
    seen.add(i.pageUrl);
    return true;
  });
}

function parseTurkfan(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];

  $("article a, .series-block a, .MovieBlock a, .block-contain a").each((_, el) => {
    const a = $(el);
    const href = a.attr("href") || "";
    if (!href) return;
    const fullHref = href.startsWith("http") ? href : `${TURKFAN_BASE}${href}`;

    const title =
      a.find("h2, h3, .title").first().text().trim() ||
      a.attr("title") ||
      a.find("img").attr("alt") ||
      "";
    if (!title || title.length < 3) return;

    let poster = a.find("img").attr("data-src") || a.find("img").attr("src") || "";
    if (poster.startsWith("//")) poster = "https:" + poster;

    items.push({
      id: makeId("turkish_fan", fullHref),
      title: title.trim(),
      poster: poster.startsWith("http") ? poster : "",
      pageUrl: fullHref,
      category: "مسلسلات تركية مدبلجة",
      isSeries: true,
      providerId: "turkish_hub",
      providerName: "الدراما التركية",
    });
  });

  const seen = new Set<string>();
  return items.filter((i) => {
    if (seen.has(i.pageUrl)) return false;
    seen.add(i.pageUrl);
    return true;
  });
}

export const turkishProvider: BaseProvider = {
  id: "turkish_hub",
  name: "الدراما التركية (مترجم ومدبلج)",
  baseUrl: DIZIMAG_BASE,
  isEnabled: true,

  async search(query: string) {
    const ck = `turkish:s:${query}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    const [diziHtml, fanHtml] = await Promise.all([
      fetchHtml(`${DIZIMAG_BASE}/?s=${encodeURIComponent(query)}`, `${DIZIMAG_BASE}/`),
      fetchHtml(`${TURKFAN_BASE}/?s=${encodeURIComponent(query)}`, `${TURKFAN_BASE}/`),
    ]);

    const items = [...parseDizimag(diziHtml), ...parseTurkfan(fanHtml)];
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getCategory(slug, page = 1) {
    const ck = `turkish:cat:${slug}:${page}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    let items: UnifiedMediaItem[] = [];
    if (slug === "turkish_dubbed") {
      const html = await fetchHtml(`${TURKFAN_BASE}/category/مسلسلات-تركية-مدبلجة/page/${page}/`, `${TURKFAN_BASE}/`);
      items = parseTurkfan(html);
    } else {
      const path = slug === "turkish_movies" ? "/category/افلام-تركية/" : "/category/مسلسلات-تركية/";
      const html = await fetchHtml(`${DIZIMAG_BASE}${encodeURI(path)}page/${page}/`, `${DIZIMAG_BASE}/`);
      items = parseDizimag(html);
    }

    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getDetail(pageUrl) {
    const ck = `turkish:d:${pageUrl}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    const ref = pageUrl.includes("turkfan") ? `${TURKFAN_BASE}/` : `${DIZIMAG_BASE}/`;
    const html = await fetchHtml(pageUrl, ref, 8000);
    if (!html) return null;
    const $ = cheerio.load(html);

    const title = $("h1").first().text().trim() || $('meta[property="og:title"]').attr("content") || "مسلسل تركي";
    const poster = $('meta[property="og:image"]').attr("content") || $(".poster img").first().attr("src") || "";
    const overview = $('meta[property="og:description"]').attr("content") || $(".story, p").first().text().trim() || "";
    const meta = extractMetadata($, pageUrl);

    const servers: UnifiedServer[] = [];
    $("iframe[src], .server-btn a, .watch-link, [data-embed]").each((i, el) => {
      const src = $(el).attr("src") || $(el).attr("href") || $(el).attr("data-embed") || "";
      if (src.startsWith("http") && !servers.find((s) => s.embedUrl === src)) {
        servers.push({
          name: $(el).text().trim() || `سيرفر ${i + 1}`,
          embedUrl: src,
          providerId: "turkish_hub",
          isHls: src.includes(".m3u8"),
        });
      }
    });

    if (servers.length === 0) {
      servers.push({ name: "سيرفر 1", embedUrl: pageUrl, providerId: "turkish_hub" });
    }

    const detail: UnifiedMediaDetail = {
      item: {
        id: makeId("turkish", pageUrl),
        title,
        poster,
        pageUrl,
        category: "مسلسلات تركية",
        isSeries: true,
        providerId: "turkish_hub",
        providerName: "الدراما التركية",
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
    const r = await extractStreamFromEmbed(serverUrl, DIZIMAG_BASE);
    return r?.url || serverUrl;
  },
};
