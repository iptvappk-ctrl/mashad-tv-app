import * as cheerio from "cheerio";

export interface ArabseedItem {
  id: string;
  title: string;
  poster: string;
  pageUrl: string;
  category: string;
  quality?: string;
  year?: string;
  rating?: string;
  isSeries: boolean;
}

export interface ArabseedServer {
  name: string;
  embedUrl: string;
}

export interface ArabseedEpisode {
  title: string;
  pageUrl: string;
  episodeNumber?: string;
}

export interface ArabseedSeason {
  title: string;
  pageUrl: string;
  seasonNumber?: string;
}

export interface ArabseedDetail {
  item: ArabseedItem;
  overview: string;
  servers: ArabseedServer[];
  seasons: ArabseedSeason[];
  episodes: ArabseedEpisode[];
  directStream?: string;
  proxiedStream?: string;
}

const BASE_URL = "https://arabseed.health";
const GOVID_URL = "https://govid.live";
const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

// High performance in-memory caching
const listingCache = new Map<string, { items: ArabseedItem[]; time: number }>();
const detailCache = new Map<string, { data: ArabseedDetail; time: number }>();
const streamCache = new Map<string, { url: string; time: number }>();

const LISTING_TTL = 10 * 60 * 1000; // 10 mins
const DETAIL_TTL = 30 * 60 * 1000; // 30 mins
const STREAM_TTL = 2 * 60 * 60 * 1000; // 2 hours

async function fetchHtml(url: string, referer = `${BASE_URL}/`, timeoutMs = 6000): Promise<string> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": UA,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language": "ar,en-US;q=0.9,en;q=0.8",
        Referer: referer,
        Connection: "keep-alive",
      },
      signal: controller.signal,
      cache: "no-store",
    });
    if (!res.ok) {
      throw new Error(`Failed to fetch HTML: ${res.status} from ${url}`);
    }
    return await res.text();
  } finally {
    clearTimeout(timeoutId);
  }
}

function parseListing(html: string): ArabseedItem[] {
  const $ = cheerio.load(html);
  const items: ArabseedItem[] = [];

  const elements = $(
    "a.movie__block, .item__contents a, .movie-item a, article a, .postDiv a, .MovieBlock a, .Block--Item a, a.BlockItem, .Movie--Block a, .item a, .Movie--Block, .Film--Block, .Series--Block, .BlockItem"
  );

  elements.each((_, el) => {
    const a = $(el);
    const linkEl = a.is("a") ? a : a.find("a").first();
    const href = linkEl.attr("href") || a.attr("href") || "";

    if (!href || !href.startsWith("http")) return;
    if (
      href === `${BASE_URL}/` ||
      href === BASE_URL ||
      href.includes("/category/") ||
      href.includes("/tag/") ||
      href.endsWith("/home/")
    )
      return;

    const rawTitle =
      a.find("h3, h2, h4, .Title, .title, .EntryTitle").first().text().trim() ||
      linkEl.attr("title") ||
      a.find("img").attr("alt") ||
      "";

    if (!rawTitle || rawTitle.length < 2) return;

    const img = a.find("img");
    const poster =
      img.attr("data-src") ||
      img.attr("src") ||
      img.attr("data-lazy-src") ||
      "";

    const quality =
      a.find(".Quality, .quality, .badge-quality, .__quality").first().text().trim() || "";
    const year = a.find(".Year, .year").first().text().trim() || "";
    const rating = a.find(".Rating, .rating, .imdb").first().text().trim() || "";
    const category =
      a.find(".Category, .category, .post__category").first().text().trim() || "";

    const isSeries =
      href.includes("مسلسل") ||
      href.includes("series") ||
      rawTitle.includes("مسلسل") ||
      rawTitle.includes("الموسم");

    items.push({
      id: Buffer.from(href).toString("base64url").slice(0, 16),
      title: rawTitle,
      poster,
      pageUrl: href,
      category: category || (isSeries ? "مسلسلات" : "أفلام"),
      quality,
      year,
      rating,
      isSeries,
    });
  });

  const seen = new Set<string>();
  return items.filter((it) => {
    if (seen.has(it.pageUrl)) return false;
    seen.add(it.pageUrl);
    return true;
  });
}

