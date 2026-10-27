export interface FootballMatch {
  id: string;
  leagueName: string;
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

const LEAGUES: { code: string; name: string }[] = [
  { code: "eng.1", name: "الدوري الإنجليزي الممتاز" },
  { code: "esp.1", name: "الدوري الإسباني" },
  { code: "ita.1", name: "الدوري الإيطالي" },
  { code: "ger.1", name: "الدوري الألماني" },
  { code: "fra.1", name: "الدوري الفرنسي" },
  { code: "uefa.champions", name: "دوري أبطال أوروبا" },
  { code: "uefa.europa", name: "الدوري الأوروبي" },
  { code: "sau.1", name: "دوري روشن السعودي" },
  { code: "egy.1", name: "الدوري المصري" },
];

let cachedMatches: FootballMatch[] = [];
let lastFetchTime = 0;
const CACHE_TTL = 30 * 1000; // 30 seconds

export async function getLiveMatches(force = false): Promise<FootballMatch[]> {
  const now = Date.now();
  if (!force && cachedMatches.length > 0 && now - lastFetchTime < CACHE_TTL) {
    return cachedMatches;
  }

  try {
    const promises = LEAGUES.map(async (league) => {
      try {
        const url = `https://site.api.espn.com/apis/site/v2/sports/soccer/${league.code}/scoreboard`;
        const res = await fetch(url, {
          headers: {
            "User-Agent": "Mozilla/5.0",
            Accept: "application/json",
          },
          cache: "no-store",
        });

        if (!res.ok) return [];

        const json = await res.json();
        const events = Array.isArray(json.events) ? json.events : [];

        return events.map((event: any): FootballMatch => {
          const competition = event.competitions?.[0] || {};
          const competitors = competition.competitors || [];
          const home = competitors.find((c: any) => c.homeAway === "home") || competitors[0] || {};
          const away = competitors.find((c: any) => c.homeAway === "away") || competitors[1] || {};

          const statusType = event.status?.type || {};
          const state = statusType.state || "pre"; // "pre", "in", "post"
          const displayClock = event.status?.displayClock || "";

          let statusText = "قريباً";
          if (state === "in") {
            statusText = displayClock ? `${displayClock}' مباشر` : "مباشر الآن 🔴";
          } else if (state === "post") {
            statusText = "انتهت";
          } else {
            // format time
            const date = new Date(event.date);
            statusText = date.toLocaleTimeString("ar-EG", {
              hour: "2-digit",
              minute: "2-digit",
              hour12: true,
            });
          }

          return {
            id: event.id || Math.random().toString(),
            leagueName: league.name,
            homeTeam: home.team?.displayName || home.team?.name || "الفريق المضيف",
            homeLogo: home.team?.logo || "",
            homeScore: home.score !== undefined ? String(home.score) : "-",
            awayTeam: away.team?.displayName || away.team?.name || "الفريق الضيف",
            awayLogo: away.team?.logo || "",
            awayScore: away.score !== undefined ? String(away.score) : "-",
            status: state,
            statusText,
            matchTime: event.date || "",
            minute: displayClock,
          };
        });
      } catch {
        return [];
      }
    });

    const results = await Promise.all(promises);
    const flattened = results.flat();

    // Sort: live matches first, then upcoming, then finished
    flattened.sort((a, b) => {
      const order = { in: 0, pre: 1, post: 2 };
      const aOrder = order[a.status as keyof typeof order] ?? 3;
      const bOrder = order[b.status as keyof typeof order] ?? 3;
      if (aOrder !== bOrder) return aOrder - bOrder;
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
