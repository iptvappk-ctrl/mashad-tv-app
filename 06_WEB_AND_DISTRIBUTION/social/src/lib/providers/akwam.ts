/**
 * 📦 Akwam Provider — أكوام
 * https://akwam.one / https://akw.cam
 * مكتبة ضخمة للأفلام والمسلسلات العربية والأجنبية والمدبلجة
 */

import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { fetchHtml, extractMetadata, makeId } from "./utils";
import { extractStreamFromEmbed } from "../extractors/stream-extractor";

const BASE = "https://akw.cam";
const cache = new Map<string, { data: any; time: number }>();
const TTL = 15 * 60 * 1000;

function parseListing(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];

  $('a[href*="/akfilm-"], a[href*="/akseries-"], a[href*="/akepisode-"], .entry-box a').each((_, el) => {
    const a = $(el);
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http")) return;
    if (href.includes("/category/") || href.includes("/tag/") || href.includes("/page/")) return;

    const title =
      a.find("h3, h2, .entry-title, .title").first().text().trim() ||
      a.attr("title") ||
      a.find("img").attr("alt") ||
      "";
    if (!title || title.length < 2) return;

    let poster = a.find("img").attr("data-src") || a.find("img").attr("src") || "";
    if (!poster || poster.includes("logo")) {
      const style = a.find("[style*='background-image']").attr("style") || "";
      const bg = style.match(/url\(['"]?(https?:\/\/[^'")\s]+)/);
      if (bg) poster = bg[1];
    }
    if (poster.startsWith("//")) poster = "https:" + poster;

    const isSeries = href.includes("series") || href.includes("episode") || title.includes("مسلسل");
    const cleanTitle = title.replace(/^NEW\s*/i, "").replace(/فيلم\s+\d+$/i, "").trim();

    items.push({
      id: makeId("akwam", href),
      title: cleanTitle,
      poster: poster.startsWith("http") ? poster : "",
      pageUrl: href,
      category: isSeries ? "مسلسلات أكوام" : "أفلام أكوام",
      isSeries,
      providerId: "akwam",
      providerName: "أكوام",
    });
  });

  const seen = new Set<string>();
  return items.filter((i) => {
    if (seen.has(i.pageUrl)) return false;
    seen.add(i.pageUrl);
    return true;
  });
}

export const akwamProvider: BaseProvider = {
  id: "akwam",
  name: "أكوام",
  baseUrl: BASE,
  isEnabled: true,

  async search(query: string) {
    const ck = `akwam:s:${query}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const html = await fetchHtml(`${BASE}/?s=${encodeURIComponent(query)}`, `${BASE}/`);
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getCategory(slug, page = 1) {
    const ck = `akwam:cat:${slug}:${page}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    const paths: Record<string, string> = {
      movies: "/films",
      series: "/seriestv",
      foreign_movies: "/category/افلام-اجنبي",
      arabic_movies: "/category/افلام-عربية",
      turkish_series: "/category/مسلسلات-تركية",
      anime: "/category/انمي",
      dubbed: "/category/افلام-مدبلجة",
    };
    const path = paths[slug] || "/films";
    const url = page <= 1 ? `${BASE}${encodeURI(path)}` : `${BASE}${encodeURI(path)}/page/${page}`;
    const html = await fetchHtml(url, `${BASE}/`);
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getDetail(pageUrl) {
    const ck = `akwam:d:${pageUrl}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    const html = await fetchHtml(pageUrl, `${BASE}/`, 8000);
    if (!html) return null;
    const $ = cheerio.load(html);

    const title = $("h1, .entry-title").first().text().trim() || $('meta[property="og:title"]').attr("content") || "عرض أكوام";
    const overview = $(".story, .overview, .entry-content p, p").first().text().trim() || "";
    const poster = $('meta[property="og:image"]').attr("content") || $("img.attachment-post-thumbnail").attr("src") || "";
    const isSeries = pageUrl.includes("series") || title.includes("مسلسل");
    const meta = extractMetadata($, pageUrl);

    let watchUrl = $('a[href*="/watch/"]').first().attr("href") || `${pageUrl.replace(/\/$/, "")}/servers`;
    const servers: UnifiedServer[] = [];

    try {
      const watchHtml = await fetchHtml(watchUrl, pageUrl, 5000);
      if (watchHtml) {
        const $w = cheerio.load(watchHtml);
        const primary = $w("iframe").first().attr("src");
        if (primary && primary.startsWith("http")) {
          servers.push({
            name: "سيرفر 1 (فائق السرعة)",
            embedUrl: primary,
            providerId: "akwam",
            isHls: true,
          });
        }
        $w("a[data-url], [data-src], a.server, .watchServers a").each((i, el) => {
          const sUrl = $w(el).attr("data-url") || $w(el).attr("data-src") || $w(el).attr("href");
          const sName = $w(el).text().trim().replace(/أكوام|akwam/gi, "").trim() || `سيرفر ${i + 1}`;
          if (sUrl && sUrl.startsWith("http") && !servers.some((s) => s.embedUrl === sUrl)) {
            servers.push({
              name: `سيرفر ${servers.length + 1} (${sName})`,
              embedUrl: sUrl,
              providerId: "akwam",
              isHls: sUrl.includes(".m3u8"),
            });
          }
        });
      }
    } catch {}

    if (servers.length === 0) {
      servers.push({ name: "سيرفر 1 (مباشر)", embedUrl: watchUrl, providerId: "akwam" });
    }

    const detail: UnifiedMediaDetail = {
      item: {
        id: makeId("akwam", pageUrl),
        title,
        poster,
        pageUrl,
        category: isSeries ? "مسلسلات أكوام" : "أفلام أكوام",
        isSeries,
        providerId: "akwam",
        providerName: "أكوام",
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
