/**
 * One-time cleanup script: marks duplicate/stale sessions as closed.
 * Run with: node scripts/cleanup_sessions.js
 * Requires FIREBASE_DATABASE_URL and FIREBASE_SERVICE_ACCOUNT env vars,
 * OR just reads from _lib.js if already configured.
 */
const { db } = require('../api/_lib');

async function cleanup() {
  console.log('Fetching all sessions from Firebase...');
  const snap = await db.ref('sessions').once('value');
  if (!snap.exists()) { console.log('No sessions found.'); process.exit(0); }

  const all = snap.val();
  const total = Object.keys(all).length;
  console.log(`Total sessions: ${total}`);

  const now = Date.now();
  const byDevice = {};

  // Group by deviceId
  for (const [sid, session] of Object.entries(all)) {
    const did = session.deviceId || sid;
    if (!byDevice[did]) byDevice[did] = [];
    byDevice[did].push({ sid, session });
  }

  const deviceCount = Object.keys(byDevice).length;
  console.log(`Unique devices: ${deviceCount}`);

  const updates = {};
  let closed = 0;
  let stale = 0;

  for (const [did, sessions] of Object.entries(byDevice)) {
    // Close stale active sessions (> 30 min without heartbeat)
    for (const { sid, session } of sessions) {
      if (session.status === 'active') {
        const age = now - new Date(session.lastSeen || session.openedAt).getTime();
        if (age > 30 * 60 * 1000) {
          updates[`sessions/${sid}/status`] = 'closed';
          updates[`sessions/${sid}/durationText`] = 'انتهت تلقائياً (قديمة)';
          stale++;
        }
      }
    }

    if (sessions.length <= 1) continue;

    // Sort: active last, then by lastSeen desc
    sessions.sort((a, b) => {
      if (a.session.status === 'active' && b.session.status !== 'active') return 1;
      if (b.session.status === 'active' && a.session.status !== 'active') return -1;
      return new Date(b.session.lastSeen || 0) - new Date(a.session.lastSeen || 0);
    });

    // Find most recent active session to keep
    const keepIdx = sessions.findIndex(s => s.session.status === 'active' && !updates[`sessions/${s.sid}/status`]);

    sessions.forEach(({ sid, session }, i) => {
      if (i === keepIdx) return; // keep this one
      if (session.status === 'active' && !updates[`sessions/${sid}/status`]) {
        updates[`sessions/${sid}/status`] = 'closed';
        updates[`sessions/${sid}/durationText`] = 'مكرر - أُغلق تلقائياً';
        closed++;
      }
    });
  }

  if (Object.keys(updates).length === 0) {
    console.log('✅ No duplicates or stale sessions found!');
    process.exit(0);
  }

  console.log(`Closing ${closed} duplicate active sessions...`);
  console.log(`Closing ${stale} stale sessions (> 30 min old)...`);

  // Apply updates in batches of 500
  const updateKeys = Object.keys(updates);
  for (let i = 0; i < updateKeys.length; i += 500) {
    const batch = {};
    updateKeys.slice(i, i + 500).forEach(k => { batch[k] = updates[k]; });
    await db.ref('/').update(batch);
    console.log(`  Batch ${Math.floor(i/500)+1}: applied ${Object.keys(batch).length} updates`);
  }

  console.log(`\n✅ Done! Closed ${closed + stale} sessions total.`);
  console.log(`   Devices with duplicates: ${deviceCount}`);
  process.exit(0);
}

cleanup().catch(err => {
  console.error('Error:', err.message);
  process.exit(1);
});
