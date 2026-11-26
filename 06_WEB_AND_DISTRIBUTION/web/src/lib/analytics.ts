"use client";

import type { Analytics } from "firebase/analytics";

const CLOUD_TELEMETRY_API = "https://dashboard-api-indol-eight.vercel.app";
const STATUS_CDN_API = "https://dashboard-api-indol-eight.vercel.app/api/status";

// ─── 🔐 Kill Switch Checker (called from layout/page on mount) ───────────────
// Uses cached CDN status endpoint instead of uncached POST to protect quota.
export async function checkKillSwitch(): Promise<{ allowed: boolean; message?: string }> {
  try {
    const res = await fetch(STATUS_CDN_API, {
      method: "GET",
      headers: { Accept: "application/json" },
      signal: AbortSignal.timeout(4000),
    });
    if (!res.ok) return { allowed: true }; // Server error → fail open
    const data = await res.json();
    const status = String(data?.status || "active").toLowerCase();
    const comps = data?.components || {};
    if (
      status === "frozen" ||
      status === "wiped" ||
      comps.web_player_enabled === false ||
      comps.website_enabled === false
    ) {
      return { allowed: false, message: data.message || "الموقع متوقف مؤقتاً." };
    }
    return { allowed: true };
  } catch {
    return { allowed: true }; // No internet → fail open
  }
}

let analyticsPromise: Promise<Analytics | null> | null = null;
let currentSessionId: string | null = null;
let heartbeatInterval: any = null;
let isTelemetryInitialized = false;

function getAnalyticsClient(): Promise<Analytics | null> {
  if (analyticsPromise) return analyticsPromise;
  analyticsPromise = (async () => {
    if (typeof window === "undefined") return null;
    const config = {
      apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
      authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
      projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
      storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
      messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
      appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
      measurementId: process.env.NEXT_PUBLIC_FIREBASE_MEASUREMENT_ID,
    };
    if (!config.apiKey || !config.projectId || !config.appId || !config.measurementId) return null;

    try {
      const [appSdk, analyticsSdk] = await Promise.all([
        import("firebase/app"),
        import("firebase/analytics"),
      ]);
      if (!(await analyticsSdk.isSupported())) return null;
      const app = appSdk.getApps().find((candidate) => candidate.name === "[DEFAULT]")
        ?? appSdk.initializeApp(config);
      return analyticsSdk.getAnalytics(app);
    } catch {
      return null;
    }
  })();
  return analyticsPromise;
}

async function logFirebaseEvent(name: string, params: Record<string, string | number>) {
  try {
    const analytics = await getAnalyticsClient();
    if (!analytics) return;
    const { logEvent: firebaseLogEvent } = await import("firebase/analytics");
    firebaseLogEvent(analytics, name, params);
  } catch {
    // Fail silent
  }
}

// ─── Browser / Device Helpers ────────────────────────────────────────────────

function detectBrowser(): string {
  if (typeof window === "undefined" || !navigator?.userAgent) return "Web Browser";
  const ua = navigator.userAgent;
  if (ua.includes("Edg/")) return "Microsoft Edge";
  if (ua.includes("OPR/") || ua.includes("Opera")) return "Opera";
  if (ua.includes("Chrome") && !ua.includes("Chromium")) return "Google Chrome";
  if (ua.includes("Safari") && !ua.includes("Chrome")) return "Apple Safari";
  if (ua.includes("Firefox")) return "Mozilla Firefox";
  return "Web Browser";
}

function detectOS(): string {
  if (typeof window === "undefined" || !navigator?.userAgent) return "Web";
  const ua = navigator.userAgent;
  if (ua.includes("Win")) return "Windows";
  if (ua.includes("Mac")) return ua.includes("iPhone") || ua.includes("iPad") ? "iOS" : "macOS";
  if (ua.includes("Android")) return "Android Web";
  if (ua.includes("Linux")) return "Linux";
  return "Web OS";
}

function getOrCreateWebDeviceId(): string {
  if (typeof window === "undefined") return "web_anonymous";
  try {
    let id = localStorage.getItem("zaqzouka_web_uid");
    if (!id) {
      id = "web_" + Math.random().toString(36).substring(2, 10) + "_" + Date.now().toString(36);
      localStorage.setItem("zaqzouka_web_uid", id);
    }
    return id;
  } catch {
    return "web_temp_" + Math.random().toString(36).substring(2, 8);
  }
}

// ─── Live Cloud Telemetry Engine ─────────────────────────────────────────────

