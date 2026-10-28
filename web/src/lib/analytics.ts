"use client";

import type { Analytics } from "firebase/analytics";

let analyticsPromise: Promise<Analytics | null> | null = null;

function getAnalyticsClient(): Promise<Analytics | null> {
  if (analyticsPromise) return analyticsPromise;
  analyticsPromise = (async () => {
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

async function logEvent(name: string, params: Record<string, string | number>) {
  try {
    const analytics = await getAnalyticsClient();
    if (!analytics) return;
    const { logEvent: firebaseLogEvent } = await import("firebase/analytics");
    firebaseLogEvent(analytics, name, params);
  } catch {
    // Analytics must not interrupt browsing or playback.
  }
}

export function trackContentOpen(title: string, isSeries: boolean) {
  void logEvent("content_open", {
    content_title: title.slice(0, 100),
    content_type: isSeries ? "series" : "movie",
  });
}

/** Only a coarse length category is sent; the searched text stays in the browser. */
export function trackCompletedSearch(query: string) {
  const length = query.trim().length;
  if (length < 2) return;
  const bucket = length <= 5 ? "2_5" : length <= 10 ? "6_10" : "11_plus";
  void logEvent("search", { query_length_bucket: bucket });
}

export function trackPlaybackStarted(title: string, isLive: boolean) {
  void logEvent("playback_start", {
    content_title: title.slice(0, 100),
    content_type: isLive ? "live" : "cinema",
  });
}

export function trackPlaybackError(isLive: boolean) {
  void logEvent("playback_error", { content_type: isLive ? "live" : "cinema" });
}
