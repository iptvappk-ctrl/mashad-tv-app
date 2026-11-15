const { db, sendJson, handleCors } = require('../../_lib');

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
    const { sessionId } = body;
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
      await db.ref('activityLog').push({
        type: 'bounce',
        text: `مستخدم ارتد فوراً (${session.device}) دون تصفح`,
        icon: '⚠️',
        time: formatClockTime(now),
        timestamp: now.toISOString(),
      });
    } else {
      await db.ref('activityLog').push({
        type: 'close',
        text: `أنهى (${session.device}) جلسته بعد ${minutes} دقيقة`,
        icon: '⚪',
        time: formatClockTime(now),
        timestamp: now.toISOString(),
      });
    }

    return sendJson(res, 200, { ok: true });
  } catch (err) {
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
