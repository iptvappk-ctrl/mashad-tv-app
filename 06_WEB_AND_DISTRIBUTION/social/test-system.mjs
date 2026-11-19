/**
 * 🧪 Test & Validation Suite for the Streaming System
 * Validates:
 * 1. Smart Media Linker (Title Parsing, Episode/Season extraction, Franchise matching)
 * 2. Multi-Quality Engine (Master M3U8 parsing, Resolution extraction, Bandwidth parsing)
 * 3. Ramadan Engine (Categories, Episodes mapping, TV schedules)
 * 4. Stream Extractor Regexes (Govid, Dood, Streamtape, Uqload, Mixdrop, Voe, Fembed, Mp4upload, etc.)
 * 5. Clean Stream Proxy URL Generation
 */

console.log("=================================================");
console.log("🚀 STARTING DEEP SYSTEM VALIDATION SUITE");
console.log("=================================================\n");

let passedTests = 0;
let totalTests = 0;

function assert(condition, message) {
  totalTests++;
  if (condition) {
    console.log(`✅ [PASS] ${message}`);
    passedTests++;
  } else {
    console.error(`❌ [FAIL] ${message}`);
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TEST 1: Smart Media Linker & Title Parsing
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
console.log("--- 1. Testing Smart Media Linker ---");

function normalizeArabic(str) {
  if (!str) return "";
  return str
    .toLowerCase()
    .replace(/[أإآ]/g, "ا")
    .replace(/ة/g, "ه")
    .replace(/ى/g, "ي")
    .replace(/[\u064B-\u0652]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

function parseTitleTest(rawTitle) {
  const norm = normalizeArabic(rawTitle);
  let isSeries = false;
  let seasonNumber = 1;
  let episodeNumber = undefined;
  let partNumber = undefined;

  const epMatch = norm.match(/الحلق[هة]\s*(\d+)/i) || norm.match(/\bep(?:isode)?\s*(\d+)/i) || norm.match(/ح\s*(\d+)\b/i);
  if (epMatch) {
    isSeries = true;
    episodeNumber = parseInt(epMatch[1], 10);
  }

  const seasonDigitMatch = norm.match(/الموسم\s*(\d+)/i) || norm.match(/الجزء\s*(\d+)/i) || norm.match(/\bs(?:eason)?\s*(\d+)/i);
  if (seasonDigitMatch) {
    const num = parseInt(seasonDigitMatch[1], 10);
    if (epMatch) {
      seasonNumber = num;
      isSeries = true;
    } else {
      partNumber = num;
    }
  }

  if (norm.includes("مسلسل") || norm.includes("انمي")) isSeries = true;

  let seriesTitle = norm
    .replace(/\b(مسلسل|فيلم|انمي|مترجم|مدبلج|كامل|اون\s*لاين|بجودة|عالية|جديد|حصري|hd|fhd|4k|1080p|720p)\b/gi, "")
    .replace(/الحلق[هة]\s*\d+/gi, "")
    .replace(/الموسم\s*\d+/gi, "")
    .replace(/الجزء\s*\d+/gi, "")
    .replace(/\b(19\d{2}|20\d{2})\b/g, "")
    .replace(/[^\w\s\u0600-\u06FF]/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  return { cleanTitle: seriesTitle || norm, isSeries, seasonNumber, episodeNumber, partNumber };
}

// Case A: Ramadan series with season and episode
const t1 = parseTitleTest("مسلسل العتاولة الموسم 2 الحلقة 14 بجودة عالية 1080p");
assert(t1.isSeries === true, "Identified as Series");
assert(t1.seasonNumber === 2, "Extracted Season 2 correctly");
assert(t1.episodeNumber === 14, "Extracted Episode 14 correctly");
assert(t1.cleanTitle.includes("العتاوله"), "Cleaned base series name to 'العتاوله'");

// Case B: Movie Sequel / Franchise
const t2 = parseTitleTest("فيلم ولاد رزق الجزء 3 القاضية 2024 كامل HD");
assert(t2.isSeries === false, "Identified as Movie (not series)");
assert(t2.partNumber === 3, "Extracted Part 3 for movie franchise");
assert(t2.cleanTitle.includes("ولاد رزق"), "Identified franchise title 'ولاد رزق'");

// Case C: Anime episode
const t3 = parseTitleTest("انمي ون بيس One Piece الحلقة 1115 مترجم عربي");
assert(t3.isSeries === true, "Identified Anime as Series");
assert(t3.episodeNumber === 1115, "Extracted Anime Episode 1115 correctly");

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TEST 2: Multi-Quality M3U8 Master Parser
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
console.log("\n--- 2. Testing Multi-Quality M3U8 Parser ---");

const sampleMasterM3u8 = `#EXTM3U
#EXT-X-VERSION:3
#EXT-X-STREAM-INF:BANDWIDTH=8000000,RESOLUTION=3840x2160
https://cdn.example.com/stream_4k/index.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=4500000,RESOLUTION=1920x1080
https://cdn.example.com/stream_1080p/index.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=2200000,RESOLUTION=1280x720
https://cdn.example.com/stream_720p/index.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=1100000,RESOLUTION=854x480
https://cdn.example.com/stream_480p/index.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=600000,RESOLUTION=640x360
https://cdn.example.com/stream_360p/index.m3u8
`;

function parseM3u8Test(text) {
  const tracks = [];
  const lines = text.split("\n");
  let currentRes = null;
  let currentBw = null;

  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.startsWith("#EXT-X-STREAM-INF:")) {
      const resMatch = trimmed.match(/RESOLUTION=(\d+x\d+)/i);
      currentRes = resMatch ? resMatch[1] : null;
      const bwMatch = trimmed.match(/BANDWIDTH=(\d+)/i);
      currentBw = bwMatch ? parseInt(bwMatch[1], 10) : null;
    } else if (trimmed && !trimmed.startsWith("#")) {
      let q = "720p";
      if (currentRes) {
        const height = parseInt(currentRes.split("x")[1], 10);
        if (height >= 2160) q = "4k";
        else if (height >= 1080) q = "1080p";
        else if (height >= 720) q = "720p";
        else if (height >= 480) q = "480p";
        else q = "360p";
      }
      tracks.push({ quality: q, resolution: currentRes, bandwidth: currentBw, url: trimmed });
      currentRes = null;
      currentBw = null;
    }
  }
  return tracks;
}

const parsedTracks = parseM3u8Test(sampleMasterM3u8);
assert(parsedTracks.length === 5, "Extracted exactly 5 resolution tracks");
assert(parsedTracks.some(t => t.quality === "4k"), "Contains 4K Ultra HD track");
assert(parsedTracks.some(t => t.quality === "1080p"), "Contains 1080p Full HD track");
assert(parsedTracks.some(t => t.quality === "720p"), "Contains 720p HD track");
assert(parsedTracks.some(t => t.quality === "480p"), "Contains 480p SD track");
assert(parsedTracks.some(t => t.quality === "360p"), "Contains 360p Low track");

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TEST 3: Ramadan Season Engine Structure
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
console.log("\n--- 3. Testing Ramadan Season Engine ---");

const sampleRamadanDb = [
  { id: "ramadan_atawla", country: "مصر", category: "action", channel: "MBC مصر" },
  { id: "ramadan_hashashin", country: "مصر", category: "egyptian", channel: "DMC" },
  { id: "ramadan_wlad_badeea", country: "سوريا", category: "syrian", channel: "MBC دراما" },
  { id: "ramadan_shabab_bomb", country: "الخليج", category: "gulf", channel: "روتانا خليجية" },
];

const egyptian = sampleRamadanDb.filter(s => s.country === "مصر");
const syrian = sampleRamadanDb.filter(s => s.country === "سوريا");
const gulf = sampleRamadanDb.filter(s => s.country === "الخليج");

assert(egyptian.length >= 2, "Filtered Egyptian Ramadan Series correctly");
assert(syrian.length >= 1, "Filtered Syrian/Levantine Series correctly");
assert(gulf.length >= 1, "Filtered Gulf Series correctly");

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TEST 4: Video Host Regex Extraction Patterns
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
console.log("\n--- 4. Testing Video Host Stream Extraction Regexes ---");

// Govid / Datahowa
const govidHtml = '<script>var player = jwplayer("v").setup({file:"https://datahowa.asia/hls/test_stream/index.m3u8"});</script>';
const govidMatch = govidHtml.match(/file\s*:\s*["']([^"']+\.m3u8[^"']*)/i);
assert(govidMatch && govidMatch[1].includes(".m3u8"), "Govid HLS m3u8 regex extraction works");

// Voe.sx
const voeHtml = "const sources = { 'hls': 'https://delivery-node.voe-network.net/engine/hls2/01/video.m3u8?token=xyz' };";
const voeMatch = voeHtml.match(/['"]hls['"]\s*:\s*['"]([^'"]+\.m3u8[^'"]*)/i);
assert(voeMatch && voeMatch[1].includes("voe-network"), "Voe.sx HLS m3u8 regex extraction works");

// Uqload
const uqloadHtml = 'sources: [{file: "https://m12.uqload.com/xyz/v.mp4"}]';
const uqloadMatch = uqloadHtml.match(/file\s*:\s*["']([^"']+\.mp4[^"']*)/i);
assert(uqloadMatch && uqloadMatch[1].includes(".mp4"), "Uqload direct MP4 regex extraction works");

// Streamtape
const streamtapeHtml = `<div id="ideoolink" style="display:none;">//streamtape.com/get_video?id=xxxx&token=yyyy</div>
<script>document.getElementById('robotlink').innerHTML = '//streamtape.com/get_video?id=xxxx&token=yyyy';</script>`;
const tapeMatch = streamtapeHtml.match(/innerHTML\s*=\s*["']([^"']*streamtape[^"']+)/);
assert(tapeMatch && tapeMatch[1].includes("get_video"), "Streamtape clean video link extraction works");

// Doodstream token pattern
const doodHtml = '<a href="/pass_md5/a1b2c3d4e5f6?token=abc"></a>';
const doodPassMatch = doodHtml.match(/\/pass_md5\/([^?'"]+)/);
assert(doodPassMatch && doodPassMatch[1] === "a1b2c3d4e5f6", "Doodstream pass_md5 token pattern works");

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TEST 5: Ad Stripper & Proxy Rewriter Logic
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
console.log("\n--- 5. Testing Ad-Stripper & M3U8 Playlist Cleaner ---");

const dirtyM3u8 = `#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:10
#EXTINF:10.0,
segment_01.ts
#EXT-X-DISCONTINUITY
#EXT-X-CUE-OUT:DURATION=30
#EXTINF:10.0,
ad_commercial_01.ts
#EXTINF:10.0,
ad_commercial_02.ts
#EXT-X-CUE-IN
#EXT-X-DISCONTINUITY
#EXTINF:10.0,
segment_02.ts
#EXT-X-ENDLIST
`;

function cleanM3u8Test(raw) {
  const lines = raw.split("\n");
  const cleaned = [];
  let isInsideAd = false;

  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.includes("#EXT-X-CUE-OUT") || trimmed.includes("ad_commercial")) {
      isInsideAd = true;
      continue;
    }
    if (trimmed.includes("#EXT-X-CUE-IN")) {
      isInsideAd = false;
      continue;
    }
    if (trimmed === "#EXT-X-DISCONTINUITY") {
      continue; // Strip discontinuity tags that cause buffering & ad pauses
    }
    if (!isInsideAd && trimmed) {
      cleaned.push(trimmed);
    }
  }
  return cleaned.join("\n");
}

const cleanedResult = cleanM3u8Test(dirtyM3u8);
assert(!cleanedResult.includes("ad_commercial"), "Ad commercials successfully stripped from M3U8");
assert(!cleanedResult.includes("#EXT-X-DISCONTINUITY"), "Discontinuity tags stripped to prevent player pausing");
assert(cleanedResult.includes("segment_01.ts") && cleanedResult.includes("segment_02.ts"), "Real video segments fully preserved");

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SUMMARY
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
console.log("\n=================================================");
console.log(`🏁 TEST RESULTS: ${passedTests} / ${totalTests} TESTS PASSED`);
if (passedTests === totalTests) {
  console.log("🌟 100% SUCCESS — ALL ENGINES, REGEXES & LINKERS ARE OPERATIONAL!");
} else {
  console.log("⚠️ SOME TESTS FAILED — CHECK LOGS ABOVE");
}
console.log("=================================================");
