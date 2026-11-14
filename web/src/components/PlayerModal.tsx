"use client";

import React, { useEffect, useRef, useState } from "react";
import Hls from "hls.js";
import { trackPlaybackError, trackPlaybackStarted } from "@/lib/analytics";
import {
  Play,
  Pause,
  Volume2,
  VolumeX,
  Maximize2,
  Minimize2,
  X,
  Radio,
  Settings,
  RotateCcw,
  RotateCw,
  PictureInPicture,
  Layers,
  Gauge,
  Download,
  Zap,
  ChevronLeft,
  ChevronRight,
  Tv,
  Check,
  Sparkles,
  ShieldCheck,
  Activity,
  Wifi,
} from "lucide-react";

export interface StreamSource {
  name: string;
  url: string;
  isDirect?: boolean;
}

interface PlayerModalProps {
  title: string;
  subtitle?: string;
  poster?: string;
  streams: StreamSource[];
  activeStreamIndex?: number;
  onClose: () => void;
  onSwitchStream?: (index: number) => void;
  onNextEpisode?: () => void;
  onPrevEpisode?: () => void;
  hasNextEpisode?: boolean;
  hasPrevEpisode?: boolean;
}

export type AspectRatioMode = "contain" | "cover" | "fill";

export type BandwidthTier = "smart_hd" | "ultra_saver" | "balanced" | "source" | "high";

export interface BandwidthProfile {
  id: BandwidthTier;
  title: string;
  badge: string;
  description: string;
  bitrateEstimate: string;
  hourlyConsumption: string;
  maxHeight?: number;
  bufferLength: number;
}

export const BANDWIDTH_PROFILES: BandwidthProfile[] = [
  {
    id: "smart_hd",
    title: "🌟 HD موفّر (ذكي)",
    badge: "توفير 60% من الباقة",
    description: "أعلى جودة 720p ذكية مع ضغط بت ريت فائق التوفير وجودة سينمائية نقية",
    bitrateEstimate: "~1.2 Mbps",
    hourlyConsumption: "~540 ميجابايت / ساعة",
    maxHeight: 720,
    bufferLength: 8,
  },
  {
    id: "ultra_saver",
    title: "⚡ أقصى توفير للنت",
    badge: "توفير 85% من الباقة",
    description: "أقل استهلاك ممكن للباقة، ممتاز لشبكات الهاتف 3G/4G والإنترنت المحدود",
    bitrateEstimate: "~500 Kbps",
    hourlyConsumption: "~220 ميجابايت / ساعة",
    maxHeight: 480,
    bufferLength: 4,
  },
  {
    id: "balanced",
    title: "⚖️ جودة متوازنة",
    badge: "توازن تام",
    description: "توازن مثالي بين نقاء الصورة وسرعة التحميل وجودة 720p / 1080p",
    bitrateEstimate: "~2.5 Mbps",
    hourlyConsumption: "~1.1 جيجابايت / ساعة",
    maxHeight: 1080,
    bufferLength: 14,
  },
  {
    id: "source",
    title: "💎 جودة المصدر",
    badge: "بدون أي ضغط",
    description: "البث الخام الأصلي كما تم رفعه من السيرفر مباشرة بأعلى تفاصيل ووضوح",
    bitrateEstimate: "مفتوح (Source)",
    hourlyConsumption: "~2.2 جيجابايت / ساعة",
    maxHeight: undefined,
    bufferLength: 22,
  },
  {
    id: "high",
    title: "🚀 أعلى جودة فائقة",
    badge: "أقصى أداء",
    description: "أقصى معدل بت ريت متاح مع تخزين مؤقت استباقي طويل لأقوى شاشات العرض",
    bitrateEstimate: "Max Bitrate",
    hourlyConsumption: "~3.5 جيجابايت / ساعة",
    maxHeight: undefined,
    bufferLength: 32,
  },
];

