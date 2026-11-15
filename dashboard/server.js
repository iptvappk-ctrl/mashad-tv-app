"use strict";

const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
require("dotenv").config({ path: path.join(__dirname, ".env") });

const HOST = process.env.DASHBOARD_HOST || "0.0.0.0";
const PORT = Number(process.env.DASHBOARD_PORT || 4040);
const HTML_PATH = path.join(__dirname, "index.html");
const DB_PATH = path.join(__dirname, "telemetry_db.json");

const COUNTRY_MAP = {
  EG: "مصر EG",
  SA: "السعودية SA",
  AE: "الإمارات AE",
  IQ: "العراق IQ",
  MA: "المغرب MA",
  KW: "الكويت KW",
  DZ: "الجزائر DZ",
  QA: "قطر QA",
  OM: "عمان OM",
  JO: "الأردن JO",
  LB: "لبنان LB",
  TN: "تونس TN",
  SD: "السودان SD",
  LY: "ليبيا LY",
  YE: "اليمن YE",
  SY: "سوريا SY",
  PS: "فلسطين PS",
  BH: "البحرين BH",
  US: "أمريكا US",
  GB: "بريطانيا UK",
  DE: "ألمانيا DE",
  FR: "فرنسا FR",
  TR: "تركيا TR",
  IT: "إيطاليا IT",
  ES: "إسبانيا ES",
  CA: "كندا CA",
  SE: "السويد SE",
  NL: "هولندا NL",
};

// ═══════════════════════════════════════════════════════════════════════
// Persistent Telemetry Database (100% Real Live Data & Analytics)
// ═══════════════════════════════════════════════════════════════════════

function formatClockTime(date) {
  const d = date instanceof Date ? date : new Date(date);
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function initialDatabase() {
  return {
    stats: {
      totalInstalls: 0,
      firstOpens: 0,
      bouncedUsers: 0,
      totalSearches: 0,
      totalPlays: 0,
      playbackErrors: 0,
    },
    sessions: [],
    topContent: [],
    searches: [],
    errors: [],
    activityLog: [],
  };
}

let db = loadDatabase();

function loadDatabase() {
  try {
    if (fs.existsSync(DB_PATH)) {
      const raw = fs.readFileSync(DB_PATH, "utf8");
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === "object") {
        if (!parsed.stats) parsed.stats = initialDatabase().stats;
        if (!Array.isArray(parsed.sessions)) parsed.sessions = [];
        if (!Array.isArray(parsed.topContent)) parsed.topContent = [];
        if (!Array.isArray(parsed.searches)) parsed.searches = [];
        if (!Array.isArray(parsed.errors)) parsed.errors = [];
        if (!Array.isArray(parsed.activityLog)) parsed.activityLog = [];
        return parsed;
      }
    }
  } catch (error) {
    console.warn("Could not load telemetry DB, reinitializing:", error.message);
  }
  const fresh = initialDatabase();
  saveDatabase(fresh);
  return fresh;
}

function saveDatabase(dataToSave = db) {
  try {
    fs.writeFileSync(DB_PATH, JSON.stringify(dataToSave, null, 2), "utf8");
  } catch (error) {
    console.error("Failed to persist telemetry database:", error.message);
  }
}

function logActivity(type, text, icon = "⚡") {
  const item = {
    id: "act_" + Date.now() + "_" + Math.random().toString(36).substring(2, 6),
    type,
    text,
    icon,
    time: formatClockTime(new Date()),
    timestamp: new Date().toISOString(),
  };
  db.activityLog.unshift(item);
  if (db.activityLog.length > 80) db.activityLog.pop();
}

// Check for expired active sessions (no heartbeat for > 3 minutes)
function expireStaleSessions() {
  const now = Date.now();
  let modified = false;
  for (const s of db.sessions) {
    if (s.status === "active") {
      const lastSeenTime = new Date(s.lastSeen || s.openedAt).getTime();
      if (now - lastSeenTime > 3 * 60 * 1000) {
        s.status = "closed";
        s.closeTime = formatClockTime(new Date(lastSeenTime));
        const openedAtTime = new Date(s.openedAt).getTime();
        const minutes = Math.max(1, Math.round((lastSeenTime - openedAtTime) / (60 * 1000)));
        s.durationMinutes = minutes;
        s.durationText = `${minutes} دقيقة`;
        if ((s.actionsCount || 0) <= 0) {
          db.stats.bouncedUsers = (db.stats.bouncedUsers || 0) + 1;
        }
        modified = true;
      }
    }
  }
  if (modified) saveDatabase();
}

