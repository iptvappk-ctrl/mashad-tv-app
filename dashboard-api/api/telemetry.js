// dashboard-api/api/telemetry.js
// Merged Serverless Function — handles all telemetry routes:
//   POST /api/telemetry?route=session/start
//   POST /api/telemetry?route=session/end
//   POST /api/telemetry?route=session/heartbeat
//   POST /api/telemetry?route=action
//   POST /api/telemetry?route=playback
//   POST /api/telemetry?route=search

const { db, sendJson, handleCors, resolveDetailedLocation, resolveAudienceSegment } = require('./_lib');

function formatClockTime(d) {
  d = d || new Date();
  return d.toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit', hour12: true });
}

// ─── route: session/start ────────────────────────────────────────────────────
async function sessionStart(req, res) {
  const body = req.body || {};
  const now = new Date();
  const sessionId = 'sess_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
  const rawDevice = (body.device || body.deviceModel || '').trim();
  const device = rawDevice.length > 0 ? rawDevice : 'Android Device';

  const geo = resolveDetailedLocation(body, req);
  const isWeb = body.platform === 'web' || body.deviceType === 'web';
  const lastSection = (body.currentSection || (isWeb ? 'الموقع 🌐' : 'الرئيسية 🏠')).trim();
  const lastContent = body.currentContent || '';
  const audience = resolveAudienceSegment(lastContent, lastSection);

  const newSession = {
    id: sessionId,
    deviceId: body.deviceId || sessionId,
    device,
    deviceType: body.deviceType || (isWeb ? 'web' : 'phone'),
    platform: isWeb ? 'web' : 'app',
    browser: body.browser || '',
    osVersion: body.osVersion || (isWeb ? 'Web Browser' : 'Android'),
    appVersion: body.appVersion || (isWeb ? 'Web Portal v1.0' : ''),
    country: geo.country,
    countryCode: geo.countryCode,
    city: geo.city,
    region: geo.region,
    location: geo.location,
    isp: geo.isp,
    audienceSegmentId: audience.id,
    audienceSegmentName: audience.name,
    audienceCategory: audience.category,
    audienceAdValueTier: audience.adValueTier,
    targetAdvertisers: audience.targetAdvertisers,
    openTime: formatClockTime(now),
    closeTime: 'نشط الآن...',
    durationText: 'مستمر (1 د)',
    durationMinutes: 1,
    lastSection,
    lastContent,
    status: 'active',
    openedAt: now.toISOString(),
    lastSeen: now.toISOString(),
    actionsCount: 0,
  };

  await db.ref(`sessions/${sessionId}`).set(newSession);
  await db.ref('stats').transaction((current) => {
    const s = current || {};
    s.firstOpens = (s.firstOpens || 0) + 1;
    if (isWeb) s.webVisits = (s.webVisits || 0) + 1;
    else if (body.isNewInstall === true) s.totalInstalls = (s.totalInstalls || 0) + 1;
    return s;
  });

  const actIcon = isWeb ? '🌐' : '📱';
  const actLabel = isWeb ? 'زيارة للموقع الإلكتروني' : 'فتح تطبيق زقزوقة';
  await db.ref('activityLog').push({
    type: 'open',
    text: `${actLabel} من ${device} (${geo.location})`,
    icon: actIcon,
    time: formatClockTime(now),
    timestamp: now.toISOString(),
  });

  return sendJson(res, 200, { ok: true, sessionId, location: geo.location });
}

