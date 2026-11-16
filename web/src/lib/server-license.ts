// web/src/lib/server-license.ts
// Server-side license & kill switch validator for Next.js API routes
// Prevents video resolution or streaming if owner activated the kill switch

const CLOUD_API = "https://zaqzouka-dashboard-api.vercel.app";
let cachedStatus: { allowed: boolean; timestamp: number } = { allowed: true, timestamp: 0 };
const CACHE_TTL_MS = 30000; // 30 seconds cache to preserve high performance

export async function isServiceAllowed(): Promise<boolean> {
  const now = Date.now();
  if (now - cachedStatus.timestamp < CACHE_TTL_MS) {
    return cachedStatus.allowed;
  }

  try {
    const res = await fetch(`${CLOUD_API}/api/admin?route=license-check`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-zaqzouka-key": "ZQ-OWNER-2026-SECRET-14MOON"
      },
      body: JSON.stringify({ platform: "web", buildId: "zq-web-14moon" }),
      signal: AbortSignal.timeout(3000),
      cache: "no-store",
    });

    if (res.ok) {
      const data = await res.json();
      cachedStatus = {
        allowed: data?.allowed !== false,
        timestamp: now,
      };
      return cachedStatus.allowed;
    }
  } catch {
    // Fail-open on timeout or server error so regular users are not disrupted
  }

  return true;
}
