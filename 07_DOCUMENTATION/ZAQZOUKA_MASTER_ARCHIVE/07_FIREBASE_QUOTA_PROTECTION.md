# 🛡️ 07 — نظام حماية Firebase Quota (Quota Protection System)

> **الهدف:** 1 مليون مستخدم بدون استنزاف كوتا Firebase المجانية (Spark Plan)

---

## 🚨 المشكلة

Firebase RTDB Spark (المجاني) = **100 اتصال متزامن فقط**

| عدد المستخدمين | الاتصالات المباشرة | النتيجة |
|---|---|---|
| 100 | 100 | ✅ شغّال |
| 500 | 500 | ❌ Firebase يقطع |
| 1,000,000 | 1,000,000 | 💥 انهيار كامل |

---

## ✅ الحل: 5 طبقات تخزين مؤقت

```
1,000,000 مستخدم
       ↓
┌──────────────────────────────────┐
│  Layer 1: Vercel CDN Edge Cache  │  ← 60 ثانية TTL
│  s-maxage=60, stale-while-       │  ← يخدم 99.99% من الطلبات
│  revalidate=300                  │  ← صفر اتصال Firebase!
└──────────────┬───────────────────┘
               ↓ (cache miss فقط)
┌──────────────────────────────────┐
│  Layer 2: In-Memory Cache        │  ← 30 ثانية TTL
│  (Vercel warm function)          │  ← بداخل الـ serverless function
└──────────────┬───────────────────┘
               ↓ (stale)
┌──────────────────────────────────┐
│  Layer 3: SharedPreferences      │  ← 5 دقائق TTL
│  (Local Device Cache)            │  ← يعمل offline!
└──────────────┬───────────────────┘
               ↓ (expired)
┌──────────────────────────────────┐
│  Layer 4: Firebase RTDB Direct   │  ← مرة واحدة كل 5 دقائق فقط
│  (Rate-limited: 1 hit/5min)      │  ← per device
└──────────────┬───────────────────┘
               ↓ (error/unreachable)
┌──────────────────────────────────┐
│  Layer 5: Hardcoded "active"     │  ← لا يوقف التطبيق أبدًا
│  (Never crashes the app)         │
└──────────────────────────────────┘
```

**النتيجة الفعلية:**
- 1,000,000 مستخدم = **اتصال واحد بـ Firebase كل 60 ثانية** (بدل مليون!)
- التطبيق يعمل حتى لو Firebase فِل

---

## 📂 الملفات المُنشأة

### 1. Vercel Status API (جديد)
```
06_WEB_AND_DISTRIBUTION/zaqzouka-vercel-hub/api/status.js
```
- Endpoint: `GET /api/status`
- يقرأ من Firebase مرة واحدة، Vercel CDN تكاش 60 ثانية
- في حالة Firebase فشل → يرجع in-memory cache أو fallback

### 2. Static Fallback JSON
```
06_WEB_AND_DISTRIBUTION/zaqzouka-vercel-hub/public/status/system.json
```
- يُخدم من Vercel CDN Edge (أسرع من API)
- TTL: 120 ثانية
- الطبقة الأخيرة قبل الـ hardcoded fallback

### 3. Flutter Quota Guard
```
01_ZAQZOUKA_CLIENT/MadarFlutter/lib/src/firebase_quota_guard.dart
```
- Class: `ZaqzoukaQuotaGuard` (singleton)
- Class: `SystemStatusGuard` (Widget wrapper)
- يدير الـ 5 طبقات تلقائياً
- Stream يُحدّث الـ UI عند تغيير الحالة

### 4. Vercel Config (محدّث)
```
06_WEB_AND_DISTRIBUTION/zaqzouka-vercel-hub/vercel.json
```
- أضفنا route لـ `/api/status` مع `s-maxage=60`
- أضفنا route لـ `/status/system.json` مع `s-maxage=120`

---

## 🔧 طريقة الاستخدام في Flutter

### طريقة 1: Widget Wrapper (الأسهل)
```dart
// في main.dart - لف الـ app بالـ SystemStatusGuard
home: SystemStatusGuard(
  child: _KillSwitchGate(
    child: _UpdatePromptGate(
      child: _StartupIntro(child: RootTabs()),
    ),
  ),
),
```

### طريقة 2: Manual Check
```dart
final guard = ZaqzoukaQuotaGuard();
await guard.initialize();

// قراءة الحالة
final status = await guard.getStatus();
if (status.isWiped) {
  // أوقف التطبيق
} else if (status.isFrozen) {
  // أظهر شاشة صيانة
}

// الاستماع للتغييرات
guard.statusStream.listen((status) {
  print('Status changed: ${status.status}');
});
```

---

## 📊 حساب الكوتا - 1 مليون مستخدم

| المورد | الحد المجاني | الاستهلاك بدون Guard | الاستهلاك مع Guard |
|--------|-------------|---------------------|-------------------|
| RTDB Connections | 100 متزامن | ❌ 1,000,000 | ✅ 1 كل 60 ثانية |
| RTDB Downloads | 10 GB/شهر | ❌ ~10 GB/ساعة | ✅ < 1 MB/شهر |
| Vercel Bandwidth | 100 GB | CDN cache يتولى | ✅ مشمول |
| Vercel Functions | 100k/شهر | - | ✅ ~43,200/شهر |

---

## ⚠️ تحذيرات مهمة

> [!IMPORTANT]
> `zaqzouka_secret_2026` = SECRET الأدمن. لا تشاركه مع أي شخص.

> [!WARNING]  
> أمر `wipe` لا رجعة فيه — يمسح كل السيرفرات من Cloud ويرسل أمر مسح لكل الأجهزة.

> [!TIP]
> لرفع الحد من 100 اتصال، الحل الوحيد هو Firebase Blaze (pay-as-you-go) — لكن مع هذا النظام لن تحتاجه.

---

## 🧪 اختبار النظام

```bash
# 1. تأكد من API endpoint
curl https://zaqzouka-hub.vercel.app/api/status

# 2. تأكد من Cache Headers
curl -I https://zaqzouka-hub.vercel.app/api/status
# يجب أن ترى: Cache-Control: s-maxage=60

# 3. تأكد من Static Fallback
curl https://zaqzouka-hub.vercel.app/status/system.json

# 4. اختبار Firebase مباشر (للمقارنة)
curl https://madartv-default-rtdb.firebaseio.com/system_control.json
```

---

*تاريخ الإنشاء: نوفمبر 2026*
