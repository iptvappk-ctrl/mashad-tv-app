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
      admin.initializeApp({ credential: admin.credential.cert(sa), databaseURL });
    } else {
      admin.initializeApp({ databaseURL, projectId });
    }
  }
  return admin.database();
}

const ADMIN_SECRET = process.env.ADMIN_SECRET || 'Zq!7#mK9xPd@3wR5nT';
const MASTER_SECRET = process.env.MASTER_SECRET || 'zaqzouka_master_2026';

let _sysCache = null;
let _sysCacheTime = 0;
const SYS_CACHE_TTL_MS = 30_000;

function sanitizeSystemControl(raw, normalizeAppDisabled = true) {
  const val = raw && typeof raw === 'object' ? { ...raw } : {};
  delete val.secret;
  delete val.action;
  if (!val.status) val.status = 'active';
  if (!val.message) val.message = 'المنظومة نشطة وتعمل بشكل طبيعي';
  if (val.wipeTimestamp === undefined) val.wipeTimestamp = 0;
  if (!val.components || typeof val.components !== 'object') {
    val.components = {
      download_site_enabled: true,
      web_player_enabled: true,
      website_enabled: true,
      app_enabled: true,
      player_enabled: true,
      admin_enabled: true,
    };
  } else {
    val.components = { ...val.components };
    if (val.components.download_site_enabled === undefined) val.components.download_site_enabled = true;
    if (val.components.web_player_enabled === undefined) val.components.web_player_enabled = true;
    if (val.components.website_enabled === undefined) val.components.website_enabled = true;
    if (val.components.app_enabled === undefined) val.components.app_enabled = true;
    if (val.components.player_enabled === undefined) val.components.player_enabled = true;
    if (val.components.admin_enabled === undefined) val.components.admin_enabled = true;
  }
  if (normalizeAppDisabled && val.status === 'active' && val.components.app_enabled === false) {
    val.status = 'frozen';
    val.message = val.message || 'عفواً، التطبيق متوقف حالياً للصيانة.';
  }
  return val;
}

