/**
 * (c) 2026 Zaqzouka Network Architecture - All Rights Reserved.
 * Protected under International Copyright & Digital Rights Protection Treaties.
 * Unauthorized copying, reverse engineering, tampering, or distribution is strictly prohibited.
 */
(function(_0xroot, _0xdoc) {
  'use strict';

  const APP_APK_URL = 'https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.86-93/zaqzouka-1.0.86-93.apk';
  const PLAYER_APK_URL = 'https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.86-93/zag-player-1.1.37.apk';
  const TELEMETRY_API = 'https://dashboard-api-indol-eight.vercel.app/api/telemetry';

  // ── Anti-DevTools & Anti-Inspection Shield ──
  (function _0xshield() {
    try {
      _0xdoc.addEventListener('contextmenu', function(e) {
        e.preventDefault();
        return false;
      }, false);

      _0xdoc.addEventListener('keydown', function(e) {
        if (e.keyCode === 123) {
          e.preventDefault();
          return false;
        }
        if (e.ctrlKey && e.shiftKey && (e.keyCode === 73 || e.keyCode === 74 || e.keyCode === 67)) {
          e.preventDefault();
          return false;
        }
        if (e.ctrlKey && (e.keyCode === 85 || e.keyCode === 83)) {
          e.preventDefault();
          return false;
        }
      }, false);

      if (_0xroot.console) {
        _0xroot.console.clear();
        _0xroot.console.log(
          '%c[!] نظام حماية زقزوقة المعتمد 2026\nهذه الصفحة محمية بالكامل ضد التلاعب أو النسخ.',
          'color:#E5A958; font-size:15px; font-weight:600; background:#171116; padding:12px; border:1px solid #B94E3C; border-radius:8px;'
        );
      }

      setInterval(function() {
        (function() {
          try {
            return !function() {}['constructor']('debugger')();
          } catch (_) { return false; }
        })();
      }, 3000);
    } catch (_) {}
  })();

  // ── Encrypted Cloud Kill Switch & System Verifier ──
  (function _0xverifier() {
    const _0xkey = 126;
    const _0xraw = [
      [22, 10, 10, 14, 13, 68, 81, 81, 26, 31, 13, 22, 28, 17, 31, 12, 26, 83, 31, 14, 23, 83, 23, 16, 26, 17, 18, 83, 27, 23, 25, 22, 10, 80, 8, 27, 12, 29, 27, 18, 80, 31, 14, 14, 81, 31, 14, 23, 81, 13, 7, 13, 10, 27, 19, 33, 29, 17, 16, 10, 12, 17, 18],
      [22, 10, 10, 14, 13, 68, 81, 81, 19, 31, 26, 31, 12, 10, 8, 83, 26, 27, 24, 31, 11, 18, 10, 83, 12, 10, 26, 28, 80, 24, 23, 12, 27, 28, 31, 13, 27, 23, 17, 80, 29, 17, 19, 81, 13, 7, 13, 10, 27, 19, 33, 29, 17, 16, 10, 12, 17, 18, 80, 20, 13, 17, 16]
    ];
    const _0xurls = _0xraw.map(arr => arr.map(c => String.fromCharCode(c ^ _0xkey)).join(''));

    function _0xshowMaint(_0xmsg) {
      if (_0xdoc.getElementById('zq-maintenance-overlay')) return;
      _0xdoc.documentElement.style.overflow = 'hidden';
      _0xdoc.body.style.overflow = 'hidden';

      const _0xov = _0xdoc.createElement('div');
      _0xov.id = 'zq-maintenance-overlay';
      _0xov.style.cssText = 'position:fixed;inset:0;background:#0E0B0E;z-index:9999999;display:flex;flex-direction:column;align-items:center;justify-content:center;font-family:Cairo,system-ui,sans-serif;direction:rtl;padding:24px;box-sizing:border-box;';
      _0xov.innerHTML = `
        <div style="text-align:center;max-width:520px;background:#171116;border:1px solid rgba(185,78,60,0.42);border-radius:24px;padding:48px 32px;box-shadow:0 24px 64px rgba(0,0,0,0.85);">
          <div style="width:72px;height:72px;border-radius:9999px;background:rgba(185,78,60,0.14);border:1px solid #B94E3C;display:flex;align-items:center;justify-content:center;margin:0 auto 24px;">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#E5A958" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
              <polyline points="2 17 12 22 22 17"></polyline>
              <polyline points="2 12 12 17 22 12"></polyline>
            </svg>
          </div>
          <div style="display:inline-flex;align-items:center;gap:8px;padding:4px 12px;background:rgba(185,78,60,0.14);border:1px solid rgba(185,78,60,0.42);border-radius:9999px;margin-bottom:16px;">
            <span style="width:8px;height:8px;border-radius:9999px;background:#B94E3C;"></span>
            <span style="color:#E5A958;font-size:12px;font-weight:600;">توقف مؤقت للصيانة</span>
          </div>
          <h1 style="color:#FFFBF5;font-size:24px;font-weight:600;margin:0 0 12px;">الخدمة متوقفة مؤقتاً</h1>
          <p style="color:#D4C8CE;font-size:15px;line-height:1.7;margin:0 0 24px;">${_0xmsg || 'نعمل حالياً على تحديثات شاملة وتطوير خوادم البث لضمان أفضل تجربة مشاهدة. سنعود للعمل قريباً جداً.'}</p>
          <div style="color:#9D8E96;font-size:12px;">منظومة زقزوقة TV</div>
        </div>
      `;
      if (_0xdoc.body) {
        _0xdoc.body.appendChild(_0xov);
      } else {
        _0xdoc.addEventListener('DOMContentLoaded', () => _0xdoc.body.appendChild(_0xov));
      }
    }

    async function _0xchk() {
      for (const _0xu of _0xurls) {
        try {
          const _0xr = await fetch(_0xu);
          if (_0xr.ok) {
            const _0xd = await _0xr.json();
            if (!_0xd) continue;
            const _0xs = (_0xd.status || 'active').toLowerCase();
            const _0xc = _0xd.components || {};
            const _0xon = _0xc.download_site_enabled !== false && _0xc.website_enabled !== false;

            if (_0xs === 'frozen' || _0xs === 'wiped' || !_0xon) {
              _0xshowMaint(_0xd.message);
            } else {
              const _0xel = _0xdoc.getElementById('zq-maintenance-overlay');
              if (_0xel) _0xel.remove();
              _0xdoc.documentElement.style.overflow = '';
              _0xdoc.body.style.overflow = '';
            }
            return;
          }
        } catch (_) {}
      }
    }

    _0xchk();
    setInterval(_0xchk, 120000);
  })();

  // ── 5 Portrait Interactive Motion Scenes (Modeled on zaq-motion.mp4) ──
  const MOTION_SCENES = {
    ar: [
      {
        fig: 'FIG.01 · HOME',
        code: 'ZAQZOUKA · 01 / 05',
        label: 'الرئيسية',
        headline: 'واجهة <span class="accent-terracotta">دافئة</span> ومتعة فورية.',
        subtitle: 'اكتشف مكتبة الأفلام والمسلسلات والملخصات وبثوث الكورة بتصفح سريع في ألوان زقزوقة المريحة للعين دون اشتراكات أو نوافذ مزعجة.',
        floatTag: 'أفلام ومسلسلات 2026',
        nextLabel: 'المشهد التالي · الأفلام'
      },
      {
        fig: 'FIG.02 · MOVIES',
        code: 'ZAQZOUKA · 02 / 05',
        label: 'الأفلام',
        headline: 'أحدث <span class="accent-gold">الأفلام</span> في جيبك.',
        subtitle: 'أفلام عربية وأجنبية مترجمة بأحدث الإصدارات مع جلب ذكي لأكثر من مصدر وجودات متعددة تناسب سرعة اتصالك.',
        floatTag: 'إصدارات السينما المترجمة',
        nextLabel: 'المشهد التالي · المسلسلات'
      },
      {
        fig: 'FIG.03 · SERIES',
        code: 'ZAQZOUKA · 03 / 05',
        label: 'المسلسلات',
        headline: 'مواسم <span class="accent-terracotta">مكتملة</span> بلا حلقات ناقصة.',
        subtitle: 'تصفح الدراما العربية والتركية والآسيوية مع جمع كافة حلقات الموسم مرتبة ومتابعة آخر حلقة توقفت عندها.',
        floatTag: 'عربي · تركي · آسيوي',
        nextLabel: 'المشهد التالي · الملخصات'
      },
      {
        fig: 'FIG.04 · RECAPS',
        code: 'ZAQZOUKA · 04 / 05',
        label: 'ملخصات',
        headline: 'ملخصات <span class="accent-gold">السينما</span> بضغطة واحدة.',
        subtitle: 'مكتبة تضم مئات ملخصات الأفلام والمسلسلات من أشهر قنوات الملخصات العربية جاهزة للتشغيل الفوري عبر مشغل زاغ.',
        floatTag: 'أكثر من 670 ملخص جاهز',
        nextLabel: 'المشهد التالي · بثوث الكورة'
      },
      {
        fig: 'FIG.05 · SPORTS',
        code: 'ZAQZOUKA · 05 / 05',
        label: 'بثوث الكورة',
        headline: 'المباريات الحية <span class="accent-terracotta">بدون تقطيع</span>.',
        subtitle: 'جدول المباريات اليومي وحالة البث المباشر وقنوات الرياضة العربية مع تعليق مباشر وملخصات الأهداف.',
        floatTag: 'جدول المباريات والبث الحي',
        nextLabel: 'العودة · الرئيسية'
      }
    ],
    en: [
      {
        fig: 'FIG.01 · HOME',
        code: 'ZAQZOUKA · 01 / 05',
        label: 'Home',
        headline: 'Warm interface, <span class="accent-terracotta">instant</span> cinema.',
        subtitle: 'Explore Arabic and global films, series seasons, recaps, and live football matches in our calm Paper Sand interface with zero subscriptions.',
        floatTag: '2026 Cinema & Series',
        nextLabel: 'Next Scene · Movies'
      },
      {
        fig: 'FIG.02 · MOVIES',
        code: 'ZAQZOUKA · 02 / 05',
        label: 'Movies',
        headline: 'Latest <span class="accent-gold">films</span> ready to stream.',
        subtitle: 'Browse freshly released Arabic and subtitled global features with multi source mirrors and flexible quality playback.',
        floatTag: 'Clean 2026 Film Vault',
        nextLabel: 'Next Scene · Series'
      },
      {
        fig: 'FIG.03 · SERIES',
        code: 'ZAQZOUKA · 03 / 05',
        label: 'Series',
        headline: 'Complete <span class="accent-terracotta">seasons</span> sorted cleanly.',
        subtitle: 'Arabic, Turkish, and Asian drama seasons aggregated accurately in order so no episode goes missing.',
        floatTag: 'Arabic · Turkish · Asian',
        nextLabel: 'Next Scene · Recaps'
      },
      {
        fig: 'FIG.04 · RECAPS',
        code: 'ZAQZOUKA · 04 / 05',
        label: 'Recaps',
        headline: 'Story <span class="accent-gold">recaps</span> in seconds.',
        subtitle: 'Curated hundreds of fast cinema recap channels indexed with instant search and direct ZAG Player playback.',
        floatTag: '670+ Curated Recaps',
        nextLabel: 'Next Scene · Football'
      },
      {
        fig: 'FIG.05 · SPORTS',
        code: 'ZAQZOUKA · 05 / 05',
        label: 'Live Sports',
        headline: 'Live football <span class="accent-terracotta">one tap</span> away.',
        subtitle: 'Daily match fixtures, live channel status, Arabic commentary info, and crisp stable playback.',
        floatTag: 'Daily Live Matches Hub',
        nextLabel: 'Back to Home'
      }
    ]
  };

  // ── Complete Bilingual Dictionary ──
  const I18N = {
    ar: {
      skipLink: 'تخطي إلى المحتوى الرئيسي',
      navCta: 'تحميل التطبيق',
      heroPrimaryCta: 'تحميل زقزوقة TV ومشغل زاغ',
      heroBadge: 'عرض تفاعلي حي · واجهة زقزوقة 2026',
      wordmarkText: 'زقزوقة',
      sc1Label: 'الرئيسية',
      sc1Headline: 'واجهة <span class="accent-terracotta">دافئة</span> ومتعة فورية.',
      sc1Sub: 'اكتشف مكتبة الأفلام والمسلسلات والملخصات وبثوث الكورة بتصفح سريع في ألوان زقزوقة المريحة للعين دون اشتراكات أو نوافذ مزعجة.',
      sc1Tag: 'أفلام ومسلسلات 2026',
      dlStartTitle: 'بدأ تحضير التطبيق والمشغل على جهازك',
      dlStartSub: 'ثبّت تطبيق زقزوقة TV أولاً ثم مشغل زاغ عند اكتمال التنزيل.',
      separateLabel: 'تحميل مباشر:',
      appOnly: 'تطبيق زقزوقة TV فقط',
      playerOnly: 'مشغل زاغ فقط',
      faqKicker: 'إجابات شاملة',
      faqTitle: 'الأسئلة الشائعة',
      q1: 'لماذا أحتاج لتثبيت تطبيق زقزوقة TV ومشغل زاغ بلاير معاً؟',
      a1: 'تطبيق زقزوقة TV مخصص لتنظيم الأفلام والمسلسلات والملخصات وبثوث الكورة، بينما يعمل مشغل زاغ كمحرك مادي منفصل لفك التشفير، مما يضمن تشغيلاً سريعاً بدون بطء الهاتف.',
      q2: 'هل يعمل التطبيق على شاشات Smart TV و TV Box؟',
      a2: 'نعم، يعمل كلا التطبيقين على شاشات Android TV وأجهزة Fire TV Stick وصناديق TV Box مع دعم التنقل بالريموت كنترول وتشغيل الجودات حتى 4K.',
      q3: 'ماذا أفعل عند ظهور رسالة السماح بالتثبيت من مصدر غير معروف؟',
      a3: 'هذه خطوة أمان روتينية في أندرويد عند تحميل APK من المتصفح مباشرة. اختر الإعدادات وفعل السماح من هذا المصدر وسيكتمل التثبيت فوراً.',
      q4: 'كيف أنقل الملفات بسرعة إلى التلفزيون الذكي؟',
      a4: 'يمكنك فتح zaafilm.com مباشرة من متصفح التلفزيون أو تطبيق Downloader، أو مسح رمز QR وإرسال الملفات عبر تطبيق Send Files to TV.',
      q5: 'هل يوجد أي اشتراك شهري أو تسجيل حساب؟',
      a5: 'لا، المنظومة مجانية بالكامل ولا تطلب إنشاء حساب أو إدخال بريد إلكتروني أو بطاقة دفع، فقط ثبت التطبيقين وابدأ المشاهدة.',
      q6: 'هل قسم بثوث الكورة يغطي المباريات المباشرة اليومية؟',
      a6: 'نعم، يعرض قسم بثوث الكورة جدول المباريات اليومي وحالة البث الحي والقنوات الرياضية مع ملخصات الأهداف والمواجهات.',
      q7: 'كيف أحصل على التحديثات المستقبلية بدون فقدان المفضلة؟',
      a7: 'يفحص زقزوقة TV توفر الإصدارات الأحدث تلقائياً، وعند صدور تحديث يعرض لك نافذة تحديث مباشر من داخل التطبيق مع بقاء مفضلتك كاملة.',
      footerCopy: 'جميع الحقوق محفوظة 2026 منظومة زقزوقة TV ومشغل زاغ بلاير.',
      legalPrivacy: 'سياسة الخصوصية',
      legalTerms: 'شروط الاستخدام'
    },
    en: {
      skipLink: 'Skip to main content',
      navCta: 'Download',
      heroPrimaryCta: 'Download Zaqzouka TV & ZAG Player',
      heroBadge: 'Interactive Showcase · Zaqzouka 2026 UI',
      wordmarkText: 'Zaqzouka',
      sc1Label: 'Home',
      sc1Headline: 'Warm interface, <span class="accent-terracotta">instant</span> cinema.',
      sc1Sub: 'Explore Arabic and global films, series seasons, recaps, and live football matches in our calm Paper Sand interface with zero subscriptions.',
      sc1Tag: '2026 Cinema & Series',
      dlStartTitle: 'Preparing Zaqzouka TV & ZAG Player downloads',
      dlStartSub: 'Install Zaqzouka TV first, then install ZAG Player once downloads finish.',
      separateLabel: 'Direct APK:',
      appOnly: 'Zaqzouka TV Only',
      playerOnly: 'ZAG Player Only',
      faqKicker: 'Clear Answers',
      faqTitle: 'Frequently Asked Questions',
      q1: 'Why install Zaqzouka TV together with ZAG Player?',
      a1: 'Zaqzouka TV focuses on catalog browsing across movies, series, recaps, and football while ZAG Player operates as a lightweight hardware decoder for smooth streams without lag.',
      q2: 'Does the suite run on Smart TVs and TV Boxes?',
      a2: 'Yes. Both apps work on Android TV, Fire TV Stick, and TV Boxes with full remote navigation up to 4K.',
      q3: 'What if Android prompts to allow unknown sources?',
      a3: 'That is standard Android security for APKs outside Play Store. Open Settings on the prompt and allow this browser to continue installation.',
      q4: 'How do I send APK files quickly to a TV?',
      a4: 'Open zaafilm.com directly in your TV browser or Downloader app, or scan the QR dialog and transfer via Send Files to TV.',
      q5: 'Is an account or monthly subscription required?',
      a5: 'No. Both packages are completely free with zero signup, email entry, or subscriptions.',
      q6: 'Does the Football hub cover daily matches live?',
      a6: 'Yes. The Football tab lists daily matches, live channel links, commentary info, and goal recaps.',
      q7: 'How do future updates arrive without losing favorites?',
      a7: 'Zaqzouka TV checks for updates automatically and installs cleanly while preserving all saved favorites.',
      footerCopy: 'All rights reserved 2026 Zaqzouka TV Ecosystem & ZAG Player.',
      legalPrivacy: 'Privacy Policy',
      legalTerms: 'Terms of Use'
    }
  };

  const LEGAL_CONTENT = {
    ar: {
      privacyTitle: 'سياسة الخصوصية',
      privacyBody: '<p>نحترم خصوصيتك بالكامل في موقع ZaaFilm ومنظومة زقزوقة TV. لا يتطلب استخدام التطبيق أو تحميله إنشاء حساب أو جمع أي بيانات شخصية.</p><p>نستخدم مؤشرات تقنية مجهولة الهوية فقط لقياس استقرار روابط التحميل دون تتبع هويتك.</p>',
      termsTitle: 'شروط الاستخدام',
      termsBody: '<p>حزم زقزوقة TV ومشغل زاغ بلاير متوفرة للاستخدام الشخصي غير التجاري عبر الروابط الرسمية على zaafilm.com.</p><p>يُمنع إعادة تعديل ملفات الـ APK أو توزيعها بنسخ معدلة عبر مصادر غير رسمية.</p>'
    },
    en: {
      privacyTitle: 'Privacy Policy',
      privacyBody: '<p>ZaaFilm and Zaqzouka TV respect your privacy completely. Neither downloading nor opening the suite collects personal registration data.</p><p>Only anonymous mirror reliability telemetry is recorded without user identity tracking.</p>',
      termsTitle: 'Terms of Use',
      termsBody: '<p>Zaqzouka TV and ZAG Player builds are licensed free for personal non commercial viewing strictly via zaafilm.com.</p><p>Repackaging or altering official APK binaries is prohibited.</p>'
    }
  };

  let currentLang = 'ar';
  let activeSceneIdx = 0;
  let isAutoplayOn = true;
  let renderSceneFn = null;

  const prefersReducedMotion = () =>
    _0xroot.matchMedia && _0xroot.matchMedia('(prefers-reduced-motion: reduce)').matches;

  _0xdoc.addEventListener('DOMContentLoaded', function() {
    initDustCanvas();
    initMotionTheater();
    initNavigationAndDrawer();
    initScrollRevealsAndParallax();
    initFaqAccordion();
    initLanguageSwitcher();
    initBundleDownloader();
    initModals();
    initTelemetry();
  });

  // ── 1. Kinetic Portrait Phone Choreography Engine ──
  function initMotionTheater() {
    const stageFrame = _0xdoc.getElementById('motion-stage-frame');
    const phoneRig = _0xdoc.getElementById('phone-rig');
    const floorShadow = _0xdoc.getElementById('phone-floor-shadow');
    const glintEl = _0xdoc.getElementById('screen-glint');
    const slides = _0xdoc.querySelectorAll('.phone-screen-slide');
    const codeBadge = _0xdoc.getElementById('scene-code-badge');
    const activeLabel = _0xdoc.getElementById('scene-active-label');
    const copyStage = _0xdoc.getElementById('scene-copy-stage');
    const headlineEl = _0xdoc.getElementById('scene-headline');
    const subtitleEl = _0xdoc.getElementById('scene-subtitle');
    const floatTag = _0xdoc.getElementById('phone-float-tag');
    const floatTagText = _0xdoc.getElementById('float-tag-text');
    const wordmarkEl = _0xdoc.querySelector('.zaq-wordmark');

    if (!phoneRig || !slides.length) return;

    let timer = null;
    const INTERVAL_MS = 2800;
    const total = slides.length;

    function applyScene(idx, animate) {
      const prevIdx = activeSceneIdx;
      activeSceneIdx = ((idx % total) + total) % total;
      const scenes = MOTION_SCENES[currentLang] || MOTION_SCENES.ar;
      const scene = scenes[activeSceneIdx];

      if (stageFrame) stageFrame.setAttribute('data-active-scene', String(activeSceneIdx));
      if (floorShadow) floorShadow.setAttribute('data-scene', String(activeSceneIdx));

      // Expressive Left/Right Swing Choreography + Glass Specular Glint
      if (animate && !prefersReducedMotion()) {
        phoneRig.classList.add('is-gliding');
        setTimeout(() => {
          phoneRig.setAttribute('data-scene', String(activeSceneIdx));
          phoneRig.classList.remove('is-gliding');
        }, 120);

        if (glintEl) {
          glintEl.classList.remove('is-flashing');
          void glintEl.offsetWidth;
          glintEl.classList.add('is-flashing');
        }

        if (wordmarkEl) {
          wordmarkEl.classList.add('is-pulsing');
          setTimeout(() => wordmarkEl.classList.remove('is-pulsing'), 480);
        }
      } else {
        phoneRig.setAttribute('data-scene', String(activeSceneIdx));
      }

      // Cross-slide upright 1080x2400 portrait iPhone screens
      slides.forEach((img, i) => {
        img.classList.remove('is-exiting');
        if (i === prevIdx && prevIdx !== activeSceneIdx) {
          img.classList.remove('is-active');
          img.classList.add('is-exiting');
          setTimeout(() => img.classList.remove('is-exiting'), 560);
        } else if (i === activeSceneIdx) {
          img.classList.add('is-active');
        } else {
          img.classList.remove('is-active');
        }
      });

      if (codeBadge) codeBadge.textContent = scene.code;
      if (activeLabel) activeLabel.textContent = scene.label;

      if (floatTag && floatTagText) {
        if (animate && !prefersReducedMotion()) {
          floatTag.classList.add('is-pop');
          setTimeout(() => {
            floatTagText.textContent = scene.floatTag;
            floatTag.classList.remove('is-pop');
          }, 160);
        } else {
          floatTagText.textContent = scene.floatTag;
        }
      }

      if (copyStage && headlineEl && subtitleEl) {
        if (animate && !prefersReducedMotion()) {
          copyStage.classList.add('is-switching');
          setTimeout(() => {
            headlineEl.innerHTML = scene.headline;
            subtitleEl.textContent = scene.subtitle;
            copyStage.classList.remove('is-switching');
          }, 170);
        } else {
          headlineEl.innerHTML = scene.headline;
          subtitleEl.textContent = scene.subtitle;
        }
      }
    }

    function scheduleNext() {
      clearTimeout(timer);
      if (!isAutoplayOn || prefersReducedMotion()) return;
      timer = setTimeout(() => {
        applyScene(activeSceneIdx + 1, true);
        scheduleNext();
      }, INTERVAL_MS);
    }

    // Tap or Enter/Space on iPhone rig advances immediately
    phoneRig.addEventListener('click', () => {
      applyScene(activeSceneIdx + 1, true);
      scheduleNext();
    });
    phoneRig.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        applyScene(activeSceneIdx + 1, true);
        scheduleNext();
      }
    });

    // Expose scene refresh for language switcher
    renderSceneFn = function() {
      applyScene(activeSceneIdx, false);
    };

    if (prefersReducedMotion()) {
      isAutoplayOn = false;
    }

    applyScene(0, false);
    if (isAutoplayOn) scheduleNext();

    _0xdoc.addEventListener('visibilitychange', () => {
      if (_0xdoc.hidden) {
        clearTimeout(timer);
      } else if (isAutoplayOn) {
        scheduleNext();
      }
    });
  }

  // ── 2. Ambient Cinema Gold & Paper Dust Canvas ──
  function initDustCanvas() {
    const canvas = _0xdoc.getElementById('cinema-dust-canvas');
    if (!canvas || prefersReducedMotion()) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let width = 0;
    let height = 0;
    let rafId = null;
    const motes = [];
    const MOTE_COUNT = 32;

    function resize() {
      const dpr = Math.min(_0xroot.devicePixelRatio || 1, 1.5);
      width = _0xroot.innerWidth;
      height = _0xroot.innerHeight;
      canvas.width = Math.floor(width * dpr);
      canvas.height = Math.floor(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    }

    function seed() {
      motes.length = 0;
      for (let i = 0; i < MOTE_COUNT; i++) {
        motes.push({
          x: Math.random() * width,
          y: Math.random() * height,
          r: 0.9 + Math.random() * 1.8,
          vx: -0.18 - Math.random() * 0.25,
          vy: -0.05 + Math.random() * 0.14,
          a: 0.14 + Math.random() * 0.34,
          gold: i % 2 === 0
        });
      }
    }

    function draw() {
      ctx.clearRect(0, 0, width, height);
      for (let i = 0; i < motes.length; i++) {
        const m = motes[i];
        m.x += m.vx;
        m.y += m.vy;
        if (m.x < -10) m.x = width + 10;
        if (m.y < -10) m.y = height + 10;
        if (m.y > height + 10) m.y = -10;

        ctx.beginPath();
        ctx.arc(m.x, m.y, m.r, 0, Math.PI * 2);
        ctx.fillStyle = m.gold
          ? `rgba(229, 169, 88, ${m.a})`
          : `rgba(185, 78, 60, ${m.a * 0.85})`;
        ctx.fill();
      }
      rafId = _0xroot.requestAnimationFrame(draw);
    }

    resize();
    seed();
    draw();

    let resizeTimer = null;
    _0xroot.addEventListener('resize', () => {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(() => {
        resize();
        seed();
      }, 160);
    });

    _0xdoc.addEventListener('visibilitychange', () => {
      if (_0xdoc.hidden && rafId) {
        _0xroot.cancelAnimationFrame(rafId);
        rafId = null;
      } else if (!_0xdoc.hidden && !rafId) {
        draw();
      }
    });
  }

  // ── 3. Mobile Bottom Bar Visibility After Hero ──
  function initNavigationAndDrawer() {
    const mobileBar = _0xdoc.getElementById('mobile-bottom-bar');
    const hero = _0xdoc.getElementById('hero');

    if (hero && mobileBar && 'IntersectionObserver' in _0xroot) {
      const barObserver = new IntersectionObserver(entries => {
        entries.forEach(entry => {
          const pastHero = !entry.isIntersecting && entry.boundingClientRect.top < 0;
          if (pastHero) {
            mobileBar.hidden = false;
            _0xroot.requestAnimationFrame(() => mobileBar.classList.add('is-shown'));
          } else {
            mobileBar.classList.remove('is-shown');
          }
        });
      }, { threshold: 0.08 });
      barObserver.observe(hero);
    }
  }

  // ── 4. Single rAF-Throttled Scroll Parallax & IntersectionObserver Reveals ──
  function initScrollRevealsAndParallax() {
    const revealEls = _0xdoc.querySelectorAll('.reveal-section');
    if ('IntersectionObserver' in _0xroot && !prefersReducedMotion()) {
      const obs = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-revealed');
            observer.unobserve(entry.target);
          }
        });
      }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' });
      revealEls.forEach(el => obs.observe(el));
    } else {
      revealEls.forEach(el => el.classList.add('is-revealed'));
    }

    const header = _0xdoc.getElementById('site-header');
    const hero = _0xdoc.getElementById('hero');
    const artImg = _0xdoc.getElementById('hero-art-img');
    const reduced = prefersReducedMotion();
    let ticking = false;

    function updateFrame() {
      ticking = false;
      const y = _0xroot.scrollY || 0;
      if (header) header.classList.toggle('scrolled', y > 40);

      if (artImg && hero && !reduced) {
        const heroH = hero.offsetHeight || 720;
        if (y <= heroH + 120) {
          const shift = Math.min(36, (y / heroH) * 36);
          artImg.style.transform = `translate3d(0, ${-shift.toFixed(1)}px, 0) scaleX(-1) scale(1.05)`;
        }
      }
    }

    _0xroot.addEventListener('scroll', () => {
      if (!ticking) {
        ticking = true;
        _0xroot.requestAnimationFrame(updateFrame);
      }
    }, { passive: true });

    updateFrame();
  }

  // ── 5. FAQ Accordion ──
  function initFaqAccordion() {
    const items = _0xdoc.querySelectorAll('.faq-item');
    if (!items.length) return;

    items.forEach(item => {
      const trigger = item.querySelector('.faq-trigger');
      const panel = item.querySelector('.faq-panel');
      if (!trigger || !panel) return;

      trigger.addEventListener('click', () => {
        const wasOpen = item.classList.contains('is-open');

        items.forEach(other => {
          const ot = other.querySelector('.faq-trigger');
          const op = other.querySelector('.faq-panel');
          other.classList.remove('is-open');
          if (ot) ot.setAttribute('aria-expanded', 'false');
          if (op) op.hidden = true;
        });

        if (!wasOpen) {
          panel.hidden = false;
          trigger.setAttribute('aria-expanded', 'true');
          _0xroot.requestAnimationFrame(() => item.classList.add('is-open'));
        }
      });
    });
  }

  // ── 7. Bilingual Language Switcher ──
  function initLanguageSwitcher() {
    const btn = _0xdoc.getElementById('lang-switch-btn');
    if (!btn) return;

    btn.addEventListener('click', () => {
      currentLang = currentLang === 'ar' ? 'en' : 'ar';
      const dict = I18N[currentLang];
      const isAr = currentLang === 'ar';

      _0xdoc.documentElement.lang = currentLang;
      _0xdoc.documentElement.dir = isAr ? 'rtl' : 'ltr';
      _0xdoc.body.setAttribute('dir', isAr ? 'rtl' : 'ltr');

      const lbl = btn.querySelector('.lang-label');
      if (lbl) lbl.textContent = isAr ? 'EN' : 'عربي';

      _0xdoc.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (dict[key]) el.innerHTML = dict[key];
      });

      if (typeof renderSceneFn === 'function') {
        renderSceneFn();
      }
      if (typeof _0xroot.__rebuildTagline === 'function' && dict.taglineText) {
        _0xroot.__rebuildTagline(dict.taglineText);
      }
    });
  }

  // ── 8. Unified Master Downloader ──
  function initBundleDownloader() {
    const progressCard = _0xdoc.getElementById('download-progress-card');
    const skelRow = _0xdoc.getElementById('progress-skeleton');
    const readyRow = _0xdoc.getElementById('progress-ready');

    function triggerDownload(url, filename) {
      const a = _0xdoc.createElement('a');
      a.href = url;
      a.setAttribute('download', filename);
      a.style.display = 'none';
      _0xdoc.body.appendChild(a);
      a.click();
      setTimeout(() => {
        try { _0xdoc.body.removeChild(a); } catch (_) {}
      }, 400);
    }

    function handleBundleClick(e) {
      if (e) e.preventDefault();

      if (progressCard) {
        progressCard.hidden = false;
        if (skelRow) skelRow.hidden = false;
        if (readyRow) readyRow.hidden = true;
        setTimeout(() => {
          if (skelRow) skelRow.hidden = true;
          if (readyRow) readyRow.hidden = false;
        }, 420);
      }

      const sourceBtn = e && e.currentTarget ? e.currentTarget : null;
      if (sourceBtn && sourceBtn.id !== 'heroBundleBtn') {
        const labelSpan = sourceBtn.querySelector('[data-i18n]');
        if (labelSpan) {
          const prevKey = labelSpan.getAttribute('data-i18n');
          labelSpan.textContent = currentLang === 'ar'
            ? 'بدأ تنزيل التطبيق والمشغل...'
            : 'Downloading App & Player...';
          setTimeout(() => {
            labelSpan.textContent = I18N[currentLang][prevKey] || labelSpan.textContent;
          }, 3200);
        }
      }

      triggerDownload(APP_APK_URL, 'zaqzouka-1.0.86-93.apk');
      setTimeout(() => {
        triggerDownload(PLAYER_APK_URL, 'zag-player-1.1.37.apk');
      }, 600);

      try {
        fetch(`${TELEMETRY_API}?route=landing/download`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            target: 'bundle',
            referrer: _0xdoc.referrer || 'direct',
            href: 'downloads/bundle'
          }),
          keepalive: true
        }).catch(() => {});
      } catch (_) {}
    }

    ['heroBundleBtn', 'mobileBundleBtn', 'navDownloadBtn'].forEach(id => {
      const btn = _0xdoc.getElementById(id);
      if (btn) btn.addEventListener('click', handleBundleClick);
    });
  }

  // ── 9. Legal Modal (Privacy & Terms) ──
  function initModals() {
    const legalModal = _0xdoc.getElementById('legal-modal');
    const legalCloseBtn = _0xdoc.getElementById('legal-close-btn');
    const legalTitle = _0xdoc.getElementById('legal-modal-title');
    const legalBody = _0xdoc.getElementById('legal-modal-body');

    function openModal(modal) {
      if (!modal) return;
      modal.hidden = false;
      _0xdoc.body.style.overflow = 'hidden';
      _0xroot.requestAnimationFrame(() => modal.classList.add('is-open'));
    }

    function closeModal(modal) {
      if (!modal) return;
      modal.classList.remove('is-open');
      _0xdoc.body.style.overflow = '';
      setTimeout(() => {
        if (!modal.classList.contains('is-open')) modal.hidden = true;
      }, 320);
    }

    if (legalModal && legalTitle && legalBody) {
      _0xdoc.querySelectorAll('.legal-modal-trigger').forEach(btn => {
        btn.addEventListener('click', () => {
          const kind = btn.getAttribute('data-legal') || 'privacy';
          const copy = LEGAL_CONTENT[currentLang] || LEGAL_CONTENT.ar;
          legalTitle.textContent = kind === 'terms' ? copy.termsTitle : copy.privacyTitle;
          legalBody.innerHTML = kind === 'terms' ? copy.termsBody : copy.privacyBody;
          openModal(legalModal);
        });
      });

      if (legalCloseBtn) legalCloseBtn.addEventListener('click', () => closeModal(legalModal));
      legalModal.addEventListener('click', e => {
        if (e.target === legalModal) closeModal(legalModal);
      });
    }

    _0xdoc.addEventListener('keydown', e => {
      if (e.key === 'Escape') {
        if (legalModal && legalModal.classList.contains('is-open')) closeModal(legalModal);
      }
    });
  }

  // ── 10. Download Hub Telemetry ──
  function initTelemetry() {
    const ref = _0xdoc.referrer || '';
    let cleanRef = 'مباشر (Direct)';
    if (ref) {
      try {
        const host = new URL(ref).hostname.toLowerCase();
        if (host.includes('google')) cleanRef = 'جوجل (Google Search)';
        else if (host.includes('t.me') || host.includes('telegram')) cleanRef = 'تيليجرام (Telegram)';
        else if (host.includes('facebook') || host.includes('fb.com')) cleanRef = 'فيسبوك (Facebook)';
        else if (host.includes('instagram')) cleanRef = 'إنستغرام (Instagram)';
        else if (host.includes('tiktok')) cleanRef = 'تيك توك (TikTok)';
        else if (host.includes('youtube') || host.includes('youtu.be')) cleanRef = 'يوتيوب (YouTube)';
        else if (host.includes('twitter') || host.includes('x.com')) cleanRef = 'تويتر (X / Twitter)';
        else if (host.includes('whatsapp') || host.includes('wa.me')) cleanRef = 'واتساب (WhatsApp)';
        else cleanRef = host;
      } catch (_) {
        cleanRef = ref.substring(0, 40);
      }
    }

    const ua = navigator.userAgent;
    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua);
    const deviceType = isMobile ? 'موبايل' : 'كمبيوتر / مكتبي';
    let browser = 'Chrome';
    if (ua.includes('Firefox')) browser = 'Firefox';
    else if (ua.includes('Safari') && !ua.includes('Chrome')) browser = 'Safari';
    else if (ua.includes('Edge')) browser = 'Edge';

    fetch(`${TELEMETRY_API}?route=landing/visit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        referrer: cleanRef,
        rawReferrer: ref,
        device: deviceType,
        browser: browser,
        screen: `${_0xroot.screen.width}x${_0xroot.screen.height}`,
        language: navigator.language || 'ar',
        url: _0xroot.location.href
      }),
      keepalive: true
    }).catch(() => {});

    _0xdoc.addEventListener('click', e => {
      const link = e.target.closest('a[href*=".apk"]');
      if (!link) return;
      const href = link.getAttribute('href') || '';
      const target = link.getAttribute('data-apk') || (href.includes('zag-player') ? 'player' : 'app');

      fetch(`${TELEMETRY_API}?route=landing/download`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          target: target,
          referrer: cleanRef,
          href: href
        }),
        keepalive: true
      }).catch(() => {});
    });
  }
})(window, document);
