"use client";

import React, { useEffect, useMemo, useState, useRef } from "react";
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  Film,
  Flame,
  Heart,
  LoaderCircle,
  Play,
  Search,
  Sparkles,
  Star,
  Tv,
  Info,
  SlidersHorizontal,
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
  onBrowseTab?: (tab: "movies" | "series") => void;
}

const FILTERS = [
  { id: "all", label: "الكل", icon: Sparkles },
  { id: "foreign_movies", label: "أفلام أجنبية", icon: Film },
  { id: "arabic_movies", label: "أفلام عربية", icon: Film },
  { id: "turkish_movies", label: "أفلام تركية", icon: Film },
  { id: "foreign_series", label: "مسلسلات أجنبية", icon: Tv },
  { id: "arabic_series", label: "مسلسلات عربية", icon: Tv },
  { id: "turkish_series", label: "مسلسلات تركية", icon: Tv },
  { id: "ramadan", label: "رمضان", icon: Sparkles },
  { id: "anime", label: "أنمي وكرتون", icon: Sparkles },
  { id: "action", label: "أكشن", icon: Flame },
  { id: "horror", label: "رعب وغموض", icon: Film },
  { id: "wrestling", label: "مصارعة", icon: Flame },
];

const SECTION_TITLES: Record<string, string> = {
  all: "أحدث الإضافات",
  movies: "مكتبة الأفلام",
  series: "مكتبة المسلسلات",
  foreign_movies: "أفلام أجنبية",
  arabic_movies: "أفلام عربية",
  turkish_movies: "أفلام تركية",
  foreign_series: "مسلسلات أجنبية",
  arabic_series: "مسلسلات عربية",
  turkish_series: "مسلسلات تركية",
  ramadan: "مسلسلات رمضان",
  anime: "أنمي وكرتون",
  action: "أكشن ومغامرة",
  horror: "رعب وتشويق",
  wrestling: "عروض مصارعة",
};

// Adaptive title font sizing helper (matches mobile app commit 82c7af8)
function getAdaptiveTitleClass(title: string): string {
  const len = title.trim().length;
  if (len > 45) return "text-xs sm:text-sm";
  if (len > 30) return "text-sm sm:text-base";
  return "text-base sm:text-lg";
}

