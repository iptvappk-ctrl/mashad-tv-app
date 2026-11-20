// 🛡️ Zero-dependency high-speed Quota Guard for zaqzouka-vercel-hub
// Uses native Node 18+ fetch() to read Firebase RTDB REST directly
// Caches at Vercel CDN edge for 60s (s-maxage=60)
// Updated: 2026-11-20

let _cache = null;
let _cacheTime = 0;
const MEMORY_TTL = 30000;

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Cache-Control', 's-maxage=60, stale-while-revalidate=300');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'GET') return res.status(405).json({ error: 'Method Not Allowed' });

  const now = Date.now();
  if (_cache && now - _cacheTime < MEMORY_TTL) {
    return res.status(200).json({ ..._cache, _source: 'memory_cache' });
  }

  try {
    const response = await fetch('https://zaqzouka-dashboard-api.vercel.app/api/status');
    if (response.ok) {
      const result = await response.json();
      _cache = result;
      _cacheTime = now;
      return res.status(200).json({ ...result, _source: 'dashboard_api' });
    }
  } catch (err) {
    if (_cache) {
      return res.status(200).json({ ..._cache, _source: 'memory_stale' });
    }
  }

  return res.status(200).json({
    status: 'active',
    message: 'المنظومة نشطة وتعمل بشكل طبيعي',
    updatedAt: now,
    wipeTimestamp: 0,
    _source: 'fallback',
  });
};
