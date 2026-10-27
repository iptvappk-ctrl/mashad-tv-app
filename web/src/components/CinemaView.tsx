"use client";

import React, { useState, useEffect, useRef } from "react";
import {
  Play,
  Star,
  Calendar,
  Info,
  Layers,
  Film,
  Tv,
  Loader2,
  Sparkles,
  Bookmark,
  ChevronLeft,
  ChevronRight,
  Flame,
  Clapperboard,
  Heart,
  TrendingUp,
} from "lucide-react";
import { ArabseedItem, ArabseedDetail } from "@/lib/arabseed";

interface CinemaViewProps {
  searchQuery: string;
  onPlayStream: (streamData: {
    title: string;
    subtitle?: string;
    poster?: string;
    streams: { name: string; url: string }[];
  }) => void;
  favorites: ArabseedItem[];
  onToggleFavorite: (item: ArabseedItem) => void;
}

const GENRE_TAGS = [
  { id: "all", label: "الكل", icon: Sparkles },
  { id: "movies", label: "أفلام كاملة", icon: Film },
  { id: "series", label: "مسلسلات حصرية", icon: Tv },
  { id: "action", label: "أكشن وإثارة", icon: Flame },
  { id: "horror", label: "رعب وتشويق", icon: Clapperboard },
  { id: "anime", label: "أنمي وكرتون", icon: Sparkles },
];

