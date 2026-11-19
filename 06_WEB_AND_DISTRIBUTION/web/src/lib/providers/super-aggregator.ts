/**
 * super-aggregator.ts
 * Extends the base aggregator with new providers and enhanced stream resolution.
 */

import { aggregator, normalizeTitle } from "./aggregator";
import { titlesStrictlyMatch } from "../title-matcher";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { extractStreamFromEmbed, extractStreamWithFallback } from "./stream-extractor";
import { cima4uProvider } from "./cima4u";
import { akoamProvider } from "./akoam";
import { cimaclubProvider } from "./cimaclub";
import { shahid4uProvider } from "./shahid4u";
import { turkishProvider } from "./turcidiziizle";
import { animeProvider } from "./animeprovider";
import { foreignProvider } from "./foreignprovider";

const superSearchCache = new Map<string, { items: UnifiedMediaItem[]; time: number }>();
const superCategoryCache = new Map<string, { items: UnifiedMediaItem[]; time: number }>();
const SUPER_SEARCH_TTL = 10 * 60 * 1000;
const SUPER_CATEGORY_TTL = 15 * 60 * 1000;

const NEW_PROVIDERS: BaseProvider[] = [
  cima4uProvider,
  akoamProvider,
  cimaclubProvider,
  shahid4uProvider,
  turkishProvider,
  animeProvider,
  foreignProvider,
];

export class SuperAggregator {
  get allProviders(): BaseProvider[] {
    return [...aggregator.providers, ...NEW_PROVIDERS];
  }

  getProviderById(id: string): BaseProvider | undefined {
    return this.allProviders.find((p) => p.id === id);
  }

  /**
   * Smart stream resolution with cascade fallback.
   * Tries each server in order; if it's an embed URL, runs extractStreamFromEmbed.
   * Timeout per server: 5 seconds.
   */
  async resolveWithFallback(servers: UnifiedServer[]): Promise<string | null> {
    for (const server of servers) {
      try {
        const url = server.directStream || server.embedUrl;
        if (!url) continue;

        // Already a direct stream
        if (url.includes(".m3u8") || (url.includes(".mp4") && !url.includes("mp4upload"))) {
          return url;
        }

        const result = await Promise.race([
          extractStreamFromEmbed(url, server.embedUrl),
          new Promise<null>((resolve) => setTimeout(() => resolve(null), 5000)),
        ]);

        if (result?.url) {
          // If HLS, proxy it
          if (result.type === "hls") {
            return `/api/proxy/stream?url=${encodeURIComponent(result.url)}&ref=${encodeURIComponent(result.referer || url)}`;
          }
          return result.url;
        }
      } catch {
        // continue to next server
      }
    }
    return null;
  }

  /**
   * Search across ALL providers including new ones.
   */
  async searchAll(query: string, timeoutMs = 4000): Promise<UnifiedMediaItem[]> {
    const q = query.trim();
    if (!q) return [];

    const cacheKey = `super:search:${q.toLowerCase()}`;
    const cached = superSearchCache.get(cacheKey);
    if (cached && Date.now() - cached.time < SUPER_SEARCH_TTL) return cached.items;

    // Run base aggregator + new providers in parallel
    const basePromise = aggregator.searchAll(q, timeoutMs).catch(() => [] as UnifiedMediaItem[]);
    const newProviderPromises = NEW_PROVIDERS
      .filter((p) => p.isEnabled)
      .map(async (provider) => {
        try {
          return await Promise.race([
            provider.search(q),
            new Promise<UnifiedMediaItem[]>((resolve) => setTimeout(() => resolve([]), timeoutMs)),
          ]);
        } catch {
          return [] as UnifiedMediaItem[];
        }
      });

    const [baseItems, ...newResults] = await Promise.all([basePromise, ...newProviderPromises]);
    const rawItems: UnifiedMediaItem[] = [...baseItems, ...newResults.flat()];

    // Deduplicate by normalized title
    const seen = new Map<string, UnifiedMediaItem>();
    for (const it of rawItems) {
      const norm = normalizeTitle(it.title);
      if (!norm || norm.length < 2) continue;
      if (!seen.has(norm)) {
        seen.set(norm, it);
      } else {
        const existing = seen.get(norm)!;
        if (
          (!existing.poster || existing.poster.includes("logo")) &&
          it.poster && it.poster.startsWith("http") && !it.poster.includes("logo")
        ) {
          seen.set(norm, it);
        }
      }
    }

    const merged = Array.from(seen.values()).filter((it) => titlesStrictlyMatch(q, it.title));
    if (merged.length > 0) superSearchCache.set(cacheKey, { items: merged, time: Date.now() });
    return merged;
  }

