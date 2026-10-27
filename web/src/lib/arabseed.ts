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
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

const listingCache = new Map<string, { items: ArabseedItem[]; time: number }>();
const detailCache = new Map<string, { data: ArabseedDetail; time: number }>();
const streamCache = new Map<string, string>();

const LISTING_TTL = 5 * 60 * 1000; // 5 mins
const DETAIL_TTL = 15 * 60 * 1000; // 15 mins

async function fetchHtml(url: string, referer = `${BASE_URL}/`): Promise<string> {
  const res = await fetch(url, {
    headers: {
      "User-Agent": UA,
      Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
      "Accept-Language": "ar,en-US;q=0.9,en;q=0.8",
      Referer: referer,
      Connection: "keep-alive",
    },
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error(`Failed to fetch HTML: ${res.status} from ${url}`);
  }
  return res.text();
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
    const url = page <= 1 ? `${BASE_URL}/%d8%a7%d9%81%d9%84%d8%a7%d9%85/` : `${BASE_URL}/%d8%a7%d9%81%d9%84%d8%a7%d9%85/page/${page}/`;
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
    const url = page <= 1 ? `${BASE_URL}/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/` : `${BASE_URL}/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/page/${page}/`;
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
    const html = await fetchHtml(pageUrl);
    const $ = cheerio.load(html);

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

    // Servers from watch page or current page
    const servers: ArabseedServer[] = [];
    let watchUrl = pageUrl.trim().replace(/\/$/, "") + "/watch";

    let watchHtml = "";
    try {
      watchHtml = await fetchHtml(watchUrl);
    } catch {
      watchHtml = html;
    }

    const $watch = cheerio.load(watchHtml);

    $watch("[data-embed], [data-url], [data-src], iframe").each((_, el) => {
      const s = $watch(el);
      const embedUrl = s.attr("data-embed") || s.attr("data-url") || s.attr("data-src") || s.attr("src") || "";
      const name = s.text().trim() || s.attr("title") || `سيرفر ${servers.length + 1}`;

      if (embedUrl && embedUrl.startsWith("http") && !servers.some((x) => x.embedUrl === embedUrl)) {
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

    // Resolve direct stream from first server if possible
    let directStream: string | undefined;
    if (servers.length > 0) {
      directStream = await resolveDirectStream(servers[0].embedUrl);
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
      directStream,
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
 * Decodes Mohix hex parameter in the embed player.
 */
export async function resolveDirectStream(serverUrl: string): Promise<string | undefined> {
  if (!serverUrl) return undefined;
  if (streamCache.has(serverUrl)) {
    return streamCache.get(serverUrl);
  }

  try {
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) {
      streamCache.set(serverUrl, serverUrl);
      return serverUrl;
    }

    if (serverUrl.includes("govid.live/play/")) {
      const html = await fetchHtml(serverUrl, `${BASE_URL}/`);
      const iframeMatch = html.match(/iframe[^>]+src=['"]([^'"]+)['"]/i);
      const embedUrl = iframeMatch ? iframeMatch[1] : undefined;

      if (embedUrl) {
        const embedHtml = await fetchHtml(embedUrl, `${GOVID_URL}/`);
        const mohixMatch = embedHtml.match(/const\s+Mohix\s*=\s*['"]([0-9a-fA-F]+)['"]/i);

        if (mohixMatch && mohixMatch[1]) {
          const direct = hexToString(mohixMatch[1]);
          if (direct && (direct.includes(".m3u8") || direct.includes(".mp4") || direct.startsWith("http"))) {
            streamCache.set(serverUrl, direct);
            return direct;
          }
        }
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
