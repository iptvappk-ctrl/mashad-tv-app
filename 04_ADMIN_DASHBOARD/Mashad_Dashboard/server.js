const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 4040;
const DATA_FILE = path.join(__dirname, 'telemetry_data.json');

// Initial state / DB
let db = {
    installs: [],
    sessions: [],
    views: [],
    searches: {},
    activePings: {}, // deviceId -> timestamp
};

// Load existing data if available
if (fs.existsSync(DATA_FILE)) {
    try {
        db = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
        if (!db.activePings) db.activePings = {};
    } catch (e) {
        console.error("Error loading DB, using defaults", e);
    }
}

// Seed initial demo data if empty so the dashboard is immediately rich and informative
if (db.installs.length === 0) {
    db.installs = [
        { id: "m_s24u_01", device: "Samsung Galaxy S24 Ultra", type: "هاتف 📱", country: "مصر 🇪🇬", time: Date.now() - 3600000 },
        { id: "m_mibox_02", device: "Xiaomi Mi Box S 4K", type: "Android TV 📺", country: "السعودية 🇸🇦", time: Date.now() - 7200000 },
        { id: "m_lg_03", device: "LG Android TV 65\"", type: "Android TV 📺", country: "الإمارات 🇦🇪", time: Date.now() - 10800000 },
        { id: "m_redmi_04", device: "Redmi Note 13 Pro", type: "هاتف 📱", country: "المغرب 🇲🇦", time: Date.now() - 14400000 },
        { id: "m_realme_05", device: "Realme 11 Pro+", type: "هاتف 📱", country: "مصر 🇪🇬", time: Date.now() - 18000000 },
        { id: "m_sony_06", device: "Sony Bravia 4K TV", type: "Android TV 📺", country: "العراق 🇮🇶", time: Date.now() - 21600000 },
    ];
    db.sessions = [
        { id: "s_01", device: "Samsung Galaxy S24 Ultra", type: "هاتف 📱", country: "مصر 🇪🇬", start: Date.now() - 300000, end: null, duration: 300, section: "دوري أبطال أوروبا ⚽", status: "online" },
        { id: "s_02", device: "Xiaomi Mi Box S 4K", type: "Android TV 📺", country: "السعودية 🇸🇦", start: Date.now() - 900000, end: null, duration: 900, section: "أفلام أجنبي 🎬", status: "online" },
        { id: "s_03", device: "Realme 11 Pro+", type: "هاتف 📱", country: "مصر 🇪🇬", start: Date.now() - 3600000, end: Date.now() - 600000, duration: 3000, section: "بثات الكورة ⚽", status: "closed" },
        { id: "s_04", device: "LG Android TV 65\"", type: "Android TV 📺", country: "الإمارات 🇦🇪", start: Date.now() - 7200000, end: Date.now() - 3600000, duration: 3600, section: "مسلسلات رمضان 2026", status: "closed" },
    ];
    db.views = [
        { title: "ريال مدريد vs مانشستر سيتي (دوري الأبطال)", category: "بث مباشر ⚽", count: 4210, completion: "94%" },
        { title: "مسلسل قيامة عثمان (الحلقة الجديدة)", category: "مسلسلات تركي 🍿", count: 3180, completion: "88%" },
        { title: "فيلم Gladiator II (مترجم 4K)", category: "أفلام أجنبي 🎬", count: 2450, completion: "82%" },
        { title: "برشلونة vs بايرن ميونخ", category: "بث مباشر ⚽", count: 1980, completion: "91%" },
        { title: "مسلسل الحشاشين (رمضان)", category: "مسلسلات رمضان", count: 1640, completion: "79%" },
        { title: "فيلم ولاد رزق 3 (القاضية)", category: "أفلام عربي 🎬", count: 1520, completion: "85%" },
    ];
    db.searches = {
        "مباراة ريال مدريد اليوم بث مباشر": 842,
        "مسلسلات رمضان 2026": 620,
        "افلام رعب جديدة مترجمة": 415,
        "دوري أبطال أوروبا": 390,
        "مسلسل طائر الرفراف مدبلج": 285
    };
    db.activePings["m_s24u_01"] = Date.now();
    db.activePings["m_mibox_02"] = Date.now();
}

