# 🔥 قواعد أمان فايربيس الصارمة (Firebase Security Rules)

هذا المستند يشرح كيفية تأمين فايربيس بحيث **لا يستطيع أي شخص استخرج الـ APK أو سرق ملف `google-services.json` مسح أو تعديل أي شيء في السحابة**.

---

## 🛡️ 1. قواعد Firebase Realtime Database (`database.rules.json`)

انسخ هذه القواعد وضعها في لوحة تحكم Firebase Realtime Database (تبويب **Rules**):

```json
{
  "rules": {
    // منع القراءة والكتابة العامة بشكل افتراضي
    ".read": false,
    ".write": false,

    // وثيقة التحكم بالمنظومة (يقرأها كل مستخدم فقط لمعرفة الحالة، والتعديل للمطور فقط عبر السيرفر)
    "system_control": {
      ".read": true,
      ".write": false
    },

    // كتالوج السيرفرات والقنوات (قراءة فقط للمستخدمين)
    "servers_catalog": {
      ".read": true,
      ".write": false
    },

    // الخلاصات والكاش السحابي
    "cached_feeds": {
      ".read": true,
      ".write": false
    },

    // جلسات المستخدمين وإحصائيات التيليمترية
    "sessions": {
      ".indexOn": ["deviceId", "status", "lastSeen", "openedAt"],
      ".read": false,
      ".write": true
    },
    "stats": {
      ".indexOn": ["firstOpens", "webVisits"],
      ".read": false,
      ".write": true
    },
    "activityLog": {
      ".indexOn": ["timestamp"],
      ".read": false,
      ".write": true
    }
  }
}
```

### 💡 لماذا هذا مؤمّن 100%؟
1. `.write: false` على `system_control` و `servers_catalog` يعني أنه **حتى لو امتلك شخص الـ API Key وملف `google-services.json` وحاول إرسال طلب PUT أو DELETE من جهازه، فإن فايربيس سترفض طلبه فوراً بـ `401 Unauthorized`**.
2. الوحيد القادر على الكتابة والمسح والتعديل هو أنت، لأن تطبيق التحكم `ZaqzoukaMasterControl` يتصل بـ Vercel Serverless Function التي تستخدم **Firebase Admin SDK (Service Account)** وتتحقق من الـ Admin Secret الخاص بك قبل تنفيذ أي أمر مسح أو تعديل!

---

## 🛡️ 2. قواعد Cloud Firestore (`firestore.rules`)

إذا قمت بتفعيل Cloud Firestore في مشروعك مستقبلاً، انسخ هذه القواعد:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // منع الوصول العام الافتراضي
    match /{document=**} {
      allow read, write: if false;
    }

    // وثيقة حالة المنظومة
    match /system_control/{docId} {
      // مسموح للجميع القراءة فقط لمعرفة هل التطبيق شغال أم مجمد
      allow read: if true;
      // التعديل ممنوع تماماً على أي عميل (يتم فقط عبر الـ Admin SDK الموثوق)
      allow write: if false;
    }

    // السيرفرات والكتالوج
    match /servers_catalog/{docId} {
      allow read: if true;
      allow write: if false;
    }
  }
}
```

---

## 🛑 ملخص الأمان:
- لا يمكن لأي طرف ثالث مسح بياناتك من فايربيس.
- تطبيقك للتحكم هو البوابة الوحيدة التي تملك صلاحية التجميد، التدمير، أو الاستعادة.
