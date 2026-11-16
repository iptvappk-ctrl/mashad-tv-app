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

    const isWeb = body.platform === 'web' || body.deviceType === 'web';
    const newSession = {
      id: sessionId,
      deviceId: body.deviceId || sessionId,
      device,
      deviceType: body.deviceType || (isWeb ? 'web' : 'phone'),
      platform: isWeb ? 'web' : 'app',
      browser: body.browser || '',
      osVersion: body.osVersion || (isWeb ? 'Web Browser' : 'Android'),
      appVersion: body.appVersion || (isWeb ? 'Web Portal v1.0' : ''),
      country,
      countryCode,
      openTime: formatClockTime(now),
      closeTime: 'نشط الآن...',
      durationText: 'مستمر (1 د)',
      durationMinutes: 1,
      lastSection: (body.currentSection || (isWeb ? 'الموقع 🌐' : 'الرئيسية 🏠')).trim(),
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
      if (isWeb) {
        s.webVisits = (s.webVisits || 0) + 1;
      } else if (body.isNewInstall === true) {
        s.totalInstalls = (s.totalInstalls || 0) + 1;
      }
      return s;
    });

    const actIcon = isWeb ? '🌐' : '📱';
    const actLabel = isWeb ? 'زيارة للموقع الإلكتروني' : 'فتح تطبيق زقزوقة';
    await db.ref('activityLog').push({
      type: 'open',
      text: `${actLabel} من ${device} (${country})`,
      icon: actIcon,
      time: formatClockTime(now),
      timestamp: now.toISOString(),
    });

    return sendJson(res, 200, { ok: true, sessionId });
  } catch (err) {
    console.error('session/start error:', err);
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
