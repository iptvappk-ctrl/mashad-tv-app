/**
 * title-matcher.ts
 * High-precision bilingual title matching and stop-word elimination.
 * Prevents media mismatches across all movie and series categories (Arabic, Foreign, Turkish, Korean, Anime, Classic).
 */

export const TITLE_STOP_WORDS = new Set<string>([
  "موسم", "الموسم", "حلقة", "الحلقة", "جزء", "الجزء", "مسلسل", "فيلم", "سلسلة",
  "انمي", "أنمي", "كرتون", "برنامج", "مترجم", "مدبلج", "اونلاين", "مشاهدة", "تحميل",
  "كامل", "كاملة", "حصريا", "حصري", "جديد", "عربي", "اجنبي", "تركي", "كوري", "اسيوي",
  "هندي", "مدبلجة", "مترجمة", "نسخة", "اصلية", "اصلي", "جودة", "عالية", "فيديو", "مشاهده",
  "بجودة", "بالجودة", "عاليه", "بدقة", "والاخيرة", "والاخير",
  "الاول", "الاولى", "الثاني", "الثانية", "الثالث", "الثالثة", "الرابع", "الرابعة",
  "الخامس", "الخامسة", "السادس", "السادسة", "السابع", "السابعة", "الثامن", "الثامنة",
  "التاسع", "التاسعة", "العاشر", "العاشرة", "الاخير", "الاخيرة",
  "season", "episode", "episodes", "ep", "series", "movie", "film", "watch",
  "download", "online", "full", "hd", "fhd", "4k", "1080p", "720p", "480p",
  "bluray", "blueray", "webdl", "web-dl", "web", "dl", "dvd", "rip", "cam",
  "sub", "dub", "arabic", "english", "korean", "turkish", "anime", "action",
  "the", "of", "and", "in", "on", "at", "to", "a", "an", "is", "vs",
  "akwam", "arabseed", "wecima", "fasel", "faselhd", "shahid4u", "cima4u", "egybest",
]);

export interface TitleTokens {
  allTokens: Set<string>;
  latinTokens: Set<string>;
  arabicTokens: Set<string>;
}

export function normalizeArabic(str: string): string {
  if (!str) return "";
  return str
    .toLowerCase()
    .replace(/[أإآ]/g, "ا")
    .replace(/ة/g, "ه")
    .replace(/ى/g, "ي")
    .replace(/[\u064B-\u0652]/g, "") // Diacritics
    .replace(/[^\w\s\u0600-\u06FF]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export function extractTitleTokens(title: string): TitleTokens {
  if (!title) {
    return { allTokens: new Set(), latinTokens: new Set(), arabicTokens: new Set() };
  }

  // Remove 4-digit years like (2024, 2025, 2026) to prevent false mismatches
  const norm = normalizeArabic(title)
    .replace(/(?<!\d)(?:19|20)\d{2}(?!\d)/g, " ")
    .replace(/[^\p{L}\p{N}]+/gu, " ");

  const words = norm.split(/\s+/);
  const allTokens = new Set<string>();
  const latinTokens = new Set<string>();
  const arabicTokens = new Set<string>();
  const latinRegex = /^[a-z0-9]+$/;

  for (const raw of words) {
    const w = raw.trim();
    if (w.length < 2 || !isNaN(Number(w)) || TITLE_STOP_WORDS.has(w)) continue;
    // Prefix check for Arabic letters (و, ب)
    if ((w.startsWith("ب") || w.startsWith("و")) && w.length > 2 && TITLE_STOP_WORDS.has(w.substring(1))) {
      continue;
    }

    allTokens.add(w);
    if (latinRegex.test(w)) {
      latinTokens.add(w);
    } else {
      arabicTokens.add(w);
    }
  }

  return { allTokens, latinTokens, arabicTokens };
}

/**
 * Strict bilingual comparison.
 * Returns true ONLY if candidate title actually matches the target title tokens.
 */
export function titlesStrictlyMatch(target: string, candidate: string): boolean {
  if (!target || !candidate) return false;
  const t = extractTitleTokens(target);
  const c = extractTitleTokens(candidate);

  if (t.allTokens.size === 0 || c.allTokens.size === 0) return false;

  // 1. Latin tokens matching (Foreign, English, Anime, Korean titles)
  if (t.latinTokens.size > 0 && c.latinTokens.size > 0) {
    const sharedLat = new Set([...t.latinTokens].filter((x) => c.latinTokens.has(x)));
    if (t.latinTokens.size === 1) {
      if (sharedLat.size === 1 && c.latinTokens.size <= 2) return true;
    } else if (t.latinTokens.size === 2) {
      if (sharedLat.size === 2 && c.latinTokens.size <= 3) return true;
      if (sharedLat.size === 1 && (t.arabicTokens.size > 0 || c.arabicTokens.size > 0)) {
        // Fall through to Arabic token check
      }
    } else {
      const matchRatio = sharedLat.size / t.latinTokens.size;
      if (matchRatio >= 0.6) return true;
    }
  }

  // 2. Arabic tokens matching (Arabic, Turkish, Dubbed titles)
  if (t.arabicTokens.size > 0 && c.arabicTokens.size > 0) {
    const sharedAr = new Set([...t.arabicTokens].filter((x) => c.arabicTokens.has(x)));
    if (t.arabicTokens.size === 1) {
      if (sharedAr.size === 1 && c.arabicTokens.size <= 3) return true;
    } else if (t.arabicTokens.size === 2) {
      if (sharedAr.size === 2 && c.arabicTokens.size <= 4) return true;
      if (sharedAr.size === 1 && c.arabicTokens.size <= 2) return true;
    } else {
      const matchRatio = sharedAr.size / t.arabicTokens.size;
      if (matchRatio >= 0.5) return true;
    }
  }

  // 3. Fallback: Check overall union match if bilingual (e.g. Heart of the Beast / قلب الوحش)
  const sharedAll = new Set([...t.allTokens].filter((x) => c.allTokens.has(x)));
  if (sharedAll.size >= 2) return true;

  return false;
}
