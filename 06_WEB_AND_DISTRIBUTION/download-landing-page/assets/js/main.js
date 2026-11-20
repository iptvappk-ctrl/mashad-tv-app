/**
 * ZAQZOUKA TV & ZAG PLAYER - OFFICIAL SCRIPT
 * High-performance vanilla JavaScript, zero external dependencies
 */

document.addEventListener('DOMContentLoaded', () => {
  initPhoneSwitcher();
  initFaqAccordion();
  initScrollEffects();
  initLanguageSwitcher();
  initQrModal();
});

/* -------------------------------------------------------------
   1. Interactive Phone Screen Switcher
   ------------------------------------------------------------- */
function initPhoneSwitcher() {
  const displayImg = document.getElementById('phone-display-img');
  const buttons = document.querySelectorAll('.switcher-btn');
  if (!displayImg || !buttons.length) return;

  buttons.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetSrc = btn.getAttribute('data-screen');
      if (!targetSrc || btn.classList.contains('active')) return;

      buttons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      displayImg.style.opacity = '0';
      setTimeout(() => {
        displayImg.src = targetSrc;
        displayImg.style.opacity = '1';
      }, 200);
    });
  });
}

/* -------------------------------------------------------------
   2. FAQ Accordion
   ------------------------------------------------------------- */
function initFaqAccordion() {
  const items = document.querySelectorAll('.faq-item');
  items.forEach(item => {
    const question = item.querySelector('.faq-question');
    const answer = item.querySelector('.faq-answer');
    if (!question || !answer) return;

    question.addEventListener('click', () => {
      const isOpen = item.classList.contains('active');

      // Close all
      items.forEach(i => {
        i.classList.remove('active');
        const a = i.querySelector('.faq-answer');
        if (a) a.style.maxHeight = null;
      });

      // Toggle current
      if (!isOpen) {
        item.classList.add('active');
        answer.style.maxHeight = answer.scrollHeight + 'px';
      }
    });
  });
}

/* -------------------------------------------------------------
   3. Header & Sticky Mobile Bar Scroll Effects
   ------------------------------------------------------------- */
function initScrollEffects() {
  const header = document.querySelector('.site-header');
  const mobileBar = document.querySelector('.mobile-bottom-bar');
  const hero = document.querySelector('.hero-section');

  window.addEventListener('scroll', () => {
    const scrollPos = window.scrollY;

    if (header) {
      if (scrollPos > 40) {
        header.classList.add('scrolled');
      } else {
        header.classList.remove('scrolled');
      }
    }

    if (mobileBar && hero) {
      const heroBottom = hero.offsetTop + hero.offsetHeight;
      if (scrollPos > heroBottom - 180) {
        mobileBar.style.display = 'grid';
      } else {
        mobileBar.style.display = 'none';
      }
    }
  });
}

/* -------------------------------------------------------------
   4. Language Switcher (AR / EN)
   ------------------------------------------------------------- */
