import type { CheerioAPI } from "cheerio";
import { UnifiedEpisode, UnifiedSeason } from "./types";

function absoluteUrl(href: string, pageUrl: string): string {
  try { return new URL(href, pageUrl).toString(); } catch { return ""; }
}

export function extractMediaMetadata($: CheerioAPI, pageUrl: string) {
  const cast = new Set<string>();
  const seasons: UnifiedSeason[] = [];
  const episodes: UnifiedEpisode[] = [];

  $("[itemprop='actor'], [itemprop='actors'], .actors a, .Actors a, .actor a, .cast a, .Cast a, .cast-list a, .post__actors a, .persons a")
    .each((_, element) => {
      const node = $(element);
      const name = node.attr("content") || node.find("[itemprop='name']").first().text().trim() || node.text().trim();
      if (name && name.length < 80) cast.add(name.replace(/\s+/g, " "));
    });

  const seasonSelector = "a[href*='/series/'], .Seasons--List a, .seasons-list a, .season-list a, .seasons a, .season a, .SeasonBlock a, a[href*='/season/'], a[href*='season-'], a[href*='/part/'], a[href*='الموسم'], a[href*='الجزء']";
  $(seasonSelector).each((_, element) => {
    const node = $(element);
    const href = node.attr("href") || "";
    const page = absoluteUrl(href, pageUrl);
    const title = node.text().trim() || node.attr("title") || "";
    if (!page || page === pageUrl || !title || seasons.some((season) => season.pageUrl === page)) return;
    seasons.push({ title: title.replace(/\s+/g, " "), pageUrl: page, seasonNumber: `${seasons.length + 1}` });
  });

  const episodeSelector = "a.episode__item, .episode__item a, .Episodes--List a, .episodes-list a, .episode-list a, a.ep-item, .epsList a, .episodes a, a[href*='episode-'], a[href*='الحلقة']";
  $(episodeSelector).each((_, element) => {
    const node = $(element);
    const href = node.attr("href") || "";
    const page = absoluteUrl(href, pageUrl);
    const title = (node.find(".title, h3, span").first().text().trim() || node.text().trim()).replace(/\s+/g, " ");
    if (!page || page === pageUrl || !title || episodes.some((episode) => episode.pageUrl === page)) return;
    const number = title.match(/(?:الحلقة|episode|ep)\s*#?\s*(\d+)/i)?.[1];
    episodes.push({ title, pageUrl: page, episodeNumber: number || `${episodes.length + 1}` });
  });

  return { cast: Array.from(cast), seasons, episodes };
}
