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
  movies: "أفلام تستاهل وقتك",
  series: "مسلسلات تستاهل المتابعة",
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
      className="group relative min-w-0 cursor-pointer overflow-hidden rounded-[20px] border border-slate-200 bg-white shadow-[0_6px_20px_rgba(35,49,78,0.07)] transition duration-300 hover:-translate-y-1 hover:border-indigo-200 hover:shadow-[0_14px_28px_rgba(48,63,130,0.14)]"
    >
      <div className="relative aspect-[0.72] overflow-hidden bg-slate-100">
        <img
          src={(item.poster?.startsWith("//") ? "https:" + item.poster : item.poster) || "/logo.jpg"}
          alt={item.title}
          loading="lazy"
          className="h-full w-full object-cover transition duration-500 group-hover:scale-[1.04]"
          onError={(event) => { event.currentTarget.src = "/logo.jpg"; }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950/85 via-slate-950/10 to-transparent" />
        <span className="absolute right-2.5 top-2.5 rounded-full bg-white/95 px-2.5 py-1 text-[10px] font-extrabold text-slate-700 shadow-sm">
          {item.quality || (item.isSeries ? "مسلسل" : "فيلم")}
        </span>
        {item.rating && (
          <span className="absolute left-2.5 top-2.5 inline-flex items-center gap-1 rounded-full bg-slate-950/75 px-2 py-1 text-[10px] font-bold text-white backdrop-blur">
            <Star size={11} className="fill-amber-300 text-amber-300" /> {item.rating}
          </span>
        )}
        <button
          type="button"
          aria-label={isFavorite ? "إزالة من قائمتي" : "إضافة إلى قائمتي"}
          onClick={onFavorite}
          className={`absolute left-2.5 top-10 grid h-8 w-8 place-items-center rounded-full shadow-sm transition ${isFavorite ? "bg-rose-500 text-white" : "bg-white/90 text-slate-600 hover:text-rose-500"}`}
        >
          <Heart size={15} className={isFavorite ? "fill-current" : ""} />
        </button>
        <div className="absolute inset-x-0 bottom-0 p-3.5 text-white">
          <div className="mb-1.5 flex flex-wrap items-center gap-1.5 text-[10px] font-bold text-white/85">
            <span className="rounded-full bg-white/15 px-2 py-0.5 backdrop-blur">{item.category || (item.isSeries ? "مسلسل" : "فيلم")}</span>
            {item.year && <span>{item.year}</span>}
          </div>
          <h3 className="line-clamp-2 text-sm font-extrabold leading-5">{item.title}</h3>
        </div>
        <button
          type="button"
          onClick={onPlay}
          disabled={resolving}
          aria-label={`تشغيل ${item.title}`}
          className="absolute bottom-3 left-3 grid h-10 w-10 translate-y-2 place-items-center rounded-full bg-white text-indigo-700 opacity-0 shadow-lg transition group-hover:translate-y-0 group-hover:opacity-100 focus:translate-y-0 focus:opacity-100 max-sm:translate-y-0 max-sm:opacity-100"
        >
          {resolving ? <LoaderCircle size={17} className="animate-spin" /> : <Play size={16} className="fill-current" />}
        </button>

      </div>
      <button
        type="button"
        onClick={(event) => { event.stopPropagation(); onOpen(); }}
        className="flex w-full items-center justify-between px-3.5 py-3 text-right text-[11px] font-bold text-slate-600 transition hover:text-indigo-700"
      >
        <span>التفاصيل والحلقات</span><ArrowLeft size={14} />
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
          const [movieResponse, seriesResponse] = await Promise.all([
            fetch(`/api/cinema/movies?page=${page}`, { signal: controller.signal }),
            fetch(`/api/cinema/series?page=${page}`, { signal: controller.signal }),
          ]);
          const [movieResult, seriesResult] = await Promise.all([movieResponse.json(), seriesResponse.json()]);
          if (!cancelled) {
            const movies = movieResult.success && Array.isArray(movieResult.data) ? movieResult.data : [];
            const series = seriesResult.success && Array.isArray(seriesResult.data) ? seriesResult.data : [];
            setHomeMovies(movies);
            setHomeSeries(series);
            setItems([...movies, ...series]);
            setHasMore(movies.length >= 8 || series.length >= 8);
          }
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
        <div className="skeleton-shimmer h-72 rounded-[28px] sm:h-80" />
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5 xl:grid-cols-6">
          {Array.from({ length: 12 }).map((_, index) => <div key={index} className="skeleton-shimmer aspect-[0.72] rounded-[20px]" />)}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-10 sm:space-y-10">
      {!searchQuery.trim() && (
        <div className="rounded-[22px] border border-slate-200 bg-white p-2 shadow-[0_10px_28px_rgba(35,49,78,0.05)] sm:rounded-[26px] sm:p-3">
          <div className="no-scrollbar flex gap-2 overflow-x-auto px-1 py-0.5">
            {filters.map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                type="button"
                onClick={() => { setFilter(id); setPage(1); }}
                className={`inline-flex shrink-0 items-center gap-2 rounded-xl px-3.5 py-2.5 text-xs font-bold transition ${filter === id ? "bg-indigo-600 text-white shadow-md shadow-indigo-200" : "bg-slate-50 text-slate-600 hover:bg-indigo-50 hover:text-indigo-700"}`}
              >
                <Icon size={14} />{label}
              </button>
            ))}
          </div>
        </div>
      )}

      {isHome && featured && (
        <section className="relative isolate overflow-hidden rounded-[30px] border border-indigo-100 bg-gradient-to-l from-indigo-50 via-white to-emerald-50 shadow-[0_18px_50px_rgba(47,61,116,0.09)]">
          <div className="absolute -left-16 -top-24 h-72 w-72 rounded-full bg-indigo-200/35 blur-3xl" />
          <div className="relative grid min-h-[310px] items-center gap-7 p-5 sm:p-8 md:grid-cols-[1fr_230px] md:p-10 lg:min-h-[360px] lg:grid-cols-[1fr_270px]">
            <div className="relative z-10 max-w-2xl">
              <span className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1.5 text-[11px] font-extrabold text-indigo-700 shadow-sm ring-1 ring-indigo-100">
                <Sparkles size={14} /> اقتراح اليوم
              </span>
              <h1 className="mt-5 text-3xl font-black leading-tight tracking-tight text-slate-900 sm:text-4xl lg:text-[42px]">حكاية جديدة مستنياك</h1>
              <p className="mt-3 max-w-xl text-sm leading-7 text-slate-600 sm:text-base">
                تصفّح أحدث الأفلام والمسلسلات، واختار العمل اللي يناسب مزاجك.
              </p>
              <div className="mt-6 flex flex-wrap items-center gap-2 text-xs font-bold text-slate-500">
                <span className="rounded-full bg-white px-3 py-1.5 shadow-sm">{featured.isSeries ? "مسلسل" : "فيلم"}</span>
                {featured.year && <span className="rounded-full bg-white px-3 py-1.5 shadow-sm">{featured.year}</span>}
                {featured.quality && <span className="rounded-full bg-white px-3 py-1.5 shadow-sm">{featured.quality}</span>}
              </div>
              <div className="mt-7 flex flex-wrap gap-3">
                <button type="button" onClick={() => onSelectItem(featured)} className="inline-flex items-center gap-2 rounded-2xl bg-indigo-600 px-5 py-3 text-sm font-extrabold text-white shadow-lg shadow-indigo-200 transition hover:bg-indigo-700">
                  <Play size={16} className="fill-current" /> اكتشف التفاصيل
                </button>
                <button type="button" onClick={() => onToggleFavorite(featured)} className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm font-bold text-slate-700 transition hover:border-rose-200 hover:text-rose-600">
                  <Heart size={16} /> أضف لقائمتي
                </button>
              </div>
            </div>
            <button type="button" onClick={() => onSelectItem(featured)} className="group relative mx-auto block w-40 overflow-hidden rounded-[24px] border-4 border-white shadow-[0_16px_35px_rgba(28,39,74,0.22)] transition hover:-translate-y-1 sm:w-48 md:w-full">
              <img src={featured.poster || "/logo.jpg"} alt={featured.title} className="aspect-[0.72] w-full object-cover transition duration-500 group-hover:scale-105" onError={(event) => { event.currentTarget.src = "/logo.jpg"; }} />
              <span className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-slate-950/90 to-transparent p-4 pt-12 text-right text-sm font-extrabold text-white">{featured.title}</span>
            </button>
          </div>
        </section>
      )}

      <section className="space-y-5">
        <div className="flex items-end justify-between gap-3">
          <div>
            <p className="text-[11px] font-extrabold uppercase tracking-[0.15em] text-indigo-600">{searchQuery.trim() ? "بحث المكتبة" : "مختار ليك"}</p>
            <h2 className="mt-1 text-xl font-black text-slate-900 sm:text-2xl">{pageTitle}</h2>
          </div>
          <span className="hidden items-center gap-1.5 rounded-full bg-white px-3 py-2 text-[11px] font-bold text-slate-500 ring-1 ring-slate-200 sm:inline-flex">
            <Search size={13} /> {items.length} نتيجة
          </span>
        </div>

        {items.length === 0 ? (
          <div className="rounded-[26px] border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
            <Film size={36} className="mx-auto text-indigo-300" />
            <h3 className="mt-4 text-lg font-extrabold text-slate-800">مش لاقيين نتيجة دلوقتي</h3>
            <p className="mt-1 text-sm text-slate-500">جرّب اسم تاني أو اختار تصنيف مختلف.</p>
          </div>
        ) : isHome ? (
          <div className="space-y-9">
            {homeMovies.length > 0 && (
              <div className="space-y-4">
                <div className="flex items-center justify-between"><h3 className="font-extrabold text-slate-800">أحدث الأفلام</h3><button onClick={() => onBrowseTab?.("movies")} className="inline-flex items-center gap-1 text-xs font-bold text-indigo-700">شوف الكل <ChevronLeft size={15} /></button></div>
                <div className="no-scrollbar grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-5 xl:grid-cols-6">{homeMovies.slice(0, 6).map(renderCard)}</div>
              </div>
            )}
            {homeSeries.length > 0 && (
              <div className="space-y-4">
                <div className="flex items-center justify-between"><h3 className="font-extrabold text-slate-800">مسلسلات وحلقات جديدة</h3><button onClick={() => onBrowseTab?.("series")} className="inline-flex items-center gap-1 text-xs font-bold text-indigo-700">شوف الكل <ChevronLeft size={15} /></button></div>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-5 xl:grid-cols-6">{homeSeries.slice(0, 6).map(renderCard)}</div>
              </div>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-5 xl:grid-cols-6">{items.map(renderCard)}</div>
        )}

        {!loading && hasMore && !searchQuery.trim() && (
          <div className="flex items-center justify-center gap-3 pt-3">
            <button type="button" onClick={() => setPage((current) => Math.max(1, current - 1))} disabled={page === 1} className="grid h-11 w-11 place-items-center rounded-xl border border-slate-200 bg-white text-slate-600 disabled:opacity-40"><ChevronRight size={18} /></button>
            <span className="rounded-xl bg-white px-4 py-3 text-xs font-bold text-slate-600 ring-1 ring-slate-200">صفحة {page}</span>
            <button type="button" onClick={() => setPage((current) => current + 1)} className="grid h-11 w-11 place-items-center rounded-xl border border-slate-200 bg-white text-slate-600 hover:text-indigo-700"><ChevronLeft size={18} /></button>
          </div>
        )}
      </section>
    </div>
  );
}