export default async function handler(req, res) {
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, x-admin-secret, X-Admin-Secret, x-master-secret, x-zaqzouka-key, Authorization');

  const isAdminReq = Boolean(
    req.headers['x-admin-secret'] ||
    req.headers['x-master-secret'] ||
    (req.query && req.query.nocache === '1')
  );

  if (req.method === 'GET' && !isAdminReq) {
    res.setHeader('Cache-Control', 'public, max-age=15, s-maxage=60, stale-while-revalidate=300');
  } else {
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  }

  if (req.method === 'OPTIONS') return res.status(200).end();

  const db = getDb();
  const ref = db.ref('system_control');

  // GET: Public or Admin status check
  if (req.method === 'GET') {
    const now = Date.now();
    if (!isAdminReq && _sysCache && now - _sysCacheTime < SYS_CACHE_TTL_MS) {
      return res.status(200).json({ ..._sysCache, _source: 'memory_cache' });
    }
    try {
      const snapshot = await ref.once('value');
      const normalized = sanitizeSystemControl(snapshot.val(), !isAdminReq);
      if (!isAdminReq) {
        _sysCache = normalized;
        _sysCacheTime = now;
      }
      return res.status(200).json({ ...normalized, _source: 'firebase_live' });
    } catch (err) {
      if (_sysCache) {
        return res.status(200).json({ ..._sysCache, _source: 'memory_stale' });
      }
      return res.status(200).json({ ...sanitizeSystemControl(null, true), _error: err.message, _source: 'fallback' });
    }
  }

  // POST: Admin Control actions (Secured)
  if (req.method === 'POST') {
    const secret = req.headers['x-admin-secret'] || req.headers['x-master-secret'] || (req.body && req.body.secret);
    if (secret !== ADMIN_SECRET && secret !== MASTER_SECRET) {
      return res.status(401).json({ error: 'Unauthorized: Invalid Admin Secret' });
    }
    _sysCache = null;
    _sysCacheTime = 0;

    const { action, message, servers, catalog, component, enabled } = req.body || {};
    const now = Date.now();

    try {
      if (action === 'set_app_update') {
        const b = req.body || {};
        const updateData = {
          enabled: b.enabled !== false,
          latestVersionCode: Number(b.latestVersionCode || b.versionCode) || 0,
          latestVersionName: String(b.latestVersionName || b.versionName || ''),
          apkUrl: String(b.apkUrl || b.downloadUrl || 'https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.83-90/zaqzouka-1.0.83-90.apk'),
          releaseNotes: String(b.releaseNotes || b.notes || ''),
          forced: !!b.forced,
          updatedAt: now,
        };
        await db.ref('system_control/app_update').set(updateData);
        await ref.update({ updatedAt: now });
        return res.status(200).json({ ok: true, success: true, action: 'set_app_update', data: updateData });
      }

      // NEW: Toggle individual component
      if (action === 'set_component') {
        if (!component) return res.status(400).json({ error: 'Missing component name' });
        const validComponents = ['download_site_enabled', 'web_player_enabled', 'website_enabled', 'app_enabled', 'player_enabled', 'admin_enabled'];
        if (!validComponents.includes(component)) {
          return res.status(400).json({ error: 'Invalid component name' });
        }
        await db.ref(`system_control/components/${component}`).set(enabled !== false);
        await ref.update({ updatedAt: now, updatedBy: 'master_control' });
        return res.status(200).json({ success: true, action: 'set_component', component, enabled: enabled !== false });
      }

      // Sovereign Owner Ad Configuration & Start.io Control (خاص بالمالك وتجاوز كود الشريك)
      if (action === 'set_startio_control' || action === 'set_owner_ad') {
        const {
          enabled,
          override_partner,
          active_source = 'owner', // 'owner' | 'partner' | 'disabled'
          owner_app_id = '',
          partner_app_id = '',
          banner_enabled = false,
          video_enabled = true,
          ad_html,
          ad_url,
          skip_delay_seconds,
          banner_html,
          banner_url,
        } = req.body || {};

        const payload = {
          enabled: enabled !== false,
          override_partner: override_partner !== false,
          active_source: active_source,
          owner_app_id: String(owner_app_id || '').trim(),
          partner_app_id: String(partner_app_id || '').trim(),
          banner_enabled: banner_enabled === true,
          video_enabled: video_enabled !== false,
          ad_html: ad_html || '',
          ad_url: ad_url || 'https://asiafilm.org/4/01e00aeb7c4b214f9e9469bcfc2b0486',
          skip_delay_seconds: Number(skip_delay_seconds) || 5,
          banner_html: banner_html || '',
          banner_url: banner_url || '',
          updatedAt: now,
        };

        await db.ref('system_control/owner_ad_config').set(payload);

        const partnerRef = db.ref('system_control/ad_config');
        await partnerRef.update({
          app_id: String(partner_app_id || '').trim(),
          ad_code: String(partner_app_id || '').trim(),
          status: 'active',
          enabled: true,
          ad_url: payload.ad_url,
          banner_enabled: payload.banner_enabled,
          banner_html: payload.banner_html,
          banner_url: payload.banner_url,
          video_enabled: true,
          updated_at: new Date().toISOString(),
        });

        return res.status(200).json({ success: true, action: 'set_startio_control', config: payload });
      }

      // Partner Ad Configuration (من شاشة تحكم الشريك)
      if (action === 'set_partner_ad') {
        const cfg = {
          enabled:            req.body.enabled !== false,
          ad_code:            req.body.ad_code || req.body.ad_html || '',
          ad_html:            req.body.ad_html || req.body.ad_code || '',
          ad_url:             req.body.ad_url  || '',
          skip_delay_seconds: Number(req.body.skip_delay_seconds) || 5,
          banner_enabled:     req.body.banner_enabled !== false,
          banner_html:        req.body.banner_html || req.body.ad_code || '',
          banner_url:         req.body.banner_url  || '',
          updated_at:         new Date().toISOString(),
        };
        await db.ref('system_control/ad_config').set(cfg);
        return res.status(200).json({ success: true, action: 'set_partner_ad', data: cfg });
      }

      if (action === 'freeze') {
        const payload = {
          status: 'frozen',
          message: message || 'عفواً، الخدمة متوقفة حالياً للتحديث والصيانة.',
          updatedAt: now,
          wipeTimestamp: 0,
          components: {
            download_site_enabled: false,
            web_player_enabled: false,
            website_enabled: false,
            app_enabled: false,
            player_enabled: false,
            admin_enabled: false,
          }
        };
        await ref.set(payload);
        return res.status(200).json({ success: true, action: 'freeze', state: payload });
      }

      if (action === 'wipe') {
        const payload = {
          status: 'wiped',
          message: 'تم تصفير وإغلاق الخدمة نهائياً.',
          updatedAt: now,
          wipeTimestamp: now,
          components: {
            download_site_enabled: false,
            web_player_enabled: false,
            website_enabled: false,
            app_enabled: false,
            player_enabled: false,
            admin_enabled: false,
          }
        };
        await ref.set(payload);
        try {
          const wipePaths = [
            'servers_catalog', 'cached_feeds', 'channels', 'live_sessions',
            'analytics', 'devices', 'playlists', 'sessions', 'searches',
            'stats', 'activityLog', 'errors', 'topContent'
          ];
          await Promise.all(wipePaths.map(p => db.ref(p).remove()));
        } catch (e) {}
        return res.status(200).json({ success: true, action: 'wipe', state: payload });
      }

      if (action === 'restore') {
        if (servers && typeof servers === 'object') await db.ref('servers_catalog').set(servers);
        if (catalog && typeof catalog === 'object') await db.ref('cached_feeds').set(catalog);
        const payload = {
          status: 'active',
          message: message || 'المنظومة نشطة وتعمل بكامل طاقتها.',
          updatedAt: now,
          wipeTimestamp: 0,
          components: {
            download_site_enabled: true,
            web_player_enabled: true,
            website_enabled: true,
            app_enabled: true,
            player_enabled: true,
            admin_enabled: true,
          }
        };
        await ref.set(payload);
        return res.status(200).json({ success: true, action: 'restore', state: payload });
      }

      return res.status(400).json({ error: 'Invalid action.' });
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  }

  return res.status(405).json({ error: 'Method Not Allowed' });
}
