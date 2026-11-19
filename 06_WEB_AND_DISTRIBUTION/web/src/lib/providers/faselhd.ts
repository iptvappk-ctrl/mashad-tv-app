import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractMediaMetadata } from "./metadata";

const BASE_URL = "https://www.faselhd.club";
const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

const cache = new Map<string, { data: any; time: number }>();
const CACHE_TTL = 15 * 60 * 1000;

async function fetchHtml(url: string, referer = `${BASE_URL}/`, timeoutMs = 4000): Promise<string> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": UA,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        Referer: referer,
      },
      signal: controller.signal,
    });
    return await res.text();
  } finally {
    clearTimeout(timeoutId);
  }
}

export class FaselHDProvider implements BaseProvider {
  id = "faselhd";
  name = "فاصل إعلاني";
  baseUrl = BASE_URL;
  isEnabled = true;

  parseListing(html: string): UnifiedMediaItem[] {
    const $ = cheerio.load(html);
    const items: UnifiedMediaItem[] = [];

    $('a[href*="/movie/"], a[href*="/serie/"]').each((_, el) => {
      const a = $(el);
      const href = a.attr("href") || "";
      if (!href) return;

      const title =
        a.find("img").attr("alt") ||
        a.find(".title, h2, h3").text().trim() ||
        a.text().trim().replace(/\s+/g, " ");

      if (!title || title.length < 3) return;

      const poster =
        a.find("img").attr("src") ||
        a.find("img").attr("data-src") ||
        "";

      const isSeries = href.includes("/serie/");

      items.push({
        id: `fasel_${Buffer.from(href).toString("base64url").slice(0, 16)}`,
        title,
        poster,
        pageUrl: href.startsWith("http") ? href : `${BASE_URL}${href}`,
        category: isSeries ? "مسلسلات فاصل" : "أفلام فاصل",
        isSeries,
        providerId: this.id,
        providerName: this.name,
      });
    });

    const seen = new Set<string>();
    return items.filter((it) => {
      if (seen.has(it.pageUrl)) return false;
      seen.add(it.pageUrl);
      return true;
    });
  }

  async search(query: string): Promise<UnifiedMediaItem[]> {
    const cacheKey = `search:${query}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    try {
      const url = `${BASE_URL}/search?q=${encodeURIComponent(query.trim())}`;
      const html = await fetchHtml(url);
      const items = this.parseListing(html);
      cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch (err) {
      console.error("[FaselHDProvider] search error:", err);
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    const cacheKey = `cat:${slug}:${page}`;
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.time < CACHE_TTL) return cached.data;

    try {
      let path = "/movies";
      if (slug === "series" || slug.includes("series")) {
        path = "/series";
      } else if (slug === "action") {
        path = "/categories?q=action";
      } else if (slug === "horror") {
        path = "/categories?q=horror";
      }

      const url = page <= 1 ? `${BASE_URL}${path}` : `${BASE_URL}${path}?page=${page}`;
      const html = await fetchHtml(url);
      const items = this.parseListing(html);
      cache.set(cacheKey, { data: items, time: Date.now() });
      return items;
    } catch (err) {
      console.error(`[FaselHDProvider] getCategory error (${slug}):`, err);
      return [];
    }
  }

  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    try {
      const html = await fetchHtml(pageUrl);
      const $ = cheerio.load(html);

      const title =
        $("h1").first().text().trim() ||
        $('meta[property="og:title"]').attr("content") ||
        "عرض فاصل إعلاني";

      // Try parsing JSON-LD schema for accurate poster & story
      let poster = "";
      let overview = "مشاهدة وتحميل بجودة فائقة Full HD من سيرفرات فاصل إعلاني.";

      try {
        const jsonLd = $('script[type="application/ld+json"]').html();
        if (jsonLd) {
          const parsed = JSON.parse(jsonLd);
          if (parsed.image) poster = parsed.image;
          if (parsed.description) overview = parsed.description;
        }
      } catch {}

      if (!poster) {
        poster = $('img.poster, .poster img, img[src*="cover"]').attr("src") || "";
      }

      const isSeries = pageUrl.includes("/serie/");
      const metadata = extractMediaMetadata($, pageUrl);
      const servers: UnifiedServer[] = [];

      // Extract server buttons with data-embed
      const embedButtons: { id: string; name: string }[] = [];
      $(".btn-service[data-embed]").each((_, el) => {
        const embedId = $(el).attr("data-embed");
        const sName = $(el).find(".name").text().trim() || $(el).text().trim() || "سيرفر";
        if (embedId) {
          embedButtons.push({ id: embedId, name: sName });
        }
      });

      // Fetch primary server via AJAX POST
      if (embedButtons.length > 0) {
        const first = embedButtons[0];
        try {
          const ajaxRes = await fetch(`${BASE_URL}/ajax/embed`, {
            method: "POST",
            headers: {
              "User-Agent": UA,
              Referer: pageUrl,
              "X-Requested-With": "XMLHttpRequest",
              "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            },
            body: `id=${encodeURIComponent(first.id)}`,
          });
          const ajaxText = await ajaxRes.text();
          const iframeMatch = ajaxText.match(/src=["']([^"']+)["']/i);
          if (iframeMatch && iframeMatch[1]) {
            servers.push({
              name: `سيرفر 1 (${first.name})`,
              embedUrl: iframeMatch[1],
              providerId: this.id,
              isHls: true,
            });
          }
        } catch {}

        // Add remaining servers as lazy resolvable
        for (let i = 1; i < embedButtons.length; i++) {
          const s = embedButtons[i];
          servers.push({
            name: `سيرفر ${i + 1} (${s.name})`,
            embedUrl: `${BASE_URL}/ajax/embed?id=${s.id}`,
            providerId: this.id,
            isHls: false,
          });
        }
      }

      if (servers.length === 0) {
        servers.push({
          name: "سيرفر 1 (مباشر)",
          embedUrl: pageUrl,
          providerId: this.id,
          isHls: false,
        });
      }

      return {
        item: {
          id: `fasel_${Buffer.from(pageUrl).toString("base64url").slice(0, 16)}`,
          title,
          poster,
          pageUrl,
          category: isSeries ? "مسلسلات فاصل" : "أفلام فاصل",
          isSeries,
          providerId: this.id,
          providerName: this.name,
        },
        overview,
        cast: metadata.cast,
        servers,
        seasons: metadata.seasons,
        episodes: metadata.episodes,
      };
    } catch (err) {
      console.error("[FaselHDProvider] getDetail error:", err);
      return null;
    }
  }

  async resolveStream(serverUrl: string): Promise<string | null> {
    if (serverUrl.includes("/ajax/embed?id=")) {
      try {
        const id = new URL(serverUrl).searchParams.get("id");
        if (id) {
          const res = await fetch(`${BASE_URL}/ajax/embed`, {
            method: "POST",
            headers: {
              "User-Agent": UA,
              Referer: `${BASE_URL}/`,
              "X-Requested-With": "XMLHttpRequest",
              "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            },
            body: `id=${encodeURIComponent(id)}`,
          });
          const text = await res.text();
          const iframeMatch = text.match(/src=["']([^"']+)["']/i);
          if (iframeMatch && iframeMatch[1]) {
            return iframeMatch[1];
          }
        }
      } catch {}
    }
    return serverUrl;
  }
}

export const faselHDProvider = new FaselHDProvider();
