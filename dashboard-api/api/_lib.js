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

// ─── Country & City Helpers (Detailed Real Geolocation) ─────────────────────
const COUNTRY_MAP = {
  EG: 'مصر', SA: 'السعودية', AE: 'الإمارات', IQ: 'العراق', MA: 'المغرب',
  KW: 'الكويت', DZ: 'الجزائر', QA: 'قطر', OM: 'عمان', JO: 'الأردن',
  LB: 'لبنان', TN: 'تونس', SD: 'السودان', LY: 'ليبيا', YE: 'اليمن',
  SY: 'سوريا', PS: 'فلسطين', BH: 'البحرين', US: 'أمريكا', GB: 'بريطانيا',
  DE: 'ألمانيا', FR: 'فرنسا', TR: 'تركيا', IT: 'إيطاليا', ES: 'إسبانيا',
  CA: 'كندا', SE: 'السويد', NL: 'هولندا', AU: 'أستراليا', BE: 'بلجيكا',
};

const CITY_MAP_AR = {
  tanta: 'طنطا', cairo: 'القاهرة', giza: 'الجيزة', alexandria: 'الإسكندرية',
  mansoura: 'المنصورة', 'el mansoura': 'المنصورة', zagazig: 'الزقازيق',
  ismailia: 'الإسماعيلية', suez: 'السويس', 'port said': 'بورسعيد',
  fayoum: 'الفيوم', 'beni suef': 'بني سويف', minya: 'المنيا',
  asyut: 'أسيوط', sohag: 'سوهاج', qena: 'قنا', luxor: 'الأقصر',
  aswan: 'أسوان', hurghada: 'الغردقة', 'sharm el sheikh': 'شرم الشيخ',
  damanhur: 'دمنهور', 'kafr el sheikh': 'كفر الشيخ', banha: 'بنها',
  damietta: 'دمياط', 'shibin el kom': 'شبين الكوم', arish: 'العريش',
  'marsa matruh': 'مرسى مطروح', '6th of october': '6 أكتوبر',
  'new cairo': 'القاهرة الجديدة', 'nasr city': 'مدينة نصر',
  maadi: 'المعادي', heliopolis: 'مصر الجديدة', dokki: 'الدقي',
  mohandessin: 'المهندسين', riyadh: 'الرياض', jeddah: 'جدة',
  dammam: 'الدمام', mecca: 'مكة المكرمة', medina: 'المدينة المنورة',
  dubai: 'دبي', 'abu dhabi': 'أبو ظبي', sharjah: 'الشارقة',
  kuwait: 'الكويت', 'kuwait city': 'الكويت', doha: 'الدوحة',
  baghdad: 'بغداد', amman: 'عمان', beirut: 'بيروت', muscat: 'مسقط',
  manama: 'المنامة', tripoli: 'طرابلس', tunis: 'تونس', algiers: 'الجزائر',
  casablanca: 'الدار البيضاء', rabat: 'الرباط',
};

const REGION_MAP_AR = {
  gharbia: 'الغربية', cairo: 'القاهرة', giza: 'الجيزة', alexandria: 'الإسكندرية',
  dakahlia: 'الدقهلية', sharqia: 'الشرقية', monufia: 'المنوفية',
  qalyubia: 'القليوبية', beheira: 'البحيرة', 'kafr el sheikh': 'كفر الشيخ',
  damietta: 'دمياط', 'port said': 'بورسعيد', ismailia: 'الإسماعيلية',
  suez: 'السويس', 'red sea': 'البحر الأحمر', 'south sinai': 'جنوب سيناء',
  'north sinai': 'شمال سيناء', matruh: 'مطروح', fayoum: 'الفيوم',
  'beni suef': 'بني سويف', minya: 'المنيا', asyut: 'أسيوط', sohag: 'سوهاج',
  qena: 'قنا', luxor: 'الأقصر', aswan: 'أسوان', 'new valley': 'الوادي الجديد',
  riyadh: 'منطقة الرياض', makkah: 'منطقة مكة', 'eastern province': 'المنطقة الشرقية',
  dubai: 'إمارة دبي', 'abu dhabi': 'إمارة أبو ظبي',
};

function translateCity(city) {
  if (!city) return '';
  const clean = city.trim().toLowerCase();
  return CITY_MAP_AR[clean] || city.trim();
}

function translateRegion(region) {
  if (!region) return '';
  const clean = region.trim().toLowerCase();
  return REGION_MAP_AR[clean] || region.trim();
}

