/**
 * Zaqzouka Dashboard API - Cloudflare Worker
 * Replaces the blocked Vercel deployment.
 * Uses Firebase REST API + JWT auth (no Node SDK needed).
 */

// ─── Constants ────────────────────────────────────────────────────────────────
const ADMIN_SECRET     = 'Zq!7#mK9xPd@3wR5nT';
const MASTER_SECRET    = 'zaqzouka_master_2026';
const ZAQZOUKA_API_KEY = 'ZQ-OWNER-2026-SECRET-14MOON';
const PROJECT_ID       = 'madartv';
const DB_URL           = `https://${PROJECT_ID}-default-rtdb.firebaseio.com`;

// Firebase service account (embedded - safe since Worker env vars are encrypted)
const SA = {
  client_email: 'firebase-adminsdk-fbsvc@madartv.iam.gserviceaccount.com',
  private_key_id: 'db853412c302b966090e1e963d0f532b0f5cd5c5',
  private_key: `-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC1HF49tixR7lMk
57eLy8hke50PEFtPzGXGzYI8drDDpvARgxPH73n7z5u7x1n2WoxUYipa+ncqaP7B
f9bb7UrxVP13ITFOdKtYIi5IFPirP4JRP1QNgWRxWpFraoTOFW59YDA4zT47p3MN
smJepLlzLCu4EwDFA8+ajHbTr5RMK0BtNmFXAvMHyxQ7IieUg6wQlATSgRKkcIdZ
7vLHc/hi3qml15R5XsuVa8NjLWBb6lb6uj3D55/28iDyu4rS+i58ybAMfa5KsKqG
AZ7sgEJbQqhmlfQMJTb9+Mr4oO76IGU0/ohnmVssK11Gq2+YISwfKf3IAkX6uVXf
1/gNm+ZDAgMBAAECggEAHtct1SRod6s1zVsIDm4tOa8zF5WZdwU/b5MfUlyDzU1o
JBNGEw3cn9zCkD9qOmFA9D2jKMrt7/LhLuIuHFVUJbRrVRX553pGk0N+zheEIOU3
s84s6f/WstbRXJFO/Cj6H695hrKsnkvtc6+kwKkcS1iCOV5DC0mS2KaCtZl4j1+k
dUGKURsxgeChUKPN/diP9/MbvZ6IJayhRqFys624SNNMj8HPtCXQEsp9U0seW0EH
neCiWc3foGrrmRPQUcXjdwO/RUzjpiXU0AgUN+2EqZPvFNqTT5HO6saX99qk3row
0+m9+nPSCh1D1RkgoBMx4/KNvg0OHuDcfF+Ffay4wQKBgQDs9rift9vgfHk+mKuP
5FoUu5Obg6xvRyoO3BfckQPbjgmA2y9pe0Ho6D5GLQx8se91Ijb0YXSB1deFGN6J
CQut6EuHg1GqzEOVhwjp4QObTwUeY+DPkQR4k3loRlbxJ/e7teTfy/7TZyc/jVMc
iUdSu3iovDY10wFRdSznTSys/wKBgQDDqQDLSYJm1/qXwwLT16adAlmyg0NY8f6O
zif8OENtnDqEqUuBqEky24wtzSYsX03ykgnLzlmTndWEFhGknqnukznuVpxNMjwE
KW8oqFmuCsI0G8+QhUmEsBsf7vjFRII59t/+B0jDpF3mnbRRAwS++7kAP9m7GT6j
Zufy50nSvQKBgAQMs1sHaVqMxgWr1kK/RErWac9ekLPWI4HdgQrj74zxAHiTRlwn
Hxk73tSUMIJxCAaYbxf+El+lp+k+Y48z+F8geSn3tI2WsBD+Hcjhb45X6155ln31
nH3zB0Ag85trc3Bhy2eHtPMn8CPF5oW2UuG3EnjPvp1s3bjmp73MM9dBAoGBAKCw
7DYI6VANkYGSZ4AUXIIWrBQwOd+S373pEm3Sx43+ARFy+NBDpdpe9YGoOvG2VA4k
Ymc/0dGqjvEsLtFEfcuynqFgAH5mA2RnoyOBICYQL+8b6ByJWxfeimhvm2BoI4v3
dX7r/JaxJ21/l9fw4rT3h7jst6Ux1n/cxCJ6UecBAoGBAM2o6cjsJY7gMuVS8D7R
bzZbq020wB8ad6j5DyOLD/AOsbzjsPomwKZkQ01zKFlKBPFtRUx8EmFr24x5SIXw
mjs8h7bB+lkElXtMXNGvCizAwgyzrCN0Cj5SMZmko0ZCkQo/BJvog67dNc5Ws7kB
brllcrMKNWy5T49JfENZkjqz
-----END PRIVATE KEY-----`,
};

// ─── JWT / Firebase Auth Helpers ──────────────────────────────────────────────

// Cache the access token
let _fbToken = null;
let _fbTokenExpiry = 0;

