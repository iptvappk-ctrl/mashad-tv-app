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
    const title = (body.title || '').trim();
    const eventType = body.event || 'start'; // 'start' | 'progress' | 'error'
    const now = new Date();

    if (eventType === 'error') {
      await db.ref('stats/playbackErrors').transaction((v) => (v || 0) + 1);
      await db.ref('errors').push({
        title: title || 'بث غير محدد',
        category: body.category || 'بث مباشر',
        reason: body.errorMessage || 'فشل الاتصال بمصدر البث',
        time: formatClockTime(now),
        timestamp: now.toISOString(),
      });
      await db.ref('activityLog').push({
        type: 'error',
        text: `عطل في تشغيل: ${title}`,
        icon: '🔴',
        time: formatClockTime(now),
        timestamp: now.toISOString(),
      });
      return sendJson(res, 200, { ok: true });
    }

    if (title && eventType === 'start') {
      await db.ref('stats/totalPlays').transaction((v) => (v || 0) + 1);

      // Top content aggregation
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

      await db.ref('activityLog').push({
        type: 'play',
        text: `بدأ مشاهدة: ${title} (${category})`,
        icon: '▶️',
        time: formatClockTime(now),
        timestamp: now.toISOString(),
      });
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
  } catch (err) {
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
