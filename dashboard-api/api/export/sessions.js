const { db, handleCors } = require('../_lib');

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();
  if (req.method !== 'GET') {
    res.statusCode = 405;
    return res.end('Method Not Allowed');
  }

  const secret = req.headers['x-admin-secret'] || req.query.secret;
  if (!secret || secret !== process.env.ADMIN_SECRET) {
    res.statusCode = 401;
    return res.end('Unauthorized');
  }

  try {
    const snap = await db.ref('sessions').once('value');
    const sessions = snap.val() || {};

    const rows = [];
    rows.push([
      'session_id', 'device_id', 'country', 'brand', 'model',
      'os_version', 'app_version', 'started_at', 'last_seen',
      'duration_min', 'searches', 'playbacks', 'actions'
    ].join(','));

    for (const [sid, s] of Object.entries(sessions)) {
      const durationMin = s.last_seen && s.started_at
        ? Math.round((s.last_seen - s.started_at) / 60000)
        : 0;
      const row = [
        sid,
        s.device_id || '',
        s.country || '',
        s.brand || '',
        s.model || '',
        s.os_version || '',
        s.app_version || '',
        s.started_at ? new Date(s.started_at).toISOString() : '',
        s.last_seen ? new Date(s.last_seen).toISOString() : '',
        durationMin,
        s.searches || 0,
        s.playbacks || 0,
        s.actions || 0
      ].map(v => `"${String(v).replace(/"/g, '""')}"`).join(',');
      rows.push(row);
    }

    const csv = rows.join('\n');
    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', 'attachment; filename="sessions.csv"');
    res.statusCode = 200;
    res.end('\uFEFF' + csv); // BOM for Excel Arabic support
  } catch (err) {
    console.error('export/sessions error', err);
    res.statusCode = 500;
    res.end('Internal Server Error');
  }
};