// ─── route: session/end ──────────────────────────────────────────────────────
async function sessionEnd(req, res) {
  const { sessionId } = req.body || {};
  if (!sessionId) return sendJson(res, 200, { ok: true });

  const sessionRef = db.ref(`sessions/${sessionId}`);
  const snap = await sessionRef.once('value');
  if (!snap.exists()) return sendJson(res, 200, { ok: true });

  const now = new Date();
  const session = snap.val();
  const openedAt = new Date(session.openedAt).getTime();
  const minutes = Math.max(1, Math.round((Date.now() - openedAt) / 60000));

  await sessionRef.update({
    status: 'closed',
    closeTime: formatClockTime(now),
    durationMinutes: minutes,
    durationText: `${minutes} دقيقة`,
    lastSeen: now.toISOString(),
  });

  if ((session.actionsCount || 0) <= 0) {
    await db.ref('stats/bouncedUsers').transaction((v) => (v || 0) + 1);
    await db.ref('activityLog').push({ type: 'bounce', text: `مستخدم ارتد فوراً (${session.device}) دون تصفح`, icon: '⚠️', time: formatClockTime(now), timestamp: now.toISOString() });
  } else {
    await db.ref('activityLog').push({ type: 'close', text: `أنهى (${session.device}) جلسته بعد ${minutes} دقيقة`, icon: '⚪', time: formatClockTime(now), timestamp: now.toISOString() });
  }

  return sendJson(res, 200, { ok: true });
}

// ─── route: session/heartbeat ────────────────────────────────────────────────
async function sessionHeartbeat(req, res) {
  const { sessionId, currentSection, currentContent } = req.body || {};
  if (!sessionId) return sendJson(res, 400, { error: 'sessionId required' });

  const sessionRef = db.ref(`sessions/${sessionId}`);
  const snap = await sessionRef.once('value');
  if (!snap.exists()) return sendJson(res, 200, { ok: true });

  const now = new Date();
  const session = snap.val();
  const openedAt = new Date(session.openedAt).getTime();
  const minutes = Math.max(1, Math.round((Date.now() - openedAt) / 60000));

  const updates = {
    status: 'active',
    closeTime: 'نشط الآن...',
    lastSeen: now.toISOString(),
    durationMinutes: minutes,
    durationText: `مستمر (${minutes} د)`,
    actionsCount: (session.actionsCount || 0) + 1,
  };
  if (currentSection) updates.lastSection = currentSection;
  if (currentContent) updates.lastContent = currentContent;

  await sessionRef.update(updates);
  return sendJson(res, 200, { ok: true });
}

// ─── route: action ───────────────────────────────────────────────────────────
async function telemetryAction(req, res) {
  const { sessionId, section, content } = req.body || {};
  if (!sessionId) return sendJson(res, 200, { ok: true });

  const sessionRef = db.ref(`sessions/${sessionId}`);
  const snap = await sessionRef.once('value');
  if (!snap.exists()) return sendJson(res, 200, { ok: true });

  const session = snap.val();
  const updates = { actionsCount: (session.actionsCount || 0) + 1, lastSeen: new Date().toISOString() };
  if (section) updates.lastSection = section;
  if (content) updates.lastContent = content;

  await sessionRef.update(updates);
  if (section) {
    await db.ref('activityLog').push({ type: 'nav', text: `المستخدم (${session.device}) تصفح قسم: ${section}`, icon: '🧭', time: formatClockTime(new Date()), timestamp: new Date().toISOString() });
  }
  return sendJson(res, 200, { ok: true });
}