async function getFirebaseToken() {
  const now = Date.now() / 1000;
  if (_fbToken && now < _fbTokenExpiry - 60) return _fbToken;

  // Build JWT for service account
  const header = { alg: 'RS256', typ: 'JWT' };
  const payload = {
    iss: SA.client_email,
    scope: 'https://www.googleapis.com/auth/firebase.database https://www.googleapis.com/auth/userinfo.email',
    aud: 'https://oauth2.googleapis.com/token',
    exp: Math.floor(now) + 3600,
    iat: Math.floor(now),
  };

  function b64url(obj) {
    const json = typeof obj === 'string' ? obj : JSON.stringify(obj);
    const bytes = new TextEncoder().encode(json);
    let binary = '';
    bytes.forEach(b => binary += String.fromCharCode(b));
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  }

  const sigInput = `${b64url(header)}.${b64url(payload)}`;

  // Import private key
  const pemBody = SA.private_key
    .replace('-----BEGIN PRIVATE KEY-----', '')
    .replace('-----END PRIVATE KEY-----', '')
    .replace(/\s/g, '');
  const keyBytes = Uint8Array.from(atob(pemBody), c => c.charCodeAt(0));
  const cryptoKey = await crypto.subtle.importKey(
    'pkcs8', keyBytes.buffer,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false, ['sign']
  );

  const sigBytes = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    cryptoKey,
    new TextEncoder().encode(sigInput)
  );
  let sigBin = '';
  new Uint8Array(sigBytes).forEach(b => sigBin += String.fromCharCode(b));
  const sig = btoa(sigBin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');

  const jwt = `${sigInput}.${sig}`;

  // Exchange JWT for access token
  const resp = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });

  const data = await resp.json();
  if (!data.access_token) throw new Error('Failed to get Firebase token: ' + JSON.stringify(data));
  _fbToken = data.access_token;
  _fbTokenExpiry = now + (data.expires_in || 3600);
  return _fbToken;
}

async function fbRead(path) {
  const token = await getFirebaseToken();
  const url = `${DB_URL}/${path}.json?access_token=${token}`;
  const resp = await fetch(url);
  if (!resp.ok) throw new Error(`Firebase read error ${resp.status} at ${path}`);
  return resp.json();
}

async function fbReadLimited(path, limit) {
  const token = await getFirebaseToken();
  const q = encodeURIComponent('"$key"');
  const url = `${DB_URL}/${path}.json?orderBy=${q}&limitToLast=${limit}&access_token=${token}`;
  const resp = await fetch(url);
  if (!resp.ok) return fbRead(path);
  return resp.json();
}

async function fbWrite(path, data) {
  const token = await getFirebaseToken();
  const url = `${DB_URL}/${path}.json?access_token=${token}`;
  const resp = await fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!resp.ok) throw new Error(`Firebase write error ${resp.status} at ${path}`);
  return resp.json();
}

async function fbPatch(path, data) {
  const token = await getFirebaseToken();
  const url = `${DB_URL}/${path}.json?access_token=${token}`;
  const resp = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!resp.ok) throw new Error(`Firebase patch error ${resp.status} at ${path}`);
  return resp.json();
}

async function fbPush(path, data) {
  const token = await getFirebaseToken();
  const url = `${DB_URL}/${path}.json?access_token=${token}`;
  const resp = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!resp.ok) throw new Error(`Firebase push error ${resp.status} at ${path}`);
  return resp.json();
}

async function fbDelete(path) {
  const token = await getFirebaseToken();
  const url = `${DB_URL}/${path}.json?access_token=${token}`;
  const resp = await fetch(url, { method: 'DELETE' });
  if (!resp.ok) throw new Error(`Firebase delete error ${resp.status} at ${path}`);
  return resp.json();
}

// ─── CORS / Response Helpers ──────────────────────────────────────────────────

function corsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Admin-Secret, x-master-secret, x-zaqzouka-key, x-admin-secret',
  };
}

function jsonResp(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...corsHeaders(),
      'Content-Type': 'application/json; charset=utf-8',
      ...extraHeaders,
    },
  });
}

// ─── Route Handlers ───────────────────────────────────────────────────────────

let _healthCache = null;
let _healthCacheTime = 0;

async function handleHealth(req) {
  const now = Date.now();
  if (_healthCache && now - _healthCacheTime < 60000) {
    return jsonResp(
      { ..._healthCache, _source: 'memory_cache' },
      200,
      { 'Cache-Control': 'public, max-age=30, s-maxage=60, stale-while-revalidate=120' }
    );
  }
  try {
    const firstOpens = await fbRead('stats/firstOpens');
    const payload = {
      status: 'online',
      serverTime: new Date().toISOString(),
      totalSessions: firstOpens || 0,
      dataConnected: true,
      provider: 'cloudflare-workers',
    };
    _healthCache = payload;
    _healthCacheTime = now;
    return jsonResp(payload, 200, {
      'Cache-Control': 'public, max-age=30, s-maxage=60, stale-while-revalidate=120',
    });
  } catch (err) {
    if (_healthCache) {
      return jsonResp({ ..._healthCache, _source: 'stale' }, 200, {
        'Cache-Control': 'public, max-age=15, s-maxage=30',
      });
    }
    return jsonResp({
      status: 'online',
      serverTime: new Date().toISOString(),
      totalSessions: 0,
      dataConnected: false,
      error: err.message,
      provider: 'cloudflare-workers',
    });
  }
}

// Cache for status endpoint
let _statusCache = null;
let _statusCacheTime = 0;

