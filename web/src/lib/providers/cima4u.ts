/**
 * Cima4U Provider — https://cima4u.cc
 * Mixed Arabic/Foreign content
 */
import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";
import { extractStreamFromEmbed } from "./stream-extractor";

const BASE_URL = "https://cima4u.cc";
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
const cache = new Map<string, { data: any; time: number }>();
const CACHE_TTL = 15 * 60 * 1000;

async function fetchHtml(url: string, referer = `${BASE_URL}/`, timeoutMs = 5000): Promise<string> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": UA,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "ar,en;q=0.9",
        Referer: referer,
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

function parseListing(html: string, providerId: string, providerName: string): UnifiedMediaItem[] {
  const $ = cheerio.load(html);
  const items: UnifiedMediaItem[] = [];
  const seen = new Set<string>();

  $("article.MovieBlock, .movies-list .MovieBlock, .items article, .MovieBlock").each((_, el) => {
    const $el = $(el);
    const a = $el.find("a").first();
    const href = a.attr("href") || $el.find("a[href]").first().attr("href") || "";
    if (!href || !href.startsWith("http") || seen.has(href)) return;
    seen.add(href);

    const title =
      $el.find(".Title, h3, h2, .BlockTitle").first().text().trim() ||
      a.attr("title") || "";
    if (!title || title.length < 2) return;

    const imgEl = $el.find("img").first();
    let poster =
      imgEl.attr("data-src") || imgEl.attr("src") || imgEl.attr("data-lazy") || "";
    if (poster && !poster.startsWith("http")) poster = poster.startsWith("/") ? `${BASE_URL}${poster}` : `${BASE_URL}/${poster}`;

    const quality = $el.find(".Quality, .label, .ribbon").first().text().trim() || "HD";
    const year = $el.find(".Year, .year, .date").first().text().trim().match(/\d{4}/)?.[0] || "";
    const isSeries = href.includes("/series/") || href.includes("/مسلسل") || $el.find(".Type").text().includes("مسلسل");

    items.push({
      id: `cima4u_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
      title,
      poster,
      pageUrl: href,
      category: isSeries ? "مسلسلات سيما4يو" : "أفلام سيما4يو",
      quality,
      year,
      isSeries,
      providerId,
      providerName,
    });
  });

  return items;
}

class Cima4uProvider implements BaseProvider {
  id = "cima4u";
  name = "سيما4يو";
  baseUrl = BASE_URL;
  isEnabled = true;

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `cima4u:search:${query}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;
    try {
      const html = await fetchHtml(`${BASE_URL}/?s=${encodeURIComponent(query)}`);
      const items = parseListing(html, this.id, this.name);
      if (items.length > 0) cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch {
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `cima4u:cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    let path = "/category/افلام/";
    if (slug === "series" || slug === "arabic_series") path = "/category/مسلسلات/";
    else if (slug === "turkish_series") path = "/category/مسلسلات-تركية/";
    else if (slug === "foreign_movies" || slug === "foreign") path = "/category/افلام-اجنبية/";
    else if (slug === "arabic_movies") path = "/category/افلام-عربية/";
    else if (slug === "anime") path = "/category/انمي/";
    else if (slug === "movies") path = "/category/افلام/";

    const pageParam = page > 1 ? `page/${page}/` : "";
    try {
      const html = await fetchHtml(`${BASE_URL}${encodeURI(path)}${pageParam}`);
      const items = parseListing(html, this.id, this.name);
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
        $("h1.Title, h1, .BlockTitle").first().text().trim() ||
        $('meta[property="og:title"]').attr("content") || "عرض";
      const poster =
        $('meta[property="og:image"]').attr("content") ||
        $(".Poster img, .MovieImag img").first().attr("src") || "";
      const overview =
        $(".Story, .Description, .description, p.story").first().text().trim() ||
        $('meta[property="og:description"]').attr("content") || "";
      const isSeries = pageUrl.includes("/series/") || title.includes("مسلسل");
      const metadata = extractMediaMetadata($, pageUrl);

      const servers: UnifiedServer[] = [];
      // Extract iframes
      $("iframe[src]").each((i, el) => {
        const src = $(el).attr("src") || "";
        if (src.startsWith("http") || src.startsWith("//")) {
          const embedUrl = src.startsWith("//") ? `https:${src}` : src;
          if (!servers.some(s => s.embedUrl === embedUrl)) {
            servers.push({ name: `سيرفر ${i + 1}`, embedUrl, providerId: this.id });
          }
        }
      });
      // Extract from data-link or watch buttons
      $("a[data-link], .ServersLink a, .WatchServers a, a.server-link").each((i, el) => {
        const link = $(el).attr("data-link") || $(el).attr("href") || "";
        if (link && link.startsWith("http") && !servers.some(s => s.embedUrl === link)) {
          servers.push({ name: `سيرفر ${servers.length + 1} (${$(el).text().trim() || "بديل"})`, embedUrl: link, providerId: this.id });
        }
      });

      return {
        item: {
          id: `cima4u_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
          title, poster, pageUrl,
          category: isSeries ? "مسلسلات" : "أفلام",
          isSeries, providerId: this.id, providerName: this.name,
        },
        overview,
        cast: metadata.cast,
        servers,
        seasons: metadata.seasons,
        episodes: metadata.episodes,
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

export const cima4uProvider = new Cima4uProvider();
