"use client";

import React, { useEffect, useMemo, useState } from "react";
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

const filters = [
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

const sectionNames: Record<string, string> = {
  all: "أحدث الإضافات",
  movies: "أفلام مميزة",
  series: "مسلسلات مميزة",
  foreign_movies: "أفلام أجنبية",
  arabic_movies: "أفلام عربية",
  turkish_movies: "أفلام تركية",
  foreign_series: "مسلسلات أجنبية",
  arabic_series: "مسلسلات عربية",
  turkish_series: "مسلسلات تركية",
  ramadan: "مسلسلات رمضان",
  anime: "أنمي وكرتون",
  action: "أكشن ومغامرة",
  horror: "رعب وغموض",
  wrestling: "عروض مصارعة",
};

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
  return (
    <article
      onClick={onOpen}
      className="media-poster-card group relative min-w-0 cursor-pointer overflow-hidden rounded-[22px] border border-amber-500/15 bg-[#12131D] shadow-[0_8px_25px_rgba(0,0,0,0.5)] transition duration-300 hover:-translate-y-1.5 hover:border-amber-400/60 hover:shadow-[0_12px_32px_rgba(212,175,55,0.22)]"
    >
      <div className="relative aspect-[0.72] overflow-hidden bg-[#0A0B10]">
        <img
          src={(item.poster?.startsWith("//") ? "https:" + item.poster : item.poster) || "/logo.jpg"}
          alt={item.title}
          loading="lazy"
          decoding="async"
          className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
          onError={(event) => { event.currentTarget.src = "/logo.jpg"; }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#090A0E] via-[#090A0E]/30 to-transparent" />
        
        {/* Quality or Type Badge */}
        <span className="absolute right-2.5 top-2.5 rounded-full border border-amber-500/30 bg-[#090A0E]/85 px-2.5 py-0.5 text-[10px] font-extrabold text-amber-300 shadow-md backdrop-blur-md">
          {item.quality || (item.isSeries ? "مسلسل" : "فيلم")}
        </span>

        {/* Rating */}
        {item.rating && (
          <span className="absolute left-2.5 top-2.5 inline-flex items-center gap-1 rounded-full border border-amber-500/20 bg-[#090A0E]/85 px-2.5 py-0.5 text-[10px] font-bold text-amber-400 backdrop-blur-md">
            <Star size={11} className="fill-amber-400 text-amber-400" /> {item.rating}
          </span>
        )}

        {/* Favorite Button */}
        <button
          type="button"
          aria-label={isFavorite ? "إزالة من قائمتي" : "إضافة إلى قائمتي"}
          onClick={onFavorite}
          className={`absolute left-2.5 top-10 grid h-8 w-8 place-items-center rounded-full border shadow-md transition ${
            isFavorite
              ? "border-rose-500 bg-rose-600 text-white shadow-rose-600/40"
              : "border-white/10 bg-[#090A0E]/75 text-slate-300 hover:border-rose-400 hover:text-rose-400 backdrop-blur-md"
          }`}
        >
          <Heart size={14} className={isFavorite ? "fill-current" : ""} />
        </button>

        {/* Meta & Title */}
        <div className="absolute inset-x-0 bottom-0 p-3.5 text-white">
          <div className="mb-1.5 flex flex-wrap items-center gap-1.5 text-[10px] font-bold text-amber-400/90">
            <span className="rounded-md border border-white/10 bg-white/10 px-2 py-0.5 backdrop-blur-md text-white">
              {item.category || (item.isSeries ? "مسلسل" : "فيلم")}
            </span>
            {item.year && <span>{item.year}</span>}
          </div>
          <h3 className="line-clamp-2 text-sm font-extrabold leading-tight text-white group-hover:text-amber-300 transition">
            {item.title}
          </h3>
        </div>

        {/* Floating Quick Play Button */}
        <button
          type="button"
          onClick={onPlay}
          disabled={resolving}
          aria-label={`تشغيل ${item.title}`}
          className="absolute bottom-3 left-3 grid h-10 w-10 translate-y-2 place-items-center rounded-full bg-gradient-to-r from-amber-400 to-amber-600 text-slate-950 font-black opacity-0 shadow-lg shadow-amber-500/40 transition group-hover:translate-y-0 group-hover:opacity-100 focus:translate-y-0 focus:opacity-100 max-sm:translate-y-0 max-sm:opacity-100"
        >
          {resolving ? <LoaderCircle size={17} className="animate-spin text-slate-950" /> : <Play size={16} className="fill-current" />}
        </button>
      </div>

      <button
        type="button"
        onClick={(event) => { event.stopPropagation(); onOpen(); }}
        className="flex w-full items-center justify-between border-t border-white/5 bg-[#141522] px-3.5 py-2.5 text-right text-[11px] font-bold text-slate-400 transition hover:bg-[#181A2A] hover:text-amber-300"
      >
        <span>التفاصيل والحلقات</span>
        <ArrowLeft size={13} className="text-amber-400" />
      </button>
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
          const response = await fetch(`/api/cinema/search?q=${encodeURIComponent(query)}`, { signal: controller.signal });
          const result = await response.json();
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
            const response = await fetch(`/api/cinema/${kind}?page=${page}`, { signal: controller.signal });
            const result = await response.json();
            const list = result.success && Array.isArray(result.data) ? result.data : [];
            if (kind === "movies") movies = list;
            else series = list;
            publishAvailableItems();
          };
          await Promise.allSettled([readList("movies"), readList("series")]);
          return;
        }

        const slug = filter !== "all" ? filter : activeTab;
        const endpoint = filter === "all" && (activeTab === "movies" || activeTab === "series")
          ? `/api/cinema/${activeTab}?page=${page}`
          : `/api/cinema/category?slug=${encodeURIComponent(slug)}&page=${page}`;
        const response = await fetch(endpoint, { signal: controller.signal });
        const result = await response.json();
        if (!cancelled) {
          const nextItems = result.success && Array.isArray(result.data) ? result.data : [];
          setItems(nextItems);
          setHomeMovies([]);
          setHomeSeries([]);
          setHasMore(nextItems.length >= 8);
        }
      } catch (error) {
        if (!cancelled && !(error instanceof DOMException && error.name === "AbortError")) {
          console.error("Cinema listing could not be loaded:", error);
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

  const featured = useMemo(() => items.find((item) => item.poster) || items[0], [items]);
  const isHome = activeTab === "home" && filter === "all" && !searchQuery.trim();
  const pageTitle = searchQuery.trim()
    ? `نتائج البحث عن «${searchQuery.trim()}»`
    : sectionNames[filter === "all" ? activeTab : filter] || "أحدث الأعمال";

  const playQuickly = async (event: React.MouseEvent<HTMLButtonElement>, item: ArabseedItem) => {
    event.stopPropagation();
    setResolvingId(item.id);
    try {
      const response = await fetch(`/api/cinema/detail?url=${encodeURIComponent(item.pageUrl)}`);
      const result = await response.json();
      const detail = result.data;
      const server = detail?.servers?.[0];
      if (!result.success || !server?.embedUrl) {
        onSelectItem(item);
        return;
      }
      const resolvedResponse = await fetch(`/api/cinema/resolve?serverUrl=${encodeURIComponent(server.embedUrl)}`);
      const resolved = await resolvedResponse.json();
      const url = resolved.playUrl || resolved.data?.playUrl || server.embedUrl;
      onPlayStream({ title: item.title, subtitle: server.name, poster: item.poster, streams: [{ name: server.name, url }] });
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
      isFavorite={favorites.some((favorite) => favorite.id === item.id)}
      resolving={resolvingId === item.id}
      onOpen={() => onSelectItem(item)}
      onPlay={(event) => { void playQuickly(event, item); }}
      onFavorite={(event) => { event.stopPropagation(); onToggleFavorite(item); }}
    />
  );

  if (loading && items.length === 0) {
    return (
      <div className="space-y-7">
        <div className="h-64 sm:h-80 w-full rounded-[28px] skeleton-shimmer border border-amber-500/10" />
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i} className="aspect-[0.72] rounded-[22px] skeleton-shimmer border border-white/5" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Filters Bar */}
      {!searchQuery.trim() && (
        <div className="no-scrollbar -mx-4 flex items-center gap-2 overflow-x-auto px-4 pb-2 sm:mx-0 sm:px-0">
          {filters.map(({ id, label, icon: Icon }) => {
            const active = filter === id;
            return (
              <button
                key={id}
                type="button"
                onClick={() => { setFilter(id); setPage(1); }}
                className={`inline-flex shrink-0 items-center gap-2 rounded-2xl px-4 py-2.5 text-xs font-bold transition duration-200 ${
                  active
                    ? "border border-amber-500/40 bg-gradient-to-r from-amber-500/25 to-amber-500/15 text-amber-300 shadow-[0_0_15px_rgba(212,175,55,0.2)]"
                    : "border border-white/5 bg-[#12131D] text-slate-300 hover:border-amber-500/30 hover:bg-[#161726] hover:text-white"
                }`}
              >
                <Icon size={14} className={active ? "text-amber-400" : "text-slate-400"} />
                {label}
              </button>
            );
          })}
        </div>
      )}

      {/* Featured Hero Banner */}
      {isHome && featured && (
        <section className="home-hero relative overflow-hidden rounded-[30px] border border-amber-500/20 bg-[#12131D] shadow-[0_15px_40px_rgba(0,0,0,0.6)]">
          <div className="home-hero-backdrop absolute inset-0 z-0">
            <img
              src={featured.poster || "/logo.jpg"}
              alt=""
              className="h-full w-full object-cover object-center opacity-30 blur-md scale-105"
            />
            <div className="absolute inset-0 bg-gradient-to-r from-[#090A0E] via-[#090A0E]/85 to-transparent" />
            <div className="absolute inset-0 bg-gradient-to-t from-[#090A0E] via-transparent to-transparent" />
          </div>

          <div className="relative z-10 flex flex-col md:flex-row items-center gap-6 p-6 sm:p-8 lg:p-10">
            <div className="relative w-36 sm:w-48 shrink-0 overflow-hidden rounded-[22px] border border-amber-500/30 shadow-[0_10px_30px_rgba(212,175,55,0.25)]">
              <img
                src={featured.poster || "/logo.jpg"}
                alt={featured.title}
                className="aspect-[0.72] w-full object-cover"
                loading="eager"
                fetchPriority="high"
              />
            </div>
            <div className="flex-1 text-center md:text-right space-y-3">
              <div className="flex flex-wrap items-center justify-center md:justify-start gap-2">
                <span className="rounded-full border border-amber-500/30 bg-amber-500/15 px-3 py-1 text-[11px] font-black text-amber-300">
                  اختيار اليوم
                </span>
                <span className="rounded-full border border-white/10 bg-white/10 px-3 py-1 text-[11px] font-bold text-slate-300">
                  {featured.isSeries ? "مسلسل" : "فيلم"}
                </span>
                {featured.year && (
                  <span className="rounded-full border border-white/10 bg-white/10 px-3 py-1 text-[11px] font-bold text-slate-300">
                    {featured.year}
                  </span>
                )}
                {featured.rating && (
                  <span className="inline-flex items-center gap-1 rounded-full border border-amber-500/25 bg-[#090A0E]/80 px-3 py-1 text-[11px] font-extrabold text-amber-400">
                    <Star size={12} className="fill-current" /> {featured.rating}
                  </span>
                )}
              </div>
              <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-white leading-tight">
                {featured.title}
              </h2>
              <p className="hero-description max-w-2xl text-xs sm:text-sm text-slate-300 leading-relaxed">
                اعرف القصة، اختار الموسم والحلقة، وانتقل إلى سيرفر المشاهدة المتاح للعمل.
              </p>
              <div className="flex flex-wrap items-center justify-center md:justify-start gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => onSelectItem(featured)}
                  className="hero-primary inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-amber-400 via-amber-500 to-amber-600 px-6 py-3 text-sm font-black text-slate-950 shadow-lg shadow-amber-500/30 transition hover:scale-105 active:scale-95"
                >
                  <Play size={16} className="fill-current" />
                  مشاهدة الآن
                </button>
                <button
                  type="button"
                  onClick={() => onSelectItem(featured)}
                  className="hero-secondary inline-flex items-center gap-2 rounded-2xl border border-amber-500/30 bg-[#141524]/80 px-5 py-3 text-sm font-bold text-slate-200 transition hover:border-amber-400 hover:text-white"
                >
                  التفاصيل وسيرفرات التحميل
                </button>
              </div>
            </div>
          </div>
        </section>
      )}

      {/* Main Content Sections */}
      {isHome ? (
        <div className="space-y-10">
          {/* Movies Section */}
          {homeMovies.length > 0 && (
            <section className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-[10px] font-extrabold uppercase tracking-widest text-amber-400">سينما مختارة</p>
                  <h3 className="text-xl font-black text-white">أحدث الأفلام العالمية والعربية</h3>
                </div>
                {onBrowseTab && (
                  <button
                    type="button"
                    onClick={() => onBrowseTab("movies")}
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-400 hover:text-amber-300 transition"
                  >
                    <span>عرض المزيد</span>
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
                  <p className="text-[10px] font-extrabold uppercase tracking-widest text-amber-400">دراما حصرية</p>
                  <h3 className="text-xl font-black text-white">أحدث المسلسلات والمواسم الكاملة</h3>
                </div>
                {onBrowseTab && (
                  <button
                    type="button"
                    onClick={() => onBrowseTab("series")}
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-400 hover:text-amber-300 transition"
                  >
                    <span>عرض المزيد</span>
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
        <section className="space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[10px] font-extrabold uppercase tracking-widest text-amber-400">تصفح المكتبة</p>
              <h2 className="text-2xl font-black text-white">{pageTitle}</h2>
            </div>
            {items.length > 0 && (
              <span className="rounded-full border border-amber-500/20 bg-amber-500/10 px-3 py-1 text-xs font-bold text-amber-400">
                {items.length} عمل متاح
              </span>
            )}
          </div>

          {items.length === 0 ? (
            <div className="rounded-[28px] border border-dashed border-amber-500/20 bg-[#12131D] p-12 text-center">
              <Search size={40} className="mx-auto text-amber-400/50" />
              <h3 className="mt-3 text-lg font-bold text-white">لم يتم العثور على نتائج</h3>
              <p className="mt-1 text-xs text-slate-400">جرب البحث بكلمات مختلفة أو اسم آخر للعمل.</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
              {items.map(renderCard)}
            </div>
          )}

          {/* Pagination */}
          {!searchQuery.trim() && (
            <div className="flex items-center justify-center gap-3 pt-6">
              <button
                type="button"
                disabled={page <= 1}
                onClick={() => { setPage((p) => Math.max(1, p - 1)); window.scrollTo({ top: 0, behavior: "smooth" }); }}
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
                onClick={() => { setPage((p) => p + 1); window.scrollTo({ top: 0, behavior: "smooth" }); }}
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
