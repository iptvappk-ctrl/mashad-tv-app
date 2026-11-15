const { db, resolveCountry, sendJson, handleCors } = require('../../_lib');

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
    const now = new Date();
    const sessionId = 'sess_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
    const rawDevice = (body.device || body.deviceModel || '').trim();
    const device = rawDevice.length > 0 ? rawDevice : 'Android Device';
    const remoteIp = (req.headers && req.headers['x-forwarded-for']) || '';
    const { code: countryCode, name: country } = resolveCountry(body.countryCode, body.timezone);

    const newSession = {
      id: sessionId,
      deviceId: body.deviceId || sessionId,
      device,
      deviceType: body.deviceType || 'phone',
      osVersion: body.osVersion || 'Android',
      appVersion: body.appVersion || '',
      country,
      countryCode,
      openTime: formatClockTime(now),
      closeTime: 'نشط الآن...',
      durationText: 'مستمر (1 د)',
      durationMinutes: 1,
      lastSection: (body.currentSection || 'الرئيسية 🏠').trim(),
      lastContent: body.currentContent || '',
      status: 'active',
      openedAt: now.toISOString(),
      lastSeen: now.toISOString(),
      actionsCount: 0,
    };

    await db.ref(`sessions/${sessionId}`).set(newSession);

    await db.ref('stats').transaction((current) => {
      const s = current || {};
      s.firstOpens = (s.firstOpens || 0) + 1;
      if (body.isNewInstall === true) {
        s.totalInstalls = (s.totalInstalls || 0) + 1;
      }
      return s;
    });

    await db.ref('activityLog').push({
      type: 'open',
      text: `فتح التطبيق من جهاز ${device} (${country})`,
      icon: '📱',
      time: formatClockTime(now),
      timestamp: now.toISOString(),
    });

    return sendJson(res, 200, { ok: true, sessionId });
  } catch (err) {
    console.error('session/start error:', err);
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
