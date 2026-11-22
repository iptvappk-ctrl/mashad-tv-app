import json

with open(r'c:\Users\hassa\Desktop\all project\iptv app\10_SCRATCH_AND_TOOLS\yacine_real_events.json', 'r', encoding='utf-8') as f:
    events = json.load(f)['data']

with open(r'c:\Users\hassa\Desktop\all project\iptv app\10_SCRATCH_AND_TOOLS\yacine_real_channels.json', 'r', encoding='utf-8') as f:
    channels = json.load(f)

ts_channels = []
for i, ch in enumerate(channels):
    prio = 25 - i
    ts_channels.append(f'  {{ id: "{ch["id"]}", name: "{ch["name"]}", logo: "{ch["logo"]}", isHide: 0, priority: {prio} }},')

ts_events = []
for ev in events:
    t1 = ev['team_1']
    t2 = ev['team_2']
    cid = ev['id']
    champ = ev['champions']
    chan = ev['channel']
    st = ev['start_time']
    et = ev['end_time']
    comm = ev['commentary']
    t1_name = t1['name']
    t1_logo = t1.get('logo', '')
    t1_id = str(t1.get('id', ''))
    t2_name = t2['name']
    t2_logo = t2.get('logo', '')
    t2_id = str(t2.get('id', ''))
    ts_events.append(f'''  {{
    id: "{cid}",
    champions: "{champ}",
    channel: "{chan}",
    startTime: {st},
    endTime: {et},
    commentary: "{comm}",
    team1: {{ id: "{t1_id}", name: "{t1_name}", logo: "{t1_logo}" }},
    team2: {{ id: "{t2_id}", name: "{t2_name}", logo: "{t2_logo}" }}
  }},''')

ts_code = f'''export interface YacineCategory {{
  id: string | number;
  name: string;
  logo: string;
  childCount: number;
}}

export interface YacineChannel {{
  id: string | number;
  name: string;
  logo: string;
  isHide: number;
  priority: number;
}}

export interface YacineTeam {{
  id: string | number;
  name: string;
  logo: string;
}}

export interface YacineEvent {{
  id: string | number;
  champions: string;
  channel: string;
  startTime: number;
  endTime: number;
  commentary: string;
  team1: YacineTeam;
  team2: YacineTeam;
}}

export interface YacineStream {{
  name: string;
  url: string;
  userAgent: string;
  referer: string;
  urlType: number;
  headers: Record<string, string>;
}}

// ── Built-in High Quality Categories ──
export const DEFAULT_CATEGORIES: YacineCategory[] = [
  {{
    id: "bein_sports",
    name: "beIN SPORTS",
    logo: "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_1_Digital_Mono.png",
    childCount: 20,
  }},
];

// ── beIN Sports Channels extracted from Yacine TV ──
export const BEIN_FALLBACK_CHANNELS: YacineChannel[] = [
{chr(10).join(ts_channels)}
];

// ── Real Matches extracted from Yacine TV ──
export const REAL_YACINE_EVENTS: YacineEvent[] = [
{chr(10).join(ts_events)}
];

// ── Working Live Streams for beIN Sports ──
export const DEFAULT_BEIN_STREAMS: YacineStream[] = [
  {{
    name: "beIN SPORTS HD (سيرفر رئيسي 1 فائق السرعة CDN)",
    url: "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: {{ "User-Agent": "Mozilla/5.0" }},
  }},
  {{
    name: "beIN SPORTS XTRA HD (سيرفر مباشر 2 CDN)",
    url: "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: {{ "User-Agent": "Mozilla/5.0" }},
  }},
  {{
    name: "beIN SPORTS XTRA (سيرفر بديل 3 CDN)",
    url: "https://bein-xtra-bein.amagi.tv/playlist.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: {{ "User-Agent": "Mozilla/5.0" }},
  }},
  {{
    name: "beIN SPORTS Live (سيرفر احتياطي 4)",
    url: "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8",
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    referer: "https://yacinelive.com/",
    urlType: 3,
    headers: {{ "User-Agent": "Mozilla/5.0" }},
  }},
];

export async function getYacineCategories(_force = false): Promise<YacineCategory[]> {{
  return DEFAULT_CATEGORIES;
}}

export async function getYacineChannels(_categoryId: string | number, _force = false): Promise<YacineChannel[]> {{
  return BEIN_FALLBACK_CHANNELS;
}}

export async function getYacineEvents(_force = false): Promise<YacineEvent[]> {{
  return REAL_YACINE_EVENTS;
}}

export async function getStreamsForChannel(channelId: number | string): Promise<YacineStream[]> {{
  const idStr = String(channelId).toLowerCase();
  const matched = BEIN_FALLBACK_CHANNELS.find(ch => String(ch.id).toLowerCase() === idStr || ch.name.toLowerCase().includes(idStr));
  const channelName = matched ? matched.name : "beIN SPORTS HD";
  return DEFAULT_BEIN_STREAMS.map(s => ({{ ...s, name: `${{channelName}} · ${{s.name}}` }}));
}}

export async function getStreamsForEvent(eventId: number | string): Promise<YacineStream[]> {{
  const ev = REAL_YACINE_EVENTS.find(e => String(e.id) === String(eventId));
  const chName = ev?.channel || "beIN SPORTS 1";
  return DEFAULT_BEIN_STREAMS.map(s => ({{ ...s, name: `${{chName}} · ${{s.name}}` }}));
}}
'''

target_path = r'c:\Users\hassa\Desktop\all project\iptv app\06_WEB_AND_DISTRIBUTION\web\src\lib\yacine.ts'
with open(target_path, 'w', encoding='utf-8') as f:
    f.write(ts_code)

print("Updated yacine.ts successfully! Size:", len(ts_code))