function resolveDetailedLocation(body, req) {
  let countryCode = (body.countryCode || '').trim().toUpperCase();
  const tz = (body.timezone || '').toLowerCase();
  
  // 1. Check if client explicitly sent city and region
  let city = (body.city || '').trim();
  let region = (body.region || '').trim();
  let isp = (body.isp || '').trim();

  // 2. Read Vercel / Edge Geo Headers
  const headers = req?.headers || {};
  const vercelCity = headers['x-vercel-ip-city'];
  const vercelRegion = headers['x-vercel-ip-country-region'];
  const vercelCountry = headers['x-vercel-ip-country'];

  if (!city && vercelCity) {
    try {
      city = decodeURIComponent(vercelCity);
    } catch {
      city = vercelCity;
    }
  }
  if (!region && vercelRegion) {
    region = vercelRegion;
  }
  if ((!countryCode || countryCode === 'US') && vercelCountry) {
    countryCode = vercelCountry.toUpperCase();
  }

  // 3. Timezone fallback
  if (!countryCode || countryCode === 'US') {
    if (tz.includes('cairo') || tz.includes('egypt') || tz.includes('africa/cairo')) countryCode = 'EG';
    else if (tz.includes('riyadh') || tz.includes('saudi')) countryCode = 'SA';
    else if (tz.includes('dubai') || tz.includes('abu_dhabi')) countryCode = 'AE';
    else if (tz.includes('baghdad')) countryCode = 'IQ';
    else if (tz.includes('casablanca')) countryCode = 'MA';
    else if (tz.includes('kuwait')) code = 'KW';
    else if (tz.includes('qatar') || tz.includes('doha')) countryCode = 'QA';
    else if (!countryCode) countryCode = 'EG';
  }

  // 4. In Egypt local network fallback if city not resolved yet
  if (!city && countryCode === 'EG') {
    if (tz.includes('cairo')) {
      city = 'القاهرة';
      region = 'القاهرة';
    }
  }

  const arCity = translateCity(city);
  const arRegion = translateRegion(region);
  const arCountryName = COUNTRY_MAP[countryCode] || countryCode;
  const countryFormatted = `${arCountryName} (${countryCode})`;

  let locationText = '';
  if (arCity && arRegion && arCity !== arRegion) {
    locationText = `${arCity}، ${arRegion}، ${countryFormatted}`;
  } else if (arCity) {
    locationText = `${arCity}، ${countryFormatted}`;
  } else {
    locationText = countryFormatted;
  }

  return {
    countryCode,
    country: countryFormatted,
    city: arCity,
    region: arRegion,
    location: locationText,
    isp: isp || 'WE / TE Data',
  };
}

// ─── Audience & Monetization Intelligence Helpers ───────────────────────────
function resolveAudienceSegment(content = '', section = '') {
  const text = `${content} ${section}`.toLowerCase();
  if (text.includes('مباراة') || text.includes('دوري') || text.includes('كورة') || text.includes('رياض') || text.includes('yacine') || text.includes('sports')) {
    return {
      id: 'sports_fans',
      name: 'متابعو الرياضة والمباريات ⚽',
      category: 'Sports & Live Events',
      adValueTier: 'High ($7 - $18 eCPM)',
      targetAdvertisers: 'المتاجر الرياضية، الملابس، الألعاب، المشروبات',
    };
  }
  if (text.includes('فيلم') || text.includes('أفلام') || text.includes('أكشن') || text.includes('رعب') || text.includes('cinema') || text.includes('movie')) {
    return {
      id: 'action_cinema',
      name: 'عشاق سينما الأكشن والإثارة 🎬',
      category: 'Action & Blockbuster Movies',
      adValueTier: 'Premium ($5 - $12 eCPM)',
      targetAdvertisers: 'شركات الهواتف، منصات الألعاب والـ Streaming، السيارات',
    };
  }
  if (text.includes('مسلسل') || text.includes('رمضان') || text.includes('تركي') || text.includes('كوري') || text.includes('دراما') || text.includes('series')) {
    return {
      id: 'drama_series',
      name: 'جمهور الدراما ومسلسلات رمضان 📺',
      category: 'Drama & Family Series',
      adValueTier: 'Very High ($6 - $15 eCPM)',
      targetAdvertisers: 'الأغذية والمشروبات (FMCG)، البنوك والتطبيقات المالية، التجارة الإلكترونية',
    };
  }
  if (text.includes('انمي') || text.includes('أنمي') || text.includes('anime') || text.includes('كرتون')) {
    return {
      id: 'anime_gamers',
      name: 'عشاق الأنمي والشباب والجيمنج ⚔️',
      category: 'Anime & Gaming Youth',
      adValueTier: 'High ($6 - $14 eCPM)',
      targetAdvertisers: 'إكسسوارات الجيمنج، ألعاب الفيديو، المتاجر الإلكترونية للشباب',
    };
  }
  return {
    id: 'general_entertainment',
    name: 'جمهور الترفيه والمنوعات 🍿',
    category: 'General Entertainment',
    adValueTier: 'Standard ($4 - $8 eCPM)',
    targetAdvertisers: 'عروض الاتصالات، التخفيضات الكبرى، المتاجر العامة',
  };
}

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

module.exports = {
  db,
  resolveCountry,
  resolveDetailedLocation,
  resolveAudienceSegment,
  sendJson,
  handleCors,
};
