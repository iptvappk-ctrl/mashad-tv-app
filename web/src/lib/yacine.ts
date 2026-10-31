export interface YacineCategory {
  id: string | number;
  name: string;
  logo: string;
  childCount: number;
}

export interface YacineChannel {
  id: string | number;
  name: string;
  logo: string;
  isHide: number;
  priority: number;
}

export interface YacineTeam {
  id: string | number;
  name: string;
  logo: string;
}

export interface YacineEvent {
  id: string | number;
  champions: string;
  channel: string;
  startTime: number;
  endTime: number;
  commentary: string;
  team1: YacineTeam;
  team2: YacineTeam;
}

export interface YacineStream {
  name: string;
  url: string;
  userAgent: string;
  referer: string;
  urlType: number;
  headers: Record<string, string>;
}

const HARDCODED_KEY = "c!xZj+N9&G@Ev@vw";
const PKG_NAME = "ver3.ycntivi.off";
const UA = "okhttp/4.11.0";

const HOSTS = [
  "https://def001.ycnapi.com/api",
  "https://deft.yacinelive.com/api",
  "http://def.yacinelive.com/api",
];

// In-memory caches with high TTL
let cachedCategories: YacineCategory[] = [];
let categoriesFetchTime = 0;
const CATEGORIES_TTL = 15 * 60 * 1000; // 15 minutes

const channelCache = new Map<string, { data: YacineChannel[]; time: number }>();
const CHANNEL_TTL = 15 * 60 * 1000;

let cachedEvents: YacineEvent[] = [];
let eventsFetchTime = 0;
const EVENTS_TTL = 45 * 1000; // 45 seconds

const streamCache = new Map<string, { data: YacineStream[]; time: number }>();
const STREAM_TTL = 3 * 60 * 1000; // 3 minutes

function decrypt(body: string, tHeader: string): string {
  const key = Buffer.from(HARDCODED_KEY + tHeader, "utf8");
  const decoded = Buffer.from(body.trim(), "base64");
  const result = Buffer.alloc(decoded.length);
  for (let i = 0; i < decoded.length; i++) {
    result[i] = decoded[i] ^ key[i % key.length];
  }
  return result.toString("utf8");
}

async function fetchFromYacine(endpoint: string): Promise<string> {
  let lastError: Error | null = null;

  for (const host of HOSTS) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3500);

    try {
      const url = `${host}/${endpoint}`;
      const res = await fetch(url, {
        headers: {
          "User-Agent": UA,
          Accept: "application/json",
          "x-android-package": PKG_NAME,
          Connection: "keep-alive",
        },
        signal: controller.signal,
        cache: "no-store",
      });

      if (!res.ok) continue;

      const tHeader = res.headers.get("t") || res.headers.get("T") || "";
      const body = await res.text();
      return decrypt(body, tHeader);
    } catch (err: any) {
      lastError = err;
    } finally {
      clearTimeout(timeoutId);
    }
  }

  throw new Error(`Failed to fetch from Yacine API: ${lastError?.message || "Unknown error"}`);
}

export async function getYacineCategories(force = false): Promise<YacineCategory[]> {
  const now = Date.now();
  if (!force && cachedCategories.length > 0 && now - categoriesFetchTime < CATEGORIES_TTL) {
    return cachedCategories;
  }

  try {
    const raw = await fetchFromYacine("categories");
    const json = JSON.parse(raw);
    const data = Array.isArray(json.data) ? json.data : [];

    cachedCategories = data.map((item: any) => ({
      id: item.id,
      name: item.name || "",
      logo: item.logo || "",
      childCount: item.child_count || 0,
    }));
    categoriesFetchTime = now;
    return cachedCategories;
  } catch (err) {
    console.error("Yacine categories error:", err);
    return cachedCategories;
  }
}

