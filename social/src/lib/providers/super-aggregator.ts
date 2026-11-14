/**
 * 🚀 Super Aggregator Engine
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * يجمع كل المصادر ومواقع المشاهدة في محرك واحد موحد
 * يوفر البحث الموحد، وتصنيفات الأقسام، واستخراج الروابط المباشرة
 * مع نظام Smart Fallback الذكي للتنقل بين السيرفرات في ثوانٍ معدودة وبدون إعلانات
 */

import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { arabseedProvider } from "./arabseed";
import { wecimaProvider } from "./wecima";
import { faselHDProvider } from "./faselhd";
import { akwamProvider } from "./akwam";
import { egybestProvider } from "./egybest";
import { cima4uProvider } from "./cima4u";
import { turkishProvider } from "./turkish";
import { animeProvider } from "./anime";
import { foreignProvider } from "./foreign";
import { extractStreamWithFallback, extractStreamFromEmbed, toProxiedUrl } from "../extractors/stream-extractor";

export const ALL_PROVIDERS: BaseProvider[] = [
  arabseedProvider,
  wecimaProvider,
  faselHDProvider,
  akwamProvider,
  egybestProvider,
  cima4uProvider,
  turkishProvider,
  animeProvider,
  foreignProvider,
];

const searchCache = new Map<string, { items: UnifiedMediaItem[]; time: number }>();
const categoryCache = new Map<string, { items: UnifiedMediaItem[]; time: number }>();
const streamCache = new Map<string, { url: string; time: number }>();

const SEARCH_TTL = 10 * 60 * 1000;
const CATEGORY_TTL = 15 * 60 * 1000;
const STREAM_TTL = 10 * 60 * 1000;

export class SuperAggregator {
  providers = ALL_PROVIDERS;

  getProvider(id: string): BaseProvider | undefined {
    return this.providers.find((p) => p.id === id);
  }

  getProviderForUrl(url: string): BaseProvider {
    if (url.includes("wecima") || url.includes("myweecima")) return wecimaProvider;
    if (url.includes("fasel") || url.includes("fasel4k")) return faselHDProvider;
    if (url.includes("akw") || url.includes("akwam")) return akwamProvider;
    if (url.includes("egybest")) return egybestProvider;
    if (url.includes("cima4u")) return cima4uProvider;
    if (url.includes("dizimag") || url.includes("turkfan")) return turkishProvider;
    if (url.includes("animeblkom") || url.includes("shahiidanime")) return animeProvider;
    if (url.includes("cinemana") || url.includes("shabakaty")) return foreignProvider;
    return arabseedProvider;
  }

  /**
   * 🔍 بحث موحد وشامل في كل المواقع المدعومة بالتوازي
   */
  async searchAll(query: string, timeoutMs = 4500): Promise<UnifiedMediaItem[]> {
    const q = query.trim();
    if (!q) return [];

    const ck = `search:${q.toLowerCase()}`;
    const cached = searchCache.get(ck);
    if (cached && Date.now() - cached.time < SEARCH_TTL) return cached.items;

    const promises = this.providers.map(async (provider) => {
      try {
        return await Promise.race([
          provider.search(q),
          new Promise<UnifiedMediaItem[]>((r) => setTimeout(() => r([]), timeoutMs)),
        ]);
      } catch {
        return [];
      }
    });

    const results = await Promise.allSettled(promises);
    const rawItems: UnifiedMediaItem[] = [];
    for (const r of results) {
      if (r.status === "fulfilled" && Array.isArray(r.value)) {
        rawItems.push(...r.value);
      }
    }

    // إزالة التكرار مع الحفاظ على أعلى جودة للبوستر
    const seen = new Map<string, UnifiedMediaItem>();
    for (const it of rawItems) {
      const norm = it.title.toLowerCase().replace(/\s+/g, " ").trim();
      if (!norm || norm.length < 2) continue;
      if (!seen.has(norm)) {
        seen.set(norm, it);
      } else {
        const existing = seen.get(norm)!;
        if ((!existing.poster || existing.poster.includes("logo")) && it.poster?.startsWith("http")) {
          seen.set(norm, it);
        }
      }
    }

    const merged = Array.from(seen.values());
    if (merged.length > 0) searchCache.set(ck, { items: merged, time: Date.now() });
    return merged;
  }

