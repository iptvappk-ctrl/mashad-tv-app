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
  Clapperboard,
  SlidersHorizontal,
  RefreshCw,
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
  { id: "movies", label: "أفلام", icon: Film },
  { id: "series", label: "مسلسلات", icon: Tv },
  { id: "action", label: "أكشن", icon: Flame },
  { id: "comedy", label: "كوميدي", icon: Sparkles },
  { id: "drama", label: "دراما", icon: Film },
  { id: "horror", label: "رعب", icon: Clapperboard },
  { id: "anime", label: "أنمي", icon: Sparkles },
];

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
    if (activeTab === "movies") setActiveFilter("movies");
    else if (activeTab === "series") setActiveFilter("series");
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
          // Determine endpoints based on activeFilter
          if (activeFilter === "movies") {
            const res = await fetch(`/api/cinema/movies?page=${page}`);
            const data = await res.json();
            if (!isCancelled && data.success) {
              setItems(data.data || []);
              setHasMore((data.data || []).length >= 10);
            }
          } else if (activeFilter === "series") {
            const res = await fetch(`/api/cinema/series?page=${page}`);
            const data = await res.json();
            if (!isCancelled && data.success) {
              setItems(data.data || []);
              setHasMore((data.data || []).length >= 10);
            }
          } else {
            // Home / All: combine movies & series
            const [movRes, serRes] = await Promise.all([
              fetch(`/api/cinema/movies?page=${page}`),
              fetch(`/api/cinema/series?page=${page}`),
            ]);
            const movData = await movRes.json();
            const serData = await serRes.json();

            if (!isCancelled) {
              const combined: ArabseedItem[] = [];
              const movList = movData.success ? movData.data : [];
              const serList = serData.success ? serData.data : [];
              const maxLen = Math.max(movList.length, serList.length);

              // Interleave movies & series for rich variety
              for (let i = 0; i < maxLen; i++) {
                if (movList[i]) combined.push(movList[i]);
                if (serList[i]) combined.push(serList[i]);
              }

              // Apply client-side genre filter if needed
              let filtered = combined;
              if (activeFilter === "action") {
                filtered = combined.filter((i) => (i.category || "").includes("أكشن") || (i.title || "").includes("أكشن"));
              } else if (activeFilter === "horror") {
                filtered = combined.filter((i) => (i.category || "").includes("رعب") || (i.title || "").includes("رعب"));
              } else if (activeFilter === "comedy") {
                filtered = combined.filter((i) => (i.category || "").includes("كوميد") || (i.title || "").includes("كوميد"));
              } else if (activeFilter === "drama") {
                filtered = combined.filter((i) => (i.category || "").includes("دراما") || (i.title || "").includes("دراما"));
              } else if (activeFilter === "anime") {
                filtered = combined.filter((i) => (i.category || "").includes("أنمي") || (i.category || "").includes("كرتون"));
              }

              setItems(filtered.length > 0 ? filtered : combined);
              setHasMore(movList.length >= 10 || serList.length >= 10);
            }
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

          if (resData.success && resData.playUrl) {
            onPlayStream({
              title: item.title,
              subtitle: srv.name,
              poster: item.poster,
              streams: [{ name: srv.name, url: resData.playUrl }],
            });
            return;
          }
        }

        if (detail.proxiedStream) {
          onPlayStream({
            title: item.title,
            subtitle: "سيرفر مباشر",
            poster: item.poster,
            streams: [{ name: "سيرفر مباشر", url: detail.proxiedStream }],
          });
          return;
        }
      }

      // If cannot direct play, open detail view
      onSelectItem(item);
    } catch {
      onSelectItem(item);
    } finally {
      setResolvingId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* ── Filter Bar (Wecima Style Pills) ────────────────────────── */}
      <div className="flex items-center justify-between flex-wrap gap-4 bg-[#121A28] border border-[#1E293B] p-3 rounded-2xl">
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
                    ? "bg-[#2563EB] text-white shadow-md font-black"
                    : "bg-[#182336] text-gray-300 hover:text-white hover:bg-[#202F47]"
                }`}
              >
                <Icon size={14} className={isActive ? "text-white" : "text-[#3B82F6]"} />
                <span>{f.label}</span>
              </button>
            );
          })}
        </div>

        <div className="flex items-center gap-2 text-xs font-bold text-gray-400">
          <span>الصفحة {page}</span>
          <span>•</span>
          <span className="text-[#3B82F6] font-black">{items.length} عمل متاح</span>
        </div>
      </div>

      {/* ── Section Title ─────────────────────────────────────────── */}
      <div className="flex items-center justify-between border-b border-[#1E293B] pb-3">
        <div className="flex items-center gap-2.5">
          <div className="w-2.5 h-6 rounded-full bg-[#2563EB]" />
          <h2 className="text-xl font-black text-white">
            {searchQuery.trim()
              ? `نتائج البحث عن "${searchQuery.trim()}"`
              : activeFilter === "movies"
              ? "أحدث الأفلام الحصرية"
              : activeFilter === "series"
              ? "أحدث المسلسلات الحصرية"
              : activeFilter === "anime"
              ? "أحدث عروض الأنمي والكرتون"
              : "أحدث الإضافات الحصرية على مَشْهَد TV"}
          </h2>
        </div>

        {loading && (
          <div className="flex items-center gap-2 text-xs text-[#3B82F6] font-bold">
            <Loader2 size={16} className="animate-spin" />
            <span>جاري التحديث...</span>
          </div>
        )}
      </div>

      {/* ── Loading Skeleton Grid ─────────────────────────────────── */}
      {loading ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {Array.from({ length: 15 }).map((_, i) => (
            <div
              key={i}
              className="bg-[#121A28] border border-[#1E293B] rounded-2xl overflow-hidden aspect-[2/3.1] animate-pulse flex flex-col justify-end p-4 gap-2"
            >
              <div className="h-4 bg-[#1E293B] rounded w-3/4" />
              <div className="h-3 bg-[#1E293B] rounded w-1/2" />
            </div>
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="bg-[#121A28] border border-[#1E293B] rounded-3xl p-12 text-center space-y-4">
          <Film size={48} className="mx-auto text-gray-500" />
          <h3 className="text-lg font-black text-white">لا توجد أعمال تطابق بحثك حالياً</h3>
          <p className="text-xs text-gray-400">جرب البحث بكلمات مختلفة أو اختر تصنيفاً آخر من القائمة الجانبية.</p>
          <button
            onClick={() => {
              setActiveFilter("all");
              setPage(1);
            }}
            className="px-6 py-2.5 rounded-xl bg-[#2563EB] text-white text-xs font-bold hover:bg-[#1D4ED8] transition"
          >
            العودة لكل الأعمال
          </button>
        </div>
      ) : (
        /* ── Wecima Movie/Series Grid ──────────────────────────────── */
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 xl:grid-cols-5 gap-4 sm:gap-5">
          {items.map((item) => {
            const isFav = favorites.some((f) => f.id === item.id);
            const isResolving = resolvingId === item.id;

            return (
              <div
                key={item.id}
                onClick={() => onSelectItem(item)}
                className="group relative bg-[#121A28] rounded-2xl overflow-hidden border border-[#1E293B] hover:border-[#2563EB]/80 cursor-pointer shadow-lg hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-1.5 flex flex-col select-none aspect-[2/3.1]"
              >
                {/* Poster Image */}
                <div className="relative w-full h-full overflow-hidden bg-black/40">
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
                  <div className="absolute inset-0 bg-gradient-to-t from-[#0B0F17] via-[#0B0F17]/30 to-transparent" />

                  {/* Top-Right: Quality Badge (Wecima Style) */}
                  <div className="absolute top-2.5 right-2.5 z-10">
                    <span className="px-2.5 py-0.5 rounded-lg bg-black/85 text-yellow-400 text-[10px] font-black border border-yellow-400/40 shadow-md uppercase">
                      {item.quality || "WEB-DL"}
                    </span>
                  </div>

                  {/* Top-Left: IMDB Rating Circle (Wecima Style) */}
                  <div className="absolute top-2.5 left-2.5 z-10 flex items-center gap-1 px-2 py-0.5 rounded-full bg-black/85 text-white text-[10px] font-black border border-yellow-500/50 shadow-md">
                    <Star size={10} className="fill-yellow-400 text-yellow-400" />
                    <span>{item.rating || "6.8"}</span>
                  </div>

                  {/* Favorite Heart Button (Top, below rating) */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleFavorite(item);
                    }}
                    className={`absolute top-9 left-2.5 z-10 w-7 h-7 rounded-full flex items-center justify-center transition border shadow-md ${
                      isFav
                        ? "bg-[#EF4444] text-white border-red-500"
                        : "bg-black/75 text-gray-300 hover:text-white border-white/20 hover:scale-110"
                    }`}
                    title={isFav ? "إزالة من المفضلة" : "إضافة للمفضلة"}
                  >
                    <Heart size={13} className={isFav ? "fill-white" : ""} />
                  </button>

                  {/* Bottom Info Container */}
                  <div className="absolute bottom-0 inset-x-0 p-3 flex flex-col gap-1.5 z-10">
                    {/* Genre / Category Badge */}
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <span className="px-2 py-0.5 rounded-md bg-[#2563EB]/80 text-white text-[9px] font-black">
                        {item.category || (item.isSeries ? "مسلسل" : "فيلم")}
                      </span>
                      {item.year && (
                        <span className="px-1.5 py-0.5 rounded-md bg-black/70 text-gray-300 text-[9px] font-bold">
                          {item.year}
                        </span>
                      )}
                    </div>

                    {/* Title */}
                    <h3 className="text-xs sm:text-sm font-black text-white leading-tight line-clamp-2 group-hover:text-[#3B82F6] transition-colors">
                      {item.title}
                    </h3>
                  </div>

                  {/* Hover Overlay with Large Play Button */}
                  <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col items-center justify-center gap-3 p-4 z-20">
                    <button
                      onClick={(e) => handleQuickPlay(e, item)}
                      disabled={isResolving}
                      className="w-14 h-14 rounded-full bg-[#EF4444] hover:bg-[#DC2626] text-white flex items-center justify-center shadow-2xl transform hover:scale-110 transition-all border-2 border-white/60"
                      title="تشغيل سريع"
                    >
                      {isResolving ? (
                        <Loader2 size={24} className="animate-spin text-white" />
                      ) : (
                        <Play size={24} className="fill-white ml-0.5" />
                      )}
                    </button>

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectItem(item);
                      }}
                      className="px-4 py-1.5 rounded-xl bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-xs font-bold transition shadow-md"
                    >
                      عرض التفاصيل والسيرفرات
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── Pagination Bar (Wecima Style) ──────────────────────────── */}
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
            className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#121A28] border border-[#1E293B] text-xs font-bold text-gray-300 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition"
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
                  ? "bg-[#2563EB] text-white shadow-md"
                  : "bg-[#121A28] border border-[#1E293B] text-gray-300 hover:text-white hover:bg-[#1C2638]"
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
            className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#121A28] border border-[#1E293B] text-xs font-bold text-gray-300 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            <span>التالي</span>
            <ChevronLeft size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
