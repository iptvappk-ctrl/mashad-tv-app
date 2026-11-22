// Vercel Serverless Function: /api/version
export default function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Cache-Control', 's-maxage=3600, stale-while-revalidate=86400');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const host = req.headers.host ? `https://${req.headers.host}` : 'https://zaqzouka-updates.vercel.app';

  const updateData = {
    packageName: "com.madar.madar_flutter",
    versionCode: 76,
    versionName: "1.0.71",
    releaseNotes: "تحديث جديد لتطبيق زقزوقة: إصلاحات شاملة لملخصات المباريات، إلغاء النوافذ الخارجية، دعم كامل لمشغل ZAG Player ومزامنة أسرع للقنوات والأفلام.",
    apkUrl: `${host}/downloads/zaqzouka-app.apk`,
    sha256: "772194c0cde15d35ee8d1775a57bdcc05a4dc4fde7e874123cbaa4d15ad16ece",
    player: {
      versionCode: 28,
      versionName: "1.1.28",
      releaseNotes: "مشغل ZAG Player المحدث: ملء كامل الشاشة وإلغاء الحواف السوداء، تشغيل فوري بدون كتم الصوت، ودعم متقدم للترجمة.",
      apkUrl: `${host}/downloads/zag-player.apk`,
      sha256: "58d6162436149ad41ae69108ec1ce4a5c8a678d821c66cc28ffab3ab71b4cb27"
    }
  };

  return res.status(200).json(updateData);
}
