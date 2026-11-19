import { NextRequest, NextResponse } from "next/server";

// Proxy a web page's HTML to extract og:image and other meta tags
// Used by aggregator to get posters from source sites (Akwam, WeCima, etc.)
export async function GET(request: NextRequest) {
  const url = request.nextUrl.searchParams.get("url");
  if (!url) {
    return NextResponse.json({ error: "Missing url" }, { status: 400 });
  }

  try {
    const decoded = decodeURIComponent(url);
    if (!decoded.startsWith("http")) {
      return NextResponse.json({ error: "Invalid url" }, { status: 400 });
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 5000);

    const res = await fetch(decoded, {
      signal: controller.signal,
      headers: {
        "User-Agent":
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        Accept: "text/html,application/xhtml+xml",
        "Accept-Language": "ar,en;q=0.9",
        Referer: new URL(decoded).origin + "/",
      },
    });

    clearTimeout(timeout);

    if (!res.ok) {
      return NextResponse.json({ error: `HTTP ${res.status}` }, { status: res.status });
    }

    // Only return first 50KB (enough for meta tags in <head>)
    const buffer = await res.arrayBuffer();
    const text = new TextDecoder("utf-8").decode(buffer.slice(0, 50000));

    return new NextResponse(text, {
      status: 200,
      headers: {
        "Content-Type": "text/html; charset=utf-8",
        "Cache-Control": "public, max-age=3600",
      },
    });
  } catch (err: unknown) {
    if (err instanceof Error && err.name === "AbortError") {
      return NextResponse.json({ error: "Timeout" }, { status: 408 });
    }
    return NextResponse.json({ error: "Fetch failed" }, { status: 500 });
  }
}
