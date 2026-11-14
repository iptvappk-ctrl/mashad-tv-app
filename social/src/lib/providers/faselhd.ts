/**
 * 🎬 FaselHD Provider — فاصل اعلاني
 * https://fasel4k.site | 4K وFHD — أفلام ومسلسلات
 */
import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { fetchHtml, extractMetadata, makeId } from "./utils";
import { extractStreamFromEmbed } from "../extractors/stream-extractor";

const BASE = "https://wb49584.fasel4k.site";
const cache = new Map<string, { data: any; time: number }>();
const TTL = 15 * 60 * 1000;

function parseListing(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];
  $('a[href*="/movie/"], a[href*="/serie/"]').each((_, el) => {
    const a = $(el);
    const href = a.attr("href") || "";
    if (!href) return;
    const title = a.find("img").attr("alt") || a.find(".title, h2, h3").text().trim() || "";
    if (!title || title.length < 3) return;
    let poster = a.find("img").attr("src") || a.find("img").attr("data-src") || "";
    if (poster.startsWith("//")) poster = "https:" + poster;
    const isSeries = href.includes("/serie/");
    items.push({
      id: makeId("faselhd", href),
      title: title.trim(),
      poster: poster.startsWith("http") ? poster : "",
      pageUrl: href.startsWith("http") ? href : `${BASE}${href}`,
      category: isSeries ? "مسلسلات فاصل" : "أفلام فاصل",
      isSeries,
      providerId: "faselhd",
      providerName: "فاصل اعلاني",
    });
  });
  const seen = new Set<string>();
  return items.filter((i) => { if (seen.has(i.pageUrl)) return false; seen.add(i.pageUrl); return true; });
}

export const faselHDProvider: BaseProvider = {
  id: "faselhd",
  name: "فاصل اعلاني",
  baseUrl: BASE,
  isEnabled: true,

  async search(query) {
    const ck = `faselhd:s:${query}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const html = await fetchHtml(`${BASE}/search?q=${encodeURIComponent(query)}`, BASE + "/");
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getCategory(slug, page = 1) {
    const ck = `faselhd:cat:${slug}:${page}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const paths: Record<string, string> = {
      movies: "/movies", series: "/series", foreign_movies: "/movies?genre=foreign",
      turkish_series: "/series?genre=turkish", anime: "/series?genre=anime",
    };
    const path = paths[slug] || "/movies";
    const url = page <= 1 ? `${BASE}${path}` : `${BASE}${path}${path.includes("?") ? "&" : "?"}page=${page}`;
    const html = await fetchHtml(url, BASE + "/");
    const items = parseListing(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getDetail(pageUrl) {
    const ck = `faselhd:d:${pageUrl}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;
    const html = await fetchHtml(pageUrl, BASE + "/", 8000);
    if (!html) return null;
    const $ = cheerio.load(html);
    const title = $("h1").first().text().trim() || $('meta[property="og:title"]').attr("content") || "";
    const poster = $('meta[property="og:image"]').attr("content") || $("img.poster").attr("src") || "";
    const overview = $('meta[property="og:description"]').attr("content") || $(".story").first().text().trim() || "";
    const isSeries = pageUrl.includes("/serie/");
    const meta = extractMetadata($, pageUrl);
    const servers: UnifiedServer[] = [];

    // FaselHD uses AJAX embed
    const embedBtns: { id: string; name: string }[] = [];
    $(".btn-service[data-embed]").each((_, el) => {
      const id = $(el).attr("data-embed") || "";
      const name = $(el).find(".name").text().trim() || $(el).text().trim() || "سيرفر";
      if (id) embedBtns.push({ id, name });
    });

    if (embedBtns.length > 0) {
      try {
        const ajaxRes = await fetch(`${BASE}/ajax/embed`, {
          method: "POST",
          headers: { "User-Agent": "Mozilla/5.0", Referer: pageUrl, "X-Requested-With": "XMLHttpRequest", "Content-Type": "application/x-www-form-urlencoded" },
          body: `id=${encodeURIComponent(embedBtns[0].id)}`,
        });
        const ajaxText = await ajaxRes.text();
        const iframeMatch = ajaxText.match(/src=["']([^"']+)["']/i);
        if (iframeMatch?.[1]) {
          servers.push({ name: `سيرفر 1 (${embedBtns[0].name})`, embedUrl: iframeMatch[1], providerId: "faselhd", isHls: true });
        }
      } catch {}
      for (let i = 1; i < embedBtns.length; i++) {
        servers.push({ name: `سيرفر ${i + 1} (${embedBtns[i].name})`, embedUrl: `${BASE}/ajax/embed?id=${embedBtns[i].id}`, providerId: "faselhd" });
      }
    }

    $("iframe[src]").each((i, el) => {
      const src = $(el).attr("src") || "";
      if (src.startsWith("http") && !servers.find(s => s.embedUrl === src))
        servers.push({ name: `سيرفر ${servers.length + 1}`, embedUrl: src, providerId: "faselhd" });
    });

    if (!servers.length) servers.push({ name: "سيرفر 1", embedUrl: pageUrl, providerId: "faselhd" });
    const detail: UnifiedMediaDetail = {
      item: { id: makeId("faselhd", pageUrl), title, poster, pageUrl, category: isSeries ? "مسلسلات" : "أفلام", isSeries, providerId: "faselhd", providerName: "فاصل اعلاني" },
      overview, cast: meta.cast, servers, seasons: meta.seasons, episodes: meta.episodes,
    };
    cache.set(ck, { data: detail, time: Date.now() });
    return detail;
  },

  async resolveStream(serverUrl) {
    if (serverUrl.includes("/ajax/embed?id=")) {
      try {
        const id = new URL(serverUrl).searchParams.get("id");
        if (id) {
          const res = await fetch(`${BASE}/ajax/embed`, {
            method: "POST",
            headers: { "User-Agent": "Mozilla/5.0", Referer: BASE + "/", "X-Requested-With": "XMLHttpRequest", "Content-Type": "application/x-www-form-urlencoded" },
            body: `id=${encodeURIComponent(id)}`,
          });
          const text = await res.text();
          const m = text.match(/src=["']([^"']+)["']/i);
          if (m?.[1]) serverUrl = m[1];
        }
      } catch {}
    }
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) return serverUrl;
    const r = await extractStreamFromEmbed(serverUrl, BASE);
    return r?.url || serverUrl;
  },
};
