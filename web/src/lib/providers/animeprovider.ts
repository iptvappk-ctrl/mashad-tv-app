/**
 * Anime Provider
 * Sources: AnimeBlkom (Arabic dubbed), Shahiid Anime
 * Categories: shounen, shojo, isekai, action
 */
import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";
import { extractStreamFromEmbed } from "./stream-extractor";

const ANIMEBLKOM_BASE = "https://animeblkom.net";
const SHAHIID_BASE = "https://shahiidanime.net";
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

const cache = new Map<string, { data: any; time: number }>();
const CACHE_TTL = 15 * 60 * 1000;

async function fetchHtml(url: string, referer?: string, timeoutMs = 5000): Promise<string> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": UA,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "ar,en;q=0.9",
        Referer: referer || url,
      },
      signal: controller.signal,
    });
    return res.ok ? await res.text() : "";
  } catch {
    return "";
  } finally {
    clearTimeout(timeoutId);
  }
}

function parseAnimeBlkomListing(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];
  const seen = new Set<string>();

  $(".anime-card, article, .content-box, .AnimeBlock, .show-card").each((_, el) => {
    const $el = $(el);
    const a = $el.find("a").first();
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http") || seen.has(href)) return;
    if (href.includes("/category/") || href.includes("/tag/") || href.includes("/page/")) return;
    seen.add(href);

    const title =
      $el.find("h3, h2, .title, .anime-title, .AnimeTitle").first().text().trim() ||
      a.attr("title") || a.find("img").attr("alt") || "";
    if (!title || title.length < 2) return;

    const imgEl = $el.find("img").first();
    let poster = imgEl.attr("data-src") || imgEl.attr("src") || imgEl.attr("data-lazy") || "";
    if (poster && !poster.startsWith("http")) {
      poster = poster.startsWith("/") ? `${ANIMEBLKOM_BASE}${poster}` : `${ANIMEBLKOM_BASE}/${poster}`;
    }

    const year = $el.find(".year, .date, .Year").first().text().trim().match(/\d{4}/)?.[0] || "";
    const rating = $el.find(".rating, .score, .Rate").first().text().trim() || "";
    const isSeries = !href.includes("/movies/") && !href.includes("/افلام/");

    items.push({
      id: `anime_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
      title,
      poster,
      pageUrl: href,
      category: "أنمي",
      year,
      rating,
      isSeries,
      providerId: "anime",
      providerName: "أنمي",
    });
  });

  return items;
}

function parseShahiidListing(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];
  const seen = new Set<string>();

  $("article, .movie-card, .anime-item, .show-block").each((_, el) => {
    const $el = $(el);
    const a = $el.find("a").first();
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http") || seen.has(href)) return;
    if (href.includes("/category/") || href.includes("/tag/") || href.includes("/page/")) return;
    seen.add(href);

    const title =
      $el.find("h3, h2, .title, .anime-name").first().text().trim() ||
      a.attr("title") || a.find("img").attr("alt") || "";
    if (!title || title.length < 2) return;

    const imgEl = $el.find("img").first();
    let poster = imgEl.attr("data-src") || imgEl.attr("src") || "";
    if (poster && !poster.startsWith("http")) {
      poster = poster.startsWith("/") ? `${SHAHIID_BASE}${poster}` : `${SHAHIID_BASE}/${poster}`;
    }

    const year = $el.find(".year, .date").first().text().trim().match(/\d{4}/)?.[0] || "";

    items.push({
      id: `anime_sh_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
      title,
      poster,
      pageUrl: href,
      category: "أنمي",
      year,
      isSeries: true,
      providerId: "anime",
      providerName: "أنمي",
    });
  });

  return items;
}

