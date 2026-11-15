const { db, sendJson, handleCors } = require('../_lib');

function formatClockTime(d) {
  d = d || new Date();
  return d.toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit', hour12: true });
}

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();
  if (req.method !== 'POST') return sendJson(res, 405, { error: 'Method not allowed' });

  try {
    const body = req.body || {};
    const { sessionId, section, content } = body;
    if (!sessionId) return sendJson(res, 200, { ok: true });

    const sessionRef = db.ref(`sessions/${sessionId}`);
    const snap = await sessionRef.once('value');
    if (!snap.exists()) return sendJson(res, 200, { ok: true });

    const session = snap.val();
    const updates = {
      actionsCount: (session.actionsCount || 0) + 1,
      lastSeen: new Date().toISOString(),
    };
    if (section) updates.lastSection = section;
    if (content) updates.lastContent = content;

    await sessionRef.update(updates);

    if (section) {
      await db.ref('activityLog').push({
        type: 'nav',
        text: `المستخدم (${session.device}) تصفح قسم: ${section}`,
        icon: '🧭',
        time: formatClockTime(new Date()),
        timestamp: new Date().toISOString(),
      });
    }

    return sendJson(res, 200, { ok: true });
  } catch (err) {
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
