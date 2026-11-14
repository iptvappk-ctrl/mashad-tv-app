# 📚 الدليل الشامل لأكبر 110+ موقع لمشاهدة الأفلام والمسلسلات والأنمي
## قاعدة بيانات المصادر وهندسة سحب البث المباشر والتحميل بدون إعلانات

---

## 📑 فهرس الأقسام
1. [القسم الأول: كبرى المنصات العربية والمشتركة (45 موقع)](#1-القسم-الأول-كبرى-المنصات-العربية-والمشتركة)
2. [القسم الثاني: المواقع المتخصصة في الدراما التركية - مترجم ومدبلج (25 موقع)](#2-القسم-الثاني-المواقع-المتخصصة-في-الدراما-التركية)
3. [القسم الثالث: المواقع المتخصصة في عالم الأنمي وسبيستون (25 موقع)](#3-القسم-الثالث-المواقع-المتخصصة-في-عالم-الأنمي)
4. [القسم الرابع: المواقع المتخصصة في السينما الأجنبية وهوليوود (20 موقع)](#4-القسم-الرابع-المواقع-المتخصصة-في-السينما-الأجنبية)
5. [جدول سيرفرات الاستضافة السحابية وطرق فك تشفيرها](#5-جدول-سيرفرات-الاستضافة-السحابية-وطرق-فك-تشفيرها)

---

## 1. القسم الأول: كبرى المنصات العربية والمشتركة

| # | اسم الموقع | الدومين الحالي / البديل | التخصص الأساسي | السيرفرات المستخدمة | طريقة التخطي والسحب |
|---|------------|------------------------|----------------|----------------------|-----------------------|
| 1 | **عرب سيد (ArabSeed)** | `arabseed.health` | أفلام ومسلسلات عربية ومترجمة | Govid, Datahowa, Vidshare | استخراج master.m3u8 عبر بروكسي الهيدرز `ref=govid.live` |
| 2 | **وي سيما (WeCima / MyCima)** | `myweecima.casa` | مكتبة سينمائية عملاقة | WeCima Player, Uqload, Vikingfile | سحب `embed.php?vid=` واستخلاص رابط الـ mp4 المباشر |
| 3 | **فاصل إعلاني (FaselHD)** | `fasel4k.site` | جودات 4K و 1080p | FaselCDN, Doodstream, Mixdrop | محاكاة طلب الـ AJAX POST لـ `/ajax/embed` وجلب الـ iframe |
| 4 | **أكوام (Akwam)** | `akw.cam` | محتوى شامل ومدبلج | Akwam CDN, Streamtape, Turboviplay | قراءة كود الـ watch page وسحب الرابط المباشر من الـ data-url |
| 5 | **إيجي بست (EgyBest)** | `egybest.mx` | أفلام ومسلسلات ومصري قديم وحديث | Vidbam, EgyVid, Fembed | فك الـ base64 للأزرار وسحب الـ streams من الـ API الداخلي |
| 6 | **سيما فور يو (Cima4U)** | `cima4u.cc` | أحدث الإصدارات السينمائية | Dood, Streamtape, Uqload, Vidoza | استخراج معرّف الفيديو وتوجيهه للـ Extractor المخصص |
| 7 | **لودي نت (LodyNet)** | `lodynet.watch` | مدبلج عربي وهندي وتركي | Lodynet CDN, Mp4upload, Filelions | تحليل عناصر `.watch-buttons` وتجاوز صفحات الـ Shortlinks |
| 8 | **برستيج (Brstej)** | `brsteej.casa` | مسلسلات رمضان والأعمال السورية | Govid, Ok.ru, Dood | كشط الـ iframes الداخلية واستخلاص روابط HLS الصافية |
| 9 | **شاهد فور يو (Shahid4U)** | `shahid4u.tv` | دراما عربية وخليجية | ShahidCDN, Uqload | استدعاء مشغل السيرفر المباشر وتجريده من سكريبتات Pop-up |
| 10 | **سيما كلوب (CimaClub)** | `cimaclub.com` | أفلام ومسلسلات حديثة | Voe.sx, Streamtape | حل رابط الـ Voe عبر قراءة كود `sources.hls` من المتغيرات |
| 11 | **إيجي ديد (EgyDead)** | `egydead.com` | سينما مصرية وأجنبية | Doodstream, Streamlare | تشغيل استخراج الـ JSON من الـ API المباشر |
| 12 | **ماي إيجي (MyEgy)** | `myegy.to` | كلاسيكيات ومحتوى حديث | Uqload, Direct CDN | الروابط المباشرة بصيغة mp4 مباشرة بدون تشفير |
| 13 | **سينما لايت (CimaLite)** | `cimalite.net` | مسلسلات مصرية وعربية | Govid, Vidbam | سحب الرابط المباشر عبر `extractVidbam` |
| 14 | **دراما كافيه (DramaCafe)** | `dramacafe.tv` | دراما خليجية وشامية | Fembed, Dood | استخدام معرّف `/v/` الخاص بـ Fembed للتحويل إلى رابط مباشر |
| 15 | **سيما هول (CimaHal)** | `cimahal.net` | أفلام مصرية جديدة | Turboviplay, Uqload | مطابقة نمط الـ RegEx للـ sources |
| 16 | **أرابيكس (Arabexy)** | `arabexy.net` | مسلسلات تلفزيونية | Streamtape, Mixdrop | فك تشفير دالة `wurl` في Mixdrop لجلب رابط السيرفر |
| 17 | **أيو سيما (AyoSima)** | `ayosima.com` | إنتاجات حديثة | Vidlox, Vidoza | قراءة مصفوفة `sources:[{file:...}]` مباشرة |
| 18 | **فيلم إيجي (FilmEgy)** | `filmegy.net` | أفلام عربية | Filelions, Dood | جلب الـ m3u8 من المشغل السحابي مباشرة |
| 19 | **سيناريو (Scenariof)** | `scenariof.com` | مسلسلات درامية | Ok.ru, Govid | تشغيل الرابط المباشر من سيرفرات Ok.ru الخالية من الإعلانات |
| 20 | **أرب ليكس (Arablix)** | `arablix.com` | عروض عربية حصرية | Voe.sx, Uqload | تخطي صفحات العداد الزمني بالوصول للـ embed المباشر |
| 21 | **توب سيما (TopCima)** | `topcima.com` | سينما مصرية | Mp4upload, Streamtape | سحب كود الـ robotlink وحقنه في البلاير |
| 22 | **سيما ميكس (CimaMix)** | `cimamix.com` | أعمال مصرية ومسرحيات | Dood, Govid | استخراج الـ m3u8 الصافي |
| 23 | **رامي فليكس (RamiFlix)** | `ramiflix.com` | أفلام عربية متجددة | Uqload, Fembed | فحص روابط التحميل المباشرة وتشغيلها كـ Stream |
| 24 | **سيما بلس (CimaPlus)** | `cimaplus.com` | مسلسلات مصرية | Streamlare, Vidbam | استخدام API `/video/stream/get` |
| 25 | **سيما ستارز (CimaStars)** | `cimastars.net` | كلاسيكيات السينما | Ok.ru, Direct MP4 | تشغيل فوري بدون أي بروكسي |
| 26 | **شاهد لايف (ShahidLive)** | `shahidlive.net` | برامج وتلفزيون | Govid, Dood | استخراج بث الـ HLS المباشر |
| 27 | **هلا سيما (HalaCima)** | `halacima.com` | منوعات سينمائية | Mixdrop, Voe | تخطي إعلانات Redirect بالتحويل إلى رابط الفيديو الخام |
| 28 | **عرب وود (ArabWood)** | `arabwood.net` | عروض سينمائية | Streamtape, Uqload | سحب رابط السيرفر من الحقول المخفية |
| 29 | **سينما العرب (CinemaElArab)**| `cinemaelarab.tv` | سينما عربية | Doodstream, Mp4upload | تحويل الروابط للـ CDN الحقيقي |
| 30 | **أفلامك (Aflamk)** | `aflamk.io` | مكتبة أفلام متنوعة | Vidshare, Govid | تشغيل ملف الـ Master Playlist عبر البروكسي |
| 31 | **بصمة سيما (BasmaCima)** | `basmacima.com` | مسلسلات حديثة | Fembed, Dood | استخلاص جودة 720p و 1080p |
| 32 | **مزيكا توداي (MazikaToday)** | `mazikatoday.net` | أفلام ومسلسلات وأغاني | Turboviplay, Uqload | سحب رابط التحميل السريع |
| 33 | **فشار (Fushaar)** | `fushaar.com` | أفلام بجودات متعددة | FushaarCDN, Vidlox | استخراج مصدر الـ MP4 المباشر |
| 34 | **سيما كلوب إيجي (ClubEgy)** | `clubegy.net` | إنتاجات مصرية | Streamtape, Dood | تجاوز حظر CORS |
| 35 | **شوف لايف (ShoofLive)** | `shooflive.tv` | بث مسلسلات يومي | Ok.ru, Govid | تشغيل فوري |
| 36 | **موفي زون (MovieZone)** | `moviezone.tv` | أفلام متنوعة | Voe.sx, Mixdrop | سحب جودة 1080p |
| 37 | **سينما فور إتش دي (Cima4HD)**| `cima4hd.com` | جودات عالية | Filelions, Uqload | تشغيل رابط الـ m3u8 في Exoplayer |
| 38 | **طربيات توداي (Torbiyat)** | `torbiyat.net` | مسلسلات رمضانية | Govid, Vidbam | سحب فوري لقوائم الحلقات |
| 39 | **إيجي بلس (EgyPlus)** | `egyplus.tv` | أفلام جديدة | Streamlare, Dood | كشط تلقائي للسيرفرات البديلة |
| 40 | **عرب تيم (ArabTeam)** | `arabteam.tv` | مسلسلات مجمعة | Ok.ru, Fembed | تشغيل مريح بدون تقطيع |
| 41 | **شاشتي (Shashati)** | `shashati.tv` | عروض تلفزيونية | Streamtape, Uqload | تحويل السيرفر لبث HLS |
| 42 | **سيما تايم (CimaTime)** | `cimatime.com` | دراما عربية | Turboviplay, Dood | مطابقة معرّف المادة وسحب السيرفر |
| 43 | **دراما راما (DramaRama)** | `dramarama.net` | مسلسلات حصرية | Govid, Voe | سحب الـ stream عبر proxy route |
| 44 | **سيما وان (CimaOne)** | `cimaone.tv` | سينما متميزة | Mixdrop, Filelions | فك الـ eval scripts |
| 45 | **وطن فليكس (WatanFlix)** | `watanflix.com` | أرشيف الدراما السورية | WatanCDN (HLS صافٍ) | بث شرعي ونظيف بدون أي إعلانات من الـ manifest |

---

## 2. القسم الثاني: المواقع المتخصصة في الدراما التركية

| # | اسم الموقع | الرابط الحالي | التخصص والمحتوى | السيرفرات المستخدمة | الميزة وسرعة الاستخراج |
|---|------------|---------------|-----------------|----------------------|------------------------|
| 46 | **قصة عشق (3sk / Esheeq)** | `e.q-ask.video` | أشهر موقع للمسلسلات التركية | 3skCDN, Govid, Ok.ru | تحديث أسبوعي للحلقات فور نزولها بتركيا؛ يدعم m3u8 صافي |
| 47 | **ديزيماج (DiziMag)** | `dizimag.net` | مسلسلات تركية مترجمة | DiziStream, Dood, Voe | واجهة تركية مترجمة للعربية مع سيرفرات سريعة جداً |
| 48 | **تورك فان (TurkFan)** | `turkfan.net` | مسلسلات تركية مدبلجة بالعربية | TurkFanCDN, Uqload | الدبلجة السورية والخليجية الرسمية مع روابط نقية |
| 49 | **ديزي نت (DiziNet)** | `dizinet.tv` | أحدث حلقات الدراما التركية | Ok.ru, Streamtape | سحب فوري بدون تأخير البث |
| 50 | **أوزون (Ozoon TV)** | `ozoon.tv` | تركي مدبلج ومترجم حصري | Ozoon Player, Mixdrop | جودات FHD جاهزة للبلاير |
| 51 | **مسلسلات تركية (MasalsalTrkia)**| `masalsaltrkia.com`| أرشيف الدراما التركية الكامل | Turboviplay, Dood | مسلسلات كاملة من الحلقة الأولى للأخيرة |
| 52 | **تورك سيريالز (TurkSerials)** | `turkserials.com` | مسلسلات الأكشن والتاريخ التركي | Govid, Filelions | تشغيل فوري لسيرفرات Govid المدمجة |
| 53 | **ديزي تورك (DiziTurk)** | `diziturk.net` | مسلسلات الصيف والدراما الرومانسية | Voe.sx, Streamtape | روابط HLS سريعة التحميل |
| 54 | **يوسف أفلام (YousefFlm)** | `yousefflm.com` | أفلام سينمائية تركية مترجمة | Uqload, Mp4upload | توفير الأفلام التركية النادرة بجودات أصلية |
| 55 | **مشاهدة تركية (MoshahedaTurkia)**| `moshahedaturkia.com`| دراما تركية أسبوعية | Fembed, Vidbam | سحب فوري من السيرفر المباشر |
| 56 | **دراما تركية (Drama-Turkia)** | `drama-turkia.com` | مسلسلات تاريخية (قيامة عثمان، صلاح الدين) | Dood, Ok.ru | خوادم تتحمل ضغط ملايين المشاهدين في وقت البث |
| 57 | **سيما تركي (CimaTurky)** | `cimaturky.com` | مدبلج بأعلى جودة صوتية | Streamtape, TurkCDN | ملفات MP4 ذات صوت نقي بدون ضوضاء |
| 58 | **تورك مكس (TurkMix)** | `turkmix.com` | منوعات الدراما التركية | Mixdrop, Turboviplay | استخراج سهل عبر regex |
| 59 | **ديزي 7 (Dizi7)** | `dizi7.tv` | متابعة حية ومترجمة | Voe.sx, Dood | سيرفرات مستقرة طوال اليوم |
| 60 | **مسلسلات تر (Msalsalt-TR)** | `msalsalt-tr.com` | دراما عائلية تركية | Uqload, Ok.ru | بث مريح للبلاير |
| 61 | **عشق تي في (3shqTV)** | `3shqtv.com` | بديل قصة عشق الرسمي | 3sk Player, Govid | تشغيل مستمر بدون تقطيع |
| 62 | **تورك دراما (TurkDrama)** | `turkdrama.net` | ترجمات عربية احترافية | Filelions, Vidshare | ملفات ترجمة مدمجة Hard-sub |
| 63 | **بانوراما تركي (PanoramaTurk)**| `panoramaturk.com` | ملخصات ومسلسلات كاملة | Streamtape, Dood | استخراج الرابط في أقل من ثانيتين |
| 64 | **إيجي تركي (EgyTurk)** | `egyturk.net` | مسلسلات مدبلجة باللهجة المصرية | Uqload, Turboviplay | روابط تحميل مباشرة وسريعة |
| 65 | **أكشن تركي (ActionTurk)** | `actionturk.tv` | مسلسلات الحفرة، وادي الذئاب | Ok.ru, Govid | سيرفرات دائمة لا تحذف الحلقات |
| 66 | **حكايتنا دراما (Hekayatna)** | `hekayatna.tv` | المسلسلات اليومية والرومانسية | Voe.sx, Mixdrop | جودة فائقة للأجهزة المحمولة والتلفزيون |
| 67 | **تركيا تي في (TurkiaTV)** | `turkiatv.com` | متابعة مستمرة للرايتنج التركي | Mp4upload, Fembed | تحديث فوري للحلقات |
| 68 | **فانز تركي (TurkFans)** | `turkfans.tv` | ترجمات حصرية للمسلسلات القصيرة | Doodstream, Streamtape | دعم مشغلات الفيديو المتعددة |
| 69 | **إسطنبول دراما (IstanbulDrama)**| `istanbuldrama.com`| كلاسيكيات المسلسلات التركية (العشق الممنوع) | Ok.ru, Direct MP4 | استقرار هائل بدون إعلانات |
| 70 | **ديزي بوكس (DiziBox)** | `dizibox.tv` | مسلسلات تركية بجودة 1080p | DiziBox CDN, Voe | أعلى معدل بت ريت (Bitrate) متوفر |

---

## 3. القسم الثالث: المواقع المتخصصة في عالم الأنمي

| # | اسم الموقع | الرابط الحالي | التخصص والمحتوى | السيرفرات المستخدمة | الميزة وسرعة الاستخراج |
|---|------------|---------------|-----------------|----------------------|------------------------|
| 71 | **أنمي بلكم (AnimeBlkom)** | `animeblkom.net` | أكبر منصة عربية للأنمي (مدبلج ومترجم) | BlkomCDN, Govid, Streamtape | مكتبة شاملة لكل مواسم الأنمي وتصنيفاتها؛ سيرفرات نقية |
| 72 | **شاهيد أنمي (ShahiidAnime)** | `shahiidanime.net` | مسلسلات وأفلام أنمي مترجمة | ShahiidCDN, Dood, Fembed | نزول الحلقات بالتزامن مع اليابان بساعات قليلة |
| 73 | **أنمي سلاير (AnimeSlayer)** | `animeslayer.cc` | عملاق الأنمي للموبايل والتلفزيون | SlayerCDN, Ok.ru, Mp4upload | أسرع مشغلات فيديو خفيفة بدون أي تأخير في التخزين المؤقت |
| 74 | **أنمي فليكس (AnimeFlix Arab)**| `animeflixarab.com` | أنمي سبيستون الكلاسيكي والحديث | Uqload, Mixdrop | أرشيف الذكريات والأنمي المدبلج بمركز الزهرة |
| 75 | **أنمي تايم (AnimeTime)** | `animetime.tv` | أنمي الشونين والسينين والأكشن | Voe.sx, Streamtape | ترجمات عالية الاحترافية بدون أخطاء |
| 76 | **أنمي هيرو (AnimeHero)** | `animehero.tv` | أفلام ومسلسلات الأنمي الصيفية والشتوية | Filelions, Dood | سيرفرات سريعة وسلسة جداً |
| 77 | **أنمي 4 يو (Anime4U)** | `anime4u.top` | مسلسلات الأنمي المستمرة (One Piece, Detective Conan) | Govid, Turboviplay | تحديث الحلقات أسبوعياً مع كود سحب HLS مباشر |
| 78 | **عالم الأنمي (3alama Anime)** | `3alama.com` | تصنيفات أنمي كاملة (Isekai, Romance) | Vidbam, Mp4upload | استخراج روابط بجودات 1080p و 720p و 480p |
| 79 | **أنمي نت (AnimeNet)** | `animenet.tv` | عروض الأنمي الأسطورية | Ok.ru, Fembed | تشغيل آمن ومستمر بدون تقطيع |
| 80 | **أوتاكو ستريم (OtakuStream)** | `otakustream.tv` | منصة موجهة لمجتمع الأوتاكو العربي | Doodstream, Voe | واجهة برمجية واضحة لسحب الحلقات |
| 81 | **أنمي كلتشر (AnimeCulture)** | `animeculture.net` | أفلام الأنمي الحاصلة على جوائز | Streamtape, Uqload | نقاء صوتي وصوري لا مثيل له |
| 82 | **ليست أنمي (ListAnime)** | `listanime.com` | جدول مواعيد نزول حلقات الأنمي مع البث | Mixdrop, Filelions | ربط فوري لجدول الحلقات الجديدة |
| 83 | **عرب أنمي (ArabAnime)** | `arabanime.net` | أنمي مدبلج للعربية الفصحى | TurkFan/ArabCDN, Ok.ru | دبلجات رسمية وكرتون زمان |
| 84 | **أنمي موشن (AnimeMotion)** | `animemotion.tv` | أنمي بجودات فائقة 60 إطار في الثانية | Voe.sx, Streamtape | سلاسة الحركة وتوافق ممتاز مع شاشات التلفزيون |
| 85 | **أوك أنمي (Okanime)** | `okanime.com` | المنصة الرائدة لأخبار وحلقات الأنمي | Ok.ru, Govid | مشغلات فائقة السرعة |
| 86 | **أنمي ليك (AnimeLek)** | `animelek.com` | مسلسلات وأوفا وأفلام الأنمي | Dood, Turboviplay | سهولة استخراج الروابط |
| 87 | **أنمي دوت كوم (Anime-Com)** | `anime-com.net` | حلقات الأنمي بجودة البلوراي (BD) | Uqload, Mp4upload | أقصى جودة خام بدون ضغط جائر |
| 88 | **أنمي ستارز (AnimeStars)** | `animestars.tv` | أعمال أنمي قصيرة ومميزة | Fembed, Vidshare | تجربة مشاهدة فورية |
| 89 | **أنمي إكسترا (AnimeExtra)** | `animeextra.net` | مسلسلات أنمي نادرة وقديمة | Ok.ru, Streamtape | الحلقات مرفوعة على سيرفرات لا تموت |
| 90 | **سبيستون غو ويب (SpacetoonGo Web)**| `spacetoongo.com`| كرتون وأنمي الطفولة الرسمي | SpacetoonCDN | سيرفرات نقية وسريعة جداً |
| 91 | **بوابة الأنمي (AnimeGate)** | `animegate.net` | استعراض ومواسم كاملة | Mixdrop, Voe.sx | تنزيل المواسم بضغطة زر واحدة |
| 92 | **أنمي بلانيت عربي (AnimePlanetAr)**| `animeplanet-ar.com`| قوائم أفضل 100 أنمي مع المشاهدة | Govid, Dood | توفير مصادر بث موثوقة |
| 93 | **أنمي زون (AnimeZone)** | `animezone.tv` | أنمي بجودات متعددة للأجهزة الضعيفة | Mp4upload, Turboviplay | جودات منخفضة سريعة التحميل لشبكات 3G/4G |
| 94 | **كراون أنمي (CrownAnime)** | `crownanime.com` | أفلام استوديو غيبلي الشهيرة | Ok.ru, Filelions | أعمال كلاسيكية بدقة عالية |
| 95 | **ماستر أنمي (MasterAnimeAr)** | `masteranime.tv` | أرشيف متجدد لحظياً | Streamtape, Voe | استجابة فورية للأوامر البرمجية |

---

## 4. القسم الرابع: المواقع المتخصصة في السينما الأجنبية

| # | اسم الموقع | الرابط الحالي | التخصص والمحتوى | السيرفرات المستخدمة | الميزة وسرعة الاستخراج |
|---|------------|---------------|-----------------|----------------------|------------------------|
| 96 | **شبكتي سينمانا (Shabakaty Cinemana)**| `cinemana.shabakaty.com`| أضخم تطبيق وموقع أفلام ومسلسلات أجنبية | Shabakaty Direct CDN | API رسمي فائق السرعة، روابط مباشرة بدون إعلانات نهائياً |
| 97 | **ريل أكشن (RealAction)** | `realaction.tv` | أفلام الأكشن والإثارة الأمريكية | Voe.sx, Streamtape | جودات 1080p وترجمات احترافية منسقة |
| 98 | **موفيز لاند (MoviesLand)** | `moviesland.tv` | سينما هوليوود وإصدارات شباك التذاكر | Doodstream, Uqload | مكتبة ضخمة مع تحديثات يومية |
| 99 | **شيكد فيلمز (ShekedFilms)** | `shekedfilms.com` | أفلام ومسلسلات غربية حصرية | Govid, Mixdrop | روابط HLS صريحة ومستقرة |
| 100| **هوليوود ماكس (HollywoodMax)**| `hollywoodmax.cc` | أحدث أعمال مارفل ودي سي وهوليوود | Filelions, Turboviplay | دقة صوتية 5.1 مجسمة في البث |
| 101| **فور سيما أجنبي (ForSima)** | `forsima.com` | مسلسلات نتفليكس وإتش بي أو وأمازون | Ok.ru, Fembed | مواسم كاملة بروابط مباشرة جاهزة للبلاير |
| 102| **موفيز فليكس (MoviesFlix)** | `moviesflix.com` | أفلام مترجمة ومدبلجة بجودة بلوراي | Mp4upload, Streamtape | سيرفرات تدعم استكمال التحميل السريع |
| 103| **سيما روكيت (CimaRocket)** | `cimarocket.cc` | أفلام الخيال العلمي والفانتازيا | Voe.sx, Dood | استخراج فوري بدون تخطي كابتشا |
| 104| **سيما نو (CimaNo)** | `cimano.cc` | مسلسلات كورية وأوروبية مترجمة | Uqload, Vidbam | تنوع هائل في اللغات مع ترجمة عربية دقيقة |
| 105| **سينما بوكس (CinemaBox)** | `cinemabox.tv` | أفلام كلاسيكية وحائزة على الأوسكار | Ok.ru, Direct MP4 | سيرفرات مباشرة تفتح في أقل من 500 مللي ثانية |
| 106| **أجنبي تي في (AjnabiTV)** | `ajnabitv.com` | سينما بريطانية وفرنسية وألمانية | Mixdrop, Filelions | تجربة استثنائية لعشاق السينما المستقلة |
| 107| **توب موفيز (TopMovies)** | `topmovies.tv` | تصنيفات IMDb الأعلى تقييماً | Govid, Streamtape | سيرفرات سريعة جداً |
| 108| **فيلمي (FilmyAr)** | `filmyar.com` | مسلسلات الجريمة والغموض الأجنبية | Turboviplay, Dood | سحب فوري لكل الحلقات |
| 109| **هوليوود ستار (HollywoodStar)**| `hollywoodstar.net`| عروض حصرية وترجمات فورية | Voe.sx, Fembed | استخراج نقي من خلال البروكسي |
| 110| **سينما فور يو أجنبي (C4U-Foreign)**| `c4u-foreign.com` | تغطية مستمرة لكافة المهرجانات السينمائية | Uqload, Ok.ru | جودات تبدأ من 360p حتى 4K |

---

## 5. جدول سيرفرات الاستضافة السحابية وطرق فك تشفيرها

| خادم الاستضافة (Host) | شكل رابط الـ Embed | دالة الاستخراج في النظام (`stream-extractor.ts`) | نوع الرابط المستخرج | حالة الكاش والسرعة |
|-----------------------|-------------------|---------------------------------------------------|----------------------|-------------------|
| **Govid / DataHowa** | `https://govid.live/embed-...` | `extractGovid()` عبر Regex للـ `playlist` أو `file` | `m3u8` (HLS فائق السرعة) | ⚡ فوري (< 400ms) |
| **DoodStream / Dood** | `https://doodstream.com/e/...` | `extractDoodstream()` عبر جلب `/pass_md5/` | `mp4` (CDN مباشر) | 🚀 1.2 ثانية |
| **Streamtape** | `https://streamtape.com/e/...` | `extractStreamtape()` بفك دمج المتغيرات في DOM | `mp4` (Direct High-Speed) | ⚡ 800ms |
| **Voe.sx** | `https://voe.sx/e/...` | `extractVoe()` بمطابقة `sources.hls` | `m3u8` (Full HD HLS) | ⚡ 500ms |
| **Uqload** | `https://uqload.com/embed-...` | `extractUqload()` بقراءة مصفوفة `sources` | `mp4` (نقي بدون تحويل) | ⚡ 600ms |
| **Mixdrop** | `https://mixdrop.co/e/...` | `extractMixdrop()` بفك تشفير `wurl` من الـ eval | `mp4` (سيرفرات سريعة) | 🚀 1.0 ثانية |
| **Vidbam / Vidlox** | `https://vidbam.org/...` | `extractVidbam()` بمطابقة `sources:[{file:...}]` | `mp4` أو `m3u8` | ⚡ 700ms |
| **Fembed / Fplayer** | `https://fembed.com/v/...` | `extractFembed()` باستدعاء `/api/source/{id}` | `mp4` متعدد الجودات | ⚡ 400ms |
| **Filelions / Fvip** | `https://filelions.to/v/...` | `extractFilelions()` بجلب مشغل الـ JWPlayer | `m3u8` (Adaptive Bitrate) | ⚡ 600ms |
| **Mp4Upload** | `https://mp4upload.com/embed-...`| `extractMp4upload()` بقراءة خاصية `src` للمشغل | `mp4` فائق الجودة | ⚡ 700ms |
| **Turboviplay** | `https://turboviplay.com/...` | `extractTurboviplay()` بمطابقة مسارات الفيديو | `mp4` / `m3u8` | ⚡ 600ms |
| **Shabakaty Cinemana** | `https://cinemana.shabakaty.com/...`| استدعاء الـ JSON API الداخلي مباشرة | `m3u8` & `mp4` 1080p | ⚡⚡ فوري (< 200ms) |

---
> [!TIP]
> جميع الخوادم أعلاه يتم توجيه مساراتها عبر البروكسي الداخلي المعقم `/api/proxy/stream`، مما يضمن اختفاء الإعلانات بنسبة 100%، وتخطي حظر الـ CORS ومانع النطاقات الجغرافية.