// ─── route: playback ─────────────────────────────────────────────────────────
async function telemetryPlayback(req, res) {
  const body = req.body || {};
  const title = (body.title || '').trim();
  const eventType = body.event || 'start';
  const now = new Date();

  if (eventType === 'error') {
    await db.ref('stats/playbackErrors').transaction((v) => (v || 0) + 1);
    await db.ref('errors').push({ title: title || 'بث غير محدد', category: body.category || 'بث مباشر', reason: body.errorMessage || 'فشل الاتصال بمصدر البث', time: formatClockTime(now), timestamp: now.toISOString() });
    await db.ref('activityLog').push({ type: 'error', text: `عطل في تشغيل: ${title}`, icon: '🔴', time: formatClockTime(now), timestamp: now.toISOString() });
    return sendJson(res, 200, { ok: true });
  }

  if (title && eventType === 'start') {
    await db.ref('stats/totalPlays').transaction((v) => (v || 0) + 1);
    const contentKey = Buffer.from(title).toString('hex').substring(0, 24);
    const contentRef = db.ref(`topContent/${contentKey}`);
    const snap = await contentRef.once('value');
    const category = body.category || 'بث مباشر';
    const icon = category.includes('مسلسل') ? '🍿' : category.includes('فيلم') ? '🎬' : '⚽';
    if (snap.exists()) {
      await contentRef.update({ views: (snap.val().views || 0) + 1 });
    } else {
      await contentRef.set({ title, category, categoryIcon: icon, views: 1, completionRate: 85, createdAt: now.toISOString() });
    }
    await db.ref('activityLog').push({ type: 'play', text: `بدأ مشاهدة: ${title} (${category})`, icon: '▶️', time: formatClockTime(now), timestamp: now.toISOString() });
  }

  if (body.sessionId) {
    const session = (await db.ref(`sessions/${body.sessionId}`).once('value')).val();
    if (session) {
      const updates = { actionsCount: (session.actionsCount || 0) + 1, lastSeen: now.toISOString() };
      if (title) updates.lastContent = title;
      if (body.category) updates.lastSection = body.category;
      await db.ref(`sessions/${body.sessionId}`).update(updates);
    }
  }

  return sendJson(res, 200, { ok: true });
}

// ─── route: search ───────────────────────────────────────────────────────────
async function telemetrySearch(req, res) {
  const body = req.body || {};
  const query = (body.query || '').trim();
  if (!query || query.length < 1) return sendJson(res, 200, { ok: true });

  const isFound = body.found !== false && (body.resultsCount == null || body.resultsCount > 0);
  const searchRef = db.ref(`searches/${Buffer.from(query).toString('hex').substring(0, 32)}`);
  const snap = await searchRef.once('value');

  if (snap.exists()) {
    await searchRef.update({ count: (snap.val().count || 0) + 1, status: isFound ? 'available' : 'demanded', statusText: isFound ? 'متوفر الآن' : 'مطلوب بقوة' });
  } else {
    await searchRef.set({ query, count: 1, status: isFound ? 'available' : 'demanded', statusText: isFound ? 'متوفر الآن' : 'مطلوب بقوة', createdAt: new Date().toISOString() });
  }

  await db.ref('stats/totalSearches').transaction((v) => (v || 0) + 1);
  if (body.sessionId) {
    await db.ref(`sessions/${body.sessionId}`).update({ lastSection: `بحث: ${query}`, lastSeen: new Date().toISOString() }).catch(() => {});
  }
  await db.ref('activityLog').push({ type: 'search', text: `بحث عن: "${query}" (${isFound ? 'متوفر' : 'غير متوفر'})`, icon: '🔍', time: formatClockTime(new Date()), timestamp: new Date().toISOString() });

  return sendJson(res, 200, { ok: true });
}

// ─── Main Router ─────────────────────────────────────────────────────────────
module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();

  // Route via query param or URL path
  const url = req.url || '';
  const route = req.query.route ||
    (url.includes('/session/start') ? 'session/start' :
     url.includes('/session/end') ? 'session/end' :
     url.includes('/session/heartbeat') ? 'session/heartbeat' :
     url.includes('/action') ? 'action' :
     url.includes('/playback') ? 'playback' :
     url.includes('/search') ? 'search' : '');

  try {
    if (route === 'session/start') return await sessionStart(req, res);
    if (route === 'session/end') return await sessionEnd(req, res);
    if (route === 'session/heartbeat') return await sessionHeartbeat(req, res);
    if (route === 'action') return await telemetryAction(req, res);
    if (route === 'playback') return await telemetryPlayback(req, res);
    if (route === 'search') return await telemetrySearch(req, res);
    return sendJson(res, 404, { error: 'Unknown telemetry route. Use ?route=session/start|session/end|session/heartbeat|action|playback|search' });
  } catch (err) {
    console.error('telemetry error:', err);
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
