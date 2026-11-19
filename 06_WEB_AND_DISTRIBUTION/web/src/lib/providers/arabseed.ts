import * as cheerio from "cheerio";
import { BaseProvider, UnifiedMediaDetail, UnifiedMediaItem, UnifiedServer } from "./types";
import {
  getArabseedCategory,
  getArabseedDetail,
  searchArabseed,
  resolveDirectStream,
} from "../arabseed";

export class ArabseedProvider implements BaseProvider {
  id = "arabseed";
  name = "عرب سيد";
  baseUrl = "https://m.myseed.tv";
  isEnabled = true;

  async search(query: string): Promise<UnifiedMediaItem[]> {
    try {
      const items = await searchArabseed(query);
      return items.map((it) => ({
        ...it,
        providerId: this.id,
        providerName: this.name,
      }));
    } catch (err) {
      console.error("[ArabseedProvider] search error:", err);
      return [];
    }
  }

  async getCategory(slug: string, page = 1): Promise<UnifiedMediaItem[]> {
    try {
      const items = await getArabseedCategory(slug, page);
      return items.map((it) => ({
        ...it,
        providerId: this.id,
        providerName: this.name,
      }));
    } catch (err) {
      console.error(`[ArabseedProvider] getCategory error (${slug}):`, err);
      return [];
    }
  }

  async getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null> {
    try {
      const detail = await getArabseedDetail(pageUrl);
      if (!detail) return null;

      const servers: UnifiedServer[] = detail.servers.map((s) => ({
        name: `[عرب سيد] ${s.name}`,
        embedUrl: s.embedUrl,
        providerId: this.id,
        isHls: true,
      }));

      return {
        item: {
          ...detail.item,
          providerId: this.id,
          providerName: this.name,
        },
        overview: detail.overview,
        cast: detail.cast,
        servers,
        seasons: detail.seasons,
        episodes: detail.episodes,
        proxiedStream: detail.proxiedStream,
      };
    } catch (err) {
      console.error("[ArabseedProvider] getDetail error:", err);
      return null;
    }
  }

  async resolveStream(serverUrl: string): Promise<string | null> {
    try {
      const direct = await resolveDirectStream(serverUrl);
      return direct || null;
    } catch (err) {
      console.error("[ArabseedProvider] resolveStream error:", err);
      return null;
    }
  }
}

export const arabseedProvider = new ArabseedProvider();
