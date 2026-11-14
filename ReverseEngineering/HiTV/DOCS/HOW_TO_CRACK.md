# 🔓 طرق فك حماية HiTV بالكامل

## الطريقة 1: Frida Dynamic Analysis (الأسرع والأدق)
**المتطلبات:** جهاز Android مـ rooted أو Emulator

```bash
# 1. شغّل الـ APK على emulator
# 2. Attach Frida واعمل hook على JNI functions

frida -U -f tech.hitv.movie --no-pause -l hitv_hook.js
```

### hitv_hook.js
```javascript
// Hook على libdec.so JNI functions
Java.perform(function() {
    // Hook الـ OkHttp لالتقاط كل الـ requests والـ responses
    var OkHttpClient = Java.use("okhttp3.OkHttpClient");
    var Request = Java.use("okhttp3.Request");
    var Response = Java.use("okhttp3.Response");
    
    // Intercept كل الـ HTTP calls
    var Interceptor = Java.use("okhttp3.Interceptor");
    
    // Hook decryption function
    var JniUtil = Java.use("cibntest.baoyun.com.decjni.JniUtil");
    JniUtil.dec.implementation = function(encrypted, key) {
        var result = this.dec(encrypted, key);
        console.log("[DEC] Input:", encrypted.substring(0, 100));
        console.log("[DEC] Key:", key);
        console.log("[DEC] Output:", result.substring(0, 200));
        return result;
    };
    
    // Hook sign function
    JniUtil.sign.implementation = function(data) {
        var sig = this.sign(data);
        console.log("[SIGN] Data:", data);
        console.log("[SIGN] Signature:", sig);
        return sig;
    };
});
```

---

## الطريقة 2: Network Interception (الأسهل)
**المتطلبات:** mitmproxy + جهاز Android API level <= 24

```bash
# 1. شغّل mitmproxy
mitmproxy --mode transparent --ssl-insecure

# 2. على الجهاز: ضبط Proxy على IP الكمبيوتر:8080
# 3. شغّل HiTV وافتح محتوى
# 4. شوف كل الـ requests في mitmproxy
```

### نتيجة متوقعة:
```
GET https://api.???.com/s1/w/series/api/home
Headers:
  platform: android
  did: xxxxxxxxxxxx
  sign: md5(timestamp + secret_key)
  
Response: JSON مشفر أو plain JSON
```

---

## الطريقة 3: Memory Dump (لو مفيش روت)
```bash
# بعد تشغيل الـ App على Emulator (Android Studio)
# الـ DEX بيتفك في RAM
adb shell am start -n tech.hitv.movie/.ui.activity.HimaHomeActivity
adb shell dd if=/proc/$(adb shell pidof tech.hitv.movie)/mem bs=1M | strings | grep -E "https?://"
```

---

## ✅ إيه اللي هنعمله دلوقتي بدون فك الحماية

بما إن الـ API مش متاحة بعد، هنعمل **نظام هجين**:

1. **WebView مع AdBlock** — نعرض موقع HiTV بدون إعلانات
2. **Stream Extractor** — نسحب الـ m3u8 من الـ WebView
3. **لما نلاقي API** — نبدل لـ Native API calls

هذا النظام شغال زي ما بيشتغل Telegram Bots و YouTube scrapers.
