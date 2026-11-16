const { db } = require('../api/_lib');

async function purgeDuplicates() {
  console.log('Fetching all sessions from Firebase for deduplication...');
  const snap = await db.ref('sessions').once('value');
  if (!snap.exists()) {
    console.log('No sessions found.');
    process.exit(0);
  }

  const all = snap.val();
  const total = Object.keys(all).length;
  console.log(`Total sessions in database: ${total}`);

  const byDevice = {};
  for (const [sid, session] of Object.entries(all)) {
    const did = session.deviceId || session.device || sid;
    if (!byDevice[did]) byDevice[did] = [];
    byDevice[did].push({ sid, session });
  }

  console.log(`Unique devices found: ${Object.keys(byDevice).length}`);

  const toRemove = [];

  for (const [did, sessions] of Object.entries(byDevice)) {
    // Sort sessions: active first, then by openedAt / lastSeen descending (newest first)
    sessions.sort((a, b) => {
      const aActive = a.session.status === 'active' ? 1 : 0;
      const bActive = b.session.status === 'active' ? 1 : 0;
      if (aActive !== bActive) return bActive - aActive;
      const aTime = new Date(a.session.lastSeen || a.session.openedAt || 0).getTime();
      const bTime = new Date(b.session.lastSeen || b.session.openedAt || 0).getTime();
      return bTime - aTime;
    });

    // Keep at most 2 most recent sessions per device; remove the rest
    if (sessions.length > 2) {
      const excess = sessions.slice(2);
      for (const item of excess) {
        toRemove.push(item.sid);
      }
    }
  }

  console.log(`Sessions to delete: ${toRemove.length}`);

  if (toRemove.length === 0) {
    console.log('✅ Nothing to delete.');
    process.exit(0);
  }

  // Remove excess sessions
  const updates = {};
  for (const sid of toRemove) {
    updates[`sessions/${sid}`] = null;
  }

  await db.ref('/').update(updates);
  console.log(`✅ Successfully purged ${toRemove.length} duplicate historical sessions!`);
  
  // Verify count
  const afterSnap = await db.ref('sessions').once('value');
  console.log(`Remaining sessions: ${Object.keys(afterSnap.val() || {}).length}`);
  process.exit(0);
}

purgeDuplicates().catch(err => {
  console.error('Purge error:', err);
  process.exit(1);
});