function PosterCard({
  item,
  isFavorite,
  resolving,
  onOpen,
  onPlay,
  onFavorite,
}: {
  item: ArabseedItem;
  isFavorite: boolean;
  resolving: boolean;
  onOpen: () => void;
  onPlay: (event: React.MouseEvent<HTMLButtonElement>) => void;
  onFavorite: (event: React.MouseEvent<HTMLButtonElement>) => void;
}) {
  const posterUrl = (item.poster?.startsWith("//") ? "https:" + item.poster : item.poster) || "/logo.jpg";

  return (
    <article
      onClick={onOpen}
      className="media-poster-card group relative min-w-0 cursor-pointer overflow-hidden rounded-2xl sm:rounded-[22px] border border-amber-500/15 bg-[#12131D] shadow-[0_8px_25px_rgba(0,0,0,0.5)] transition-all duration-300 hover:-translate-y-1.5 hover:border-amber-400/60 hover:shadow-[0_12px_35px_rgba(212,175,55,0.22)]"
    >
      <div className="relative aspect-[0.72] overflow-hidden bg-[#0A0B10]">
        <img
          src={posterUrl}
          alt={item.title}
          loading="lazy"
          decoding="async"
          className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
          onError={(e) => {
            e.currentTarget.src = "/logo.jpg";
          }}
        />
        {/* Ambient Gradient */}
        <div className="absolute inset-0 bg-gradient-to-t from-[#090A0E] via-[#090A0E]/30 to-transparent opacity-90 transition group-hover:opacity-100" />

        {/* Quality / Type Badge */}
        <span className="absolute right-2 top-2 sm:right-2.5 sm:top-2.5 rounded-full border border-amber-500/30 bg-[#090A0E]/90 px-2 sm:px-2.5 py-0.5 text-[9px] sm:text-[10px] font-black text-amber-300 shadow-md backdrop-blur-md">
          {item.quality || (item.isSeries ? "مسلسل" : "فيلم")}
        </span>

        {/* Rating */}
        {item.rating && (
          <span className="absolute left-2 top-2 sm:left-2.5 sm:top-2.5 inline-flex items-center gap-1 rounded-full border border-amber-500/20 bg-[#090A0E]/90 px-2 sm:px-2.5 py-0.5 text-[9px] sm:text-[10px] font-bold text-amber-400 backdrop-blur-md">
            <Star size={10} className="fill-amber-400 text-amber-400" /> {item.rating}
          </span>
        )}

        {/* Favorite Button */}
        <button
          type="button"
          aria-label={isFavorite ? "إزالة من المفضلة" : "إضافة للمفضلة"}
          onClick={onFavorite}
          className={`absolute left-2 top-8 sm:left-2.5 sm:top-9 grid h-7 w-7 sm:h-8 sm:w-8 place-items-center rounded-full border shadow-md transition ${
            isFavorite
              ? "border-rose-500 bg-rose-600 text-white shadow-rose-600/40"
              : "border-white/10 bg-[#090A0E]/80 text-slate-300 hover:border-rose-400 hover:text-rose-400 backdrop-blur-md"
          }`}
        >
          <Heart size={13} className={isFavorite ? "fill-current" : ""} />
        </button>

        {/* Bottom Info & Full Adaptive Title */}
        <div className="absolute inset-x-0 bottom-0 p-2.5 sm:p-3.5 text-white">
          <div className="mb-1 flex flex-wrap items-center gap-1 sm:gap-1.5 text-[9px] sm:text-[10px] font-bold text-amber-400/90">
            <span className="rounded border border-white/10 bg-white/10 px-1.5 sm:px-2 py-0.5 backdrop-blur-md text-white">
              {item.category || (item.isSeries ? "مسلسل" : "فيلم")}
            </span>
            {item.year && <span>{item.year}</span>}
          </div>
          <h3
            className={`font-black leading-tight text-white group-hover:text-amber-300 transition line-clamp-2 ${getAdaptiveTitleClass(
              item.title
            )}`}
          >
            {item.title}
          </h3>
        </div>

        {/* Floating Quick Play Button */}
        <button
          type="button"
          onClick={onPlay}
          disabled={resolving}
          aria-label={`تشغيل ${item.title}`}
          className="absolute bottom-2.5 left-2.5 sm:bottom-3 sm:left-3 grid h-9 w-9 sm:h-10 sm:w-10 translate-y-2 place-items-center rounded-full bg-gradient-to-r from-amber-400 to-amber-600 text-slate-950 font-black opacity-0 shadow-lg shadow-amber-500/40 transition group-hover:translate-y-0 group-hover:opacity-100 focus:translate-y-0 focus:opacity-100 max-sm:translate-y-0 max-sm:opacity-100"
        >
          {resolving ? (
            <LoaderCircle size={16} className="animate-spin text-slate-950" />
          ) : (
            <Play size={15} className="fill-current" />
          )}
        </button>
      </div>

      <div className="flex w-full items-center justify-between border-t border-white/5 bg-[#141522] px-3 py-2 text-right text-[10px] sm:text-[11px] font-bold text-slate-400 transition group-hover:bg-[#181A2A] group-hover:text-amber-300">
        <span>التفاصيل والسيرفرات</span>
        <ArrowLeft size={12} className="text-amber-400 transition group-hover:-translate-x-1" />
      </div>
    </article>
  );
}