// ═══════════════════════════════════════════════════════════════════════
// Advanced Analytics Aggregation Engine
// ═══════════════════════════════════════════════════════════════════════

function computeAnalytics() {
  const totalSessions = db.sessions.length;
  const closedSessions = db.sessions.filter((s) => s.status === "closed");

  // 1. Average Session Duration
  let totalMinutes = 0;
  for (const s of db.sessions) {
    totalMinutes += s.durationMinutes || 1;
  }
  const avgDuration = totalSessions > 0 ? (totalMinutes / totalSessions).toFixed(1) : "0";

  // 2. TV vs Phone Ratio
  let tvCount = 0;
  let phoneCount = 0;
  for (const s of db.sessions) {
    if (s.deviceType === "tv") tvCount++;
    else phoneCount++;
  }
  const tvRatio = totalSessions > 0 ? Math.round((tvCount / totalSessions) * 100) : 0;
  const phoneRatio = totalSessions > 0 ? 100 - tvRatio : 100;

  // 3. Bounce Rate %
  const firstOpens = db.stats.firstOpens || 0;
  const bounced = db.stats.bouncedUsers || 0;
  const bounceRate = firstOpens > 0 ? Math.round((bounced / firstOpens) * 100) : 0;

  // 4. Stream Health %
  const totalPlays = db.stats.totalPlays || 0;
  const errors = db.stats.playbackErrors || 0;
  const streamHealth = (totalPlays + errors) > 0 ? ((totalPlays / (totalPlays + errors)) * 100).toFixed(1) : "100";

  // 5. Device Brands Breakdown
  const brandMap = {};
  for (const s of db.sessions) {
    let brand = "أخرى";
    const dev = (s.device || "").toLowerCase();
    if (dev.includes("xiaomi") || dev.includes("redmi") || dev.includes("poco")) brand = "شاومي (Xiaomi)";
    else if (dev.includes("samsung") || dev.includes("galaxy") || dev.includes("sm-")) brand = "سامسونج (Samsung)";
    else if (dev.includes("realme") || dev.includes("rmx")) brand = "ريلمي (Realme)";
    else if (dev.includes("iphone") || dev.includes("ipad") || dev.includes("apple")) brand = "أبل (Apple)";
    else if (dev.includes("tv") || dev.includes("box") || s.deviceType === "tv") brand = "شاشات وتلفزيونات (TV)";
    else if (dev.includes("huawei") || dev.includes("honor")) brand = "هواوي (Huawei)";
    else if (dev.includes("oppo")) brand = "أوبو (Oppo)";
    else if (dev.includes("google") || dev.includes("pixel")) brand = "جوجل بيكسل (Pixel)";
    else if (dev.includes("android")) brand = "أندرويد عام";

    brandMap[brand] = (brandMap[brand] || 0) + 1;
  }
  const deviceBrands = Object.entries(brandMap)
    .map(([brand, count]) => ({
      brand,
      count,
      percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
    }))
    .sort((a, b) => b.count - a.count);

  // 6. Category Breakdown
  const catMap = {
    "بث مباشر ⚽": 0,
    "أفلام 🎬": 0,
    "مسلسلات 🍿": 0,
    "ملخصات 🎥": 0,
  };
  for (const c of db.topContent) {
    const cat = c.category || "";
    if (cat.includes("بث") || cat.includes("كورة")) catMap["بث مباشر ⚽"] += c.views || 1;
    else if (cat.includes("فيلم") || cat.includes("أفلام")) catMap["أفلام 🎬"] += c.views || 1;
    else if (cat.includes("مسلسل")) catMap["مسلسلات 🍿"] += c.views || 1;
    else if (cat.includes("ملخص")) catMap["ملخصات 🎥"] += c.views || 1;
    else catMap["بث مباشر ⚽"] += c.views || 1;
  }
  const totalCatViews = Object.values(catMap).reduce((a, b) => a + b, 0);
  const categoryShare = Object.entries(catMap).map(([name, views]) => ({
    name,
    views,
    percent: totalCatViews > 0 ? Math.round((views / totalCatViews) * 100) : 25,
  }));

  // 7. Countries Breakdown
  const countryCounts = {};
  for (const s of db.sessions) {
    const c = s.country || "مصر EG";
    countryCounts[c] = (countryCounts[c] || 0) + 1;
  }
  const topCountries = Object.entries(countryCounts)
    .map(([name, count]) => ({
      name,
      count,
      percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
    }))
    .sort((a, b) => b.count - a.count);

  // 8. Hourly Traffic Activity (0 to 23 hours)
  const hourlyTraffic = Array(24).fill(0);
  for (const s of db.sessions) {
    if (s.openedAt) {
      const h = new Date(s.openedAt).getHours();
      if (h >= 0 && h < 24) hourlyTraffic[h] += 1;
    }
  }

  // 9. OS Version Breakdown
  const osMap = {};
  for (const s of db.sessions) {
    const os = s.osVersion || "Android 14";
    const cleanOs = os.split("(")[0].trim();
    osMap[cleanOs] = (osMap[cleanOs] || 0) + 1;
  }
  const osList = Object.entries(osMap)
    .map(([os, count]) => ({
      os,
      count,
      percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
    }))
    .sort((a, b) => b.count - a.count);

  return {
    avgDuration,
    tvRatio,
    phoneRatio,
    bounceRate,
    streamHealth,
    deviceBrands,
    categoryShare,
    topCountries,
    hourlyTraffic,
    osList,
  };
}

