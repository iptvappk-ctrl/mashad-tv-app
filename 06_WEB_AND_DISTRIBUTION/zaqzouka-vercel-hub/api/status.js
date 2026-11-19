/**
 * 🛡️ ZAQZOUKA QUOTA GUARD - Vercel Edge Function
 * /api/status  →  Public cached endpoint (READ ONLY)
 * 
 * الغرض: منع مليون مستخدم من فتح connection مباشر على Firebase
 * الحل:  Vercel CDN تخزن النتيجة 60 ثانية، فكل المستخدمين يقرأوا من CDN مش Firebase
 * 
 * 🔥 Firebase connections needed: 1 per 60 sec (not 1M!)
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// In-memory cache (Vercel warm function)
let _cache = null;
let _cacheTime = 0;
const MEMORY_TTL_MS = 30_000; // 30 seconds in-process cache

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
      admin.initializeApp({ credential: admin.credential.cert(sa), databaseURL });
    } else {
      admin.initializeApp({ databaseURL, projectId });
    }
  }
  return admin.database();
}

// Static fallback: if Firebase unreachable, serve "active"
const FALLBACK = {
  status: 'active',
  message: 'المنظومة نشطة وتعمل بشكل طبيعي',
  updatedAt: Date.now(),
  wipeTimestamp: 0,
  _source: 'fallback',
};

export default async function handler(req, res) {
  // CORS - allow all origins (apps call this)
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Vary', 'Accept-Encoding');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'GET') return res.status(405).json({ error: 'Method Not Allowed' });

  // 🏎️ LAYER 1: Vercel CDN edge cache (60s) - all 1M users hit cache, not Firebase
  res.setHeader('Cache-Control', 's-maxage=60, stale-while-revalidate=300');

  // 🏎️ LAYER 2: In-memory cache (30s) - warm Vercel functions skip Firebase
  const now = Date.now();
  if (_cache && now - _cacheTime < MEMORY_TTL_MS) {
    return res.status(200).json({ ..._cache, _source: 'memory_cache' });
  }

  // 🔥 LAYER 3: One real Firebase read (happens max once per 30s per region)
  try {
    const db = getDb();
    const snap = await db.ref('system_control').once('value');
    const val = snap.val();

    const result = val || FALLBACK;
    _cache = result;
    _cacheTime = now;

    return res.status(200).json({ ...result, _source: 'firebase_live' });
  } catch (err) {
    // Firebase unreachable → serve in-memory cache or fallback
    if (_cache) {
      return res.status(200).json({ ..._cache, _source: 'memory_stale', _error: err.message });
    }
    return res.status(200).json({ ...FALLBACK, _error: err.message });
  }
}
