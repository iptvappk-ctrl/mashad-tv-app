"use client";

import React, { useEffect, useRef, useState } from "react";
import Hls from "hls.js";
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
  HelpCircle,
  Gauge,
  Tv,
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
}

export default function PlayerModal({
  title,
  subtitle,
  poster,
  streams,
  activeStreamIndex = 0,
  onClose,
  onSwitchStream,
}: PlayerModalProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const hideControlsTimer = useRef<NodeJS.Timeout | null>(null);

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

  // Quality settings
  const [qualities, setQualities] = useState<{ id: number; height: number; name: string }[]>([]);
  const [currentQuality, setCurrentQuality] = useState(-1); // -1 = Auto
  const [showQualityMenu, setShowQualityMenu] = useState(false);
  const [showSpeedMenu, setShowSpeedMenu] = useState(false);
  const [showHelpModal, setShowHelpModal] = useState(false);

  const [showControls, setShowControls] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isBuffering, setIsBuffering] = useState(true);

  const currentStream = streams[currentIndex] || streams[0];

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
        backBufferLength: 60,
        maxBufferSize: 30 * 1024 * 1024,
      });

      hlsRef.current = hls;
      hls.loadSource(streamUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, (_, data) => {
        setIsBuffering(false);
        setIsLive(hls.levels[0]?.details?.live ?? false);

        const parsedQualities = data.levels.map((lvl, index) => ({
          id: index,
          height: lvl.height,
          name: lvl.height ? `${lvl.height}p` : `جودة ${index + 1}`,
        }));
        setQualities(parsedQualities);

        video.play().catch(() => setIsPlaying(false));
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
              setErrorMsg("تعذر تحميل البث من هذا السيرفر. يرجى تجربة سيرفر آخر.");
              break;
          }
        }
      });
    } else if (video.canPlayType("application/vnd.apple.mpegurl") || streamUrl.includes(".mp4")) {
      video.src = streamUrl;
      video.addEventListener("loadedmetadata", () => {
        setIsBuffering(false);
        video.play().catch(() => setIsPlaying(false));
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
      }
    }, 4000);
  };

  useEffect(() => {
    resetHideTimer();
    return () => {
      if (hideControlsTimer.current) clearTimeout(hideControlsTimer.current);
    };
  }, [isPlaying]);

  // Video Event Handlers
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
    setDuration(video.duration || 0);

    if (video.buffered.length > 0) {
      setBuffered(video.buffered.end(video.buffered.length - 1));
    }
  };

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const video = videoRef.current;
    if (!video) return;
    const target = parseFloat(e.target.value);
    video.currentTime = target;
    setCurrentTime(target);
  };

  const jumpSeconds = (seconds: number) => {
    const video = videoRef.current;
    if (!video) return;
    video.currentTime = Math.max(0, Math.min(duration, video.currentTime + seconds));
  };

  const syncToLive = () => {
    if (hlsRef.current && isLive) {
      const video = videoRef.current;
      if (video) video.currentTime = video.duration - 0.5;
    }
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
      } else if (e.key === "?") {
        setShowHelpModal((prev) => !prev);
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

  return (
    <div className="fixed inset-0 z-50 bg-black/98 flex items-center justify-center backdrop-blur-2xl animate-fadeIn">
      <div
        ref={containerRef}
        onMouseMove={resetHideTimer}
        onClick={resetHideTimer}
        className="relative w-full h-full max-w-[95vw] max-h-[94vh] bg-black rounded-3xl overflow-hidden shadow-2xl flex flex-col justify-center select-none border border-mashad-border/60"
      >
        {/* Main Video Element */}
        <video
          ref={videoRef}
          poster={poster}
          playsInline
          onPlay={() => setIsPlaying(true)}
          onPause={() => setIsPlaying(false)}
          onWaiting={() => setIsBuffering(true)}
          onPlaying={() => setIsBuffering(false)}
          onTimeUpdate={handleTimeUpdate}
          onClick={handlePlayPause}
          className="w-full h-full object-contain cursor-pointer"
        />

        {/* Mashad TV Dynamic Watermark (Masks Yacine & external stream logos) */}
        {isLive && (
          <div className="absolute bottom-16 left-6 z-20 pointer-events-none select-none flex items-center gap-2.5 px-3 py-1.5 rounded-xl bg-[#0A0E17]/95 border border-mashad-gold/80 shadow-2xl backdrop-blur-md">
            <div className="w-8 h-8 rounded-lg overflow-hidden border border-mashad-gold/60 p-0.5 bg-black flex-shrink-0">
              <img
                src="/logo.jpg"
                alt="مشهد TV"
                className="w-full h-full object-cover rounded"
                onError={(e) => {
                  (e.target as HTMLImageElement).src = "/logo.png";
                }}
              />
            </div>
            <div className="flex flex-col text-right">
              <span className="text-xs font-black text-mashad-gold tracking-wide">مَـشْـهَـدْ TV</span>
              <span className="text-[9px] font-semibold text-gray-200">بث مباشر رسمي</span>
            </div>
          </div>
        )}

        {/* Buffering Indicator */}
        {isBuffering && !errorMsg && (
          <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none gap-4">
            <div className="relative">
              <div className="w-20 h-20 rounded-3xl overflow-hidden border-2 border-mashad-gold shadow-gold p-1 bg-black">
                <img
                  src="/logo.jpg"
                  alt="مَشْهَد TV"
                  className="w-full h-full object-cover rounded-2xl"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = "/logo.png";
                  }}
                />
              </div>
              <div className="absolute -inset-2.5 border-4 border-t-mashad-gold border-r-transparent border-b-transparent border-l-transparent rounded-full animate-spin shadow-gold" />
            </div>
            <span className="text-xs font-black text-mashad-gold tracking-widest uppercase">
              جاري تشغيل البث المباشر...
            </span>
          </div>
        )}

        {/* Fatal Error Screen */}
        {errorMsg && (
          <div className="absolute inset-0 bg-black/90 flex flex-col items-center justify-center p-6 text-center z-30">
            <div className="w-16 h-16 rounded-full bg-red-500/20 text-red-500 border border-red-500/40 flex items-center justify-center mb-4 text-2xl font-bold shadow-live">
              !
            </div>
            <h3 className="text-xl font-black text-white mb-2">{errorMsg}</h3>
            <p className="text-xs text-gray-400 mb-6">يمكنك اختيار سيرفر بديل من السيرفرات المتوفرة أدناه</p>

            {streams.length > 1 && (
              <div className="flex gap-2.5 flex-wrap justify-center max-w-lg">
                {streams.map((s, idx) => (
                  <button
                    key={idx}
                    onClick={() => {
                      setCurrentIndex(idx);
                      onSwitchStream?.(idx);
                    }}
                    className={`px-5 py-2.5 rounded-xl text-xs font-black transition-all ${
                      idx === currentIndex
                        ? "bg-mashad-gold text-black shadow-gold scale-105"
                        : "bg-mashad-card text-gray-200 hover:bg-mashad-elevated border border-mashad-border"
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
          className={`absolute top-0 inset-x-0 p-6 bg-gradient-to-b from-black/95 via-black/50 to-transparent flex items-center justify-between z-20 transition-opacity duration-300 ${
            showControls ? "opacity-100" : "opacity-0 pointer-events-none"
          }`}
        >
          <div className="flex items-center gap-4">
            <button
              onClick={onClose}
              className="w-11 h-11 rounded-2xl bg-black/60 hover:bg-white/20 flex items-center justify-center text-white transition border border-white/10 backdrop-blur-md"
              title="إغلاق المشغل (Esc)"
            >
              <X size={20} />
            </button>
            <div>
              <div className="flex items-center gap-2.5">
                <h2 className="text-base sm:text-lg font-black text-white line-clamp-1">{title}</h2>
                {isLive && (
                  <button
                    onClick={syncToLive}
                    className="flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-red-600/30 border border-red-500 text-red-400 text-xs font-black hover:bg-red-600 hover:text-white transition"
                    title="الرجوع للبث الحي"
                  >
                    <span className="w-2 h-2 rounded-full bg-red-500 live-badge-glow" />
                    مباشر الآن
                  </button>
                )}
              </div>
              {subtitle && <p className="text-xs text-mashad-gold font-bold mt-0.5">{subtitle}</p>}
            </div>
          </div>

          {/* Streaming Server Switcher Chips */}
          {streams.length > 1 && (
            <div className="flex items-center gap-2 overflow-x-auto max-w-[45%] no-scrollbar">
              {streams.map((s, idx) => (
                <button
                  key={idx}
                  onClick={() => {
                    setCurrentIndex(idx);
                    onSwitchStream?.(idx);
                  }}
                  className={`px-3.5 py-1.5 rounded-xl text-xs font-black whitespace-nowrap transition-all ${
                    idx === currentIndex
                      ? "bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black shadow-gold scale-105"
                      : "bg-black/60 text-gray-300 hover:bg-black/90 border border-white/10 backdrop-blur-md"
                  }`}
                >
                  {s.name}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* ── Bottom HUD Controls Bar ───────────────────────────────── */}
        <div
          className={`absolute bottom-0 inset-x-0 p-6 bg-gradient-to-t from-black/98 via-black/60 to-transparent z-20 transition-opacity duration-300 flex flex-col gap-3.5 ${
            showControls ? "opacity-100" : "opacity-0 pointer-events-none"
          }`}
        >
          {/* Timeline Scrubber (for movies & series) */}
          {!isLive && duration > 0 && (
            <div className="relative w-full flex items-center group cursor-pointer py-1.5">
              {/* Buffer Bar */}
              <div
                className="absolute h-1.5 bg-white/20 rounded-full"
                style={{ width: `${(buffered / duration) * 100}%` }}
              />
              {/* Progress Bar */}
              <div
                className="absolute h-1.5 bg-gradient-to-r from-mashad-goldDark to-mashad-gold rounded-full shadow-gold"
                style={{ width: `${(currentTime / duration) * 100}%` }}
              />
              <input
                type="range"
                min={0}
                max={duration || 100}
                step={0.1}
                value={currentTime}
                onChange={handleSeek}
                className="w-full h-1.5 opacity-0 cursor-pointer z-10"
              />
            </div>
          )}

          {/* Controls Buttons Row */}
          <div className="flex items-center justify-between">
            {/* Left Controls */}
            <div className="flex items-center gap-3 sm:gap-4">
              <button
                onClick={handlePlayPause}
                className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-mashad-gold to-mashad-goldLight text-black flex items-center justify-center hover:scale-105 active:scale-95 transition-all shadow-gold"
                title={isPlaying ? "إيقاف مؤقت (Space)" : "تشغيل (Space)"}
              >
                {isPlaying ? <Pause size={22} /> : <Play size={22} className="ml-0.5" />}
              </button>

              {/* Seek 10s Backward/Forward */}
              {!isLive && (
                <>
                  <button
                    onClick={() => jumpSeconds(-10)}
                    className="w-9 h-9 rounded-xl bg-black/40 hover:bg-white/10 text-gray-300 hover:text-white flex items-center justify-center transition border border-white/5"
                    title="تأخير 10 ثوانٍ (Arrow Left)"
                  >
                    <RotateCcw size={16} />
                  </button>
                  <button
                    onClick={() => jumpSeconds(10)}
                    className="w-9 h-9 rounded-xl bg-black/40 hover:bg-white/10 text-gray-300 hover:text-white flex items-center justify-center transition border border-white/5"
                    title="تقديم 10 ثوانٍ (Arrow Right)"
                  >
                    <RotateCw size={16} />
                  </button>
                </>
              )}

              {/* Volume Slider */}
              <div className="flex items-center gap-2 group">
                <button
                  onClick={toggleMute}
                  className="text-gray-300 hover:text-white transition p-1"
                  title="كتم الصوت (M)"
                >
                  {isMuted || volume === 0 ? <VolumeX size={20} /> : <Volume2 size={20} />}
                </button>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={isMuted ? 0 : volume}
                  onChange={handleVolumeChange}
                  className="w-16 sm:w-24 h-1.5 accent-mashad-gold bg-white/20 rounded-lg cursor-pointer"
                />
              </div>

              {/* Timestamp */}
              {!isLive && duration > 0 && (
                <span className="text-xs font-mono font-bold text-gray-300 tracking-wider">
                  {formatTime(currentTime)} / {formatTime(duration)}
                </span>
              )}
            </div>

            {/* Right Controls */}
            <div className="flex items-center gap-2.5 relative">
              {/* Playback Speed Menu */}
              {!isLive && (
                <div className="relative">
                  <button
                    onClick={() => {
                      setShowSpeedMenu(!showSpeedMenu);
                      setShowQualityMenu(false);
                    }}
                    className="flex items-center gap-1 px-3 py-2 rounded-xl bg-black/50 hover:bg-white/10 text-xs font-black text-gray-200 border border-white/10 transition"
                  >
                    <Gauge size={14} />
                    <span>{playbackRate}x</span>
                  </button>

                  {showSpeedMenu && (
                    <div className="absolute bottom-full right-0 mb-2 py-1.5 w-28 bg-mashad-card border border-mashad-border rounded-2xl shadow-2xl z-30 overflow-hidden backdrop-blur-xl">
                      {[0.5, 0.75, 1, 1.25, 1.5, 2].map((rate) => (
                        <button
                          key={rate}
                          onClick={() => changeSpeed(rate)}
                          className={`w-full px-3 py-1.5 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                            playbackRate === rate ? "text-mashad-gold" : "text-gray-300"
                          }`}
                        >
                          <span>{rate}x</span>
                          {playbackRate === rate && <span>✓</span>}
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
                    }}
                    className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-black/50 hover:bg-white/10 text-xs font-black text-gray-200 border border-white/10 transition"
                  >
                    <Settings size={15} />
                    <span>
                      {currentQuality === -1
                        ? "تلقائي"
                        : qualities[currentQuality]?.name || "تلقائي"}
                    </span>
                  </button>

                  {showQualityMenu && (
                    <div className="absolute bottom-full right-0 mb-2 py-1.5 w-36 bg-mashad-card border border-mashad-border rounded-2xl shadow-2xl z-30 overflow-hidden backdrop-blur-xl">
                      <button
                        onClick={() => setQuality(-1)}
                        className={`w-full px-3.5 py-2 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                          currentQuality === -1 ? "text-mashad-gold" : "text-gray-300"
                        }`}
                      >
                        <span>تلقائي (Auto)</span>
                        {currentQuality === -1 && <span>✓</span>}
                      </button>
                      {qualities.map((q) => (
                        <button
                          key={q.id}
                          onClick={() => setQuality(q.id)}
                          className={`w-full px-3.5 py-2 text-xs text-right font-bold hover:bg-white/5 transition flex items-center justify-between ${
                            currentQuality === q.id ? "text-mashad-gold" : "text-gray-300"
                          }`}
                        >
                          <span>{q.name}</span>
                          {currentQuality === q.id && <span>✓</span>}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* PiP Button */}
              <button
                onClick={togglePiP}
                className="w-10 h-10 rounded-xl bg-black/50 hover:bg-white/10 text-gray-300 hover:text-white flex items-center justify-center transition border border-white/10"
                title="وضع صورة داخل صورة (PiP)"
              >
                <PictureInPicture size={18} />
              </button>

              {/* Fullscreen Button */}
              <button
                onClick={toggleFullscreen}
                className="w-10 h-10 rounded-xl bg-black/50 hover:bg-white/10 text-gray-300 hover:text-white flex items-center justify-center transition border border-white/10"
                title="شاشة كاملة (F)"
              >
                {isFullscreen ? <Minimize2 size={19} /> : <Maximize2 size={19} />}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
