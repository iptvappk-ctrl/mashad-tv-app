// dashboard-api/api/admin/license-check.js
// 🔐 License Verification Gateway — Called by app & web on startup
// Returns: { allowed: true/false, killSwitch: {...}, message: "...", build: "..." }
// If kill switch is active → returns allowed: false → App shows shutdown screen

const { db, sendJson, handleCors } = require('../_lib');

// Authorized app build fingerprints (you control this list)
// Add builds you authorize, remove any build that belongs to unauthorized copies
const AUTHORIZED_BUILDS = (process.env.AUTHORIZED_BUILDS || '').split(',').map(s => s.trim()).filter(Boolean);

// Authorized API Keys - generate a unique one for each deployment
// Set ZAQZOUKA_API_KEY in Vercel env vars (only you know this value)
const ZAQZOUKA_API_KEY = process.env.ZAQZOUKA_API_KEY || '';

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();
  if (req.method !== 'POST' && req.method !== 'GET') {
    return sendJson(res, 405, { error: 'Method not allowed' });
  }

  try {
    // ─── 1. Check the master Kill Switch first ─────────────────────────────
    const killSnap = await db.ref('killSwitch').once('value');
    const killData = killSnap.val() || { active: false };

    if (killData.active) {
      const target = killData.target || 'all';
      const requestPlatform = req.body?.platform || req.query?.platform || 'all';
      
      // Check if kill switch applies to this platform
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

    // ─── 2. Check API Key if set (optional layer) ─────────────────────────
    if (ZAQZOUKA_API_KEY) {
      const apiKey = req.headers['x-zaqzouka-key'] || req.body?.apiKey || req.query?.apiKey || '';
      if (apiKey !== ZAQZOUKA_API_KEY) {
        return sendJson(res, 200, {
          allowed: false,
          reason: 'INVALID_API_KEY',
          message: 'تطبيق غير مصرح.',
          messageEn: 'Unauthorized application instance.',
        });
      }
    }

    // ─── 3. Check authorized builds (optional layer) ──────────────────────
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

    // ─── 4. All checks passed — service is running ────────────────────────
    return sendJson(res, 200, {
      allowed: true,
      status: 'OPERATIONAL',
      message: 'خدمة زقزوقة تعمل بشكل طبيعي.',
      killSwitch: { active: false },
    });

  } catch (err) {
    // On server error, let the app run (fail open) to avoid false positives
    return sendJson(res, 200, { allowed: true, status: 'SERVER_ERROR_FAIL_OPEN' });
  }
};
