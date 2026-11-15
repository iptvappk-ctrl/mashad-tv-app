const { db, sendJson, handleCors } = require('./_lib');

const ADMIN_SECRET = process.env.ADMIN_SECRET || '';

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();
  if (req.method !== 'GET') return sendJson(res, 405, { error: 'Method not allowed' });

  // Optional simple protection
  const auth = (req.headers && req.headers['x-admin-secret']) || (req.query && req.query.secret) || '';
  if (ADMIN_SECRET && auth !== ADMIN_SECRET) {
    return sendJson(res, 403, { error: 'Unauthorized' });
  }

  try {
    const [statsSnap, sessionsSnap, topContentSnap, searchesSnap, errorsSnap, actLogSnap] = await Promise.all([
      db.ref('stats').once('value'),
      db.ref('sessions').orderByChild('openedAt').limitToLast(600).once('value'),
      db.ref('topContent').once('value'),
      db.ref('searches').once('value'),
      db.ref('errors').orderByChild('timestamp').limitToLast(50).once('value'),
      db.ref('activityLog').orderByChild('timestamp').limitToLast(50).once('value'),
    ]);

    const stats = statsSnap.val() || {};
    const sessionsObj = sessionsSnap.val() || {};
    const sessions = Object.values(sessionsObj).sort((a, b) =>
      new Date(b.openedAt).getTime() - new Date(a.openedAt).getTime()
    );

    // Auto-expire stale active sessions (no heartbeat > 4 min)
    const now = Date.now();
    for (const s of sessions) {
      if (s.status === 'active') {
        const lastSeen = new Date(s.lastSeen || s.openedAt).getTime();
        if (now - lastSeen > 4 * 60 * 1000) {
          s.status = 'closed';
          await db.ref(`sessions/${s.id}`).update({ status: 'closed' }).catch(() => {});
        }
      }
    }

    const activeNow = sessions.filter((s) => s.status === 'active').length;
    const totalSessions = sessions.length;

    // 1. Avg session duration
    const totalMinutes = sessions.reduce((acc, s) => acc + (s.durationMinutes || 1), 0);
    const avgDuration = totalSessions > 0 ? (totalMinutes / totalSessions).toFixed(1) : '0';

    // 2. TV vs Phone
    const tvCount = sessions.filter((s) => s.deviceType === 'tv').length;
    const tvRatio = totalSessions > 0 ? Math.round((tvCount / totalSessions) * 100) : 0;

    // 3. Bounce rate
    const firstOpens = stats.firstOpens || 0;
    const bounced = stats.bouncedUsers || 0;
    const bounceRate = firstOpens > 0 ? Math.round((bounced / firstOpens) * 100) : 0;

    // 4. Stream health
    const totalPlays = stats.totalPlays || 0;
    const errors = stats.playbackErrors || 0;
    const streamHealth =
      totalPlays + errors > 0
        ? ((totalPlays / (totalPlays + errors)) * 100).toFixed(1)
        : '100';

    // 5. Device brands
    const brandMap = {};
    for (const s of sessions) {
      const dev = (s.device || '').toLowerCase();
      let brand = 'أخرى';
      if (dev.includes('xiaomi') || dev.includes('redmi') || dev.includes('poco')) brand = 'شاومي (Xiaomi)';
      else if (dev.includes('samsung') || dev.includes('galaxy') || dev.includes('sm-')) brand = 'سامسونج (Samsung)';
      else if (dev.includes('realme') || dev.includes('rmx')) brand = 'ريلمي (Realme)';
      else if (dev.includes('huawei') || dev.includes('honor')) brand = 'هواوي (Huawei)';
      else if (dev.includes('oppo')) brand = 'أوبو (Oppo)';
      else if (dev.includes('vivo')) brand = 'فيفو (Vivo)';
      else if (dev.includes('tecno')) brand = 'تكنو (Tecno)';
      else if (dev.includes('infinix')) brand = 'انفينيكس (Infinix)';
      else if (dev.includes('itel')) brand = 'ايتيل (Itel)';
      else if (dev.includes('tv') || dev.includes('box') || s.deviceType === 'tv') brand = 'شاشات TV';
      else if (dev.includes('android')) brand = 'أندرويد عام';
      brandMap[brand] = (brandMap[brand] || 0) + 1;
    }
    const deviceBrands = Object.entries(brandMap)
      .map(([brand, count]) => ({
        brand,
        count,
        percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
      }))
      .sort((a, b) => b.count - a.count);

    // 6. Countries
    const countryMap = {};
    for (const s of sessions) {
      const c = s.country || 'مصر (EG)';
      countryMap[c] = (countryMap[c] || 0) + 1;
    }
    const topCountries = Object.entries(countryMap)
      .map(([name, count]) => ({
        name,
        count,
        percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
      }))
      .sort((a, b) => b.count - a.count);

    // 7. Hourly traffic
    const hourlyTraffic = Array(24).fill(0);
    for (const s of sessions) {
      if (s.openedAt) {
        const h = new Date(s.openedAt).getHours();
        if (h >= 0 && h < 24) hourlyTraffic[h]++;
      }
    }

    // 8. OS breakdown
    const osMap = {};
    for (const s of sessions) {
      const os = (s.osVersion || 'Android').split('(')[0].trim();
      osMap[os] = (osMap[os] || 0) + 1;
    }
    const osList = Object.entries(osMap)
      .map(([os, count]) => ({
        os,
        count,
        percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
      }))
      .sort((a, b) => b.count - a.count);

    // 9. Top content
    const topContentObj = topContentSnap.val() || {};
    const topContent = Object.values(topContentObj)
      .sort((a, b) => b.views - a.views)
      .slice(0, 30);

    // 10. Searches
    const searchesObj = searchesSnap.val() || {};
    const searchesList = Object.values(searchesObj)
      .sort((a, b) => b.count - a.count)
      .slice(0, 50);

    // 11. Errors
    const errorsObj = errorsSnap.val() || {};
    const errorsList = Object.values(errorsObj).sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );

    // 12. Activity log
    const actLogObj = actLogSnap.val() || {};
    const activityLog = Object.values(actLogObj)
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
      .slice(0, 30);

    return sendJson(res, 200, {
      ok: true,
      updatedAt: new Date().toISOString(),
      kpis: {
        activeNow,
        firstOpens,
        totalInstalls: stats.totalInstalls || 0,
        bouncedUsers: bounced,
        bounceRate,
        totalSearches: stats.totalSearches || 0,
        totalPlays,
        playbackErrors: errors,
        streamHealth,
        avgDurationMinutes: avgDuration,
        tvRatio,
        phoneRatio: 100 - tvRatio,
      },
      analytics: { deviceBrands, topCountries, hourlyTraffic, osList },
      sessions,
      topContent,
      searches: searchesList,
      errors: errorsList,
      activityLog,
    });
  } catch (err) {
    console.error('stats error:', err);
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