export default function CinemaView({
  activeTab = "home",
  selectedGenre,
  searchQuery,
  onSelectItem,
  onPlayStream,
  favorites,
  onToggleFavorite,
  onBrowseTab,
}: CinemaViewProps) {
  const [filter, setFilter] = useState("all");
  const [items, setItems] = useState<ArabseedItem[]>([]);
  const [homeMovies, setHomeMovies] = useState<ArabseedItem[]>([]);
  const [homeSeries, setHomeSeries] = useState<ArabseedItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [resolvingId, setResolvingId] = useState<string | null>(null);

  // Multi-Slide Featured Carousel State
  const [carouselIndex, setCarouselIndex] = useState(0);
  const carouselTimerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (selectedGenre) setFilter(selectedGenre);
    else setFilter("all");
    setPage(1);
  }, [activeTab, selectedGenre]);

  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const query = searchQuery.trim();
        if (query) {
          const res = await fetch(`/api/cinema/search?q=${encodeURIComponent(query)}`, {
            signal: controller.signal,
          });
          const result = await res.json();
          if (!cancelled) {
            setItems(result.success && Array.isArray(result.data) ? result.data : []);
            setHomeMovies([]);
            setHomeSeries([]);
            setHasMore(false);
          }
          return;
        }

        if (activeTab === "home" && filter === "all") {
          let movies: ArabseedItem[] = [];
          let series: ArabseedItem[] = [];
          const publishAvailableItems = () => {
            if (cancelled) return;
            setHomeMovies(movies);
            setHomeSeries(series);
            setItems([...movies, ...series]);
            setHasMore(movies.length >= 8 || series.length >= 8);
          };
          const readList = async (kind: "movies" | "series") => {
            const res = await fetch(`/api/cinema/${kind}?page=${page}`, {
              signal: controller.signal,
            });
            const result = await res.json();
            const list = result.success && Array.isArray(result.data) ? result.data : [];
            if (kind === "movies") movies = list;
            else series = list;
            publishAvailableItems();
          };
          await Promise.allSettled([readList("movies"), readList("series")]);
          return;
        }

        const slug = filter !== "all" ? filter : activeTab;
        const endpoint =
          filter === "all" && (activeTab === "movies" || activeTab === "series")
            ? `/api/cinema/${activeTab}?page=${page}`
            : `/api/cinema/category?slug=${encodeURIComponent(slug)}&page=${page}`;
        const res = await fetch(endpoint, { signal: controller.signal });
        const result = await res.json();
        if (!cancelled) {
          const nextItems = result.success && Array.isArray(result.data) ? result.data : [];
          setItems(nextItems);
          setHomeMovies([]);
          setHomeSeries([]);
          setHasMore(nextItems.length >= 8);
        }
      } catch (err) {
        if (!cancelled && !(err instanceof DOMException && err.name === "AbortError")) {
          setItems([]);
          setHomeMovies([]);
          setHomeSeries([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }, searchQuery.trim() ? 250 : 0);

    return () => {
      cancelled = true;
      clearTimeout(timer);
      controller.abort();
    };
  }, [activeTab, filter, page, searchQuery]);

  // Featured carousel items (top 5 items with posters)
  const featuredItems = useMemo(() => {
    return items.filter((i) => i.poster && !i.poster.includes("default")).slice(0, 5);
  }, [items]);

  const activeFeatured = featuredItems[carouselIndex] || featuredItems[0] || items[0];

  // Auto-advance featured carousel every 7 seconds
  useEffect(() => {
    if (featuredItems.length <= 1) return;
    carouselTimerRef.current = setInterval(() => {
      setCarouselIndex((prev) => (prev + 1) % featuredItems.length);
    }, 7000);
    return () => {
      if (carouselTimerRef.current) clearInterval(carouselTimerRef.current);
    };
  }, [featuredItems.length]);

  const isHome = activeTab === "home" && filter === "all" && !searchQuery.trim();
  const pageTitle = searchQuery.trim()
    ? `نتائج البحث عن «${searchQuery.trim()}»`
    : SECTION_TITLES[filter === "all" ? activeTab : filter] || "أحدث الأعمال";

  const playQuickly = async (event: React.MouseEvent<HTMLButtonElement>, item: ArabseedItem) => {
    event.stopPropagation();
    setResolvingId(item.id);
    try {
      const res = await fetch(`/api/cinema/detail?url=${encodeURIComponent(item.pageUrl)}`);
      const result = await res.json();
      const detail = result.data;
      const server = detail?.servers?.[0];
      if (!result.success || !server?.embedUrl) {
        onSelectItem(item);
        return;
      }
      const resolvedRes = await fetch(`/api/cinema/resolve?serverUrl=${encodeURIComponent(server.embedUrl)}`);
      const resolved = await resolvedRes.json();
      const url = resolved.playUrl || resolved.data?.playUrl || server.embedUrl;
      onPlayStream({
        title: item.title,
        subtitle: server.name,
        poster: item.poster,
        streams: [{ name: server.name, url }],
      });
    } catch {
      onSelectItem(item);
    } finally {
      setResolvingId(null);
    }
  };

  const renderCard = (item: ArabseedItem) => (
    <PosterCard
      key={`${item.providerId || "source"}:${item.id}`}
      item={item}
      isFavorite={favorites.some((f) => f.id === item.id)}
      resolving={resolvingId === item.id}
      onOpen={() => onSelectItem(item)}
      onPlay={(e) => {
        void playQuickly(e, item);
      }}
      onFavorite={(e) => {
        e.stopPropagation();
        onToggleFavorite(item);
      }}
    />
  );

  if (loading && items.length === 0) {
    return (
      <div className="space-y-7">
        <div className="h-64 sm:h-96 w-full rounded-[28px] skeleton-shimmer border border-amber-500/10" />
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i} className="aspect-[0.72] rounded-2xl sm:rounded-[22px] skeleton-shimmer border border-white/5" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8 sm:space-y-10">
      {/* Filters Bar */}
      {!searchQuery.trim() && (
        <div className="no-scrollbar -mx-4 flex items-center gap-2 overflow-x-auto px-4 pb-2 sm:mx-0 sm:px-0">
          {FILTERS.map(({ id, label, icon: Icon }) => {
            const active = filter === id;
            return (
              <button
                key={id}
                type="button"
                onClick={() => {
                  setFilter(id);
                  setPage(1);
                }}
                className={`inline-flex shrink-0 items-center gap-2 rounded-2xl px-3.5 sm:px-4 py-2 sm:py-2.5 text-xs font-bold transition duration-200 ${
                  active
                    ? "border border-amber-500/40 bg-gradient-to-r from-amber-500/25 to-amber-500/15 text-amber-300 shadow-[0_0_15px_rgba(212,175,55,0.2)]"
                    : "border border-white/5 bg-[#12131D] text-slate-300 hover:border-amber-500/30 hover:bg-[#161726] hover:text-white"
                }`}
              >
                <Icon size={14} className={active ? "text-amber-400" : "text-slate-400"} />
                <span>{label}</span>
              </button>
            );
          })}
        </div>
      )}

      {/* Featured Hero Carousel Banner (Home Mode) */}
      {isHome && activeFeatured && (
        <section className="relative overflow-hidden rounded-3xl sm:rounded-[32px] border border-amber-500/25 bg-[#12131D] shadow-[0_15px_50px_rgba(0,0,0,0.7)] transition-all duration-500">
          {/* Blurred Cinematic Backdrop */}
          <div className="absolute inset-0 z-0">
            <img
              src={activeFeatured.poster || "/logo.jpg"}
              alt=""
              className="h-full w-full object-cover object-center opacity-30 blur-xl scale-110 transition-all duration-700"
            />
            <div className="absolute inset-0 bg-gradient-to-r from-[#090A0E] via-[#090A0E]/85 to-transparent" />
            <div className="absolute inset-0 bg-gradient-to-t from-[#090A0E] via-transparent to-transparent" />
          </div>

          <div className="relative z-10 flex flex-col md:flex-row items-center gap-6 p-6 sm:p-8 lg:p-12">
            {/* Foreground Poster Card */}
            <div className="relative w-36 sm:w-52 shrink-0 overflow-hidden rounded-2xl sm:rounded-[24px] border border-amber-500/40 shadow-[0_10px_35px_rgba(212,175,55,0.3)] transition duration-500 hover:scale-105">
              <img
                src={activeFeatured.poster || "/logo.jpg"}
                alt={activeFeatured.title}
                className="aspect-[0.72] w-full object-cover"
                loading="eager"
              />
            </div>

            {/* Meta & Full Adaptive Title */}
            <div className="flex-1 text-center md:text-right space-y-3 sm:space-y-4">
              <div className="flex flex-wrap items-center justify-center md:justify-start gap-2">
                <span className="rounded-full border border-amber-500/30 bg-amber-500/20 px-3 py-1 text-[11px] font-black text-amber-300 shadow-sm">
                  ✨ اختيار اليوم المميز
                </span>
                <span className="rounded-full border border-white/10 bg-white/10 px-3 py-1 text-[11px] font-bold text-slate-200">
                  {activeFeatured.isSeries ? "مسلسل" : "فيلم"}
                </span>
                {activeFeatured.year && (
                  <span className="rounded-full border border-white/10 bg-white/10 px-3 py-1 text-[11px] font-bold text-slate-300">
                    {activeFeatured.year}
                  </span>
                )}
                {activeFeatured.quality && (
                  <span className="rounded-full border border-amber-500/20 bg-[#090A0E]/80 px-3 py-1 text-[11px] font-extrabold text-amber-400">
                    {activeFeatured.quality}
                  </span>
                )}
                {activeFeatured.rating && (
                  <span className="inline-flex items-center gap-1 rounded-full border border-amber-500/25 bg-[#090A0E]/80 px-3 py-1 text-[11px] font-extrabold text-amber-400">
                    <Star size={12} className="fill-current" /> {activeFeatured.rating}
                  </span>
                )}
              </div>

              {/* Dynamic Adaptive Font Sizing (Complete title, no truncation) */}
              <h2
                className={`font-black text-white leading-tight transition-all duration-300 ${
                  activeFeatured.title.length > 40
                    ? "text-xl sm:text-2xl lg:text-3xl"
                    : activeFeatured.title.length > 25
                    ? "text-2xl sm:text-3xl lg:text-4xl"
                    : "text-3xl sm:text-4xl lg:text-5xl"
                }`}
              >
                {activeFeatured.title}
              </h2>

              <p className="max-w-2xl text-xs sm:text-sm text-slate-300 leading-relaxed">
                شاهد بجودة فائقة السرعة مع إمكانية اختيار الجودة والترجمة وتخطي السيرفرات المتوقفة تلقائياً.
              </p>

              {/* Action Buttons */}
              <div className="flex flex-wrap items-center justify-center md:justify-start gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => onSelectItem(activeFeatured)}
                  className="inline-flex items-center gap-2.5 rounded-2xl bg-gradient-to-r from-amber-400 via-amber-500 to-amber-600 px-7 py-3.5 text-sm font-black text-slate-950 shadow-lg shadow-amber-500/30 transition hover:scale-105 active:scale-95"
                >
                  <Play size={17} className="fill-current" />
                  مشاهدة الآن
                </button>
                <button
                  type="button"
                  onClick={() => onSelectItem(activeFeatured)}
                  className="inline-flex items-center gap-2 rounded-2xl border border-amber-500/30 bg-[#141524]/90 px-6 py-3.5 text-sm font-bold text-slate-200 transition hover:border-amber-400 hover:text-white"
                >
                  <Info size={16} />
                  الحلقات وسيرفرات التحميل
                </button>
              </div>

              {/* Carousel Slide Indicators */}
              {featuredItems.length > 1 && (
                <div className="flex items-center justify-center md:justify-start gap-2 pt-3">
                  {featuredItems.map((_, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => setCarouselIndex(idx)}
                      className={`h-2 rounded-full transition-all duration-300 ${
                        carouselIndex === idx ? "w-8 bg-amber-400 shadow-sm shadow-amber-400" : "w-2 bg-white/20 hover:bg-white/40"
                      }`}
                      aria-label={`الشريحة ${idx + 1}`}
                    />
                  ))}
                </div>
              )}
            </div>
          </div>
        </section>
      )}

      {/* Main Content Sections */}
      {isHome ? (
        <div className="space-y-12">
          {/* Movies Section */}
          {homeMovies.length > 0 && (
            <section className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-amber-400">سينما مختارة</p>
                  <h3 className="text-xl sm:text-2xl font-black text-white">أحدث الأفلام العالمية والعربية</h3>
                </div>
                {onBrowseTab && (
                  <button
                    type="button"
                    onClick={() => onBrowseTab("movies")}
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-400 hover:text-amber-300 transition"
                  >
                    <span>عرض الكل ({homeMovies.length})</span>
                    <ArrowLeft size={14} />
                  </button>
                )}
              </div>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
                {homeMovies.slice(0, 12).map(renderCard)}
              </div>
            </section>
          )}

          {/* Series Section */}
          {homeSeries.length > 0 && (
            <section className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-amber-400">دراما حصرية</p>
                  <h3 className="text-xl sm:text-2xl font-black text-white">أحدث المسلسلات والمواسم الكاملة</h3>
                </div>
                {onBrowseTab && (
                  <button
                    type="button"
                    onClick={() => onBrowseTab("series")}
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-400 hover:text-amber-300 transition"
                  >
                    <span>عرض الكل ({homeSeries.length})</span>
                    <ArrowLeft size={14} />
                  </button>
                )}
              </div>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
                {homeSeries.slice(0, 12).map(renderCard)}
              </div>
            </section>
          )}
        </div>
      ) : (
        <section className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[10px] font-black uppercase tracking-widest text-amber-400">تصفح المكتبة</p>
              <h2 className="text-2xl sm:text-3xl font-black text-white">{pageTitle}</h2>
            </div>
            {items.length > 0 && (
              <span className="rounded-full border border-amber-500/20 bg-amber-500/10 px-3.5 py-1 text-xs font-bold text-amber-400">
                {items.length} عمل متاح
              </span>
            )}
          </div>

          {items.length === 0 ? (
            <div className="rounded-[28px] border border-dashed border-amber-500/20 bg-[#12131D] p-12 text-center">
              <Search size={40} className="mx-auto text-amber-400/50 mb-3" />
              <h3 className="text-lg font-bold text-white">لم يتم العثور على نتائج</h3>
              <p className="mt-1 text-xs text-slate-400">جرب البحث باسم آخر أو بكلمات أبسط.</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
              {items.map(renderCard)}
            </div>
          )}

          {/* Pagination */}
          {!searchQuery.trim() && items.length > 0 && (
            <div className="flex items-center justify-center gap-3 pt-8">
              <button
                type="button"
                disabled={page <= 1}
                onClick={() => {
                  setPage((p) => Math.max(1, p - 1));
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
                className="inline-flex items-center gap-2 rounded-2xl border border-amber-500/20 bg-[#12131D] px-5 py-2.5 text-xs font-bold text-white transition hover:border-amber-400 hover:text-amber-300 disabled:opacity-40"
              >
                <ChevronRight size={16} /> الصفحة السابقة
              </button>
              <span className="rounded-xl border border-amber-500/30 bg-amber-500/15 px-4 py-2 text-xs font-black text-amber-300">
                صفحة {page}
              </span>
              <button
                type="button"
                disabled={!hasMore}
                onClick={() => {
                  setPage((p) => p + 1);
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
                className="inline-flex items-center gap-2 rounded-2xl border border-amber-500/20 bg-[#12131D] px-5 py-2.5 text-xs font-bold text-white transition hover:border-amber-400 hover:text-amber-300 disabled:opacity-40"
              >
                الصفحة التالية <ChevronLeft size={16} />
              </button>
            </div>
          )}
        </section>
      )}
    </div>
  );
}