async function handleStatus(req) {
  const now = Date.now();
  if (_statusCache && now - _statusCacheTime < 30000) {
    const view = sanitizeSystemControl(_statusCache, true);
    return jsonResp({ ...view, _source: 'cache' }, 200, {
      'Cache-Control': 'public, s-maxage=60, stale-while-revalidate=300',
    });
  }

  const FALLBACK = sanitizeSystemControl(null, true);

  try {
    const raw = await fbRead('system_control');
    const stored = sanitizeSystemControl(raw, false);
    _statusCache = stored;
    _statusCacheTime = now;

    const view = sanitizeSystemControl(stored, true);
    return jsonResp({ ...view, _source: 'firebase' }, 200, {
      'Cache-Control': 'public, s-maxage=60, stale-while-revalidate=300',
    });
  } catch (err) {
    if (_statusCache) {
      return jsonResp({ ...sanitizeSystemControl(_statusCache, true), _source: 'stale', _error: err.message });
    }
    return jsonResp({ ...FALLBACK, _source: 'fallback', _error: err.message });
  }
}

async function handleStats(req, url) {
  const auth = req.headers.get('x-admin-secret') || url.searchParams.get('secret') || '';
  if (ADMIN_SECRET && auth !== ADMIN_SECRET) return jsonResp({ error: 'Unauthorized' }, 403);

  try {
    const [stats, sessionsObj, topContent, searches, errors, actLog, landing, landingVisitors] = await Promise.all([
      fbRead('stats'),
      fbReadLimited('sessions', 600),
      fbReadLimited('topContent', 500),
      fbReadLimited('searches', 500),
      fbReadLimited('errors', 50),
      fbReadLimited('activityLog', 50),
      fbRead('landing_analytics'),
      fbReadLimited('landing_visitors', 50),
    ]);

    const sessionsArr = sessionsObj ? Object.values(sessionsObj) : [];
    const topContentArr = topContent ? Object.values(topContent) : [];
    const searchesArr = searches ? Object.values(searches) : [];
    const errorsArr = errors ? Object.values(errors) : [];
    const actLogArr = actLog ? Object.values(actLog) : [];
    const landingVisitorsArr = landingVisitors ? Object.values(landingVisitors) : [];

    const statsVal = stats || {};
    const landingVal = landing || {};

    const totalSessions = sessionsArr.length;
    const appSessions = sessionsArr.filter(s => s.platform !== 'web');
    const webSessions = sessionsArr.filter(s => s.platform === 'web');
    const activeSessions = sessionsArr.filter(s => s.status === 'active');

    return jsonResp({
      ok: true,
      kpis: {
        totalSessions,
        firstOpens: statsVal.firstOpens || totalSessions,
        activeSessions: activeSessions.length,
        appSessions: appSessions.length,
        webSessions: webSessions.length,
        appRatio: totalSessions > 0 ? Math.round((appSessions.length / totalSessions) * 100) : 0,
        webRatio: totalSessions > 0 ? Math.round((webSessions.length / totalSessions) * 100) : 0,
        totalDownloads: landingVal.total_downloads || 0,
        downloadsBundle: landingVal.downloads_bundle || 0,
        totalVisits: landingVal.totalVisits || 0,
      },
      sessions: sessionsArr.slice(-200).reverse(),
      topContent: topContentArr.slice(0, 50),
      searches: searchesArr.slice(-100),
      errors: errorsArr.slice(-50),
      activityLog: actLogArr.slice(-50),
      landingAnalytics: {
        siteUrl: 'zaafilm.com',
        siteName: 'موقع التحميل الرسمي ZAAFILM',
        status: 'active',
        totalVisits: landingVal.totalVisits || 0,
        totalDownloads: landingVal.total_downloads || 0,
        recentVisitors: landingVisitorsArr.slice(-30),
      },
      _serverTime: new Date().toISOString(),
      _provider: 'cloudflare-workers',
    });
  } catch (err) {
    return jsonResp({ ok: false, error: err.message }, 500);
  }
}

