// web/src/lib/server-license.ts
// Server-side license & kill switch validator for Next.js API routes
// Prevents video resolution or streaming if owner activated the kill switch

const CLOUD_API = "https://dashboard-api-indol-eight.vercel.app";
let cachedStatus: { allowed: boolean; timestamp: number } = { allowed: true, timestamp: 0 };
const CACHE_TTL_MS = 120000; // 2 minutes server cache + CDN edge cache

export async function isServiceAllowed(): Promise<boolean> {
  const now = Date.now();
  if (now - cachedStatus.timestamp < CACHE_TTL_MS) {
    return cachedStatus.allowed;
  }

  try {
    const res = await fetch(`${CLOUD_API}/api/status`, {
      method: "GET",
      headers: { Accept: "application/json" },
      signal: AbortSignal.timeout(3000),
    });

    if (res.ok) {
      const data = await res.json();
      const status = String(data?.status || "active").toLowerCase();
      const comps = data?.components || {};
      const blocked =
        status === "frozen" ||
        status === "wiped" ||
        comps.web_player_enabled === false ||
        comps.website_enabled === false;
      cachedStatus = {
        allowed: !blocked,
        timestamp: now,
      };
      return cachedStatus.allowed;
    }
  } catch {
    // Fail-open on timeout or server error so regular users are not disrupted
  }

  return true;
}
