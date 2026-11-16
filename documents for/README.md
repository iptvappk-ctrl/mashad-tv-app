# 🌕 حقيبة التوثيق الشاملة لمنظومة زقزوقة TV - الإصدار 14 Moon
**تاريخ الإصدار والاعتماد:** 16 نوفمبر 2026  
**اسم النسخة البرمجية:** `14 moon` (Release Tag: `14-moon`)  

---

## 🌟 مرحباً بك في التوثيق الكامل لمنظومة Zaqzouka TV
تحتوي هذه الحقيبة التوثيقية (`documents for/`) على التوثيق الهندسي والمعماري الدقيق والمفصل لكل جزء في المنظومة، موثقة سطراً بسطر وحركة بحركة من الألف إلى الياء، لتكون الدليل المرجعي الشامل لتطوير وصيانة وتشغيل المنظومة تجارياً وتقنياً.

---

## 📚 فهرس وثائق المنظومة

| الوثيقة | الوصف والنطاق المغطى | الرابط المباشر |
|---|---|---|
| **01. تطبيق الهاتف والشاشات** | توثيق تطبيق `MadarFlutter`، شاشات الأفلام والمسلسلات والمباريات، خوارزمية التطبيع الصارم، ودعم أجهزة التي في بوكس والريموت. | [01_ZAQZOUKA_TV_MOBILE_APP.md](file:///c:/Users/hassa/Desktop/all%20project/iptv%20app/documents%20for/01_ZAQZOUKA_TV_MOBILE_APP.md) |
| **02. بوابة الموقع الإلكتروني** | توثيق موقع الويب `web` المبني بـ Next.js 16، محركات السحب المباشر، مشغل الويب، وتكامل التتبع اللحظي. | [02_ZAQZOUKA_WEB_PORTAL.md](file:///c:/Users/hassa/Desktop/all%20project/iptv%20app/documents%20for/02_ZAQZOUKA_WEB_PORTAL.md) |
| **03. محركات الفيديو والتبديل** | توثيق مشغل الفيديو الأصلي (ExoPlayer + HLS.js)، فك تشفير سيرفرات Voe و Vidmoly، والتبديل التلقائي عند التعطل، وإعلانات VAST. | [03_ZAQZOUKA_VIDEO_PLAYER_ENGINE.md](file:///c:/Users/hassa/Desktop/all%20project/iptv%20app/documents%20for/03_ZAQZOUKA_VIDEO_PLAYER_ENGINE.md) |
| **04. لوحة الأدمن وسوق الداتا** | توثيق لوحة تحليلات زقزوقة (8 تبويبات)، التحديد الجغرافي الدقيق للمدن، محرك تسييل البيانات، وسماسرة الـ DMP. | [04_ZAQZOUKA_ADMIN_AND_TELEMETRY_BACKEND.md](file:///c:/Users/hassa/Desktop/all%20project/iptv%20app/documents%20for/04_ZAQZOUKA_ADMIN_AND_TELEMETRY_BACKEND.md) |
| **05. المخطط الشامل التكاملي** | المخطط الهندسي التكاملي (End-to-End Blueprint)، مخططات Mermaid، فهرس كافة ملفات المشروع، وأدلة البناء والنشر. | [05_FULL_SYSTEM_END_TO_END_BLUEPRINT.md](file:///c:/Users/hassa/Desktop/all%20project/iptv%20app/documents%20for/05_FULL_SYSTEM_END_TO_END_BLUEPRINT.md) |

---

## 🎯 المكتسبات التقنية في إصدار 14 Moon
1. **الواقعية الكاملة (Pure Live Data 100%):** تنظيف المنظومة كلياً من أي بيانات وهمية أو صور ثابتة Dummy Data؛ كل فيلم ومباراة يتم سحبها وفحصها لحظياً.
2. **منع اختلاط المحتوى (Zero Mixed Content):** تطبيق نظام مطابقة الكلمات والرموز الصارم `Strict Bilingual Token Matcher` لمنع ظهور أي أعمال غير مطابقة.
3. **التتبع الجغرافي للمدن والمحافظات (Exact Geolocation):** تسجيل المدينة باللغة العربية (مثل `القاهرة، C، مصر (EG)` أو `طنطا، الغربية، مصر (EG)`) ومزود الإنترنت ISP لكل جلسة.
4. **سوق البيانات والإعلانات المعتمد (VAST/VMAP & DMP Hub):** جاهزية كاملة لتسييل المشاهدات عبر شبكات إعلانات الفيديو وسماسرة الجمهور وفق أعلى المعايير القانونية (قانون حماية البيانات المصري 151 و GDPR).
