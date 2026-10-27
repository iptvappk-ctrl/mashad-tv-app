"use client";

import React, { useState, useEffect } from "react";
import Navbar from "@/components/Navbar";
import Sidebar from "@/components/Sidebar";
import CinemaView from "@/components/CinemaView";
import DetailView from "@/components/DetailView";
import LiveTvView from "@/components/LiveTvView";
import PlayerModal, { StreamSource } from "@/components/PlayerModal";
import { ArabseedItem, ArabseedDetail } from "@/lib/arabseed";
import { Bookmark, Film, Play, Star, Trash2 } from "lucide-react";

export default function HomePage() {
  const [activeTab, setActiveTab] = useState<
    "home" | "movies" | "series" | "anime" | "matches" | "favorites"
  >("home");
  const [selectedGenre, setSelectedGenre] = useState<string | undefined>(undefined);
  const [searchQuery, setSearchQuery] = useState("");
  const [favorites, setFavorites] = useState<ArabseedItem[]>([]);
  const [liveMatchesCount, setLiveMatchesCount] = useState(0);

  // Detail View State (Full Page in Main Area - Wecima Style)
  const [selectedDetailItem, setSelectedDetailItem] = useState<ArabseedItem | null>(null);
  const [selectedDetailData, setSelectedDetailData] = useState<ArabseedDetail | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [resolvingServer, setResolvingServer] = useState<string | null>(null);
  const [recommendations, setRecommendations] = useState<ArabseedItem[]>([]);

  // Global Player State
  const [playerState, setPlayerState] = useState<{
    isOpen: boolean;
    title: string;
    subtitle?: string;
    poster?: string;
    streams: StreamSource[];
  }>({
    isOpen: false,
    title: "",
    streams: [],
  });

  // Load Favorites from LocalStorage
  useEffect(() => {
    try {
      const saved = localStorage.getItem("mashad_favorites");
      if (saved) {
        setFavorites(JSON.parse(saved));
      }
    } catch {}
  }, []);

  // Save Favorites to LocalStorage
  const handleToggleFavorite = (item: ArabseedItem) => {
    setFavorites((prev) => {
      let updated: ArabseedItem[];
      if (prev.some((f) => f.id === item.id)) {
        updated = prev.filter((f) => f.id !== item.id);
      } else {
        updated = [item, ...prev];
      }
      try {
        localStorage.setItem("mashad_favorites", JSON.stringify(updated));
      } catch {}
      return updated;
    });
  };

  // Pre-load recommendations from movies
  useEffect(() => {
    async function fetchRecs() {
      try {
        const res = await fetch("/api/cinema/movies?page=1");
        const data = await res.json();
        if (data.success && Array.isArray(data.data)) {
          setRecommendations(data.data.slice(0, 10));
        }
      } catch {}
    }
    fetchRecs();
  }, []);

  // Poll Live Matches Count for Navbar Badge
  useEffect(() => {
    async function fetchMatchesCount() {
      try {
        const res = await fetch("/api/football/matches");
        const data = await res.json();
        if (data.success && Array.isArray(data.data)) {
          const live = data.data.filter((m: any) => m.status === "in").length;
          setLiveMatchesCount(live);
        }
      } catch {}
    }

    fetchMatchesCount();
    const interval = setInterval(fetchMatchesCount, 45000);
    return () => clearInterval(interval);
  }, []);

  // Handle selecting an item to open Wecima Detail View
  const handleSelectItem = async (item: ArabseedItem) => {
    setSelectedDetailItem(item);
    setSelectedDetailData(null);
    setLoadingDetail(true);
    window.scrollTo({ top: 0, behavior: "smooth" });

    try {
      const res = await fetch(`/api/cinema/detail?url=${encodeURIComponent(item.pageUrl)}`);
      const data = await res.json();
      if (data.success && data.data) {
        setSelectedDetailData(data.data);
      }
    } catch (err) {
      console.error("Failed to load item detail:", err);
    } finally {
      setLoadingDetail(false);
    }
  };

  // Handle Selecting an Episode inside DetailView
  const handleSelectEpisode = async (epUrl: string, epTitle: string) => {
    if (!selectedDetailItem) return;
    setLoadingDetail(true);
    window.scrollTo({ top: 0, behavior: "smooth" });

    try {
      const res = await fetch(`/api/cinema/detail?url=${encodeURIComponent(epUrl)}`);
      const data = await res.json();
      if (data.success && data.data) {
        setSelectedDetailData(data.data);
        setSelectedDetailItem((prev) =>
          prev ? { ...prev, title: epTitle, pageUrl: epUrl } : null
        );
      }
    } catch (err) {
      console.error("Failed to load episode detail:", err);
    } finally {
      setLoadingDetail(false);
    }
  };

  // Handle Playing a server from DetailView
  const handlePlayServer = async (serverUrl: string, serverName: string) => {
    setResolvingServer(serverUrl);
    try {
      if (
        serverUrl.includes("/api/proxy/stream") ||
        serverUrl.endsWith(".m3u8") ||
        serverUrl.endsWith(".mp4")
      ) {
        handlePlayStream({
          title: selectedDetailItem?.title || "مشاهدة العرض",
          subtitle: serverName,
          poster: selectedDetailItem?.poster,
          streams: [{ name: serverName, url: serverUrl }],
        });
        return;
      }

      const res = await fetch(`/api/cinema/resolve?serverUrl=${encodeURIComponent(serverUrl)}`);
      const data = await res.json();
      const playUrl = data.playUrl || data.data?.playUrl;

      if (data.success && playUrl) {
        handlePlayStream({
          title: selectedDetailItem?.title || "مشاهدة العرض",
          subtitle: serverName,
          poster: selectedDetailItem?.poster,
          streams: [{ name: serverName, url: playUrl }],
        });
      } else if (selectedDetailData?.proxiedStream) {
        handlePlayStream({
          title: selectedDetailItem?.title || "مشاهدة العرض",
          subtitle: "سيرفر بديل مباشر",
          poster: selectedDetailItem?.poster,
          streams: [{ name: "سيرفر بديل مباشر", url: selectedDetailData.proxiedStream }],
        });
      } else {
        alert("تعذر تشغيل هذا السيرفر حالياً. يرجى تجربة سيرفر آخر.");
      }
    } catch {
      alert("حدث خطأ في تشغيل السيرفر.");
    } finally {
      setResolvingServer(null);
    }
  };

  const handlePlayStream = (data: {
    title: string;
    subtitle?: string;
    poster?: string;
    streams: StreamSource[];
  }) => {
    setPlayerState({
      isOpen: true,
      title: data.title,
      subtitle: data.subtitle,
      poster: data.poster,
      streams: data.streams,
    });
  };

  const closePlayer = () => {
    setPlayerState((prev) => ({ ...prev, isOpen: false }));
  };

  return (
    <div
      dir="rtl"
      className="min-h-screen flex flex-col bg-[#07090E] text-[#F3F4F6] selection:bg-[#E5B842] selection:text-black font-sans"
    >
      {/* ── Top Navbar ────────────────────────────────────────────── */}
      <Navbar
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          setSelectedDetailItem(null);
          setSelectedDetailData(null);
          setSelectedGenre(undefined);
          setSearchQuery("");
        }}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        liveMatchesCount={liveMatchesCount}
      />

      {/* ── Main Two-Column Wecima Layout ─────────────────────────── */}
      <div className="flex-1 w-full max-w-[1920px] mx-auto flex flex-col lg:flex-row gap-6 p-4 sm:p-6">
        {/* Main Content Area (Takes left in RTL = lg:order-2) */}
        <main className="order-1 lg:order-2 flex-1 min-w-0">
          {selectedDetailItem ? (
            <DetailView
              item={selectedDetailItem}
              detail={selectedDetailData}
              loading={loadingDetail}
              onBack={() => {
                setSelectedDetailItem(null);
                setSelectedDetailData(null);
              }}
              onPlayServer={handlePlayServer}
              resolvingServer={resolvingServer}
              isFavorite={favorites.some((f) => f.id === selectedDetailItem.id)}
              onToggleFavorite={() => handleToggleFavorite(selectedDetailItem)}
              onSelectEpisode={handleSelectEpisode}
              recommendations={recommendations}
              onSelectRecommendation={(rec) => {
                handleSelectItem(rec);
              }}
            />
          ) : activeTab === "matches" ? (
            <LiveTvView searchQuery={searchQuery} onPlayStream={handlePlayStream} />
          ) : activeTab === "favorites" ? (
            /* ── Favorites Grid ───────────────────────────────────── */
            <div className="space-y-6">
              <div className="flex items-center justify-between border-b border-[#1E293B] pb-3">
                <div className="flex items-center gap-2.5">
                  <div className="w-2.5 h-6 rounded-full bg-[#EF4444]" />
                  <h2 className="text-xl font-black text-white">
                    قائمة المشاهدة لاحقاً ({favorites.length})
                  </h2>
                </div>

                {favorites.length > 0 && (
                  <button
                    onClick={() => {
                      if (confirm("هل تريد إفراغ قائمة المفضلة بالكامل؟")) {
                        setFavorites([]);
                        localStorage.removeItem("mashad_favorites");
                      }
                    }}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-red-600/15 text-red-400 hover:bg-red-600 hover:text-white text-xs font-bold transition"
                  >
                    <Trash2 size={14} />
                    <span>مسح الكل</span>
                  </button>
                )}
              </div>

              {favorites.length === 0 ? (
                <div className="bg-[#121A28] border border-[#1E293B] rounded-3xl p-12 text-center space-y-4">
                  <Bookmark size={48} className="mx-auto text-gray-500" />
                  <h3 className="text-lg font-black text-white">لا توجد أعمال في المفضلة بعد</h3>
                  <p className="text-xs text-gray-400">
                    اضغط على أيقونة القلب على أي فيلم أو مسلسل لحفظه هنا والرجوع إليه لاحقاً.
                  </p>
                  <button
                    onClick={() => setActiveTab("home")}
                    className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black text-xs font-black hover:shadow-gold transition"
                  >
                    تصفح الأعمال الآن
                  </button>
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 xl:grid-cols-5 gap-4 sm:gap-5">
                  {favorites.map((item) => (
                    <div
                      key={item.id}
                      onClick={() => handleSelectItem(item)}
                      className="group relative bg-[#0D111A] rounded-2xl overflow-hidden border border-[#1D273B] hover:border-[#E5B842]/80 cursor-pointer shadow-lg hover:shadow-gold transition-all duration-300 transform hover:-translate-y-1.5 flex flex-col select-none aspect-[2/3.1]"
                    >
                      <div className="relative w-full h-full overflow-hidden bg-black/40">
                        <img
                          src={item.poster}
                          alt={item.title}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                          onError={(e) => {
                            (e.target as HTMLImageElement).src = "/logo.jpg";
                          }}
                        />

                        <div className="absolute inset-0 bg-gradient-to-t from-[#0B0F17] via-[#0B0F17]/30 to-transparent" />

                        {/* Top Quality Badge */}
                        <div className="absolute top-2.5 right-2.5 z-10">
                          <span className="px-2.5 py-0.5 rounded-lg bg-black/85 text-yellow-400 text-[10px] font-black border border-yellow-400/40 shadow-md">
                            {item.quality || "WEB-DL"}
                          </span>
                        </div>

                        {/* Top Rating */}
                        <div className="absolute top-2.5 left-2.5 z-10 flex items-center gap-1 px-2 py-0.5 rounded-full bg-black/85 text-white text-[10px] font-black border border-yellow-500/50 shadow-md">
                          <Star size={10} className="fill-yellow-400 text-yellow-400" />
                          <span>{item.rating || "6.8"}</span>
                        </div>

                        {/* Remove from favs */}
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleToggleFavorite(item);
                          }}
                          className="absolute top-9 left-2.5 z-10 w-7 h-7 rounded-full bg-red-600 text-white flex items-center justify-center shadow-md hover:scale-110 transition"
                          title="إزالة من المفضلة"
                        >
                          <Trash2 size={13} />
                        </button>

                        <div className="absolute bottom-0 inset-x-0 p-3 flex flex-col gap-1.5 z-10">
                          <span className="px-2 py-0.5 rounded-md bg-[#2563EB]/80 text-white text-[9px] font-black w-fit">
                            {item.category || (item.isSeries ? "مسلسل" : "فيلم")}
                          </span>
                          <h3 className="text-xs sm:text-sm font-black text-white leading-tight line-clamp-2">
                            {item.title}
                          </h3>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : (
            /* ── Cinema View Grid (Home / Movies / Series / Anime) ─── */
            <CinemaView
              activeTab={activeTab}
              selectedGenre={selectedGenre}
              searchQuery={searchQuery}
              onSelectItem={handleSelectItem}
              onPlayStream={handlePlayStream}
              favorites={favorites}
              onToggleFavorite={handleToggleFavorite}
            />
          )}
        </main>

        {/* Wecima Left/Right Sidebar (Takes right in RTL = lg:order-1) */}
        <div className="order-2 lg:order-1 w-full lg:w-72 flex-shrink-0">
          <Sidebar
            activeTab={activeTab}
            onTabChange={(tab) => {
              setActiveTab(tab);
              setSelectedDetailItem(null);
              setSelectedDetailData(null);
              setSelectedGenre(undefined);
            }}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            selectedGenre={selectedGenre}
            onSelectGenre={(genre) => {
              setSelectedGenre(genre);
              setSelectedDetailItem(null);
              setSelectedDetailData(null);
              if (activeTab === "matches" || activeTab === "favorites") {
                setActiveTab("home");
              }
            }}
          />
        </div>
      </div>

      {/* ── Global Video Player Modal ─────────────────────────────── */}
      {playerState.isOpen && (
        <PlayerModal
          title={playerState.title}
          subtitle={playerState.subtitle}
          poster={playerState.poster}
          streams={playerState.streams}
          onClose={closePlayer}
        />
      )}
    </div>
  );
}
