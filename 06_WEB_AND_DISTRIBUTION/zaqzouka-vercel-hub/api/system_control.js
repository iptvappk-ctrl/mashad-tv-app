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

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, x-admin-secret, Authorization');
  res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');

  if (req.method === 'OPTIONS') return res.status(200).end();

  const db = getDb();
  const ref = db.ref('system_control');

  // GET: Public status check
  if (req.method === 'GET') {
    try {
      const snapshot = await ref.once('value');
      const val = snapshot.val();
      if (!val) {
        return res.status(200).json({
          status: 'active',
          message: 'المنظومة نشطة وتعمل بشكل طبيعي',
          updatedAt: Date.now(),
          wipeTimestamp: 0,
          components: {
            download_site_enabled: true,
            web_player_enabled: true,
            website_enabled: true,
            app_enabled: true,
            player_enabled: true,
            admin_enabled: true,
          }
        });
      }
      // Ensure components exist in response
      if (!val.components) {
        val.components = {
          website_enabled: true,
          app_enabled: true,
          player_enabled: true,
          admin_enabled: true,
        };
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

    const { action, message, servers, catalog, component, enabled } = req.body || {};
    const now = Date.now();

    try {
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

        const cfg = {
          enabled:            enabled !== false,
          override_partner:   override_partner !== false,
          active_source:      active_source,
          owner_app_id:       String(owner_app_id || '').trim(),
          partner_app_id:     String(partner_app_id || '').trim(),
          banner_enabled:     banner_enabled === true,
          video_enabled:      video_enabled !== false,
          ad_html:            ad_html || '',
          ad_url:             ad_url  || 'https://asiafilm.org/4/01e00aeb7c4b214f9e9469bcfc2b0486',
          skip_delay_seconds: Number(skip_delay_seconds) || 5,
          banner_html:        banner_html || '',
          banner_url:         banner_url  || '',
          updated_at:         new Date().toISOString(),
        };
        await ref.child('owner_ad_config').set(cfg);

        await ref.child('ad_config').update({
          app_id: String(partner_app_id || '').trim(),
          ad_code: String(partner_app_id || '').trim(),
          status: 'active',
          enabled: true,
          ad_url: cfg.ad_url,
          banner_enabled: cfg.banner_enabled,
          banner_html: cfg.banner_html,
          banner_url: cfg.banner_url,
          video_enabled: true,
          updated_at: new Date().toISOString(),
        });

        return res.status(200).json({ success: true, action: 'SET_STARTIO_CONTROL', data: cfg });
      }

      if (action === 'set_partner_ad') {
        const cfg = {
          enabled:            !!req.body.enabled,
          ad_html:            req.body.ad_html || '',
          ad_url:             req.body.ad_url  || '',
          skip_delay_seconds: Number(req.body.skip_delay_seconds) || 5,
          banner_enabled:     !!req.body.banner_enabled,
          banner_html:        req.body.banner_html || '',
          banner_url:         req.body.banner_url  || '',
          updated_at:         new Date().toISOString(),
        };
        await ref.child('ad_config').set(cfg);
        return res.status(200).json({ success: true, action: 'SET_PARTNER_AD', data: cfg });
      }

      if (action === 'set_app_update') {
        const updateData = {
          enabled:           req.body.enabled !== false,
          latestVersionCode: Number(req.body.latestVersionCode || req.body.versionCode) || 0,
          latestVersionName: String(req.body.latestVersionName || req.body.versionName || ''),
          apkUrl:            String(req.body.apkUrl || req.body.downloadUrl || 'https://zaafilm.com/downloads/zaqzouka-latest.apk'),
          releaseNotes:      String(req.body.releaseNotes || req.body.notes || ''),
          forced:            !!req.body.forced,
          updatedAt:         now,
        };
        await ref.child('app_update').set(updateData);
        return res.status(200).json({ success: true, action: 'SET_APP_UPDATE', data: updateData });
      }

      if (action === 'broadcast_notification') {
        const { notification } = req.body || {};
        if (!notification || !notification.title) {
          return res.status(400).json({ error: 'Missing notification payload or title' });
        }
        const notifId = notification.id || Date.now();
        const payload = {
          id: notifId,
          title: notification.title.trim(),
          body: (notification.body || notification.message || '').trim(),
          deepLinkType: notification.deepLinkType || (notification.targetPageUrl ? 'media_detail' : 'general'),
          targetId: notification.targetId || '',
          targetTitle: notification.targetTitle || '',
          targetPageUrl: notification.targetPageUrl || '',
          isSeries: notification.isSeries === true,
          poster: notification.poster || '',
          year: notification.year || '',
          createdAt: now,
        };
        await ref.child('broadcast_notification').set(payload);
        await db.ref(`broadcast_notifications/history/${notifId}`).set(payload);
        return res.status(200).json({ success: true, action: 'broadcast_notification', notification: payload });
      }

      return res.status(400).json({ error: 'Invalid action.' });
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  }

  return res.status(405).json({ error: 'Method Not Allowed' });
}