const i18n = {
  ar: {
    brandTag: 'منظومة البث السينمائي المتكاملة',
    navHome: 'الرئيسية',
    navApps: 'التطبيقات',
    navHowTo: 'طريقة التثبيت',
    navFeatures: 'المميزات',
    navFaq: 'الأسئلة الشائعة',
    btnFastDownload: 'تحميل مباشر',
    heroBadge: 'الإصدار المعتمد 2026 — آمن 100% وخالٍ من الإعلانات',
    heroTitle: 'عالمك الترفيهي بدون حدود مع <span class="highlight-brick">تطبيق زقزوقة</span> ومحرك <span class="highlight-gold">زاغ بلاير</span>',
    heroDesc: 'منظومة متكاملة لمشاهدة آلاف الأفلام والمسلسلات الحصرية والقنوات التلفزيونية والرياضية الحية بأعلى جودة وسيرفرات فائقة السرعة بدون أي تقطيع أو اشتراكات.',
    heroBtnApp: 'تحميل تطبيق زقزوقة TV',
    heroBtnAppSub: 'v1.0.74 · حزمة APK مباشرة (53.4 MB)',
    heroBtnPlayer: 'تحميل مشغل زاغ ZAG Player',
    heroBtnPlayerSub: 'v1.1.28 · المحرك الأساسي (4.6 MB)',
    trustVerified: 'مفحوص ونظيف 100%',
    trustSmartTv: 'يدعم الهواتف وشاشات Smart TV',
    trustNoBuffer: 'تقنية مانع التقطيع',
    tabHome: 'الرئيسية',
    tabSeries: 'المسلسلات والمواسم',
    tabLive: 'البث المباشر',
    appsTag: 'التحميل المباشر',
    appsTitle: 'حمّل التطبيقين معاً لتجربة سينمائية كاملة',
    appsDesc: 'تطبيق زقزوقة يمنحك مكتبة الأفلام والمسلسلات والبحث الذكي، ومشغل زاغ يتولى فك التشفير والتشغيل الفوري بدون أي استهلاك للموارد.',
    app1Role: 'التطبيق الرئيسي',
    app1Desc: 'تصفح أكبر وأحدث مكتبة عربية وعالمية للأفلام والمسلسلات، مع تصنيفات منظمة ومواسم كاملة وتحديثات فورية لحظة بلحظة.',
    app1F1: 'مكتبة شاملة تتحدث يومياً بأعلى الجودات (4K / 1080p)',
    app1F2: 'محرك بحث فوري مع فلاتر النوع والسنة والتقييم',
    app1F3: 'اكتشاف كامل المواسم والحلقات وسيرفرات مشاهدة متعددة',
    app1F4: 'تكامل تلقائي مع مشغل زاغ بلاير بضغطة واحدة',
    app2Role: 'المشغل الاحترافي',
    app2Desc: 'مشغل وسائط مخصص مبني على محرك أندرويد الأصلي لفك تشفير الفيديو والبث المباشر بأعلى سرعة وبدون استهلاك للذاكرة أو البطارية.',
    app2F1: 'تقنية مانع التقطيع وتشغيل سلس حتى مع سرعات الإنترنت الضعيفة',
    app2F2: 'دعم كامل لصيغ HLS و M3U8 و MP4 ومسارات الترجمة المتعددة',
    app2F3: 'فائق الخفة (4.6 MB فقط) وموفر لاستهلاك البطارية والرام',
    app2F4: 'توافق تام مع أجهزة وشاشات Android TV و TV Box والريموت',
    btnDownloadApk: 'تحميل APK مباشر',
    btnQr: 'مسح QR',
    stepsTag: 'طريقة التثبيت',
    stepsTitle: 'ثلاث خطوات بسيطة لبدء المشاهدة',
    stepsDesc: 'التثبيت لا يستغرق أكثر من دقيقة واحدة على أي هاتف أو شاشة تعمل بنظام أندرويد.',
    step1Title: 'تحميل حزمتي التطبيق',
    step1Desc: 'اضغط على أزرار التحميل المباشر بالأعلى لتنزيل تطبيقي "زقزوقة TV" و "مشغل زاغ" على جهازك.',
    step2Title: 'السماح بالتثبيت لمرة واحدة',
    step2Desc: 'عند فتح الملف، اضغط على الإعدادات ثم فعّل "السماح بالتثبيت من هذا المصدر" لإكمال عملية التثبيت بأمان.',
    step3Title: 'افتح واستمتع بالمشاهدة',
    step3Desc: 'افتح تطبيق زقزوقة، اختر فيلمك أو مسلسلك أو قناتك المفضلة واضغط تشغيل وسيبدأ العرض فوراً بسلاسة.',
    featTag: 'لماذا زقزوقة TV؟',
    featTitle: 'أداء سينمائي متكامل مبني لأجلك',
    f1Title: 'سيرفرات فائقة السرعة',
    f1Desc: 'شبكة توزيع محتوى سحابية ذكية تضمن بدء تشغيل الفيديو في جزء من الثانية دون انتظار أو تحميل مسبق.',
    f2Title: 'جودة 4K و Full HD',
    f2Desc: 'نقاء بصري استثنائي وصوت مجسم محيطي مع إمكانية التبديل بين الجودات تلقائياً أو يدوياً.',
    f3Title: 'دعم كامل لشاشات التلفزيون',
    f3Desc: 'واجهة مهيأة تماماً للتنقل السلس بالريموت كنترول على شاشات Android TV وأجهزة TV Box.',
    f4Title: 'تحديثات مستمرة للمحتوى',
    f4Desc: 'إضافة أحدث حلقات المسلسلات المعروضة والأفلام الجديدة لحظة بلحظة وبكافة مواسمها وتصنيفاتها.',
    f5Title: 'خالٍ تماماً من الإعلانات',
    f5Desc: 'لا نوافذ منبثقة، لا إعلانات إجبارية مزعجة، تجربة مشاهدة نقية ومريحة تحترم خصوصيتك وراحتك.',
    f6Title: 'مجاني 100% بدون اشتراكات',
    f6Desc: 'استمتع بكافة محتويات المنظومة مجاناً بدون الحاجة لحسابات بنكية أو رسوم خفية أو قيود على الأجهزة.',
    faqTag: 'الأسئلة الشائعة',
    faqTitle: 'كل ما تود معرفته عن التطبيق والمشغل',
    q1: 'لماذا أحتاج لتحميل تطبيقين (زقزوقة TV ومشغل زاغ)؟',
    a1: 'تطبيق زقزوقة يمثل مكتبة العرض والبحث وإدارة المفضلة والمواسم، بينما مشغل زاغ هو المحرك المتخصص بفك التشفير والبث عالي الكفاءة. فصل المشغل يجعل التطبيق خفيفاً جداً ويضمن سرعة تشغيل استثنائية دون تهنيج أو استنزاف للبطارية.',
    q2: 'هل التطبيق يعمل على شاشات التلفزيون الذكية (Smart TV)؟',
    a2: 'نعم، التطبيقان متوافقان تماماً مع شاشات Android TV وأجهزة TV Box و Fire TV Stick، ويدعمان التحكم الكامل بالريموت كنترول وعرض المحتوى بدقة 4K.',
    q3: 'ماذا أفعل إذا ظهرت رسالة "تطبيق غير معروف" أثناء التثبيت؟',
    a3: 'هذا إجراء أمان قياسي في أندرويد عند تثبيت أي تطبيق بصيغة APK من خارج متجر جوجل بلاي. كل ما عليك هو الضغط على "الإعدادات" ثم تفعيل "السماح بالتثبيت من هذا المصدر"، فالتطبيق مفحوص وآمن تماماً.',
    q4: 'كيف يمكنني تحديث التطبيق عند صدور إصدارات جديدة؟',
    a4: 'يحتوي تطبيق زقزوقة على نظام تحديث تلقائي ينبهك فوراً عند توفر أي إصدار أحدث ويتيح لك التحديث بنقرة زر واحدة من داخل التطبيق دون فقدان مفضلتك أو بياناتك.',
    footerCopy: 'جميع الحقوق محفوظة © 2026 منظومة زقزوقة TV ومحرك زاغ بلاير الرسمي.',
    qrTitle: 'مسح رمز الاستجابة السريعة (QR)',
    qrDesc: 'امسح الرمز بكاميرا هاتفك أو شاشتك لبدء التحميل الفوري:',
    mobileBtnApp: 'تحميل زقزوقة TV',
    mobileBtnPlayer: 'مشغل زاغ'
  },
  en: {
    brandTag: 'All-in-One Cinema Streaming',
    navHome: 'Home',
    navApps: 'Applications',
    navHowTo: 'How to Install',
    navFeatures: 'Features',
    navFaq: 'FAQ',
    btnFastDownload: 'Direct Download',
    heroBadge: 'Official 2026 Release — 100% Safe & Ad-Free',
    heroTitle: 'Unlimited Cinema Streaming with <span class="highlight-brick">Zaqzouka TV</span> & <span class="highlight-gold">ZAG Player</span>',
    heroDesc: 'A complete media suite to stream thousands of exclusive movies, full series seasons, and live sports in crisp 4K Ultra HD with zero buffering and zero subscription fees.',
    heroBtnApp: 'Download Zaqzouka TV',
    heroBtnAppSub: 'v1.0.74 · Direct APK (53.4 MB)',
    heroBtnPlayer: 'Download ZAG Player',
    heroBtnPlayerSub: 'v1.1.28 · Core Engine (4.6 MB)',
    trustVerified: '100% Verified & Clean',
    trustSmartTv: 'Smart TV & Android TV Support',
    trustNoBuffer: 'Anti-Buffering Engine',
    tabHome: 'Home',
    tabSeries: 'Series & Seasons',
    tabLive: 'Live Sports',
    appsTag: 'Direct Downloads',
    appsTitle: 'Download Both Apps for Complete Experience',
    appsDesc: 'Zaqzouka TV provides the rich catalog, search, and season navigation, while ZAG Player provides hardware-accelerated video decoding with zero battery drain.',
    app1Role: 'Main Application',
    app1Desc: 'Explore the largest Arab and international library of movies, series, and live channels with organized seasons, full episodes, and real-time updates.',
    app1F1: 'Comprehensive catalog updated daily in 4K & FHD',
    app1F2: 'Instant search engine with genre, year, and rating filters',
    app1F3: 'Full seasons and multi-source streaming discovery',
    app1F4: 'One-click seamless integration with ZAG Player',
    app2Role: 'Hardware Player',
    app2Desc: 'Dedicated playback engine built on native Android ExoPlayer to stream HLS, M3U8, and MP4 formats with lowest memory footprint.',
    app2F1: 'Anti-buffering buffer engine optimized for all network speeds',
    app2F2: 'Full subtitle tracks, multi-audio, and aspect ratio controls',
    app2F3: 'Ultra lightweight (4.6 MB) with near-zero CPU and battery usage',
    app2F4: 'Native remote control support for Android TV and TV boxes',
    btnDownloadApk: 'Download Direct APK',
    btnQr: 'Scan QR',
    stepsTag: 'Installation Guide',
    stepsTitle: 'Three Simple Steps to Start Watching',
    stepsDesc: 'Installation takes less than one minute on any Android phone, tablet, or Smart TV.',
    step1Title: 'Download APK Packages',
    step1Desc: 'Click the direct download buttons above to download both Zaqzouka TV and ZAG Player APKs.',
    step2Title: 'Allow Unknown Sources',
    step2Desc: 'When prompted during install, tap Settings and enable "Install from this source" once.',
    step3Title: 'Open and Enjoy Streaming',
    step3Desc: 'Launch Zaqzouka TV, pick your favorite movie or series, and hit play for instant playback.',
    featTag: 'Why Zaqzouka TV?',
    featTitle: 'Engineered for Pure Cinema Performance',
    f1Title: 'Ultra-Fast Servers',
    f1Desc: 'Smart CDN edge caching ensures instant video playback with zero wait time.',
    f2Title: '4K & Full HD Streams',
    f2Desc: 'Crystal clear picture and surround audio with auto and manual bitrate options.',
    f3Title: 'Full Android TV Compatibility',
    f3Desc: 'Intuitive 10-foot interface navigation designed specifically for TV remotes.',
    f4Title: 'Instant Content Updates',
    f4Desc: 'New series episodes and blockbuster releases synced immediately upon global debut.',
    f5Title: '100% Ad-Free Experience',
    f5Desc: 'No annoying popups or forced banners, keeping your viewing experience undisturbed.',
    f6Title: 'Free Forever',
    f6Desc: 'No monthly subscriptions, no hidden paywalls, and no device quantity restrictions.',
    faqTag: 'FAQ',
    faqTitle: 'Frequently Asked Questions',
    q1: 'Why do I need to install two apps (Zaqzouka TV and ZAG Player)?',
    a1: 'Zaqzouka TV manages content discovery, catalog browsing, and episodes navigation. ZAG Player handles low-level hardware decoding. Decoupling them keeps the app extremely fast and responsive without memory bloat.',
    q2: 'Does it work on Android TV and Fire TV?',
    a2: 'Yes, both applications are fully tested and compatible with Android TV, TV boxes, and smart TVs with full remote navigation.',
    q3: 'What if Android warns about an unknown source?',
    a3: 'This is standard Android security for apps installed outside Google Play. Simply tap Settings and toggle Allow from this source. All binaries are verified and clean.',
    q4: 'How do updates work?',
    a4: 'Zaqzouka TV includes an in-app update detector that notifies you whenever a new build is released and updates with a single tap without resetting your favorites.',
    footerCopy: 'All rights reserved © 2026 Zaqzouka TV Network & ZAG Player Official.',
    qrTitle: 'Scan QR Code for Fast Download',
    qrDesc: 'Scan this code with your phone camera or TV to start direct download:',
    mobileBtnApp: 'Download Zaqzouka TV',
    mobileBtnPlayer: 'ZAG Player'
  }
};

