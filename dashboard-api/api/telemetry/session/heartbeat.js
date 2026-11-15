const { db, sendJson, handleCors } = require('../../_lib');

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();
  if (req.method !== 'POST') return sendJson(res, 405, { error: 'Method not allowed' });

  try {
    const body = req.body || {};
    const { sessionId, currentSection, currentContent } = body;
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
  } catch (err) {
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
