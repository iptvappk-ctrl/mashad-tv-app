const { db, handleCors, sendJson } = require('../_lib');

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();
  if (req.method !== 'POST') return sendJson(res, 405, { error: 'Method Not Allowed' });

  const secret = req.headers['x-admin-secret'] || req.query.secret;
  if (!secret || secret !== process.env.ADMIN_SECRET) {
    return sendJson(res, 401, { error: 'Unauthorized' });
  }

  try {
    // Clear all telemetry data
    await db.ref('/').set(null);
    return sendJson(res, 200, { ok: true, message: 'All data cleared' });
  } catch (err) {
    console.error('reset error', err);
    return sendJson(res, 500, { error: err.message });
  }
};
