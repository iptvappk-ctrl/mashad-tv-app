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
    versionCode: 92,
    versionName: "1.0.85",
    releaseNotes: "زقزوقة TV v1.0.85: تصحيح جلب حلقات AraDrama، تصحيح البحث للمسلسلات، تحديث إجباري.",
    apkUrl: "https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.85-92/zaqzouka-1.0.85-92.apk",
    sha256: "264938cce97f21f38cababf6ea64b214f0fcad135e3d7f51f796c7710770af83",
    player: {
      versionCode: 37,
      versionName: "1.1.37",
      releaseNotes: "ZAG Player 1.1.37: إصلاح منطق الإعلانات والعداد التنازلي.",
      apkUrl: "https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.85-92/zag-player-1.1.37.apk",
      sha256: "d616cc5bbbcbe4716d1caf42b60e74bbbc4cc75962af09c66e3c225345b3cd10"
    }
  };

  return res.status(200).json(updateData);
}