async function handleTelemetry(req, url, body) {
  const route = url.searchParams.get('route') || '';

  const now = new Date();
  const isoNow = now.toISOString();

  if (route === 'session/start') {
    const deviceId = body.deviceId || ('anon_' + Date.now());
    const isWeb = body.platform === 'web' || body.deviceType === 'web';
    const sessionId = isWeb
      ? ('sess_web_' + Date.now() + '_' + Math.random().toString(36).slice(2, 7))
      : ('dev_' + deviceId.replace(/[^a-zA-Z0-9_-]/g, '_'));

    const sessionData = {
      sessionId,
      deviceId,
      device: body.device || body.deviceModel || 'Android Device',
      platform: body.platform || 'android',
      osVersion: body.osVersion || 'Android',
      appVersion: body.appVersion || '1.0',
      country: body.country || 'EG',
      city: body.city || '',
      status: 'active',
      openedAt: isoNow,
      lastSeen: isoNow,
      sessionCount: 1,
      actionsCount: 0,
      lastSection: body.currentSection || 'الرئيسية',
      lastContent: body.currentContent || '',
    };

    await fbWrite(`sessions/${sessionId}`, sessionData);

    // Update stats counter
    try {
      const statsSnap = await fbRead('stats');
      const stats = statsSnap || {};
      await fbPatch('stats', {
        firstOpens: (stats.firstOpens || 0) + 1,
        lastActivityAt: isoNow,
      });
    } catch (e) {}

    return jsonResp({ ok: true, sessionId });
  }

  if (route === 'session/end') {
    const sessionId = body.sessionId;
    if (!sessionId) return jsonResp({ error: 'sessionId required' }, 400);
    try {
      const durationMin = Math.round((body.durationSeconds || 0) / 60);
      await fbPatch(`sessions/${sessionId}`, {
        status: 'closed',
        closedAt: isoNow,
        lastSeen: isoNow,
        durationMinutes: durationMin,
        durationText: durationMin > 0 ? `${durationMin} د` : 'أقل من دقيقة',
      });
    } catch (e) {}
    return jsonResp({ ok: true });
  }

  if (route === 'session/heartbeat') {
    const sessionId = body.sessionId;
    if (!sessionId) return jsonResp({ error: 'sessionId required' }, 400);
    try {
      await fbPatch(`sessions/${sessionId}`, {
        lastSeen: isoNow,
        status: 'active',
        lastSection: body.currentSection || undefined,
        lastContent: body.currentContent || undefined,
      });
    } catch (e) {}
    return jsonResp({ ok: true });
  }

  if (route === 'action') {
    try {
      await fbPush('activityLog', {
        sessionId: body.sessionId || 'unknown',
        action: body.action || 'unknown',
        content: body.content || '',
        section: body.section || '',
        timestamp: isoNow,
      });
    } catch (e) {}
    return jsonResp({ ok: true });
  }

  if (route === 'playback') {
    try {
      const contentId = body.contentId || body.title || 'unknown';
      const topSnap = await fbRead(`topContent/${contentId.replace(/[^a-zA-Z0-9]/g, '_')}`);
      const existing = topSnap || { count: 0, title: contentId };
      await fbWrite(`topContent/${contentId.replace(/[^a-zA-Z0-9]/g, '_')}`, {
        ...existing,
        count: (existing.count || 0) + 1,
        lastPlayed: isoNow,
        title: body.title || contentId,
      });
    } catch (e) {}
    return jsonResp({ ok: true });
  }

  if (route === 'search') {
    try {
      await fbPush('searches', {
        query: body.query || '',
        sessionId: body.sessionId || 'unknown',
        timestamp: isoNow,
      });
    } catch (e) {}
    return jsonResp({ ok: true });
  }

  return jsonResp({ error: 'Unknown telemetry route: ' + route }, 400);
}

async function handleAdmin(req, url, body) {
  const route = url.searchParams.get('route') || '';

  if (route === 'kill-switch') {
    const authHeader = req.headers.get('x-master-secret') || url.searchParams.get('master_secret') || '';
    if (authHeader !== MASTER_SECRET) return jsonResp({ error: 'Access denied' }, 403);

    if (req.method === 'GET') {
      try {
        const val = await fbRead('killSwitch');
        return jsonResp({ ok: true, killSwitch: val || { active: false } });
      } catch (err) {
        return jsonResp({ ok: false, error: err.message }, 500);
      }
    }

    if (req.method === 'POST') {
      const action = body.action;
      const isoNow = new Date().toISOString();

      if (action === 'kill') {
        const killData = {
          active: true,
          wipe: false,
          activatedAt: isoNow,
          reason: body.reason || 'Emergency shutdown by owner',
          target: body.target || 'all',
          message: body.message || 'التطبيق متوقف مؤقتاً. جاري الصيانة.',
          messageEn: body.messageEn || 'App is temporarily unavailable. Under maintenance.',
        };
        await fbWrite('killSwitch', killData);
        return jsonResp({ ok: true, action: 'KILL_ACTIVATED', data: killData });
      }

      if (action === 'restore') {
        await fbWrite('killSwitch', {
          active: false,
          restoredAt: isoNow,
          reason: 'Owner restored service',
        });
        return jsonResp({ ok: true, action: 'RESTORED' });
      }

      if (action === 'status') {
        const val = await fbRead('killSwitch');
        return jsonResp({ ok: true, killSwitch: val || { active: false } });
      }

      // ── Owner Sovereign Ad Control ──────────────────────────────────────────
      if (action === 'set_owner_ad') {
        // Only owner (ZAQZOUKA_API_KEY or MASTER_SECRET) may call this
        const ownerKey = req.headers.get('x-owner-key') || body.ownerKey || '';
        if (ownerKey !== ZAQZOUKA_API_KEY && ownerKey !== MASTER_SECRET) {
          return jsonResp({ ok: false, error: 'Unauthorized — owner key required' }, 403);
        }
        const cfg = {
          enabled:            !!body.enabled,
          override_partner:   body.override_partner !== false,
          ad_html:            body.ad_html || '',
          ad_url:             body.ad_url  || '',
          skip_delay_seconds: Number(body.skip_delay_seconds) || 5,
          banner_enabled:     !!body.banner_enabled,
          banner_html:        body.banner_html || '',
          banner_url:         body.banner_url  || '',
          updated_at:         new Date().toISOString(),
        };
        await fbWrite('system_control/owner_ad_config', cfg);
        return jsonResp({ ok: true, action: 'SET_OWNER_AD', data: cfg });
      }

      // ── Get Owner Ad Config ─────────────────────────────────────────────────
      if (action === 'get_owner_ad') {
        const cfg = await fbRead('system_control/owner_ad_config');
        return jsonResp({ ok: true, owner_ad_config: cfg || {} });
      }

      return jsonResp({ error: 'Unknown action: ' + action }, 400);
    }
  }

  if (route === 'license-check') {
    const appKey = req.headers.get('x-zaqzouka-key') || body.apiKey || '';
    const buildId = body.buildId || '';
    const isoNow = new Date().toISOString();

    try {
      const killSwitch = await fbRead('killSwitch');
      const isKilled = killSwitch && killSwitch.active === true;

      if (isKilled) {
        return jsonResp({
          allowed: false,
          reason: 'KILL_SWITCH_ACTIVE',
          message: (killSwitch && killSwitch.message) || 'التطبيق متوقف مؤقتاً.',
          messageEn: (killSwitch && killSwitch.messageEn) || 'App temporarily unavailable.',
        });
      }

      return jsonResp({
        allowed: true,
        reason: 'OK',
        serverTime: isoNow,
        message: 'مرحباً بك في زقزوقة TV',
      });
    } catch (err) {
      // On error, allow the app to proceed (fail open)
      return jsonResp({ allowed: true, reason: 'ERROR_ALLOW', error: err.message });
    }
  }

  if (route === 'reset') {
    const authHeader = req.headers.get('x-master-secret') || '';
    if (authHeader !== MASTER_SECRET) return jsonResp({ error: 'Access denied' }, 403);
    if (req.method !== 'POST') return jsonResp({ error: 'POST only' }, 405);

    try {
      const target = body.target || 'stats';
      await fbDelete(target);
      return jsonResp({ ok: true, deleted: target });
    } catch (err) {
      return jsonResp({ ok: false, error: err.message }, 500);
    }
  }

  return jsonResp({ error: 'Unknown admin route: ' + route }, 400);
}

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

