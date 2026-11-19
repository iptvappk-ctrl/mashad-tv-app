/**
 * Turkish Content Provider
 * Aggregates from: Dizimag, Yabanci Dizi, TurkFan
 * Specializes in Turkish series (Arabic dubbed + original)
 */
import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";
import { extractStreamFromEmbed } from "./stream-extractor";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

// Primary source: Dizimag
const DIZIMAG_BASE = "https://dizimag.net";
const TURKFAN_BASE = "https://turkfan.net";

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
        "Accept-Language": "ar,tr,en;q=0.9",
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

function parseDizimagListing(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];
  const seen = new Set<string>();

  $("article, .movie-card, .series-item, .dizi-card, .content-item").each((_, el) => {
    const $el = $(el);
    const a = $el.find("a").first();
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http") || seen.has(href)) return;
    if (href.includes("/category/") || href.includes("/tag/") || href.includes("/page/")) return;
    seen.add(href);

    const title =
      $el.find("h3, h2, .title, .dizi-title, .series-name").first().text().trim() ||
      a.attr("title") || a.find("img").attr("alt") || "";
    if (!title || title.length < 2) return;

    const imgEl = $el.find("img").first();
    let poster = imgEl.attr("data-src") || imgEl.attr("src") || imgEl.attr("data-lazy") || "";
    if (poster && !poster.startsWith("http")) {
      poster = poster.startsWith("/") ? `${DIZIMAG_BASE}${poster}` : `${DIZIMAG_BASE}/${poster}`;
    }

    const year = $el.find(".year, .date").first().text().trim().match(/\d{4}/)?.[0] || "";
    const isDubbed = title.toLowerCase().includes("مدبلج") || title.toLowerCase().includes("مترجم");

    items.push({
      id: `turkish_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
      title,
      poster,
      pageUrl: href,
      category: isDubbed ? "مسلسلات تركية مدبلجة" : "مسلسلات تركية",
      year,
      isSeries: true,
      providerId: "turkish",
      providerName: "مسلسلات تركية",
    });
  });

  return items;
}

function parseTurkfanListing(html: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];
  const seen = new Set<string>();

  $("article, .movie-block, .content-block, .item-block").each((_, el) => {
    const $el = $(el);
    const a = $el.find("a").first();
    const href = a.attr("href") || "";
    if (!href || !href.startsWith("http") || seen.has(href)) return;
    if (href.includes("/category/") || href.includes("/tag/") || href.includes("/page/")) return;
    seen.add(href);

    const title =
      $el.find("h3, h2, .title").first().text().trim() ||
      a.attr("title") || a.find("img").attr("alt") || "";
    if (!title || title.length < 2) return;

    const imgEl = $el.find("img").first();
    let poster = imgEl.attr("data-src") || imgEl.attr("src") || "";
    if (poster && !poster.startsWith("http")) {
      poster = poster.startsWith("/") ? `${TURKFAN_BASE}${poster}` : `${TURKFAN_BASE}/${poster}`;
    }

    const year = $el.find(".year, .date").first().text().trim().match(/\d{4}/)?.[0] || "";

    items.push({
      id: `turkish_tf_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
      title,
      poster,
      pageUrl: href,
      category: "مسلسلات تركية",
      year,
      isSeries: true,
      providerId: "turkish",
      providerName: "مسلسلات تركية",
    });
  });

  return items;
}

class TurkishProvider implements BaseProvider {
  id = "turkish";
  name = "مسلسلات تركية";
  baseUrl = DIZIMAG_BASE;
  isEnabled = true;

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `turkish:search:${query}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;
    try {
      const [html1, html2] = await Promise.allSettled([
        fetchHtml(`${DIZIMAG_BASE}/?s=${encodeURIComponent(query)}`),
        fetchHtml(`${TURKFAN_BASE}/?s=${encodeURIComponent(query)}`),
      ]);
      const items: UnifiedMediaItem[] = [];
      if (html1.status === "fulfilled" && html1.value) items.push(...parseDizimagListing(html1.value));
      if (html2.status === "fulfilled" && html2.value) items.push(...parseTurkfanListing(html2.value));
      const seen = new Set<string>();
      const unique = items.filter(i => { if (seen.has(i.pageUrl)) return false; seen.add(i.pageUrl); return true; });
      if (unique.length > 0) cache.set(cacheKey, { data: unique, time: Date.now() });
      return unique;
    } catch {
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `turkish:cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    const pageParam = page > 1 ? `page/${page}/` : "";
    let url = `${DIZIMAG_BASE}/${pageParam}`;
    if (slug === "turkish_movies") url = `${DIZIMAG_BASE}/turkish-movies/${pageParam}`;
    else if (slug === "arabic_dubbed_turkish") url = `${DIZIMAG_BASE}/arabic-dubbed/${pageParam}`;

    try {
      const html = await fetchHtml(url);
      const items = parseDizimagListing(html);
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
        $("h1, .dizi-title, .series-title, .Title").first().text().trim() ||
        $('meta[property="og:title"]').attr("content") || "مسلسل تركي";
      const poster =
        $('meta[property="og:image"]').attr("content") ||
        $(".poster img, .dizi-poster img, .thumbnail img").first().attr("src") || "";
      const overview =
        $(".description, .overview, .story, .dizi-desc").first().text().trim() ||
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
      $("a[data-episode], .episode-btn, .server-link, a[data-link]").each((i, el) => {
        const link = $(el).attr("data-link") || $(el).attr("data-episode") || $(el).attr("href") || "";
        if (!link || !link.startsWith("http")) return;
        if (!servers.some(s => s.embedUrl === link)) {
          servers.push({ name: `سيرفر ${servers.length + 1} (${$(el).text().trim() || "بديل"})`, embedUrl: link, providerId: this.id });
        }
      });

      // Episodes
      const episodes = metadata.episodes;
      if (episodes.length === 0) {
        $("a.episode, .episodes-list a, .ep-link, a[href*='episode']").each((_, el) => {
          const href = $(el).attr("href") || "";
          const epTitle = $(el).text().trim() || `حلقة ${episodes.length + 1}`;
          if (href && href.startsWith("http") && !episodes.some(e => e.pageUrl === href)) {
            episodes.push({ title: epTitle, pageUrl: href, episodeNumber: `${episodes.length + 1}` });
          }
        });
      }

      return {
        item: {
          id: `turkish_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
          title, poster, pageUrl,
          category: "مسلسلات تركية",
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

export const turkishProvider = new TurkishProvider();