export async function initWebTelemetry(): Promise<void> {
  if (typeof window === "undefined" || isTelemetryInitialized) return;
  isTelemetryInitialized = true;

  try {
    const deviceId = getOrCreateWebDeviceId();
    const browser = detectBrowser();
    const os = detectOS();
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || "Africa/Cairo";

    let city = "طنطا";
    let region = "الغربية";
    let isp = "WE / TE Data";
    let location = "طنطا، الغربية، مصر (EG)";

    try {
      const geoRes = await fetch("https://ipapi.co/json/", { signal: AbortSignal.timeout(2000) });
      if (geoRes.ok) {
        const geoData = await geoRes.json();
        if (geoData?.city) {
          city = geoData.city === "Tanta" ? "طنطا" : (geoData.city === "Cairo" ? "القاهرة" : geoData.city);
          region = geoData.region === "Gharbia" ? "الغربية" : (geoData.region || "");
          isp = geoData.org || isp;
          location = `${city}، ${region ? region + "، " : ""}مصر (${geoData.country_code || "EG"})`;
        }
      }
    } catch {
      // Fallback to local default
    }

    const payload = {
      deviceId,
      device: `${browser} (${os})`,
      deviceType: "web",
      platform: "web",
      browser,
      osVersion: os,
      countryCode: "EG",
      city,
      region,
      location,
      isp,
      timezone,
      currentSection: "الموقع الإلكتروني 🌐",
      isNewInstall: false,
    };

    const res = await fetch(`${CLOUD_TELEMETRY_API}/api/telemetry/session/start`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (res.ok) {
      const data = await res.json();
      if (data?.sessionId) {
        currentSessionId = data.sessionId;
      }
    }
  } catch {
    // Fail safe
  }

  // Heartbeat every 3 minutes when tab is visible to keep quota low
  if (!heartbeatInterval && typeof window !== "undefined") {
    heartbeatInterval = setInterval(async () => {
      if (!currentSessionId || document.visibilityState !== "visible") return;
      try {
        await fetch(`${CLOUD_TELEMETRY_API}/api/telemetry/session/heartbeat`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            sessionId: currentSessionId,
            currentSection: "الموقع الإلكتروني 🌐",
          }),
        });
      } catch {
        // Fail safe
      }
    }, 180000);

    // End session gracefully on page unload
    window.addEventListener("beforeunload", () => {
      if (!currentSessionId) return;
      const endUrl = `${CLOUD_TELEMETRY_API}/api/telemetry/session/end`;
      const data = JSON.stringify({ sessionId: currentSessionId });
      if (navigator.sendBeacon) {
        navigator.sendBeacon(endUrl, data);
      } else {
        fetch(endUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: data,
          keepalive: true,
        }).catch(() => {});
      }
    });
  }
}

// ─── Event Tracking ─────────────────────────────────────────────────────────

export function trackContentOpen(title: string, isSeries: boolean) {
  void logFirebaseEvent("content_open", {
    content_title: title.slice(0, 100),
    content_type: isSeries ? "series" : "movie",
  });

  if (currentSessionId && typeof window !== "undefined") {
    fetch(`${CLOUD_TELEMETRY_API}/api/telemetry/action`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        sessionId: currentSessionId,
        section: isSeries ? "مسلسلات (ويب) 🌐" : "أفلام (ويب) 🌐",
        content: title.slice(0, 100),
      }),
    }).catch(() => {});
  }
}

export function trackCompletedSearch(query: string) {
  const trimmed = query.trim();
  if (trimmed.length < 2) return;

  const bucket = trimmed.length <= 5 ? "2_5" : trimmed.length <= 10 ? "6_10" : "11_plus";
  void logFirebaseEvent("search", { query_length_bucket: bucket });

  if (currentSessionId && typeof window !== "undefined") {
    fetch(`${CLOUD_TELEMETRY_API}/api/telemetry/search`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        sessionId: currentSessionId,
        query: trimmed,
        found: true,
      }),
    }).catch(() => {});
  }
}

export function trackPlaybackStarted(title: string, isLive: boolean) {
  void logFirebaseEvent("playback_start", {
    content_title: title.slice(0, 100),
    content_type: isLive ? "live" : "cinema",
  });

  if (currentSessionId && typeof window !== "undefined") {
    fetch(`${CLOUD_TELEMETRY_API}/api/telemetry/playback`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        sessionId: currentSessionId,
        title: title.slice(0, 100),
        category: isLive ? "مباريات حية ⚽ (ويب)" : "سينما زقزوقة 🎬 (ويب)",
        event: "start",
      }),
    }).catch(() => {});
  }
}

export function trackPlaybackError(isLive: boolean) {
  void logFirebaseEvent("playback_error", { content_type: isLive ? "live" : "cinema" });

  if (currentSessionId && typeof window !== "undefined") {
    fetch(`${CLOUD_TELEMETRY_API}/api/telemetry/playback`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        sessionId: currentSessionId,
        category: isLive ? "مباريات حية ⚽ (ويب)" : "سينما زقزوقة 🎬 (ويب)",
        event: "error",
      }),
    }).catch(() => {});
  }
}
