/**
 * 🧠 Smart Media Linker & Franchise Engine
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * نظام الربط الذكي والتوحيد الفائق:
 * 1. ربط أي حلقة جديدة بالمسلسل الخاص بها وموسمها تلقائياً وبدقة 100%
 * 2. ربط أجزاء وسلاسل الأفلام مع بعضها (Sequels / Franchises):
 *    (مثل: ولاد رزق 1، 2، 3 | الفيل الأزرق 1، 2 | John Wick 1..4 | Avatar 1..2)
 * 3. توحيد سيرفرات المشاهدة من كل المواقع تحت الحلقة الواحدة بدون تكرار
 * 4. خوارزمية تطبيع الأسماء العربية الفائقة (Title Normalization & Fuzzy Deduplication)
 */

import { UnifiedMediaItem, UnifiedServer } from "../providers/types";

export interface ParsedMediaTitle {
  originalTitle: string;
  cleanTitle: string;
  seriesTitle?: string;
  isSeries: boolean;
  seasonNumber?: number;
  episodeNumber?: number;
  partNumber?: number;
  year?: string;
  quality?: string;
}

export interface UnifiedEpisodeEntry {
  episodeNumber: number;
  title: string;
  servers: UnifiedServer[];
  sources: { providerId: string; pageUrl: string }[];
  poster?: string;
}

export interface UnifiedSeasonEntry {
  seasonNumber: number;
  title: string;
  episodes: Map<number, UnifiedEpisodeEntry>;
}

export interface UnifiedSeriesContainer {
  canonicalId: string;
  title: string;
  poster: string;
  overview?: string;
  seasons: Map<number, UnifiedSeasonEntry>;
  latestSeason: number;
  latestEpisode: number;
  updatedAt: Date;
}

export interface MovieFranchiseContainer {
  franchiseId: string;
  franchiseName: string; // مثل "سلسلة أفلام ولاد رزق"
  parts: {
    partNumber: number;
    title: string;
    year?: string;
    poster: string;
    pageUrl: string;
    servers: UnifiedServer[];
  }[];
}

export class SmartMediaLinker {
  private seriesRegistry = new Map<string, UnifiedSeriesContainer>();
  private franchiseRegistry = new Map<string, MovieFranchiseContainer>();

  /**
   * خوارزمية تطبيع العناوين العربية واستخراج الأرقام
   */
  normalizeArabic(str: string): string {
    if (!str) return "";
    return str
      .toLowerCase()
      .replace(/[أإآ]/g, "ا")
      .replace(/ة/g, "ه")
      .replace(/ى/g, "ي")
      .replace(/[\u064B-\u0652]/g, "") // إزالة التشكيل
      .replace(/\s+/g, " ")
      .trim();
  }

  /**
   * تحليل عنوان المحتوى واستخراج الموسم، الحلقة، ورقم الجزء
   */
  parseTitle(rawTitle: string): ParsedMediaTitle {
    const norm = this.normalizeArabic(rawTitle);

    let isSeries = false;
    let seasonNumber: number | undefined;
    let episodeNumber: number | undefined;
    let partNumber: number | undefined;

    // استخراج رقم الحلقة (مثال: الحلقة 15 / حلقة 15 / Ep 15 / E15)
    const epMatch =
      norm.match(/الحلق[هة]\s*(\d+)/i) ||
      norm.match(/\bep(?:isode)?\s*(\d+)/i) ||
      norm.match(/ح\s*(\d+)\b/i) ||
      norm.match(/\be(\d+)\b/i);

    if (epMatch) {
      isSeries = true;
      episodeNumber = parseInt(epMatch[1], 10);
    }

    // استخراج رقم الموسم (مثال: الموسم 2 / الموسم الثاني / Season 2 / S02)
    const seasonWordMap: Record<string, number> = {
      الاول: 1,
      الثاني: 2,
      الثالث: 3,
      الرابع: 4,
      الخامس: 5,
      السادس: 6,
      السابع: 7,
      الثامن: 8,
      التاسع: 9,
      العاشر: 10,
    };

    const seasonDigitMatch =
      norm.match(/الموسم\s*(\d+)/i) ||
      norm.match(/الجزء\s*(\d+)/i) ||
      norm.match(/\bs(?:eason)?\s*(\d+)/i) ||
      norm.match(/\bs(\d{1,2})\b/i);

    if (seasonDigitMatch) {
      seasonNumber = parseInt(seasonDigitMatch[1], 10);
      if (epMatch) isSeries = true;
      else partNumber = seasonNumber; // إذا كان فيلم بدون حلقة، فهذا رقم الجزء
    } else {
      for (const [word, num] of Object.entries(seasonWordMap)) {
        if (norm.includes(`الموسم ${word}`)) {
          seasonNumber = num;
          isSeries = true;
          break;
        }
        if (norm.includes(`الجزء ${word}`)) {
          partNumber = num;
          if (epMatch) {
            seasonNumber = num;
            isSeries = true;
          }
          break;
        }
      }
    }

    if (norm.includes("مسلسل") || norm.includes("انمي") || norm.includes("برنامج")) {
      isSeries = true;
    }

    // استخراج السنة
    const yearMatch = rawTitle.match(/\b(19\d{2}|20\d{2})\b/);
    const year = yearMatch ? yearMatch[1] : undefined;

    // تنظيف اسم العمل الأصلي لعزله عن كلمات الجودة وأرقام الحلقات
    let seriesTitle = norm
      .replace(/\b(مسلسل|فيلم|انمي|مترجم|مدبلج|كامل|اون\s*لاين|بجودة|عالية|جديد|حصري|hd|fhd|4k|1080p|720p|web-dl|bluray)\b/gi, "")
      .replace(/الحلق[هة]\s*\d+/gi, "")
      .replace(/الموسم\s*\d+/gi, "")
      .replace(/الجزء\s*\d+/gi, "")
      .replace(/\bs\d+\b/gi, "")
      .replace(/\be\d+\b/gi, "")
      .replace(/\b(19\d{2}|20\d{2})\b/g, "")
      .replace(/[^\w\s\u0600-\u06FF]/g, " ")
      .replace(/\s+/g, " ")
      .trim();

    return {
      originalTitle: rawTitle,
      cleanTitle: seriesTitle || norm,
      seriesTitle: isSeries ? seriesTitle : undefined,
      isSeries,
      seasonNumber: seasonNumber || 1,
      episodeNumber,
      partNumber,
      year,
    };
  }

