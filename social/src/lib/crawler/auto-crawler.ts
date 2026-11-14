/**
 * 🕷️ Real-Time Auto-Crawler Engine
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * نظام الرصد والسحب الآلي لحظياً:
 * 1. فحص ومراقبة صفحات "أضيف حديثاً" من كافة المواقع كل فترة زمنية محددة
 * 2. اكتشاف أي حلقة جديدة فور نزولها وتمريرها لنظام الربط التلقائي (Smart Linker)
 * 3. تحديث قائمة "جديد اليوم" والأعمال الأكثر مشاهدة تلقائياً في التطبيق
 * 4. نظام إشعارات فوري للأفلام والحلقات الجديدة (Push Notification Hook)
 */

import { UnifiedMediaItem } from "../providers/types";
import { fetchHtml, makeId } from "../providers/utils";
import { smartMediaLinker } from "../linker/smart-media-linker";
import * as cheerio from "cheerio";

export interface CrawledReleaseEvent {
  id: string;
  title: string;
  cleanTitle: string;
  type: "episode" | "movie";
  seriesTitle?: string;
  seasonNumber?: number;
  episodeNumber?: number;
  partNumber?: number;
  poster: string;
  pageUrl: string;
  provider: string;
  detectedAt: Date;
}

export class AutoCrawlerEngine {
  private isRunning = false;
  private intervalTimer: NodeJS.Timeout | null = null;
  private seenUrls = new Set<string>();
  private latestReleases: CrawledReleaseEvent[] = [];
  private onNewReleaseCallbacks: ((item: CrawledReleaseEvent) => void)[] = [];

  // المصادر التي يتم رصدها دورياً للحصول على أحدث ما نزل
  private feedUrls = [
    { name: "عرب سيد", url: "https://arabseed.health/", provider: "arabseed" },
    { name: "وي سيما", url: "https://myweecima.casa/", provider: "wecima" },
    { name: "أكوام", url: "https://akw.cam/", provider: "akwam" },
    { name: "فاصل إعلاني", url: "https://wb49584.fasel4k.site/", provider: "faselhd" },
    { name: "أنمي بلكم", url: "https://animeblkom.net/", provider: "animeblkom" },
    { name: "تورك فان", url: "https://turkfan.net/", provider: "turkfan" },
  ];

  /**
   * تشغيل الرصد الآلي كل X دقيقة (افتراضياً كل 3 دقائق)
   */
  start(intervalMs = 3 * 60 * 1000) {
    if (this.isRunning) return;
    this.isRunning = true;
    console.log(`[AutoCrawler] Started periodic crawler every ${intervalMs / 1000}s`);

    // تنفيذ أول دورة فحص فوراً
    this.crawlAllFeeds().catch(() => {});

    // جدولة الفحص الدوري
    this.intervalTimer = setInterval(() => {
      this.crawlAllFeeds().catch(() => {});
    }, intervalMs);
  }

  /**
   * إيقاف الرصد
   */
  stop() {
    if (this.intervalTimer) {
      clearInterval(this.intervalTimer);
      this.intervalTimer = null;
    }
    this.isRunning = false;
    console.log("[AutoCrawler] Stopped");
  }

  /**
   * تسجيل دالة استماع لتنبيه المشغل أو إرسال إشعار للمستخدم
   */
  onNewRelease(callback: (item: CrawledReleaseEvent) => void) {
    this.onNewReleaseCallbacks.push(callback);
  }

  /**
   * جلب قائمة الحلقات والأفلام التي تم رصدها حديثاً
   */
  getLatestReleases(limit = 50): CrawledReleaseEvent[] {
    return this.latestReleases.slice(0, limit);
  }

  /**
   * تنفيذ دورة فحص شاملة لكافة المواقع بالتوازي
   */
  async crawlAllFeeds(): Promise<CrawledReleaseEvent[]> {
    const newlyDiscovered: CrawledReleaseEvent[] = [];

    await Promise.allSettled(
      this.feedUrls.map(async (feed) => {
        try {
          const html = await fetchHtml(feed.url, feed.url, 4000);
          if (!html) return;
          const $ = cheerio.load(html);

          $("article a, .MovieBlock a, .GridItem a, .Thumb--GridItem a, .film-box a").each((_, el) => {
            const a = $(el);
            const href = a.attr("href") || "";
            if (!href.startsWith("http")) return;

            // إذا كان الرابط قد تمت معالجته مسبقاً، نتخطاه لتوفير الموارد
            if (this.seenUrls.has(href)) return;

            const title =
              a.find("h3, h2, .title, .BlockTitle, strong").first().text().trim() ||
              a.attr("title") ||
              "";
            if (!title || title.length < 3) return;

            let poster = a.find("img").attr("data-src") || a.find("img").attr("src") || "";
            if (poster.startsWith("//")) poster = "https:" + poster;

            this.seenUrls.add(href);

            // استخدام المحلل الذكي لتحديد نوع المادة ورقم الحلقة والموسم
            const parsed = smartMediaLinker.parseTitle(title);

            const releaseEvent: CrawledReleaseEvent = {
              id: makeId("rel", href),
              title: title.trim(),
              cleanTitle: parsed.cleanTitle,
              type: parsed.isSeries ? "episode" : "movie",
              seriesTitle: parsed.seriesTitle,
              seasonNumber: parsed.seasonNumber,
              episodeNumber: parsed.episodeNumber,
              partNumber: parsed.partNumber,
              poster,
              pageUrl: href,
              provider: feed.provider,
              detectedAt: new Date(),
            };

            newlyDiscovered.push(releaseEvent);

            // ربط المادة آلياً في شجرة المسلسلات وسلاسل الأفلام
            smartMediaLinker.registerItem({
              id: releaseEvent.id,
              title: releaseEvent.title,
              poster: releaseEvent.poster,
              pageUrl: releaseEvent.pageUrl,
              category: releaseEvent.type === "episode" ? "مسلسل" : "فيلم",
              isSeries: releaseEvent.type === "episode",
              providerId: releaseEvent.provider,
              providerName: feed.name,
            });
          });
        } catch {}
      })
    );

    if (newlyDiscovered.length > 0) {
      console.log(`[AutoCrawler] Discovered ${newlyDiscovered.length} brand new media items!`);
      this.latestReleases.unshift(...newlyDiscovered);

      // تقليم القائمة لأحدث 200 عنصر
      if (this.latestReleases.length > 200) {
        this.latestReleases = this.latestReleases.slice(0, 200);
      }

      // إطلاق تنبيهات الإشعارات
      for (const item of newlyDiscovered) {
        for (const cb of this.onNewReleaseCallbacks) {
          try {
            cb(item);
          } catch {}
        }
      }
    }

    return newlyDiscovered;
  }
}

export const autoCrawler = new AutoCrawlerEngine();
