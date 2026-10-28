import { NextRequest, NextResponse } from "next/server";
import { isAllowedProxyUrl, checkRateLimit } from "@/lib/security";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

function getRefererForUrl(targetUrl: string): string {
  if (targetUrl.includes("akw.cam") || targetUrl.includes("akwam")) return "https://akw.cam/";
  if (targetUrl.includes("wecima") || targetUrl.includes("myweecima")) return "https://myweecima.beauty/";
  if (targetUrl.includes("fasel") || targetUrl.includes("fasel4k")) return "https://wb49584.fasel4k.site/";
  if (targetUrl.includes("egybest")) return "https://egybest.mx/";
  if (targetUrl.includes("lodynet")) return "https://lodynet.art/";
  if (targetUrl.includes("brstej")) return "https://brstej.surf/";
  if (targetUrl.includes("q-ask") || targetUrl.includes("esheeq") || targetUrl.includes("3sk")) return "https://e.q-ask.video/";
  if (targetUrl.includes("uqload")) return "https://uqload.com/";
  if (targetUrl.includes("doodstream") || targetUrl.includes("dood")) return "https://doodstream.com/";
  if (targetUrl.includes("govid") || targetUrl.includes("datahowa")) return "https://govid.live/";
  try {
    return new URL(targetUrl).origin + "/";
  } catch {
    return "";
  }
}

export async function GET(req: NextRequest) {
  const ip = req.headers.get("x-forwarded-for") || "127.0.0.1";
  if (!checkRateLimit(ip, 600, 60000)) {
    return new NextResponse("Too Many Requests", { status: 429 });
  }

  const { searchParams } = new URL(req.url);
  const targetUrl = searchParams.get("url");

  if (!targetUrl) {
    return new NextResponse("Missing url parameter", { status: 400 });
  }

  if (!isAllowedProxyUrl(targetUrl)) {
    return new NextResponse("Forbidden target URL", { status: 403 });
  }

  const referer = searchParams.get("ref") || getRefererForUrl(targetUrl);
  const userAgent =
    searchParams.get("ua") ||
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

  try {
    const upstreamRes = await fetch(targetUrl, {
      headers: {
        "User-Agent": userAgent,
        Referer: referer,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "ar,en;q=0.9",
      },
      redirect: "follow",
    });

    if (!upstreamRes.ok) {
      return new NextResponse(`Upstream error: ${upstreamRes.status}`, { status: upstreamRes.status });
    }

    let html = await upstreamRes.text();
    const parsedTarget = new URL(targetUrl);
    const origin = parsedTarget.origin;

    // Inject <base href> so relative scripts/styles/images resolve correctly
    const baseTag = `<base href="${origin}/">`;
    if (html.includes("<head>")) {
      html = html.replace("<head>", `<head>${baseTag}`);
    } else if (html.includes("<HEAD>")) {
      html = html.replace("<HEAD>", `<HEAD>${baseTag}`);
    } else {
      html = baseTag + html;
    }

    // Clean player CSS: make video fill 100% of container, hide annoying ads/overlays
    const customStyle = `
      <style id="mashad-embed-style">
        * { box-sizing: border-box !important; }
        html, body {
          width: 100vw !important;
          height: 100vh !important;
          margin: 0 !important;
          padding: 0 !important;
          overflow: hidden !important;
          background: #000 !important;
        }
        video, iframe, #player, .video-js, .jwplayer, div[id*="player"], div[class*="player"] {
          width: 100vw !important;
          height: 100vh !important;
          max-width: 100vw !important;
          max-height: 100vh !important;
          position: fixed !important;
          top: 0 !important;
          left: 0 !important;
          z-index: 9999 !important;
          object-fit: contain !important;
          background: #000 !important;
        }
        header, footer, nav, .ad, .banner, .ads, [id*="ad"], [class*="ad"] {
          display: none !important;
        }
      </style>
      <script>
        window.open = function() { return null; };
        window.alert = function() { return true; };
      </script>
    `;

    if (html.includes("</head>")) {
      html = html.replace("</head>", `${customStyle}</head>`);
    } else {
      html += customStyle;
    }

    return new NextResponse(html, {
      status: 200,
      headers: {
        "Content-Type": "text/html; charset=utf-8",
        // Crucial: remove all frame-ancestors restrictions
        "X-Frame-Options": "ALLOWALL",
        "Access-Control-Allow-Origin": "*",
      },
    });
  } catch (err: any) {
    return new NextResponse(`Proxy error: ${err.message}`, { status: 500 });
  }
}
