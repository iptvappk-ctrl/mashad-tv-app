/**
 * (c) 2026 Zaqzouka Network Architecture - All Rights Reserved.
 * Protected under International Copyright & Digital Rights Protection Treaties.
 * Unauthorized copying, reverse engineering, tampering, or distribution is strictly prohibited.
 */
(function(_0xroot, _0xdoc) {
  'use strict';

  // â”€â”€ Anti-DevTools & Anti-Inspection Shield â”€â”€
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
            '%c[!] ظ†ط¸ط§ظ… ط­ظ…ط§ظٹط© ط²ظ‚ط²ظˆظ‚ط© ط§ظ„ظ…ط¹طھظ…ط¯ 2026\nظ‡ط°ظ‡ ط§ظ„طµظپط­ط© ظ…ط´ظپط±ط© ظˆظ…ط­ظ…ظٹط© ط¨ط§ظ„ظƒط§ظ…ظ„ ط¶ط¯ ط§ظ„ظ‡ظ†ط¯ط³ط© ط§ظ„ط¹ظƒط³ظٹط© ط£ظˆ ط§ظ„طھظ„ط§ط¹ط¨.',
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

  // â”€â”€ Encrypted Cloud Kill Switch & System Verifier â”€â”€
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
            <span style="color:#E5A958;font-size:13px;font-weight:700;">طھظˆظ‚ظپ ظ…ط¤ظ‚طھ ظ„ظ„طµظٹط§ظ†ط©</span>
          </div>
          <h1 style="color:#FFFFFF;font-size:24px;font-weight:800;margin:0 0 14px;letter-spacing:-0.5px;">ط§ظ„ط®ط¯ظ…ط© ظ…طھظˆظ‚ظپط© ظ…ط¤ظ‚طھط§ظ‹</h1>
          <p style="color:#9E9A9E;font-size:15px;line-height:1.7;margin:0 0 28px;">${_0xmsg || 'ظ†ط¹ظ…ظ„ ط­ط§ظ„ظٹط§ظ‹ ط¹ظ„ظ‰ طھط­ط¯ظٹط«ط§طھ ط´ط§ظ…ظ„ط© ظˆطھط·ظˆظٹط± ط®ظˆط§ط¯ظ… ط§ظ„ط¨ط« ظ„ط¶ظ…ط§ظ† ط£ظپط¶ظ„ طھط¬ط±ط¨ط© ظ…ط´ط§ظ‡ط¯ط©. ط³ظ†ط¹ظˆط¯ ظ„ظ„ط¹ظ…ظ„ ظ‚ط±ظٹط¨ط§ظ‹ ط¬ط¯ط§ظ‹.'}</p>
          <div style="color:#6E6A6E;font-size:12px;border-top:1px solid rgba(255,255,255,0.06);padding-top:20px;">
            ظ…ظ†ط¸ظˆظ…ط© ط²ظ‚ط²ظˆظ‚ط© TV - ط§ظ„ط¥ط¯ط§ط±ط© ط§ظ„ط¹ظ„ظٹط§
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

  // â”€â”€ Interactive UI Components â”€â”€
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
      brandTag: 'ظ…ظ†ط¸ظˆظ…ط© ط§ظ„ط¨ط« ط§ظ„ط³ظٹظ†ظ…ط§ط¦ظٹ ط§ظ„ظ…طھظƒط§ظ…ظ„ط©',
      navHome: 'ط§ظ„ط±ط¦ظٹط³ظٹط©',
      navApps: 'ط§ظ„طھط·ط¨ظٹظ‚ط§طھ',
      navHowTo: 'ط·ط±ظٹظ‚ط© ط§ظ„طھط«ط¨ظٹطھ',
      navFeatures: 'ط§ظ„ظ…ظ…ظٹط²ط§طھ',
      navFaq: 'ط§ظ„ط£ط³ط¦ظ„ط© ط§ظ„ط´ط§ط¦ط¹ط©',
      btnFastDownload: 'طھط­ظ…ظٹظ„ ظ…ط¨ط§ط´ط±',
      heroBadge: 'ط§ظ„ط¥طµط¯ط§ط± ط§ظ„ظ…ط¹طھظ…ط¯ 2026 â€” ط¢ظ…ظ† 100% ظˆط®ط§ظ„ظچ ظ…ظ† ط§ظ„ط¥ط¹ظ„ط§ظ†ط§طھ',
      heroTitle: 'ط¹ط§ظ„ظ…ظƒ ط§ظ„طھط±ظپظٹظ‡ظٹ ط¨ط¯ظˆظ† ط­ط¯ظˆط¯ ظ…ط¹ <span class="highlight-brick">طھط·ط¨ظٹظ‚ ط²ظ‚ط²ظˆظ‚ط©</span> ظˆظ…ط­ط±ظƒ <span class="highlight-gold">ط²ط§ط؛ ط¨ظ„ط§ظٹط±</span>',
      heroDesc: 'ظ…ظ†ط¸ظˆظ…ط© ظ…طھظƒط§ظ…ظ„ط© ظ„ظ…ط´ط§ظ‡ط¯ط© ط¢ظ„ط§ظپ ط§ظ„ط£ظپظ„ط§ظ… ظˆط§ظ„ظ…ط³ظ„ط³ظ„ط§طھ ط§ظ„ط­طµط±ظٹط© ظˆط§ظ„ظ‚ظ†ظˆط§طھ ط§ظ„طھظ„ظپط²ظٹظˆظ†ظٹط© ظˆط§ظ„ط±ظٹط§ط¶ظٹط© ط§ظ„ط­ظٹط© ط¨ط£ط¹ظ„ظ‰ ط¬ظˆط¯ط© ظˆط³ظٹط±ظپط±ط§طھ ظپط§ط¦ظ‚ط© ط§ظ„ط³ط±ط¹ط© ط¨ط¯ظˆظ† ط£ظٹ طھظ‚ط·ظٹط¹ ط£ظˆ ط§ط´طھط±ط§ظƒط§طھ.',
      heroBtnBundle: 'تحميل وتثبيت المنظومة كاملة (التطبيق + المشغل)',
      heroBtnBundleSub: 'نقرة واحدة لتحميل تطبيق زقزوقة + مشغل زاغ معاً (58 MB)',
      bundleDownloading: 'جاري تنزيل التطبيق والمشغل معاً... بعد اكتمال التنزيل قم بتثبيت كلا الملفين ⚡',
      separateDownload: 'أو تحميل منفصل:',
      appOnly: 'تطبيق زقزوقة فقط',
      playerOnly: 'مشغل زاغ فقط',
      mobileBtnBundle: 'تحميل وتثبيت المنظومة كاملة (التطبيق + المشغل) ⚡',
      heroBtnApp: 'تحميل تطبيق زقزوقة TV',
      heroBtnAppSub: 'v1.0.77 · حزمة APK مباشرة (53.4 MB)',
      heroBtnPlayer: 'تحميل مشغل زاغ ZAG Player',
      heroBtnPlayerSub: 'v1.1.29 · المحرك الأساسي (4.6 MB)',
      trustVerified: 'ظ…ظپط­ظˆطµ ظˆظ†ط¸ظٹظپ 100%',
      trustSmartTv: 'ظٹط¯ط¹ظ… ط§ظ„ظ‡ظˆط§طھظپ ظˆط´ط§ط´ط§طھ Smart TV',
      trustNoBuffer: 'طھظ‚ظ†ظٹط© ظ…ط§ظ†ط¹ ط§ظ„طھظ‚ط·ظٹط¹',
      tabHome: 'ط§ظ„ط±ط¦ظٹط³ظٹط©',
      tabSeries: 'ط§ظ„ظ…ط³ظ„ط³ظ„ط§طھ ظˆط§ظ„ظ…ظˆط§ط³ظ…',
      tabLive: 'ط§ظ„ط¨ط« ط§ظ„ظ…ط¨ط§ط´ط±',
      appsTag: 'ط§ظ„طھط­ظ…ظٹظ„ ط§ظ„ظ…ط¨ط§ط´ط±',
      appsTitle: 'ط­ظ…ظ‘ظ„ ط§ظ„طھط·ط¨ظٹظ‚ظٹظ† ظ…ط¹ط§ظ‹ ظ„طھط¬ط±ط¨ط© ط³ظٹظ†ظ…ط§ط¦ظٹط© ظƒط§ظ…ظ„ط©',
      appsDesc: 'طھط·ط¨ظٹظ‚ ط²ظ‚ط²ظˆظ‚ط© ظٹظ…ظ†ط­ظƒ ظ…ظƒطھط¨ط© ط§ظ„ط£ظپظ„ط§ظ… ظˆط§ظ„ظ…ط³ظ„ط³ظ„ط§طھ ظˆط§ظ„ط¨ط­ط« ط§ظ„ط°ظƒظٹطŒ ظˆظ…ط´ط؛ظ„ ط²ط§ط؛ ظٹطھظˆظ„ظ‰ ظپظƒ ط§ظ„طھط´ظپظٹط± ظˆط§ظ„طھط´ط؛ظٹظ„ ط§ظ„ظپظˆط±ظٹ ط¨ط¯ظˆظ† ط£ظٹ ط§ط³طھظ‡ظ„ط§ظƒ ظ„ظ„ظ…ظˆط§ط±ط¯.',
      app1Role: 'ط§ظ„طھط·ط¨ظٹظ‚ ط§ظ„ط±ط¦ظٹط³ظٹ',
      app1Name: 'طھط·ط¨ظٹظ‚ ط²ظ‚ط²ظˆظ‚ط© TV ط§ظ„ط±ط³ظ…ظٹ',
      app1Desc: 'طھطµظپط­ ط£ظƒط¨ط± ظ…ظƒطھط¨ط© ظ…ط³ظ„ط³ظ„ط§طھ ظˆط£ظپظ„ط§ظ… ط¹ط±ط¨ظٹط© ظˆط£ط¬ظ†ط¨ظٹط© ط¨ط¬ظˆط¯ط© ظپط§ط¦ظ‚ط© ظˆط¨ط«ظˆط« ط±ظٹط§ط¶ظٹط© ظ…ط¨ط§ط´ط±ط© ظ…ط¹ ط¥ظ…ظƒط§ظ†ظٹط© ط§ظ„ط¨ط­ط« ظˆط§ظ„طھط±ط¬ظ…ط© ط§ظ„ظپظˆط±ظٹط© ظˆطھط­ط¯ظٹط¯ ظ‚ظˆط§ط¦ظ… ط§ظ„ظ…ط´ط§ظ‡ط¯ط© ط§ظ„ظ…ظپط¶ظ„ط©.',
      app1Req: 'ط£ظ†ط¯ط±ظˆظٹط¯ 6.0 ط£ظˆ ط£ط­ط¯ط« (ظ‡ط§طھظپ ط£ظˆ TV)',
      app1Pkg: 'com.madar.madar_flutter',
      btnDownloadApp: 'طھط­ظ…ظٹظ„ طھط·ط¨ظٹظ‚ ط²ظ‚ط²ظˆظ‚ط© TV',
      btnQrApp: 'ط±ظ…ط² QR',
      app2Role: 'ظ…ط­ط±ظƒ ط§ظ„طھط´ط؛ظٹظ„ ظˆظپظƒ ط§ظ„طھط´ظپظٹط±',
      app2Name: 'ظ…ط´ط؛ظ„ ط²ط§ط؛ ط¨ظ„ط§ظٹط± ZAG Player',
      app2Desc: 'ط§ظ„ظ…ط­ط±ظƒ ط§ظ„ط¨ط±ظ…ط¬ظٹ ط§ظ„ظ…ط³ط¤ظˆظ„ ط¹ظ† طھط´ط؛ظٹظ„ ط§ظ„ظپظٹط¯ظٹظˆظ‡ط§طھ ظˆظپظƒ ط§ظ„طھط´ظپظٹط± ط¨ط£ط¹ظ„ظ‰ ظƒظپط§ط،ط© ظ„ط¶ظ…ط§ظ† ط¨ط« ظ…طھظˆط§طµظ„ ط¨ط¯ظˆظ† طھظ‚ط·ظٹط¹ ظˆط¨ط£ظ‚ظ„ ط§ط³طھظ‡ظ„ط§ظƒ ظ„ط°ط§ظƒط±ط© ط§ظ„ط¬ظ‡ط§ط² ظˆط§ظ„ط¥ظ†طھط±ظ†طھ.',
      app2Req: 'ط£ظ†ط¯ط±ظˆظٹط¯ 5.0 ط£ظˆ ط£ط­ط¯ط« (طھظ„ظ‚ط§ط¦ظٹ ط§ظ„طھظƒط§ظ…ظ„)',
      app2Pkg: 'com.madar.player',
      btnDownloadPlayer: 'طھط­ظ…ظٹظ„ ظ…ط´ط؛ظ„ ط²ط§ط؛ ZAG Player',
      btnQrPlayer: 'ط±ظ…ط² QR',
      howTag: 'ط¯ظ„ظٹظ„ ط§ظ„ط§ط³طھط®ط¯ط§ظ…',
      howTitle: 'ط®ط·ظˆط§طھ ط§ظ„طھط´ط؛ظٹظ„ ظپظٹ ط£ظ‚ظ„ ظ…ظ† ط¯ظ‚ظٹظ‚طھظٹظ†',
      howDesc: 'ط§طھط¨ط¹ ظ‡ط°ظ‡ ط§ظ„ط®ط·ظˆط§طھ ط§ظ„ط¨ط³ظٹط·ط© ظ„طھط«ط¨ظٹطھ ط§ظ„ظ…ظ†ط¸ظˆظ…ط© ط¨ط§ظ„ظƒط§ظ…ظ„ ظˆط§ظ„ط¨ط¯ط، ط¨ط§ظ„ظ…ط´ط§ظ‡ط¯ط© ظپظˆط±ط§ظ‹ ط¨ط¯ظˆظ† طھط¹ظ‚ظٹط¯.',
      step1Num: '01',
      step1Title: 'طھظ†ط²ظٹظ„ ط­ط²ظ…طھظٹ APK',
      step1Desc: 'ط§ط¶ط؛ط· ط¹ظ„ظ‰ ط£ط²ط±ط§ط± ط§ظ„طھط­ظ…ظٹظ„ ط£ط¹ظ„ط§ظ‡ ظ„طھظ†ط²ظٹظ„ طھط·ط¨ظٹظ‚ظٹ (ط²ظ‚ط²ظˆظ‚ط© TV) ظˆ (ط²ط§ط؛ ط¨ظ„ط§ظٹط±) ظ…ط¨ط§ط´ط±ط© ط¥ظ„ظ‰ ط¬ظ‡ط§ط²ظƒ.',
      step2Num: '02',
      step2Title: 'ط§ظ„ط³ظ…ط§ط­ ط¨ط§ظ„ظ…طµط§ط¯ط± ط؛ظٹط± ط§ظ„ظ…ط¹ط±ظˆظپط©',
      step2Desc: 'ط¹ظ†ط¯ ظپطھط­ ظ…ظ„ظپ ط§ظ„ظ€ APKطŒ ط§ط®طھط± ط§ظ„ط³ظ…ط§ط­ ط¨طھط«ط¨ظٹطھ ط§ظ„طھط·ط¨ظٹظ‚ط§طھ ظ…ظ† ظ‡ط°ط§ ط§ظ„ظ…طھطµظپط­ ظ„ظ„ظ…طھط§ط¨ط¹ط© ط¨ظƒظ„ ط£ظ…ط§ظ†.',
      step3Num: '03',
      step3Title: 'طھط«ط¨ظٹطھ ط§ظ„طھط·ط¨ظٹظ‚ظٹظ† ظ…ط¹ط§ظ‹',
      step3Desc: 'ظ‚ظ… ط¨طھط«ط¨ظٹطھ ط§ظ„طھط·ط¨ظٹظ‚ ط§ظ„ط±ط¦ظٹط³ظٹ ط£ظˆظ„ط§ظ‹ ط«ظ… ظ…ط´ط؛ظ„ ط²ط§ط؛ ط¨ظ„ط§ظٹط± ظ„ظٹظƒطھظ…ظ„ ط§ظ†ط¯ظ…ط§ط¬ ظ…ط­ط±ظƒ ط§ظ„ظ…ط´ط§ظ‡ط¯ط©.',
      step4Num: '04',
      step4Title: 'ظپطھط­ ط²ظ‚ط²ظˆظ‚ط© ظˆط§ظ„ط§ط³طھظ…طھط§ط¹',
      step4Desc: 'ط´ط؛ظ‘ظ„ طھط·ط¨ظٹظ‚ ط²ظ‚ط²ظˆظ‚ط© TVطŒ ظˆط³ظٹطھظˆظ„ظ‰ ط§ظ„طھط·ط¨ظٹظ‚ ط§ظ„ط§طھطµط§ظ„ طھظ„ظ‚ط§ط¦ظٹط§ظ‹ ط¨ظ…ط´ط؛ظ„ ط²ط§ط؛ ظ„ط¨ط¯ط، ط§ظ„ط¹ط±ط¶ ظپظˆط±ط§ظ‹.',
      featTag: 'ظ„ظ…ط§ط°ط§ ط²ظ‚ط²ظˆظ‚ط© TVطں',
      featTitle: 'ط£ظ‚ظˆظ‰ ط§ظ„ظ…ط²ط§ظٹط§ ط§ظ„ظ…طµظ…ظ…ط© ظ„ط¹ط´ط§ظ‚ ط§ظ„ط³ظٹظ†ظ…ط§',
      f1Title: 'ط³ظٹط±ظپط±ط§طھ ظپط§ط¦ظ‚ط© ط§ظ„ط³ط±ط¹ط©',
      f1Desc: 'ط®ظˆط§ط¯ظ… ظ…ط¬ظ‡ط²ط© ط¨طھظ‚ظ†ظٹط§طھ CDN ظ„ط¶ظ…ط§ظ† طھط´ط؛ظٹظ„ ط§ظ„ظپظٹط¯ظٹظˆ ظپظٹ ط£ط¬ط²ط§ط، ظ…ظ† ط§ظ„ط«ط§ظ†ظٹط© ط¯ظˆظ† ط£ظٹ ط§ظ†طھط¸ط§ط±.',
      f2Title: 'ط¨ط« 4K ظˆ Full HD',
      f2Desc: 'طµظˆط±ط© ظ†ظ‚ظٹط© ظˆطµظˆطھ ظ…ط­ظٹط·ظٹ ظ†ظ‚ظٹ ظ…ط¹ ط®ظٹط§ط±ط§طھ ط¬ظˆط¯ط© ظ…طھط¹ط¯ط¯ط© طھظ†ط§ط³ط¨ ظƒظ„ ط³ط±ط¹ط§طھ ط§ظ„ط¥ظ†طھط±ظ†طھ.',
      f3Title: 'ط¯ط¹ظ… ظƒط§ظ…ظ„ ظ„ط´ط§ط´ط§طھ ط§ظ„طھظ„ظپط²ظٹظˆظ†',
      f3Desc: 'ظˆط§ط¬ظ‡ط© ظ…طµظ…ظ…ط© ط®طµظٹطµط§ظ‹ ظ„ظ„طھظ†ظ‚ظ„ ط§ظ„ط³ظ„ط³ ط¨ط§ظ„ط±ظٹظ…ظˆطھ ظƒظ†طھط±ظˆظ„ ط¹ظ„ظ‰ ط´ط§ط´ط§طھ Android TV ظˆ TV Box.',
      f4Title: 'طھط­ط¯ظٹط«ط§طھ ظ…ط³طھظ…ط±ط© ظˆظپظˆط±ظٹط©',
      f4Desc: 'ط¥ط¶ط§ظپط© ط§ظ„ط­ظ„ظ‚ط§طھ ظˆط§ظ„ط£ظپظ„ط§ظ… ط§ظ„ط¬ط¯ظٹط¯ط© ظ„ط­ط¸ط© ط¨ظ„ط­ط¸ط© ظپظˆط± ط¹ط±ط¶ظ‡ط§ ط¹ط§ظ„ظ…ظٹط§ظ‹ ظ…ط¹ ط³ظٹط±ظپط±ط§طھ ط¨ط¯ظٹظ„ط© ط¯ط§ط¦ظ…ط§ظ‹.',
      f5Title: 'ط¨ط¯ظˆظ† ط¥ط¹ظ„ط§ظ†ط§طھ ظ…ط²ط¹ط¬ط©',
      f5Desc: 'طھط¬ط±ط¨ط© ظ…ط´ط§ظ‡ط¯ط© طµط§ظپظٹط© ظˆظ…ط±ظٹط­ط© ط®ط§ظ„ظٹط© طھظ…ط§ظ…ط§ظ‹ ظ…ظ† ط§ظ„ظ†ظˆط§ظپط° ط§ظ„ظ…ظ†ط¨ط«ظ‚ط© ظˆط§ظ„ط¥ط¹ظ„ط§ظ†ط§طھ ط§ظ„ط¥ط¬ط¨ط§ط±ظٹط©.',
      f6Title: 'ظ…ط¬ط§ظ†ظٹ ط¨ط§ظ„ظƒط§ظ…ظ„',
      f6Desc: 'ط¨ط¯ظˆظ† ط£ظٹ ط±ط³ظˆظ… ط§ط´طھط±ط§ظƒطŒ ظˆط¨ط¯ظˆظ† ط£ظٹ ط¨ط§ظ‚ط§طھ ظ…ط¯ظپظˆط¹ط©طŒ ظˆظ…طھط§ط­ ظ„ط¬ظ…ظٹط¹ ط£ط¬ظ‡ط²طھظƒ ط¨ظ„ط§ ظ‚ظٹظˆط¯.',
      faqTag: 'ط§ظ„ط£ط³ط¦ظ„ط© ط§ظ„ط´ط§ط¦ط¹ط©',
      faqTitle: 'ط¥ط¬ط§ط¨ط§طھ ط¹ظ„ظ‰ ط£ظ‡ظ… ط§ظ„ط§ط³طھظپط³ط§ط±ط§طھ',
      q1: 'ظ„ظ…ط§ط°ط§ ط£ط­طھط§ط¬ ظ„طھط«ط¨ظٹطھ طھط·ط¨ظٹظ‚ظٹظ† (ط²ظ‚ط²ظˆظ‚ط© ظˆط²ط§ط؛)طں',
      a1: 'طھط·ط¨ظٹظ‚ ط²ظ‚ط²ظˆظ‚ط© TV ظ‡ظˆ ظˆط§ط¬ظ‡ط© ط§ظ„ظ…ظƒطھط¨ط© ظˆط§ظ„ط¨ط­ط« ظˆط§ط®طھظٹط§ط± ط§ظ„ط­ظ„ظ‚ط§طھطŒ ط¨ظٹظ†ظ…ط§ ط²ط§ط؛ ط¨ظ„ط§ظٹط± ظ‡ظˆ ط§ظ„ظ…ط­ط±ظƒ ط§ظ„ظ…ط§ط¯ظٹ ط§ظ„ظ…ط®طµطµ ظ„ظپظƒ طھط´ظپظٹط± ط§ظ„ظپظٹط¯ظٹظˆ ط¯ظˆظ† ط§ط³طھظ‡ظ„ط§ظƒ ط§ظ„ط°ط§ظƒط±ط©. ظپطµظ„ظ‡ظ…ط§ ظٹظ…ظ†ط­ظƒ ط³ط±ط¹ط© ط§ط³طھط«ظ†ط§ط¦ظٹط© ظˆط®ظپط© ظ„ط§ ظ…ط«ظٹظ„ ظ„ظ‡ط§.',
      q2: 'ظ‡ظ„ ظٹط¹ظ…ظ„ ط§ظ„طھط·ط¨ظٹظ‚ ط¹ظ„ظ‰ ط£ط¬ظ‡ط²ط© ط§ظ„طھظ„ظپط²ظٹظˆظ† ظˆ TV Boxطں',
      a2: 'ظ†ط¹ظ…طŒ ط§ظ„طھط·ط¨ظٹظ‚ط§ظ† ظ…طھظˆط§ظپظ‚ط§ظ† ط¨ظ†ط³ط¨ط© 100% ظ…ط¹ ط¬ظ…ظٹط¹ ط´ط§ط´ط§طھ ط§ظ„طھظ„ظپط²ظٹظˆظ† ط§ظ„ط°ظƒظٹط© ط§ظ„طھظٹ طھط¹ظ…ظ„ ط¨ظ†ط¸ط§ظ… Android TV ظˆط£ط¬ظ‡ط²ط© Fire TV ظ…ط¹ ط¯ط¹ظ… ظƒط§ظ…ظ„ ظ„ظ„ط±ظٹظ…ظˆطھ ظƒظ†طھط±ظˆظ„.',
      q3: 'ظ…ط§ط°ط§ ط£ظپط¹ظ„ ط¥ط°ط§ ط¸ظ‡ط± طھط­ط°ظٹط± ط£ظ…ظ†ظٹ ط¹ظ†ط¯ ط§ظ„طھط«ط¨ظٹطھطں',
      a3: 'ظ‡ط°ط§ ط¥ط¬ط±ط§ط، ط±ظˆطھظٹظ†ظٹ ط·ط¨ظٹط¹ظٹ ظ…ظ† ط£ظ†ط¯ط±ظˆظٹط¯ ط¹ظ†ط¯ طھط«ط¨ظٹطھ طھط·ط¨ظٹظ‚ط§طھ ظ…ظ† ط®ط§ط±ط¬ ظ…طھط¬ط± ط¬ظˆط¬ظ„ ط¨ظ„ط§ظٹ. ط§ظ„طھط·ط¨ظٹظ‚ط§طھ ط¢ظ…ظ†ط© ظˆظ…ظپط­ظˆطµط© طھظ…ط§ظ…ط§ظ‹طŒ ظپظ‚ط· ط§ط¶ط؛ط· ط¹ظ„ظ‰ ط§ظ„ط¥ط¹ط¯ط§ط¯ط§طھ ط«ظ… ط§ط³ظ…ط­ ط¨ط§ظ„طھط«ط¨ظٹطھ.',
      q4: 'ظƒظٹظپ ظٹظ…ظƒظ†ظ†ظٹ طھط­ط¯ظٹط« ط§ظ„طھط·ط¨ظٹظ‚ ظ…ط³طھظ‚ط¨ظ„ط§ظ‹طں',
      a4: 'ظٹط­طھظˆظٹ طھط·ط¨ظٹظ‚ ط²ظ‚ط²ظˆظ‚ط© ط¹ظ„ظ‰ ظ†ط¸ط§ظ… طھط­ط¯ظٹط« ط¯ط§ط®ظ„ظٹ ط°ظƒظٹ ظٹظ†ط¨ظ‡ظƒ ظپظˆط± طµط¯ظˆط± ط£ظٹ ط¥طµط¯ط§ط± ط¬ط¯ظٹط¯طŒ ظˆظٹظ…ظƒظ†ظƒ ط§ظ„طھط­ط¯ظٹط« ط¨ظ†ظ‚ط±ط© ظˆط§ط­ط¯ط© ط¯ظˆظ† ط£ظ† طھظپظ‚ط¯ ط£ظٹ ظ…ظ† ظ…ظپط¶ظ„ط§طھظƒ.',
      footerCopy: 'ط¬ظ…ظٹط¹ ط§ظ„ط­ظ‚ظˆظ‚ ظ…ط­ظپظˆط¸ط© آ© 2026 ط´ط¨ظƒط© ط²ظ‚ط²ظˆظ‚ط© TV ظˆظ…ط´ط؛ظ„ ط²ط§ط؛ ط§ظ„ط±ط³ظ…ظٹ.',
      qrTitle: 'ط§ظ…ط³ط­ ط±ظ…ط² QR ظ„ظ„طھط­ظ…ظٹظ„ ط§ظ„ط³ط±ظٹط¹',
      qrDesc: 'ط§ظ…ط³ط­ ظ‡ط°ط§ ط§ظ„ط±ظ…ط² ط¨ظƒط§ظ…ظٹط±ط§ ظ‡ط§طھظپظƒ ط£ظˆ ط´ط§ط´طھظƒ ط§ظ„ط°ظƒظٹط© ظ„ط¨ط¯ط، ط§ظ„طھط­ظ…ظٹظ„ ط§ظ„ظ…ط¨ط§ط´ط± ظپظˆط±ط§ظ‹:',
      mobileBtnApp: 'طھط­ظ…ظٹظ„ طھط·ط¨ظٹظ‚ ط²ظ‚ط²ظˆظ‚ط©',
      mobileBtnPlayer: 'ظ…ط´ط؛ظ„ ط²ط§ط؛ ط¨ظ„ط§ظٹط±'
    },
    en: {
      brandTag: 'All-in-One Cinematic Streaming',
      navHome: 'Home',
      navApps: 'Apps',
      navHowTo: 'How to Install',
      navFeatures: 'Features',
      navFaq: 'FAQ',
      btnFastDownload: 'Direct Download',
      heroBadge: 'Verified 2026 Edition â€” 100% Safe & Ad-Free',
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
      heroBtnAppSub: 'v1.0.77 · Direct APK (53.4 MB)',
      heroBtnPlayer: 'Download ZAG Player Engine',
      heroBtnPlayerSub: 'v1.1.29 · Core Engine (4.6 MB)',
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
      footerCopy: 'All rights reserved آ© 2026 Zaqzouka TV Network & ZAG Player Official.',
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
      if (_0xlbl) _0xlbl.textContent = _0xcurr === 'ar' ? 'EN' : 'ط¹ط±ط¨ظٹ';
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
      const _0xfurl = _0xroot.location.origin + _0xroot.location.pathname.replace(/\/+$/, '') + '/downloads/' + _0xfname;
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

  // â”€â”€ Download Hub Smart Telemetry & Conversion Tracker â”€â”€
  (function _initDownloadTelemetry() {
    const API_ENDPOINT = 'https://zaqzouka-dashboard-api.vercel.app/api/telemetry';

    // 1. Detect Referrer
    const ref = _0xdoc.referrer || '';
    let cleanRef = 'ظ…ط¨ط§ط´ط± (Direct)';
    if (ref) {
      try {
        const u = new URL(ref);
        const host = u.hostname.toLowerCase();
        if (host.includes('google')) cleanRef = 'ط¬ظˆط¬ظ„ (Google Search)';
        else if (host.includes('t.me') || host.includes('telegram')) cleanRef = 'طھظ„ظٹط¬ط±ط§ظ… (Telegram)';
        else if (host.includes('facebook') || host.includes('fb.com')) cleanRef = 'ظپظٹط³ط¨ظˆظƒ (Facebook)';
        else if (host.includes('instagram')) cleanRef = 'ط¥ظ†ط³طھط؛ط±ط§ظ… (Instagram)';
        else if (host.includes('tiktok')) cleanRef = 'طھظٹظƒ طھظˆظƒ (TikTok)';
        else if (host.includes('youtube') || host.includes('youtu.be')) cleanRef = 'ظٹظˆطھظٹظˆط¨ (YouTube)';
        else if (host.includes('twitter') || host.includes('x.com')) cleanRef = 'طھظˆظٹطھط± (X / Twitter)';
        else if (host.includes('whatsapp') || host.includes('wa.me')) cleanRef = 'ظˆط§طھط³ط§ط¨ (WhatsApp)';
        else cleanRef = host;
      } catch (_) {
        cleanRef = ref.substring(0, 40);
      }
    }

    // 2. Detect Device & Browser
    const ua = navigator.userAgent;
    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua);
    const deviceType = isMobile ? 'ظ…ظˆط¨ط§ظٹظ„' : 'ظƒظ…ط¨ظٹظˆطھط± / ظ…ظƒطھط¨ظٹ';
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
      triggerDownload('downloads/zaqzouka-app.apk', 'zaqzouka-app.apk');

      // 2. Download Player APK with 600ms delay to ensure browsers allow both downloads
      setTimeout(() => {
        triggerDownload('downloads/zag-player.apk', 'zag-player.apk');
      }, 600);

      // 3. Track telemetry for bundle
      try {
        fetch('https://zaqzouka-dashboard-api.vercel.app/api/telemetry?route=landing/download', {
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