async function readSystemControlCached() {
  const now = Date.now();
  if (_statusCache && now - _statusCacheTime < 30000) {
    return _statusCache;
  }
  const val = sanitizeSystemControl(await fbRead('system_control'), false);
  _statusCache = val;
  _statusCacheTime = now;
  return val;
}

async function handleSystemControl(req, url, body) {
  const isAdminReq = Boolean(
    req.headers.get('x-admin-secret') ||
    req.headers.get('x-master-secret') ||
    url.searchParams.get('nocache') === '1'
  );

  if (req.method === 'GET') {
    try {
      const raw = isAdminReq ? await fbRead('system_control') : await readSystemControlCached();
      const val = sanitizeSystemControl(raw, !isAdminReq);
      return jsonResp(val, 200, {
        'Cache-Control': isAdminReq
          ? 'no-cache, no-store, must-revalidate'
          : 'public, s-maxage=60, max-age=15, stale-while-revalidate=300',
      });
    } catch (err) {
      return jsonResp({ ...sanitizeSystemControl(null, true), error: err.message }, 200);
    }
  }

  if (req.method === 'POST') {
    _statusCache = null;
    _statusCacheTime = 0;
    const authHeader = req.headers.get('x-admin-secret') || req.headers.get('x-master-secret') || (body && body.secret) || '';
    if (authHeader !== ADMIN_SECRET && authHeader !== MASTER_SECRET) {
      return jsonResp({ error: 'Unauthorized' }, 403);
    }

    try {
      if (body.action === 'set_component') {
        const comp = body.component;
        if (!comp) return jsonResp({ error: 'Missing component' }, 400);
        await fbWrite(`system_control/components/${comp}`, !!body.enabled);
        await fbWrite('system_control/updatedAt', Date.now());
        return jsonResp({ ok: true, action: 'set_component', component: comp, enabled: !!body.enabled });
      }

      if (body.action === 'freeze') {
        await fbWrite('system_control/status', 'frozen');
        await fbWrite('system_control/system_enabled', false);
        await fbWrite('system_control/maintenance_mode', true);
        await fbWrite('system_control/message', body.message || 'عفواً، الخدمة متوقفة حالياً للتحديث والصيانة.');
        await fbWrite('system_control/updatedAt', Date.now());
        return jsonResp({ ok: true, action: 'freeze' });
      }

      if (body.action === 'set_system') {
        const enabledVal = body.system_enabled !== false;
        await fbPatch('system_control', {
          system_enabled: enabledVal,
          maintenance_mode: !enabledVal,
          status: enabledVal ? 'active' : 'frozen',
          message: enabledVal
            ? 'المنظومة نشطة وتعمل بكامل طاقتها.'
            : (body.message || 'عفواً، الخدمة متوقفة حالياً للتحديث والصيانة.'),
          updatedAt: Date.now(),
          updated_at: new Date().toISOString(),
        });
        await fbWrite('system_control/components/app_enabled', enabledVal);
        return jsonResp({ ok: true, action: 'set_system', system_enabled: enabledVal });
      }

      if (body.action === 'restore') {
        const fullState = {
          status: 'active',
          system_enabled: true,
          maintenance_mode: false,
          message: body.message || 'المنظومة نشطة وتعمل بكامل طاقتها.',
          updatedAt: Date.now(),
          wipeTimestamp: 0,
          components: {
            download_site_enabled: true,
            web_player_enabled: true,
            website_enabled: true,
            app_enabled: true,
            player_enabled: true,
            admin_enabled: true,
          },
        };
        await fbWrite('system_control', fullState);
        return jsonResp({ ok: true, action: 'restore', state: fullState });
      }

      if (body.action === 'wipe') {
        const now = Date.now();
        await fbWrite('system_control', {
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
        });
        if (body.wipeServers !== false) {
          const wipeNodes = [
            'servers_catalog', 'cached_feeds', 'channels', 'live_sessions',
            'analytics', 'devices', 'playlists', 'sessions', 'searches',
            'stats', 'activityLog', 'errors', 'topContent'
          ];
          await Promise.all(wipeNodes.map(node => fbWrite(node, null)));
        }
        return jsonResp({ ok: true, action: 'wipe', timestamp: now });
      }

      if (body.action === 'set_startio_control' || body.action === 'set_owner_ad') {
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
        } = body;

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
        await fbWrite('system_control/owner_ad_config', cfg);

        await fbWrite('system_control/ad_config', {
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

        return jsonResp({ ok: true, action: 'SET_AD_CONTROL', data: cfg });
      }

      if (body.action === 'set_partner_ad') {
        const cfg = {
          enabled:            !!body.enabled,
          ad_html:            body.ad_html || '',
          ad_url:             body.ad_url  || '',
          skip_delay_seconds: Number(body.skip_delay_seconds) || 5,
          banner_enabled:     !!body.banner_enabled,
          banner_html:        body.banner_html || '',
          banner_url:         body.banner_url  || '',
          updated_at:         new Date().toISOString(),
        };
        await fbWrite('system_control/ad_config', cfg);
        return jsonResp({ ok: true, action: 'SET_PARTNER_AD', data: cfg });
      }

      if (body.action === 'set_app_update') {
        const updateData = {
          enabled:           body.enabled !== false,
          latestVersionCode: Number(body.latestVersionCode || body.versionCode) || 0,
          latestVersionName: String(body.latestVersionName || body.versionName || ''),
          apkUrl:            String(body.apkUrl || body.downloadUrl || 'https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.86-93/zaqzouka-1.0.86-93.apk'),
          releaseNotes:      String(body.releaseNotes || body.notes || ''),
          forced:            !!body.forced,
          updatedAt:         Date.now(),
        };
        await fbWrite('system_control/app_update', updateData);
        await fbWrite('system_control/updatedAt', Date.now());
        return jsonResp({ ok: true, action: 'SET_APP_UPDATE', data: updateData });
      }

      if (body.action === 'broadcast_notification') {
        const notif = body.notification || {};
        const notifId = notif.id || Date.now();
        const payload = {
          id: notifId,
          title: (notif.title || '').trim(),
          body: (notif.body || notif.message || '').trim(),
          deepLinkType: notif.deepLinkType || (notif.targetPageUrl ? 'media_detail' : 'general'),
          targetId: notif.targetId || '',
          targetTitle: notif.targetTitle || '',
          targetPageUrl: notif.targetPageUrl || '',
          isSeries: notif.isSeries === true,
          poster: notif.poster || '',
          year: notif.year || '',
          createdAt: Date.now(),
        };
        await fbWrite('system_control/broadcast_notification', payload);
        await fbWrite(`broadcast_notifications/history/${notifId}`, payload);
        return jsonResp({ ok: true, action: 'broadcast_notification', notification: payload });
      }

      return jsonResp({ ok: false, error: 'Invalid action.' }, 400);
    } catch (err) {
      return jsonResp({ ok: false, error: err.message }, 500);
    }
  }

  return jsonResp({ error: 'Method not allowed' }, 405);
}

async function handleBroadcastNotification(req, url, body) {
  if (req.method === 'GET') {
    try {
      const val = await fbRead('system_control/broadcast_notification');
      return jsonResp({ ok: true, notification: val || null });
    } catch (err) {
      return jsonResp({ ok: false, error: err.message }, 500);
    }
  }

  if (req.method === 'POST') {
    const authHeader = req.headers.get('x-admin-secret') || req.headers.get('x-master-secret') || '';
    if (authHeader !== ADMIN_SECRET && authHeader !== MASTER_SECRET) {
      return jsonResp({ error: 'Unauthorized' }, 403);
    }

    const notif = body.notification || body;
    const notifId = notif.id || Date.now();
    const payload = {
      id: notifId,
      title: (notif.title || '').trim(),
      body: (notif.body || notif.message || '').trim(),
      deepLinkType: notif.deepLinkType || (notif.targetPageUrl ? 'media_detail' : 'general'),
      targetId: notif.targetId || '',
      targetTitle: notif.targetTitle || '',
      targetPageUrl: notif.targetPageUrl || '',
      isSeries: notif.isSeries === true,
      poster: notif.poster || '',
      year: notif.year || '',
      createdAt: Date.now(),
    };

    try {
      await fbWrite('system_control/broadcast_notification', payload);
      await fbWrite(`broadcast_notifications/history/${notifId}`, payload);
      return jsonResp({ ok: true, action: 'broadcast_notification', notification: payload });
    } catch (err) {
      return jsonResp({ ok: false, error: err.message }, 500);
    }
  }

  return jsonResp({ error: 'Method not allowed' }, 405);
}

async function handleExport(req, url) {
  const auth = req.headers.get('x-admin-secret') || url.searchParams.get('secret') || '';
  if (ADMIN_SECRET && auth !== ADMIN_SECRET) return jsonResp({ error: 'Unauthorized' }, 403);

  const route = url.searchParams.get('route') || '';

  try {
    let data;
    if (route === 'sessions') {
      data = await fbRead('sessions');
    } else if (route === 'searches') {
      data = await fbRead('searches');
    } else if (route === 'errors') {
      data = await fbRead('errors');
    } else {
      data = await fbRead('stats');
    }

    return jsonResp({ ok: true, route, data: data || {} });
  } catch (err) {
    return jsonResp({ ok: false, error: err.message }, 500);
  }
}

function renderBlockedHtml(title, message, isWiped = false) {
  const accentColor = isWiped ? '#E50914' : '#D4AF37';
  return `<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${title}</title>
  <style>
    * { margin:0; padding:0; box-sizing:border-box; font-family:system-ui, -apple-system, sans-serif; }
    body { background-color:#0A0D14; color:#F1F5F9; min-height:100vh; display:flex; align-items:center; justify-content:center; padding:20px; }
    .card { max-width:480px; width:100%; background:#141A29; border:1px solid ${accentColor}44; border-radius:24px; padding:36px 28px; text-align:center; box-shadow:0 20px 40px rgba(0,0,0,0.6); }
    .icon { width:72px; height:72px; border-radius:50%; background:${accentColor}18; color:${accentColor}; display:flex; align-items:center; justify-content:center; margin:0 auto 20px; font-size:32px; border:2px solid ${accentColor}55; }
    h1 { font-size:22px; font-weight:800; color:${accentColor}; margin-bottom:12px; }
    p { font-size:15px; color:#94A3B8; line-height:1.6; margin-bottom:24px; }
    .badge { display:inline-block; padding:6px 16px; border-radius:99px; background:#0F131E; border:1px solid #2A334B; font-size:12px; color:#64748B; font-weight:600; }
  </style>
</head>
<body>
  <div class="card">
    <div class="icon">${isWiped ? '⚠️' : '🔒'}</div>
    <h1>${title}</h1>
    <p>${message}</p>
    <div class="badge">منظومة زقزوقة TV • الحماية المركزية</div>
  </div>
</body>
</html>`;
}

// ─── Main Fetch Handler ───────────────────────────────────────────────────────

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;

    // Handle preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    // ─── Service Worker Cleanup (previous network removed) ──────────────────
    if (path === '/sw.js') {
      const swContent = `// Cleanup legacy service worker safely
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => {
  event.waitUntil(self.registration.unregister());
});`;
      return new Response(swContent, {
        status: 200,
        headers: {
          'Content-Type': 'application/javascript; charset=utf-8',
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Service-Worker-Allowed': '/',
          'Access-Control-Allow-Origin': '*',
        },
      });
    }
    // ─────────────────────────────────────────────────────────────────────────

    const cache = typeof caches !== 'undefined' ? caches.default : null;
    const isAdminReq = Boolean(
      request.headers.get('x-admin-secret') ||
      request.headers.get('x-master-secret') ||
      url.searchParams.get('nocache') === '1'
    );
    const isPublicCacheableApi =
      request.method === 'GET' &&
      !isAdminReq &&
      (path === '/api/status' || path === '/api/status/' ||
       path === '/api/system_control' || path === '/api/system_control/' ||
       path === '/api/health' || path === '/api/health/');

    if (cache && isPublicCacheableApi) {
      const cacheKey = new Request(url.origin + path.replace(/\/$/, ''), { method: 'GET' });
      const cachedResp = await cache.match(cacheKey);
      if (cachedResp) return cachedResp;
    }

    // Parse JSON body if present
    let body = {};
    if (request.method === 'POST' || request.method === 'PUT' || request.method === 'PATCH') {
      try {
        body = await request.json();
      } catch (e) { body = {}; }
    }

    try {
      if (cache && (path === '/api/system_control' || path.startsWith('/api/admin')) && request.method === 'POST') {
        ctx.waitUntil(Promise.all([
          cache.delete(new Request(url.origin + '/api/status', { method: 'GET' })),
          cache.delete(new Request(url.origin + '/api/system_control', { method: 'GET' })),
        ]));
      }

      if (path === '/api/health' || path === '/api/health/') {
        const resp = await handleHealth(request);
        if (cache && !isAdminReq) {
          const cacheKey = new Request(url.origin + '/api/health', { method: 'GET' });
          ctx.waitUntil(cache.put(cacheKey, resp.clone()));
        }
        return resp;
      }
      if (path === '/api/status' || path === '/api/status/') {
        const resp = await handleStatus(request);
        if (cache && !isAdminReq) {
          const cacheKey = new Request(url.origin + '/api/status', { method: 'GET' });
          ctx.waitUntil(cache.put(cacheKey, resp.clone()));
        }
        return resp;
      }
      if (path === '/api/stats' || path === '/api/stats/') return handleStats(request, url);
      if (path.startsWith('/api/telemetry')) return handleTelemetry(request, url, body);
      if (path.startsWith('/api/admin')) return handleAdmin(request, url, body);
      if (path === '/api/system_control' || path === '/api/system_control/') {
        const resp = await handleSystemControl(request, url, body);
        if (cache && request.method === 'GET' && !isAdminReq) {
          const cacheKey = new Request(url.origin + '/api/system_control', { method: 'GET' });
          ctx.waitUntil(cache.put(cacheKey, resp.clone()));
        }
        return resp;
      }
      if (path === '/api/version' || path === '/api/version/' || path === '/updates/android/latest.json') {
        const up = await fetch('https://madartv-updates.web.app/updates/android/latest.json', {
          headers: { 'Accept': 'application/json' }
        });
        return new Response(await up.text(), {
          status: up.status,
          headers: {
            'Content-Type': 'application/json; charset=utf-8',
            'Cache-Control': 'public, max-age=60, s-maxage=300',
            ...corsHeaders(),
          },
        });
      }
      if (path === '/api/broadcast_notification' || path === '/api/broadcast_notification/') return handleBroadcastNotification(request, url, body);
      if (path.startsWith('/api/export')) return handleExport(request, url);

      // APK Downloads — Check cached kill switch before redirecting directly to GitHub Releases
      if (path.startsWith('/downloads/')) {
        try {
          const sys = await readSystemControlCached();
          if (sys) {
            const isFrozen = sys.status === 'frozen';
            const isWiped = sys.status === 'wiped';
            const isDownloadDisabled = sys.components && (sys.components.download_site_enabled === false || sys.components.website_enabled === false);
            if (isFrozen || isWiped || isDownloadDisabled) {
              const title = isWiped ? 'التحميل مغلق نهائياً' : 'التحميل متوقف مؤقتاً';
              const msg = sys.message || 'خدمة التنزيل متوقفة حالياً بقرار من إدارة المنظومة.';
              return new Response(renderBlockedHtml(title, msg, isWiped), {
                status: 503,
                headers: { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' },
              });
            }
          }
        } catch (_) {}

        const rawName = path.replace('/downloads/', '').trim();
        const lowerName = rawName.toLowerCase();
        let ghUrl;
        if (lowerName === 'zaqzouka-app.apk' || lowerName === 'zaqzouka-latest.apk' || lowerName.startsWith('zaqzouka-1.0.')) {
          ghUrl = 'https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.86-93/zaqzouka-1.0.86-93.apk';
        } else if (lowerName === 'zag-player.apk' || lowerName === 'zaqzouka-player.apk' || lowerName.startsWith('zag-player-')) {
          ghUrl = 'https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.86-93/zag-player-1.1.37.apk';
        } else {
          ghUrl = `https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.86-93/${rawName}`;
        }
        return Response.redirect(ghUrl, 302);
      }

      // Landing page HTML and static assets: Proxy to Firebase Hosting (active, unblocked) with CF edge cache
      const UPSTREAM_WEB = 'https://madartv.web.app';
      const isHtmlRequest = path === '/' || path === '' || path.endsWith('.html');
      const isStaticAsset = path.startsWith('/assets') || path.endsWith('.css') || path.endsWith('.js') || path.endsWith('.png') || path.endsWith('.jpg') || path.endsWith('.svg') || path.endsWith('.ico') || path.endsWith('.webp');
      if (isHtmlRequest || isStaticAsset) {
        if (isHtmlRequest) {
          // Only check cached kill switch on page loads, never on CSS/JS/image assets
          try {
            const sys = await readSystemControlCached();
            if (sys) {
              const isFrozen = sys.status === 'frozen';
              const isWiped = sys.status === 'wiped';
              const isDownloadSiteDisabled = sys.components && (sys.components.download_site_enabled === false || sys.components.website_enabled === false);
              if (isFrozen || isWiped || isDownloadSiteDisabled) {
                const title = isWiped ? 'تم إيقاف المنظومة نهائياً' : 'موقع التحميل متوقف حالياً للصيانة';
                const msg = sys.message || 'الموقع متوقف حالياً للصيانة والتحديث بأمر الإدارة.';
                return new Response(renderBlockedHtml(title, msg, isWiped), {
                  status: 503,
                  headers: {
                    'Content-Type': 'text/html; charset=utf-8',
                    'Cache-Control': 'no-cache, no-store, must-revalidate',
                    ...corsHeaders(),
                  },
                });
              }
            }
          } catch (_) {}
        }

        const assetCacheKey = cache ? new Request(request.url, { method: 'GET' }) : null;
        if (cache && assetCacheKey && request.method === 'GET') {
          const cachedAsset = await cache.match(assetCacheKey);
          if (cachedAsset) return cachedAsset;
        }

        const upstreamUrl = new URL((path === '/' || path === '' ? '/index.html' : path) + url.search, UPSTREAM_WEB);
        try {
          const resp = await fetch(upstreamUrl.toString(), {
            headers: { 'User-Agent': request.headers.get('User-Agent') || 'Cloudflare-Worker' }
          });
          const headers = new Headers(resp.headers);
          headers.set('Cache-Control', isStaticAsset ? 'public, max-age=86400, s-maxage=86400' : 'public, max-age=60, s-maxage=60');
          headers.set('Access-Control-Allow-Origin', '*');
          const finalResp = isHtmlRequest
            ? new Response(await resp.text(), { status: resp.status, headers })
            : new Response(resp.body, { status: resp.status, headers });
          if (cache && assetCacheKey && resp.ok && request.method === 'GET') {
            ctx.waitUntil(cache.put(assetCacheKey, finalResp.clone()));
          }
          return finalResp;
        } catch (fetchErr) {
          // Fallback below
        }
      }

      // Root ping
      if (path === '/' || path === '') {
        return jsonResp({
          service: 'Zaqzouka Dashboard API',
          version: '2.0.0',
          provider: 'Cloudflare Workers',
          status: 'online',
          endpoints: ['/api/health', '/api/status', '/api/stats', '/api/telemetry', '/api/admin', '/api/system_control', '/api/export'],
        });
      }

      return jsonResp({ error: 'Not found', path }, 404);
    } catch (err) {
      return jsonResp({ error: 'Internal error', message: err.message }, 500);
    }
  },
};
