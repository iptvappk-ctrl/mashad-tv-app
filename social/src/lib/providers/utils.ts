/**
 * 🛠️ Shared fetch helper + metadata extractor
 * يستخدمه كل الـ providers
 */

import * as cheerio from "cheerio";
import { UnifiedEpisode, UnifiedSeason } from "./types";

const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

export async function fetchHtml(
  url: string,
  referer: string,
  timeoutMs = 6000
): Promise<string> {
  const ctrl = new AbortController();
  const tid = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": UA,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "ar,en;q=0.9",
        Referer: referer,
      },
      signal: ctrl.signal,
      redirect: "follow",
    });
    return res.ok ? await res.text() : "";
  } catch {
    return "";
  } finally {
    clearTimeout(tid);
  }
}

export interface MediaMetadata {
  cast: string[];
  seasons: UnifiedSeason[];
  episodes: UnifiedEpisode[];
}

/** يستخرج cast + seasons + episodes من HTML بطرق مختلفة */
export function extractMetadata($: cheerio.CheerioAPI, pageUrl: string): MediaMetadata {
  const cast: string[] = [];
  const seasons: UnifiedSeason[] = [];
  const episodes: UnifiedEpisode[] = [];

  // Cast
  $(".cast a, .actors a, .cast-box a, [class*='cast'] a").each((_, el) => {
    const name = $(el).text().trim();
    if (name && name.length > 1 && !cast.includes(name)) cast.push(name);
  });

  // Seasons
  $(".seasons a, .season-list a, a[href*='/season/'], a[href*='/الموسم']").each((_, el) => {
    const href = $(el).attr("href") || "";
    const title = $(el).text().trim() || `موسم ${seasons.length + 1}`;
    if (href) {
      seasons.push({
        title,
        pageUrl: href.startsWith("http") ? href : "",
        seasonNumber: `${seasons.length + 1}`,
      });
    }
  });

  // Episodes
  $(".episodes a, .ep-list a, a[href*='/episode/'], a[href*='/ep-'], a[href*='/الحلقة']").each((_, el) => {
    const href = $(el).attr("href") || "";
    const epTitle = $(el).text().trim() || `حلقة ${episodes.length + 1}`;
    if (href) {
      episodes.push({
        title: epTitle,
        pageUrl: href.startsWith("http") ? href : "",
        episodeNumber: `${episodes.length + 1}`,
      });
    }
  });

  return { cast: cast.slice(0, 15), seasons, episodes };
}

export function makeId(prefix: string, url: string): string {
  return `${prefix}_${Buffer.from(url).toString("base64url").slice(0, 16)}`;
}
