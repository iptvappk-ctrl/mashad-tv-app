const { db } = require('../api/_lib');

async function migrate() {
  console.log('Fetching all sessions from Firebase for device migration...');
  const snap = await db.ref('sessions').once('value');
  if (!snap.exists()) {
    console.log('No sessions found.');
    process.exit(0);
  }

  const all = snap.val();
  console.log(`Current raw session count: ${Object.keys(all).length}`);

  const byDevice = {};
  const webSessions = {};

  for (const [sid, session] of Object.entries(all)) {
    if (session.platform === 'web' || session.deviceType === 'web' || sid.startsWith('sess_web_')) {
      webSessions[sid] = session;
      continue;
    }
    const did = session.deviceId || session.device || 'unknown_device';
    if (!byDevice[did]) byDevice[did] = [];
    byDevice[did].push({ sid, session });
  }

  console.log(`Unique mobile/TV devices: ${Object.keys(byDevice).length}`);
  console.log(`Web sessions: ${Object.keys(webSessions).length}`);

  const cleanSessions = {};

  for (const [did, items] of Object.entries(byDevice)) {
    // Sort items: active first, then newest lastSeen / openedAt
    items.sort((a, b) => {
      const aAct = a.session.status === 'active' ? 1 : 0;
      const bAct = b.session.status === 'active' ? 1 : 0;
      if (aAct !== bAct) return bAct - aAct;
      return new Date(b.session.lastSeen || b.session.openedAt || 0) - new Date(a.session.lastSeen || a.session.openedAt || 0);
    });

    const newest = items[0].session;
    const cleanId = 'dev_' + did.replace(/[^a-zA-Z0-9_-]/g, '_');
    const totalDuration = items.reduce((sum, it) => sum + (it.session.durationMinutes || 1), 0);
    const totalActions = items.reduce((sum, it) => sum + (it.session.actionsCount || 0), 0);

    cleanSessions[cleanId] = {
      ...newest,
      id: cleanId,
      deviceId: did,
      sessionCount: items.length,
      durationMinutes: totalDuration,
      actionsCount: totalActions,
      lastSeen: newest.lastSeen || new Date().toISOString(),
    };
  }

  // Also preserve up to 5 newest web sessions
  const webList = Object.entries(webSessions).map(([sid, session]) => ({ sid, session }));
  webList.sort((a, b) => new Date(b.session.lastSeen || 0) - new Date(a.session.lastSeen || 0));
  webList.slice(0, 5).forEach(({ sid, session }) => {
    cleanSessions[sid] = session;
  });

  console.log(`Total new clean entries: ${Object.keys(cleanSessions).length}`);

  // Overwrite /sessions with the deduplicated map
  await db.ref('sessions').set(cleanSessions);
  console.log('✅ /sessions successfully replaced with 1 entry per device!');

  process.exit(0);
}

migrate().catch(err => {
  console.error('Migration error:', err);
  process.exit(1);
});