  /**
   * 📂 جلب محتوى الأقسام (أفلام، مسلسلات، تركي، أنمي، أجنبي) مع دمج المصادر المتخصصة
   */
  async getCategoryWithFallback(slug: string, page = 1, timeoutMs = 4500): Promise<UnifiedMediaItem[]> {
    const ck = `cat:${slug}:${page}`;
    const cached = categoryCache.get(ck);
    if (cached && Date.now() - cached.time < CATEGORY_TTL) return cached.items;

    let targetProviders: BaseProvider[] = [arabseedProvider, wecimaProvider, akwamProvider];

    if (slug === "anime" || slug === "anime_dubbed" || slug === "anime_subbed" || slug === "anime_movie") {
      targetProviders = [animeProvider, egybestProvider, akwamProvider];
    } else if (slug === "turkish_series" || slug === "turkish_movies" || slug === "turkish_dubbed") {
      targetProviders = [turkishProvider, wecimaProvider, arabseedProvider];
    } else if (slug === "foreign_movies" || slug === "foreign_series") {
      targetProviders = [foreignProvider, faselHDProvider, egybestProvider, akwamProvider];
    } else if (slug === "arabic_movies" || slug === "arabic_series") {
      targetProviders = [cima4uProvider, arabseedProvider, wecimaProvider, egybestProvider];
    }

    const promises = targetProviders.map(async (provider) => {
      try {
        return await Promise.race([
          provider.getCategory(slug, page),
          new Promise<UnifiedMediaItem[]>((r) => setTimeout(() => r([]), timeoutMs)),
        ]);
      } catch {
        return [];
      }
    });

    const results = await Promise.allSettled(promises);
    const rawItems: UnifiedMediaItem[] = [];
    for (const r of results) {
      if (r.status === "fulfilled" && Array.isArray(r.value)) {
        rawItems.push(...r.value);
      }
    }

    const seen = new Set<string>();
    const deduplicated: UnifiedMediaItem[] = [];
    for (const it of rawItems) {
      const norm = it.title.toLowerCase().replace(/\s+/g, " ").trim();
      if (!norm || seen.has(norm) || seen.has(it.pageUrl)) continue;
      seen.add(norm);
      seen.add(it.pageUrl);
      deduplicated.push(it);
    }

    if (deduplicated.length > 0) categoryCache.set(ck, { items: deduplicated, time: Date.now() });
    return deduplicated;
  }

  /**
   * 📄 جلب تفاصيل الفيلم أو المسلسل وقائمة السيرفرات
   */
  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    const provider = this.getProviderForUrl(pageUrl);
    try {
      return await provider.getDetail(pageUrl);
    } catch {
      return null;
    }
  }

  /**
   * ⚡ Smart Stream Fallback Engine
   * الميزة الجوهرية: يجرب قائمة السيرفرات بالتوازي والتوالي في ثوانٍ معدودة
   * ويرجع أول رابط فيديو نظيف ومباشر بدون إعلانات نهائياً (m3u8 / mp4)
   */
  async resolveWithFallback(
    servers: UnifiedServer[],
    referer?: string
  ): Promise<{ url: string; proxied: string; type: string; quality?: string } | null> {
    if (!servers || servers.length === 0) return null;

    // 1. فحص السيرفرات المباشرة فوراً
    for (const s of servers) {
      if (s.directStream && (s.directStream.includes(".m3u8") || s.directStream.includes(".mp4"))) {
        const proxied = toProxiedUrl(s.directStream, s.embedUrl || referer);
        return {
          url: s.directStream,
          proxied,
          type: s.directStream.includes(".m3u8") ? "hls" : "mp4",
          quality: s.quality || "HD",
        };
      }
    }

    // 2. فحص الكاش السريع
    const cacheKey = servers.map((s) => s.embedUrl).join("|");
    const cached = streamCache.get(cacheKey);
    if (cached && Date.now() - cached.time < STREAM_TTL) {
      const proxied = toProxiedUrl(cached.url, referer);
      return {
        url: cached.url,
        proxied,
        type: cached.url.includes(".m3u8") ? "hls" : "mp4",
      };
    }

    // 3. استخراج الرابط المباشر من خلال محرك الـ Extractors مع الـ Fallback
    const embedUrls = servers.map((s) => s.embedUrl).filter(Boolean);
    const extracted = await extractStreamWithFallback(embedUrls, referer);

    if (extracted?.url) {
      streamCache.set(cacheKey, { url: extracted.url, time: Date.now() });
      const proxied = toProxiedUrl(extracted.url, extracted.referer || referer);
      return {
        url: extracted.url,
        proxied,
        type: extracted.type,
        quality: extracted.quality || "HD",
      };
    }

    // 4. Fallback نهائي: توجيه الـ embed عبر بروكسي معقم من الإعلانات
    if (servers[0]?.embedUrl) {
      const embedProxied = `/api/proxy/embed?url=${encodeURIComponent(servers[0].embedUrl)}`;
      return {
        url: servers[0].embedUrl,
        proxied: embedProxied,
        type: "embed",
      };
    }

    return null;
  }

  /**
   * حل سيرفر واحد مباشر
   */
  async resolveSingleServer(serverUrl: string): Promise<string | null> {
    if (!serverUrl) return null;
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) {
      return toProxiedUrl(serverUrl);
    }

    const provider = this.getProviderForUrl(serverUrl);
    try {
      const direct = await Promise.race([
        provider.resolveStream(serverUrl),
        new Promise<null>((r) => setTimeout(() => r(null), 5000)),
      ]);
      if (direct && (direct.includes(".m3u8") || direct.includes(".mp4"))) {
        return toProxiedUrl(direct, serverUrl);
      }
    } catch {}

    try {
      const ext = await extractStreamFromEmbed(serverUrl);
      if (ext?.url) {
        return toProxiedUrl(ext.url, serverUrl);
      }
    } catch {}

    return `/api/proxy/embed?url=${encodeURIComponent(serverUrl)}`;
  }
}

export const superAggregator = new SuperAggregator();
