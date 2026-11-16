const { db, handleCors } = require('../_lib');

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();
  if (req.method !== 'GET') {
    res.statusCode = 405;
    return res.end('Method Not Allowed');
  }

  try {
    const snap = await db.ref('sessions').once('value');
    const sessions = snap.val() || {};

    const format = req.query.format === 'json' ? 'json' : 'csv';

    const profiles = [];
    for (const [sid, s] of Object.entries(sessions)) {
      profiles.push({
        audience_id: 'zaq_aud_' + (s.deviceId || sid).slice(0, 12),
        platform: s.platform || (s.deviceType === 'web' ? 'web' : 'mobile_app'),
        country: s.country || 'مصر (EG)',
        city: s.city || 'المدينة غير محددة',
        region: s.region || '',
        location: s.location || s.country || 'مصر',
        isp: s.isp || 'Telecom Provider',
        audience_segment: s.audienceSegmentName || 'عشاق سينما الأكشن والإثارة 🎬',
        iab_category: s.audienceCategory || 'Action & Blockbuster Movies',
        ad_value_tier: s.audienceAdValueTier || 'High ($5 - $12 eCPM)',
        target_advertisers: s.targetAdvertisers || 'Gaming, Tech, Streaming',
        last_active: s.lastSeen || s.openedAt || new Date().toISOString(),
      });
    }

    if (format === 'json') {
      res.setHeader('Content-Type', 'application/json; charset=utf-8');
      res.setHeader('Content-Disposition', 'attachment; filename="zaqzouka_dmp_audience_profiles.json"');
      res.statusCode = 200;
      return res.end(JSON.stringify({
        generated_at: new Date().toISOString(),
        standard: 'IAB Tech Lab / GDPR Anonymous Audience Taxonomy v3.0',
        total_profiles: profiles.length,
        profiles,
      }, null, 2));
    }

    // CSV format
    const headers = [
      'معرف الجمهور المجهول',
      'المنصة',
      'الدولة',
      'المدينة',
      'المحافظة',
      'الموقع الكامل',
      'مزود الخدمة ISP',
      'شريحة الجمهور',
      'تصنيف IAB الدولي',
      'فئة العائد الإعلاني eCPM',
      'المعلنون المستهدفون',
      'آخر نشاط'
    ];

    const rows = profiles.map((p) => [
      p.audience_id,
      p.platform,
      p.country,
      p.city,
      p.region,
      p.location,
      p.isp,
      p.audience_segment,
      p.iab_category,
      p.ad_value_tier,
      p.target_advertisers,
      p.last_active
    ].map(v => `"${String(v).replace(/"/g, '""')}"`).join(','));

    const csvContent = '\uFEFF' + [headers.join(','), ...rows].join('\r\n');
    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', 'attachment; filename="zaqzouka_dmp_audience_profiles.csv"');
    res.statusCode = 200;
    return res.end(csvContent);
  } catch (err) {
    console.error('export/audience error', err);
    res.statusCode = 500;
    res.end('Internal Server Error');
  }
};
