const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

const mirrors = [
  // Akwam
  { name: 'Akwam .cx', url: 'https://akwam.cx/' },
  // WeCima
  { name: 'WeCima .tube', url: 'https://wecima.tube/' },
  { name: 'WeCima .page', url: 'https://mycima.page/' },
  { name: 'WeCima .mx', url: 'https://mycima.mx/' },
  { name: 'WeCima .video', url: 'https://wecima.video/' },
  { name: 'WeCima .in', url: 'https://mycima.in/' },
  { name: 'WeCima .fun', url: 'https://wecima.fun/' },
  // FaselHD
  { name: 'FaselHD .club', url: 'https://faselhd.club/' },
  { name: 'FaselHD .top', url: 'https://faselhd.top/' },
  { name: 'FaselHD .pro', url: 'https://faselhd.pro/' },
  { name: 'FaselHD .vip', url: 'https://www.faselhd.vip/' },
  // Egybest
  { name: 'Egybest .page', url: 'https://egybest.page/' },
  { name: 'Egybest .vip', url: 'https://egybest.vip/' },
  { name: 'Egybest .life', url: 'https://egybest.life/' },
  { name: 'Egybest .mx', url: 'https://egybest.mx/' },
  // CimaClub
  { name: 'CimaClub .skin', url: 'https://cimaclub.skin/' },
  { name: 'CimaClub .io', url: 'https://cimaclub.io/' },
  { name: 'CimaClub .cc', url: 'https://cimaclub.cc/' },
  // 3sk
  { name: '3sk .video', url: 'https://3sk.video/' },
  { name: '3sk .org', url: 'https://esheeq.org/' },
  { name: '3sk .co', url: 'https://esheeq.co/' },
  // Shahid4U
  { name: 'Shahid4U .net', url: 'https://shahid4u.net/' },
  { name: 'Shahid4U .im', url: 'https://shahid4u.im/' },
  { name: 'Shahid4U .io', url: 'https://shahed4u.io/' },
  // Movizland
  { name: 'Movizland .icu', url: 'https://movizland.icu/' },
  { name: 'Movizland .online', url: 'https://movizland.online/' },
  // Arablionz
  { name: 'Arablionz .info', url: 'https://arablionz.info/' },
  { name: 'Arablionz .top', url: 'https://arablionz.top/' },
  // CimaLight
  { name: 'CimaLight .vip', url: 'https://cimalight.vip/' },
  // ShoofLive
  { name: 'ShoofLive .tv', url: 'https://shooflive.tv/' },
];

async function checkMirrors() {
  for (const t of mirrors) {
    const start = Date.now();
    try {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 3500);
      const res = await fetch(t.url, {
        headers: {
          'User-Agent': UA,
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
          'Referer': 'https://google.com/',
        },
        signal: controller.signal
      });
      clearTimeout(timeout);
      const time = Date.now() - start;
      if (res.status === 200) {
        console.log(`[PASS] ${t.name} -> 200 OK (${time}ms) [${res.url}]`);
      } else {
        console.log(`[FAIL] ${t.name} -> HTTP ${res.status} [${res.url}]`);
      }
    } catch (e) {
      console.log(`[ERROR] ${t.name} -> ${e.message}`);
    }
  }
}

checkMirrors();
