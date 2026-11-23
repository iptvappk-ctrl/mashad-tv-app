# 🛡️ تقرير التوثيق الأمني وهندسة النظام — زقزوقة TV & حمادة AI

**تاريخ التحديث:** 21 نوفمبر 2026  
**الحالة:** مكتمل ومفعل في الإنتاج (Production Active) ✅  

---

## 1. ملخص الإجراءات الأمنية المنفذة

1. **تدوير المفتاح السري (Secret Key Rotation):**
   * إبطال المفتاح القديم `zaqzouka_secret_2026` فوراً من كافة المنظومات.
   * توليد واعتماد مفتاح أمان مشفر جديد عالي التعقيد: `[ADMIN_SECRET_REDACTED]`.
   * تحديث متغير البيئة `ADMIN_SECRET` في مشروع Vercel السحابي وإعادة النشر بالكامل.

2. **تطهير واجهة المستخدم (UI Sanitization):**
   * إزالة زر ترس الإعدادات (`tune_rounded`) من التطبيق تماماً.
   * حذف نافذة حوار "إعدادات الاتصال بالسحابة" بالكامل من الكود.
   * تثبيت العناوين والمفاتيح داخلياً في الباك إند (Hardcoded Internal Constants) دون أي واجهة مستخدم تمكن أي طرف من الاطلاع عليها أو تعديلها.

3. **إعادة بناء وتحديث حزم التطبيقات (APK Rebuild & Deployment):**
   * بناء نسخة APK رسمية نظيفة وتثبيتها واختبارها حياً على المحاكي والتأكد من اختفاء زر الإعدادات وظهور حالة الاتصال بنجاح.
   * رفع وتحديث السيرفرات السحابية.

---

## 2. جدول بيانات الاعتماد والمفاتيح المحدثة (Credentials)

| العنصر | القيمة القديمة (ملغاة ومحظورة) | القيمة الجديدة المعتمدة (نشطة الآن) | مكان الاستخدام |
|---|---|---|---|
| **Admin Secret** | `zaqzouka_secret_2026` | `[ADMIN_SECRET_REDACTED]` | Vercel API / Admin App / Master Control |
| **Master Secret** | — | `zaqzouka_master_2026` | Emergency Reset Endpoints |
| **API Client Key** | — | `ZQ-OWNER-2026-SECRET-14MOON` | ترخيص تطبيقات العملاء |
| **Firebase RTDB** | — | `https://madartv-default-rtdb.firebaseio.com` | قاعدة البيانات الحية والتحكم اللحظي |

---

## 3. السيرفرات السحابية وروابط الـ Backend الحية

تم إعادة نشر (Redeploy) الخدمات التالية على Vercel بأحدث إعدادات أمان:

* **بوابة التوزيع والموقع الرسمي للمنظومة:**  
  https://zaqzouka-tv.vercel.app  
  *Deployment Status:* Ready ✅ (توزيع مباشر لتطبيقات APK بدون إعلانات)

* **لوحة تحكم وتحليلات السحابة الرئيسية:**  
  https://zaqzouka-dashboard-api.vercel.app  
  *Deployment ID:* `dpl_GVXP7fPU8UhQiFwiBpeiX1DnysHD` (Ready ✅)

* **محور التوزيع والتحكم الاحتياطي (Failover Hub):**  
  https://zaqzouka-vercel-hub.vercel.app  
  *Deployment ID:* `dpl_GnQ8BZnvMxBTjcqZreicJK7pNVA8` (Ready ✅)

---

## 4. قائمة الملفات التي تم تعديلها

### أ. تطبيقات الهاتف (Flutter Clients):
1. **لوحة تحليلات الأدمن (`ZaqzoukaAdmin`):**
   * `04_ADMIN_DASHBOARD/ZaqzoukaAdmin/lib/main.dart`
     * تحديث `_adminSecret` إلى القيمة الجديدة المشفرة.
     * حذف زر `IconButton` الخاص بإعدادات الربط.
     * حذف كود `_showSettingsDialog` بالكامل.
2. **تطبيق التحكم الماستر (`ZaqzoukaMasterControl`):**
   * `03_MASTER_CONTROL/ZaqzoukaMasterControl/lib/api_client.dart`
     * تحديث `_adminSecret = '[ADMIN_SECRET_REDACTED]'`.

### ب. الخدمات الخلفية والسحابية (Cloud Backend & Functions):
1. `04_ADMIN_DASHBOARD/dashboard-api/api/admin.js`: تحديث التحقق من الـ `ADMIN_SECRET`.
2. `04_ADMIN_DASHBOARD/dashboard-api/api/system_control.js`: تحديث مفتاح المصادقة الداخلي.
3. `06_WEB_AND_DISTRIBUTION/zaqzouka-vercel-hub/api/system_control.js`: تحديث التحقق في السيرفر الاحتياطي.
4. `10_SCRATCH_AND_TOOLS/scratch/set-vercel-env.js`: سكريبت تدوير المفاتيح على Vercel CLI.

---

## 5. النسخ التنفيذية الجاهزة للتوزيع (Binaries & APKs)

| اسم التطبيق | المسار | الحجم | التفاصيل |
|---|---|---|---|
| **Zaqzouka TV Client** | `ZAQZOUKA_OFFICIAL_WEBSITE/downloads/zaqzouka-app.apk` | 53.4 MB | تطبيق المشاهدة الرئيسي للجمهور |
| **ZAG Player Engine** | `ZAQZOUKA_OFFICIAL_WEBSITE/downloads/zag-player.apk` | 4.6 MB | مشغل الفيديو النقي التوربيني |
| **Zaqzouka Admin** | `c:\Users\hassa\Desktop\zaqzouka-admin.apk` | 49.8 MB | نسخة الأدمن الشاملة والآمنة بدون زر إعدادات |
| **Zaqzouka Admin v2** | `c:\Users\hassa\Desktop\zaqzouka-admin-v2.apk` | 49.8 MB | نسخة احتياطية متطابقة على الديسكتوب |
| **Hamada AI** | `c:\Users\hassa\Desktop\Hamada_AI.apk` | 16.1 MB | المساعد الصوتي المصري الفوري (Whisper + Gemini TTS) |

---

## 6. موقع الويب الرسمي المخصص للتوزيع
* المجلد الرئيسي للموقع داخل المشروع: `ZAQZOUKA_OFFICIAL_WEBSITE/`
* النسخة المتزامنة في التوزيع: `06_WEB_AND_DISTRIBUTION/download-landing-page/`
* الرابط المباشر للجمهور: https://zaqzouka-tv.vercel.app
* تم توثيق كامل البنية في: `07_DOCUMENTATION/11_ZAQZOUKA_OFFICIAL_WEBSITE_AND_DISTRIBUTION.md`