export function extractYouTubeId(url?: string): string | null {
  if (!url) return null;
  const match = url.match(
    /(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|shorts\/)|&v=)([^#&?]{11})/
  );
  return match ? match[1] : null;
}

declare global {
  interface Window {
    YT: any;
    onYouTubeIframeAPIReady: () => void;
  }
}

export default function PlayerModal({
  title,
  subtitle,
  poster,
  streams,
  activeStreamIndex = 0,
  onClose,
  onSwitchStream,
  onNextEpisode,
  onPrevEpisode,
  hasNextEpisode = false,
  hasPrevEpisode = false,
}: PlayerModalProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const progressBarRef = useRef<HTMLDivElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const ytPlayerRef = useRef<any>(null);
  const ytSyncTimer = useRef<NodeJS.Timeout | null>(null);
  const hideControlsTimer = useRef<NodeJS.Timeout | null>(null);
  const lastPlaybackReported = useRef<string | null>(null);

  const [currentIndex, setCurrentIndex] = useState(activeStreamIndex);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [volume, setVolume] = useState(1);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [buffered, setBuffered] = useState(0);
  const [isLive, setIsLive] = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [hoverTime, setHoverTime] = useState<number | null>(null);
  const [hoverPosition, setHoverPosition] = useState<number>(0);
  const [aspectMode, setAspectMode] = useState<AspectRatioMode>("contain");

  // Smart Bandwidth Management (5 Tiers)
  const [bandwidthTier, setBandwidthTier] = useState<BandwidthTier>("smart_hd");
  const [showBandwidthMenu, setShowBandwidthMenu] = useState(false);
  const [liveBitrate, setLiveBitrate] = useState<string>("1.2 Mbps");

  // Quality settings
  const [qualities, setQualities] = useState<{ id: number; height: number; name: string }[]>([]);
  const [currentQuality, setCurrentQuality] = useState(-1); // -1 = Auto
  const [showQualityMenu, setShowQualityMenu] = useState(false);
  const [showSpeedMenu, setShowSpeedMenu] = useState(false);
  const [showAspectMenu, setShowAspectMenu] = useState(false);

  const [showControls, setShowControls] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isBuffering, setIsBuffering] = useState(true);
  const [isAutoplayMuted, setIsAutoplayMuted] = useState(false);
  const [useIframeFallback, setUseIframeFallback] = useState(false);
  const [downloadingVideo, setDownloadingVideo] = useState(false);
  const [seekFeedback, setSeekFeedback] = useState<string | null>(null);

  const currentStream = streams[currentIndex] || streams[0];
  const youtubeVideoId = extractYouTubeId(currentStream?.url);
  const isYouTube = Boolean(youtubeVideoId);

  const isMovieOrSeries = Boolean(
    subtitle?.includes("سيرفر") ||
      subtitle?.includes("الحلقة") ||
      subtitle?.includes("الموسم") ||
      subtitle?.includes("ملخص") ||
      currentStream?.name?.includes("سيرفر") ||
      currentStream?.url?.includes("datahowa") ||
      currentStream?.url?.includes("govid") ||
      currentStream?.url?.includes("cinema") ||
      isYouTube
  );

  const isStreamLive =
    !isMovieOrSeries &&
    (isLive || title.includes("مباراة") || title.includes("beIN") || title.includes("SSC"));

  const isEmbed = Boolean(
    currentStream?.url &&
      !isYouTube &&
      !currentStream.url.includes(".m3u8") &&
      !currentStream.url.includes("/api/proxy/stream") &&
      !currentStream.url.includes(".mp4") &&
      !currentStream.url.includes("govid.live") &&
      !currentStream.url.includes("datahowa") &&
      !currentStream.url.includes("pl.dood")
  );

  // Apply Smart Bandwidth profile constraints to HLS instance
  const applyBandwidthPolicy = (hls: Hls, tier: BandwidthTier) => {
    if (!hls) return;
    const profile = BANDWIDTH_PROFILES.find((p) => p.id === tier) || BANDWIDTH_PROFILES[0];
    setLiveBitrate(profile.bitrateEstimate);

    if (hls.levels && hls.levels.length > 0) {
      if (profile.maxHeight) {
        let targetIndex = -1;
        let maxH = 0;
        hls.levels.forEach((lvl, idx) => {
          if (lvl.height && lvl.height <= profile.maxHeight! && lvl.height > maxH) {
            maxH = lvl.height;
            targetIndex = idx;
          }
        });
        if (targetIndex !== -1) {
          hls.autoLevelCapping = targetIndex;
        } else {
          hls.autoLevelCapping = 0; // lowest available
        }
      } else {
        hls.autoLevelCapping = -1; // uncapped
      }
    }

    hls.config.maxBufferLength = profile.bufferLength;
    hls.config.maxMaxBufferLength = profile.bufferLength * 2;
  };

  // YouTube IFrame API Controller
  useEffect(() => {
    if (!isYouTube || !youtubeVideoId) return;

    setErrorMsg(null);
    setIsBuffering(true);
    let isDisposed = false;

    const initYouTubePlayer = () => {
      if (isDisposed) return;
      try {
        if (ytPlayerRef.current) {
          try {
            ytPlayerRef.current.destroy();
          } catch {}
          ytPlayerRef.current = null;
        }

        const ytTarget = document.getElementById("yt-pure-player-mount");
        if (!ytTarget) return;

        // Clear target
        ytTarget.innerHTML = "";
        const playerDiv = document.createElement("div");
        playerDiv.id = "yt-player-inner";
        ytTarget.appendChild(playerDiv);

        const player = new window.YT.Player("yt-player-inner", {
          videoId: youtubeVideoId,
          width: "100%",
          height: "100%",
          playerVars: {
            autoplay: 1,
            controls: 0,
            disablekb: 1,
            enablejsapi: 1,
            fs: 0,
            iv_load_policy: 3,
            modestbranding: 1,
            rel: 0,
            showinfo: 0,
            playsinline: 1,
            origin: typeof window !== "undefined" ? window.location.origin : undefined,
          },
          events: {
            onReady: (event: any) => {
              if (isDisposed) return;
              ytPlayerRef.current = event.target;
              setIsBuffering(false);
              setIsPlaying(true);
              try {
                event.target.playVideo();
                const dur = event.target.getDuration();
                if (dur && dur > 0) setDuration(dur);
                event.target.setVolume(Math.round(volume * 100));
                if (isMuted) event.target.mute();
              } catch {}
            },
            onStateChange: (event: any) => {
              if (isDisposed) return;
              // YT.PlayerState: 1 = PLAYING, 2 = PAUSED, 3 = BUFFERING, 0 = ENDED
              if (event.data === 1) {
                setIsPlaying(true);
                setIsBuffering(false);
                const dur = event.target.getDuration();
                if (dur && dur > 0) setDuration(dur);
              } else if (event.data === 2) {
                setIsPlaying(false);
                setIsBuffering(false);
              } else if (event.data === 3) {
                setIsBuffering(true);
              } else if (event.data === 0) {
                setIsPlaying(false);
                if (hasNextEpisode && onNextEpisode) {
                  onNextEpisode();
                }
              }
            },
            onError: () => {
              if (isDisposed) return;
              setErrorMsg("تعذر تشغيل الفيديو من سيرفر الملخصات");
            },
          },
        });
      } catch (err) {
        console.error("YouTube Player init error:", err);
      }
    };

    // Load YouTube API script if not present
    if (!window.YT || !window.YT.Player) {
      const existingScript = document.getElementById("youtube-iframe-api-script");
      if (!existingScript) {
        const tag = document.createElement("script");
        tag.id = "youtube-iframe-api-script";
        tag.src = "https://www.youtube.com/iframe_api";
        document.head.appendChild(tag);
      }
      window.onYouTubeIframeAPIReady = () => {
        initYouTubePlayer();
      };
      const checkTimer = setInterval(() => {
        if (window.YT && window.YT.Player) {
          clearInterval(checkTimer);
          initYouTubePlayer();
        }
      }, 200);
      return () => {
        clearInterval(checkTimer);
        isDisposed = true;
      };
    } else {
      initYouTubePlayer();
    }

    return () => {
      isDisposed = true;
      if (ytPlayerRef.current) {
        try {
          ytPlayerRef.current.destroy();
        } catch {}
        ytPlayerRef.current = null;
      }
    };
  }, [youtubeVideoId, isYouTube]);

  // Sync YouTube player time ticker
  useEffect(() => {
    if (!isYouTube) return;
    if (ytSyncTimer.current) clearInterval(ytSyncTimer.current);

    ytSyncTimer.current = setInterval(() => {
      const player = ytPlayerRef.current;
      if (!player) return;
      try {
        const cur = player.getCurrentTime();
        const dur = player.getDuration();
        const fraction = player.getVideoLoadedFraction();

        if (typeof cur === "number" && !isNaN(cur)) setCurrentTime(cur);
        if (typeof dur === "number" && !isNaN(dur) && dur > 0) setDuration(dur);
        if (typeof fraction === "number" && dur > 0) setBuffered(fraction * dur);
      } catch {}
    }, 300);

    return () => {
      if (ytSyncTimer.current) clearInterval(ytSyncTimer.current);
    };
  }, [isYouTube]);

  // HLS / Direct Video stream attachment (When NOT YouTube)
  useEffect(() => {
    if (isYouTube) return;
    const video = videoRef.current;
    if (!video || !currentStream) return;

    setErrorMsg(null);
    setIsBuffering(true);

    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    const streamUrl = currentStream.url;

    if (Hls.isSupported() && (streamUrl.includes(".m3u8") || !streamUrl.includes(".mp4"))) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        startFragPrefetch: true,
        liveSyncDurationCount: 1,
        liveMaxLatencyDurationCount: 3,
        maxBufferSize: 16 * 1024 * 1024,
        manifestLoadingTimeOut: 5000,
        manifestLoadingMaxRetry: 2,
        levelLoadingTimeOut: 5000,
        fragLoadingTimeOut: 7000,
        fragLoadingMaxRetry: 2,
        progressive: true,
      });

      hlsRef.current = hls;
      hls.loadSource(streamUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, (_, data) => {
        setIsBuffering(false);
        if (!isMovieOrSeries) {
          setIsLive(hls.levels[0]?.details?.live ?? false);
        } else {
          setIsLive(false);
        }

        const parsedQualities = data.levels.map((lvl, index) => ({
          id: index,
          height: lvl.height,
          name: lvl.height ? `${lvl.height}p` : `جودة ${index + 1}`,
        }));
        setQualities(parsedQualities);

        applyBandwidthPolicy(hls, bandwidthTier);

        video.play().catch(() => {
          video.muted = true;
          setIsMuted(true);
          setIsAutoplayMuted(true);
          video.play().catch(() => setIsPlaying(false));
        });
      });

      hls.on(Hls.Events.LEVEL_LOADED, (_, data) => {
        if (data?.details) {
          if (!isMovieOrSeries) {
            setIsLive(Boolean(data.details.live));
          } else {
            setIsLive(false);
          }
          if (data.details.totalduration && data.details.totalduration > 0) {
            setDuration(data.details.totalduration);
          }
        }
      });

      hls.on(Hls.Events.FRAG_LOADED, (_, data: any) => {
        setIsBuffering(false);
        const stats = data?.frag?.stats || data?.stats;
        if (stats && stats.total && stats.bwQuote) {
          const kbps = Math.round((stats.total * 8) / (stats.bwQuote / 1000));
          if (kbps > 0) setLiveBitrate(`${(kbps / 1000).toFixed(1)} Mbps`);
        }
      });

      hls.on(Hls.Events.LEVEL_SWITCHED, (_, data) => {
        setCurrentQuality(data.level);
      });

      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              hls.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              hls.recoverMediaError();
              break;
            default:
              hls.destroy();
              setErrorMsg("تعذر تحميل البث من هذا السيرفر. جرب سيرفر بديل.");
              break;
          }
        }
      });
    } else if (video.canPlayType("application/vnd.apple.mpegurl") || streamUrl.includes(".mp4")) {
      video.src = streamUrl;
      video.addEventListener("loadedmetadata", () => {
        setIsBuffering(false);
        if (video.duration && !isNaN(video.duration) && isFinite(video.duration) && video.duration > 0) {
          setDuration(video.duration);
        }
        video.play().catch(() => {
          video.muted = true;
          setIsMuted(true);
          setIsAutoplayMuted(true);
          video.play().catch(() => setIsPlaying(false));
        });
      });
    } else {
      video.src = streamUrl;
    }

    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
    };
  }, [currentStream, isYouTube]);

  // Handle Controls Timeout
  const resetHideTimer = () => {
    setShowControls(true);
    if (hideControlsTimer.current) clearTimeout(hideControlsTimer.current);
    hideControlsTimer.current = setTimeout(() => {
      if (isPlaying) {
        setShowControls(false);
        setShowQualityMenu(false);
        setShowSpeedMenu(false);
        setShowAspectMenu(false);
        setShowBandwidthMenu(false);
      }
    }, 4500);
  };

  useEffect(() => {
    resetHideTimer();
    return () => {
      if (hideControlsTimer.current) clearTimeout(hideControlsTimer.current);
    };
  }, [isPlaying]);

  // Unified Play / Pause handler
  const handlePlayPause = () => {
    if (isYouTube) {
      const player = ytPlayerRef.current;
      if (!player) return;
      try {
        if (isPlaying) {
          player.pauseVideo();
          setIsPlaying(false);
        } else {
          player.playVideo();
          setIsPlaying(true);
        }
      } catch {}
    } else {
      const video = videoRef.current;
      if (!video) return;
      if (video.paused) {
        video.play();
        setIsPlaying(true);
      } else {
        video.pause();
        setIsPlaying(false);
      }
    }
    resetHideTimer();
  };

  const handleTimeUpdate = () => {
    const video = videoRef.current;
    if (!video) return;
    setCurrentTime(video.currentTime);
    if (video.duration && !isNaN(video.duration) && isFinite(video.duration) && video.duration > 0) {
      setDuration(video.duration);
    }
    if (video.buffered.length > 0) {
      try {
        setBuffered(video.buffered.end(video.buffered.length - 1));
      } catch {}
    }
  };

  const seekToPosition = (clientX: number) => {
    if (!progressBarRef.current) return;
    const rect = progressBarRef.current.getBoundingClientRect();
    const clickX = clientX - rect.left;
    const ratio = Math.max(0, Math.min(1, clickX / rect.width));
    const effectiveDuration = duration > 0 ? duration : (videoRef.current?.duration || 0);

    if (effectiveDuration > 0) {
      const newTime = ratio * effectiveDuration;
      if (isYouTube && ytPlayerRef.current) {
        try {
          ytPlayerRef.current.seekTo(newTime, true);
        } catch {}
      } else if (videoRef.current) {
        videoRef.current.currentTime = newTime;
      }
      setCurrentTime(newTime);
    }
  };

  const handleProgressBarClick = (e: React.MouseEvent<HTMLDivElement>) => {
    seekToPosition(e.clientX);
    resetHideTimer();
  };

  const handleProgressBarHover = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!progressBarRef.current) return;
    const rect = progressBarRef.current.getBoundingClientRect();
    const hoverX = e.clientX - rect.left;
    const ratio = Math.max(0, Math.min(1, hoverX / rect.width));
    setHoverPosition(ratio * 100);
    const effectiveDuration = duration > 0 ? duration : (videoRef.current?.duration || 0);
    if (effectiveDuration > 0) {
      setHoverTime(ratio * effectiveDuration);
    }
  };

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const target = parseFloat(e.target.value);
    if (isYouTube && ytPlayerRef.current) {
      try {
        ytPlayerRef.current.seekTo(target, true);
      } catch {}
    } else if (videoRef.current) {
      videoRef.current.currentTime = target;
    }
    setCurrentTime(target);
    resetHideTimer();
  };

  const jumpSeconds = (seconds: number) => {
    const target = Math.max(0, Math.min(duration || 10000, currentTime + seconds));
    if (isYouTube && ytPlayerRef.current) {
      try {
        ytPlayerRef.current.seekTo(target, true);
      } catch {}
    } else if (videoRef.current) {
      videoRef.current.currentTime = target;
    }
    setCurrentTime(target);
    setSeekFeedback(seconds > 0 ? `+${seconds}ث` : `${seconds}ث`);
    setTimeout(() => setSeekFeedback(null), 800);
    resetHideTimer();
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseFloat(e.target.value);
    setVolume(val);
    setIsMuted(val === 0);
    if (isYouTube && ytPlayerRef.current) {
      try {
        ytPlayerRef.current.setVolume(Math.round(val * 100));
        if (val === 0) ytPlayerRef.current.mute();
        else ytPlayerRef.current.unMute();
      } catch {}
    } else if (videoRef.current) {
      videoRef.current.volume = val;
      videoRef.current.muted = val === 0;
    }
  };

  const toggleMute = () => {
    const nextMuted = !isMuted;
    setIsMuted(nextMuted);
    if (isYouTube && ytPlayerRef.current) {
      try {
        if (nextMuted) ytPlayerRef.current.mute();
        else {
          ytPlayerRef.current.unMute();
          ytPlayerRef.current.setVolume(Math.round((volume || 1) * 100));
        }
      } catch {}
    } else if (videoRef.current) {
      videoRef.current.muted = nextMuted;
    }
  };

  const toggleFullscreen = async () => {
    const container = containerRef.current;
    if (!container) return;

    if (!document.fullscreenElement) {
      try {
        await container.requestFullscreen();
        setIsFullscreen(true);
      } catch {
        (videoRef.current as any)?.webkitEnterFullscreen?.();
      }
    } else {
      await document.exitFullscreen();
      setIsFullscreen(false);
    }
  };

  const togglePiP = async () => {
    if (isYouTube) return;
    const video = videoRef.current;
    if (!video) return;
    try {
      if (document.pictureInPictureElement) {
        await document.exitPictureInPicture();
      } else {
        await video.requestPictureInPicture();
      }
    } catch {}
  };

  const setTier = (tier: BandwidthTier) => {
    setBandwidthTier(tier);
    setShowBandwidthMenu(false);
    if (hlsRef.current) {
      applyBandwidthPolicy(hlsRef.current, tier);
    }
  };

  const changeSpeed = (rate: number) => {
    setPlaybackRate(rate);
    setShowSpeedMenu(false);
    if (isYouTube && ytPlayerRef.current) {
      try {
        ytPlayerRef.current.setPlaybackRate(rate);
      } catch {}
    } else if (videoRef.current) {
      videoRef.current.playbackRate = rate;
    }
  };

  const setQuality = (id: number) => {
    setCurrentQuality(id);
    setShowQualityMenu(false);
    if (hlsRef.current) {
      hlsRef.current.currentLevel = id;
    }
  };

  const handleDownloadCurrentVideo = () => {
    if (!currentStream?.url || isYouTube) return;
    setDownloadingVideo(true);
    try {
      const safeTitle = `${title.replace(/[\/\\?%*:|"<>]/g, "_")}.mp4`;
      const link = document.createElement("a");
      link.href = currentStream.url;
      link.setAttribute("download", safeTitle);
      link.setAttribute("target", "_blank");
      link.setAttribute("rel", "noopener noreferrer");
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (e) {
      console.error("Download error:", e);
    } finally {
      setTimeout(() => setDownloadingVideo(false), 2000);
    }
  };

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;

      switch (e.code) {
        case "Space":
          e.preventDefault();
          handlePlayPause();
          break;
        case "ArrowRight":
          e.preventDefault();
          jumpSeconds(10);
          break;
        case "ArrowLeft":
          e.preventDefault();
          jumpSeconds(-10);
          break;
        case "KeyM":
          e.preventDefault();
          toggleMute();
          break;
        case "KeyF":
          e.preventDefault();
          toggleFullscreen();
          break;
        case "Escape":
          e.preventDefault();
          onClose();
          break;
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isPlaying, isMuted, volume, duration, currentTime]);

  const formatTime = (secs: number) => {
    if (isNaN(secs) || secs < 0) return "00:00";
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = Math.floor(secs % 60);
    if (h > 0) {
      return `${h}:${m < 10 ? "0" : ""}${m}:${s < 10 ? "0" : ""}${s}`;
    }
    return `${m < 10 ? "0" : ""}${m}:${s < 10 ? "0" : ""}${s}`;
  };

  const currentProfile = BANDWIDTH_PROFILES.find((p) => p.id === bandwidthTier) || BANDWIDTH_PROFILES[0];

  const videoAspectClass =
    aspectMode === "cover"
      ? "object-cover"
      : aspectMode === "fill"
      ? "object-fill w-full h-full"
      : "object-contain";

  return (
    <div className="player-modal fixed inset-0 z-50 bg-black/95 flex items-center justify-center backdrop-blur-2xl animate-fadeIn p-0 sm:p-4 select-none">
      <div
        ref={containerRef}
        onMouseMove={resetHideTimer}
        onClick={resetHideTimer}
        className="relative w-full h-full max-w-[100vw] sm:max-w-[96vw] max-h-[100vh] sm:max-h-[94vh] bg-[#090A0E] sm:rounded-3xl overflow-hidden shadow-[0_0_60px_rgba(0,0,0,0.9)] flex flex-col justify-center border border-amber-500/25"
      >
        {/* 🎬 1. YOUTUBE CINEMA-PURE PLAYER (Eradicating YouTube Chrome/Overlays) */}
        {isYouTube ? (
          <div className="relative w-full h-full overflow-hidden flex items-center justify-center bg-black">
            {/* The scaled container pushing edge headers and watermarks outside */}
            <div
              id="yt-pure-player-mount"
              className="w-full h-full pointer-events-none select-none transition-transform duration-300"
              style={{
                transform: "scale(1.08)",
                transformOrigin: "center center",
              }}
            />

            {/* Transparent click catcher & gesture layer */}
            <div
              onClick={handlePlayPause}
              onDoubleClick={(e) => {
                const rect = e.currentTarget.getBoundingClientRect();
                const isRightSide = e.clientX - rect.left > rect.width / 2;
                jumpSeconds(isRightSide ? 10 : -10);
              }}
              className="absolute inset-0 z-10 cursor-pointer"
            />
          </div>
        ) : isEmbed || useIframeFallback ? (
          /* Generic Iframe Fallback */
          <iframe
            src={
              currentStream.url.startsWith("/api/")
                ? currentStream.url
                : `/api/proxy/embed?url=${encodeURIComponent(currentStream.url)}`
            }
            className="w-full h-full border-0 sm:rounded-3xl"
            allowFullScreen
            allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
          />
        ) : (
          /* Native HTML5 / HLS Video */
          <video
            ref={videoRef}
            poster={poster}
            playsInline
            onPlay={() => setIsPlaying(true)}
            onPause={() => setIsPlaying(false)}
            onWaiting={() => setIsBuffering(true)}
            onPlaying={() => {
              setIsBuffering(false);
              if (lastPlaybackReported.current !== currentStream?.url) {
                lastPlaybackReported.current = currentStream?.url || "unknown";
                trackPlaybackStarted(title, isStreamLive);
              }
            }}
            onError={() => trackPlaybackError(isStreamLive)}
            onTimeUpdate={handleTimeUpdate}
            onClick={handlePlayPause}
            className={`w-full h-full cursor-pointer transition-all duration-300 ${videoAspectClass}`}
          />
        )}

        {/* 🌟 Dynamic Zakzouka TV Watermark */}
        <div className="absolute top-20 right-6 z-20 pointer-events-none select-none hidden sm:flex items-center gap-2.5 px-3 py-1.5 rounded-2xl bg-[#090A0E]/85 border border-amber-500/30 shadow-2xl backdrop-blur-md">
          <div className="w-7 h-7 rounded-lg overflow-hidden border border-amber-500/40 p-0.5 bg-black flex-shrink-0">
            <img
              src="/logo.jpg"
              alt="زقزوقة"
              className="w-full h-full object-cover rounded"
              onError={(e) => {
                (e.target as HTMLImageElement).src = "/logo.png";
              }}
            />
          </div>
          <span className="text-xs font-black bg-gradient-to-r from-amber-200 to-amber-400 bg-clip-text text-transparent">
            زقزوقة بلاير • Cinema Pure
          </span>
        </div>

        {/* 📊 Live Bandwidth Engine HUD Badge */}
        <div className="absolute top-20 left-6 z-20 pointer-events-none select-none hidden sm:flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#12131D]/90 border border-amber-500/40 shadow-lg backdrop-blur-md">
          <Zap size={13} className="text-amber-400 fill-amber-400 animate-pulse" />
          <span className="text-[11px] font-black text-amber-300">
            {currentProfile.title}
          </span>
          <span className="text-[10px] text-slate-400 font-mono">({liveBitrate})</span>
        </div>

        {/* Seek Jump Feedback Toast */}
        {seekFeedback && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-30 animate-scaleUp">
            <div className="px-6 py-3 rounded-2xl bg-black/85 border border-amber-400 text-amber-300 font-black text-xl shadow-2xl backdrop-blur-md flex items-center gap-2">
              <RotateCw size={24} />
              <span>{seekFeedback}</span>
            </div>
          </div>
        )}

        {/* Unmute Autoplay Prompt */}
        {isAutoplayMuted && isPlaying && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              if (videoRef.current) {
                videoRef.current.muted = false;
                setIsMuted(false);
                setIsAutoplayMuted(false);
              }
            }}
            className="absolute top-28 right-6 z-30 flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-gradient-to-r from-amber-400 to-amber-600 text-slate-950 text-xs font-black shadow-2xl hover:scale-105 transition animate-bounce border border-amber-300/40"
          >
            <Volume2 size={16} />
            <span>انقر لتشغيل الصوت 🔊</span>
          </button>
        )}

        {/* Buffering Indicator */}
        {isBuffering && !errorMsg && (
          <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none gap-4 z-20">
            <div className="relative">
              <div className="w-20 h-20 rounded-3xl overflow-hidden border-2 border-amber-400 shadow-[0_0_25px_rgba(212,175,55,0.4)] p-1 bg-black">
                <img
                  src="/logo.jpg"
                  alt="زقزوقة TV"
                  className="w-full h-full object-cover rounded-2xl"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = "/logo.png";
                  }}
                />
              </div>
              <div className="absolute -inset-2.5 border-4 border-t-amber-400 border-r-transparent border-b-transparent border-l-transparent rounded-full animate-spin" />
            </div>
            <span className="text-xs font-black text-amber-300 tracking-widest uppercase">
              جاري تجهيز البث فائق السرعة...
            </span>
          </div>
        )}

        {/* Error Screen */}
        {errorMsg && (
          <div className="absolute inset-0 bg-black/90 flex flex-col items-center justify-center p-6 text-center z-30">
            <div className="w-16 h-16 rounded-full bg-red-500/20 text-red-500 border border-red-500/40 flex items-center justify-center mb-4 text-2xl font-bold">
              !
            </div>
            <h3 className="text-xl font-black text-white mb-2">{errorMsg}</h3>
            <p className="text-xs text-slate-400 mb-4">يمكنك تجربة المشغل المباشر أو اختيار سيرفر بديل</p>
            <div className="flex items-center gap-3 mb-6">
              <button
                onClick={() => {
                  setErrorMsg(null);
                  setUseIframeFallback(true);
                }}
                className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-amber-400 to-amber-600 text-slate-950 font-black text-xs shadow-lg transition hover:scale-105"
              >
                ▶ الانتقال للمشغل المباشر
              </button>
            </div>
            {streams.length > 1 && (
              <div className="flex gap-2 flex-wrap justify-center max-w-lg">
                {streams.map((s, idx) => (
                  <button
                    key={idx}
                    onClick={() => {
                      setCurrentIndex(idx);
                      onSwitchStream?.(idx);
                    }}
                    className={`px-4 py-2 rounded-xl text-xs font-black transition ${
                      idx === currentIndex
                        ? "bg-amber-400 text-slate-950 font-black shadow-lg"
                        : "bg-[#161726] text-slate-200 border border-white/10"
                    }`}
                  >
                    {s.name}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {/* ── Top HUD Header ────────────────────────────────────────── */}
        <div
          className={`absolute top-0 inset-x-0 p-4 sm:p-6 bg-gradient-to-b from-black/95 via-black/60 to-transparent flex items-center justify-between z-30 transition-opacity duration-300 ${
            showControls ? "opacity-100" : "opacity-0 pointer-events-none"
          }`}
        >
          <div className="flex items-center gap-3 sm:gap-4">
            <button
              onClick={onClose}
              className="w-10 h-10 sm:w-11 sm:h-11 rounded-2xl bg-black/60 hover:bg-white/20 flex items-center justify-center text-white transition border border-white/10 backdrop-blur-md"
              title="إغلاق المشغل (Esc)"
            >
              <X size={20} />
            </button>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-sm sm:text-base font-black text-white line-clamp-1">{title}</h2>
                {isStreamLive && (
                  <span className="flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-red-600/30 border border-red-500 text-red-400 text-[10px] font-black">
                    <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
                    مباشر
                  </span>
                )}
                {isYouTube && (
                  <span className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-500/20 border border-amber-500/40 text-amber-300 text-[10px] font-black">
                    ملخص سينمائي
                  </span>
                )}
              </div>
              {subtitle && <p className="text-xs text-amber-300 font-bold mt-0.5">{subtitle}</p>}
            </div>
          </div>

          {/* Quick Header Actions: Smart Bandwidth & Direct Download */}
          <div className="flex items-center gap-2">
            {/* ⚡ Smart Bandwidth Engine Button */}
            <div className="relative">
              <button
                type="button"
                onClick={() => {
                  setShowBandwidthMenu(!showBandwidthMenu);
                  setShowQualityMenu(false);
                  setShowSpeedMenu(false);
                  setShowAspectMenu(false);
                }}
                className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-black transition border shadow-sm border-amber-500/40 bg-amber-500/20 text-amber-300 hover:bg-amber-500/30"
                title="التحكم في استهلاك الباقة وجودة النت"
              >
                <Zap size={14} className="fill-amber-400 text-amber-400" />
                <span className="hidden sm:inline">{currentProfile.title}</span>
                <span className="sm:hidden">الجودة والنت</span>
              </button>

              {/* Bandwidth Selection Modal Dropdown */}
              {showBandwidthMenu && (
                <div className="absolute top-full left-0 mt-2 w-72 sm:w-80 bg-[#12131D] border border-amber-500/40 rounded-3xl p-3 shadow-2xl z-40 backdrop-blur-2xl animate-scaleUp">
                  <div className="px-3 py-2 border-b border-white/10 mb-2">
                    <p className="text-xs font-black text-amber-400 uppercase tracking-wider flex items-center gap-1.5">
                      <Zap size={14} /> محرك توفير النت والجودة
                    </p>
                    <p className="text-[10px] text-slate-400 mt-0.5">
                      اختر نمط الاستهلاك الأنسب لباقة الإنترنت الخاصة بك
                    </p>
                  </div>

                  <div className="space-y-1.5">
                    {BANDWIDTH_PROFILES.map((profile) => {
                      const isSelected = profile.id === bandwidthTier;
                      return (
                        <button
                          key={profile.id}
                          type="button"
                          onClick={() => setTier(profile.id)}
                          className={`w-full text-right p-3 rounded-2xl transition flex flex-col gap-1 border ${
                            isSelected
                              ? "bg-gradient-to-r from-amber-500/20 to-amber-500/10 border-amber-400 text-white"
                              : "border-transparent hover:bg-white/5 text-slate-300"
                          }`}
                        >
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-black flex items-center gap-1.5">
                              {profile.title}
                            </span>
                            {isSelected && <Check size={14} className="text-amber-400" />}
                          </div>
                          <p className="text-[10px] text-slate-400 leading-tight">
                            {profile.description}
                          </p>
                          <div className="flex items-center gap-3 text-[10px] font-mono text-amber-300/80 pt-1">
                            <span>الاستهلاك: {profile.hourlyConsumption}</span>
                            <span>•</span>
                            <span>السرعة: {profile.bitrateEstimate}</span>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>

            {/* Direct Download in Player (For non-youtube direct streams) */}
            {!isYouTube && (
              <button
                type="button"
                onClick={handleDownloadCurrentVideo}
                disabled={downloadingVideo}
                className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-amber-500/30 bg-gradient-to-r from-amber-500/20 to-amber-500/10 text-xs font-black text-amber-300 hover:border-amber-400 hover:text-white transition shadow-sm"
                title="تحميل مباشر للفيديو"
              >
                <Download size={14} className="stroke-[2.5]" />
                <span className="hidden sm:inline">تحميل</span>
              </button>
            )}
          </div>
        </div>

        {/* ── Bottom HUD Controls Bar ───────────────────────────────── */}
        <div
          className={`absolute bottom-0 inset-x-0 p-4 sm:p-6 bg-gradient-to-t from-black/98 via-black/70 to-transparent z-30 transition-opacity duration-300 flex flex-col gap-3 ${
            showControls ? "opacity-100" : "opacity-0 pointer-events-none"
          }`}
        >
          {/* Progress Timeline */}
          {!isStreamLive && (
            <div className="flex flex-col gap-1 w-full select-none">
              <div
                dir="ltr"
                ref={progressBarRef}
                onMouseMove={handleProgressBarHover}
                onMouseLeave={() => setHoverTime(null)}
                onClick={handleProgressBarClick}
                className="relative w-full flex items-center group cursor-pointer py-2 select-none"
              >
                {/* Floating Hover Time */}
                {hoverTime !== null && (
                  <div
                    className="absolute -top-8 -translate-x-1/2 px-2.5 py-1 rounded-lg bg-[#0D0E15] border border-amber-400 text-[11px] font-mono font-black text-amber-300 shadow-2xl pointer-events-none z-30"
                    style={{ left: `${hoverPosition}%` }}
                  >
                    {formatTime(hoverTime)}
                  </div>
                )}

                {/* Base Track */}
                <div className="w-full h-2.5 sm:h-3 bg-[#1A1C2E] rounded-full overflow-hidden relative group-hover:h-3.5 transition-all duration-200 border border-white/10 shadow-inner">
                  {/* Buffer Bar */}
                  <div
                    className="absolute left-0 top-0 bottom-0 bg-white/25 rounded-full transition-all duration-150"
                    style={{
                      width: `${Math.min(
                        100,
                        Math.max(0, (buffered / (duration || 1)) * 100)
                      )}%`,
                    }}
                  />
                  {/* Active Progress Bar in Royal Gold */}
                  <div
                    className="absolute left-0 top-0 bottom-0 bg-gradient-to-r from-amber-600 via-amber-400 to-amber-300 rounded-full shadow-[0_0_15px_rgba(212,175,55,0.9)]"
                    style={{
                      width: `${Math.min(
                        100,
                        Math.max(0, (currentTime / (duration || 1)) * 100)
                      )}%`,
                    }}
                  />
                </div>

                {/* Scrubber Thumb */}
                <div
                  className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-4 h-4 sm:w-5 sm:h-5 rounded-full bg-amber-400 border-2 border-white shadow-[0_0_15px_rgba(212,175,55,1)] group-hover:scale-125 transition-transform pointer-events-none z-20"
                  style={{
                    left: `${Math.min(
                      100,
                      Math.max(0, (currentTime / (duration || 1)) * 100)
                    )}%`,
                  }}
                />

                <input
                  type="range"
                  min={0}
                  max={duration > 0 ? duration : 100}
                  step={0.1}
                  value={currentTime}
                  onChange={handleSeek}
                  className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-30"
                />
              </div>
            </div>
          )}

          {/* Controls Buttons Row */}
          <div className="flex items-center justify-between">
            {/* Left Controls */}
            <div className="flex items-center gap-2.5 sm:gap-3.5">
              {/* Play / Pause Button in Gold Gradient */}
              <button
                type="button"
                onClick={handlePlayPause}
                className="w-11 h-11 sm:w-12 sm:h-12 rounded-2xl bg-gradient-to-tr from-amber-400 to-amber-600 text-slate-950 font-black flex items-center justify-center hover:scale-105 active:scale-95 transition-all shadow-[0_0_20px_rgba(212,175,55,0.4)]"
                title={isPlaying ? "إيقاف مؤقت (Space)" : "تشغيل (Space)"}
              >
                {isPlaying ? (
                  <Pause size={20} className="fill-current" />
                ) : (
                  <Play size={20} className="fill-current ml-0.5" />
                )}
              </button>

              {/* Prev Episode Button */}
              {hasPrevEpisode && onPrevEpisode && (
                <button
                  type="button"
                  onClick={onPrevEpisode}
                  className="px-3 py-2 rounded-xl border border-amber-500/30 bg-[#141524] hover:bg-amber-500/20 text-xs font-black text-amber-300 transition flex items-center gap-1"
                  title="الحلقة السابقة"
                >
                  <ChevronRight size={14} />
                  <span className="hidden md:inline">الحلقة السابقة</span>
                </button>
              )}

              {/* Next Episode Button */}
              {hasNextEpisode && onNextEpisode && (
                <button
                  type="button"
                  onClick={onNextEpisode}
                  className="px-3 py-2 rounded-xl border border-amber-500/30 bg-[#141524] hover:bg-amber-500/20 text-xs font-black text-amber-300 transition flex items-center gap-1"
                  title="الحلقة التالية"
                >
                  <span className="hidden md:inline">الحلقة التالية</span>
                  <ChevronLeft size={14} />
                </button>
              )}

              {/* Seek 10s Backward & Forward */}
              {!isStreamLive && (
                <>
                  <button
                    type="button"
                    onClick={() => jumpSeconds(-10)}
                    className="w-8 h-8 sm:w-9 sm:h-9 rounded-xl bg-black/40 hover:bg-white/10 text-slate-300 hover:text-white flex items-center justify-center transition border border-white/5"
                    title="تأخير 10 ثوانٍ"
                  >
                    <RotateCcw size={15} />
                  </button>
                  <button
                    type="button"
                    onClick={() => jumpSeconds(10)}
                    className="w-8 h-8 sm:w-9 sm:h-9 rounded-xl bg-black/40 hover:bg-white/10 text-slate-300 hover:text-white flex items-center justify-center transition border border-white/5"
                    title="تقديم 10 ثوانٍ"
                  >
                    <RotateCw size={15} />
                  </button>
                </>
              )}

              {/* Volume Slider */}
              <div className="flex items-center gap-1.5 group">
                <button
                  type="button"
                  onClick={toggleMute}
                  className="text-slate-300 hover:text-white transition p-1"
                  title="كتم الصوت (M)"
                >
                  {isMuted || volume === 0 ? <VolumeX size={18} /> : <Volume2 size={18} />}
                </button>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={isMuted ? 0 : volume}
                  onChange={handleVolumeChange}
                  className="w-14 sm:w-20 h-1.5 accent-amber-400 bg-white/20 rounded-lg cursor-pointer"
                />
              </div>

              {/* Timestamp Counter */}
              {!isStreamLive && (
                <div className="hidden sm:flex items-center gap-1.5 px-3 py-1 rounded-xl bg-black/60 border border-white/10 text-xs font-mono font-bold">
                  <span className="text-amber-300 font-black">{formatTime(currentTime)}</span>
                  <span className="text-slate-500">/</span>
                  <span className="text-slate-300">
                    {duration > 0 ? formatTime(duration) : "00:00"}
                  </span>
                </div>
              )}
            </div>

            {/* Right Controls */}
            <div className="flex items-center gap-2 relative">
              {/* Aspect Ratio Menu (for native video) */}
              {!isYouTube && (
                <div className="relative">
                  <button
                    type="button"
                    onClick={() => {
                      setShowAspectMenu(!showAspectMenu);
                      setShowQualityMenu(false);
                      setShowSpeedMenu(false);
                      setShowBandwidthMenu(false);
                    }}
                    className="flex items-center gap-1 px-2.5 py-1.5 sm:px-3 sm:py-2 rounded-xl bg-black/50 hover:bg-white/10 text-xs font-bold text-slate-200 border border-white/10 transition"
                    title="أبعاد الشاشة"
                  >
                    <Tv size={14} className="text-amber-400" />
                    <span className="hidden sm:inline">الأبعاد</span>
                  </button>

                  {showAspectMenu && (
                    <div className="absolute bottom-full right-0 mb-2 py-1.5 w-32 bg-[#12131D] border border-amber-500/30 rounded-2xl shadow-2xl z-30 overflow-hidden backdrop-blur-xl">
                      <button
                        type="button"
                        onClick={() => {
                          setAspectMode("contain");
                          setShowAspectMenu(false);
                        }}
                        className={`w-full px-3.5 py-2 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                          aspectMode === "contain" ? "text-amber-300" : "text-slate-300"
                        }`}
                      >
                        <span>احتواء كامل</span>
                        {aspectMode === "contain" && <Check size={14} />}
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setAspectMode("cover");
                          setShowAspectMenu(false);
                        }}
                        className={`w-full px-3.5 py-2 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                          aspectMode === "cover" ? "text-amber-300" : "text-slate-300"
                        }`}
                      >
                        <span>ملء الشاشة</span>
                        {aspectMode === "cover" && <Check size={14} />}
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setAspectMode("fill");
                          setShowAspectMenu(false);
                        }}
                        className={`w-full px-3.5 py-2 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                          aspectMode === "fill" ? "text-amber-300" : "text-slate-300"
                        }`}
                      >
                        <span>تمدد كامل</span>
                        {aspectMode === "fill" && <Check size={14} />}
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Playback Speed Menu */}
              {!isStreamLive && (
                <div className="relative">
                  <button
                    type="button"
                    onClick={() => {
                      setShowSpeedMenu(!showSpeedMenu);
                      setShowQualityMenu(false);
                      setShowAspectMenu(false);
                      setShowBandwidthMenu(false);
                    }}
                    className="flex items-center gap-1 px-2.5 py-1.5 sm:px-3 sm:py-2 rounded-xl bg-black/50 hover:bg-white/10 text-xs font-black text-slate-200 border border-white/10 transition"
                    title="سرعة التشغيل"
                  >
                    <Gauge size={14} />
                    <span>{playbackRate}x</span>
                  </button>

                  {showSpeedMenu && (
                    <div className="absolute bottom-full right-0 mb-2 py-1.5 w-28 bg-[#12131D] border border-amber-500/30 rounded-2xl shadow-2xl z-30 overflow-hidden backdrop-blur-xl">
                      {[0.5, 0.75, 1, 1.25, 1.5, 2].map((rate) => (
                        <button
                          key={rate}
                          type="button"
                          onClick={() => changeSpeed(rate)}
                          className={`w-full px-3 py-1.5 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                            playbackRate === rate ? "text-amber-300 font-black" : "text-slate-300"
                          }`}
                        >
                          <span>{rate}x</span>
                          {playbackRate === rate && <Check size={14} />}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Quality Menu Button (For HLS streams) */}
              {!isYouTube && qualities.length > 0 && (
                <div className="relative">
                  <button
                    type="button"
                    onClick={() => {
                      setShowQualityMenu(!showQualityMenu);
                      setShowSpeedMenu(false);
                      setShowAspectMenu(false);
                      setShowBandwidthMenu(false);
                    }}
                    className="flex items-center gap-1.5 px-2.5 py-1.5 sm:px-3 sm:py-2 rounded-xl bg-black/50 hover:bg-white/10 text-xs font-black text-slate-200 border border-white/10 transition"
                  >
                    <Settings size={14} />
                    <span>
                      {currentQuality === -1
                        ? "تلقائي"
                        : qualities[currentQuality]?.name || "تلقائي"}
                    </span>
                  </button>

                  {showQualityMenu && (
                    <div className="absolute bottom-full right-0 mb-2 py-1.5 w-36 bg-[#12131D] border border-amber-500/30 rounded-2xl shadow-2xl z-30 overflow-hidden backdrop-blur-xl">
                      <button
                        type="button"
                        onClick={() => setQuality(-1)}
                        className={`w-full px-3.5 py-2 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                          currentQuality === -1 ? "text-amber-300 font-black" : "text-slate-300"
                        }`}
                      >
                        <span>تلقائي (Auto)</span>
                        {currentQuality === -1 && <Check size={14} />}
                      </button>
                      {qualities.map((q) => (
                        <button
                          key={q.id}
                          type="button"
                          onClick={() => setQuality(q.id)}
                          className={`w-full px-3.5 py-2 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                            currentQuality === q.id
                              ? "text-amber-300 font-black"
                              : "text-slate-300"
                          }`}
                        >
                          <span>{q.name}</span>
                          {currentQuality === q.id && <Check size={14} />}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* PiP Button */}
              {!isYouTube && (
                <button
                  type="button"
                  onClick={togglePiP}
                  className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-black/50 hover:bg-white/10 text-slate-300 hover:text-white flex items-center justify-center transition border border-white/10"
                  title="وضع صورة داخل صورة (PiP)"
                >
                  <PictureInPicture size={17} />
                </button>
              )}

              {/* Fullscreen Button */}
              <button
                type="button"
                onClick={toggleFullscreen}
                className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-black/50 hover:bg-white/10 text-slate-300 hover:text-white flex items-center justify-center transition border border-white/10"
                title="شاشة كاملة (F)"
              >
                {isFullscreen ? <Minimize2 size={18} /> : <Maximize2 size={18} />}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