export async function getArabseedMovies(page = 1, force = false): Promise<ArabseedItem[]> {
  const cacheKey = `movies:${page}`;
  const now = Date.now();
  const cached = listingCache.get(cacheKey);
  if (!force && cached && now - cached.time < LISTING_TTL) {
    return cached.items;
  }

  try {
    const url =
      page <= 1
        ? `${BASE_URL}/%d8%a7%d9%81%d9%84%d8%a7%d9%85/`
        : `${BASE_URL}/%d8%a7%d9%81%d9%84%d8%a7%d9%85/page/${page}/`;
    const html = await fetchHtml(url);
    const items = parseListing(html);
    if (items.length > 0) {
      listingCache.set(cacheKey, { items, time: now });
    }
    return items;
  } catch (err) {
    console.error("Arabseed movies error:", err);
    return cached ? cached.items : [];
  }
}

export async function getArabseedSeries(page = 1, force = false): Promise<ArabseedItem[]> {
  const cacheKey = `series:${page}`;
  const now = Date.now();
  const cached = listingCache.get(cacheKey);
  if (!force && cached && now - cached.time < LISTING_TTL) {
    return cached.items;
  }

  try {
    const url =
      page <= 1
        ? `${BASE_URL}/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/`
        : `${BASE_URL}/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/page/${page}/`;
    const html = await fetchHtml(url);
    const items = parseListing(html);
    if (items.length > 0) {
      listingCache.set(cacheKey, { items, time: now });
    }
    return items;
  } catch (err) {
    console.error("Arabseed series error:", err);
    return cached ? cached.items : [];
  }
}

export async function searchArabseed(query: string): Promise<ArabseedItem[]> {
  try {
    const encoded = encodeURIComponent(query.trim());
    const url = `${BASE_URL}/?s=${encoded}`;
    const html = await fetchHtml(url);
    return parseListing(html);
  } catch (err) {
    console.error("Arabseed search error:", err);
    return [];
  }
}

export async function getArabseedDetail(pageUrl: string, force = false): Promise<ArabseedDetail | null> {
  const now = Date.now();
  const cached = detailCache.get(pageUrl);
  if (!force && cached && now - cached.time < DETAIL_TTL) {
    return cached.data;
  }

  try {
    const cleanUrl = pageUrl.trim().replace(/\/$/, "");
    const watchUrl = `${cleanUrl}/watch`;

    // Fetch detail page and watch page IN PARALLEL for maximum speed
    const [pageHtml, watchHtml] = await Promise.all([
      fetchHtml(cleanUrl + "/"),
      fetchHtml(watchUrl, cleanUrl + "/").catch(() => ""),
    ]);

    const $ = cheerio.load(pageHtml);

    // Title & basic info
    const title = $("h1, .EntryTitle, .Title").first().text().trim();
    const poster =
      $("img.Poster, .Single--Poster img, .Poster img").attr("src") ||
      $("img.Poster, .Single--Poster img, .Poster img").attr("data-src") ||
      "";

    // Overview / Story
    const overview =
      $(".post__story, .story, .Story, .entry__desc, div.desc, .Overview, p.Story")
        .text()
        .trim() || "لا يوجد وصف متوفر.";

    // Parse Watch Servers from both watchHtml and pageHtml
    const servers: ArabseedServer[] = [];
    const sourceHtml = watchHtml || pageHtml;
    const $watch = cheerio.load(sourceHtml);

    // 1. Parse server items from #watch li, ul.servers li with data-link
    $watch(
      "#watch li, ul.servers li, .watch-servers li, #servers li, [data-link], [data-embed], [data-url], iframe#videoIframe, iframe"
    ).each((_, el) => {
      const s = $watch(el);
      const embedUrl =
        s.attr("data-link") ||
        s.attr("data-embed") ||
        s.attr("data-url") ||
        s.attr("data-server") ||
        s.attr("src") ||
        "";
      let name = s.text().trim() || s.attr("title") || "";

      if (embedUrl && embedUrl.startsWith("http") && !servers.some((x) => x.embedUrl === embedUrl)) {
        if (!name || name === "undefined") {
          name = `سيرفر ${servers.length + 1}`;
        }
        servers.push({ name, embedUrl });
      }
    });

    // Check for Seasons & Episodes
    const seasons: ArabseedSeason[] = [];
    $(".Seasons--List a, .seasons-list a, .SeasonBlock a").each((_, el) => {
      const a = $(el);
      const sUrl = a.attr("href");
      if (sUrl) {
        seasons.push({
          title: a.text().trim() || `الموسم ${seasons.length + 1}`,
          pageUrl: sUrl,
          seasonNumber: `${seasons.length + 1}`,
        });
      }
    });

    const episodes: ArabseedEpisode[] = [];
    $(".Episodes--List a, .episodes-list a, .EpisodeBlock a, .EpBlock a").each((_, el) => {
      const a = $(el);
      const epUrl = a.attr("href");
      if (epUrl) {
        episodes.push({
          title: a.text().trim() || `الحلقة ${episodes.length + 1}`,
          pageUrl: epUrl,
          episodeNumber: `${episodes.length + 1}`,
        });
      }
    });

    // Background pre-resolve the 1st server without blocking the detail response!
    if (servers.length > 0) {
      resolveDirectStream(servers[0].embedUrl).catch(() => {});
    }

    const detail: ArabseedDetail = {
      item: {
        id: Buffer.from(pageUrl).toString("base64url").slice(0, 16),
        title: title || "مشهد فيديو",
        poster,
        pageUrl,
        category: seasons.length > 0 || episodes.length > 0 ? "مسلسلات" : "أفلام",
        isSeries: seasons.length > 0 || episodes.length > 0,
      },
      overview,
      servers,
      seasons,
      episodes,
    };

    detailCache.set(pageUrl, { data: detail, time: now });
    return detail;
  } catch (err) {
    console.error("Arabseed detail error:", err);
    return null;
  }
}

