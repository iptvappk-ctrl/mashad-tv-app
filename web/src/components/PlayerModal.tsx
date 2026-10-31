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

type AspectRatioMode = "contain" | "cover" | "fill";

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

  // Smart Data Saver Mode (720p cap @ ~1 Mbps, buffer cap, saving ~60% quota)
  const [dataSaver, setDataSaver] = useState(true);
  const [aspectMode, setAspectMode] = useState<AspectRatioMode>("contain");

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

  const currentStream = streams[currentIndex] || streams[0];

  const isMovieOrSeries = Boolean(
    subtitle?.includes("سيرفر") ||
      subtitle?.includes("الحلقة") ||
      subtitle?.includes("الموسم") ||
      currentStream?.name?.includes("سيرفر") ||
      currentStream?.url?.includes("datahowa") ||
      currentStream?.url?.includes("govid") ||
      currentStream?.url?.includes("cinema")
  );

  const isStreamLive =
    !isMovieOrSeries &&
    (isLive || title.includes("مباراة") || title.includes("beIN") || title.includes("SSC"));

  useEffect(() => {
    lastPlaybackReported.current = null;
  }, [currentStream?.url]);

  const isEmbed = Boolean(
    currentStream?.url &&
      !currentStream.url.includes(".m3u8") &&
      !currentStream.url.includes("/api/proxy/stream") &&
      !currentStream.url.includes(".mp4") &&
      !currentStream.url.includes("govid.live") &&
      !currentStream.url.includes("datahowa") &&
      !currentStream.url.includes("pl.dood")
  );

  // Apply Smart Data Saver constraints to HLS instance
  const applyDataSaverPolicy = (hls: Hls, enabled: boolean) => {
    if (!hls || !hls.levels || hls.levels.length === 0) return;
    if (enabled) {
      // Find highest level with height <= 720 (High Definition with low data footprint)
      let targetIndex = -1;
      let maxH = 0;
      hls.levels.forEach((lvl, idx) => {
        if (lvl.height && lvl.height <= 720 && lvl.height > maxH) {
          maxH = lvl.height;
          targetIndex = idx;
        }
      });
      if (targetIndex !== -1) {
        hls.autoLevelCapping = targetIndex;
      }
      hls.config.maxBufferLength = 6;
      hls.config.maxMaxBufferLength = 10;
    } else {
      hls.autoLevelCapping = -1; // Uncapped
      hls.config.maxBufferLength = 20;
      hls.config.maxMaxBufferLength = 40;
    }
  };

  // Initialize and attach HLS stream
  useEffect(() => {
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
        maxBufferLength: dataSaver ? 6 : 15,
        maxMaxBufferLength: dataSaver ? 10 : 30,
        maxBufferSize: 12 * 1024 * 1024,
        manifestLoadingTimeOut: 4500,
        manifestLoadingMaxRetry: 2,
        levelLoadingTimeOut: 4500,
        fragLoadingTimeOut: 6500,
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

        applyDataSaverPolicy(hls, dataSaver);

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

      hls.on(Hls.Events.FRAG_LOADED, () => {
        setIsBuffering(false);
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
              setErrorMsg("تعذر تحميل البث من هذا السيرفر. يمكنك تجربة سيرفر بديل.");
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
  }, [currentStream]);

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
      }
    }, 4000);
  };

  useEffect(() => {
    resetHideTimer();
    return () => {
      if (hideControlsTimer.current) clearTimeout(hideControlsTimer.current);
    };
  }, [isPlaying]);

  const handlePlayPause = () => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) {
      video.play();
      setIsPlaying(true);
    } else {
      video.pause();
      setIsPlaying(false);
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
    if (!progressBarRef.current || !videoRef.current) return;
    const rect = progressBarRef.current.getBoundingClientRect();
    const clickX = clientX - rect.left;
    const ratio = Math.max(0, Math.min(1, clickX / rect.width));
    const effectiveDuration = duration > 0 ? duration : (videoRef.current.duration || 0);
    if (effectiveDuration > 0) {
      const newTime = ratio * effectiveDuration;
      videoRef.current.currentTime = newTime;
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
    const video = videoRef.current;
    if (!video) return;
    const target = parseFloat(e.target.value);
    video.currentTime = target;
    setCurrentTime(target);
    resetHideTimer();
  };

  const jumpSeconds = (seconds: number) => {
    const video = videoRef.current;
    if (!video) return;
    video.currentTime = Math.max(0, Math.min(duration, video.currentTime + seconds));
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const video = videoRef.current;
    if (!video) return;
    const val = parseFloat(e.target.value);
    video.volume = val;
    setVolume(val);
    setIsMuted(val === 0);
  };

  const toggleMute = () => {
    const video = videoRef.current;
    if (!video) return;
    video.muted = !isMuted;
    setIsMuted(!isMuted);
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

  const toggleDataSaver = () => {
    const next = !dataSaver;
    setDataSaver(next);
    if (hlsRef.current) {
      applyDataSaverPolicy(hlsRef.current, next);
    }
  };

  const handleDownloadCurrentVideo = () => {
    if (!currentStream?.url) return;
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

  const changeSpeed = (rate: number) => {
    const video = videoRef.current;
    if (video) {
      video.playbackRate = rate;
      setPlaybackRate(rate);
    }
    setShowSpeedMenu(false);
  };

  const setQuality = (id: number) => {
    if (hlsRef.current) {
      hlsRef.current.currentLevel = id;
      setCurrentQuality(id);
    }
    setShowQualityMenu(false);
  };

  // Keyboard Shortcuts Handler
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        if (!document.fullscreenElement) onClose();
      } else if (e.key === " " || e.key === "k") {
        e.preventDefault();
        handlePlayPause();
      } else if (e.key === "f") {
        e.preventDefault();
        toggleFullscreen();
      } else if (e.key === "m") {
        e.preventDefault();
        toggleMute();
      } else if (e.key === "ArrowRight") {
        jumpSeconds(10);
      } else if (e.key === "ArrowLeft") {
        jumpSeconds(-10);
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        if (videoRef.current) {
          const next = Math.min(1, volume + 0.1);
          videoRef.current.volume = next;
          setVolume(next);
        }
      } else if (e.key === "ArrowDown") {
        e.preventDefault();
        if (videoRef.current) {
          const next = Math.max(0, volume - 0.1);
          videoRef.current.volume = next;
          setVolume(next);
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose, isPlaying, isMuted, volume]);

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

  const videoAspectClass =
    aspectMode === "cover"
      ? "object-cover"
      : aspectMode === "fill"
      ? "object-fill w-full h-full"
      : "object-contain";

  return (
    <div className="fixed inset-0 z-50 bg-black/95 flex items-center justify-center backdrop-blur-2xl animate-fadeIn p-0 sm:p-4">
      <div
        ref={containerRef}
        onMouseMove={resetHideTimer}
        onClick={resetHideTimer}
        className="relative w-full h-full max-w-[100vw] sm:max-w-[96vw] max-h-[100vh] sm:max-h-[94vh] bg-[#090A0E] sm:rounded-3xl overflow-hidden shadow-[0_0_60px_rgba(0,0,0,0.9)] flex flex-col justify-center select-none border border-amber-500/25"
      >
        {/* Video Player or Embed Fallback */}
        {isEmbed || useIframeFallback ? (
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

        {/* Dynamic Zakzouka TV Watermark */}
        <div className="absolute top-20 right-6 z-20 pointer-events-none select-none hidden sm:flex items-center gap-2.5 px-3 py-1.5 rounded-2xl bg-[#090A0E]/85 border border-amber-500/30 shadow-2xl backdrop-blur-md">
          <div className="w-7 h-7 rounded-lg overflow-hidden border border-amber-500/40 p-0.5 bg-black flex-shrink-0">
            <img src="/logo.jpg" alt="زقزوقة" className="w-full h-full object-cover rounded" onError={(e) => { (e.target as HTMLImageElement).src = "/logo.png"; }} />
          </div>
          <span className="text-xs font-black bg-gradient-to-r from-amber-200 to-amber-400 bg-clip-text text-transparent">
            زقزوقة بلاير
          </span>
        </div>

        {/* Data Saver Mode Active Pill Badge */}
        {dataSaver && (
          <div className="absolute top-20 left-6 z-20 pointer-events-none select-none flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-950/80 border border-emerald-500/40 shadow-lg backdrop-blur-md">
            <Zap size={13} className="text-emerald-400 fill-emerald-400 animate-pulse" />
            <span className="text-[11px] font-black text-emerald-300">
              توفير النت الفائق (توفير 60% من الباقة)
            </span>
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
          <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none gap-4">
            <div className="relative">
              <div className="w-20 h-20 rounded-3xl overflow-hidden border-2 border-amber-400 shadow-[0_0_25px_rgba(212,175,55,0.4)] p-1 bg-black">
                <img
                  src="/logo.jpg"
                  alt="زقزوقة TV"
                  className="w-full h-full object-cover rounded-2xl"
                  onError={(e) => { (e.target as HTMLImageElement).src = "/logo.png"; }}
                />
              </div>
              <div className="absolute -inset-2.5 border-4 border-t-amber-400 border-r-transparent border-b-transparent border-l-transparent rounded-full animate-spin" />
            </div>
            <span className="text-xs font-black text-amber-300 tracking-widest uppercase">
              جاري تجهيز البث السريع...
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
                onClick={() => { setErrorMsg(null); setUseIframeFallback(true); }}
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
                    onClick={() => { setCurrentIndex(idx); onSwitchStream?.(idx); }}
                    className={`px-4 py-2 rounded-xl text-xs font-black transition ${
                      idx === currentIndex ? "bg-amber-400 text-slate-950 font-black shadow-lg" : "bg-[#161726] text-slate-200 border border-white/10"
                    }`}
                  >
                    {s.name}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Top HUD Header */}
        <div
          className={`absolute top-0 inset-x-0 p-4 sm:p-6 bg-gradient-to-b from-black/95 via-black/60 to-transparent flex items-center justify-between z-20 transition-opacity duration-300 ${
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
              </div>
              {subtitle && <p className="text-xs text-amber-300 font-bold mt-0.5">{subtitle}</p>}
            </div>
          </div>

          {/* Quick Header Actions: Data Saver & Direct Download */}
          <div className="flex items-center gap-2">
            {/* Smart Data Saver Toggle */}
            <button
              type="button"
              onClick={toggleDataSaver}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-black transition border shadow-sm ${
                dataSaver
                  ? "border-emerald-500/50 bg-emerald-500/20 text-emerald-300 shadow-emerald-500/20"
                  : "border-white/10 bg-black/60 text-slate-300 hover:text-white"
              }`}
              title="تفعيل/تعطيل وضع توفير النت الفائق"
            >
              <Zap size={14} className={dataSaver ? "fill-emerald-400 text-emerald-400" : ""} />
              <span className="hidden sm:inline">توفير النت</span>
            </button>

            {/* Direct Download in Player */}
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
          </div>
        </div>

        {/* Bottom HUD Controls Bar */}
        <div
          className={`absolute bottom-0 inset-x-0 p-4 sm:p-6 bg-gradient-to-t from-black/98 via-black/70 to-transparent z-20 transition-opacity duration-300 flex flex-col gap-3 ${
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
                    style={{ width: `${Math.min(100, Math.max(0, (buffered / (duration || 1)) * 100))}%` }}
                  />
                  {/* Active Progress Bar in Luxury Royal Gold */}
                  <div
                    className="absolute left-0 top-0 bottom-0 bg-gradient-to-r from-amber-600 via-amber-400 to-amber-300 rounded-full shadow-[0_0_15px_rgba(212,175,55,0.9)]"
                    style={{ width: `${Math.min(100, Math.max(0, (currentTime / (duration || 1)) * 100))}%` }}
                  />
                </div>

                {/* Scrubber Thumb */}
                <div
                  className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-4 h-4 sm:w-5 sm:h-5 rounded-full bg-amber-400 border-2 border-white shadow-[0_0_15px_rgba(212,175,55,1)] group-hover:scale-125 transition-transform pointer-events-none z-20"
                  style={{ left: `${Math.min(100, Math.max(0, (currentTime / (duration || 1)) * 100))}%` }}
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
                onClick={handlePlayPause}
                className="w-11 h-11 sm:w-12 sm:h-12 rounded-2xl bg-gradient-to-tr from-amber-400 to-amber-600 text-slate-950 font-black flex items-center justify-center hover:scale-105 active:scale-95 transition-all shadow-[0_0_20px_rgba(212,175,55,0.4)]"
                title={isPlaying ? "إيقاف مؤقت (Space)" : "تشغيل (Space)"}
              >
                {isPlaying ? <Pause size={20} className="fill-current" /> : <Play size={20} className="fill-current ml-0.5" />}
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

              {/* Seek 10s */}
              {!isStreamLive && (
                <>
                  <button
                    onClick={() => jumpSeconds(-10)}
                    className="w-8 h-8 sm:w-9 sm:h-9 rounded-xl bg-black/40 hover:bg-white/10 text-slate-300 hover:text-white flex items-center justify-center transition border border-white/5"
                    title="تأخير 10 ثوانٍ"
                  >
                    <RotateCcw size={15} />
                  </button>
                  <button
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
                <button onClick={toggleMute} className="text-slate-300 hover:text-white transition p-1" title="كتم الصوت (M)">
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
                  <span className="text-slate-300">{duration > 0 ? formatTime(duration) : "00:00"}</span>
                </div>
              )}
            </div>

            {/* Right Controls */}
            <div className="flex items-center gap-2 relative">
              {/* Aspect Ratio Menu */}
              <div className="relative">
                <button
                  type="button"
                  onClick={() => {
                    setShowAspectMenu(!showAspectMenu);
                    setShowQualityMenu(false);
                    setShowSpeedMenu(false);
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
                      onClick={() => { setAspectMode("contain"); setShowAspectMenu(false); }}
                      className={`w-full px-3.5 py-2 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                        aspectMode === "contain" ? "text-amber-300" : "text-slate-300"
                      }`}
                    >
                      <span>احتواء كامل</span>
                      {aspectMode === "contain" && <Check size={14} />}
                    </button>
                    <button
                      onClick={() => { setAspectMode("cover"); setShowAspectMenu(false); }}
                      className={`w-full px-3.5 py-2 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                        aspectMode === "cover" ? "text-amber-300" : "text-slate-300"
                      }`}
                    >
                      <span>ملء الشاشة</span>
                      {aspectMode === "cover" && <Check size={14} />}
                    </button>
                    <button
                      onClick={() => { setAspectMode("fill"); setShowAspectMenu(false); }}
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

              {/* Playback Speed Menu */}
              {!isStreamLive && (
                <div className="relative">
                  <button
                    onClick={() => {
                      setShowSpeedMenu(!showSpeedMenu);
                      setShowQualityMenu(false);
                      setShowAspectMenu(false);
                    }}
                    className="flex items-center gap-1 px-2.5 py-1.5 sm:px-3 sm:py-2 rounded-xl bg-black/50 hover:bg-white/10 text-xs font-black text-slate-200 border border-white/10 transition"
                  >
                    <Gauge size={14} />
                    <span>{playbackRate}x</span>
                  </button>

                  {showSpeedMenu && (
                    <div className="absolute bottom-full right-0 mb-2 py-1.5 w-28 bg-[#12131D] border border-amber-500/30 rounded-2xl shadow-2xl z-30 overflow-hidden backdrop-blur-xl">
                      {[0.5, 0.75, 1, 1.25, 1.5, 2].map((rate) => (
                        <button
                          key={rate}
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

              {/* Quality Menu Button */}
              {qualities.length > 0 && (
                <div className="relative">
                  <button
                    onClick={() => {
                      setShowQualityMenu(!showQualityMenu);
                      setShowSpeedMenu(false);
                      setShowAspectMenu(false);
                    }}
                    className="flex items-center gap-1.5 px-2.5 py-1.5 sm:px-3 sm:py-2 rounded-xl bg-black/50 hover:bg-white/10 text-xs font-black text-slate-200 border border-white/10 transition"
                  >
                    <Settings size={14} />
                    <span>{currentQuality === -1 ? "تلقائي" : qualities[currentQuality]?.name || "تلقائي"}</span>
                  </button>

                  {showQualityMenu && (
                    <div className="absolute bottom-full right-0 mb-2 py-1.5 w-36 bg-[#12131D] border border-amber-500/30 rounded-2xl shadow-2xl z-30 overflow-hidden backdrop-blur-xl">
                      <button
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
                          onClick={() => setQuality(q.id)}
                          className={`w-full px-3.5 py-2 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                            currentQuality === q.id ? "text-amber-300 font-black" : "text-slate-300"
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
              <button
                onClick={togglePiP}
                className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-black/50 hover:bg-white/10 text-slate-300 hover:text-white flex items-center justify-center transition border border-white/10"
                title="وضع صورة داخل صورة (PiP)"
              >
                <PictureInPicture size={17} />
              </button>

              {/* Fullscreen Button */}
              <button
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
