"use client";

import React, { useState, useEffect } from "react";
import {
  Play,
  Star,
  Film,
  Tv,
  Loader2,
  Sparkles,
  Heart,
  ChevronLeft,
  ChevronRight,
  Flame,
} from "lucide-react";
import { ArabseedItem } from "@/lib/arabseed";

interface CinemaViewProps {
  activeTab?: "home" | "movies" | "series" | "anime";
  selectedGenre?: string;
  searchQuery: string;
  onSelectItem: (item: ArabseedItem) => void;
  onPlayStream: (streamData: {
    title: string;
    subtitle?: string;
    poster?: string;
    streams: { name: string; url: string }[];
  }) => void;
  favorites: ArabseedItem[];
  onToggleFavorite: (item: ArabseedItem) => void;
}

const GENRE_FILTERS = [
  { id: "all", label: "الكل", icon: Sparkles },
  { id: "foreign_movies", label: "أفلام أجنبية", icon: Film },
  { id: "arabic_movies", label: "أفلام عربية", icon: Film },
  { id: "turkish_movies", label: "أفلام تركية", icon: Film },
  { id: "foreign_series", label: "مسلسلات أجنبية", icon: Tv },
  { id: "arabic_series", label: "مسلسلات عربية", icon: Tv },
  { id: "turkish_series", label: "مسلسلات تركية", icon: Tv },
  { id: "ramadan", label: "مسلسلات رمضان 2026", icon: Sparkles },
  { id: "anime", label: "أنمي وكرتون", icon: Sparkles },
  { id: "action", label: "أكشن وإثارة", icon: Flame },
  { id: "horror", label: "رعب وغموض", icon: Film },
  { id: "wrestling", label: "عروض مصارعة", icon: Flame },
];

const FILTER_TITLES: Record<string, string> = {
  all: "أحدث الإضافات الحصرية",
  movies: "أحدث الأفلام الحصرية",
  series: "أحدث المسلسلات الحصرية",
  foreign_movies: "أحدث الأفلام الأجنبية الحصرية",
  arabic_movies: "أحدث الأفلام العربية الجديدة",
  turkish_movies: "أحدث الأفلام التركية المترجمة",
  foreign_series: "أحدث المسلسلات الأجنبية كاملة",
  arabic_series: "أحدث المسلسلات العربية والمصرية",
  turkish_series: "أحدث المسلسلات التركية والمدبلجة",
  ramadan: "مسلسلات رمضان 2026 الحصرية",
  anime: "أحدث عروض الأنمي والكرتون",
  action: "أقوى أفلام ومسلسلات الأكشن والإثارة",
  horror: "أفلام ومسلسلات الرعب والغموض",
  wrestling: "أحدث عروض المصارعة الحرة WWE",
};

