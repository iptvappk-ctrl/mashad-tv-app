export interface YacineCategory {
  id: string | number;
  name: string;
  logo: string;
  childCount: number;
}

export interface YacineChannel {
  id: string | number;
  name: string;
  logo: string;
  isHide: number;
  priority: number;
}

export interface YacineTeam {
  id: string | number;
  name: string;
  logo: string;
}

export interface YacineEvent {
  id: string | number;
  champions: string;
  channel: string;
  startTime: number;
  endTime: number;
  commentary: string;
  team1: YacineTeam;
  team2: YacineTeam;
}

export interface YacineStream {
  name: string;
  url: string;
  userAgent: string;
  referer: string;
  urlType: number;
  headers: Record<string, string>;
}

// ── Built-in High Quality Categories ──
export const DEFAULT_CATEGORIES: YacineCategory[] = [
  {
    id: "bein_sports",
    name: "beIN SPORTS",
    logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_1_Digital_Mono.png",
    childCount: 20,
  },
];

// ── beIN Sports Channels extracted from Yacine TV ──
export const BEIN_FALLBACK_CHANNELS: YacineChannel[] = [
  { id: "1143098968567368033", name: "beIN SPORTS 1", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS1_DIGITAL_Mono.png", isHide: 0, priority: 25 },
  { id: "1143098968566343572", name: "beIN SPORTS 2", logo: "https://assets.bein.com/mena/sites/4/2021/02/beIN_SPORTS2_DIGITAL_Mono.png", isHide: 0, priority: 24 },
  { id: "1143098968569416219", name: "beIN SPORTS 3", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS3_DIGITAL_Mono.png", isHide: 0, priority: 23 },
  { id: "1143098968568628598", name: "beIN SPORTS 4", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS4_DIGITAL_Mono.png", isHide: 0, priority: 22 },
  { id: "1143098968563317197", name: "beIN SPORTS 5", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS5_DIGITAL_Mono.png", isHide: 0, priority: 21 },
  { id: "1143098968562474496", name: "beIN SPORTS 6", logo: "https://assets.bein.com/mena/sites/4/2021/02/beIN_SPORTS6_DIGITAL_Mono.png", isHide: 0, priority: 20 },
  { id: "1143098968565447332", name: "beIN SPORTS 7", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS7_DIGITAL_Mono.png", isHide: 0, priority: 19 },
  { id: "1143098968564573297", name: "beIN SPORTS 8", logo: "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTS8.png", isHide: 0, priority: 18 },
  { id: "1143098968559140230", name: "beIN SPORTS 9", logo: "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTS9.png", isHide: 0, priority: 17 },
  { id: "1143098968570920632", name: "beIN XTRA 1", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_XTRA1_Digital_Mono.png", isHide: 0, priority: 16 },
  { id: "1143098968573688335", name: "beIN XTRA 2", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_XTRA2_Digital_Mono.png", isHide: 0, priority: 15 },
  { id: "1143098968572456170", name: "beIN XTRA 3", logo: "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTSXTRA3.png", isHide: 0, priority: 14 },
  { id: "1143098968558341739", name: "beIN France 1", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS1_FRENCH_Digital_Mono.png", isHide: 0, priority: 13 },
  { id: "1143098968561167816", name: "beIN France 2", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS2_FRENCH_Digital_Mono.png", isHide: 0, priority: 12 },
  { id: "1143098968560204757", name: "beIN France 3", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS3_FRENCH_Digital_Mono.png", isHide: 0, priority: 11 },
  { id: "1143098968589111354", name: "beIN News", logo: "https://assets.bein.com/mena/sites/4/2015/06/NEWS_DIGITAL_Mono.png", isHide: 0, priority: 10 },
  { id: "1143098968587560095", name: "beIN SPORTS", logo: "https://assets.bein.com/mena/sites/4/2015/06/bein_SPORTS_FTA_DIGITAL_Mono.png", isHide: 0, priority: 9 },
  { id: "1143098968590638860", name: "beIN English 1", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS1_ENGLISH_Digital_Mono.png", isHide: 0, priority: 8 },
  { id: "1143098968590149209", name: "beIN English 2", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS2_ENGLISH_Digital_Mono.png", isHide: 0, priority: 7 },
  { id: "1143098968584995693", name: "beIN English 3", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS3_ENGLISH_Digital_Mono.png", isHide: 0, priority: 6 },
];

// ── Real Matches extracted from Yacine TV ──
export const REAL_YACINE_EVENTS: YacineEvent[] = [
  {
    id: "1452951978498025827",
    champions: "الدوري الإنجليزي الممتاز",
    channel: "beIN SPORTS 1",
    startTime: 1795264200,
    endTime: 1795271400,
    commentary: "علي محمد علي",
    team1: { id: "38", name: "مان سيتي", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/z44l-a0W1v5FmgPnemV6Xw_96x96.png" },
    team2: { id: "45", name: "فولهام", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/Gh7_5p3n364p4vxeM8FhNg_96x96.png" }
  },
  {
    id: "1452951978486950270",
    champions: "تشامبيونشيب البطولة الإنجليزية",
    channel: "beIN SPORTS 3",
    startTime: 1795264200,
    endTime: 1795271400,
    commentary: "أحمد عبده",
    team1: { id: "835", name: "ميلوول", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/FjQESJIwYuTRJ5B7HvtzPA_96x96.png" },
    team2: { id: "681", name: "كوينز بارك رينجرز", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/SYBiHsr8jq3vT4p0HauolA_96x96.png" }
  },
  {
    id: "1452951978505231935",
    champions: "الدوري الإسباني",
    channel: "beIN SPORTS 9",
    startTime: 1795266000,
    endTime: 1795273200,
    commentary: "محمد بركات",
    team1: { id: "188", name: "الافيس", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/optimized/RUcTOeFcBc7q7uku1nR_iQ_96x96.png" },
    team2: { id: "945", name: "لاكورونيا", logo: "https://toppng.com/uploads/preview/rc-deportivo-la-coruna-vector-logo-11574288712ywghhf3zec.png" }
  },
  {
    id: "1452951978484787804",
    champions: "الدوري التركي",
    channel: "beIN SPORTS 2",
    startTime: 1795266000,
    endTime: 1795273200,
    commentary: "نوفل باشي",
    team1: { id: "470", name: "غلطة سراي", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/TWjoccvTU4JXZJZ3aW3cVg_96x96.png" },
    team2: { id: "910", name: "سامسون سبور", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/lF7bCDr-Ai-fCbNZnfcxyA_96x96.png" }
  },
  {
    id: "1452951978511059366",
    champions: "الدوري الإنجليزي الممتاز",
    channel: "beIN SPORTS 4",
    startTime: 1795273200,
    endTime: 1795280400,
    commentary: "جواد بدة",
    team1: { id: "48", name: "استون فيلا", logo: "https://upload.wikimedia.org/wikipedia/fr/thumb/1/14/Aston_Villa_FC_2024.svg/langfr-250px-Aston_Villa_FC_2024.svg.png" },
    team2: { id: "555", name: "سندرلاند", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/CQFeTfHrtxqgr3VKWtTwfA_96x96.png" }
  },
  {
    id: "1452951978512458741",
    champions: "الدوري الإنجليزي الممتاز",
    channel: "beIN SPORTS 5",
    startTime: 1795273200,
    endTime: 1795280400,
    commentary: "محمد السعدي",
    team1: { id: "50", name: "بورنموث", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/IcOt-hrK04B-RlRwI3R0yA_96x96.png" },
    team2: { id: "54", name: "نوتينغهام فورست", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/Zr6FbE-8pDH7UBpWCO8U9A_96x96.png" }
  },
  {
    id: "1452951978510194059",
    champions: "الدوري الإنجليزي الممتاز",
    channel: "beIN SPORTS 1",
    startTime: 1795273200,
    endTime: 1795280400,
    commentary: "علي الكعبي",
    team1: { id: "44", name: "تشيلسي", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/fhBITrIlbQxhVB6IjxUO6Q_96x96.png" },
    team2: { id: "51", name: "ليدز يونايتد", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/5dqfOKpjjW6EwTAx_FysKQ_96x96.png" }
  },
  {
    id: "1452951978500011204",
    champions: "الدوري الإنجليزي الممتاز",
    channel: "beIN SPORTS 6",
    startTime: 1795273200,
    endTime: 1795280400,
    commentary: "محمد نضال",
    team1: { id: "577", name: "كوفنتري سيتي", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/KHpmY4tIwqiutl8Cfl0MAw_96x96.png" },
    team2: { id: "47", name: "كريستال بلاس", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/8piQOzndGmApKYTcvyN9vA_96x96.png" }
  },
  {
    id: "1452951978499099665",
    champions: "الدوري الإنجليزي الممتاز",
    channel: "beIN SPORTS 3",
    startTime: 1795273200,
    endTime: 1795280400,
    commentary: "عبدالله الغامدي",
    team1: { id: "40", name: "توتنهام", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/k3Q_mKE98Dnohrcea0JFgQ_96x96.png" },
    team2: { id: "546", name: "إيبسويتش تاون", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/56vquJBk5U16Dng7txLXCw_96x96.png" }
  },
  {
    id: "1452951978507203162",
    champions: "الدوري الإسباني",
    channel: "beIN SPORTS 9",
    startTime: 1795274100,
    endTime: 1795281300,
    commentary: "بكر علوان",
    team1: { id: "10", name: "رايو فاليكانو", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/i5LifmxEVIl0sbvIysiyhw_96x96.png" },
    team2: { id: "14", name: "فالنسيا", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/QPbjvDwI_0Wuu4tCS2O6uw_96x96.png" }
  },
  {
    id: "1452951978486227561",
    champions: "الدوري التركي",
    channel: "beIN SPORTS 7",
    startTime: 1795276800,
    endTime: 1795284000,
    commentary: "أحمد البلوشي",
    team1: { id: "959", name: "كوجالي سبور", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/optimized/Z0OjkzD4X_FSNX4zUvEb4A_96x96.png" },
    team2: { id: "322", name: "فنربخشه", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/HOQreZgFrmtQiLi8HKb9zA_96x96.png" }
  },
  {
    id: "1452951978487367491",
    champions: "الدوري الفرنسي",
    channel: "beIN SPORTS 2",
    startTime: 1795277700,
    endTime: 1795284900,
    commentary: "حسن العيدروس",
    team1: { id: "89", name: "نيس", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/Llrxrqsc3Tw4JzE6xM7GWw_96x96.png" },
    team2: { id: "81", name: "باريس", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/mcpMspef1hwHwi9qrfp4YQ_96x96.png" }
  },
  {
    id: "1452951978507731705",
    champions: "الدوري الإسباني",
    channel: "beIN SPORTS 3",
    startTime: 1795282200,
    endTime: 1795289400,
    commentary: "سمير اليعقوبي",
    team1: { id: "26", name: "اشبيلية", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/hCTs5EX3WjCMC5Jl3QE4Rw_96x96.png" },
    team2: { id: "8", name: "ريال بيتيس", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/S0fDZjYYytbZaUt0f3cIhg_96x96.png" }
  },
  {
    id: "1452951978512563432",
    champions: "الدوري الإنجليزي الممتاز",
    channel: "beIN SPORTS 1",
    startTime: 1795282200,
    endTime: 1795289400,
    commentary: "حفيظ دراجي",
    team1: { id: "39", name: "نيوكاسل", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/96CcNNQ0AYDAbssP0V9LuQ_96x96.png" },
    team2: { id: "37", name: "ارسنال", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/4us2nCgl6kgZc0t3hpW75Q_96x96.png" }
  },
  {
    id: "1452951978480518048",
    champions: "الدوري الفرنسي",
    channel: "beIN SPORTS 5",
    startTime: 1795290300,
    endTime: 1795297500,
    commentary: "محمد بركات",
    team1: { id: "99", name: "ستراسبورغ", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/Eb9xtMpUy8FXQ0RCKvLxcg_96x96.png" },
    team2: { id: "96", name: "بريست", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/HNqZwlu71GHXo60XjYrPxg_96x96.png" }
  },
  {
    id: "1452951978481735021",
    champions: "الدوري الفرنسي",
    channel: "beIN SPORTS 4",
    startTime: 1795290300,
    endTime: 1795297500,
    commentary: "جواد بدة",
    team1: { id: "86", name: "موناكو", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/RX0XTi5Dtg4joMtuHNmYKg_96x96.png" },
    team2: { id: "97", name: "اوكسير", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/jX0oM3wnm6cWOGKoUR3AyA_96x96.png" }
  },
  {
    id: "1452951978482769458",
    champions: "الدوري الفرنسي",
    channel: "beIN SPORTS 2",
    startTime: 1795290300,
    endTime: 1795297500,
    commentary: "عبدالله الغامدي",
    team1: { id: "87", name: "ليل", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/D2AQe8qoyPIP4K8MzLvwuA_96x96.png" },
    team2: { id: "88", name: "ليون", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/SrKK55dUkCxe4mJsyshfCg_96x96.png" }
  },
  {
    id: "1452951978509239980",
    champions: "الدوري الإسباني",
    channel: "beIN SPORTS 1",
    startTime: 1795291200,
    endTime: 1795298400,
    commentary: "أحمد البلوشي",
    team1: { id: "4", name: "ريال مدريد", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/Th4fAVAZeCJWRcKoLW7koA_96x96.png" },
    team2: { id: "24", name: "سيلتا فيغو", logo: "https://ssl.gstatic.com/onebox/media/sports/logos/optimized/wpdhixHP_sloegfP0UlwAw_96x96.png" }
  },
];

// ── Working Live Streams for beIN Sports ──
export const DEFAULT_BEIN_STREAMS: YacineStream[] = [
  {
    name: "beIN SPORTS HD (سيرفر رئيسي 1 فائق السرعة CDN)",
    url: "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: { "User-Agent": "Mozilla/5.0" },
  },
  {
    name: "beIN SPORTS XTRA HD (سيرفر مباشر 2 CDN)",
    url: "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: { "User-Agent": "Mozilla/5.0" },
  },
  {
    name: "beIN SPORTS XTRA (سيرفر بديل 3 CDN)",
    url: "https://bein-xtra-bein.amagi.tv/playlist.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: { "User-Agent": "Mozilla/5.0" },
  },
  {
    name: "beIN SPORTS Live (سيرفر احتياطي 4)",
    url: "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: { "User-Agent": "Mozilla/5.0" },
  },
];

export async function getYacineCategories(_force = false): Promise<YacineCategory[]> {
  return DEFAULT_CATEGORIES;
}

export async function getYacineChannels(_categoryId: string | number, _force = false): Promise<YacineChannel[]> {
  return BEIN_FALLBACK_CHANNELS;
}

export async function getYacineEvents(_force = false): Promise<YacineEvent[]> {
  return REAL_YACINE_EVENTS;
}

export async function getStreamsForChannel(channelId: number | string): Promise<YacineStream[]> {
  const idStr = String(channelId).toLowerCase();
  const matched = BEIN_FALLBACK_CHANNELS.find(ch => String(ch.id).toLowerCase() === idStr || ch.name.toLowerCase().includes(idStr));
  const channelName = matched ? matched.name : "beIN SPORTS HD";
  return DEFAULT_BEIN_STREAMS.map(s => ({ ...s, name: `${channelName} · ${s.name}` }));
}

export async function getStreamsForEvent(eventId: number | string): Promise<YacineStream[]> {
  const ev = REAL_YACINE_EVENTS.find(e => String(e.id) === String(eventId));
  const chName = ev?.channel || "beIN SPORTS 1";
  return DEFAULT_BEIN_STREAMS.map(s => ({ ...s, name: `${chName} · ${s.name}` }));
}
