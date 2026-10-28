"use strict";

const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const { BetaAnalyticsDataClient } = require("@google-analytics/data");
require("dotenv").config({ path: path.join(__dirname, ".env") });

const HOST = process.env.DASHBOARD_HOST || "127.0.0.1";
const PORT = Number(process.env.DASHBOARD_PORT || 4040);
const HTML_PATH = path.join(__dirname, "index.html");
const RANGES = new Set(["7d", "30d", "90d"]);
const CACHE_MS = 60_000;
let analyticsClient;
const statsCache = new Map();

function getAnalyticsClient() {
  if (!analyticsClient) analyticsClient = new BetaAnalyticsDataClient();
  return analyticsClient;
}

function value(row, index) {
  return row?.metricValues?.[index]?.value ?? "0";
}

function dimension(row, index) {
  return row?.dimensionValues?.[index]?.value ?? "";
}

function int(valueToParse) {
  const parsed = Number.parseInt(valueToParse ?? "0", 10);
  return Number.isFinite(parsed) ? parsed : 0;
}

function rangeDates(range) {
  if (range === "7d") return { startDate: "6daysAgo", endDate: "today" };
  if (range === "90d") return { startDate: "89daysAgo", endDate: "today" };
  return { startDate: "29daysAgo", endDate: "today" };
}

function eventFilter(eventName) {
  return {
    filter: {
      fieldName: "eventName",
      stringFilter: { matchType: "EXACT", value: eventName },
    },
  };
}

async function report(client, property, request) {
  const [result] = await client.runReport({ property: `properties/${property}`, ...request });
  return result;
}

async function realtime(client, property) {
  const [result] = await client.runRealtimeReport({
    property: `properties/${property}`,
    metrics: [{ name: "activeUsers" }],
  });
  return int(value(result.rows?.[0], 0));
}

async function buildStats(range) {
  const property = (process.env.GA_PROPERTY_ID || "").trim();
  if (!property) {
    return {
      configured: false,
      setup: "missing_property",
      message: "أضف GA_PROPERTY_ID إلى إعدادات لوحة الإدارة لبدء قراءة تقارير Analytics.",
    };
  }

  const credentialsPath = (process.env.GOOGLE_APPLICATION_CREDENTIALS || "").trim();
  if (credentialsPath && !fs.existsSync(credentialsPath)) {
    return {
      configured: false,
      setup: "missing_credentials",
      message: "ملف اعتماد Google Analytics غير موجود في المسار المحلي المحدد. احفظ ملف JSON خارج المشروع ثم أعد تشغيل لوحة الإدارة.",
    };
  }

  const client = getAnalyticsClient();
  const dates = rangeDates(range);
  const base = { dateRanges: [dates] };

  const [live, summary, events, daily, platforms, devices, countries, topContent] = await Promise.all([
    realtime(client, property),
    report(client, property, { ...base, metrics: [{ name: "activeUsers" }, { name: "sessions" }, { name: "eventCount" }] }),
    report(client, property, {
      ...base,
      dimensions: [{ name: "eventName" }],
      metrics: [{ name: "eventCount" }],
      limit: 100,
    }),
    report(client, property, {
      ...base,
      dimensions: [{ name: "date" }],
      metrics: [{ name: "sessions" }, { name: "eventCount" }],
      orderBys: [{ dimension: { dimensionName: "date" } }],
      limit: 100,
    }),
    report(client, property, {
      ...base,
      dimensions: [{ name: "platform" }],
      metrics: [{ name: "activeUsers" }],
      limit: 10,
    }),
    report(client, property, {
      ...base,
      dimensions: [{ name: "deviceCategory" }],
      metrics: [{ name: "activeUsers" }],
      limit: 10,
    }),
    report(client, property, {
      ...base,
      dimensions: [{ name: "country" }],
      metrics: [{ name: "activeUsers" }],
      orderBys: [{ metric: { metricName: "activeUsers" }, desc: true }],
      limit: 8,
    }),
    report(client, property, {
      ...base,
      dimensions: [{ name: "customEvent:content_title" }],
      metrics: [{ name: "eventCount" }],
      dimensionFilter: eventFilter("playback_start"),
      orderBys: [{ metric: { metricName: "eventCount" }, desc: true }],
      limit: 8,
    }).catch((error) => {
      // Custom dimensions must be registered in the GA4 property before they can be queried.
      console.warn("Analytics custom content dimension is not ready:", error.message);
      return null;
    }),
  ]);

  const eventCounts = Object.fromEntries(
    (events.rows || []).map((row) => [dimension(row, 0), int(value(row, 0))]),
  );

  return {
    configured: true,
    range,
    fetchedAt: new Date().toISOString(),
    activeUsersNow: live,
    activeUsers: int(value(summary.rows?.[0], 0)),
    sessions: int(value(summary.rows?.[0], 1)),
    eventCount: int(value(summary.rows?.[0], 2)),
    firstOpens: eventCounts.first_open || 0,
    contentOpens: eventCounts.content_open || 0,
    searches: eventCounts.search || 0,
    playbackStarts: eventCounts.playback_start || 0,
    playbackErrors: eventCounts.playback_error || 0,
    eventCounts,
    daily: (daily.rows || []).map((row) => ({ date: dimension(row, 0), sessions: int(value(row, 0)), events: int(value(row, 1)) })),
    platforms: (platforms.rows || []).map((row) => ({ name: dimension(row, 0), users: int(value(row, 0)) })),
    devices: (devices.rows || []).map((row) => ({ name: dimension(row, 0), users: int(value(row, 0)) })),
    countries: (countries.rows || []).map((row) => ({ name: dimension(row, 0), users: int(value(row, 0)) })),
    topContentAvailable: Boolean(topContent),
    topContent: (topContent?.rows || []).map((row) => ({ title: dimension(row, 0), plays: int(value(row, 0)) })),
    privacy: {
      searchTermsCollected: false,
      userLevelSessionListAvailable: false,
      firstOpenIsDownloadCount: false,
    },
  };
}