// ═══════════════════════════════════════════════════════════════════════
// HTTP Helpers
// ═══════════════════════════════════════════════════════════════════════

function sendJson(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS, DELETE",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
  });
  res.end(JSON.stringify(body));
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", (chunk) => {
      raw += chunk;
      if (raw.length > 512 * 1024) reject(new Error("Body too large"));
    });
    req.on("end", () => {
      try {
        resolve(raw ? JSON.parse(raw) : {});
      } catch (err) {
        resolve({});
      }
    });
    req.on("error", reject);
  });
}

// ═══════════════════════════════════════════════════════════════════════
// Server Implementation
// ═══════════════════════════════════════════════════════════════════════

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url || "/", `http://${req.headers.host || `${HOST}:${PORT}`}`);

  if (req.method === "OPTIONS") {
    res.writeHead(204, {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS, DELETE",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
    });
    return res.end();
  }

  // Serve UI
  if ((req.method === "GET" || req.method === "HEAD") && (url.pathname === "/" || url.pathname === "/index.html")) {
    if (!fs.existsSync(HTML_PATH)) return sendJson(res, 404, { error: "Dashboard HTML missing" });
    res.writeHead(200, {
      "Content-Type": "text/html; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
    });
    if (req.method === "HEAD") return res.end();
    return res.end(fs.readFileSync(HTML_PATH));
  }

  // Health check
  if (url.pathname === "/api/health") {
    expireStaleSessions();
    return sendJson(res, 200, {
      status: "online",
      serverTime: new Date().toISOString(),
      activeSessions: db.sessions.filter((s) => s.status === "active").length,
      totalTrackedSessions: db.sessions.length,
    });
  }

  // Stats & Deep Analytics for Dashboard UI
  if (req.method === "GET" && url.pathname === "/api/stats") {
    expireStaleSessions();
    const activeSessionsCount = db.sessions.filter((s) => s.status === "active").length;
    const analytics = computeAnalytics();

    return sendJson(res, 200, {
      ok: true,
      updatedAt: new Date().toISOString(),
      kpis: {
        activeNow: activeSessionsCount,
        firstOpens: db.stats.firstOpens || 0,
        totalInstalls: db.stats.totalInstalls || 0,
        bouncedUsers: db.stats.bouncedUsers || 0,
        bounceRate: analytics.bounceRate,
        totalSearches: db.stats.totalSearches || 0,
        totalPlays: db.stats.totalPlays || 0,
        playbackErrors: db.stats.playbackErrors || 0,
        streamHealth: analytics.streamHealth,
        avgDurationMinutes: analytics.avgDuration,
        tvRatio: analytics.tvRatio,
        phoneRatio: analytics.phoneRatio,
      },
      analytics: {
        deviceBrands: analytics.deviceBrands,
        categoryShare: analytics.categoryShare,
        topCountries: analytics.topCountries,
        hourlyTraffic: analytics.hourlyTraffic,
        osList: analytics.osList,
      },
      sessions: db.sessions,
      topContent: db.topContent,
      searches: db.searches,
      errors: db.errors,
      activityLog: db.activityLog.slice(0, 30),
    });
  }

  // Export Sessions to CSV
  if (req.method === "GET" && url.pathname === "/api/export/sessions.csv") {
    expireStaleSessions();
    const headers = ["معرف الجلسة", "الجهاز والموديل", "نوع الجهاز", "الدولة", "نظام التشغيل", "وقت الفتح", "وقت الإغلاق", "مدة الجلسة", "آخر قسم", "الحالة"];
    const rows = db.sessions.map((s) => [
      `"${s.id}"`,
      `"${s.device}"`,
      `"${s.deviceType === "tv" ? "شاشة TV" : "موبايل"}"`,
      `"${s.country}"`,
      `"${s.osVersion}"`,
      `"${s.openTime}"`,
      `"${s.closeTime}"`,
      `"${s.durationText}"`,
      `"${s.lastSection}"`,
      `"${s.status === "active" ? "يشاهد الآن" : "انتهت"}"`,
    ]);

    const csvContent = "\uFEFF" + [headers.join(","), ...rows.map((r) => r.join(","))].join("\r\n");
    res.writeHead(200, {
      "Content-Type": "text/csv; charset=utf-8",
      "Content-Disposition": `attachment; filename="zaqzouka_telemetry_${Date.now()}.csv"`,
      "Cache-Control": "no-store",
    });
    return res.end(csvContent);
  }

  // Reset database endpoint
  if (req.method === "POST" && url.pathname === "/api/admin/reset") {
    db = initialDatabase();
    saveDatabase();
    return sendJson(res, 200, { ok: true, message: "Telemetry database reset to clean state" });
  }

  // Telemetry: Session Start (App Opened on real device)
  if (req.method === "POST" && url.pathname === "/api/telemetry/session/start") {
    const body = await readBody(req);
    const now = new Date();
    const sessionId = "sess_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7);

    const rawDevice = (body.device || body.deviceModel || "").trim();
    const device = rawDevice.length > 0 ? rawDevice : "Android Device";

    let countryCode = (body.countryCode || "").trim().toUpperCase();
    const tz = (body.timezone || "").toLowerCase();
    
    // Fix: If Android device language is English (US), it might report US while being in Egypt
    if (countryCode === "US" || !countryCode) {
      if (tz.includes("cairo") || tz.includes("egypt") || tz.includes("africa")) {
        countryCode = "EG";
      } else if (tz.includes("riyadh") || tz.includes("saudi")) {
        countryCode = "SA";
      } else if (tz.includes("dubai")) {
        countryCode = "AE";
      } else if (tz.includes("baghdad")) {
        countryCode = "IQ";
      } else if (tz.includes("casablanca")) {
        countryCode = "MA";
      } else if (req.socket.remoteAddress?.includes("192.168.") || req.socket.remoteAddress?.includes("127.0.0.1") || req.socket.remoteAddress?.includes("10.0.")) {
        // Local Egyptian testing network
        countryCode = "EG";
      }
    }

    const country = COUNTRY_MAP[countryCode] || body.country || (countryCode ? `${countryCode}` : "مصر EG");
    const section = (body.currentSection || "الرئيسية 🏠").trim();

    const newSession = {
      id: sessionId,
      deviceId: body.deviceId || sessionId,
      device: device,
      deviceType: body.deviceType || "phone",
      osVersion: body.osVersion || "Android",
      country: country,
      countryCode: countryCode || "EG",
      openTime: formatClockTime(now),
      closeTime: "نشط الآن...",
      durationText: "مستمر (1 د)",
      durationMinutes: 1,
      lastSection: section,
      lastContent: body.currentContent || "",
      status: "active",
      openedAt: now.toISOString(),
      lastSeen: now.toISOString(),
      actionsCount: 0,
    };

    db.sessions.unshift(newSession);
    if (db.sessions.length > 800) db.sessions.pop();

    db.stats.firstOpens = (db.stats.firstOpens || 0) + 1;
    if (body.isNewInstall === true) {
      db.stats.totalInstalls = (db.stats.totalInstalls || 0) + 1;
    } else if (!db.stats.totalInstalls || db.stats.totalInstalls < 1) {
      db.stats.totalInstalls = 1;
    }

    logActivity("open", `فتح التطبيق من جهاز ${device} (${country})`, "📱");
    saveDatabase();
    return sendJson(res, 200, { ok: true, sessionId });
  }

  // Telemetry: Session Heartbeat (Periodic ping while user uses app)
  if (req.method === "POST" && url.pathname === "/api/telemetry/session/heartbeat") {
    const body = await readBody(req);
    const sessionId = body.sessionId;
    const session = db.sessions.find((s) => s.id === sessionId);

    if (session) {
      session.status = "active";
      session.closeTime = "نشط الآن...";
      session.lastSeen = new Date().toISOString();
      if (body.currentSection) session.lastSection = body.currentSection;
      if (body.currentContent) session.lastContent = body.currentContent;

      const openedAt = new Date(session.openedAt).getTime();
      const minutes = Math.max(1, Math.round((Date.now() - openedAt) / (60 * 1000)));
      session.durationMinutes = minutes;
      session.durationText = `مستمر (${minutes} د)`;
      session.actionsCount = (session.actionsCount || 0) + 1;
      saveDatabase();
    }

    return sendJson(res, 200, { ok: true });
  }

  // Telemetry: User action (tapping tabs, clicking items, etc.)
  if (req.method === "POST" && url.pathname === "/api/telemetry/action") {
    const body = await readBody(req);
    const sessionId = body.sessionId;
    const session = db.sessions.find((s) => s.id === sessionId);

    if (session) {
      session.actionsCount = (session.actionsCount || 0) + 1;
      session.lastSeen = new Date().toISOString();
      if (body.section) {
        session.lastSection = body.section;
        logActivity("nav", `المستخدم (${session.device}) تصفح قسم: ${body.section}`, "🧭");
      }
      if (body.content) session.lastContent = body.content;
      saveDatabase();
    }

    return sendJson(res, 200, { ok: true });
  }

  // Telemetry: Session End (User Exits App or App goes to background)
  if (req.method === "POST" && url.pathname === "/api/telemetry/session/end") {
    const body = await readBody(req);
    const sessionId = body.sessionId;
    const session = db.sessions.find((s) => s.id === sessionId);

    if (session) {
      const now = new Date();
      session.status = "closed";
      session.closeTime = formatClockTime(now);

      const openedAt = new Date(session.openedAt).getTime();
      const minutes = Math.max(1, Math.round((Date.now() - openedAt) / (60 * 1000)));
      session.durationMinutes = minutes;
      session.durationText = `${minutes} دقيقة`;

      if ((session.actionsCount || 0) <= 0) {
        db.stats.bouncedUsers = (db.stats.bouncedUsers || 0) + 1;
        logActivity("bounce", `مستخدم ارتد فوراً (${session.device}) دون تصفح`, "⚠️");
      } else {
        logActivity("close", `أنهى (${session.device}) جلسته بعد ${session.durationText}`, "⚪");
      }
      saveDatabase();
    }

    return sendJson(res, 200, { ok: true });
  }

  // Telemetry: Search query submitted in app
  if (req.method === "POST" && url.pathname === "/api/telemetry/search") {
    const body = await readBody(req);
    const query = (body.query || "").trim();

    if (query.length >= 1) {
      db.stats.totalSearches = (db.stats.totalSearches || 0) + 1;

      if (body.sessionId) {
        const session = db.sessions.find((s) => s.id === body.sessionId);
        if (session) {
          session.actionsCount = (session.actionsCount || 0) + 1;
          session.lastSection = `بحث: ${query}`;
        }
      }

      let existing = db.searches.find((s) => s.query.toLowerCase() === query.toLowerCase());
      const isFound = body.found !== false && (body.resultsCount == null || body.resultsCount > 0);

      if (existing) {
        existing.count += 1;
        if (isFound) {
          existing.status = "available";
          existing.statusText = "متوفر الآن";
          existing.action = "تثبيت في الواجهة";
          existing.actionIcon = "⭐";
        }
      } else {
        db.searches.unshift({
          query: query,
          count: 1,
          status: isFound ? "available" : "demanded",
          statusText: isFound ? "متوفر الآن" : "مطلوب بقوة",
          action: isFound ? "تثبيت في الواجهة" : "إدراج في الأولوية",
          actionIcon: isFound ? "⭐" : "⚡",
        });
      }

      db.searches.sort((a, b) => b.count - a.count);
      logActivity("search", `بحث عن: "${query}" (${isFound ? "متوفر" : "مطلوب وغير متوفر"})`, "🔍");
      saveDatabase();
    }

    return sendJson(res, 200, { ok: true });
  }

  // Telemetry: Playback event (Started, Progress, or Error)
  if (req.method === "POST" && url.pathname === "/api/telemetry/playback") {
    const body = await readBody(req);
    const title = (body.title || "").trim();
    const eventType = body.event || "start"; // 'start' | 'progress' | 'error'

    if (body.sessionId) {
      const session = db.sessions.find((s) => s.id === body.sessionId);
      if (session) {
        session.actionsCount = (session.actionsCount || 0) + 1;
        if (title) session.lastContent = title;
        if (body.category) session.lastSection = body.category;
      }
    }

    if (eventType === "error") {
      db.stats.playbackErrors = (db.stats.playbackErrors || 0) + 1;
      const errItem = {
        id: "err_" + Date.now(),
        title: title || "بث غير محدد",
        category: body.category || "بث مباشر",
        reason: body.errorMessage || "فشل الاتصال بمصدر البث",
        time: formatClockTime(new Date()),
        timestamp: new Date().toISOString(),
      };
      db.errors.unshift(errItem);
      if (db.errors.length > 50) db.errors.pop();

      logActivity("error", `عطل في تشغيل: ${title} (${errItem.reason})`, "🔴");
      saveDatabase();
      return sendJson(res, 200, { ok: true });
    }

    if (title) {
      if (eventType === "start") {
        db.stats.totalPlays = (db.stats.totalPlays || 0) + 1;
        logActivity("play", `بدأ مشاهدة: ${title} (${body.category || "محتوى"})`, "▶️");
      }

      let item = db.topContent.find((c) =>
        c.title.toLowerCase().includes(title.toLowerCase()) ||
        title.toLowerCase().includes(c.title.toLowerCase())
      );

      const category = body.category || "بث مباشر";
      const icon = category.includes("مسلسل") ? "🍿" : category.includes("فيلم") ? "🎬" : "⚽";

      if (item) {
        if (eventType === "start") item.views += 1;
        if (body.completionPercent) {
          item.completionRate = Math.round((item.completionRate * 4 + body.completionPercent) / 5);
        }
      } else if (eventType === "start") {
        db.topContent.unshift({
          id: "c_" + Date.now(),
          title: title,
          category: category,
          categoryIcon: icon,
          views: 1,
          completionRate: body.completionPercent || 85,
        });
      }

      db.topContent.sort((a, b) => b.views - a.views);
      saveDatabase();
    }

    return sendJson(res, 200, { ok: true });
  }

  // Admin action button toggle
  if (req.method === "POST" && url.pathname === "/api/admin/action") {
    const body = await readBody(req);
    const targetQuery = (body.query || "").trim();
    const search = db.searches.find((s) => s.query === targetQuery);

    if (search) {
      search.action = body.action || search.action;
      search.actionApplied = true;
      saveDatabase();
      return sendJson(res, 200, { ok: true, search });
    }
    return sendJson(res, 404, { error: "Query not found" });
  }

  // Fallback 404
  return sendJson(res, 404, { error: "Not found" });
});

server.listen(PORT, HOST, () => {
  console.log(`\n======================================================`);
  console.log(`🚀 Zaqzouka TV Live Admin Dashboard is running!`);
  console.log(`🌐 Local Web UI:    http://127.0.0.1:${PORT}`);
  console.log(`📡 Network Web UI:  http://192.168.100.2:${PORT}`);
  console.log(`📡 Telemetry API:   POST /api/telemetry/session/start`);
  console.log(`📊 Deep Analytics:  GET /api/stats`);
  console.log(`📥 CSV Export:      GET /api/export/sessions.csv`);
  console.log(`======================================================\n`);
});
