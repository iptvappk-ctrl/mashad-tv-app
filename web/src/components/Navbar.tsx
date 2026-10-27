"use client";

import React, { useState, useEffect, useRef } from "react";
import {
  Film,
  Tv2,
  Search,
  Sparkles,
  Bookmark,
  Radio,
  Maximize2,
  Minimize2,
  X,
  Play,
  Flame,
} from "lucide-react";

interface NavbarProps {
  activeTab: "cinema" | "livetv" | "favorites";
  onTabChange: (tab: "cinema" | "livetv" | "favorites") => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  liveMatchesCount?: number;
  favoritesCount?: number;
}

export default function Navbar({
  activeTab,
  onTabChange,
  searchQuery,
  onSearchChange,
  liveMatchesCount = 0,
  favoritesCount = 0,
}: NavbarProps) {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Keyboard shortcut: Press "/" or "Ctrl+K" to focus search
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.key === "/" || (e.ctrlKey && e.key === "k")) && document.activeElement !== searchInputRef.current) {
        e.preventDefault();
        searchInputRef.current?.focus();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  const toggleBrowserFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
      setIsFullscreen(true);
    } else {
      document.exitFullscreen().catch(() => {});
      setIsFullscreen(false);
    }
  };

  return (
    <>
      <header
        className={`sticky top-0 z-40 w-full transition-all duration-300 ${
          isScrolled
            ? "glass-header shadow-2xl py-2.5"
            : "bg-gradient-to-b from-black/95 via-mashad-bg/90 to-transparent py-4 border-b border-mashad-border/40"
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
          {/* Brand Logo */}
          <div
            className="flex items-center gap-3.5 cursor-pointer group select-none"
            onClick={() => onTabChange("cinema")}
          >
            <div className="relative">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-mashad-goldDark via-mashad-gold to-mashad-goldLight p-0.5 shadow-gold group-hover:scale-105 transition-transform duration-300">
                <div className="w-full h-full bg-mashad-bg rounded-[14px] flex items-center justify-center relative overflow-hidden">
                  <span className="text-2xl font-black text-mashad-gold tracking-tighter gold-glow">
                    م
                  </span>
                  <div className="absolute inset-0 bg-gradient-to-tr from-transparent via-white/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
                </div>
              </div>
              <div className="absolute -top-1 -right-1 w-3 h-3 bg-mashad-gold rounded-full border-2 border-mashad-bg animate-pulse" />
            </div>

            <div>
              <div className="flex items-center gap-2">
                <span className="text-2xl md:text-3xl font-black text-white tracking-tight">
                  مَشْهَدْ
                </span>
                <span className="px-2 py-0.5 rounded-lg bg-gradient-to-r from-mashad-goldDark to-mashad-gold text-black text-[11px] font-black uppercase tracking-widest shadow-gold">
                  TV
                </span>
              </div>
              <p className="text-[11px] text-gray-400 font-medium hidden sm:block">
                السينما الحصرية والبثوث الرياضية الحية
              </p>
            </div>
          </div>

          {/* Center Navigation Tabs (Desktop & Tablet) */}
          <nav className="hidden md:flex items-center bg-mashad-surface/90 p-1.5 rounded-2xl border border-mashad-border/80 shadow-inner backdrop-blur-xl">
            <button
              onClick={() => onTabChange("cinema")}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 ${
                activeTab === "cinema"
                  ? "bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black shadow-gold scale-[1.03]"
                  : "text-gray-400 hover:text-white hover:bg-white/5"
              }`}
            >
              <Film size={16} />
              <span>سينما مشهد</span>
            </button>

            <button
              onClick={() => onTabChange("livetv")}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 relative ${
                activeTab === "livetv"
                  ? "bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black shadow-gold scale-[1.03]"
                  : "text-gray-400 hover:text-white hover:bg-white/5"
              }`}
            >
              <Tv2 size={16} />
              <span>بثات الكورة والقنوات</span>
              {liveMatchesCount > 0 && (
                <span className="flex items-center gap-1 px-1.5 py-0.5 rounded-full bg-red-600 text-white text-[9px] font-black animate-pulse">
                  <span className="w-1.5 h-1.5 rounded-full bg-white" />
                  {liveMatchesCount}
                </span>
              )}
            </button>

            <button
              onClick={() => onTabChange("favorites")}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 ${
                activeTab === "favorites"
                  ? "bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black shadow-gold scale-[1.03]"
                  : "text-gray-400 hover:text-white hover:bg-white/5"
              }`}
            >
              <Bookmark size={15} />
              <span>المفضلة</span>
              {favoritesCount > 0 && (
                <span className="px-1.5 py-0.2 rounded-full bg-mashad-card text-mashad-gold text-[10px] font-bold">
                  {favoritesCount}
                </span>
              )}
            </button>
          </nav>

          {/* Right Action Items */}
          <div className="flex items-center gap-3">
            {/* Search Input Box */}
            <div className="relative flex items-center">
              <Search
                size={17}
                className="absolute right-3.5 text-gray-400 pointer-events-none transition-colors"
              />
              <input
                ref={searchInputRef}
                type="text"
                value={searchQuery}
                onChange={(e) => onSearchChange(e.target.value)}
                placeholder={
                  activeTab === "cinema"
                    ? "ابحث عن فيلم، مسلسل، ممثل... (/)"
                    : "ابحث عن مباراة أو قناة... (/)"
                }
                className="w-44 sm:w-64 md:w-72 pr-10 pl-9 py-2 bg-mashad-card/90 border border-mashad-border text-xs text-white placeholder-gray-500 rounded-xl focus:outline-none focus:border-mashad-gold focus:ring-1 focus:ring-mashad-gold/50 transition-all duration-300 shadow-inner"
              />
              {searchQuery ? (
                <button
                  onClick={() => onSearchChange("")}
                  className="absolute left-3 text-xs text-gray-400 hover:text-white transition"
                  title="مسح البحث"
                >
                  <X size={14} />
                </button>
              ) : (
                <span className="absolute left-3 text-[10px] font-mono text-gray-500 border border-mashad-border px-1.5 py-0.5 rounded hidden sm:inline-block">
                  /
                </span>
              )}
            </div>

            {/* Browser Fullscreen Toggle */}
            <button
              onClick={toggleBrowserFullscreen}
              className="w-10 h-10 rounded-xl bg-mashad-card hover:bg-mashad-elevated border border-mashad-border text-gray-300 hover:text-white flex items-center justify-center transition shadow-sm"
              title={isFullscreen ? "الخروج من الشاشة الكاملة" : "ملء الشاشة"}
            >
              {isFullscreen ? <Minimize2 size={17} /> : <Maximize2 size={17} />}
            </button>
          </div>
        </div>
      </header>

      {/* Mobile Floating Bottom Navigation Bar */}
      <nav className="md:hidden fixed bottom-3 inset-x-3 z-40 bg-mashad-card/95 border border-mashad-border/80 backdrop-blur-2xl rounded-2xl p-1.5 shadow-2xl flex items-center justify-around">
        <button
          onClick={() => onTabChange("cinema")}
          className={`flex-1 flex flex-col items-center py-2 rounded-xl text-[11px] font-bold transition-all ${
            activeTab === "cinema"
              ? "bg-mashad-gold text-black shadow-gold font-black"
              : "text-gray-400 hover:text-white"
          }`}
        >
          <Film size={18} />
          <span className="mt-1">السينما</span>
        </button>

        <button
          onClick={() => onTabChange("livetv")}
          className={`flex-1 flex flex-col items-center py-2 rounded-xl text-[11px] font-bold transition-all relative ${
            activeTab === "livetv"
              ? "bg-mashad-gold text-black shadow-gold font-black"
              : "text-gray-400 hover:text-white"
          }`}
        >
          <Tv2 size={18} />
          <span className="mt-1">المباريات والقنوات</span>
          {liveMatchesCount > 0 && (
            <span className="absolute top-1 right-4 w-2 h-2 rounded-full bg-red-500 live-badge-glow" />
          )}
        </button>

        <button
          onClick={() => onTabChange("favorites")}
          className={`flex-1 flex flex-col items-center py-2 rounded-xl text-[11px] font-bold transition-all ${
            activeTab === "favorites"
              ? "bg-mashad-gold text-black shadow-gold font-black"
              : "text-gray-400 hover:text-white"
          }`}
        >
          <Bookmark size={18} />
          <span className="mt-1">المفضلة</span>
        </button>
      </nav>
    </>
  );
}
