export interface FootballMatch {
  id: string;
  leagueName: string;
  leagueCode: string;
  homeTeam: string;
  homeLogo: string;
  homeScore: string;
  awayTeam: string;
  awayLogo: string;
  awayScore: string;
  status: string; // "pre" | "in" | "post"
  statusText: string;
  matchTime: string;
  minute?: string;
}

export const TOP_LEAGUES: { code: string; name: string }[] = [
  { code: "eng.1", name: "الدوري الإنجليزي الممتاز" },
  { code: "esp.1", name: "الدوري الإسباني" },
  { code: "ita.1", name: "الدوري الإيطالي" },
  { code: "ger.1", name: "الدوري الألماني" },
  { code: "fra.1", name: "الدوري الفرنسي" },
  { code: "uefa.champions", name: "دوري أبطال أوروبا" },
];

const ARABIC_TEAMS: Record<string, string> = {
  // Premier League
  "Arsenal": "آرسنال",
  "Manchester City": "مانشستر سيتي",
  "Liverpool": "ليفربول",
  "Manchester United": "مانشستر يونايتد",
  "Chelsea": "تشيلسي",
  "Tottenham Hotspur": "توتنهام",
  "Newcastle United": "نيوكاسل",
  "Aston Villa": "أستون فيلا",
  "West Ham United": "وست هام",
  "Brighton & Hove Albion": "برايتون",
  "Everton": "إيفرتون",
  "Wolverhampton Wanderers": "وولفرهامبتون",
  "Crystal Palace": "كريستال بالاس",
  "Brentford": "برينتفورد",
  "Fulham": "فولهام",
  "Bournemouth": "بورنموث",
  "AFC Bournemouth": "بورنموث",
  "Nottingham Forest": "نوتينغهام فورست",
  "Leicester City": "ليستر سيتي",
  "Ipswich Town": "إبسويتش تاون",
  "Southampton": "ساوثهامبتون",
  "Leeds United": "ليدز يونايتد",
  "Sunderland": "سندرلاند",
  "Coventry City": "كوفنتري",
  "Hull City": "هال سيتي",

  // La Liga
  "Real Madrid": "ريال مدريد",
  "FC Barcelona": "برشلونة",
  "Barcelona": "برشلونة",
  "Atlético Madrid": "أتلتيكو مدريد",
  "Atletico Madrid": "أتلتيكو مدريد",
  "Sevilla": "إشبيلية",
  "Real Betis": "ريال بيتيس",
  "Real Sociedad": "ريال سوسيداد",
  "Athletic Club": "أتلتيك بيلباو",
  "Villarreal": "فياريال",
  "Valencia": "فالنسيا",
  "Girona": "جيرونا",
  "Osasuna": "أوساسونا",
  "Celta Vigo": "سلتا فيغو",
  "Espanyol": "إسبانيول",
  "Mallorca": "مايوركا",
  "Rayo Vallecano": "رايو فاليكانو",
  "Getafe": "خيتافي",
  "Las Palmas": "لاس بالماس",
  "Alavés": "ألافيس",
  "Leganés": "ليغانيس",
  "Real Valladolid": "بلد الوليد",

  // Serie A
  "Inter Milan": "إنتر ميلان",
  "AC Milan": "ميلان",
  "Juventus": "يوفنتوس",
  "Napoli": "نابولي",
  "AS Roma": "روما",
  "Roma": "روما",
  "Lazio": "لاتسيو",
  "Atalanta": "أتالانتا",
  "Fiorentina": "فيورنتينا",
  "Bologna": "بولونيا",
  "Torino": "تورينو",
  "Monza": "مونزا",
  "Genoa": "جنوى",
  "Udinese": "أودينيزي",
  "Cagliari": "كالياري",
  "Parma": "بارما",
  "Como": "كومو",
  "Empoli": "إمبولي",
  "Hellas Verona": "هيلاس فيرونا",
  "Venezia": "فينيسيا",
  "Lecce": "ليتشي",
  "Sassuolo": "ساسولو",

  // Bundesliga
  "Bayern Munich": "بايرن ميونخ",
  "Borussia Dortmund": "بوروسيا دورتموند",
  "Bayer Leverkusen": "باير ليفركوزن",
  "RB Leipzig": "لايبزيغ",
  "Eintracht Frankfurt": "آينتراخت فرانكفورت",
  "VfB Stuttgart": "شتوتغارت",
  "Borussia Mönchengladbach": "مونشنغلادباخ",
  "VfL Wolfsburg": "فولفسبورغ",
  "SC Freiburg": "فرايبورغ",
  "Werder Bremen": "فيردر بريمن",
  "FC Augsburg": "أوغسبورغ",
  "Union Berlin": "يونيون برلين",
  "1. FC Heidenheim 1846": "هايدنهايم",
  "Mainz": "ماينز",
  "1. FSV Mainz 05": "ماينز",
  "St. Pauli": "سانت باولي",
  "Holstein Kiel": "هولشتاين كيل",
  "VfL Bochum": "بوخوم",
  "SV Elversberg": "إلفيرسبيرغ",

  // Ligue 1
  "Paris Saint-Germain": "باريس سان جيرمان",
  "Marseille": "مارسيليا",
  "Monaco": "موناكو",
  "Lyon": "ليون",
  "Lille": "ليل",
  "Nice": "نيس",
  "Lens": "لانس",
  "Rennes": "رين",
  "Stade de Reims": "ستاد ريمس",
  "Strasbourg": "ستراسبورغ",
  "Toulouse": "تولوز",
  "Nantes": "نانت",
  "Montpellier": "مونبلييه",
  "Brest": "بريست",
  "Auxerre": "أوكسير",
  "Angers": "أنجيه",
  "Saint-Étienne": "سانت إتيان",
  "Le Havre AC": "لو هافر",

  // UCL Teams
  "Galatasaray": "غلطة سراي",
  "Fenerbahce": "فنربخشة",
  "Besiktas": "بشكتاش",
  "Sporting CP": "سبورتينغ لشبونة",
  "Benfica": "بنفيكا",
  "FC Porto": "بورتو",
  "Ajax": "أياكس",
  "PSV Eindhoven": "بي إس في آيندهوفن",
  "Feyenoord": "فينورد",
  "Celtic": "سلتيك",
  "Rangers": "رينجرز",
  "Club Brugge": "كلوب بروج",
  "Shakhtar Donetsk": "شاختار",
  "Dynamo Kyiv": "دينامو كييف",
  "Red Star Belgrade": "النجم الأحمر",
  "Sparta Prague": "سبارتا براغ",
  "Young Boys": "يانغ بويز",
  "Dinamo Zagreb": "دينامو زغرب",
  "Sturm Graz": "شتورم غراتس",
  "AEK Athens": "أيك أثينا",
  "Bodo/Glimt": "بودو غليمت",
};

