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
    "a.movie__block, .item__contents a, .movie-item a, article a, .postDiv a, .MovieBlock a, .Block--Item a, a.BlockItem, .Movie--Block a, .item a, .Movie--Block, .Film--Block, .Series--Block, .BlockItem, a.episode__item"
  );

  elements.each((_, el) => {
    const a = $(el);
    const linkEl = a.is("a") ? a : a.find("a").first();
    const href = linkEl.attr("href") || a.attr("href") || "";

    if (!href || !href.startsWith("http")) return;
    if (
      href === `${BASE_URL}/` ||
      href === BASE_URL ||
      href.endsWith("/home/")
    )
      return;

    const rawTitle =
      a.find("h3, h2, h4, .Title, .title, .EntryTitle").first().text().trim() ||
      linkEl.attr("title") ||
      a.find("img").attr("alt") ||
      a.text().trim() ||
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
      rawTitle.includes("الموسم") ||
      rawTitle.includes("الحلقة");

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

// Category mappings for Arabseed
const CATEGORY_MAP: Record<string, string> = {
  movies: "/%d8%a7%d9%81%d9%84%d8%a7%d9%85/",
  series: "/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/",
  foreign_movies: "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a/",
  arabic_movies: "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%b9%d8%b1%d8%a8%d9%8a/",
  turkish_movies: "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%aa%d8%b1%d9%83%d9%8a%d8%a9/",
  asian_movies: "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a%d8%a9/",
  indian_movies: "/category/%d8%a7%d9%81%d9%84%d8%a7%d9%85-%d9%87%d9%86%d8%af%d9%8a/",
  foreign_series: "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d8%ac%d9%86%d8%a8%d9%8a/",
  arabic_series: "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b9%d8%b1%d8%a8%d9%8a/",
  turkish_series: "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%aa%d8%b1%d9%83%d9%8a%d8%a9/",
  asian_series: "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d8%b3%d9%8a%d9%88%d9%8a%d8%a9/",
  ramadan: "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b1%d9%85%d8%b6%d8%a7%d9%86-2026/",
  anime: "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%a7%d9%86%d9%85%d9%8a/",
  wrestling: "/category/%d8%b9%d8%b1%d9%88%d8%b6-%d9%85%d8%b5%d8%a7%d8%b1%d8%b9%d8%a9/",
};

const GENRE_SEARCH_MAP: Record<string, string> = {
  action: "اكشن",
  horror: "رعب",
  comedy: "كوميدي",
  romance: "رومانسي",
  scifi: "خيال علمي",
};

export async function getArabseedCategory(slug: string, page = 1, force = false): Promise<ArabseedItem[]> {
  const cacheKey = `cat:${slug}:${page}`;
  const now = Date.now();
  const cached = listingCache.get(cacheKey);
  if (!force && cached && now - cached.time < LISTING_TTL) {
    return cached.items;
  }

  try {
    let url: string;
    if (GENRE_SEARCH_MAP[slug]) {
      const q = encodeURIComponent(GENRE_SEARCH_MAP[slug]);
      url = page <= 1 ? `${BASE_URL}/?s=${q}` : `${BASE_URL}/page/${page}/?s=${q}`;
    } else {
      const path = CATEGORY_MAP[slug] || CATEGORY_MAP["movies"];
      const cleanPath = path.replace(/\/$/, "");
      url = page <= 1 ? `${BASE_URL}${cleanPath}/` : `${BASE_URL}${cleanPath}/page/${page}/`;
    }

    const html = await fetchHtml(url);
    const items = parseListing(html);
    if (items.length > 0) {
      listingCache.set(cacheKey, { items, time: now });
    }
    return items;
  } catch (err) {
    console.error(`Arabseed category error (${slug}):`, err);
    return cached ? cached.items : [];
  }
}

export async function getArabseedMovies(page = 1, force = false): Promise<ArabseedItem[]> {
  return getArabseedCategory("movies", page, force);
}

export async function getArabseedSeries(page = 1, force = false): Promise<ArabseedItem[]> {
  return getArabseedCategory("series", page, force);
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

    // Parse Watch Servers
    const servers: ArabseedServer[] = [];
    const sourceHtml = watchHtml || pageHtml;
    const $watch = cheerio.load(sourceHtml);

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

    // Check for Seasons
    const seasons: ArabseedSeason[] = [];
    $("a[href*='/series/'], .Seasons--List a, .seasons-list a, .SeasonBlock a").each((_, el) => {
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

    // Check for Episodes (Real Arabseed selectors!)
    const episodes: ArabseedEpisode[] = [];
    $("a.episode__item, .episode__item a, a[href*='/%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9-'], .Episodes--List a, .EpisodeBlock a").each((_, el) => {
      const a = $(el);
      const epUrl = a.attr("href");
      if (epUrl && !episodes.some((e) => e.pageUrl === epUrl)) {
        let epTitle = a.find(".title, h3, span").text().trim() || a.text().trim();
        // Extract "الحلقة X" if text contains it
        const match = epTitle.match(/الحلقة\s*([0-9]+)/i);
        if (match) {
          epTitle = `الحلقة ${match[1]}`;
        }
        episodes.push({
          title: epTitle || `الحلقة ${episodes.length + 1}`,
          pageUrl: epUrl,
          episodeNumber: `${episodes.length + 1}`,
        });
      }
    });

    // Background pre-resolve 1st server
    if (servers.length > 0) {
      resolveDirectStream(servers[0].embedUrl).catch(() => {});
    }

    const isSeries =
      episodes.length > 0 ||
      seasons.length > 0 ||
      pageUrl.includes("مسلسل") ||
      title.includes("مسلسل") ||
      title.includes("الموسم");

    const detail: ArabseedDetail = {
      item: {
        id: Buffer.from(pageUrl).toString("base64url").slice(0, 16),
        title: title || "مشهد فيديو",
        poster,
        pageUrl,
        category: isSeries ? "مسلسلات" : "أفلام",
        isSeries,
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

      if (serverUrl.includes("/play/")) {
        const html = await fetchHtml(serverUrl, `${BASE_URL}/`, 4000);
        const iframeMatch = html.match(/iframe[^>]+src=['"]([^'"]+)['"]/i);
        if (iframeMatch && iframeMatch[1]) {
          embedPageUrl = iframeMatch[1];
        }
      }

      const embedHtml = await fetchHtml(embedPageUrl, `${GOVID_URL}/`, 4000);

      const mohixMatch = embedHtml.match(/const\s+Mohix\s*=\s*['"]([0-9a-fA-F]+)['"]/i);
      if (mohixMatch && mohixMatch[1]) {
        const direct = hexToString(mohixMatch[1]);
        if (direct && (direct.includes(".m3u8") || direct.includes(".mp4") || direct.startsWith("http"))) {
          streamCache.set(serverUrl, { url: direct, time: now });
          return direct;
        }
      }

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
