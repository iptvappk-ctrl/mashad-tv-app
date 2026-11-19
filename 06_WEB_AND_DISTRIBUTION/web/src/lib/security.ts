import { NextResponse } from "next/server";

// Rate limiting store: IP -> { count, resetTime }
const rateLimitMap = new Map<string, { count: number; resetTime: number }>();

export function checkRateLimit(ip: string, limit = 120, windowMs = 60000): boolean {
  const now = Date.now();
  const entry = rateLimitMap.get(ip);

  if (!entry || now > entry.resetTime) {
    rateLimitMap.set(ip, { count: 1, resetTime: now + windowMs });
    return true;
  }

  if (entry.count >= limit) {
    return false;
  }

  entry.count++;
  return true;
}

// Clean up old rate limit entries every 5 minutes
if (typeof setInterval !== "undefined") {
  setInterval(() => {
    const now = Date.now();
    for (const [key, val] of rateLimitMap.entries()) {
      if (now > val.resetTime) rateLimitMap.delete(key);
    }
  }, 300000);
}

/**
 * Strict SSRF protection:
 * Ensures the target URL does not point to localhost, private IP subnets,
 * or cloud metadata IP endpoints (AWS, GCP, Azure, etc.)
 */
export function isAllowedProxyUrl(urlStr: string): boolean {
  try {
    const parsed = new URL(urlStr);
    const host = parsed.hostname.toLowerCase();

    // Must be HTTP or HTTPS
    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
      return false;
    }

    // Block localhost and standard loopbacks
    if (
      host === "localhost" ||
      host === "127.0.0.1" ||
      host === "0.0.0.0" ||
      host === "::1" ||
      host.endsWith(".local") ||
      host.endsWith(".internal")
    ) {
      return false;
    }

    // Block AWS/GCP/Azure link-local metadata (169.254.169.254)
    if (host.startsWith("169.254.")) {
      return false;
    }

    // Check private IPv4 ranges:
    // 10.0.0.0/8
    // 172.16.0.0/12 (172.16 - 172.31)
    // 192.168.0.0/16
    const ipParts = host.split(".").map(Number);
    if (ipParts.length === 4 && ipParts.every((p) => !isNaN(p) && p >= 0 && p <= 255)) {
      if (ipParts[0] === 10) return false;
      if (ipParts[0] === 127) return false;
      if (ipParts[0] === 192 && ipParts[1] === 168) return false;
      if (ipParts[0] === 172 && ipParts[1] >= 16 && ipParts[1] <= 31) return false;
      if (ipParts[0] === 169 && ipParts[1] === 254) return false;
    }

    return true;
  } catch {
    return false;
  }
}

export function sanitizeInput(str: string): string {
  if (!str) return "";
  return str.replace(/[<>"'`;]/g, "").trim();
}
