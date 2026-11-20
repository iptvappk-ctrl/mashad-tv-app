// Vercel Serverless Function: /api/system_control
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

function getDb() {
  if (admin.apps.length > 0) return admin.database();
  const projectId = process.env.FIREBASE_PROJECT_ID || 'madartv';
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
  const privateKey = process.env.FIREBASE_PRIVATE_KEY
    ? process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n')
    : undefined;
  const databaseURL =
    process.env.FIREBASE_DATABASE_URL ||
    `https://${projectId}-default-rtdb.firebaseio.com`;

  if (clientEmail && privateKey) {
    admin.initializeApp({
      credential: admin.credential.cert({ projectId, clientEmail, privateKey }),
      databaseURL,
    });
  } else {
    const saPath = path.resolve(__dirname, '../service-account.json');
    if (fs.existsSync(saPath)) {
      const sa = JSON.parse(fs.readFileSync(saPath, 'utf8'));
      admin.initializeApp({
        credential: admin.credential.cert(sa),
        databaseURL,
      });
    } else {
      admin.initializeApp({ databaseURL, projectId });
    }
  }
  return admin.database();
}

const ADMIN_SECRET = process.env.ADMIN_SECRET || 'Zq!7#mK9xPd@3wR5nT';

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, x-admin-secret, Authorization');
  res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const db = getDb();
  const ref = db.ref('system_control');

  // GET: Public status check (Every user phone calls this)
  if (req.method === 'GET') {
    try {
      const snapshot = await ref.once('value');
      const val = snapshot.val();
      if (!val) {
        return res.status(200).json({
          status: 'active',
          message: 'المنظومة نشطة وتعمل بشكل طبيعي',
          updatedAt: Date.now(),
          wipeTimestamp: 0
        });
      }
      return res.status(200).json(val);
    } catch (err) {
      return res.status(500).json({ error: err.message, status: 'active' });
    }
  }

  // POST: Admin Control actions (Secured)
  if (req.method === 'POST') {
    const secret = req.headers['x-admin-secret'] || (req.body && req.body.secret);
    if (secret !== ADMIN_SECRET) {
      return res.status(401).json({ error: 'Unauthorized: Invalid Admin Secret' });
    }

    const { action, message, servers, catalog } = req.body || {};
    const now = Date.now();

    try {
      if (action === 'freeze') {
        const payload = {
          status: 'frozen',
          message: message || 'عفواً، الخدمة متوقفة حالياً للصيانة والتحديث.',
          updatedAt: now,
          wipeTimestamp: 0
        };
        await ref.set(payload);
        return res.status(200).json({ success: true, action: 'freeze', state: payload });
      }

      if (action === 'wipe') {
        // 1. Set status to wiped with timestamp
        const payload = {
          status: 'wiped',
          message: 'تم تصفير وإغلاق الخدمة نهائياً.',
          updatedAt: now,
          wipeTimestamp: now
        };
        await ref.set(payload);

        // 2. Clear any servers / channels in database
        try {
          await db.ref('servers_catalog').remove();
          await db.ref('cached_feeds').remove();
        } catch (e) {}

        return res.status(200).json({
          success: true,
          action: 'wipe',
          state: payload,
          note: 'Cloud data zeroed and wipe command dispatched to all devices.'
        });
      }

      if (action === 'restore') {
        // Restore servers if provided
        if (servers && typeof servers === 'object') {
          await db.ref('servers_catalog').set(servers);
        }
        if (catalog && typeof catalog === 'object') {
          await db.ref('cached_feeds').set(catalog);
        }

        const payload = {
          status: 'active',
          message: message || 'المنظومة نشطة وتعمل بكامل طاقتها.',
          updatedAt: now,
          wipeTimestamp: 0
        };
        await ref.set(payload);
        return res.status(200).json({ success: true, action: 'restore', state: payload });
      }

      return res.status(400).json({ error: 'Invalid action. Choose freeze, wipe, or restore.' });
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  }

  return res.status(405).json({ error: 'Method Not Allowed' });
}