export default function CinemaView({
  activeTab = "home",
  selectedGenre,
  searchQuery,
  onSelectItem,
  onPlayStream,
  favorites,
  onToggleFavorite,
}: CinemaViewProps) {
  const [activeFilter, setActiveFilter] = useState("all");
  const [items, setItems] = useState<ArabseedItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [resolvingId, setResolvingId] = useState<string | null>(null);

  // Sync internal filter with activeTab prop
  useEffect(() => {
    if (activeTab === "movies") setActiveFilter("foreign_movies");
    else if (activeTab === "series") setActiveFilter("foreign_series");
    else if (activeTab === "anime") setActiveFilter("anime");
    else if (selectedGenre) setActiveFilter(selectedGenre);
    else setActiveFilter("all");
    setPage(1);
  }, [activeTab, selectedGenre]);

  // Fetch items based on search or category
  useEffect(() => {
    let isCancelled = false;

    async function fetchData() {
      setLoading(true);
      try {
        if (searchQuery.trim()) {
          const res = await fetch(`/api/cinema/search?q=${encodeURIComponent(searchQuery.trim())}`);
          const data = await res.json();
          if (!isCancelled && data.success) {
            setItems(data.data || []);
            setHasMore(false);
          }
        } else {
          let endpoint = `/api/cinema/movies?page=${page}`;
          if (activeFilter === "all") {
            endpoint = `/api/cinema/movies?page=${page}`;
          } else {
            endpoint = `/api/cinema/category?slug=${activeFilter}&page=${page}`;
          }

          const res = await fetch(endpoint);
          const data = await res.json();
          if (!isCancelled && data.success) {
            setItems(data.data || []);
            setHasMore((data.data || []).length >= 8);
          }
        }
      } catch (err) {
        console.error("Failed to load cinema items:", err);
      } finally {
        if (!isCancelled) setLoading(false);
      }
    }

    fetchData();

    return () => {
      isCancelled = true;
    };
  }, [searchQuery, activeFilter, page]);

  // Quick Play handler from card
  const handleQuickPlay = async (e: React.MouseEvent, item: ArabseedItem) => {
    e.stopPropagation();
    setResolvingId(item.id);

    try {
      const detRes = await fetch(`/api/cinema/detail?url=${encodeURIComponent(item.pageUrl)}`);
      const detData = await detRes.json();

      if (detData.success && detData.data) {
        const detail = detData.data;
        if (detail.servers && detail.servers.length > 0) {
          const srv = detail.servers[0];
          const resRes = await fetch(`/api/cinema/resolve?serverUrl=${encodeURIComponent(srv.embedUrl)}`);
          const resData = await resRes.json();
          const playUrl = resData.playUrl || resData.data?.playUrl;

          if (resData.success && playUrl) {
            onPlayStream({
              title: item.title,
              subtitle: srv.name,
              poster: item.poster,
              streams: [{ name: srv.name, url: playUrl }],
            });
            return;
          }

          // Fallback to embed player directly!
          onPlayStream({
            title: item.title,
            subtitle: srv.name,
            poster: item.poster,
            streams: [{ name: srv.name, url: srv.embedUrl }],
          });
          return;
        }

        if (detail.proxiedStream) {
          onPlayStream({
            title: item.title,
            subtitle: "سيرفر مَشْهَد المباشر",
            poster: item.poster,
            streams: [{ name: "سيرفر مَشْهَد المباشر", url: detail.proxiedStream }],
          });
          return;
        }
      }

      onSelectItem(item);
    } catch {
      onSelectItem(item);
    } finally {
      setResolvingId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* ── Filter Bar (Mashhad Luxury Gold Pills) ───────────────────── */}
      <div className="flex items-center justify-between flex-wrap gap-4 bg-[#0D111A] border border-[#1D273B] p-3 rounded-2xl shadow-xl">
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar w-full sm:w-auto">
          {GENRE_FILTERS.map((f) => {
            const Icon = f.icon;
            const isActive = activeFilter === f.id;
            return (
              <button
                key={f.id}
                onClick={() => {
                  setActiveFilter(f.id);
                  setPage(1);
                }}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all duration-200 ${
                  isActive
                    ? "bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black shadow-gold font-black scale-[1.02]"
                    : "bg-[#121824] text-gray-300 hover:text-white hover:bg-[#182234]"
                }`}
              >
                <Icon size={14} className={isActive ? "text-black" : "text-[#E5B842]"} />
                <span>{f.label}</span>
              </button>
            );
          })}
        </div>

        <div className="flex items-center gap-2 text-xs font-bold text-gray-400">
          <span>الصفحة {page}</span>
          <span>•</span>
          <span className="text-[#E5B842] font-black">{items.length} عمل متاح</span>
        </div>
      </div>

      {/* ── Section Title ─────────────────────────────────────────── */}
      <div className="flex items-center justify-between border-b border-[#1D273B] pb-3">
        <div className="flex items-center gap-2.5">
          <div className="w-2.5 h-6 rounded-full bg-gradient-to-b from-[#E5B842] to-[#C69214] shadow-gold" />
          <h2 className="text-xl font-black text-white">
            {searchQuery.trim()
              ? `نتائج البحث عن "${searchQuery.trim()}"`
              : FILTER_TITLES[activeFilter] || "أحدث الأعمال الحصرية على مَشْهَد TV"}
          </h2>
        </div>

        {loading && (
          <div className="flex items-center gap-2 text-xs text-[#E5B842] font-bold">
            <Loader2 size={16} className="animate-spin text-[#E5B842]" />
            <span>جاري التحديث بأقصى سرعة...</span>
          </div>
        )}
      </div>

      {/* ── Loading Skeleton Grid ─────────────────────────────────── */}
      {loading ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {Array.from({ length: 15 }).map((_, i) => (
            <div
              key={i}
              className="bg-[#0D111A] border border-[#1D273B] rounded-2xl overflow-hidden aspect-[2/3.1] animate-pulse flex flex-col justify-end p-4 gap-2"
            >
              <div className="h-4 bg-[#1D273B] rounded w-3/4" />
              <div className="h-3 bg-[#1D273B] rounded w-1/2" />
            </div>
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="bg-[#0D111A] border border-[#1D273B] rounded-3xl p-12 text-center space-y-4 shadow-2xl">
          <Film size={48} className="mx-auto text-[#E5B842]" />
          <h3 className="text-lg font-black text-white">لا توجد أعمال في هذا التصنيف حالياً</h3>
          <p className="text-xs text-gray-400">اختر تصنيفاً آخر أو تصفح الصفحة الأولى من القائمة.</p>
          <button
            onClick={() => {
              setActiveFilter("all");
              setPage(1);
            }}
            className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black font-black text-xs hover:shadow-gold transition"
          >
            العودة لكافة الأعمال
          </button>
        </div>
      ) : (
        /* ── Mashhad TV Golden Movie/Series Grid ───────────────────── */
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 xl:grid-cols-5 gap-4 sm:gap-5">
          {items.map((item) => {
            const isFav = favorites.some((f) => f.id === item.id);
            const isResolving = resolvingId === item.id;

            return (
              <div
                key={item.id}
                onClick={() => onSelectItem(item)}
                onMouseEnter={() => {
                  fetch(`/api/cinema/detail?url=${encodeURIComponent(item.pageUrl)}`, { priority: "low" }).catch(() => {});
                }}
                className="group relative bg-[#0D111A] rounded-2xl overflow-hidden border border-[#1D273B] hover:border-[#E5B842]/80 cursor-pointer shadow-lg hover:shadow-[0_0_25px_rgba(229,184,66,0.25)] transition-all duration-300 transform hover:-translate-y-2 flex flex-col select-none aspect-[2/3.1]"
              >
                {/* Poster Image */}
                <div className="relative w-full h-full overflow-hidden bg-black/60">
                  <img
                    src={item.poster}
                    alt={item.title}
                    loading="lazy"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    onError={(e) => {
                      (e.target as HTMLImageElement).src = "/logo.jpg";
                    }}
                  />

                  {/* Gradient Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-t from-[#07090E] via-[#07090E]/30 to-transparent" />

                  {/* Top-Right: Quality Badge */}
                  <div className="absolute top-2.5 right-2.5 z-10">
                    <span className="px-2.5 py-0.5 rounded-lg bg-black/90 text-[#F5D061] text-[10px] font-black border border-[#E5B842]/50 shadow-md uppercase">
                      {item.quality || (item.isSeries ? "مسلسل" : "1080p FHD")}
                    </span>
                  </div>

                  {/* Top-Left: IMDB Rating Circle */}
                  <div className="absolute top-2.5 left-2.5 z-10 flex items-center gap-1 px-2 py-0.5 rounded-full bg-black/90 text-white text-[10px] font-black border border-[#E5B842]/60 shadow-md">
                    <Star size={10} className="fill-[#E5B842] text-[#E5B842]" />
                    <span>{item.rating || "7.2"}</span>
                  </div>

                  {/* Favorite Heart Button */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleFavorite(item);
                    }}
                    className={`absolute top-9 left-2.5 z-10 w-7 h-7 rounded-full flex items-center justify-center transition border shadow-md ${
                      isFav
                        ? "bg-[#EF4444] text-white border-red-500 scale-110"
                        : "bg-black/80 text-gray-300 hover:text-white border-white/20 hover:scale-110"
                    }`}
                    title={isFav ? "إزالة من المفضلة" : "إضافة للمفضلة"}
                  >
                    <Heart size={13} className={isFav ? "fill-white" : ""} />
                  </button>

                  {/* Bottom Info Container */}
                  <div className="absolute bottom-0 inset-x-0 p-3.5 flex flex-col gap-1.5 z-10">
                    {/* Genre / Category Badge */}
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <span className="px-2 py-0.5 rounded-md bg-[#E5B842]/20 text-[#F5D061] border border-[#E5B842]/30 text-[9px] font-black">
                        {item.category || (item.isSeries ? "مسلسل" : "فيلم")}
                      </span>
                      {item.year && (
                        <span className="px-1.5 py-0.5 rounded-md bg-black/70 text-gray-300 text-[9px] font-bold">
                          {item.year}
                        </span>
                      )}
                    </div>

                    {/* Title */}
                    <h3 className="text-xs sm:text-sm font-black text-white leading-tight line-clamp-2 group-hover:text-[#F5D061] transition-colors">
                      {item.title}
                    </h3>
                  </div>

                  {/* Hover Overlay with Large Gold Play Button */}
                  <div className="absolute inset-0 bg-black/70 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col items-center justify-center gap-3 p-4 z-20 backdrop-blur-[2px]">
                    <button
                      onClick={(e) => handleQuickPlay(e, item)}
                      disabled={isResolving}
                      className="w-14 h-14 rounded-full bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black flex items-center justify-center shadow-goldGlow transform hover:scale-110 transition-all border-2 border-white/60"
                      title="تشغيل فوري"
                    >
                      {isResolving ? (
                        <Loader2 size={24} className="animate-spin text-black" />
                      ) : (
                        <Play size={24} className="fill-black ml-0.5" />
                      )}
                    </button>

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectItem(item);
                      }}
                      className="px-4 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-bold transition border border-white/20 shadow-md backdrop-blur-md"
                    >
                      تفاصيل وسيرفرات العرض
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── Pagination Bar ─────────────────────────────────────────── */}
      {!loading && items.length > 0 && !searchQuery.trim() && (
        <div className="flex items-center justify-center gap-2 pt-6 pb-12">
          <button
            onClick={() => {
              if (page > 1) {
                setPage((p) => p - 1);
                window.scrollTo({ top: 0, behavior: "smooth" });
              }
            }}
            disabled={page === 1}
            className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#0D111A] border border-[#1D273B] text-xs font-bold text-gray-300 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            <ChevronRight size={16} />
            <span>السابق</span>
          </button>

          {[1, 2, 3, 4, 5].map((pNum) => (
            <button
              key={pNum}
              onClick={() => {
                setPage(pNum);
                window.scrollTo({ top: 0, behavior: "smooth" });
              }}
              className={`w-10 h-10 rounded-xl text-xs font-black transition ${
                page === pNum
                  ? "bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black shadow-gold scale-105"
                  : "bg-[#0D111A] border border-[#1D273B] text-gray-300 hover:text-white hover:bg-[#161E2E]"
              }`}
            >
              {pNum}
            </button>
          ))}

          <button
            onClick={() => {
              if (hasMore) {
                setPage((p) => p + 1);
                window.scrollTo({ top: 0, behavior: "smooth" });
              }
            }}
            disabled={!hasMore}
            className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#0D111A] border border-[#1D273B] text-xs font-bold text-gray-300 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            <span>التالي</span>
            <ChevronLeft size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