export default function CinemaView({
  searchQuery,
  onPlayStream,
  favorites,
  onToggleFavorite,
}: CinemaViewProps) {
  const [activeCategory, setActiveCategory] = useState("all");
  const [moviesList, setMoviesList] = useState<ArabseedItem[]>([]);
  const [seriesList, setSeriesList] = useState<ArabseedItem[]>([]);
  const [loading, setLoading] = useState(true);

  // Spotlight carousel
  const [currentSlide, setCurrentSlide] = useState(0);
  const carouselTimer = useRef<NodeJS.Timeout | null>(null);

  // Selected item modal
  const [selectedItem, setSelectedItem] = useState<ArabseedItem | null>(null);
  const [itemDetail, setItemDetail] = useState<ArabseedDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [resolvingServer, setResolvingServer] = useState<string | null>(null);

  // Row scroll references
  const moviesRowRef = useRef<HTMLDivElement>(null);
  const seriesRowRef = useRef<HTMLDivElement>(null);

  // Fetch initial movies and series
  useEffect(() => {
    let isCancelled = false;

    async function loadData() {
      setLoading(true);
      try {
        if (searchQuery.trim()) {
          const res = await fetch(`/api/cinema/search?q=${encodeURIComponent(searchQuery.trim())}`);
          const data = await res.json();
          if (!isCancelled && data.success) {
            setMoviesList(data.data || []);
            setSeriesList([]);
          }
        } else {
          const [movRes, serRes] = await Promise.all([
            fetch("/api/cinema/movies?page=1"),
            fetch("/api/cinema/series?page=1"),
          ]);
          const movData = await movRes.json();
          const serData = await serRes.json();

          if (!isCancelled) {
            setMoviesList(movData.success ? movData.data : []);
            setSeriesList(serData.success ? serData.data : []);
          }
        }
      } catch (err) {
        console.error("Failed to load cinema data:", err);
      } finally {
        if (!isCancelled) setLoading(false);
      }
    }

    const debounce = setTimeout(loadData, searchQuery ? 350 : 0);
    return () => {
      isCancelled = true;
      clearTimeout(debounce);
    };
  }, [searchQuery]);

  // Spotlight Auto Carousel Rotation
  const heroItems = moviesList.slice(0, 5);
  useEffect(() => {
    if (heroItems.length === 0 || searchQuery) return;

    if (carouselTimer.current) clearInterval(carouselTimer.current);
    carouselTimer.current = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % heroItems.length);
    }, 6500);

    return () => {
      if (carouselTimer.current) clearInterval(carouselTimer.current);
    };
  }, [heroItems.length, searchQuery]);

  // Scroll Row Helper
  const scrollRow = (ref: React.RefObject<HTMLDivElement | null>, direction: "left" | "right") => {
    if (ref.current) {
      const scrollAmount = direction === "left" ? -450 : 450;
      ref.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
    }
  };

  // Open Details Modal
  const openDetail = async (item: ArabseedItem) => {
    setSelectedItem(item);
    setDetailLoading(true);
    setItemDetail(null);

    try {
      const res = await fetch(`/api/cinema/detail?url=${encodeURIComponent(item.pageUrl)}`);
      const data = await res.json();
      if (data.success) {
        setItemDetail(data.data);
      }
    } catch (err) {
      console.error("Failed to load detail:", err);
    } finally {
      setDetailLoading(false);
    }
  };

  // Play from server
  const handlePlayServer = async (serverUrl: string, serverName: string) => {
    setResolvingServer(serverUrl);
    try {
      const res = await fetch(`/api/cinema/resolve?serverUrl=${encodeURIComponent(serverUrl)}`);
      const data = await res.json();

      if (data.success && data.data?.playUrl) {
        onPlayStream({
          title: selectedItem?.title || "مشاهدة",
          subtitle: serverName,
          poster: selectedItem?.poster,
          streams: [{ name: serverName, url: data.data.playUrl }],
        });
      } else {
        alert("تعذر استخراج البث المباشر من هذا السيرفر، يرجى تجربة سيرفر آخر.");
      }
    } catch {
      alert("حدث خطأ في الاتصال بالسيرفر.");
    } finally {
      setResolvingServer(null);
    }
  };

  const isItemFavorite = (id: string) => favorites.some((f) => f.id === id);

  const currentHero = heroItems[currentSlide] || heroItems[0];

  return (
    <div className="space-y-12 animate-fadeIn pb-24">
      {/* ── 1. Hero Showcase Carousel ─────────────────────────────────── */}
      {!searchQuery && currentHero && (
        <section className="relative w-full h-[400px] sm:h-[480px] lg:h-[560px] rounded-3xl overflow-hidden shadow-2xl border border-mashad-border/80 group">
          {/* Background Backdrop Layer */}
          <div
            className="absolute inset-0 bg-cover bg-center transition-all duration-1000 scale-105 group-hover:scale-100"
            style={{ backgroundImage: `url(${currentHero.poster})` }}
          />

          {/* Luxury Cinematic Gradient Overlays */}
          <div className="absolute inset-0 bg-gradient-to-t from-mashad-bg via-mashad-bg/75 to-transparent" />
          <div className="absolute inset-0 bg-gradient-to-r from-mashad-bg via-mashad-bg/85 to-transparent sm:w-3/4" />
          <div className="absolute inset-0 bg-radial-gradient from-transparent to-black/60" />

          {/* Spotlight Content Box */}
          <div className="absolute bottom-0 right-0 p-6 sm:p-10 lg:p-16 max-w-2xl text-right z-10 flex flex-col items-start gap-4">
            <div className="flex items-center gap-2.5 flex-wrap">
              <span className="flex items-center gap-1.5 px-3.5 py-1 rounded-full bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black text-xs font-black uppercase tracking-wider shadow-gold">
                <Sparkles size={14} />
                أقوى عروض اليوم
              </span>

              {currentHero.quality && (
                <span className="px-2.5 py-1 rounded-lg bg-white/10 text-white text-xs font-bold border border-white/10 backdrop-blur-md">
                  {currentHero.quality}
                </span>
              )}

              <span className="px-2.5 py-1 rounded-lg bg-black/50 text-mashad-gold text-xs font-extrabold border border-mashad-gold/30">
                {currentHero.category || "أفلام حصرية"}
              </span>
            </div>

            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black text-white leading-tight gold-glow drop-shadow-xl line-clamp-2">
              {currentHero.title}
            </h1>

            <p className="text-xs sm:text-sm text-gray-300 leading-relaxed max-w-xl drop-shadow line-clamp-2 font-normal">
              مشاهدة مجانية حصرية بأعلى جودة وصوت نقي مع روابط سيرفرات متعددة وسريعة بدون إعلانات مزعجة.
            </p>

            <div className="flex items-center gap-3 pt-2">
              <button
                onClick={() => openDetail(currentHero)}
                className="flex items-center gap-2 px-6 sm:px-8 py-3.5 rounded-2xl bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black text-sm font-black hover:scale-105 active:scale-95 transition-all shadow-gold"
              >
                <Play size={18} className="fill-black ml-0.5" />
                <span>شاهد الآن</span>
              </button>

              <button
                onClick={() => onToggleFavorite(currentHero)}
                className={`p-3.5 rounded-2xl border transition-all ${
                  isItemFavorite(currentHero.id)
                    ? "bg-mashad-gold/20 border-mashad-gold text-mashad-gold shadow-gold"
                    : "bg-white/10 hover:bg-white/20 border-white/10 text-white backdrop-blur-md"
                }`}
                title="إضافة إلى المفضلة"
              >
                <Bookmark size={18} className={isItemFavorite(currentHero.id) ? "fill-mashad-gold" : ""} />
              </button>

              <button
                onClick={() => openDetail(currentHero)}
                className="flex items-center gap-2 px-5 py-3.5 rounded-2xl bg-white/10 hover:bg-white/20 text-white text-xs sm:text-sm font-bold border border-white/10 backdrop-blur-md transition"
              >
                <Info size={17} />
                <span>السيرفرات والقصة</span>
              </button>
            </div>
          </div>

          {/* Carousel Slide Indicators */}
          <div className="absolute bottom-6 left-6 z-20 flex items-center gap-2">
            {heroItems.map((_, idx) => (
              <button
                key={idx}
                onClick={() => setCurrentSlide(idx)}
                className={`transition-all duration-300 rounded-full ${
                  idx === currentSlide
                    ? "w-8 h-2.5 bg-mashad-gold shadow-gold"
                    : "w-2.5 h-2.5 bg-white/30 hover:bg-white/60"
                }`}
              />
            ))}
          </div>
        </section>
      )}

      {/* ── 2. Genre & Category Filter Pills ─────────────────────────── */}
      <section className="flex items-center justify-between gap-4 flex-wrap">
        <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar max-w-full">
          {GENRE_TAGS.map((tag) => {
            const Icon = tag.icon;
            const isActive = activeCategory === tag.id;
            return (
              <button
                key={tag.id}
                onClick={() => setActiveCategory(tag.id)}
                className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-black whitespace-nowrap transition-all duration-300 ${
                  isActive
                    ? "bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black shadow-gold scale-105"
                    : "bg-mashad-card text-gray-300 hover:bg-mashad-elevated hover:text-white border border-mashad-border"
                }`}
              >
                <Icon size={14} />
                <span>{tag.label}</span>
              </button>
            );
          })}
        </div>

        <span className="text-xs font-bold text-gray-400">
          {searchQuery
            ? `نتائج البحث عن "${searchQuery}" (${moviesList.length})`
            : `إجمالي الأعمال المعروضة (${moviesList.length + seriesList.length})`}
        </span>
      </section>

      {/* ── 3. Content View (Search / Rows / Filtered) ────────────────── */}
      {loading ? (
        /* Shimmer Skeletons */
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
          {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((i) => (
            <div key={i} className="aspect-[2/3] rounded-2xl skeleton-shimmer border border-mashad-border/60" />
          ))}
        </div>
      ) : searchQuery ? (
        /* Search Results Grid */
        <div className="space-y-4">
          <h2 className="text-lg font-black text-white">نتائج البحث</h2>
          {moviesList.length === 0 ? (
            <div className="py-20 text-center text-gray-400">لم يتم العثور على أي عمل يطابق بحثك.</div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
              {moviesList.map((item) => (
                <MovieCard
                  key={item.id}
                  item={item}
                  isFavorite={isItemFavorite(item.id)}
                  onSelect={() => openDetail(item)}
                  onToggleFavorite={() => onToggleFavorite(item)}
                />
              ))}
            </div>
          )}
        </div>
      ) : (
        /* Netflix Style Horizontal Rows */
        <div className="space-y-12">
          {/* Row 1: Movies */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <Film size={20} className="text-mashad-gold" />
                <h2 className="text-xl sm:text-2xl font-black text-white">أحدث الأفلام الأجنبية والعربية</h2>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => scrollRow(moviesRowRef, "right")}
                  className="w-8 h-8 rounded-xl bg-mashad-card hover:bg-mashad-elevated text-white border border-mashad-border flex items-center justify-center transition"
                >
                  <ChevronRight size={18} />
                </button>
                <button
                  onClick={() => scrollRow(moviesRowRef, "left")}
                  className="w-8 h-8 rounded-xl bg-mashad-card hover:bg-mashad-elevated text-white border border-mashad-border flex items-center justify-center transition"
                >
                  <ChevronLeft size={18} />
                </button>
              </div>
            </div>

            <div
              ref={moviesRowRef}
              className="flex items-center gap-4 overflow-x-auto pb-4 no-scrollbar scroll-smooth"
            >
              {moviesList.map((movie) => (
                <div key={movie.id} className="min-w-[160px] sm:min-w-[190px] md:min-w-[210px]">
                  <MovieCard
                    item={movie}
                    isFavorite={isItemFavorite(movie.id)}
                    onSelect={() => openDetail(movie)}
                    onToggleFavorite={() => onToggleFavorite(movie)}
                  />
                </div>
              ))}
            </div>
          </div>

          {/* Row 2: Series */}
          {seriesList.length > 0 && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <Tv size={20} className="text-mashad-gold" />
                  <h2 className="text-xl sm:text-2xl font-black text-white">أحدث المسلسلات والحلقات</h2>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => scrollRow(seriesRowRef, "right")}
                    className="w-8 h-8 rounded-xl bg-mashad-card hover:bg-mashad-elevated text-white border border-mashad-border flex items-center justify-center transition"
                  >
                    <ChevronRight size={18} />
                  </button>
                  <button
                    onClick={() => scrollRow(seriesRowRef, "left")}
                    className="w-8 h-8 rounded-xl bg-mashad-card hover:bg-mashad-elevated text-white border border-mashad-border flex items-center justify-center transition"
                  >
                    <ChevronLeft size={18} />
                  </button>
                </div>
              </div>

              <div
                ref={seriesRowRef}
                className="flex items-center gap-4 overflow-x-auto pb-4 no-scrollbar scroll-smooth"
              >
                {seriesList.map((series) => (
                  <div key={series.id} className="min-w-[160px] sm:min-w-[190px] md:min-w-[210px]">
                    <MovieCard
                      item={series}
                      isFavorite={isItemFavorite(series.id)}
                      onSelect={() => openDetail(series)}
                      onToggleFavorite={() => onToggleFavorite(series)}
                    />
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── 4. Full Detail & Streaming Modal ─────────────────────────── */}
      {selectedItem && (
        <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-xl flex items-center justify-center p-4 animate-fadeIn">
          <div className="relative w-full max-w-4xl bg-mashad-surface border border-mashad-border rounded-3xl overflow-hidden shadow-2xl max-h-[92vh] flex flex-col">
            {/* Modal Close Button */}
            <button
              onClick={() => setSelectedItem(null)}
              className="absolute top-4 left-4 z-30 w-10 h-10 rounded-full bg-black/70 hover:bg-white/20 text-white flex items-center justify-center border border-white/10 transition backdrop-blur-md"
            >
              ✕
            </button>

            {/* Modal Scrollable Body */}
            <div className="overflow-y-auto p-6 sm:p-10 space-y-8">
              {/* Media Header Info */}
              <div className="flex flex-col sm:flex-row gap-6 sm:gap-8 items-start">
                <div className="relative w-36 sm:w-52 rounded-2xl overflow-hidden shadow-2xl border border-mashad-border/80 self-center sm:self-start aspect-[2/3] flex-shrink-0">
                  <img
                    src={selectedItem.poster}
                    alt={selectedItem.title}
                    className="w-full h-full object-cover"
                  />
                  {selectedItem.quality && (
                    <span className="absolute top-2 right-2 px-2 py-0.5 rounded-md bg-black/80 text-mashad-gold text-[10px] font-black border border-mashad-gold/40">
                      {selectedItem.quality}
                    </span>
                  )}
                </div>

                <div className="flex-1 text-right space-y-4">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="px-3 py-1 rounded-full bg-mashad-gold/20 text-mashad-gold text-xs font-extrabold">
                      {selectedItem.category}
                    </span>
                    {selectedItem.year && (
                      <span className="px-3 py-1 rounded-full bg-white/10 text-gray-300 text-xs font-bold">
                        {selectedItem.year}
                      </span>
                    )}
                  </div>

                  <h2 className="text-2xl sm:text-3xl font-black text-white leading-tight">
                    {selectedItem.title}
                  </h2>

                  <p className="text-xs sm:text-sm text-gray-300 leading-relaxed font-normal">
                    {detailLoading
                      ? "جاري فك تشفير البيانات وجلب قصة العمل..."
                      : itemDetail?.overview || "لا يوجد وصف متوفر لهذا العمل."}
                  </p>

                  {/* Direct Fast Play Button (if pre-resolved) */}
                  {itemDetail?.proxiedStream && (
                    <button
                      onClick={() =>
                        onPlayStream({
                          title: selectedItem.title,
                          subtitle: "سيرفر مشهد المباشر",
                          poster: selectedItem.poster,
                          streams: [{ name: "سيرفر مشهد الفائق", url: itemDetail.proxiedStream! }],
                        })
                      }
                      className="flex items-center gap-2 px-7 py-3 rounded-2xl bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black font-black hover:scale-105 active:scale-95 transition-all shadow-gold"
                    >
                      <Play size={18} className="fill-black ml-0.5" />
                      <span>تشغيل فوري بأعلى جودة</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Server List */}
              <div className="border-t border-mashad-border/60 pt-6 space-y-4 text-right">
                <div className="flex items-center justify-between">
                  <h3 className="text-base font-black text-white flex items-center gap-2">
                    <Layers size={18} className="text-mashad-gold" />
                    <span>سيرفرات المشاهدة المتاحة</span>
                  </h3>
                  <span className="text-xs text-gray-400">اضغط على السيرفر لتشغيل البث</span>
                </div>

                {detailLoading ? (
                  <div className="py-8 flex items-center justify-center gap-2.5 text-gray-400 text-sm">
                    <Loader2 size={20} className="animate-spin text-mashad-gold" />
                    <span>جاري استخراج السيرفرات وفك تشفير Mohix...</span>
                  </div>
                ) : itemDetail?.servers && itemDetail.servers.length > 0 ? (
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                    {itemDetail.servers.map((srv, idx) => (
                      <button
                        key={idx}
                        disabled={resolvingServer === srv.embedUrl}
                        onClick={() => handlePlayServer(srv.embedUrl, srv.name)}
                        className="flex items-center justify-between px-4 py-3.5 rounded-2xl bg-mashad-card hover:bg-mashad-elevated border border-mashad-border hover:border-mashad-gold/50 text-xs font-bold text-gray-200 transition-all group disabled:opacity-50"
                      >
                        <Play size={14} className="text-mashad-gold group-hover:scale-110 transition" />
                        <span>{srv.name}</span>
                        {resolvingServer === srv.embedUrl && (
                          <Loader2 size={14} className="animate-spin text-mashad-gold" />
                        )}
                      </button>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-gray-400">لا توجد سيرفرات خارجية متوفرة حالياً.</p>
                )}
              </div>

              {/* Seasons & Episodes (if series) */}
              {itemDetail?.episodes && itemDetail.episodes.length > 0 && (
                <div className="border-t border-mashad-border/60 pt-6 space-y-4 text-right">
                  <div className="flex items-center justify-between">
                    <h3 className="text-base font-black text-white flex items-center gap-2">
                      <Tv size={18} className="text-mashad-gold" />
                      <span>قائمة الحلقات</span>
                    </h3>
                    <span className="text-xs text-mashad-gold font-bold">
                      {itemDetail.episodes.length} حلقة متوفرة
                    </span>
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2.5 max-h-60 overflow-y-auto p-1">
                    {itemDetail.episodes.map((ep, idx) => (
                      <button
                        key={idx}
                        onClick={() =>
                          openDetail({ ...selectedItem, pageUrl: ep.pageUrl, title: ep.title })
                        }
                        className="p-3 rounded-xl bg-mashad-card hover:bg-mashad-gold hover:text-black border border-mashad-border text-center text-xs font-extrabold text-gray-200 transition-all line-clamp-1"
                      >
                        {ep.title}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ── Standalone Movie Card Component ─────────────────────────────────────
function MovieCard({
  item,
  isFavorite,
  onSelect,
  onToggleFavorite,
}: {
  item: ArabseedItem;
  isFavorite: boolean;
  onSelect: () => void;
  onToggleFavorite: () => void;
}) {
  return (
    <div
      onClick={onSelect}
      className="glass-card rounded-2xl overflow-hidden cursor-pointer flex flex-col group relative select-none"
    >
      {/* Poster */}
      <div className="relative aspect-[2/3] w-full overflow-hidden bg-mashad-card">
        <img
          src={item.poster}
          alt={item.title}
          loading="lazy"
          className="w-full h-full object-cover group-hover:scale-108 transition-transform duration-500"
          onError={(e) => {
            (e.target as HTMLElement).style.display = "none";
          }}
        />

        {/* Quality Badge */}
        {item.quality && (
          <span className="absolute top-2.5 right-2.5 px-2 py-0.5 rounded-lg bg-black/80 backdrop-blur-md text-mashad-gold text-[10px] font-black border border-mashad-gold/30">
            {item.quality}
          </span>
        )}

        {/* Favorite Bookmark Button */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            onToggleFavorite();
          }}
          className={`absolute top-2.5 left-2.5 w-8 h-8 rounded-lg flex items-center justify-center transition-all backdrop-blur-md ${
            isFavorite
              ? "bg-mashad-gold text-black shadow-gold scale-105"
              : "bg-black/50 hover:bg-black/80 text-white/70 hover:text-white"
          }`}
        >
          <Bookmark size={15} className={isFavorite ? "fill-black" : ""} />
        </button>

        {/* Hover Center Play Button */}
        <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
          <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-mashad-gold to-mashad-goldLight text-black flex items-center justify-center shadow-gold group-hover:scale-110 transition-transform">
            <Play size={22} className="fill-black ml-0.5" />
          </div>
        </div>
      </div>

      {/* Info */}
      <div className="p-3 flex flex-col justify-between flex-1 bg-mashad-card/95">
        <h3 className="text-xs sm:text-sm font-bold text-white line-clamp-2 leading-snug group-hover:text-mashad-gold transition">
          {item.title}
        </h3>

        <div className="flex items-center justify-between text-[11px] text-gray-400 mt-2 font-medium">
          <span className="line-clamp-1">{item.category}</span>
          {item.year && <span>{item.year}</span>}
        </div>
      </div>
    </div>
  );
}