  /**
   * Get streams for a specific category with smart provider selection.
   */
  async getCategoryWithFallback(slug: string, page = 1, timeoutMs = 4000): Promise<UnifiedMediaItem[]> {
    const cacheKey = `super:cat:${slug}:${page}`;
    const cached = superCategoryCache.get(cacheKey);
    if (cached && Date.now() - cached.time < SUPER_CATEGORY_TTL) return cached.items;

    // Select providers based on slug
    let targetNewProviders: BaseProvider[] = [];
    if (slug === "anime") {
      targetNewProviders = [animeProvider];
    } else if (slug === "turkish_series" || slug === "turkish_movies" || slug === "turkish") {
      targetNewProviders = [turkishProvider, shahid4uProvider];
    } else if (slug === "arabic_series" || slug === "arabic_movies") {
      targetNewProviders = [cima4uProvider, akoamProvider, cimaclubProvider];
    } else if (slug === "foreign_movies" || slug === "foreign_series" || slug === "foreign") {
      targetNewProviders = [foreignProvider, cima4uProvider];
    } else if (slug === "movies") {
      targetNewProviders = [cima4uProvider, akoamProvider, foreignProvider];
    } else if (slug === "series") {
      targetNewProviders = [cima4uProvider, shahid4uProvider, cimaclubProvider];
    } else {
      targetNewProviders = [cima4uProvider, akoamProvider];
    }

    // Run base aggregator + selected new providers
    const basePromise = aggregator.getCategory(slug, page, timeoutMs).catch(() => [] as UnifiedMediaItem[]);
    const newProviderPromises = targetNewProviders
      .filter((p) => p.isEnabled)
      .map(async (provider) => {
        try {
          return await Promise.race([
            provider.getCategory(slug, page),
            new Promise<UnifiedMediaItem[]>((resolve) => setTimeout(() => resolve([]), timeoutMs)),
          ]);
        } catch {
          return [] as UnifiedMediaItem[];
        }
      });

    const [baseItems, ...newResults] = await Promise.all([basePromise, ...newProviderPromises]);
    const rawItems: UnifiedMediaItem[] = [...baseItems, ...newResults.flat()];

    // Deduplicate
    const seen = new Set<string>();
    const deduplicated: UnifiedMediaItem[] = [];
    for (const it of rawItems) {
      const norm = normalizeTitle(it.title);
      if (!norm || seen.has(norm) || seen.has(it.pageUrl)) continue;
      seen.add(norm);
      seen.add(it.pageUrl);
      deduplicated.push(it);
    }

    if (deduplicated.length > 0) superCategoryCache.set(cacheKey, { items: deduplicated, time: Date.now() });
    return deduplicated;
  }

  /**
   * Get detail via base aggregator (checks all provider URLs) + new providers.
   */
  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    // Try base aggregator first
    try {
      const baseDetail = await aggregator.getDetail(pageUrl);
      if (baseDetail) return baseDetail;
    } catch {}

    // Try new providers
    for (const provider of NEW_PROVIDERS) {
      if (!provider.isEnabled) continue;
      if (!pageUrl.includes(provider.baseUrl.replace("https://", "").replace("http://", ""))) continue;
      try {
        const detail = await provider.getDetail(pageUrl);
        if (detail) return detail;
      } catch {}
    }
    return null;
  }

  /**
   * Resolve a stream URL using base aggregator + stream extractor fallback.
   */
  async resolveStream(serverUrl: string): Promise<string | null> {
    if (!serverUrl) return null;

    // Already direct
    if (serverUrl.includes(".m3u8") || serverUrl.endsWith(".mp4")) {
      return serverUrl;
    }

    // Try base aggregator first
    try {
      const baseResult = await aggregator.resolveStream(serverUrl);
      if (baseResult && (baseResult.includes(".m3u8") || baseResult.endsWith(".mp4"))) {
        return baseResult;
      }
    } catch {}

    // Try new providers
    for (const provider of NEW_PROVIDERS) {
      if (!provider.isEnabled) continue;
      if (!serverUrl.includes(provider.baseUrl.replace("https://", "").replace("http://", ""))) continue;
      try {
        const result = await provider.resolveStream(serverUrl);
        if (result) return result;
      } catch {}
    }

    // Last resort: stream extractor
    try {
      const extracted = await Promise.race([
        extractStreamFromEmbed(serverUrl),
        new Promise<null>((resolve) => setTimeout(() => resolve(null), 7000)),
      ]);
      if (extracted?.url) {
        if (extracted.type === "hls") {
          return `/api/proxy/stream?url=${encodeURIComponent(extracted.url)}&ref=${encodeURIComponent(extracted.referer || serverUrl)}`;
        }
        return extracted.url;
      }
    } catch {}

    return serverUrl;
  }

  async resolveSingleServer(serverUrl: string): Promise<string | null> {
    return this.resolveStream(serverUrl);
  }
}

export const superAggregator = new SuperAggregator();
