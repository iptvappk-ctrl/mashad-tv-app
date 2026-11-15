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
    const query = (body.query || '').trim();
    if (!query || query.length < 1) return sendJson(res, 200, { ok: true });

    const isFound = body.found !== false && (body.resultsCount == null || body.resultsCount > 0);
    const searchRef = db.ref(`searches/${Buffer.from(query).toString('hex').substring(0, 32)}`);
    const snap = await searchRef.once('value');

    if (snap.exists()) {
      await searchRef.update({
        count: (snap.val().count || 0) + 1,
        status: isFound ? 'available' : 'demanded',
        statusText: isFound ? 'متوفر الآن' : 'مطلوب بقوة',
      });
    } else {
      await searchRef.set({
        query,
        count: 1,
        status: isFound ? 'available' : 'demanded',
        statusText: isFound ? 'متوفر الآن' : 'مطلوب بقوة',
        createdAt: new Date().toISOString(),
      });
    }

    await db.ref('stats/totalSearches').transaction((v) => (v || 0) + 1);

    if (body.sessionId) {
      await db.ref(`sessions/${body.sessionId}`).update({
        lastSection: `بحث: ${query}`,
        lastSeen: new Date().toISOString(),
      }).catch(() => {});
    }

    await db.ref('activityLog').push({
      type: 'search',
      text: `بحث عن: "${query}" (${isFound ? 'متوفر' : 'غير متوفر'})`,
      icon: '🔍',
      time: formatClockTime(new Date()),
      timestamp: new Date().toISOString(),
    });

    return sendJson(res, 200, { ok: true });
  } catch (err) {
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
