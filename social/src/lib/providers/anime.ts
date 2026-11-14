/**
 * 🎌 Anime Hub Provider — متخصص الأنمي (مدبلج ومترجم)
 * مصادر: AnimeBlkom + Shahiid Anime + AnimeSlayer
 */

import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { fetchHtml, extractMetadata, makeId } from "./utils";
import { extractStreamFromEmbed } from "../extractors/stream-extractor";

const BLKOM_BASE = "https://animeblkom.net";
const SHAHIID_BASE = "https://shahiidanime.net";
const cache = new Map<string, { data: any; time: number }>();
const TTL = 15 * 60 * 1000;

function parseBlkom(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];

  $(".anime-card a, .anime_card a, .animeCard a, .block-contain a").each((_, el) => {
    const a = $(el);
    const href = a.attr("href") || "";
    if (!href) return;
    const fullHref = href.startsWith("http") ? href : `${BLKOM_BASE}${href}`;

    const title =
      a.find("span.title, .info h3, .name, h3, h2").first().text().trim() ||
      a.attr("title") ||
      a.find("img").attr("alt") ||
      "";
    if (!title || title.length < 2) return;

    let poster = a.find("img").attr("data-src") || a.find("img").attr("src") || "";
    if (poster.startsWith("//")) poster = "https:" + poster;

    items.push({
      id: makeId("anime_blkom", fullHref),
      title: title.trim(),
      poster: poster.startsWith("http") ? poster : "",
      pageUrl: fullHref,
      category: "أنمي مدبلج ومترجم",
      isSeries: true,
      providerId: "anime_hub",
      providerName: "عالم الأنمي",
    });
  });

  const seen = new Set<string>();
  return items.filter((i) => {
    if (seen.has(i.pageUrl)) return false;
    seen.add(i.pageUrl);
    return true;
  });
}

function parseShahiid(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];

  $("article a, .anime-card a, .item a").each((_, el) => {
    const a = $(el);
    const href = a.attr("href") || "";
    if (!href) return;
    const fullHref = href.startsWith("http") ? href : `${SHAHIID_BASE}${href}`;

    const title =
      a.find("h2, h3, .title, .name").first().text().trim() ||
      a.attr("title") ||
      a.find("img").attr("alt") ||
      "";
    if (!title || title.length < 2) return;

    let poster = a.find("img").attr("data-src") || a.find("img").attr("src") || "";
    if (poster.startsWith("//")) poster = "https:" + poster;

    items.push({
      id: makeId("anime_shahiid", fullHref),
      title: title.trim(),
      poster: poster.startsWith("http") ? poster : "",
      pageUrl: fullHref,
      category: "أنمي مترجم",
      isSeries: true,
      providerId: "anime_hub",
      providerName: "عالم الأنمي",
    });
  });

  const seen = new Set<string>();
  return items.filter((i) => {
    if (seen.has(i.pageUrl)) return false;
    seen.add(i.pageUrl);
    return true;
  });
}

export const animeProvider: BaseProvider = {
  id: "anime_hub",
  name: "عالم الأنمي (سبيستون، مدبلج، مترجم)",
  baseUrl: BLKOM_BASE,
  isEnabled: true,

  async search(query: string) {
    const ck = `anime:s:${query}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    const [bHtml, sHtml] = await Promise.all([
      fetchHtml(`${BLKOM_BASE}/search?q=${encodeURIComponent(query)}`, `${BLKOM_BASE}/`),
      fetchHtml(`${SHAHIID_BASE}/?s=${encodeURIComponent(query)}`, `${SHAHIID_BASE}/`),
    ]);

    const items = [...parseBlkom(bHtml), ...parseShahiid(sHtml)];
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getCategory(slug, page = 1) {
    const ck = `anime:cat:${slug}:${page}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    let path = `/animes?page=${page}`;
    if (slug === "anime_dubbed") path = `/animes?dub=1&page=${page}`;
    else if (slug === "anime_subbed") path = `/animes?sub=1&page=${page}`;
    else if (slug === "anime_movie") path = `/animes?type=movie&page=${page}`;

    const html = await fetchHtml(`${BLKOM_BASE}${path}`, `${BLKOM_BASE}/`);
    const items = parseBlkom(html);
    if (items.length) cache.set(ck, { data: items, time: Date.now() });
    return items;
  },

  async getDetail(pageUrl) {
    const ck = `anime:d:${pageUrl}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    const ref = pageUrl.includes("shahiidanime") ? `${SHAHIID_BASE}/` : `${BLKOM_BASE}/`;
    const html = await fetchHtml(pageUrl, ref, 8000);
    if (!html) return null;
    const $ = cheerio.load(html);

    const title = $("h1.anime-name, h1").first().text().trim() || $('meta[property="og:title"]').attr("content") || "أنمي";
    const poster = $('meta[property="og:image"]').attr("content") || $(".anime-poster img, .poster img").first().attr("src") || "";
    const overview = $('meta[property="og:description"]').attr("content") || $(".anime-story, .story").first().text().trim() || "";
    const meta = extractMetadata($, pageUrl);

    // Parse Anime episodes
    const episodes: { title: string; pageUrl: string }[] = [];
    $(".episodes-list a, .ep-list a, .episode-item a, a[href*='/episode/'], a[href*='/ep-']").each((_, el) => {
      const href = $(el).attr("href") || "";
      const epTitle = $(el).text().trim() || `حلقة ${episodes.length + 1}`;
      if (href) {
        episodes.push({
          title: epTitle,
          pageUrl: href.startsWith("http") ? href : `${BLKOM_BASE}${href}`,
        });
      }
    });

    const servers: UnifiedServer[] = [];
    $("iframe[src]").each((i, el) => {
      const src = $(el).attr("src") || "";
      if (src.startsWith("http")) {
        servers.push({
          name: `سيرفر ${i + 1}`,
          embedUrl: src,
          providerId: "anime_hub",
          isHls: false,
        });
      }
    });

    $("[data-embed], .server-btn, .watch-btn a").each((_, el) => {
      const embedUrl = $(el).attr("data-embed") || $(el).attr("href") || "";
      if (embedUrl && embedUrl.startsWith("http") && !servers.find((s) => s.embedUrl === embedUrl)) {
        servers.push({
          name: $(el).text().trim() || `سيرفر ${servers.length + 1}`,
          embedUrl,
          providerId: "anime_hub",
          isHls: false,
        });
      }
    });

    if (servers.length === 0) {
      servers.push({ name: "سيرفر 1", embedUrl: pageUrl, providerId: "anime_hub" });
    }

    const detail: UnifiedMediaDetail = {
      item: {
        id: makeId("anime", pageUrl),
        title,
        poster,
        pageUrl,
        category: "أنمي",
        isSeries: true,
        providerId: "anime_hub",
        providerName: "عالم الأنمي",
      },
      overview,
      cast: meta.cast,
      servers,
      seasons: meta.seasons,
      episodes: meta.episodes.length ? meta.episodes : episodes.map((e) => ({ ...e, episodeNumber: "" })),
    };
    cache.set(ck, { data: detail, time: Date.now() });
    return detail;
  },

  async resolveStream(serverUrl) {
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) return serverUrl;
    const r = await extractStreamFromEmbed(serverUrl, BLKOM_BASE);
    return r?.url || serverUrl;
  },
};
