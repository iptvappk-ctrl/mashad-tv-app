// dashboard-api/api/admin/kill-switch.js
// 🔐 Kill Switch API - Emergency App Shutdown Controller
// Owner ONLY endpoint: Activate/Deactivate the global kill switch
// This endpoint requires the MASTER_SECRET environment variable (set only by you in Vercel)

const { db, sendJson, handleCors } = require('../_lib');

const MASTER_SECRET = process.env.MASTER_SECRET || '';

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();

  // ─── Authentication: Only the owner (you) can toggle the kill switch ───
  const authHeader = req.headers['x-master-secret'] || req.query.master_secret || '';
  if (!MASTER_SECRET || authHeader !== MASTER_SECRET) {
    return sendJson(res, 403, {
      error: 'Access denied. Owner authorization required.',
      code: 'UNAUTHORIZED'
    });
  }

  if (req.method === 'POST') {
    // Activate or deactivate kill switch
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
    // Public status check — used by app/web to verify if service is running
    const snap = await db.ref('killSwitch').once('value');
    const data = snap.val() || { active: false };
    return sendJson(res, 200, { killSwitch: data });
  }

  return sendJson(res, 405, { error: 'Method not allowed' });
};
