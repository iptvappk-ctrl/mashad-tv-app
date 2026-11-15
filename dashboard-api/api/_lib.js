// Shared Firebase Admin + DB helpers used by all Vercel serverless functions
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

function initFirebase() {
  if (admin.apps.length > 0) return admin.apps[0];
  const projectId = process.env.FIREBASE_PROJECT_ID || 'madartv';
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
  const privateKey = process.env.FIREBASE_PRIVATE_KEY
    ? process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n')
    : undefined;
  const databaseURL =
    process.env.FIREBASE_DATABASE_URL ||
    `https://${projectId}-default-rtdb.firebaseio.com`;

  if (clientEmail && privateKey) {
    return admin.initializeApp({
      credential: admin.credential.cert({ projectId, clientEmail, privateKey }),
      databaseURL,
    });
  }

  // Fallback to local service-account.json if present
  const saPath = path.resolve(__dirname, '../service-account.json');
  if (fs.existsSync(saPath)) {
    const sa = JSON.parse(fs.readFileSync(saPath, 'utf8'));
    return admin.initializeApp({
      credential: admin.credential.cert(sa),
      databaseURL,
    });
  }

  return admin.initializeApp({ databaseURL, projectId });
}

initFirebase();
const db = admin.database();

// ─── Country helpers ──────────────────────────────────────────────────────────
const COUNTRY_MAP = {
  EG:'مصر',SA:'السعودية',AE:'الإمارات',IQ:'العراق',MA:'المغرب',
  KW:'الكويت',DZ:'الجزائر',QA:'قطر',OM:'عمان',JO:'الأردن',
  LB:'لبنان',TN:'تونس',SD:'السودان',LY:'ليبيا',YE:'اليمن',
  SY:'سوريا',PS:'فلسطين',BH:'البحرين',US:'أمريكا',GB:'بريطانيا',
  DE:'ألمانيا',FR:'فرنسا',TR:'تركيا',IT:'إيطاليا',ES:'إسبانيا',
  CA:'كندا',SE:'السويد',NL:'هولندا',AU:'أستراليا',BE:'بلجيكا',
};

function resolveCountry(countryCode, timezone) {
  let code = (countryCode || '').trim().toUpperCase();
  const tz = (timezone || '').toLowerCase();
  if (!code || code === 'US') {
    if (tz.includes('cairo') || tz.includes('egypt') || tz.includes('africa/cairo')) code = 'EG';
    else if (tz.includes('riyadh') || tz.includes('saudi')) code = 'SA';
    else if (tz.includes('dubai') || tz.includes('abu_dhabi')) code = 'AE';
    else if (tz.includes('baghdad')) code = 'IQ';
    else if (tz.includes('casablanca')) code = 'MA';
    else if (tz.includes('kuwait')) code = 'KW';
    else if (tz.includes('qatar') || tz.includes('doha')) code = 'QA';
    else if (tz.includes('beirut')) code = 'LB';
    else if (!code) code = 'EG';
  }
  const name = COUNTRY_MAP[code];
  return { code, name: name ? `${name} (${code})` : code };
}

function sendJson(res, status, body) {
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Admin-Secret');
  res.setHeader('Cache-Control', 'no-store');
  res.statusCode = status;
  res.end(JSON.stringify(body));
}

function handleCors(res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Admin-Secret');
  res.setHeader('Cache-Control', 'no-store');
}

module.exports = { db, resolveCountry, sendJson, handleCors };
