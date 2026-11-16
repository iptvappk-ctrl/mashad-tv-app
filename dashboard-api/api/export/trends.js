const { db, handleCors } = require('../_lib');

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();
  if (req.method !== 'GET') {
    res.statusCode = 405;
    return res.end('Method Not Allowed');
  }

  try {
    const [statsSnap, topContentSnap, searchesSnap, sessionsSnap] = await Promise.all([
      db.ref('stats').once('value'),
      db.ref('topContent').once('value'),
      db.ref('searches').once('value'),
      db.ref('sessions').once('value'),
    ]);

    const stats = statsSnap.val() || {};
    const topContent = Object.values(topContentSnap.val() || {}).sort((a, b) => b.views - a.views);
    const searches = Object.values(searchesSnap.val() || {}).sort((a, b) => b.count - a.count);
    const sessions = Object.values(sessionsSnap.val() || {});

    // City rankings
    const cityMap = {};
    for (const s of sessions) {
      const loc = s.location || (s.city ? `${s.city}، ${s.country || ''}` : s.country || 'مصر');
      cityMap[loc] = (cityMap[loc] || 0) + 1;
    }
    const geographicDistribution = Object.entries(cityMap)
      .map(([location, users]) => ({ location, users }))
      .sort((a, b) => b.users - a.users);

    const report = {
      report_title: 'تقرير اتجاهات المشاهدة وسلوك الجمهور الرقمي - 2026',
      publisher: 'Zaqzouka Streaming Intelligence Network',
      commercial_value: '$1,000 USD (Commercial Market Research Grade)',
      generated_at: new Date().toISOString(),
      executive_summary: {
        total_unique_viewers: sessions.length,
        total_video_plays: stats.totalPlays || 0,
        total_searches: stats.totalSearches || 0,
        viewer_peak_hours: '8:00 PM - 1:00 AM (توقيت القاهرة/مكة)',
        dominant_market: 'مصر (القاهرة، طنطا، الإسكندرية، الجيزة)',
      },
      top_performing_content: topContent.slice(0, 20).map((c, i) => ({
        rank: i + 1,
        title: c.title,
        views: c.views,
        category: c.category || 'سينما ودراما',
      })),
      top_search_trends: searches.slice(0, 25).map((s, i) => ({
        rank: i + 1,
        keyword: s.query,
        demand_volume: s.count,
      })),
      geographic_distribution: geographicDistribution.slice(0, 15),
    };

    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.setHeader('Content-Disposition', 'attachment; filename="zaqzouka_market_trend_report_2026.json"');
    res.statusCode = 200;
    return res.end(JSON.stringify(report, null, 2));
  } catch (err) {
    console.error('export/trends error', err);
    res.statusCode = 500;
    res.end('Internal Server Error');
  }
};