function initLanguageSwitcher() {
  const btn = document.getElementById('lang-switch-btn');
  if (!btn) return;

  let currentLang = 'ar';

  btn.addEventListener('click', () => {
    currentLang = currentLang === 'ar' ? 'en' : 'ar';
    document.documentElement.lang = currentLang;
    document.documentElement.dir = currentLang === 'ar' ? 'rtl' : 'ltr';
    document.body.setAttribute('dir', currentLang === 'ar' ? 'rtl' : 'ltr');

    const label = btn.querySelector('.lang-label');
    if (label) label.textContent = currentLang === 'ar' ? 'EN' : 'عربي';

    const dict = i18n[currentLang];
    document.querySelectorAll('[data-i18n]').forEach(el => {
      const key = el.getAttribute('data-i18n');
      if (dict[key]) {
        el.innerHTML = dict[key];
      }
    });

    // Update active accordion max-heights after text resize
    document.querySelectorAll('.faq-item.active .faq-answer').forEach(ans => {
      ans.style.maxHeight = ans.scrollHeight + 'px';
    });
  });
}

/* -------------------------------------------------------------
   5. QR Code Modal
   ------------------------------------------------------------- */
function initQrModal() {
  const modal = document.getElementById('qr-modal');
  const closeBtn = document.querySelector('.modal-close-btn');
  const qrButtons = document.querySelectorAll('.btn-card-qr');
  const targetLabel = document.getElementById('qr-target-filename');
  const wrapper = document.getElementById('qr-canvas-wrapper');
  if (!modal || !wrapper) return;

  function renderQrCode(targetFilename) {
    const fullUrl = window.location.origin + window.location.pathname.replace(/\/+$/, '') + '/downloads/' + targetFilename;
    const qrApiUrl = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(fullUrl)}&color=181216&bgcolor=FFFFFF`;
    wrapper.innerHTML = `<img src="${qrApiUrl}" alt="QR Code" width="180" height="180" style="display:block; border-radius: 8px;">`;
  }

  qrButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const target = btn.getAttribute('data-qr-target') || 'zaqzouka-app.apk';
      if (targetLabel) targetLabel.textContent = target;
      renderQrCode(target);
      modal.classList.add('open');
    });
  });

  if (closeBtn) {
    closeBtn.addEventListener('click', () => modal.classList.remove('open'));
  }

  modal.addEventListener('click', e => {
    if (e.target === modal) modal.classList.remove('open');
  });
}
