"use client";

import React, { useState, useEffect } from "react";
import Navbar from "@/components/Navbar";
import CinemaView from "@/components/CinemaView";
import LiveTvView from "@/components/LiveTvView";
import PlayerModal, { StreamSource } from "@/components/PlayerModal";
import { ArabseedItem } from "@/lib/arabseed";
import { Bookmark, Film, Play, Sparkles, Trash2 } from "lucide-react";

export default function HomePage() {
  const [activeTab, setActiveTab] = useState<"cinema" | "livetv" | "favorites">("cinema");
  const [searchQuery, setSearchQuery] = useState("");
  const [favorites, setFavorites] = useState<ArabseedItem[]>([]);
  const [liveMatchesCount, setLiveMatchesCount] = useState(0);

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
    <div className="min-h-screen flex flex-col bg-mashad-bg text-mashad-text selection:bg-mashad-gold selection:text-black">
      {/* Top Navbar */}
      <Navbar
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          setSearchQuery("");
        }}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        liveMatchesCount={liveMatchesCount}
        favoritesCount={favorites.length}
      />

      {/* Main Content Viewport */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 pt-6 sm:pt-8">
        {activeTab === "cinema" ? (
          <CinemaView
            searchQuery={searchQuery}
            onPlayStream={handlePlayStream}
            favorites={favorites}
            onToggleFavorite={handleToggleFavorite}
          />
        ) : activeTab === "livetv" ? (
          <LiveTvView searchQuery={searchQuery} onPlayStream={handlePlayStream} />
        ) : (
          /* Favorites Tab View */
          <div className="space-y-8 animate-fadeIn pb-24">
            <div className="flex items-center justify-between border-b border-mashad-border/60 pb-5">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-mashad-goldDark to-mashad-gold p-0.5 shadow-gold flex items-center justify-center">
                  <div className="w-full h-full bg-mashad-bg rounded-[14px] flex items-center justify-center">
                    <Bookmark size={18} className="text-mashad-gold" />
                  </div>
                </div>
                <div>
                  <h2 className="text-2xl font-black text-white">قائمة المفضلة الخاصة بك</h2>
                  <p className="text-xs text-gray-400">الأفلام والمسلسلات التي قمت بحفظها للمشاهدة لاحقاً</p>
                </div>
              </div>

              {favorites.length > 0 && (
                <button
                  onClick={() => {
                    if (confirm("هل تريد مسح جميع العناصر من المفضلة؟")) {
                      setFavorites([]);
                      localStorage.removeItem("mashad_favorites");
                    }
                  }}
                  className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-400 text-xs font-bold border border-red-500/30 transition"
                >
                  <Trash2 size={14} />
                  <span>مسح الكل</span>
                </button>
              )}
            </div>

            {favorites.length === 0 ? (
              <div className="py-24 flex flex-col items-center justify-center text-center gap-4">
                <div className="w-16 h-16 rounded-3xl bg-mashad-card border border-mashad-border flex items-center justify-center text-gray-500">
                  <Bookmark size={28} />
                </div>
                <h3 className="text-lg font-bold text-white">لا توجد عناصر في المفضلة بعد</h3>
                <p className="text-xs text-gray-400 max-w-sm">
                  اضغط على أيقونة الحفظ ⭐ الموجودة على أي فيلم أو مسلسل في سينما مشهد لحفظه والوصول إليه بسرعة.
                </p>
                <button
                  onClick={() => setActiveTab("cinema")}
                  className="px-6 py-2.5 rounded-xl bg-mashad-gold text-black text-xs font-black shadow-gold hover:scale-105 transition"
                >
                  تصفح السينما الآن
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
                {favorites.map((item) => (
                  <div
                    key={item.id}
                    className="glass-card rounded-2xl overflow-hidden cursor-pointer flex flex-col group relative"
                  >
                    <div className="relative aspect-[2/3] w-full overflow-hidden bg-mashad-card">
                      <img src={item.poster} alt={item.title} className="w-full h-full object-cover" />
                      <button
                        onClick={() => handleToggleFavorite(item)}
                        className="absolute top-2.5 left-2.5 w-8 h-8 rounded-lg bg-red-600/80 hover:bg-red-600 text-white flex items-center justify-center backdrop-blur-md transition shadow-md"
                        title="إزالة من المفضلة"
                      >
                        ✕
                      </button>
                    </div>

                    <div className="p-3 bg-mashad-card/95 flex flex-col justify-between flex-1">
                      <h4 className="text-xs font-bold text-white line-clamp-2 leading-snug">
                        {item.title}
                      </h4>
                      <span className="text-[10px] text-mashad-gold font-bold mt-2">
                        {item.category}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>

      {/* Universal Player Modal */}
      {playerState.isOpen && (
        <PlayerModal
          title={playerState.title}
          subtitle={playerState.subtitle}
          poster={playerState.poster}
          streams={playerState.streams}
          onClose={closePlayer}
        />
      )}

      {/* Cinema-Grade Footer */}
      <footer className="border-t border-mashad-border/40 py-8 mt-16 bg-mashad-surface/60 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-gray-400">
          <div className="flex items-center gap-2">
            <span className="text-mashad-gold font-black">مَشْهَدْ TV</span>
            <span>— تجربة سينمائية ورياضية متكاملة عبر الويب</span>
          </div>
          <p className="text-[11px] text-gray-500">
            سيرفرات فائقة السرعة • دعم الهواتف والشاشات الذكية • بدون إعلانات منبثقة
          </p>
        </div>
      </footer>
    </div>
  );
}
