// dashboard-api/api/admin.js
// Merged Admin Serverless Function:
//   - POST/GET /api/admin?route=kill-switch (Kill Switch Controller)
//   - POST/GET /api/admin?route=license-check (Client Startup License Verification)
//   - POST     /api/admin?route=reset (Data reset)

const { db, sendJson, handleCors } = require('./_lib');

const MASTER_SECRET = process.env.MASTER_SECRET || '';
const ADMIN_SECRET = process.env.ADMIN_SECRET || '';
const ZAQZOUKA_API_KEY = process.env.ZAQZOUKA_API_KEY || 'ZQ-OWNER-2026-SECRET-14MOON';
const AUTHORIZED_BUILDS = (process.env.AUTHORIZED_BUILDS || '').split(',').map(s => s.trim()).filter(Boolean);

// ─── route: kill-switch ───────────────────────────────────────────────────────
async function handleKillSwitch(req, res) {
  const authHeader = req.headers['x-master-secret'] || req.query.master_secret || '';
  if (!MASTER_SECRET || authHeader !== MASTER_SECRET) {
    return sendJson(res, 403, {
      error: 'Access denied. Owner authorization required.',
      code: 'UNAUTHORIZED'
    });
  }

  if (req.method === 'POST') {
    const body = req.body || {};
    const action = body.action; // 'kill' | 'restore' | 'status'
    const reason = body.reason || 'Emergency shutdown by owner';
    const targetApp = body.target || 'all'; // 'app' | 'web' | 'all'

    if (action === 'kill') {
      const killData = {
        active: true,
        activatedAt: new Date().toISOString(),
        reason,
        target: targetApp,
        message: body.message || 'التطبيق متوقف مؤقتاً. جاري الصيانة.',
        messageEn: body.messageEn || 'App is temporarily unavailable. Under maintenance.',
      };
      await db.ref('killSwitch').set(killData);
      return sendJson(res, 200, {
        success: true,
        action: 'KILL_ACTIVATED',
        target: targetApp,
        message: `Kill switch activated for: ${targetApp}`,
        data: killData
      });
    }

    if (action === 'restore') {
      await db.ref('killSwitch').set({
        active: false,
        restoredAt: new Date().toISOString(),
        reason: 'Owner restored service',
      });
      return sendJson(res, 200, {
        success: true,
        action: 'SERVICE_RESTORED',
        message: 'Kill switch deactivated. App is back online.'
      });
    }

    if (action === 'status') {
      const snap = await db.ref('killSwitch').once('value');
      return sendJson(res, 200, { killSwitch: snap.val() || { active: false } });
    }

    return sendJson(res, 400, { error: 'Invalid action. Use: kill | restore | status' });
  }

  if (req.method === 'GET') {
    const snap = await db.ref('killSwitch').once('value');
    return sendJson(res, 200, { killSwitch: snap.val() || { active: false } });
  }

  return sendJson(res, 405, { error: 'Method not allowed' });
}

// ─── route: license-check ─────────────────────────────────────────────────────
async function handleLicenseCheck(req, res) {
  try {
    // 1. Check Kill Switch
    const killSnap = await db.ref('killSwitch').once('value');
    const killData = killSnap.val() || { active: false };

    if (killData.active) {
      const target = killData.target || 'all';
      const requestPlatform = req.body?.platform || req.query?.platform || 'all';
      const isKilled = target === 'all' || target === requestPlatform;

      if (isKilled) {
        return sendJson(res, 200, {
          allowed: false,
          reason: 'KILL_SWITCH_ACTIVE',
          message: killData.message || 'التطبيق متوقف مؤقتاً.',
          messageEn: killData.messageEn || 'Service temporarily unavailable.',
          killSwitch: killData,
        });
      }
    }

    // 2. Check API Key
    if (ZAQZOUKA_API_KEY) {
      const apiKey = req.headers['x-zaqzouka-key'] || req.body?.apiKey || req.query?.apiKey || '';
      if (apiKey && apiKey !== ZAQZOUKA_API_KEY) {
        return sendJson(res, 200, {
          allowed: false,
          reason: 'INVALID_API_KEY',
          message: 'تطبيق غير مصرح.',
          messageEn: 'Unauthorized application instance.',
        });
      }
    }

    // 3. Check authorized builds
    if (AUTHORIZED_BUILDS.length > 0) {
      const buildId = req.body?.buildId || req.query?.buildId || '';
      if (buildId && !AUTHORIZED_BUILDS.includes(buildId)) {
        return sendJson(res, 200, {
          allowed: false,
          reason: 'UNAUTHORIZED_BUILD',
          message: 'هذه النسخة غير مصرح بها.',
          messageEn: 'This build is not authorized.',
        });
      }
    }

    return sendJson(res, 200, {
      allowed: true,
      status: 'OPERATIONAL',
      message: 'خدمة زقزوقة تعمل بشكل طبيعي.',
      killSwitch: { active: false },
    });
  } catch (err) {
    return sendJson(res, 200, { allowed: true, status: 'SERVER_ERROR_FAIL_OPEN' });
  }
}

