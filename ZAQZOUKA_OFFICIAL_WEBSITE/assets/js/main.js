/**
 * (c) 2026 Zaqzouka Network Architecture - All Rights Reserved.
 * Protected under International Copyright & Digital Rights Protection Treaties.
 * Unauthorized copying, reverse engineering, tampering, or distribution is strictly prohibited.
 */
(function(_0xroot, _0xdoc) {
  'use strict';

  // ── Anti-DevTools & Anti-Inspection Shield ──
  (function _0xshield() {
    try {
      // Disable Context Menu (Right Click)
      _0xdoc.addEventListener('contextmenu', function(e) {
        e.preventDefault();
        return false;
      }, false);

      // Disable DevTools Key Combinations
      _0xdoc.addEventListener('keydown', function(e) {
        // F12
        if (e.keyCode === 123) {
          e.preventDefault();
          return false;
        }
        // Ctrl+Shift+I, Ctrl+Shift+J, Ctrl+Shift+C
        if (e.ctrlKey && e.shiftKey && (e.keyCode === 73 || e.keyCode === 74 || e.keyCode === 67)) {
          e.preventDefault();
          return false;
        }
        // Ctrl+U (View Source), Ctrl+S (Save)
        if (e.ctrlKey && (e.keyCode === 85 || e.keyCode === 83)) {
          e.preventDefault();
          return false;
        }
      }, false);

      // Console Warning Banner
      const _0xcw = function() {
        if (_0xroot.console) {
          _0xroot.console.clear();
          _0xroot.console.log(
            '%c[!] نظام حماية زقزوقة المعتمد 2026\nهذه الصفحة مشفرة ومحمية بالكامل ضد الهندسة العكسية أو التلاعب.',
            'color:#E5A958; font-size:16px; font-weight:bold; background:#141014; padding:12px; border:2px solid #B94E3C; border-radius:8px;'
          );
        }
      };
      _0xcw();

      // Anti-Debugging Guard (Pauses execution if DevTools is force-opened)
      setInterval(function() {
        (function() {
          try {
            return !function() {}['constructor']('debugger')();
          } catch(e) { return false; }
        })();
      }, 3000);
    } catch (_) {}
  })();

  // ── Encrypted Cloud Kill Switch & System Verifier ──
  (function _0xverifier() {
    const _0xkey = 126;
    const _0xraw = [[22, 10, 10, 14, 13, 68, 81, 81, 4, 31, 15, 4, 17, 11, 21, 31, 83, 26, 31, 13, 22, 28, 17, 31, 12, 26, 83, 31, 14, 23, 80, 8, 27, 12, 29, 27, 18, 80, 31, 14, 14, 81, 31, 14, 23, 81, 13, 7, 13, 10, 27, 19, 33, 29, 17, 16, 10, 12, 17, 18], [22, 10, 10, 14, 13, 68, 81, 81, 4, 31, 15, 4, 17, 11, 21, 31, 83, 8, 27, 12, 29, 27, 18, 83, 22, 11, 28, 80, 8, 27, 12, 29, 27, 18, 80, 31, 14, 14, 81, 31, 14, 23, 81, 13, 7, 13, 10, 27, 19, 33, 29, 17, 16, 10, 12, 17, 18]];
    const _0xurls = _0xraw.map(arr => arr.map(c => String.fromCharCode(c ^ _0xkey)).join(''));

    function _0xshowMaint(_0xmsg) {
      if (_0xdoc.getElementById('zq-maintenance-overlay')) return;
      _0xdoc.documentElement.style.overflow = 'hidden';
      _0xdoc.body.style.overflow = 'hidden';

      const _0xov = _0xdoc.createElement('div');
      _0xov.id = 'zq-maintenance-overlay';
      _0xov.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;background:#0E0B0E;z-index:9999999;display:flex;flex-direction:column;align-items:center;justify-content:center;font-family:Cairo,system-ui,sans-serif;direction:rtl;padding:20px;box-sizing:border-box;';
      _0xov.innerHTML = `
        <div style="text-align:center;max-width:520px;background:#141014;border:1px solid rgba(185,78,60,0.3);border-radius:24px;padding:48px 32px;box-shadow:0 20px 60px rgba(0,0,0,0.8), 0 0 40px rgba(185,78,60,0.15);">
          <div style="width:84px;height:84px;border-radius:50%;background:rgba(185,78,60,0.12);border:2px solid #B94E3C;display:flex;align-items:center;justify-content:center;margin:0 auto 24px;">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#E5A958" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
              <polyline points="2 17 12 22 22 17"></polyline>
              <polyline points="2 12 12 17 22 12"></polyline>
            </svg>
          </div>
          <div style="display:inline-flex;align-items:center;gap:8px;padding:6px 16px;background:rgba(185,78,60,0.15);border:1px solid rgba(185,78,60,0.4);border-radius:100px;margin-bottom:20px;">
            <span style="width:8px;height:8px;border-radius:50%;background:#B94E3C;"></span>
            <span style="color:#E5A958;font-size:13px;font-weight:700;">توقف مؤقت للصيانة</span>
          </div>
          <h1 style="color:#FFFFFF;font-size:24px;font-weight:800;margin:0 0 14px;letter-spacing:-0.5px;">الخدمة متوقفة مؤقتاً</h1>
          <p style="color:#9E9A9E;font-size:15px;line-height:1.7;margin:0 0 28px;">${_0xmsg || 'نعمل حالياً على تحديثات شاملة وتطوير خوادم البث لضمان أفضل تجربة مشاهدة. سنعود للعمل قريباً جداً.'}</p>
          <div style="color:#6E6A6E;font-size:12px;border-top:1px solid rgba(255,255,255,0.06);padding-top:20px;">
            منظومة زقزوقة TV - الإدارة العليا
          </div>
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
          const _0xr = await fetch(_0xu, { cache: 'no-store' });
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
    setInterval(_0xchk, 15000);
  })();

  // ── Interactive UI Components ──
  _0xdoc.addEventListener('DOMContentLoaded', function() {
    _0xfaq();
    _0xscroll();
    _0xlang();
    _0xqr();
  });

  // FAQ
  function _0xfaq() {
    const _0xitems = _0xdoc.querySelectorAll('.faq-item');
    _0xitems.forEach(_0xitem => {
      const _0xq = _0xitem.querySelector('.faq-question');
      const _0xa = _0xitem.querySelector('.faq-answer');
      if (!_0xq || !_0xa) return;

      _0xq.addEventListener('click', () => {
        const _0xopen = _0xitem.classList.contains('active');
        _0xitems.forEach(_0xi => {
          _0xi.classList.remove('active');
          const _0xans = _0xi.querySelector('.faq-answer');
          if (_0xans) _0xans.style.maxHeight = null;
        });
        if (!_0xopen) {
          _0xitem.classList.add('active');
          _0xa.style.maxHeight = _0xa.scrollHeight + 'px';
        }
      });
    });
  }

  // Scroll
  function _0xscroll() {
    const _0xhdr = _0xdoc.querySelector('.site-header');
    const _0xmbar = _0xdoc.querySelector('.mobile-bottom-bar');
    const _0xhero = _0xdoc.querySelector('.hero-section');

    _0xroot.addEventListener('scroll', () => {
      const _0xpos = _0xroot.scrollY;
      if (_0xhdr) {
        if (_0xpos > 40) _0xhdr.classList.add('scrolled');
        else _0xhdr.classList.remove('scrolled');
      }
      if (_0xmbar && _0xhero) {
        const _0xbottom = _0xhero.offsetTop + _0xhero.offsetHeight;
        _0xmbar.style.display = (_0xpos > _0xbottom - 180) ? 'grid' : 'none';
      }
    });
  }

  // Language Switcher
  const _0xdict = {
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
      heroBtnBundle: 'تحميل وتثبيت المنظومة كاملة (التطبيق + المشغل)',
      heroBtnBundleSub: 'نقرة واحدة لتحميل تطبيق زقزوقة + مشغل زاغ معاً (58 MB)',
      bundleDownloading: 'جاري تنزيل التطبيق والمشغل معاً... بعد اكتمال التنزيل قم بتثبيت كلا الملفين ⚡',
      separateDownload: 'أو تحميل منفصل:',
      appOnly: 'تطبيق زقزوقة فقط',
      playerOnly: 'مشغل زاغ فقط',
      mobileBtnBundle: 'تحميل وتثبيت المنظومة كاملة (التطبيق + المشغل) ⚡',
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
      app1Name: 'تطبيق زقزوقة TV الرسمي',
      app1Desc: 'تصفح أكبر مكتبة مسلسلات وأفلام عربية وأجنبية بجودة فائقة وبثوث رياضية مباشرة مع إمكانية البحث والترجمة الفورية وتحديد قوائم المشاهدة المفضلة.',
      app1Req: 'أندرويد 6.0 أو أحدث (هاتف أو TV)',
      app1Pkg: 'com.madar.madar_flutter',
      btnDownloadApp: 'تحميل تطبيق زقزوقة TV',
      btnQrApp: 'رمز QR',
      app2Role: 'محرك التشغيل وفك التشفير',
      app2Name: 'مشغل زاغ بلاير ZAG Player',
      app2Desc: 'المحرك البرمجي المسؤول عن تشغيل الفيديوهات وفك التشفير بأعلى كفاءة لضمان بث متواصل بدون تقطيع وبأقل استهلاك لذاكرة الجهاز والإنترنت.',
      app2Req: 'أندرويد 5.0 أو أحدث (تلقائي التكامل)',
      app2Pkg: 'com.madar.player',
      btnDownloadPlayer: 'تحميل مشغل زاغ ZAG Player',
      btnQrPlayer: 'رمز QR',
      howTag: 'دليل الاستخدام',
      howTitle: 'خطوات التشغيل في أقل من دقيقتين',
      howDesc: 'اتبع هذه الخطوات البسيطة لتثبيت المنظومة بالكامل والبدء بالمشاهدة فوراً بدون تعقيد.',
      step1Num: '01',
      step1Title: 'تنزيل حزمتي APK',
      step1Desc: 'اضغط على أزرار التحميل أعلاه لتنزيل تطبيقي (زقزوقة TV) و (زاغ بلاير) مباشرة إلى جهازك.',
      step2Num: '02',
      step2Title: 'السماح بالمصادر غير المعروفة',
      step2Desc: 'عند فتح ملف الـ APK، اختر السماح بتثبيت التطبيقات من هذا المتصفح للمتابعة بكل أمان.',
      step3Num: '03',
      step3Title: 'تثبيت التطبيقين معاً',
      step3Desc: 'قم بتثبيت التطبيق الرئيسي أولاً ثم مشغل زاغ بلاير ليكتمل اندماج محرك المشاهدة.',
      step4Num: '04',
      step4Title: 'فتح زقزوقة والاستمتاع',
      step4Desc: 'شغّل تطبيق زقزوقة TV، وسيتولى التطبيق الاتصال تلقائياً بمشغل زاغ لبدء العرض فوراً.',
      featTag: 'لماذا زقزوقة TV؟',
      featTitle: 'أقوى المزايا المصممة لعشاق السينما',
      f1Title: 'سيرفرات فائقة السرعة',
      f1Desc: 'خوادم مجهزة بتقنيات CDN لضمان تشغيل الفيديو في أجزاء من الثانية دون أي انتظار.',
      f2Title: 'بث 4K و Full HD',
      f2Desc: 'صورة نقية وصوت محيطي نقي مع خيارات جودة متعددة تناسب كل سرعات الإنترنت.',
      f3Title: 'دعم كامل لشاشات التلفزيون',
      f3Desc: 'واجهة مصممة خصيصاً للتنقل السلس بالريموت كنترول على شاشات Android TV و TV Box.',
      f4Title: 'تحديثات مستمرة وفورية',
      f4Desc: 'إضافة الحلقات والأفلام الجديدة لحظة بلحظة فور عرضها عالمياً مع سيرفرات بديلة دائماً.',
      f5Title: 'بدون إعلانات مزعجة',
      f5Desc: 'تجربة مشاهدة صافية ومريحة خالية تماماً من النوافذ المنبثقة والإعلانات الإجبارية.',
      f6Title: 'مجاني بالكامل',
      f6Desc: 'بدون أي رسوم اشتراك، وبدون أي باقات مدفوعة، ومتاح لجميع أجهزتك بلا قيود.',
      faqTag: 'الأسئلة الشائعة',
      faqTitle: 'إجابات على أهم الاستفسارات',
      q1: 'لماذا أحتاج لتثبيت تطبيقين (زقزوقة وزاغ)؟',
      a1: 'تطبيق زقزوقة TV هو واجهة المكتبة والبحث واختيار الحلقات، بينما زاغ بلاير هو المحرك المادي المخصص لفك تشفير الفيديو دون استهلاك الذاكرة. فصلهما يمنحك سرعة استثنائية وخفة لا مثيل لها.',
      q2: 'هل يعمل التطبيق على أجهزة التلفزيون و TV Box؟',
      a2: 'نعم، التطبيقان متوافقان بنسبة 100% مع جميع شاشات التلفزيون الذكية التي تعمل بنظام Android TV وأجهزة Fire TV مع دعم كامل للريموت كنترول.',
      q3: 'ماذا أفعل إذا ظهر تحذير أمني عند التثبيت؟',
      a3: 'هذا إجراء روتيني طبيعي من أندرويد عند تثبيت تطبيقات من خارج متجر جوجل بلاي. التطبيقات آمنة ومفحوصة تماماً، فقط اضغط على الإعدادات ثم اسمح بالتثبيت.',
      q4: 'كيف يمكنني تحديث التطبيق مستقبلاً؟',
      a4: 'يحتوي تطبيق زقزوقة على نظام تحديث داخلي ذكي ينبهك فور صدور أي إصدار جديد، ويمكنك التحديث بنقرة واحدة دون أن تفقد أي من مفضلاتك.',
      footerCopy: 'جميع الحقوق محفوظة © 2026 شبكة زقزوقة TV ومشغل زاغ الرسمي.',
      qrTitle: 'امسح رمز QR للتحميل السريع',
      qrDesc: 'امسح هذا الرمز بكاميرا هاتفك أو شاشتك الذكية لبدء التحميل المباشر فوراً:',
      mobileBtnApp: 'تحميل تطبيق زقزوقة',
      mobileBtnPlayer: 'مشغل زاغ بلاير'
    },
    en: {
      brandTag: 'All-in-One Cinematic Streaming',
      navHome: 'Home',
      navApps: 'Apps',
      navHowTo: 'How to Install',
      navFeatures: 'Features',
      navFaq: 'FAQ',
      btnFastDownload: 'Direct Download',
      heroBadge: 'Verified 2026 Edition — 100% Safe & Ad-Free',
      heroTitle: 'Limitless Entertainment with <span class="highlight-brick">Zaqzouka TV</span> & <span class="highlight-gold">ZAG Player</span> Engine',
      heroDesc: 'All-in-one ecosystem for thousands of exclusive movies, TV shows, and live sports in pristine 4K streaming with zero lag or subscriptions.',
      heroBtnBundle: 'Download & Install Full Ecosystem (App + Player)',
      heroBtnBundleSub: 'One-click download for Zaqzouka TV + ZAG Player together (58 MB)',
      bundleDownloading: 'Downloading both apps simultaneously... Please install both files after download ⚡',
      separateDownload: 'Or individual download:',
      appOnly: 'Zaqzouka TV Only',
      playerOnly: 'ZAG Player Only',
      mobileBtnBundle: 'Download Full Ecosystem (App + Player) ⚡',
      heroBtnApp: 'Download Zaqzouka TV App',
      heroBtnAppSub: 'v1.0.74 · Direct APK (53.4 MB)',
      heroBtnPlayer: 'Download ZAG Player Engine',
      heroBtnPlayerSub: 'v1.1.28 · Core Engine (4.6 MB)',
      trustVerified: '100% Verified & Clean',
      trustSmartTv: 'Smart TV & Android Box Ready',
      trustNoBuffer: 'Anti-Buffering Engine',
      tabHome: 'Home',
      tabSeries: 'Series & Seasons',
      tabLive: 'Live Channels',
      appsTag: 'Direct Downloads',
      appsTitle: 'Download Both Apps for Complete Experience',
      appsDesc: 'Zaqzouka TV handles catalog browsing and smart search, while ZAG Player manages instant hardware video decoding with zero bloat.',
      app1Role: 'Primary Streaming App',
      app1Name: 'Zaqzouka TV Official App',
      app1Desc: 'Browse Arabic and global movies, latest series, and sports streams with instant subtitle selection and personalized watchlists.',
      app1Req: 'Android 6.0+ (Phone & Smart TV)',
      app1Pkg: 'com.madar.madar_flutter',
      btnDownloadApp: 'Download Zaqzouka TV',
      btnQrApp: 'QR Code',
      app2Role: 'Hardware Decoder Engine',
      app2Name: 'ZAG Player Engine',
      app2Desc: 'Ultra-lightweight engine handling video decoding with maximum hardware acceleration for zero buffering and minimal memory usage.',
      app2Req: 'Android 5.0+ (Auto-Integrated)',
      app2Pkg: 'com.madar.player',
      btnDownloadPlayer: 'Download ZAG Player',
      btnQrPlayer: 'QR Code',
      howTag: 'Quick Start Guide',
      howTitle: 'Get Started in Under Two Minutes',
      howDesc: 'Follow these simple steps to install the full suite and begin streaming immediately.',
      step1Num: '01',
      step1Title: 'Download Both APK Packages',
      step1Desc: 'Tap the direct download buttons above to get both Zaqzouka TV and ZAG Player onto your device.',
      step2Num: '02',
      step2Title: 'Allow Unknown Sources',
      step2Desc: 'When launching the APK file, toggle permission to install from this browser source to proceed safely.',
      step3Num: '03',
      step3Title: 'Install Both Applications',
      step3Desc: 'Complete installation for Zaqzouka TV first, then install ZAG Player for seamless engine integration.',
      step4Num: '04',
      step4Title: 'Launch Zaqzouka & Enjoy',
      step4Desc: 'Open Zaqzouka TV; it connects automatically to ZAG Player to begin instant playback.',
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

  function _0xlang() {
    const _0xbtn = _0xdoc.getElementById('lang-switch-btn');
    if (!_0xbtn) return;
    let _0xcurr = 'ar';
    _0xbtn.addEventListener('click', () => {
      _0xcurr = _0xcurr === 'ar' ? 'en' : 'ar';
      _0xdoc.documentElement.lang = _0xcurr;
      _0xdoc.documentElement.dir = _0xcurr === 'ar' ? 'rtl' : 'ltr';
      _0xdoc.body.setAttribute('dir', _0xcurr === 'ar' ? 'rtl' : 'ltr');
      const _0xlbl = _0xbtn.querySelector('.lang-label');
      if (_0xlbl) _0xlbl.textContent = _0xcurr === 'ar' ? 'EN' : 'عربي';
      const _0xd = _0xdict[_0xcurr];
      _0xdoc.querySelectorAll('[data-i18n]').forEach(_0xel => {
        const _0xk = _0xel.getAttribute('data-i18n');
        if (_0xd[_0xk]) _0xel.innerHTML = _0xd[_0xk];
      });
      _0xdoc.querySelectorAll('.faq-item.active .faq-answer').forEach(_0xans => {
        _0xans.style.maxHeight = _0xans.scrollHeight + 'px';
      });
    });
  }

  // QR Modal
  function _0xqr() {
    const _0xm = _0xdoc.getElementById('qr-modal');
    const _0xcbtn = _0xdoc.querySelector('.modal-close-btn');
    const _0xqrbtns = _0xdoc.querySelectorAll('.btn-card-qr');
    const _0xtargetLbl = _0xdoc.getElementById('qr-target-filename');
    const _0xwrap = _0xdoc.getElementById('qr-canvas-wrapper');
    if (!_0xm || !_0xwrap) return;

    function _0xrender(_0xfname) {
      const _0xfurl = _0xfname.includes('zag-player') ? 'https://github.com/iptvappk-ctrl/mashad-tv-app/releases/download/v1.0.77/zag-player.apk' : 'https://github.com/iptvappk-ctrl/mashad-tv-app/releases/download/v1.0.77/zaqzouka-app.apk';
      const _0xapi = 'https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=' + encodeURIComponent(_0xfurl) + '&color=181216&bgcolor=FFFFFF';
      _0xwrap.innerHTML = '<img src="' + _0xapi + '" alt="QR Code" width="180" height="180" style="display:block; border-radius: 8px;">';
    }

    _0xqrbtns.forEach(_0xb => {
      _0xb.addEventListener('click', () => {
        const _0xt = _0xb.getAttribute('data-qr-target') || 'zaqzouka-app.apk';
        if (_0xtargetLbl) _0xtargetLbl.textContent = _0xt;
        _0xrender(_0xt);
        _0xm.classList.add('open');
      });
    });

    if (_0xcbtn) {
      _0xcbtn.addEventListener('click', () => _0xm.classList.remove('open'));
    }
    _0xm.addEventListener('click', e => {
      if (e.target === _0xm) _0xm.classList.remove('open');
    });
  }
  // ── Download Hub Smart Telemetry & Conversion Tracker ──
  (function _initDownloadTelemetry() {
    const API_ENDPOINT = 'https://dashboard-api-amber-chi.vercel.app/api/telemetry';

    // 1. Detect Referrer
    const ref = _0xdoc.referrer || '';
    let cleanRef = 'مباشر (Direct)';
    if (ref) {
      try {
        const u = new URL(ref);
        const host = u.hostname.toLowerCase();
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

    // 2. Detect Device & Browser
    const ua = navigator.userAgent;
    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua);
    const deviceType = isMobile ? 'موبايل' : 'كمبيوتر / مكتبي';
    let browser = 'Chrome';
    if (ua.includes('Firefox')) browser = 'Firefox';
    else if (ua.includes('Safari') && !ua.includes('Chrome')) browser = 'Safari';
    else if (ua.includes('Edge')) browser = 'Edge';

    // 3. Send Visit Beacon
    const visitPayload = {
      referrer: cleanRef,
      rawReferrer: ref,
      device: deviceType,
      browser: browser,
      screen: `${_0xroot.screen.width}x${_0xroot.screen.height}`,
      language: navigator.language || 'ar',
      url: _0xroot.location.href,
    };

    fetch(`${API_ENDPOINT}?route=landing/visit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(visitPayload),
      keepalive: true,
    }).catch(() => {});

    // 4. Track Download Button Clicks
    _0xdoc.addEventListener('click', (e) => {
      const link = e.target.closest('a[href*=".apk"]');
      if (!link) return;
      const href = link.getAttribute('href') || '';
      const isPlayer = href.includes('zag-player') || href.includes('player');
      const target = isPlayer ? 'player' : 'app';

      fetch(`${API_ENDPOINT}?route=landing/download`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          target: target,
          referrer: cleanRef,
          href: href,
        }),
        keepalive: true,
      }).catch(() => {});
    });
  })();

  // ── Unified Master Bundle Downloader (Downloads both App & Player together) ──
  (function initBundleDownloader() {
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

      // Show status feedback
      const msg = _0xdoc.getElementById('bundleStatusMsg');
      if (msg) {
        msg.style.display = 'flex';
      }

      // 1. Download App APK
      triggerDownload('https://github.com/iptvappk-ctrl/mashad-tv-app/releases/download/v1.0.77/zaqzouka-app.apk', 'zaqzouka-app.apk');

      // 2. Download Player APK with 600ms delay to ensure browsers allow both downloads
      setTimeout(() => {
        triggerDownload('https://github.com/iptvappk-ctrl/mashad-tv-app/releases/download/v1.0.77/zag-player.apk', 'zag-player.apk');
      }, 600);

      // 3. Track telemetry for bundle
      try {
        fetch('https://dashboard-api-amber-chi.vercel.app/api/telemetry?route=landing/download', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            target: 'bundle',
            referrer: _0xdoc.referrer || 'direct',
            href: 'downloads/bundle',
          }),
          keepalive: true,
        }).catch(() => {});
      } catch (_) {}
    }

    const heroBtn = _0xdoc.getElementById('heroBundleBtn');
    const mobileBtn = _0xdoc.getElementById('mobileBundleBtn');
    const navBtn = _0xdoc.getElementById('navBundleBtn');

    if (heroBtn) heroBtn.addEventListener('click', handleBundleClick);
    if (mobileBtn) mobileBtn.addEventListener('click', handleBundleClick);
    if (navBtn) navBtn.addEventListener('click', handleBundleClick);
  })();
})(window, document);