function toArabic(name: string): string {
  if (!name) return "";
  if (ARABIC_TEAMS[name]) return ARABIC_TEAMS[name];
  for (const [en, ar] of Object.entries(ARABIC_TEAMS)) {
    if (name.toLowerCase().includes(en.toLowerCase())) return ar;
  }
  return name;
}

function formatStatus(event: any): { state: string; statusText: string; minute: string } {
  const statusType = event.status?.type || {};
  const state = statusType.state || "pre"; // "pre", "in", "post"
  const displayClock = event.status?.displayClock || "";

  let statusText = "قريباً";
  if (state === "in") {
    statusText = displayClock ? `${displayClock}' مباشر 🔴` : "مباشر الآن 🔴";
  } else if (state === "post") {
    statusText = "انتهت";
  } else {
    try {
      const date = new Date(event.date);
      const days = ["الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"];
      const dayName = days[date.getDay()];
      const timeStr = date.toLocaleTimeString("ar-EG", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: true,
      });
      const today = new Date();
      if (date.toDateString() === today.toDateString()) {
        statusText = `اليوم ${timeStr}`;
      } else {
        statusText = `${dayName} ${timeStr}`;
      }
    } catch {
      statusText = "قريباً";
    }
  }

  return { state, statusText, minute: displayClock };
}

function parseEspnEvent(event: any, leagueCode: string, leagueName: string): FootballMatch | null {
  const competition = event.competitions?.[0] || {};
  const competitors = competition.competitors || [];
  if (competitors.length < 2) return null;

  const home = competitors.find((c: any) => c.homeAway === "home") || competitors[0] || {};
  const away = competitors.find((c: any) => c.homeAway === "away") || competitors[1] || {};

  const rawHomeName = home.team?.displayName || home.team?.name || "الفريق المضيف";
  const rawAwayName = away.team?.displayName || away.team?.name || "الفريق الضيف";

  const { state, statusText, minute } = formatStatus(event);

  return {
    id: event.id || Math.random().toString(),
    leagueName,
    leagueCode,
    homeTeam: toArabic(rawHomeName),
    homeLogo: home.team?.logo || "",
    homeScore: home.score !== undefined ? String(home.score) : "-",
    awayTeam: toArabic(rawAwayName),
    awayLogo: away.team?.logo || "",
    awayScore: away.score !== undefined ? String(away.score) : "-",
    status: state,
    statusText,
    matchTime: event.date || "",
    minute,
  };
}

