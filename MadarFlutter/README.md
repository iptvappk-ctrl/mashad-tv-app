# زقزوقة Flutter

نسخة Flutter مستقلة بتصميم RTL فاتح. تعرض الكتالوج والبحث والتفاصيل والمواسم والحلقات والمفضلة وجدول المباريات، وتستخدم كاش Android محلي للنتائج. فتح الفيديو يتم عبر تطبيق `com.madar.player` من خلال MethodChannel.

## إعداد خادم المحتوى

العنوان الافتراضي `http://10.0.2.2:3000` مناسب لمحاكي Android فقط عندما يعمل خادم `web` على الكمبيوتر. يمكنك تغيير العنوان من زر إعداد الخادم داخل التطبيق وحفظ عنوان HTTPS عام تملكه ويمكن للجهاز الوصول إليه. لا يوجد عنوان API إنتاجي عام مضبوط في هذا المستودع حاليًا.

```powershell
& 'C:\src\flutter\bin\flutter.bat' build apk --debug --dart-define=MADAR_API_BASE_URL=https://YOUR-HOST
```

واجهات API المستخدمة موضحة في `../DOCUMENTATION/12_FLUTTER_PORT_AND_FAST_START.md`. لا تحفظ مفاتيح API في التطبيق أو ملف dart-define؛ هذا العنوان عام ويظهر في APK.
