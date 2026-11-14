/**
 * 🌍 Foreign & Hollywood Provider — السينما العالمية والأجنبية
 * مصادر: شبكتي سينمانا (Cinemana) + بدائل هوليوود المترجمة والمدبلجة
 */

import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import { fetchHtml, extractMetadata, makeId } from "./utils";
import { extractStreamFromEmbed } from "../extractors/stream-extractor";

const BASE = "https://cinemana.shabakaty.com";
const cache = new Map<string, { data: any; time: number }>();
const TTL = 15 * 60 * 1000;

export const foreignProvider: BaseProvider = {
  id: "foreign_hub",
  name: "السينما الأجنبية وهوليوود",
  baseUrl: BASE,
  isEnabled: true,

  async search(query: string) {
    const ck = `foreign:s:${query}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    try {
      // Shabakaty Cinemana Public Search API
      const res = await fetch(`https://cinemana.shabakaty.com/api/android/allVideo/search?videoTitle=${encodeURIComponent(query)}&page=0`, {
        headers: { "User-Agent": "okhttp/4.9.0", "Accept": "application/json" }
      });
      if (res.ok) {
        const data = await res.json();
        const list = Array.isArray(data) ? data : data?.items || [];
        const items: UnifiedMediaItem[] = list.map((it: any) => ({
          id: makeId("foreign", it.nb || it.id || it.en_title),
          title: it.ar_title || it.en_title || "فيلم أجنبي",
          poster: it.img_url || it.poster || "",
          pageUrl: `https://cinemana.shabakaty.com/video/ar/${it.nb || it.id}`,
          category: it.is_series === "1" ? "مسلسلات أجنبية" : "أفلام أجنبية",
          quality: it.resolution || "1080p",
          year: it.year || "",
          rating: it.rate || "",
          isSeries: it.is_series === "1",
          providerId: "foreign_hub",
          providerName: "السينما الأجنبية وهوليوود",
        }));
        if (items.length) {
          cache.set(ck, { data: items, time: Date.now() });
          return items;
        }
      }
    } catch {}
    return [];
  },

  async getCategory(slug, page = 1) {
    const ck = `foreign:cat:${slug}:${page}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    try {
      const type = slug.includes("series") ? "series" : "movies";
      const res = await fetch(`https://cinemana.shabakaty.com/api/android/allVideo/all/type/${type}/page/${page - 1}`, {
        headers: { "User-Agent": "okhttp/4.9.0", "Accept": "application/json" }
      });
      if (res.ok) {
        const data = await res.json();
        const list = Array.isArray(data) ? data : data?.items || [];
        const items: UnifiedMediaItem[] = list.map((it: any) => ({
          id: makeId("foreign", it.nb || it.id || it.en_title),
          title: it.ar_title || it.en_title || "فيلم أجنبي",
          poster: it.img_url || it.poster || "",
          pageUrl: `https://cinemana.shabakaty.com/video/ar/${it.nb || it.id}`,
          category: it.is_series === "1" ? "مسلسلات أجنبية" : "أفلام أجنبية",
          quality: it.resolution || "1080p",
          year: it.year || "",
          rating: it.rate || "",
          isSeries: it.is_series === "1",
          providerId: "foreign_hub",
          providerName: "السينما الأجنبية وهوليوود",
        }));
        if (items.length) {
          cache.set(ck, { data: items, time: Date.now() });
          return items;
        }
      }
    } catch {}
    return [];
  },

  async getDetail(pageUrl) {
    const ck = `foreign:d:${pageUrl}`;
    const c = cache.get(ck);
    if (c && Date.now() - c.time < TTL) return c.data;

    const videoId = pageUrl.split("/").pop() || "";
    try {
      const res = await fetch(`https://cinemana.shabakaty.com/api/android/videoFiles/id/${videoId}`, {
        headers: { "User-Agent": "okhttp/4.9.0", "Accept": "application/json" }
      });
      if (res.ok) {
        const files = await res.json();
        const servers: UnifiedServer[] = (Array.isArray(files) ? files : []).map((f: any, i: number) => ({
          name: `سيرفر ${i + 1} (${f.resolution || "HD"})`,
          embedUrl: f.videoUrl || "",
          directStream: f.videoUrl || "",
          quality: f.resolution || "HD",
          providerId: "foreign_hub",
          isHls: (f.videoUrl || "").includes(".m3u8"),
        }));

        const detail: UnifiedMediaDetail = {
          item: {
            id: makeId("foreign", pageUrl),
            title: "فيلم هوليوود أجنبي",
            poster: "",
            pageUrl,
            category: "أفلام أجنبية",
            isSeries: false,
            providerId: "foreign_hub",
            providerName: "السينما الأجنبية وهوليوود",
          },
          overview: "مشاهدة وتحميل فائق السرعة بدون إعلانات بأعلى جودة 1080p.",
          servers: servers.length ? servers : [{ name: "سيرفر 1", embedUrl: pageUrl, providerId: "foreign_hub" }],
          seasons: [],
          episodes: [],
        };
        cache.set(ck, { data: detail, time: Date.now() });
        return detail;
      }
    } catch {}

    return null;
  },

  async resolveStream(serverUrl) {
    if (serverUrl.includes(".m3u8") || serverUrl.includes(".mp4")) return serverUrl;
    const r = await extractStreamFromEmbed(serverUrl, BASE);
    return r?.url || serverUrl;
  },
};
