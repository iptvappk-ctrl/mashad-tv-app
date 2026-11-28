// Vercel Serverless Function: /api/version
export default function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Cache-Control', 's-maxage=3600, stale-while-revalidate=86400');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const host = req.headers.host ? `https://${req.headers.host}` : 'https://zaqzouka-vercel-hub-sigma.vercel.app';

  const updateData = {
    packageName: "com.madar.madar_flutter",
    versionCode: 93,
    versionName: "1.0.86",
    releaseNotes: "زقزوقة TV v1.0.86: إصلاح شامل لجلب ومطابقة مواسم وحلقات كل المسلسلات عبر جميع المصادر.",
    apkUrl: "https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.86-93/zaqzouka-1.0.86-93.apk",
    sha256: "ffa32663bc5df90a591bc3b3617b97464f2c39f66ff4411e3659c9ef8078a4c3",
    player: {
      packageName: "com.madar.player",
      versionCode: 37,
      versionName: "1.1.37",
      releaseNotes: "ZAG Player 1.1.37: إصلاح منطق الإعلانات والعداد التنازلي.",
      apkUrl: "https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.86-93/zag-player-1.1.37.apk",
      sha256: "920b398e511702d2376b1c880fe2eb764295262566902fed4072d9f4f3a4949c"
    }
  };

  return res.status(200).json(updateData);
}
