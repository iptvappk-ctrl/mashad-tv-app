const { db, sendJson, handleCors } = require('./_lib');

const ADMIN_SECRET = process.env.ADMIN_SECRET || '';

module.exports = async (req, res) => {
  handleCors(res);
  if (req.method === 'OPTIONS') return res.end();
  if (req.method !== 'GET') return sendJson(res, 405, { error: 'Method not allowed' });

  // Optional simple protection
  const auth = (req.headers && req.headers['x-admin-secret']) || (req.query && req.query.secret) || '';
  if (ADMIN_SECRET && auth !== ADMIN_SECRET) {
    return sendJson(res, 403, { error: 'Unauthorized' });
  }

  try {
    const [statsSnap, sessionsSnap, topContentSnap, searchesSnap, errorsSnap, actLogSnap, landingSnap, landingVisitorsSnap] = await Promise.all([
      db.ref('stats').once('value'),
      db.ref('sessions').orderByChild('openedAt').limitToLast(600).once('value'),
      db.ref('topContent').limitToLast(500).once('value'),
      db.ref('searches').limitToLast(500).once('value'),
      db.ref('errors').orderByChild('timestamp').limitToLast(50).once('value'),
      db.ref('activityLog').orderByChild('timestamp').limitToLast(50).once('value'),
      db.ref('landing_analytics').once('value'),
      db.ref('landing_visitors').limitToLast(50).once('value'),
    ]);

    const landing = landingSnap.val() || {};
    const landingVisitorsObj = landingVisitorsSnap.val() || {};
    const landingVisitors = Object.values(landingVisitorsObj)
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
      .slice(0, 30)
      .map((v) => {
        let dev = (v.device || 'هاتف').toString();
        if (dev.includes('mob') || dev.includes('موب') || dev.startsWith('ظ') || dev.includes('Phone')) dev = 'هاتف محمول';
        else if (dev.includes('desk') || dev.includes('كمب') || dev.includes('PC') || dev.includes('Windows')) dev = 'كمبيوتر';

        let ref = (v.referrer || 'مباشر').toString();
        if (ref.includes('Facebook') || ref.includes('فيسبوك') || ref.startsWith('ظپ')) ref = 'فيسبوك (Facebook)';
        else if (ref.includes('Direct') || ref.includes('مباشر') || ref.startsWith('ظ…ط¨')) ref = 'زيارة مباشرة (Direct)';
        else if (ref.includes('zaafilm')) ref = 'موقع زافيلم (zaafilm.com)';
        else if (ref.includes('Google') || ref.includes('جوجل')) ref = 'بحث جوجل (Google)';
        else if (ref.includes('TikTok') || ref.includes('تيك توك')) ref = 'تيك توك (TikTok)';

        return {
          ...v,
          device: dev,
          referrer: ref,
        };
      });

    const landingTotalVisits = landing.totalVisits || 0;
    const landingBundleDownloads = landing.downloads_bundle || 0;
    const landingAppDownloads = landing.downloads_app || 0;
    const landingPlayerDownloads = landing.downloads_player || 0;
    const landingTotalDownloads = landing.total_downloads || (landingBundleDownloads + landingAppDownloads + landingPlayerDownloads);
    const landingDownloadsPerVisit = landingTotalVisits > 0
      ? (landingTotalDownloads / landingTotalVisits).toFixed(1)
      : '0.0';
    const landingConversionRate = landingTotalVisits > 0
      ? Math.min(100, Math.round((landingTotalDownloads / (landingTotalVisits * 2)) * 100)).toFixed(1)
      : '0.0';

    const landingTopReferrers = Object.entries(landing.referrers || {})
      .map(([name, count]) => {
        let clean = name.replace(/_/g, '.');
        if (clean.includes('Facebook') || clean.includes('فيسبوك') || clean.startsWith('ظپ')) clean = 'فيسبوك (Facebook)';
        else if (clean.includes('Direct') || clean.includes('مباشر') || clean.startsWith('ظ…ط¨')) clean = 'زيارة مباشرة (Direct)';
        else if (clean.includes('zaafilm')) clean = 'موقع زافيلم (zaafilm.com)';
        else if (clean.includes('Google') || clean.includes('جوجل')) clean = 'بحث جوجل (Google)';
        else if (clean.includes('TikTok') || clean.includes('تيك توك')) clean = 'تيك توك (TikTok)';
        return {
          name: clean,
          count,
          percent: landingTotalVisits > 0 ? Math.min(100, Math.round((count / landingTotalVisits) * 100)) : 0,
        };
      })
      .sort((a, b) => b.count - a.count);

    const landingCountries = Object.entries(landing.countries || {})
      .map(([country, count]) => {
        let clean = country.replace(/_/g, '.');
        if (clean.includes('مصر') || clean.includes('EG')) clean = 'مصر 🇪🇬';
        else if (clean.includes('السعودية') || clean.includes('SA')) clean = 'السعودية 🇸🇦';
        else if (clean.includes('ليبيا') || clean.includes('LY')) clean = 'ليبيا 🇱🇾';
        else if (clean.includes('الجزائر') || clean.includes('DZ')) clean = 'الجزائر 🇩🇿';
        else if (clean.includes('السودان') || clean.includes('SD')) clean = 'السودان 🇸🇩';
        else if (clean.includes('سوريا') || clean.includes('SY')) clean = 'سوريا 🇸🇾';
        else if (clean.includes('الأردن') || clean.includes('JO')) clean = 'الأردن 🇯🇴';
        else if (clean.includes('العراق') || clean.includes('IQ')) clean = 'العراق 🇮🇶';
        else if (clean.includes('الإمارات') || clean.includes('AE')) clean = 'الإمارات 🇦🇪';
        else if (clean.includes('أمريكا') || clean.includes('US')) clean = 'أمريكا 🇺🇸';
        else if (clean.includes('ألمانيا') || clean.includes('DE')) clean = 'ألمانيا 🇩🇪';
        return {
          country: clean,
          count,
          percent: landingTotalVisits > 0 ? Math.min(100, Math.round((count / landingTotalVisits) * 100)) : 0,
        };
      })
      .sort((a, b) => b.count - a.count)
      .slice(0, 15);

    const stats = statsSnap.val() || {};
    const sessionsObj = sessionsSnap.val() || {};
    const sessions = Object.values(sessionsObj).sort((a, b) =>
      new Date(b.openedAt).getTime() - new Date(a.openedAt).getTime()
    );

    // Auto-expire stale active sessions (no heartbeat > 6 min, synced with 3m heartbeat interval)
    const now = Date.now();
    const staleUpdates = [];
    for (const s of sessions) {
      if (s.status === 'active') {
        const lastSeen = new Date(s.lastSeen || s.openedAt).getTime();
        if (now - lastSeen > 6 * 60 * 1000) {
          s.status = 'closed';
          staleUpdates.push(db.ref(`sessions/${s.id}`).update({ status: 'closed' }).catch(() => {}));
        }
      }
    }
    if (staleUpdates.length > 0) {
      await Promise.all(staleUpdates);
    }

    const activeNow = sessions.filter((s) => s.status === 'active').length;
    const totalSessions = sessions.length;

    // Platform separation (App vs Web)
    const webSessions = sessions.filter((s) => s.platform === 'web' || s.deviceType === 'web');
    const appSessions = sessions.filter((s) => s.platform !== 'web' && s.deviceType !== 'web');
    const webActiveNow = webSessions.filter((s) => s.status === 'active').length;
    const appActiveNow = appSessions.filter((s) => s.status === 'active').length;

    // Web browsers breakdown
    const browserMap = {};
    for (const s of webSessions) {
      const b = (s.browser || s.device || 'Chrome').split('(')[0].trim();
      browserMap[b] = (browserMap[b] || 0) + 1;
    }
    const webBrowsers = Object.entries(browserMap)
      .map(([browser, count]) => ({
        browser,
        count,
        percent: webSessions.length > 0 ? Math.round((count / webSessions.length) * 100) : 0,
      }))
      .sort((a, b) => b.count - a.count);

    // 1. Avg session duration
    const totalMinutes = sessions.reduce((acc, s) => acc + (s.durationMinutes || 1), 0);
    const avgDuration = totalSessions > 0 ? (totalMinutes / totalSessions).toFixed(1) : '0';

    // 2. TV vs Phone
    const tvCount = sessions.filter((s) => s.deviceType === 'tv').length;
    const tvRatio = totalSessions > 0 ? Math.round((tvCount / totalSessions) * 100) : 0;

    // 3. Bounce rate
    const firstOpens = stats.firstOpens || 0;
    const bounced = stats.bouncedUsers || 0;
    const bounceRate = firstOpens > 0 ? Math.round((bounced / firstOpens) * 100) : 0;

    // 4. Stream health
    const totalPlays = stats.totalPlays || 0;
    const errors = stats.playbackErrors || 0;
    const streamHealth =
      totalPlays + errors > 0
        ? ((totalPlays / (totalPlays + errors)) * 100).toFixed(1)
        : '100';

    // 5. Unique Devices Aggregation (1 card per physical device)
    const deviceMap = new Map();
    for (const s of sessions) {
      const dId = s.deviceId || s.device || s.id;
      if (!deviceMap.has(dId)) {
        deviceMap.set(dId, {
          ...s,
          id: dId,
          deviceId: dId,
          sessionCount: 1,
          totalDurationMinutes: s.durationMinutes || 1,
          sessionsHistory: [s],
          isActiveDevice: s.status === 'active',
        });
      } else {
        const d = deviceMap.get(dId);
        d.sessionCount += 1;
        d.totalDurationMinutes += (s.durationMinutes || 1);
        d.sessionsHistory.push(s);
        if (s.status === 'active') {
          d.isActiveDevice = true;
          d.status = 'active';
          d.lastSeen = s.lastSeen || d.lastSeen;
          d.lastContent = s.lastContent || d.lastContent;
          d.lastSection = s.lastSection || d.lastSection;
          d.openTime = s.openTime || d.openTime;
          d.durationText = s.durationText || d.durationText;
        }
      }
    }
    const dayMs = 24 * 60 * 60 * 1000;
    let appDevicesCount = 0;
    let installedActive = 0;
    let installedIdle = 0;
    let uninstalledCount = 0;

    function cleanLocationStr(loc, city, region, country) {
      let s = (loc || (city ? `${city}، ${region ? region + '، ' : ''}${country || 'مصر (EG)'}` : country || 'مصر (EG)')).toString();
      const egCities = ['طنطا', 'القاهرة', 'الجيزة', 'الإسكندرية', 'المنصورة', 'الزقازيق', 'المحلة', 'دمنهور', 'بنها', 'سوهاج', 'أسيوط', 'المنيا', 'قنا', 'الأقصر', 'أسوان'];
      for (const egC of egCities) {
        if (s.includes(egC)) {
          s = s.replace(/السعودية\s*\(SA\)/g, 'مصر (EG)')
               .replace(/Medina Region/gi, '')
               .replace(/Riyadh/gi, '')
               .replace(/Saudi Arabia/gi, 'Egypt');
        }
      }
      return s.replace(/،\s*،/g, '،').replace(/^،\s*/, '').replace(/،\s*$/, '').trim();
    }

    const devices = Array.from(deviceMap.values()).map(d => {
      const isApp = d.platform !== 'web' && d.deviceType !== 'web';
      const lastSeenTime = new Date(d.lastSeen || d.openedAt || 0).getTime();
      const daysSince = Math.max(0, Math.floor((now - lastSeenTime) / dayMs));

      let installStatus = 'installed_active';
      let installStatusText = 'مثبت ونشط 🟢';

      if (isApp) {
        appDevicesCount++;
        if (d.isActiveDevice || daysSince <= 1) {
          installedActive++;
          installStatus = 'installed_active';
          installStatusText = 'مثبت ونشط 🟢';
        } else if (daysSince <= 7) {
          installedIdle++;
          installStatus = 'installed_idle';
          installStatusText = 'مثبت (خامل) 🟡';
        } else {
          uninstalledCount++;
          installStatus = 'uninstalled_churn';
          installStatusText = 'احتمال حذف التطبيق 🔴';
        }
      }

      const cleanLoc = cleanLocationStr(d.location, d.city, d.region, d.country);

      return {
        ...d,
        location: cleanLoc,
        status: d.isActiveDevice ? 'active' : 'closed',
        actionsCount: d.sessionsHistory.reduce((acc, x) => acc + (x.actionsCount || 0), 0),
        daysSinceLastSeen: daysSince,
        installStatus,
        installStatusText,
      };
    });

    const totalAppInstalls = Math.max(stats.totalInstalls || 0, appDevicesCount);
    // Real retention calculation (fixed truncation bug where limitToLast(600) forced 74% uninstalls):
    const sampleActiveRatio = appDevicesCount > 0 ? (installedActive + installedIdle) / appDevicesCount : 0.92;
    const stillInstalled = Math.min(totalAppInstalls, Math.max(installedActive + installedIdle, Math.round(totalAppInstalls * Math.min(sampleActiveRatio, 0.95))));
    uninstalledCount = Math.max(0, totalAppInstalls - stillInstalled);
    const retentionRate = totalAppInstalls > 0 ? ((stillInstalled / totalAppInstalls) * 100).toFixed(1) : '100.0';
    const uninstallRate = totalAppInstalls > 0 ? ((uninstalledCount / totalAppInstalls) * 100).toFixed(1) : '0.0';

    // 6. Device brands
    const brandMap = {};
    for (const s of sessions) {
      const dev = (s.device || '').toLowerCase();
      let brand = 'أخرى';
      if (dev.includes('xiaomi') || dev.includes('redmi') || dev.includes('poco')) brand = 'شاومي (Xiaomi)';
      else if (dev.includes('samsung') || dev.includes('galaxy') || dev.includes('sm-')) brand = 'سامسونج (Samsung)';
      else if (dev.includes('realme') || dev.includes('rmx')) brand = 'ريلمي (Realme)';
      else if (dev.includes('huawei') || dev.includes('honor')) brand = 'هواوي (Huawei)';
      else if (dev.includes('oppo')) brand = 'أوبو (Oppo)';
      else if (dev.includes('vivo')) brand = 'فيفو (Vivo)';
      else if (dev.includes('tecno')) brand = 'تكنو (Tecno)';
      else if (dev.includes('infinix')) brand = 'انفينيكس (Infinix)';
      else if (dev.includes('itel')) brand = 'ايتيل (Itel)';
      else if (dev.includes('tv') || dev.includes('box') || s.deviceType === 'tv') brand = 'شاشات TV';
      else if (dev.includes('android')) brand = 'أندرويد عام';
      brandMap[brand] = (brandMap[brand] || 0) + 1;
    }
    const deviceBrands = Object.entries(brandMap)
      .map(([brand, count]) => ({
        brand,
        count,
        percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
      }))
      .sort((a, b) => b.count - a.count);

    // 6. Countries
    const countryMap = {};
    for (const s of sessions) {
      const c = s.country || 'مصر (EG)';
      countryMap[c] = (countryMap[c] || 0) + 1;
    }
    const topCountries = Object.entries(countryMap)
      .map(([name, count]) => ({
        name,
        count,
        percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
      }))
      .sort((a, b) => b.count - a.count);

    // 6.b Detailed Cities Breakdown (المواقع والمدن بالتفصيل)
    const cityMap = {};
    for (const s of sessions) {
      const loc = cleanLocationStr(s.location, s.city, s.region, s.country);
      cityMap[loc] = (cityMap[loc] || 0) + 1;
    }
    const topCities = Object.entries(cityMap)
      .map(([name, count]) => ({
        name,
        count,
        percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
      }))
      .sort((a, b) => b.count - a.count);

    // 6.c Audience Segments Breakdown (شرائح الجمهور المصنفة للإعلانات)
    const segMap = {};
    for (const s of sessions) {
      const segName = s.audienceSegmentName || 'عشاق سينما الأكشن والإثارة 🎬';
      segMap[segName] = (segMap[segName] || 0) + 1;
    }
    const audienceSegments = Object.entries(segMap)
      .map(([name, count]) => ({
        name,
        count,
        percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
      }))
      .sort((a, b) => b.count - a.count);

    // 6.d Real Data Monetization & Video Ad Valuation (تسييل البيانات والأرباح التقديرية)
    const plays = stats.totalPlays || 0;
    const activeProfiles = totalSessions;
    const estDailyVastImpressions = plays;
    const ecpmAvgUsd = 5.0; // Market average eCPM for MENA pre-rolls ($3 - $8)
    const egpUsdRate = 50.0;
    const estDailyRevenueUsd = Number(((estDailyVastImpressions * ecpmAvgUsd) / 1000).toFixed(2));
    const estMonthlyRevenueUsd = Number((estDailyRevenueUsd * 30).toFixed(2));
    const estDailyRevenueEgp = Math.round(estDailyRevenueUsd * egpUsdRate);
    const estMonthlyRevenueEgp = Math.round(estMonthlyRevenueUsd * egpUsdRate);
    const dmpProfileRatePer1k = 1.5; // $1.50 per 1k profiles (LiveRamp, Lotame)
    const dmpMonthlyRevenueUsd = Number(((activeProfiles * dmpProfileRatePer1k) / 1000).toFixed(2));

    const monetization = {
      totalPlays: plays,
      activeProfiles,
      estDailyVastImpressions,
      ecpmAvgUsd,
      estDailyRevenueUsd,
      estMonthlyRevenueUsd,
      estDailyRevenueEgp,
      estMonthlyRevenueEgp,
      dmpMonthlyRevenueUsd,
      trendReportPriceUsd: 1000,
    };

    // 7. Hourly traffic
    const hourlyTraffic = Array(24).fill(0);
    for (const s of sessions) {
      if (s.openedAt) {
        const h = new Date(s.openedAt).getHours();
        if (h >= 0 && h < 24) hourlyTraffic[h]++;
      }
    }

    // 8. OS breakdown
    const osMap = {};
    for (const s of sessions) {
      const os = (s.osVersion || 'Android').split('(')[0].trim();
      osMap[os] = (osMap[os] || 0) + 1;
    }
    const osList = Object.entries(osMap)
      .map(([os, count]) => ({
        os,
        count,
        percent: totalSessions > 0 ? Math.round((count / totalSessions) * 100) : 0,
      }))
      .sort((a, b) => b.count - a.count);

    // 9. Top content with accurate dynamic icons
    const topContentObj = topContentSnap.val() || {};
    const topContent = Object.values(topContentObj)
      .map((item) => {
        let cat = (item.category || '').toLowerCase();
        let title = (item.title || '').toLowerCase();
        let icon = '🎬';
        if (cat.includes('بث') || cat.includes('مباراة') || cat.includes('رياضة') || cat.includes('sport') || title.includes('سيتي') || title.includes('مباراة') || title.includes('bein') || title.includes('كأس')) {
          icon = '⚽';
          cat = item.category || 'بث مباشر ورياضة';
        } else if (cat.includes('مسلسل') || title.includes('مسلسل') || title.includes('حلقة')) {
          icon = '📺';
          cat = item.category || 'مسلسلات';
        } else if (cat.includes('أنمي') || cat.includes('انمي') || title.includes('أنمي')) {
          icon = '⚔️';
          cat = item.category || 'أنمي';
        } else if (cat.includes('مسرح')) {
          icon = '🎭';
          cat = item.category || 'مسرحيات';
        } else {
          icon = '🎬';
          cat = item.category || 'أفلام';
        }
        return {
          ...item,
          category: cat,
          categoryIcon: icon,
        };
      })
      .sort((a, b) => b.views - a.views)
      .slice(0, 30);

    // 10. Searches
    const searchesObj = searchesSnap.val() || {};
    const searchesList = Object.values(searchesObj)
      .sort((a, b) => b.count - a.count)
      .slice(0, 50);

    // 11. Errors
    const errorsObj = errorsSnap.val() || {};
    const errorsList = Object.values(errorsObj).sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );

    // 12. Activity log
    const actLogObj = actLogSnap.val() || {};
    const activityLog = Object.values(actLogObj)
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
      .slice(0, 30);

    return sendJson(res, 200, {
      ok: true,
      updatedAt: new Date().toISOString(),
      kpis: {
        activeNow,
        appActiveNow,
        webActiveNow,
        firstOpens,
        totalInstalls: stats.totalInstalls || 0,
        totalAppInstalls,
        stillInstalled,
        uninstalledCount,
        installedActive,
        installedIdle,
        retentionRate,
        uninstallRate,
        webVisits: stats.webVisits || 0,
        bouncedUsers: bounced,
        bounceRate,
        totalSearches: stats.totalSearches || 0,
        totalPlays,
        playbackErrors: errors,
        streamHealth,
        avgDurationMinutes: avgDuration,
        tvRatio,
        phoneRatio: 100 - tvRatio,
        appSessionsCount: appSessions.length,
        webSessionsCount: webSessions.length,
        appRatio: totalSessions > 0 ? Math.round((appSessions.length / totalSessions) * 100) : 0,
        webRatio: totalSessions > 0 ? Math.round((webSessions.length / totalSessions) * 100) : 0,
      },
      analytics: {
        deviceBrands,
        topCountries,
        topCities,
        audienceSegments,
        hourlyTraffic,
        osList,
        webBrowsers,
        platforms: [
          { name: 'تطبيق الهاتف والشاشات 📱', count: appSessions.length, active: appActiveNow },
          { name: 'الموقع الإلكتروني (Web Portal) 🌐', count: webSessions.length, active: webActiveNow },
        ],
      },
      monetization,
      landingAnalytics: {
        siteUrl: 'zaafilm.com',
        siteName: 'موقع التحميل الرسمي ZAAFILM',
        status: 'active',
        statusText: 'نشط ومتصل بالسحابة',
        totalVisits: landingTotalVisits,
        downloadsBundle: landingBundleDownloads,
        downloadsApp: landingAppDownloads,
        downloadsPlayer: landingPlayerDownloads,
        totalDownloads: landingTotalDownloads,
        downloadsPerVisit: landingDownloadsPerVisit,
        conversionRate: landingConversionRate,
        topReferrers: landingTopReferrers,
        topCountries: landingCountries,
        recentVisitors: landingVisitors,
      },
      devices,
      sessions: sessions.map(s => ({
        ...s,
        location: cleanLocationStr(s.location, s.city, s.region, s.country),
      })),
      topContent,
      searches: searchesList,
      errors: errorsList,
      activityLog,
    });
  } catch (err) {
    console.error('stats error:', err);
    return sendJson(res, 500, { ok: false, error: err.message });
  }
};
