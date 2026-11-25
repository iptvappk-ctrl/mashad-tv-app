const { db, handleCors } = require('./_lib');

let _healthCache = null;
let _healthCacheTime = 0;
const HEALTH_TTL_MS = 60_000;

module.exports = async (req, res) => {
  handleCors(res);
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Cache-Control', 'public, max-age=30, s-maxage=60, stale-while-revalidate=120');
  if (req.method === 'OPTIONS') return res.end();

  const now = Date.now();
  if (_healthCache && now - _healthCacheTime < HEALTH_TTL_MS) {
    res.statusCode = 200;
    return res.end(JSON.stringify({ ..._healthCache, _source: 'memory_cache' }));
  }

  try {
    const snap = await db.ref('stats/firstOpens').once('value');
    const firstOpens = snap.val() || 0;
    const payload = {
      status: 'online',
      serverTime: new Date().toISOString(),
      totalSessions: firstOpens,
      dataConnected: true,
    };
    _healthCache = payload;
    _healthCacheTime = now;

    res.statusCode = 200;
    return res.end(JSON.stringify(payload));
  } catch (err) {
    if (_healthCache) {
      res.statusCode = 200;
      return res.end(JSON.stringify({ ..._healthCache, _source: 'memory_stale' }));
    }
    res.statusCode = 200;
    return res.end(JSON.stringify({
      status: 'online',
      serverTime: new Date().toISOString(),
      totalSessions: 0,
      dataConnected: false,
      error: err.message,
    }));
  }
};