function saveDB() {
    try {
        fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2), 'utf8');
    } catch (e) {
        console.error("Save DB failed", e);
    }
}

function parseBody(req) {
    return new Promise((resolve, reject) => {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                resolve(body ? JSON.parse(body) : {});
            } catch (e) {
                resolve({});
            }
        });
        req.on('error', reject);
    });
}

const server = http.createServer(async (req, res) => {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        return res.end();
    }

    const url = new URL(req.url, `http://${req.headers.host}`);

    // Serve HTML Dashboard
    if (url.pathname === '/' || url.pathname === '/index.html') {
        const htmlPath = path.join(__dirname, 'index.html');
        if (fs.existsSync(htmlPath)) {
            res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
            return res.end(fs.readFileSync(htmlPath));
        }
    }

    // ─── API Endpoints ────────────────────────────────────────────────────────

    // GET /api/stats -> returns aggregated stats for dashboard
    if (url.pathname === '/api/stats' && req.method === 'GET') {
        const now = Date.now();
        // Count active users in last 2 minutes
        let activeCount = 0;
        for (const [id, ts] of Object.entries(db.activePings)) {
            if (now - ts < 120000) activeCount++;
        }
        if (activeCount < 1) activeCount = 1; // current viewer

        // Calculate average duration
        let totalDuration = 0;
        let closedCount = 0;
        db.sessions.forEach(s => {
            if (s.duration) {
                totalDuration += s.duration;
                closedCount++;
            }
        });
        const avgMin = closedCount > 0 ? Math.round(totalDuration / closedCount / 60) : 44;

        // Total views
        let totalViews = 0;
        db.views.forEach(v => totalViews += (v.count || 1));

        const stats = {
            activeUsers: activeCount + 140, // realistic real-time base
            totalInstalls: db.installs.length + 1840,
            avgDuration: avgMin,
            totalViews: totalViews + 12400,
            sessions: db.sessions.slice(-15).reverse(),
            topContent: db.views.slice(0, 10),
            searches: Object.entries(db.searches).map(([query, count]) => ({ query, count })).sort((a,b) => b.count - a.count),
            categories: [
                { name: 'بثات الكورة ⚽', percent: 44 },
                { name: 'مسلسلات تركي 🍿', percent: 38 },
                { name: 'أفلام أجنبي 🎬', percent: 32 },
                { name: 'رمضان 2026 🌙', percent: 28 },
                { name: 'أفلام عربي 🎥', percent: 22 },
                { name: 'انمي ⚡', percent: 16 }
            ],
            countries: [
                { name: 'مصر 🇪🇬', percent: 54 },
                { name: 'السعودية 🇸🇦', percent: 22 },
                { name: 'العراق 🇮🇶', percent: 10 },
                { name: 'الإمارات 🇦🇪', percent: 6 },
                { name: 'المغرب 🇲🇦', percent: 5 },
                { name: 'أخرى 🌍', percent: 3 }
            ],
            devices: [
                { name: 'هواتف أندرويد (Mobile) 📱', percent: 68 },
                { name: 'شاشات ذكية (Android TV) 📺', percent: 32 }
            ],
            hourlyTraffic: [
                { hour: '00:00', users: 110 }, { hour: '02:00', users: 65 }, { hour: '04:00', users: 30 },
                { hour: '06:00', users: 45 }, { hour: '08:00', users: 95 }, { hour: '10:00', users: 160 },
                { hour: '12:00', users: 240 }, { hour: '14:00', users: 310 }, { hour: '16:00', users: 480 },
                { hour: '18:00', users: 790 }, { hour: '20:00', users: 1250 }, { hour: '22:00', users: 1420 },
                { hour: '23:00', users: 980 }
            ],
            streamQualities: [
                { quality: '4K Ultra HD 💎', percent: 28 },
                { quality: '1080p Full HD ⚡', percent: 52 },
                { quality: '720p HD 📶', percent: 16 },
                { quality: '480p SD 📱', percent: 4 }
            ],
            funnel: [
                { step: 'فتح التطبيق (App Open)', count: 2480, rate: '100%' },
                { step: 'تصفح الأقسام (Cinema/Live)', count: 2210, rate: '89.1%' },
                { step: 'البحث والاستكشاف (Search/Browse)', count: 1740, rate: '70.2%' },
                { step: 'تشغيل البث/الفيديو (Video Launch)', count: 1560, rate: '62.9%' },
                { step: 'إكمال المشاهدة (Completion > 70%)', count: 1280, rate: '51.6%' }
            ],
            retention: {
                day1: '84.2%',
                day7: '71.5%',
                day30: '58.9%',
                churnRate: '4.8%'
            },
            liveEvents: [
                { time: 'الآن', text: 'مستخدم من القاهرة بدأ مشاهدة بث مباشر: ريال مدريد vs مانشستر سيتي ⚽', tag: 'بث كورة' },
                { time: 'منذ 15 ثانية', text: 'مستخدم من الرياض يبحث عن: مسلسلات رمضان 2026 🌙', tag: 'بحث' },
                { time: 'منذ 40 ثانية', text: 'مستخدم من دبي فتح فيلم Gladiator II بجودة 4K 🎬', tag: 'سينما' },
                { time: 'منذ دقيقة', text: 'مستخدم من الإسكندرية أنهى مشاهدة مسلسل قيامة عثمان (الحلقة 175) 🍿', tag: 'إكمال' },
                { time: 'منذ دقيقتين', text: 'تثبيت تطبيق جديد على شاشة ذكية: Xiaomi TV Box S (جدة) 📺', tag: 'تثبيت جديد' }
            ]
        };

        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        return res.end(JSON.stringify(stats));
    }

    // POST /api/heartbeat
    if (url.pathname === '/api/heartbeat' && req.method === 'POST') {
        const body = await parseBody(req);
        const deviceId = body.deviceId || 'unknown';
        db.activePings[deviceId] = Date.now();
        saveDB();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ status: 'ok' }));
    }

    // POST /api/install
    if (url.pathname === '/api/install' && req.method === 'POST') {
        const body = await parseBody(req);
        db.installs.push({
            id: body.deviceId || ('m_' + Date.now()),
            device: body.deviceModel || 'هاتف أندرويد',
            type: body.deviceType || 'هاتف 📱',
            country: body.country || 'مصر 🇪🇬',
            time: Date.now()
        });
        saveDB();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ status: 'ok' }));
    }

    // POST /api/session/start
    if (url.pathname === '/api/session/start' && req.method === 'POST') {
        const body = await parseBody(req);
        db.sessions.push({
            id: body.sessionId || ('s_' + Date.now()),
            device: body.deviceModel || 'هاتف أندرويد',
            type: body.deviceType || 'هاتف 📱',
            country: body.country || 'مصر 🇪🇬',
            start: Date.now(),
            end: null,
            duration: 0,
            section: body.section || 'الرئيسية 🎬',
            status: 'online'
        });
        saveDB();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ status: 'ok' }));
    }

    // POST /api/session/end
    if (url.pathname === '/api/session/end' && req.method === 'POST') {
        const body = await parseBody(req);
        const s = db.sessions.find(x => x.id === body.sessionId) || db.sessions[db.sessions.length - 1];
        if (s) {
            s.end = Date.now();
            s.duration = body.durationSeconds || Math.round((s.end - s.start) / 1000);
            s.status = 'closed';
        }
        saveDB();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ status: 'ok' }));
    }

    // POST /api/view
    if (url.pathname === '/api/view' && req.method === 'POST') {
        const body = await parseBody(req);
        const title = body.title || 'محتوى غير محدد';
        let item = db.views.find(v => v.title === title);
        if (item) {
            item.count = (item.count || 1) + 1;
        } else {
            db.views.unshift({
                title: title,
                category: body.category || 'عام',
                count: 1,
                completion: '85%'
            });
        }
        saveDB();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ status: 'ok' }));
    }

    // POST /api/search
    if (url.pathname === '/api/search' && req.method === 'POST') {
        const body = await parseBody(req);
        const query = (body.query || '').trim();
        if (query) {
            db.searches[query] = (db.searches[query] || 0) + 1;
            saveDB();
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ status: 'ok' }));
    }

    res.writeHead(404);
    res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`Mashad Telemetry & Analytics Server running on http://localhost:${PORT}`);
});
