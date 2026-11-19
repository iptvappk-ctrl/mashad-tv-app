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

// ── Built-in High Quality Categories (mirrored from YacineRepository.kt) ──
export const DEFAULT_CATEGORIES: YacineCategory[] = [
  {
    id: "bein_sports",
    name: "beIN SPORTS",
    logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_1_Digital_Mono.png",
    childCount: 11,
  },
  {
    id: "sports_arab",
    name: "قنوات رياضية عربية",
    logo: "https://shoof.alkass.net/shoof/assets/images/logo.png",
    childCount: 9,
  },
];

// ── beIN Sports Channels ──
export const BEIN_FALLBACK_CHANNELS: YacineChannel[] = [
  { id: "bein_hd1", name: "beIN SPORTS 1 HD", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS1_DIGITAL_Mono.png", isHide: 0, priority: 11 },
  { id: "bein_hd2", name: "beIN SPORTS 2 HD", logo: "https://assets.bein.com/mena/sites/4/2021/02/beIN_SPORTS2_DIGITAL_Mono.png", isHide: 0, priority: 10 },
  { id: "bein_hd3", name: "beIN SPORTS 3 HD", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS3_DIGITAL_Mono.png", isHide: 0, priority: 9 },
  { id: "bein_hd4", name: "beIN SPORTS 4 HD", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS4_DIGITAL_Mono.png", isHide: 0, priority: 8 },
  { id: "bein_hd5", name: "beIN SPORTS 5 HD", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS5_DIGITAL_Mono.png", isHide: 0, priority: 7 },
  { id: "bein_hd6", name: "beIN SPORTS 6 HD", logo: "https://assets.bein.com/mena/sites/4/2021/02/beIN_SPORTS6_DIGITAL_Mono.png", isHide: 0, priority: 6 },
  { id: "bein_hd7", name: "beIN SPORTS 7 HD", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS7_DIGITAL_Mono.png", isHide: 0, priority: 5 },
  { id: "bein_hd8", name: "beIN SPORTS 8 HD", logo: "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTS8.png", isHide: 0, priority: 4 },
  { id: "bein_hd9", name: "beIN SPORTS 9 HD", logo: "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTS9.png", isHide: 0, priority: 3 },
  { id: "bein_xtra1", name: "beIN SPORTS XTRA 1", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_XTRA1_Digital_Mono.png", isHide: 0, priority: 2 },
  { id: "bein_xtra2", name: "beIN SPORTS XTRA 2", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_XTRA2_Digital_Mono.png", isHide: 0, priority: 1 },
];

// ── Arab Sports Channels (Alkass, Kuwait, Bahrain, Oman, Jordan, Iraq) ──
export const ARAB_SPORTS_CHANNELS: YacineChannel[] = [
  { id: "alkass_1", name: "الكأس 1 HD", logo: "https://shoof.alkass.net/shoof/assets/images/logo.png", isHide: 0, priority: 12 },
  { id: "alkass_2", name: "الكأس 2 HD", logo: "https://shoof.alkass.net/shoof/assets/images/logo.png", isHide: 0, priority: 11 },
  { id: "arab_kuwait", name: "الكويت الرياضية HD", logo: "https://media.gov.kw/Images/Logo.png", isHide: 0, priority: 10 },
  { id: "arab_kuwait_plus", name: "الكويت بلس الرياضية HD", logo: "https://media.gov.kw/Images/Logo.png", isHide: 0, priority: 9 },
  { id: "arab_bahrain_1", name: "البحرين الرياضية 1 HD", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS1_DIGITAL_Mono.png", isHide: 0, priority: 8 },
  { id: "arab_bahrain_2", name: "البحرين الرياضية 2 HD", logo: "https://assets.bein.com/mena/sites/4/2021/02/beIN_SPORTS2_DIGITAL_Mono.png", isHide: 0, priority: 7 },
  { id: "arab_oman", name: "عمان الرياضية HD", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS3_DIGITAL_Mono.png", isHide: 0, priority: 6 },
  { id: "arab_jordan", name: "الأردن الرياضية HD", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS4_DIGITAL_Mono.png", isHide: 0, priority: 5 },
  { id: "arab_iraq", name: "العراقية الرياضية HD", logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS5_DIGITAL_Mono.png", isHide: 0, priority: 4 },
];

// ── Working Live Streams for beIN Sports ──
export const DEFAULT_BEIN_STREAMS: YacineStream[] = [
  {
    name: "beIN SPORTS XTRA HD (فائق السرعة ⚡)",
    url: "https://bein-xtra-bein.amagi.tv/playlist.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: { "User-Agent": "Mozilla/5.0" },
  },
  {
    name: "beIN SPORTS XTRA 2 (بث بديل)",
    url: "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: { "User-Agent": "Mozilla/5.0" },
  },
  {
    name: "beIN SPORTS HD (سيرفر مباشر 3)",
    url: "https://5c7b683162943.streamlock.net/live/ngrp:sportsone_all/playlist.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: { "User-Agent": "Mozilla/5.0" },
  },
];

// ── Working Live Streams for Arab Sports Channels ──
export const ARAB_SPORTS_STREAMS_MAP: Record<string, YacineStream[]> = {
  alkass_1: [
    {
      name: "الكأس 1 HD (بث مباشر رسمي)",
      url: "https://shoof.alkass.net/shoof/live/live_1/index.m3u8",
      userAgent: "Mozilla/5.0",
      referer: "https://shoof.alkass.net/",
      urlType: 3,
      headers: {},
    },
  ],
  alkass_2: [
    {
      name: "الكأس 2 HD (بث مباشر رسمي)",
      url: "https://shoof.alkass.net/shoof/live/live_2/index.m3u8",
      userAgent: "Mozilla/5.0",
      referer: "https://shoof.alkass.net/",
      urlType: 3,
      headers: {},
    },
  ],
  arab_kuwait: [
    {
      name: "الكويت الرياضية HD (بث مباشر)",
      url: "https://kwtspta.cdn.mangomolo.com/sp/smil:sp.stream.smil/chunklist.m3u8",
      userAgent: "Mozilla/5.0",
      referer: "https://media.gov.kw/",
      urlType: 3,
      headers: {},
    },
    {
      name: "الكويت بلس الرياضية (بديل)",
      url: "https://kwtsplta.cdn.mangomolo.com/spl/smil:spl.stream.smil/chunklist.m3u8",
      userAgent: "Mozilla/5.0",
      referer: "https://media.gov.kw/",
      urlType: 3,
      headers: {},
    },
  ],
  arab_kuwait_plus: [
    {
      name: "الكويت بلس الرياضية HD (بث مباشر)",
      url: "https://kwtsplta.cdn.mangomolo.com/spl/smil:spl.stream.smil/chunklist.m3u8",
      userAgent: "Mozilla/5.0",
      referer: "https://media.gov.kw/",
      urlType: 3,
      headers: {},
    },
  ],
  arab_bahrain_1: [
    {
      name: "البحرين الرياضية 1 HD (بث مباشر)",
      url: "https://5c7b683162943.streamlock.net/live/ngrp:sportsone_all/playlist.m3u8",
      userAgent: "Mozilla/5.0",
      referer: "https://yacinelive.com/",
      urlType: 3,
      headers: {},
    },
  ],
  arab_bahrain_2: [
    {
      name: "البحرين الرياضية 2 HD (بث مباشر)",
      url: "https://5c7b683162943.streamlock.net/live/ngrp:bahrainsportstwo_all/playlist.m3u8",
      userAgent: "Mozilla/5.0",
      referer: "https://yacinelive.com/",
      urlType: 3,
      headers: {},
    },
  ],
  arab_oman: [
    {
      name: "عمان الرياضية HD (بث مباشر)",
      url: "https://partneta.cdn.mgmlcdn.com/omsport/smil:omsport.stream.smil/chunklist.m3u8",
      userAgent: "Mozilla/5.0",
      referer: "https://partneta.cdn.mgmlcdn.com/",
      urlType: 3,
      headers: {},
    },
  ],
  arab_jordan: [
    {
      name: "الأردن الرياضية HD (بث مباشر)",
      url: "https://jrtv-live.ercdn.net/jordansporthd/jordansporthd.m3u8",
      userAgent: "Mozilla/5.0",
      referer: "https://jrtv.gov.jo/",
      urlType: 3,
      headers: {},
    },
  ],
  arab_iraq: [
    {
      name: "العراقية الرياضية HD (بث مباشر)",
      url: "https://imn-live.esite-lab.com/hls/iraqia-sports-1.m3u8",
      userAgent: "Mozilla/5.0",
      referer: "https://imn.iq/",
      urlType: 3,
      headers: {},
    },
  ],
};

export async function getYacineCategories(_force = false): Promise<YacineCategory[]> {
  return DEFAULT_CATEGORIES;
}

export async function getYacineChannels(categoryId: string | number, _force = false): Promise<YacineChannel[]> {
  const catKey = String(categoryId).toLowerCase();
  if (catKey === "sports_arab" || catKey === "2") {
    return ARAB_SPORTS_CHANNELS;
  }
  return BEIN_FALLBACK_CHANNELS;
}

export async function getYacineEvents(_force = false): Promise<YacineEvent[]> {
  return [];
}

export async function getStreamsForChannel(channelId: number | string): Promise<YacineStream[]> {
  const idStr = String(channelId).toLowerCase();

  // 1. Direct match in Arab Sports map
  if (ARAB_SPORTS_STREAMS_MAP[idStr]) {
    return ARAB_SPORTS_STREAMS_MAP[idStr];
  }

  // 2. Default to reliable beIN Sports live streams
  return DEFAULT_BEIN_STREAMS;
}

export async function getStreamsForEvent(_eventId: number | string): Promise<YacineStream[]> {
  return DEFAULT_BEIN_STREAMS;
}