// ─── route: reset ─────────────────────────────────────────────────────────────
async function handleReset(req, res) {
  if (req.method !== 'POST') return sendJson(res, 405, { error: 'Method Not Allowed' });
  const secret = req.headers['x-admin-secret'] || req.query.secret;
  if (!secret || secret !== ADMIN_SECRET) {
    return sendJson(res, 401, { error: 'Unauthorized' });
  }
  try {
    await db.ref('/').set(null);
    return sendJson(res, 200, { ok: true, message: 'All data cleared' });
  } catch (err) {
    return sendJson(res, 500, { error: err.message });
  }
}

// ─── route: cleanup-sessions (remove duplicate/stale sessions per device) ─────
async function handleCleanupSessions(req, res) {
  if (req.method !== 'POST') return sendJson(res, 405, { error: 'POST required' });
  const secret = req.headers['x-master-secret'] || req.query.secret;
  if (!MASTER_SECRET || secret !== MASTER_SECRET) {
    return sendJson(res, 403, { error: 'Unauthorized' });
  }

  try {
    const snap = await db.ref('sessions').once('value');
    if (!snap.exists()) return sendJson(res, 200, { ok: true, removed: 0 });

    const all = snap.val();
    const now = Date.now();
    const TEN_MIN = 10 * 60 * 1000;

    // Group sessions by deviceId
    const byDevice = {};
    for (const [sid, session] of Object.entries(all)) {
      const did = session.deviceId || sid;
      if (!byDevice[did]) byDevice[did] = [];
      byDevice[did].push({ sid, session });
    }

    const updates = {};
    let removed = 0;

    for (const [did, sessions] of Object.entries(byDevice)) {
      if (sessions.length <= 1) continue; // no duplicates

      // Sort: active sessions last, then by lastSeen descending
      sessions.sort((a, b) => {
        if (a.session.status === 'active' && b.session.status !== 'active') return 1;
        if (b.session.status === 'active' && a.session.status !== 'active') return -1;
        return new Date(b.session.lastSeen) - new Date(a.session.lastSeen);
      });

      // Keep the most recent active session, close all others
      const keepIdx = sessions.findIndex(s => s.session.status === 'active');
      sessions.forEach((item, i) => {
        if (i === keepIdx) return; // keep this one
        if (item.session.status === 'active') {
          // Mark old active sessions as closed
          updates[`sessions/${item.sid}/status`] = 'closed';
          updates[`sessions/${item.sid}/durationText`] = 'مكرر - أُغلق تلقائياً';
          removed++;
        }
      });

      // Also close stale active sessions (last seen > 30 min ago)
      sessions.forEach((item) => {
        if (item.session.status === 'active') {
          const age = now - new Date(item.session.lastSeen).getTime();
          if (age > 30 * 60 * 1000) {
            updates[`sessions/${item.sid}/status`] = 'closed';
            updates[`sessions/${item.sid}/durationText`] = 'انتهت تلقائياً (30 د)';
            removed++;
          }
        }
      });
    }

    if (Object.keys(updates).length > 0) {
      await db.ref('/').update(updates);
    }

    return sendJson(res, 200, { ok: true, removed, deviceGroups: Object.keys(byDevice).length });
  } catch (err) {
    return sendJson(res, 500, { error: err.message });
  }
}

// ─── Main Router ─────────────────────────────────────────────────────────────
module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();

  const url = req.url || '';
  const route = req.query.route ||
    (url.includes('/kill-switch') ? 'kill-switch' :
     url.includes('/license-check') ? 'license-check' :
     url.includes('/reset') ? 'reset' : '');

  try {
    if (route === 'kill-switch') return await handleKillSwitch(req, res);
    if (route === 'license-check') return await handleLicenseCheck(req, res);
    if (route === 'reset') return await handleReset(req, res);
    if (route === 'cleanup-sessions') return await handleCleanupSessions(req, res);
    return sendJson(res, 404, { error: 'Unknown admin route. Use ?route=kill-switch|license-check|reset|cleanup-sessions' });
  } catch (err) {
    console.error('admin router error:', err);
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