function sendJson(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "no-referrer",
  });
  res.end(JSON.stringify(body));
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url || "/", `http://${req.headers.host || `${HOST}:${PORT}`}`);

  if (req.method !== "GET") return sendJson(res, 405, { error: "Method not allowed" });

  if (url.pathname === "/" || url.pathname === "/index.html") {
    if (!fs.existsSync(HTML_PATH)) return sendJson(res, 404, { error: "Dashboard file is missing" });
    res.writeHead(200, {
      "Content-Type": "text/html; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
      "Referrer-Policy": "no-referrer",
      "X-Frame-Options": "DENY",
    });
    return res.end(fs.readFileSync(HTML_PATH));
  }

  if (url.pathname === "/api/health") {
    const credentialsPath = (process.env.GOOGLE_APPLICATION_CREDENTIALS || "").trim();
    return sendJson(res, 200, {
      ok: true,
      configured: Boolean((process.env.GA_PROPERTY_ID || "").trim()),
      credentialsConfigured: credentialsPath ? fs.existsSync(credentialsPath) : null,
      host: HOST,
    });
  }

  if (url.pathname === "/api/stats") {
    const requestedRange = url.searchParams.get("range") || "30d";
    const range = RANGES.has(requestedRange) ? requestedRange : "30d";
    const cached = statsCache.get(range);
    if (cached && Date.now() - cached.time < CACHE_MS) return sendJson(res, 200, cached.data);

    try {
      const data = await buildStats(range);
      if (data.configured) statsCache.set(range, { time: Date.now(), data });
      return sendJson(res, 200, data);
    } catch (error) {
      console.error("Google Analytics Data API request failed:", error);
      return sendJson(res, 503, {
        configured: true,
        setup: "analytics_access",
        message: "تعذر جلب التقرير. راجع صلاحية حساب الخدمة على خاصية Analytics وتفعيل Data API.",
      });
    }
  }

  return sendJson(res, 404, { error: "Not found" });
});

server.listen(PORT, HOST, () => {
  console.log(`MadarTV analytics dashboard: http://${HOST}:${PORT}`);
  if (!process.env.GA_PROPERTY_ID) console.log("Set GA_PROPERTY_ID to connect a real Analytics property.");
});
