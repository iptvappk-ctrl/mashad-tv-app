/**
 * 🎬 YouTube Stream Resolver
 * GET /api/cinema/ytstream?id=VIDEO_ID
 *
 * يستخدم Innertube API لجلب رابط stream مباشر لفيديو YouTube
 * حتى للفيديوهات الممنوع تضمينها (embedding disabled)
 */

import { NextRequest, NextResponse } from "next/server";
import { isServiceAllowed } from "@/lib/server-license";
import { checkRateLimit } from "@/lib/security";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

const INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
const INNERTUBE_CLIENT_VERSION = "2.20231121.00.00";

interface InnertubeFormat {
  url?: string;
  mimeType?: string;
  quality?: string;
  qualityLabel?: string;
  itag?: number;
  bitrate?: number;
  width?: number;
  height?: number;
  audioQuality?: string;
}

async function fetchInnertubeData(videoId: string): Promise<any> {
  const body = {
    videoId,
    context: {
      client: {
        clientName: "ANDROID",
        clientVersion: "19.09.37",
        androidSdkVersion: 30,
        hl: "en",
        gl: "US",
        userAgent:
          "com.google.android.youtube/19.09.37 (Linux; U; Android 11) gzip",
      },
    },
  };

  const res = await fetch(
    `https://www.youtube.com/youtubei/v1/player?key=${INNERTUBE_API_KEY}&prettyPrint=false`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-YouTube-Client-Name": "3",
        "X-YouTube-Client-Version": "19.09.37",
        "User-Agent":
          "com.google.android.youtube/19.09.37 (Linux; U; Android 11) gzip",
        Origin: "https://www.youtube.com",
      },
      body: JSON.stringify(body),
    }
  );

  if (!res.ok) {
    throw new Error(`Innertube HTTP ${res.status}`);
  }

  return res.json();
}

function pickBestFormat(formats: InnertubeFormat[]): InnertubeFormat | null {
  // Prefer progressive (audio+video) formats with high quality
  const progressive = formats.filter(
    (f) =>
      f.url &&
      f.mimeType?.includes("video/mp4") &&
      !f.mimeType?.includes("webm")
  );

  if (progressive.length === 0) return null;

  // Sort by height descending (best quality first)
  progressive.sort((a, b) => (b.height || 0) - (a.height || 0));

  // Try to find 720p or best available
  const hd = progressive.find(
    (f) => (f.height || 0) >= 480 && (f.height || 0) <= 1080
  );
  return hd || progressive[0];
}

export async function GET(req: NextRequest) {
  if (!(await isServiceAllowed())) {
    return NextResponse.json(
      { success: false, error: "Service temporarily suspended." },
      { status: 403 }
    );
  }

  const ip = req.headers.get("x-forwarded-for") || "127.0.0.1";
  if (!checkRateLimit(ip, 60, 60000)) {
    return NextResponse.json(
      { success: false, error: "Too Many Requests" },
      { status: 429 }
    );
  }

  const { searchParams } = new URL(req.url);
  const videoId = searchParams.get("id");

  if (!videoId || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
    return NextResponse.json(
      { success: false, error: "Invalid YouTube video ID" },
      { status: 400 }
    );
  }

  try {
    const data = await fetchInnertubeData(videoId);

    // Check playability
    const status = data?.playabilityStatus?.status;
    if (status === "ERROR" || status === "UNPLAYABLE") {
      const reason =
        data?.playabilityStatus?.reason || "Video unavailable";
      return NextResponse.json(
        { success: false, error: reason },
        { status: 404 }
      );
    }

    const streamingData = data?.streamingData;
    if (!streamingData) {
      return NextResponse.json(
        { success: false, error: "No streaming data found" },
        { status: 404 }
      );
    }

    // Try progressive formats (combined audio+video)
    const formats: InnertubeFormat[] = streamingData.formats || [];
    const adaptiveFormats: InnertubeFormat[] =
      streamingData.adaptiveFormats || [];
    const allFormats = [...formats, ...adaptiveFormats];

    const best = pickBestFormat(allFormats);

    if (!best?.url) {
      return NextResponse.json(
        { success: false, error: "No playable format found" },
        { status: 404 }
      );
    }

    return NextResponse.json({
      success: true,
      videoId,
      url: best.url,
      quality: best.qualityLabel || best.quality || "HD",
      mimeType: best.mimeType || "video/mp4",
    });
  } catch (err: any) {
    console.error("[YTStream] Error:", err?.message);
    return NextResponse.json(
      { success: false, error: err?.message || "Unknown error" },
      { status: 500 }
    );
  }
}
