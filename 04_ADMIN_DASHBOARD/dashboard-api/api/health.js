const { db, sendJson, handleCors } = require('./_lib');

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();

  try {
    const snap = await db.ref('stats').once('value');
    const stats = snap.val() || {};

    return sendJson(res, 200, {
      status: 'online',
      serverTime: new Date().toISOString(),
      totalSessions: stats.firstOpens || 0,
      dataConnected: true,
    });
  } catch (err) {
    return sendJson(res, 200, {
      status: 'online',
      serverTime: new Date().toISOString(),
      totalSessions: 0,
      dataConnected: false,
      error: err.message,
    });
  }
};
