"use client";

import React, { useEffect, useMemo, useState } from "react";
import {
  Clapperboard,
  Film,
  Flame,
  LoaderCircle,
  Play,
  RotateCcw,
  Search,
  Sparkles,
  TrendingUp,
  Tv,
  Youtube,
} from "lucide-react";

export interface MovieRecapItem {
  id: string;
  title: string;
  channelName: string;
  channelId: string;
  publishedAt: string;
  thumbnailUrl: string;
  videoUrl: string;
}

export interface RecapChannel {
  name: string;
  channelId: string;
  emoji: string;
}

interface RecapsViewProps {
  onPlayStream: (streamData: {
    title: string;
    subtitle?: string;
    poster?: string;
    streams: { name: string; url: string }[];
  }) => void;
}

function formatRelativeTime(dateString: string): string {
  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMinutes < 60) {
      return `منذ ${Math.max(1, diffMinutes)} دقيقة`;
    }
    if (diffHours < 24) {
      return `منذ ${diffHours} ${diffHours === 1 ? "ساعة" : diffHours === 2 ? "ساعتين" : "ساعات"}`;
    }
    if (diffDays < 30) {
      return `منذ ${diffDays} ${diffDays === 1 ? "يوم" : diffDays === 2 ? "يومين" : "أيام"}`;
    }
    const diffMonths = Math.floor(diffDays / 30);
    return `منذ ${diffMonths} شهر`;
  } catch {
    return "";
  }
}