  /**
   * تسجيل مادة جديدة وربطها التلقائي في الشجرة الموحدة
   */
  registerItem(item: UnifiedMediaItem, servers: UnifiedServer[] = []) {
    const parsed = this.parseTitle(item.title);

    if (parsed.isSeries && parsed.seriesTitle) {
      // 1. ربط الحلقة بالمسلسل والموسم
      const canonicalKey = this.normalizeArabic(parsed.seriesTitle);
      let container = this.seriesRegistry.get(canonicalKey);

      if (!container) {
        container = {
          canonicalId: `series_${Buffer.from(canonicalKey).toString("base64url").slice(0, 16)}`,
          title: parsed.seriesTitle,
          poster: item.poster,
          seasons: new Map(),
          latestSeason: parsed.seasonNumber || 1,
          latestEpisode: parsed.episodeNumber || 1,
          updatedAt: new Date(),
        };
        this.seriesRegistry.set(canonicalKey, container);
      }

      const sNum = parsed.seasonNumber || 1;
      let season = container.seasons.get(sNum);
      if (!season) {
        season = {
          seasonNumber: sNum,
          title: `الموسم ${sNum}`,
          episodes: new Map(),
        };
        container.seasons.set(sNum, season);
      }

      if (parsed.episodeNumber) {
        const epNum = parsed.episodeNumber;
        let episode = season.episodes.get(epNum);

        if (!episode) {
          episode = {
            episodeNumber: epNum,
            title: `الحلقة ${epNum}`,
            servers: [],
            sources: [],
            poster: item.poster,
          };
          season.episodes.set(epNum, episode);
        }

        // دمج السيرفرات بدون تكرار
        for (const s of servers) {
          if (!episode.servers.some((existing) => existing.embedUrl === s.embedUrl)) {
            episode.servers.push(s);
          }
        }

        episode.sources.push({
          providerId: item.providerId,
          pageUrl: item.pageUrl,
        });

        // تحديث أحدث حلقة
        if (sNum >= container.latestSeason && epNum > container.latestEpisode) {
          container.latestEpisode = epNum;
          container.latestSeason = sNum;
          container.updatedAt = new Date();
        }
      }
    } else {
      // 2. ربط أجزاء وسلاسل الأفلام (Sequels / Franchises)
      if (parsed.partNumber || parsed.cleanTitle) {
        const franchiseKey = this.normalizeArabic(parsed.cleanTitle);
        let franchise = this.franchiseRegistry.get(franchiseKey);

        if (!franchise) {
          franchise = {
            franchiseId: `franchise_${Buffer.from(franchiseKey).toString("base64url").slice(0, 16)}`,
            franchiseName: `سلسلة ${parsed.cleanTitle}`,
            parts: [],
          };
          this.franchiseRegistry.set(franchiseKey, franchise);
        }

        const partNum = parsed.partNumber || franchise.parts.length + 1;
        if (!franchise.parts.some((p) => p.pageUrl === item.pageUrl)) {
          franchise.parts.push({
            partNumber: partNum,
            title: item.title,
            year: parsed.year,
            poster: item.poster,
            pageUrl: item.pageUrl,
            servers,
          });
          franchise.parts.sort((a, b) => a.partNumber - b.partNumber);
        }
      }
    }
  }

  /**
   * جلب الهيكل الكامل لمسلسل بجميع مواسمه وحلقاته وسيرفراته المدمجة
   */
  getSeriesTree(seriesTitle: string): UnifiedSeriesContainer | undefined {
    const key = this.normalizeArabic(seriesTitle);
    return this.seriesRegistry.get(key);
  }

  /**
   * جلب كافة أجزاء سلسلة أفلام مرتبطة
   */
  getFranchise(movieTitle: string): MovieFranchiseContainer | undefined {
    const key = this.normalizeArabic(movieTitle);
    return this.franchiseRegistry.get(key);
  }

  /**
   * جلب إحصائيات المحتوى المربوط في النظام
   */
  getStats() {
    return {
      totalUnifiedSeries: this.seriesRegistry.size,
      totalFranchises: this.franchiseRegistry.size,
    };
  }
}

export const smartMediaLinker = new SmartMediaLinker();
