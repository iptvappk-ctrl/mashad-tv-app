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
      // Ensure status exists
      if (!val.status) val.status = 'active';
      // Ensure components exist in response
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
        if (val.components.download_site_enabled === undefined) val.components.download_site_enabled = true;
        if (val.components.web_player_enabled === undefined) val.components.web_player_enabled = true;
        if (val.components.website_enabled === undefined) val.components.website_enabled = true;
        if (val.components.app_enabled === undefined) val.components.app_enabled = true;
        if (val.components.player_enabled === undefined) val.components.player_enabled = true;
        if (val.components.admin_enabled === undefined) val.components.admin_enabled = true;
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

      // Sovereign Owner Ad Configuration (خاص بالمالك وتجاوز كود الشريك)
      if (action === 'set_owner_ad') {
        const {
          enabled,
          override_partner,
          ad_html,
          ad_url,
          skip_delay_seconds,
          banner_enabled,
          banner_html,
          banner_url,
        } = req.body || {};

        const payload = {
          enabled: !!enabled,
          override_partner: override_partner !== false,
          ad_html: ad_html || '',
          ad_url: ad_url || '',
          skip_delay_seconds: Number(skip_delay_seconds) || 5,
          banner_enabled: !!banner_enabled,
          banner_html: banner_html || '',
          banner_url: banner_url || '',
          updatedAt: now,
        };

        await db.ref('system_control/owner_ad_config').set(payload);
        return res.status(200).json({ success: true, action: 'set_owner_ad', config: payload });
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
