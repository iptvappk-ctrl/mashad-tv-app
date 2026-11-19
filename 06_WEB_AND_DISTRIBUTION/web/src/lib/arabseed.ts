import * as cheerio from "cheerio";
import { titlesStrictlyMatch } from "./title-matcher";

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
  providerId?: string;
  providerName?: string;
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
  cast?: string[];
  servers: ArabseedServer[];
  seasons: ArabseedSeason[];
  episodes: ArabseedEpisode[];
  directStream?: string;
  proxiedStream?: string;
}

const BASE_URL = "https://m.myseed.tv";
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
    "a.movie__block, .item__contents a, .movie-item a, article a, .postDiv a, .innov-series-card a, .innov-series-card, [class*='series-card'] a, .MovieBlock a, .Block--Item a, a.BlockItem, .Movie--Block a, .item a, .Movie--Block, .Film--Block, .Series--Block, .BlockItem, a.episode__item"
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

// Category mappings for Arabseed (Mobile Endpoint)
const CATEGORY_MAP: Record<string, string> = {
  trending: "trend/",
  recent: "recently/",
  movies: "movies/",
  series: "series/",
  foreign_movies: "category/foreign-movies/",
  arabic_movies: "category/arabic-movies/",
  turkish_movies: "category/turkish-movies/",
  asian_movies: "category/asian-movies/",
  indian_movies: "category/indian-movies/",
  animation_movies: "category/animation-movies/",
  dubbed_movies: "category/dubbed-movies/",
  foreign_series: "category/foreign-series/",
  arabic_series: "category/arabic-series/",
  turkish_series: "category/turkish-series/",
  asian_series: "category/مسلسلات-كوريه/",
  indian_series: "category/مسلسلات-هندية/",
  dubbed_series: "category/dubbed-series/",
  ramadan: "category/مسلسلات-رمضان/",
  anime: "category/animation-movies/",
  anime_movies: "category/animation-movies/",
  anime_series: "category/cartoon-series/",
  wrestling: "category/wwe-shows/",
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
      url = page <= 1 ? `${BASE_URL}/find/?word=${q}` : `${BASE_URL}/find/page/${page}/?word=${q}`;
    } else {
      const rawPath = CATEGORY_MAP[slug] || CATEGORY_MAP["movies"];
      const cleanPath = rawPath.replace(/^\/+|\/+$/g, "");
      url = page <= 1 ? `${BASE_URL}/${cleanPath}/` : `${BASE_URL}/${cleanPath}/page/${page}/`;
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
    const trimmed = query.trim();
    if (!trimmed) return [];
    const encoded = encodeURIComponent(trimmed);
    const url = `${BASE_URL}/find/?word=${encoded}`;
    const html = await fetchHtml(url);

    // Soft-404 / 0 search results check: prevent WordPress fallback to recent/featured posts
    if (
      html.includes("لم يتم العثور على أي نتائج") ||
      html.includes("لا توجد نتائج") ||
      html.includes("no-results") ||
      html.includes("not-found")
    ) {
      return [];
    }

    const items = parseListing(html);
    // Strict title matching: discard any unrelated media (e.g. BBC shows returned when searching Arabic series)
    return items.filter((it) => titlesStrictlyMatch(trimmed, it.title));
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
    const watchUrl = `${cleanUrl}/watch/`;

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

    const castNames = new Set<string>();
    $("[itemprop='actor'] [itemprop='name'], [itemprop='actor'], .Actors a, .actors a, .actor a, .cast a, .Cast a, .cast-list a, .post__actors a")
      .each((_, el) => {
        const name = $(el).attr("content") || $(el).text().trim();
        if (name && name.length < 80) castNames.add(name);
      });
    $("script[type='application/ld+json']").each((_, el) => {
      try {
        const parsed = JSON.parse($(el).text());
        const nodes = Array.isArray(parsed) ? parsed : [parsed];
        for (const node of nodes) {
          const graph = Array.isArray(node?.["@graph"]) ? node["@graph"] : [node];
          for (const entity of graph) {
            const actors = Array.isArray(entity?.actor) ? entity.actor : entity?.actor ? [entity.actor] : [];
            for (const actor of actors) {
              const name = typeof actor === "string" ? actor : actor?.name;
              if (typeof name === "string" && name.trim().length < 80) castNames.add(name.trim());
            }
          }
        }
      } catch {
        // Ignore malformed metadata from the source page.
      }
    });

    // Parse Watch Servers
    const servers: ArabseedServer[] = [];
    const sourceHtml = watchHtml || pageHtml;
    const $watch = cheerio.load(sourceHtml);

    function cleanServerName(rawName: string, url: string): string {
      const u = url.toLowerCase();
      if (u.includes("vids.php") || u.includes("player-gateway")) return "سيرفر ماي سيد (فائق السرعة ⚡)";
      if (u.includes("bysezejataos") || u.includes("vidsharing")) return "Vidsharing (فائق السرعة ⚡)";
      if (u.includes("uqload")) return "uqload (سريع)";
      if (u.includes("voe.")) return "voe (سريع)";
      if (u.includes("vidara.") || u.includes("sugbaioug.")) return "vidara (سريع)";
      if (u.includes("vinvo.")) return "Vinvo (سريع)";
      if (u.includes("vidmoly.")) return "Vidmoly (سريع)";
      if (u.includes("playtube.")) return "Playtube";
      if (u.includes("dsvplay.")) return "DSV Play";
      if (rawName && !rawName.startsWith("سيرفر")) return rawName;
      return "سيرفر";
    }

    $watch(
      "#watch li, ul.servers li, .watch-servers li, #servers li, [data-link], [data-embed], [data-url], [data-server], iframe#videoIframe, iframe"
    ).each((_, el) => {
      const s = $watch(el);
      let embedUrl =
        s.attr("data-link") ||
        s.attr("data-embed") ||
        s.attr("data-url") ||
        s.attr("data-server") ||
        s.attr("src") ||
        "";
      const rawName = s.text().trim() || s.attr("title") || s.attr("data-server-name") || "";

      if (!embedUrl) return;

      // Extract Base64 encoded target URLs if wrapped (e.g. vid/?id=...)
      if (embedUrl.includes("id=")) {
        const b64 = embedUrl.split("id=")[1]?.split("&")[0];
        if (b64) {
          try {
            const decoded = Buffer.from(b64, "base64").toString("utf8").trim();
            if (decoded.startsWith("http")) embedUrl = decoded;
          } catch (_) {}
        }
      }

      let fullUrl = embedUrl;
      if (fullUrl.startsWith("//")) fullUrl = `https:${fullUrl}`;
      else if (fullUrl.startsWith("/")) fullUrl = `${BASE_URL.replace(/\/$/, "")}${fullUrl}`;

      if (fullUrl.startsWith("http") && !servers.some((x) => x.embedUrl === fullUrl)) {
        const cleaned = cleanServerName(rawName, fullUrl);
        const name = cleaned === "سيرفر" ? `سيرفر ${servers.length + 1}` : cleaned;
        servers.push({ name, embedUrl: fullUrl });
      }
    });

function isEpisodeUrlOrTitle(title: string, url: string): boolean {
  try {
    const decodedUrl = decodeURIComponent(url).toLowerCase();
    const cleanTitle = title.toLowerCase();
    const combined = `${cleanTitle} ${decodedUrl}`;
    return /(?:الحلقة|حلقة|episode|ep[-_/\d]|%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9)/i.test(combined);
  } catch {
    return /الحلقة|حلقة|episode/i.test(title + " " + url);
  }
}

    // Check for Seasons (strictly filtering out individual episodes)
    const seasons: ArabseedSeason[] = [];
    $("a[href*='/series/'], a[href*='/selary/'], .seasons__list a, .Seasons--List a, .seasons-list a, .SeasonBlock a").each((_, el) => {
      const a = $(el);
      const sUrl = a.attr("href");
      if (sUrl && sUrl.startsWith("http") && sUrl !== pageUrl) {
        const rawTitle = a.text().trim();
        if (isEpisodeUrlOrTitle(rawTitle, sUrl)) {
          return;
        }
        if (!seasons.some((x) => x.pageUrl === sUrl)) {
          seasons.push({
            title: rawTitle || `الموسم ${seasons.length + 1}`,
            pageUrl: sUrl,
            seasonNumber: `${seasons.length + 1}`,
          });
        }
      }
    });

    // Check for Episodes (Real Arabseed selectors & proper sorting)
    const episodes: ArabseedEpisode[] = [];
    $(".episodes__list a, .episodes__section a, a.episode__item, .episode__item a, a[href*='-eps'], a[href*='/%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9-'], a[href*='الحلقة'], .Episodes--List a, .EpisodeBlock a").each((_, el) => {
      const a = $(el);
      const epUrl = a.attr("href");
      if (epUrl && epUrl.startsWith("http") && !episodes.some((e) => e.pageUrl === epUrl)) {
        let epTitle = a.find(".title, h3, span").text().trim() || a.text().trim();
        let epNum = `${episodes.length + 1}`;
        try {
          const decoded = decodeURIComponent(epUrl);
          const match = `${epTitle} ${decoded}`.match(/(?:الحلقة|حلقة|episode|ep)[\s._#=/:-]*([0-9]{1,4})/i);
          if (match && match[1]) {
            epNum = match[1];
          }
        } catch {}
        if (!epTitle.includes("الحلقة") && !epTitle.includes("حلقة")) {
          epTitle = `الحلقة ${epNum}`;
        }
        episodes.push({
          title: epTitle,
          pageUrl: epUrl,
          episodeNumber: epNum,
        });
      }
    });

    // Sort episodes ascending by episode number
    episodes.sort((a, b) => {
      const numA = parseInt(a.episodeNumber || "0", 10) || 0;
      const numB = parseInt(b.episodeNumber || "0", 10) || 0;
      return numA - numB;
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
        title: title || "زقزوقة فيديو",
        poster,
        pageUrl,
        category: isSeries ? "مسلسلات" : "أفلام",
        isSeries,
      },
      overview,
      cast: Array.from(castNames),
      servers,
      seasons,
      episodes,
    };

    if (servers.length > 0 || episodes.length > 0) {
      detailCache.set(pageUrl, { data: detail, time: now });
    }
    return detail;
  } catch (err) {
    console.error("Arabseed detail error:", err);
    return null;
  }
}

/**
 * Resolves direct video stream link (.m3u8 or .mp4) from embed servers (MySeed, Govid, Voe, Vidsharing, etc.)
 */
export async function resolveDirectStream(serverUrl: string): Promise<string | undefined> {
  if (!serverUrl) return undefined;

  const isVids = serverUrl.includes("vids.php") || serverUrl.includes("player-gateway");

  const now = Date.now();
  if (!isVids) {
    const cached = streamCache.get(serverUrl);
    if (cached && now - cached.time < STREAM_TTL) {
      return cached.url;
    }
  }

  try {
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) {
      streamCache.set(serverUrl, { url: serverUrl, time: now });
      return serverUrl;
    }

    // 1. MySeed vids.php / player-gateway resolver (Live chain, 60s tokens)
    if (serverUrl.includes("vids.php") || serverUrl.includes("player-gateway") || serverUrl.includes("myseed")) {
      try {
        let vidsHtml = "";
        let gatewayUrl = "";

        if (serverUrl.includes("player-gateway")) {
          gatewayUrl = serverUrl;
        } else {
          vidsHtml = await fetchHtml(serverUrl, `${BASE_URL}/`, 5000);
          const gwMatch = vidsHtml.match(/src=["']([^"']+player-gateway[^"']+)["']/i);
          if (gwMatch?.[1]) {
            gatewayUrl = gwMatch[1].replace(/&amp;/g, "&");
          }
        }

        if (gatewayUrl) {
          const gwHtml = await fetchHtml(gatewayUrl, serverUrl, 5000);
          const srcMatch = gwHtml.match(/<source[^>]+src=["']([^"']+\.(?:mp4|m3u8)[^"']*)["']/i);
          const rawDirect = srcMatch?.[1] ||
            gwHtml.match(/["'](https?:\/\/[^"']+\.m3u8[^"']*)/i)?.[1] ||
            gwHtml.match(/["'](https?:\/\/[^"']+\.mp4[^"']*)/i)?.[1];

          if (rawDirect && rawDirect.startsWith("http")) {
            const proxied = `/api/proxy/stream?url=${encodeURIComponent(rawDirect)}&ref=${encodeURIComponent("https://d.myseed.tv/")}`;
            return proxied;
          }
        }
      } catch (e) {
        console.warn("MySeed vids.php resolution failed:", e);
      }
    }

    // 2. Govid.live resolver
    if (serverUrl.includes("govid.live") || serverUrl.includes("datahowa")) {
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
          const proxied = direct.includes(".m3u8") || direct.includes("datahowa")
            ? `/api/proxy/stream?url=${encodeURIComponent(direct)}&ref=${encodeURIComponent("https://govid.live/")}`
            : direct;
          streamCache.set(serverUrl, { url: proxied, time: now });
          return proxied;
        }
      }

      const m3u8Match = embedHtml.match(/https?:\/\/[^"']+\.m3u8[^"']*/i);
      if (m3u8Match) {
        const proxied = `/api/proxy/stream?url=${encodeURIComponent(m3u8Match[0])}&ref=${encodeURIComponent("https://govid.live/")}`;
        streamCache.set(serverUrl, { url: proxied, time: now });
        return proxied;
      }

      const mp4Match = embedHtml.match(/https?:\/\/[^"']+\.mp4[^"']*/i);
      if (mp4Match) {
        streamCache.set(serverUrl, { url: mp4Match[0], time: now });
        return mp4Match[0];
      }
    }

    // 3. Vidsharing / bysezejataos resolver
    if (serverUrl.includes("vidsharing") || serverUrl.includes("bysezejataos")) {
      try {
        const vsHtml = await fetchHtml(serverUrl, `${BASE_URL}/`, 4000);
        const m = vsHtml.match(/file\s*:\s*["']([^"']+\.m3u8[^"']*)["']/i) || vsHtml.match(/["'](https?:\/\/[^"']+\.m3u8[^"']*)["']/i);
        if (m?.[1] && m[1].startsWith("http")) {
          const proxied = `/api/proxy/stream?url=${encodeURIComponent(m[1])}&ref=${encodeURIComponent(serverUrl)}`;
          streamCache.set(serverUrl, { url: proxied, time: now });
          return proxied;
        }
      } catch {}
    }

    // 4. Voe resolver
    if (serverUrl.includes("voe.")) {
      try {
        const voeHtml = await fetchHtml(serverUrl, `${BASE_URL}/`, 4000);
        const m = voeHtml.match(/['"]hls['"]\s*:\s*['"]([^'"]+\.m3u8[^"']*)['"]/i);
        if (m?.[1] && m[1].startsWith("http")) {
          const proxied = `/api/proxy/stream?url=${encodeURIComponent(m[1])}&ref=${encodeURIComponent(serverUrl)}`;
          streamCache.set(serverUrl, { url: proxied, time: now });
          return proxied;
        }
      } catch {}
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