class AnimeProvider implements BaseProvider {
  id = "anime";
  name = "أنمي";
  baseUrl = ANIMEBLKOM_BASE;
  isEnabled = true;

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `anime:search:${query}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;
    try {
      const [r1, r2] = await Promise.allSettled([
        fetchHtml(`${ANIMEBLKOM_BASE}/?s=${encodeURIComponent(query)}`),
        fetchHtml(`${SHAHIID_BASE}/?s=${encodeURIComponent(query)}`),
      ]);
      const items: UnifiedMediaItem[] = [];
      if (r1.status === "fulfilled" && r1.value) items.push(...parseAnimeBlkomListing(r1.value));
      if (r2.status === "fulfilled" && r2.value) items.push(...parseShahiidListing(r2.value));
      const seen = new Set<string>();
      const unique = items.filter(i => { if (seen.has(i.pageUrl)) return false; seen.add(i.pageUrl); return true; });
      if (unique.length > 0) cache.set(cacheKey, { data: unique, time: Date.now() });
      return unique;
    } catch {
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `anime:cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    const pageParam = page > 1 ? `page/${page}/` : "";
    let url = `${ANIMEBLKOM_BASE}/${pageParam}`;
    if (slug === "anime_action" || slug === "action") url = `${ANIMEBLKOM_BASE}/category/action/${pageParam}`;
    else if (slug === "anime_shounen" || slug === "shounen") url = `${ANIMEBLKOM_BASE}/category/shounen/${pageParam}`;
    else if (slug === "anime_isekai" || slug === "isekai") url = `${ANIMEBLKOM_BASE}/category/isekai/${pageParam}`;
    else if (slug === "anime_shojo" || slug === "shojo") url = `${ANIMEBLKOM_BASE}/category/shojo/${pageParam}`;
    else if (slug === "anime_movies") url = `${ANIMEBLKOM_BASE}/anime-movies/${pageParam}`;
    else if (slug === "anime_dubbed" || slug === "arabic_dubbed") url = `${ANIMEBLKOM_BASE}/dubbed/${pageParam}`;

    try {
      const html = await fetchHtml(url);
      const items = parseAnimeBlkomListing(html);
      if (items.length > 0) cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch {
      return [];
    }
  }

  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    try {
      const html = await fetchHtml(pageUrl);
      if (!html) return null;
      const $ = cheerio.load(html);

      const title =
        $("h1, .anime-title, .AnimeTitle, .Title").first().text().trim() ||
        $('meta[property="og:title"]').attr("content") || "أنمي";
      const poster =
        $('meta[property="og:image"]').attr("content") ||
        $(".anime-poster img, .poster img, .thumbnail img").first().attr("src") || "";
      const overview =
        $(".description, .overview, .story, .anime-desc, .AnimeDesc").first().text().trim() ||
        $('meta[property="og:description"]').attr("content") || "";
      const metadata = extractMediaMetadata($, pageUrl);

      const servers: UnifiedServer[] = [];
      $("iframe[src]").each((i, el) => {
        const src = $(el).attr("src") || "";
        if (!src) return;
        const embedUrl = src.startsWith("//") ? `https:${src}` : src;
        if (!embedUrl.startsWith("http")) return;
        if (!servers.some(s => s.embedUrl === embedUrl)) {
          servers.push({ name: `سيرفر ${i + 1}`, embedUrl, providerId: this.id });
        }
      });
      $("a.server-btn, .watch-servers a, [data-src], a[data-embed]").each((i, el) => {
        const link = $(el).attr("data-src") || $(el).attr("data-embed") || $(el).attr("href") || "";
        if (!link || !link.startsWith("http")) return;
        if (!servers.some(s => s.embedUrl === link)) {
          servers.push({ name: `سيرفر ${servers.length + 1}`, embedUrl: link, providerId: this.id });
        }
      });

      // Episode list extraction
      const episodes = metadata.episodes;
      if (episodes.length === 0) {
        $("a.ep-link, .episodes-list a, a[href*='episode'], a[href*='الحلقة'], .EpisodesList a").each((_, el) => {
          const href = $(el).attr("href") || "";
          const epTitle = $(el).text().trim() || `حلقة ${episodes.length + 1}`;
          if (href && href.startsWith("http") && !episodes.some(e => e.pageUrl === href)) {
            const numMatch = epTitle.match(/(\d+)/);
            episodes.push({ title: epTitle, pageUrl: href, episodeNumber: numMatch?.[1] || `${episodes.length + 1}` });
          }
        });
      }

      return {
        item: {
          id: `anime_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
          title, poster, pageUrl,
          category: "أنمي",
          isSeries: true, providerId: this.id, providerName: this.name,
        },
        overview,
        cast: metadata.cast,
        servers,
        seasons: metadata.seasons,
        episodes,
        proxiedStream: servers[0]?.embedUrl,
      };
    } catch {
      return null;
    }
  }

  async resolveStream(serverUrl: string): Promise<string | null> {
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) return serverUrl;
    try {
      const extracted = await extractStreamFromEmbed(serverUrl);
      return extracted?.url || serverUrl;
    } catch {
      return serverUrl;
    }
  }
}

export const animeProvider = new AnimeProvider();
