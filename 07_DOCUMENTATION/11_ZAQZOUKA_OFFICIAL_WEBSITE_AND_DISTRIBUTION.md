# وثيقة التوثيق الرسمية: بوابة وموقع زقزوقة TV للتوزيع والتحميل السحابي
## Zaqzouka TV & ZAG Player Official Web Portal Architecture & Distribution System

> **المعرف المرجعي**: `DOC-WEB-ZAQZOUKA-011`  
> **تاريخ التوثيق**: 21 نوفمبر 2026  
> **البيئة المستهدفة**: شبكة Vercel Edge Serverless Global Infrastructure  
> **الرابط الإنتاجي الفعال**: [https://zaqzouka-tv.vercel.app](https://zaqzouka-tv.vercel.app)

---

### 1. نظرة عامة والهدف الاستراتيجي (Strategic Overview)
تم تصميم وتطوير موقع الويب الرسمي لمنظومة **زقزوقة TV** ليكون مركز التوزيع المباشر والآمن لتطبيقات المنظومة:
1. **تطبيق زقزوقة TV الأساسي (Zaqzouka TV Media Hub v1.0.74)**: مسؤول عن تصفح مكتبة الأفلام والمسلسلات، القنوات الحية، البحث الذكي، وإدارة المفضلة.
2. **مشغل زاغ المخصص (ZAG Player Native Engine v1.1.28)**: المحرك المسؤول عن فك تشفير وتدفق الفيديو عالي الأداء مع تقنيات منع التقطيع ودعم التلفزيونات الذكية.

---

### 2. المواقع والمسارات المعتمدة في المشروع

| المسار | الوظيفة |
| :--- | :--- |
| `ZAQZOUKA_OFFICIAL_WEBSITE/` | المجلد الرئيسي الشامل لكود الموقع، ملفات الـ Assets والـ APKs الجاهزة للتحميل |
| `06_WEB_AND_DISTRIBUTION/download-landing-page/` | النسخة المتزامنة داخل قسم توزيع الويب في المنظومة |
| `c:\Users\hassa\Desktop\zaqzouka-landing-page\` | نسخة سطح المكتب المفتوحة للعمل السريع |
| `c:\Users\hassa\Desktop\zaqzouka-landing-page.zip` | الأرشيف المضغوط الجاهز للرفع الفوري على أي سيرفر أو استضافة خارجية (cPanel / Hostinger) |

---

### 3. بنية المنظومة السحابية ونقاط النهاية الحية (Live Cloud Endpoints)

| الخدمة | الرابط الحي | الحالة |
| :--- | :--- | :--- |
| **الموقع الرسمي الرئيسي** | `https://zaqzouka-tv.vercel.app` | **200 OK — متاح عالمياً** |
| **تحميل تطبيق زقزوقة TV** | `https://zaqzouka-tv.vercel.app/downloads/zaqzouka-app.apk` | **200 OK — حجم 53.4 MB** |
| **تحميل مشغل زاغ** | `https://zaqzouka-tv.vercel.app/downloads/zag-player.apk` | **200 OK — حجم 4.6 MB** |
| **لوحة تحكم الأدمن السحابية** | `https://zaqzouka-dashboard-api.vercel.app` | **200 OK — مشفرة بالمفتاح الجديد** |
| **سيرفر التحديثات وتوزيع الروابط** | `https://zaqzouka-vercel-hub.vercel.app` | **200 OK — متصل بالمنظومة** |

---

### 4. الهوية البصرية ونظام التصميم (Bespoke Cinema Design System)

* **الألوان الرسمية**:
  - `Terracotta Brick (#B94E3C)`: لون العلامة التجارية المعتمد للأزرار الرئيسية والتأكيدات.
  - `Cinema Gold (#E5A958)`: لون التمييز والنجوم وتفاصيل الثقة.
  - `Deep Obsidian (#0E0B0E)`: لون الخلفية المسائية المريح للعين.
  - `Paper Ivory (#FFFBF5)`: لون القراءة النقي للنصوص والعناوين.
* **الخطوط**: خط `Cairo` العربي الأنيق وخط `Outfit` اللاتيني المودرن.
* **الموكاب والعرض الحي**:
  - عرض واجهة حقيقية من داخل التطبيق (`screen-1.png`) بدقة 1080×2400 بيكسل تبرز الأفلام وقوائم المشاهدة الأصلية.
  - إطار Dynamic Island مع انحناءات زجاجية ناعمة وظلال تفاعلية.
  - إلغاء كافة الأزرار والتبويبات غير الضرورية لضمان تجربة مستخدم نقية وسريعة جداً.
* **خلو كامل من الإيموجي**: استخدام أيقونات SVG دقيقة وعصرية تتناسب مع الطابع البرمجي الاحترافي.

---

### 5. خطوات التحديث وإعادة النشر (Deployment Playbook)

عند الرغبة في تحديث ملفات التطبيقات أو نصوص الموقع:

```powershell
# 1. الدخول لمجلد الموقع
cd "c:\Users\hassa\Desktop\all project\iptv app\ZAQZOUKA_OFFICIAL_WEBSITE"

# 2. فحص الحالة وتجربة التعديلات
# (يمكن استبدال zaqzouka-app.apk أو zag-player.apk في مجلد downloads)

# 3. النشر الفوري والمباشر على الإنتاج
npx vercel --prod --yes
```

---

### 6. ربط الدومين الشخصي (Domain Mapping)

عند شراء دومين للموقع، يتم ضبط سجلات DNS في لوحة تحكم الدومين كالتالي:

* **A Record**:
  - Host / Name: `@`
  - Value / Target: `76.76.21.21`
  - TTL: Automatic / 3600
* **CNAME Record**:
  - Host / Name: `www`
  - Value / Target: `cname.vercel-dns.com`
  - TTL: Automatic / 3600

بعد الحفظ، يتم فتح Vercel Dashboard -> المشروع `zaqzouka-tv` -> Settings -> Domains وإضافة الدومين. سيتم تفعيل شهادة الحماية SSL تلقائياً خلال 3 إلى 5 دقائق.

---
**تم التوثيق رسمياً وحفظه ضمن الأرشيف المرجعي للمشروع.**