let cachedMatches: FootballMatch[] = [];
let lastFetchTime = 0;
const CACHE_TTL = 45 * 1000; // 45 seconds

export async function getLiveMatches(force = false): Promise<FootballMatch[]> {
  const now = Date.now();
  if (!force && cachedMatches.length > 0 && now - lastFetchTime < CACHE_TTL) {
    return cachedMatches;
  }

  const todayIso = new Date().toISOString().slice(0, 10);

  try {
    const promises = TOP_LEAGUES.map(async (league) => {
      try {
        const defaultUrl = `https://site.api.espn.com/apis/site/v2/sports/soccer/${league.code}/scoreboard`;
        const res = await fetch(defaultUrl, {
          headers: { "User-Agent": "Mozilla/5.0", Accept: "application/json" },
          cache: "no-store",
        });

        if (!res.ok) return [];
        const json = await res.json();
        const rawEvents = Array.isArray(json.events) ? json.events : [];
        const allRawEvents = [...rawEvents];

        // Check calendar for upcoming fixtures if needed
        const cal = json.leagues?.[0]?.calendar || [];
        const futureDates = cal
          .filter((d: any) => typeof d === "string" && d.slice(0, 10) >= todayIso)
          .map((d: any) => d.slice(0, 10).replace(/-/g, ""));

        if (futureDates.length > 0) {
          const nextDateStr = futureDates[0];
          try {
            const upRes = await fetch(
              `https://site.api.espn.com/apis/site/v2/sports/soccer/${league.code}/scoreboard?dates=${nextDateStr}`,
              {
                headers: { "User-Agent": "Mozilla/5.0", Accept: "application/json" },
                cache: "no-store",
              }
            );
            if (upRes.ok) {
              const upJson = await upRes.json();
              if (Array.isArray(upJson.events)) {
                allRawEvents.push(...upJson.events);
              }
            }
          } catch {}
        } else if (league.code === "uefa.champions") {
          // Dynamic lookup for next UCL matchdays
          const nowDt = new Date();
          const tuesdays: string[] = [];
          for (let i = 1; i <= 28; i++) {
            const d = new Date(nowDt.getTime() + i * 86400000);
            if (d.getDay() === 2) {
              const y = d.getFullYear();
              const m = String(d.getMonth() + 1).padStart(2, "0");
              const day = String(d.getDate()).padStart(2, "0");
              tuesdays.push(`${y}${m}${day}`);
            }
          }

          for (const tStr of tuesdays.slice(0, 3)) {
            try {
              const uclRes = await fetch(
                `https://site.api.espn.com/apis/site/v2/sports/soccer/${league.code}/scoreboard?dates=${tStr}`,
                {
                  headers: { "User-Agent": "Mozilla/5.0", Accept: "application/json" },
                  cache: "no-store",
                }
              );
              if (uclRes.ok) {
                const uclJson = await uclRes.json();
                if (Array.isArray(uclJson.events) && uclJson.events.length > 0) {
                  allRawEvents.push(...uclJson.events);
                  break;
                }
              }
            } catch {}
          }
        }

        // Parse and deduplicate
        const seenIds = new Set<string>();
        const parsed: FootballMatch[] = [];

        for (const ev of allRawEvents) {
          if (!ev?.id || seenIds.has(String(ev.id))) continue;
          seenIds.add(String(ev.id));
          const match = parseEspnEvent(ev, league.code, league.name);
          if (match) parsed.push(match);
        }

        return parsed;
      } catch {
        return [];
      }
    });

    const results = await Promise.all(promises);
    const flattened = results.flat();

    // Sort: live first, then upcoming by date ascending, then finished by date descending
    flattened.sort((a, b) => {
      const order = { in: 0, pre: 1, post: 2 };
      const aOrder = order[a.status as keyof typeof order] ?? 3;
      const bOrder = order[b.status as keyof typeof order] ?? 3;
      if (aOrder !== bOrder) return aOrder - bOrder;

      if (a.status === "post" && b.status === "post") {
        return new Date(b.matchTime).getTime() - new Date(a.matchTime).getTime();
      }
      return new Date(a.matchTime).getTime() - new Date(b.matchTime).getTime();
    });

    cachedMatches = flattened;
    lastFetchTime = now;
    return cachedMatches;
  } catch (err) {
    console.error("Football matches error:", err);
    return cachedMatches;
  }
}