export default function RecapsView({ onPlayStream }: RecapsViewProps) {
  const [items, setItems] = useState<MovieRecapItem[]>([]);
  const [channels, setChannels] = useState<RecapChannel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedChannelId, setSelectedChannelId] = useState<string>("");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [featuredIndex, setFeaturedIndex] = useState(0);

  const fetchRecaps = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/cinema/recaps?limit=150");
      const json = await res.json();
      if (json.success && Array.isArray(json.data)) {
        setItems(json.data);
        if (json.channels && Array.isArray(json.channels)) {
          setChannels(json.channels);
        }
      } else {
        setError(json.error || "تعذر جلب الملخصات");
      }
    } catch {
      setError("حدث خطأ في الاتصال بالشبكة");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecaps();
  }, []);

  const filteredItems = useMemo(() => {
    let result = items;
    if (selectedChannelId) {
      result = result.filter((item) => item.channelId === selectedChannelId);
    }
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      result = result.filter(
        (item) =>
          item.title.toLowerCase().includes(q) ||
          item.channelName.toLowerCase().includes(q)
      );
    }
    return result;
  }, [items, selectedChannelId, searchQuery]);

  const featuredItems = useMemo(() => {
    return items.slice(0, 5);
  }, [items]);

  // Rotate featured banner every 6 seconds
  useEffect(() => {
    if (featuredItems.length === 0) return;
    const interval = setInterval(() => {
      setFeaturedIndex((prev) => (prev + 1) % featuredItems.length);
    }, 6000);
    return () => clearInterval(interval);
  }, [featuredItems.length]);

  const handlePlayRecap = (item: MovieRecapItem) => {
    onPlayStream({
      title: item.title,
      subtitle: `ملخص سينمائي • ${item.channelName}`,
      poster: `https://i.ytimg.com/vi/${item.id}/maxresdefault.jpg`,
      streams: [
        {
          name: `${item.channelName} (HD نقي)`,
          url: `https://www.youtube.com/watch?v=${item.id}`,
        },
      ],
    });
  };

  const activeFeatured = featuredItems[featuredIndex];

  return (
    <div className="space-y-8 animate-fadeIn pb-12">
      {/* Header Title & Subtitle */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-black tracking-wide mb-2">
            <Sparkles size={14} className="animate-pulse" />
            <span>25 قناة ملخصات أفلام حصرية</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-black text-white flex items-center gap-3">
            <span>ملخصات الأفلام والسينما</span>
            <span className="text-sm font-bold px-3 py-1 rounded-xl bg-gradient-to-r from-red-600 to-rose-600 text-white shadow-lg shadow-red-600/30">
              مشغل سينمائي نقي
            </span>
          </h1>
          <p className="mt-2 text-sm text-slate-400 max-w-2xl leading-relaxed">
            استمتع بأحدث وأقوى ملخصات الأفلام العربية والعالمية بدون إعلانات وبمشغل زقزوقة السينمائي فائق التوفير والسرعة.
          </p>
        </div>

        {/* Search Input for Recaps */}
        <div className="w-full md:w-80">
          <div className="relative">
            <Search
              size={18}
              className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-amber-400/70"
            />
            <input
              type="search"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="ابحث في الملخصات..."
              className="h-11 w-full rounded-2xl border border-amber-500/25 bg-[#12131D] pr-11 pl-4 text-sm font-semibold text-white outline-none transition placeholder:text-slate-500 focus:border-amber-400 focus:bg-[#161726] focus:ring-2 focus:ring-amber-500/20"
            />
          </div>
        </div>
      </div>

      {/* Featured Banner Hero */}
      {activeFeatured && !searchQuery && (
        <div className="relative overflow-hidden rounded-3xl border border-amber-500/30 bg-gradient-to-br from-[#161726] to-[#0A0B10] shadow-[0_15px_45px_rgba(0,0,0,0.8)]">
          <div className="relative aspect-[21/9] sm:aspect-[2.6/1] w-full overflow-hidden">
            <img
              src={`https://i.ytimg.com/vi/${activeFeatured.id}/maxresdefault.jpg`}
              alt={activeFeatured.title}
              className="h-full w-full object-cover transition duration-700 ease-out hover:scale-105"
              onError={(e) => {
                (e.target as HTMLImageElement).src = `https://i.ytimg.com/vi/${activeFeatured.id}/hqdefault.jpg`;
              }}
            />
            <div className="absolute inset-0 bg-gradient-to-t from-[#090A0E] via-[#090A0E]/60 to-transparent" />
            <div className="absolute inset-0 bg-gradient-to-r from-[#090A0E]/90 via-[#090A0E]/40 to-transparent" />

            {/* Content overlay */}
            <div className="absolute inset-0 flex flex-col justify-end p-6 sm:p-10">
              <div className="flex items-center gap-2 mb-3">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-400 px-3 py-1 text-xs font-black text-slate-950 shadow-md">
                  <Flame size={14} className="fill-slate-950" /> مميز اليوم
                </span>
                <span className="rounded-full bg-black/60 border border-white/10 px-3 py-1 text-xs font-bold text-slate-300 backdrop-blur-md">
                  {activeFeatured.channelName}
                </span>
                {activeFeatured.publishedAt && (
                  <span className="text-xs text-slate-400">
                    {formatRelativeTime(activeFeatured.publishedAt)}
                  </span>
                )}
              </div>

              <h2 className="text-xl sm:text-3xl lg:text-4xl font-black text-white max-w-3xl line-clamp-2 leading-tight">
                {activeFeatured.title}
              </h2>

              <div className="mt-5 flex items-center gap-4">
                <button
                  type="button"
                  onClick={() => handlePlayRecap(activeFeatured)}
                  className="inline-flex items-center gap-2.5 rounded-2xl bg-gradient-to-r from-amber-400 via-amber-500 to-amber-600 px-7 py-3 text-sm font-black text-slate-950 shadow-xl shadow-amber-500/30 transition duration-200 hover:scale-105"
                >
                  <Play size={18} className="fill-slate-950" />
                  <span>مشاهدة الملخص الآن</span>
                </button>
              </div>
            </div>

            {/* Slider Dots */}
            <div className="absolute bottom-4 left-6 hidden sm:flex items-center gap-1.5">
              {featuredItems.map((_, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={() => setFeaturedIndex(i)}
                  aria-label={`الشريحة ${i + 1}`}
                  className={`h-2 rounded-full transition-all duration-300 ${
                    i === featuredIndex
                      ? "w-7 bg-amber-400 shadow-[0_0_8px_rgba(212,175,55,0.8)]"
                      : "w-2 bg-white/20 hover:bg-white/40"
                  }`}
                />
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Channel Filters Scrollable Pills */}
      <div className="relative">
        <div className="flex items-center gap-2.5 overflow-x-auto pb-2 scrollbar-none">
          <button
            type="button"
            onClick={() => setSelectedChannelId("")}
            className={`shrink-0 inline-flex items-center gap-2 rounded-2xl px-5 py-2.5 text-xs font-black transition duration-200 ${
              selectedChannelId === ""
                ? "bg-gradient-to-r from-amber-400 to-amber-500 text-slate-950 shadow-lg shadow-amber-500/25 scale-105"
                : "border border-amber-500/20 bg-[#12131D] text-slate-300 hover:bg-[#1A1C2C] hover:text-white"
            }`}
          >
            <Sparkles size={14} />
            <span>كل القنوات ({items.length})</span>
          </button>

          {channels.map((ch) => {
            const isSelected = selectedChannelId === ch.channelId;
            return (
              <button
                key={ch.channelId}
                type="button"
                onClick={() => setSelectedChannelId(ch.channelId)}
                className={`shrink-0 inline-flex items-center gap-2 rounded-2xl px-4 py-2.5 text-xs font-bold transition duration-200 ${
                  isSelected
                    ? "bg-gradient-to-r from-amber-400 to-amber-500 text-slate-950 shadow-lg shadow-amber-500/25 scale-105 font-black"
                    : "border border-white/5 bg-[#12131D] text-slate-300 hover:border-amber-500/30 hover:bg-[#1A1C2C] hover:text-white"
                }`}
              >
                <span>{ch.emoji}</span>
                <span>{ch.name}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Loading State */}
      {loading && items.length === 0 && (
        <div className="py-24 flex flex-col items-center justify-center gap-4 text-center">
          <LoaderCircle size={42} className="animate-spin text-amber-400" />
          <p className="text-base font-extrabold text-white">جاري جلب أحدث ملخصات الأفلام...</p>
          <p className="text-xs text-slate-400">نربطك بـ 25 قناة سينمائية متخصصة</p>
        </div>
      )}

      {/* Error State */}
      {error && items.length === 0 && (
        <div className="rounded-3xl border border-rose-500/30 bg-rose-500/10 p-8 text-center space-y-3">
          <p className="text-base font-black text-rose-300">{error}</p>
          <button
            type="button"
            onClick={fetchRecaps}
            className="inline-flex items-center gap-2 rounded-xl bg-rose-500 px-5 py-2 text-xs font-black text-white hover:bg-rose-600 transition"
          >
            <RotateCcw size={14} /> إعادة المحاولة
          </button>
        </div>
      )}

      {/* Recaps Grid Cards */}
      {!loading && filteredItems.length === 0 && (
        <div className="rounded-3xl border border-dashed border-white/10 bg-[#12131D] py-20 text-center space-y-3">
          <Clapperboard size={48} className="mx-auto text-amber-400/40" />
          <h3 className="text-lg font-black text-white">لم يتم العثور على ملخصات</h3>
          <p className="text-xs text-slate-400">جرب البحث بكلمة مختلفة أو اختر قناة أخرى</p>
        </div>
      )}

      {filteredItems.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {filteredItems.map((item) => (
            <article
              key={item.id}
              onClick={() => handlePlayRecap(item)}
              className="group cursor-pointer flex flex-col overflow-hidden rounded-2xl border border-white/5 bg-[#12131D] shadow-[0_8px_25px_rgba(0,0,0,0.6)] transition-all duration-300 hover:-translate-y-1.5 hover:border-amber-400/60 hover:shadow-[0_12px_32px_rgba(212,175,55,0.2)]"
            >
              {/* Thumbnail 16:9 */}
              <div className="relative aspect-video w-full overflow-hidden bg-black">
                <img
                  src={`https://i.ytimg.com/vi/${item.id}/hqdefault.jpg`}
                  alt={item.title}
                  loading="lazy"
                  className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = item.thumbnailUrl;
                  }}
                />
                <div className="absolute inset-0 bg-gradient-to-t from-[#090A0E] via-transparent to-transparent opacity-80" />

                {/* Duration/Badge */}
                <div className="absolute bottom-2.5 right-2.5 flex items-center gap-1 rounded-md bg-black/80 px-2 py-0.5 text-[10px] font-extrabold text-amber-300 backdrop-blur-md border border-amber-500/20">
                  <span>HD 1080p</span>
                </div>

                {/* Relative time */}
                {item.publishedAt && (
                  <div className="absolute top-2.5 right-2.5 rounded-md bg-black/80 px-2 py-0.5 text-[10px] font-semibold text-slate-300 backdrop-blur-md">
                    {formatRelativeTime(item.publishedAt)}
                  </div>
                )}

                {/* Floating Play Icon */}
                <div className="absolute inset-0 grid place-items-center opacity-0 group-hover:opacity-100 transition duration-300 bg-black/40 backdrop-blur-xs">
                  <div className="grid h-12 w-12 place-items-center rounded-2xl bg-gradient-to-tr from-amber-400 to-amber-600 text-slate-950 shadow-2xl transition duration-300 group-hover:scale-110">
                    <Play size={20} className="fill-slate-950 translate-x-[-1px]" />
                  </div>
                </div>
              </div>

              {/* Card Meta Content */}
              <div className="flex flex-1 flex-col justify-between p-4 gap-3">
                <div className="space-y-1.5">
                  <div className="flex items-center gap-1.5 text-[11px] font-extrabold text-amber-400">
                    <Clapperboard size={13} />
                    <span className="truncate">{item.channelName}</span>
                  </div>
                  <h3
                    className="text-sm font-bold text-white line-clamp-2 leading-snug group-hover:text-amber-300 transition"
                    title={item.title}
                  >
                    {item.title}
                  </h3>
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-white/5">
                  <span className="text-[11px] font-semibold text-slate-400 flex items-center gap-1">
                    <Sparkles size={12} className="text-amber-400" /> ملخص سينمائي
                  </span>

                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handlePlayRecap(item);
                    }}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-amber-400/10 hover:bg-amber-400 text-amber-300 hover:text-slate-950 border border-amber-400/30 px-3 py-1.5 text-xs font-black transition duration-200"
                  >
                    <Play size={12} className="fill-current" />
                    <span>تشغيل</span>
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