export async function getYacineChannels(categoryId: string | number, force = false): Promise<YacineChannel[]> {
  const catKey = String(categoryId);
  const now = Date.now();
  const cached = channelCache.get(catKey);
  if (!force && cached && now - cached.time < CHANNEL_TTL) {
    return cached.data;
  }

  try {
    const raw = await fetchFromYacine(`categories/${catKey}/channels`);
    const parsed = JSON.parse(raw);
    const data = Array.isArray(parsed) ? parsed : Array.isArray(parsed.data) ? parsed.data : [];

    const channels: YacineChannel[] = data
      .map((item: any) => ({
        id: String(item.id),
        name: item.name || "",
        logo: item.logo || "",
        isHide: item.is_hide || 0,
        priority: item.priority || 0,
      }))
      .filter((c: YacineChannel) => c.isHide === 0)
      .sort((a: YacineChannel, b: YacineChannel) => b.priority - a.priority);

    channelCache.set(catKey, { data: channels, time: now });
    return channels;
  } catch (err) {
    console.error(`Yacine channels error for cat ${catKey}:`, err);
    return cached ? cached.data : [];
  }
}

export async function getYacineEvents(force = false): Promise<YacineEvent[]> {
  const now = Date.now();
  if (!force && cachedEvents.length > 0 && now - eventsFetchTime < EVENTS_TTL) {
    return cachedEvents;
  }

  try {
    const raw = await fetchFromYacine("events");
    const json = JSON.parse(raw);
    const data = Array.isArray(json.data) ? json.data : [];

    cachedEvents = data.map((item: any) => ({
      id: item.id,
      champions: item.champions || "",
      channel: item.channel || "",
      startTime: item.start_time || 0,
      endTime: item.end_time || 0,
      commentary: item.commentary || "",
      team1: {
        id: item.team_1?.id || 0,
        name: item.team_1?.name || "",
        logo: item.team_1?.logo || "",
      },
      team2: {
        id: item.team_2?.id || 0,
        name: item.team_2?.name || "",
        logo: item.team_2?.logo || "",
      },
    }));
    eventsFetchTime = now;
    return cachedEvents;
  } catch (err) {
    console.error("Yacine events error:", err);
    return cachedEvents;
  }
}

export async function getStreamsForChannel(channelId: number | string): Promise<YacineStream[]> {
  const cacheKey = `channel:${channelId}`;
  const now = Date.now();
  const cached = streamCache.get(cacheKey);
  if (cached && now - cached.time < STREAM_TTL) {
    return cached.data;
  }

  try {
    const raw = await fetchFromYacine(`channel/${channelId}`);
    const json = JSON.parse(raw);
    const list = Array.isArray(json.data) ? json.data : [];
    const parsed = parseStreams(list);
    if (parsed.length > 0) {
      streamCache.set(cacheKey, { data: parsed, time: now });
    }
    return parsed;
  } catch (err) {
    console.error(`Yacine channel streams error (${channelId}):`, err);
    return cached ? cached.data : [];
  }
}

export async function getStreamsForEvent(eventId: number | string): Promise<YacineStream[]> {
  const cacheKey = `event:${eventId}`;
  const now = Date.now();
  const cached = streamCache.get(cacheKey);
  if (cached && now - cached.time < STREAM_TTL) {
    return cached.data;
  }

  try {
    const raw = await fetchFromYacine(`event/${eventId}`);
    const json = JSON.parse(raw);
    const list = Array.isArray(json.data) ? json.data : [];
    const parsed = parseStreams(list);
    if (parsed.length > 0) {
      streamCache.set(cacheKey, { data: parsed, time: now });
    }
    return parsed;
  } catch (err) {
    console.error(`Yacine event streams error (${eventId}):`, err);
    return cached ? cached.data : [];
  }
}

function parseStreams(arr: any[]): YacineStream[] {
  return arr
    .map((item: any) => {
      const headers = item.headers || {};
      const ua = headers["User-Agent"] || item.user_agent || UA;
      const playbackHeaderNames = new Set(["user-agent", "referer", "origin", "accept", "accept-language"]);
      const safeHeaders: Record<string, string> = Object.fromEntries(
        Object.entries(headers as Record<string, unknown>)
          .filter(([key, value]) => playbackHeaderNames.has(key.toLowerCase()) && typeof value === "string" && !/[\r\n]/.test(key + value))
          .map(([key, value]) => [key, String(value)]),
      ) as Record<string, string>;
      safeHeaders["User-Agent"] = ua;
      if (item.referer) safeHeaders.Referer = item.referer;
      return {
        name: item.name || "HD",
        url: item.url || "",
        userAgent: ua,
        referer: item.referer || "",
        urlType: item.url_type || 3,
        headers: safeHeaders,
      };
    })
    .filter((s: YacineStream) => Boolean(s.url));
}
