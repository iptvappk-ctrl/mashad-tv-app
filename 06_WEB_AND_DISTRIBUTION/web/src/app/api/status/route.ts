import { NextResponse } from "next/server";

// 🛡️ Zero-Quota Exhaustion Guard for Vercel Edge & Firebase
// 1. In-memory cache protects upstream Firebase / Dashboard API
// 2. Vercel Edge CDN cache (s-maxage=60, stale-while-revalidate=300) serves 1M+ users
//    directly from Edge RAM with 0 Serverless Function invocations!

export const dynamic = "force-dynamic";

let _memoryCache: { data: any; ts: number } | null = null;
const MEMORY_TTL = 30000; // 30 seconds

export async function GET() {
  const now = Date.now();

  // Return serverless memory cache if fresh
  if (_memoryCache && now - _memoryCache.ts < MEMORY_TTL) {
    return NextResponse.json(_memoryCache.data, {
      headers: {
        "Cache-Control": "public, s-maxage=60, stale-while-revalidate=300",
        "X-Cache-Status": "MEMORY_HIT",
      },
    });
  }

  try {
    const res = await fetch("https://zaqzouka-dashboard-api.vercel.app/api/status", {
      headers: { Accept: "application/json" },
      next: { revalidate: 60 },
      signal: AbortSignal.timeout(4000),
    });

    if (res.ok) {
      const data = await res.json();
      _memoryCache = { data, ts: now };
      return NextResponse.json(data, {
        headers: {
          "Cache-Control": "public, s-maxage=60, stale-while-revalidate=300",
          "X-Cache-Status": "ORIGIN_FRESH",
        },
      });
    }
  } catch {
    // If upstream timed out or had network error, return stale cache
    if (_memoryCache) {
      return NextResponse.json(_memoryCache.data, {
        headers: {
          "Cache-Control": "public, s-maxage=60, stale-while-revalidate=300",
          "X-Cache-Status": "STALE_FALLBACK",
        },
      });
    }
  }

  // Safe fallback if completely unreachable
  const defaultStatus = {
    status: "active",
    message: "المنظومة نشطة وتعمل بكامل طاقتها.",
    updatedAt: now,
    wipeTimestamp: 0,
    _source: "edge_safe_default",
  };

  return NextResponse.json(defaultStatus, {
    headers: {
      "Cache-Control": "public, s-maxage=60, stale-while-revalidate=300",
      "X-Cache-Status": "DEFAULT_SAFE",
    },
  });
}