/**
 * Resolves direct video stream link (.m3u8 or .mp4) from embed servers (Govid etc.)
 * High performance resolution with caching.
 */
export async function resolveDirectStream(serverUrl: string): Promise<string | undefined> {
  if (!serverUrl) return undefined;

  const now = Date.now();
  const cached = streamCache.get(serverUrl);
  if (cached && now - cached.time < STREAM_TTL) {
    return cached.url;
  }

  try {
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) {
      streamCache.set(serverUrl, { url: serverUrl, time: now });
      return serverUrl;
    }

    // Govid.live resolver
    if (serverUrl.includes("govid.live")) {
      let embedPageUrl = serverUrl;

      // If it's a /play/ URL, fetch play page to find the real /e/ iframe
      if (serverUrl.includes("/play/")) {
        const html = await fetchHtml(serverUrl, `${BASE_URL}/`, 4000);
        const iframeMatch = html.match(/iframe[^>]+src=['"]([^'"]+)['"]/i);
        if (iframeMatch && iframeMatch[1]) {
          embedPageUrl = iframeMatch[1];
        }
      }

      // Now fetch the embed player page which contains the hex-encoded stream or Mohix variable
      const embedHtml = await fetchHtml(embedPageUrl, `${GOVID_URL}/`, 4000);

      // Check 1: Mohix hex variable
      const mohixMatch = embedHtml.match(/const\s+Mohix\s*=\s*['"]([0-9a-fA-F]+)['"]/i);
      if (mohixMatch && mohixMatch[1]) {
        const direct = hexToString(mohixMatch[1]);
        if (direct && (direct.includes(".m3u8") || direct.includes(".mp4") || direct.startsWith("http"))) {
          streamCache.set(serverUrl, { url: direct, time: now });
          return direct;
        }
      }

      // Check 2: Direct m3u8 or mp4 in embed HTML
      const m3u8Match = embedHtml.match(/https?:\/\/[^"']+\.m3u8[^"']*/i);
      if (m3u8Match) {
        streamCache.set(serverUrl, { url: m3u8Match[0], time: now });
        return m3u8Match[0];
      }

      const mp4Match = embedHtml.match(/https?:\/\/[^"']+\.mp4[^"']*/i);
      if (mp4Match) {
        streamCache.set(serverUrl, { url: mp4Match[0], time: now });
        return mp4Match[0];
      }
    }
  } catch (err) {
    console.error(`Failed to resolve stream for ${serverUrl}:`, err);
  }

  return undefined;
}

function hexToString(hex: string): string {
  let str = "";
  for (let i = 0; i < hex.length; i += 2) {
    const code = parseInt(hex.substring(i, i + 2), 16);
    if (!isNaN(code)) {
      str += String.fromCharCode(code);
    }
  }
  return str;
}
