"use client";

import React from "react";
import {
  Home,
  Film,
  Tv,
  Sparkles,
  Radio,
  Search,
  Heart,
  Crown,
} from "lucide-react";

interface NavbarProps {
  activeTab: "home" | "movies" | "series" | "anime" | "matches" | "favorites";
  onTabChange: (tab: "home" | "movies" | "series" | "anime" | "matches" | "favorites") => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  liveMatchesCount?: number;
}

export default function Navbar({
  activeTab,
  onTabChange,
  searchQuery,
  onSearchChange,
  liveMatchesCount = 0,
}: NavbarProps) {
  return (
    <header className="sticky top-0 z-40 w-full bg-[#080A10]/95 backdrop-blur-xl border-b border-[#1D273B] shadow-2xl">
      <div className="w-full px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between gap-4">
        {/* Right Section: Logo + Nav Links */}
        <div className="flex items-center gap-6 overflow-x-auto no-scrollbar">
          {/* Official Mashhad Logo */}
          <div
            onClick={() => onTabChange("home")}
            className="flex items-center gap-3 cursor-pointer flex-shrink-0 group"
          >
            <div className="w-12 h-12 rounded-2xl overflow-hidden border-2 border-[#E5B842]/70 shadow-[0_0_15px_rgba(229,184,66,0.3)] flex-shrink-0 bg-black group-hover:scale-105 transition-transform duration-300">
              <img
                src="/logo.jpg"
                alt="مَشْهَد TV"
                className="w-full h-full object-cover"
                onError={(e) => {
                  (e.target as HTMLImageElement).src = "/logo.png";
                }}
              />
            </div>
            <div className="flex flex-col">
              <div className="flex items-center gap-2">
                <span className="text-xl sm:text-2xl font-black text-white tracking-wide">
                  مَـشْـهَـدْ
                </span>
                <span className="px-2 py-0.5 rounded-lg bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black text-[11px] font-black uppercase shadow-gold">
                  TV
                </span>
              </div>
              <span className="text-[10px] text-gray-400 font-semibold tracking-wider">
                سينما حصرية • بث مباريات مباشر
              </span>
            </div>
          </div>

          {/* Navigation Links with Mashhad Distinctive Gold Highlights */}
          <nav className="hidden lg:flex items-center gap-2 text-xs font-bold text-gray-300">
            <button
              onClick={() => onTabChange("home")}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl transition-all ${
                activeTab === "home"
                  ? "bg-[#E5B842] text-black font-black shadow-gold"
                  : "hover:text-white hover:bg-white/5"
              }`}
            >
              <Home size={15} />
              <span>الرئيسية</span>
            </button>

            <button
              onClick={() => onTabChange("movies")}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl transition-all ${
                activeTab === "movies"
                  ? "bg-[#E5B842] text-black font-black shadow-gold"
                  : "hover:text-white hover:bg-white/5"
              }`}
            >
              <Film size={15} />
              <span>أفلام</span>
            </button>

            <button
              onClick={() => onTabChange("series")}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl transition-all ${
                activeTab === "series"
                  ? "bg-[#E5B842] text-black font-black shadow-gold"
                  : "hover:text-white hover:bg-white/5"
              }`}
            >
              <Tv size={15} />
              <span>مسلسلات</span>
            </button>

            <button
              onClick={() => onTabChange("matches")}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl transition-all ${
                activeTab === "matches"
                  ? "bg-gradient-to-r from-red-600 to-red-700 text-white font-black shadow-live"
                  : "hover:text-white hover:bg-white/5"
              }`}
            >
              <Radio size={15} className="text-red-400 animate-pulse" />
              <span>مباريات لايف</span>
              {liveMatchesCount > 0 && (
                <span className="px-1.5 py-0.2 rounded-full bg-red-500 text-white text-[9px] font-black">
                  {liveMatchesCount}
                </span>
              )}
            </button>

            <button
              onClick={() => onTabChange("anime")}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl transition-all ${
                activeTab === "anime"
                  ? "bg-[#E5B842] text-black font-black shadow-gold"
                  : "hover:text-white hover:bg-white/5"
              }`}
            >
              <Sparkles size={15} />
              <span>أنمي وكرتون</span>
            </button>

            <button
              onClick={() => onTabChange("favorites")}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl transition-all ${
                activeTab === "favorites"
                  ? "bg-[#E5B842] text-black font-black shadow-gold"
                  : "hover:text-white hover:bg-white/5"
              }`}
            >
              <Heart size={15} />
              <span>المفضلة</span>
            </button>
          </nav>
        </div>

        {/* Left Section: Search Input + Mashhad VIP Club Button */}
        <div className="flex items-center gap-3">
          {/* Quick Search */}
          <div className="relative hidden md:block w-64 lg:w-72">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder="ابحث عن فيلم، مسلسل، أو قناة..."
              className="w-full bg-[#121824] border border-[#1D273B] text-xs text-white placeholder-gray-400 py-2.5 pr-10 pl-4 rounded-xl focus:outline-none focus:border-[#E5B842] focus:ring-1 focus:ring-[#E5B842] transition"
            />
            <Search size={16} className="absolute right-3.5 top-3 text-gray-400 pointer-events-none" />
          </div>

          {/* Golden VIP Club Button */}
          <button
            onClick={() => alert("مرحباً بك في نادي مَشْهَد VIP! كافة المحتويات متاحة مجاناً بأعلى جودة وبدون إعلانات.")}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-[#E5B842] via-[#F5D061] to-[#C69214] text-black text-xs font-black shadow-gold hover:shadow-goldGlow hover:scale-105 active:scale-95 transition-all flex-shrink-0"
          >
            <Crown size={16} className="fill-black" />
            <span className="hidden sm:inline">نادي مشهد VIP</span>
            <span className="sm:hidden">VIP</span>
          </button>
        </div>
      </div>
    </header>
  );
}
