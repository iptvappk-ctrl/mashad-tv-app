"use client";

import React from "react";
import {
  Search,
  Home,
  Film,
  Tv,
  Sparkles,
  Radio,
  Heart,
  ChevronLeft,
  Calendar,
  Layers,
  Globe,
  SlidersHorizontal,
  ShieldCheck,
  Flame,
  Clapperboard,
} from "lucide-react";

interface SidebarProps {
  activeTab: "home" | "movies" | "series" | "anime" | "matches" | "favorites";
  onTabChange: (tab: "home" | "movies" | "series" | "anime" | "matches" | "favorites") => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  onSelectGenre?: (genre: string) => void;
  selectedGenre?: string;
}

export default function Sidebar({
  activeTab,
  onTabChange,
  searchQuery,
  onSearchChange,
  onSelectGenre,
  selectedGenre,
}: SidebarProps) {
  return (
    <aside className="w-full lg:w-72 bg-[#12131D] border border-amber-500/20 p-4 sm:p-5 rounded-3xl flex flex-col gap-6 flex-shrink-0 select-none shadow-2xl">
      {/* ── Search Input Box ────────────────────────────────────────── */}
      <div className="relative">
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="ابحث في مكتبة زقزوقة..."
          className="w-full bg-[#161726] border border-amber-500/20 text-xs text-white placeholder-slate-400 py-3 pr-10 pl-4 rounded-2xl focus:outline-none focus:border-amber-400 focus:ring-1 focus:ring-amber-400 transition"
        />
        <Search size={16} className="absolute right-3.5 top-3.5 text-amber-400/60 pointer-events-none" />
      </div>

      {/* ── Primary Navigation Links ─────────────────────────────────── */}
      <nav className="flex flex-col gap-1.5">
        <button
          onClick={() => onTabChange("home")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition-all ${
            activeTab === "home"
              ? "bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black shadow-gold font-black scale-[1.02]"
              : "text-gray-300 hover:bg-[#161E2E] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Home size={18} className={activeTab === "home" ? "text-black" : "text-[#E5B842]"} />
            <span>الصفحة الرئيسية</span>
          </div>
          <ChevronLeft size={14} className={activeTab === "home" ? "text-black" : "text-gray-500"} />
        </button>

        <button
          onClick={() => onTabChange("movies")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition-all ${
            activeTab === "movies"
              ? "bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black shadow-gold font-black scale-[1.02]"
              : "text-gray-300 hover:bg-[#161E2E] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Film size={18} className={activeTab === "movies" ? "text-black" : "text-[#3B82F6]"} />
            <span>أفلام حصرية</span>
          </div>
          <ChevronLeft size={14} className={activeTab === "movies" ? "text-black" : "text-gray-500"} />
        </button>

        <button
          onClick={() => onTabChange("series")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition-all ${
            activeTab === "series"
              ? "bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black shadow-gold font-black scale-[1.02]"
              : "text-gray-300 hover:bg-[#161E2E] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Tv size={18} className={activeTab === "series" ? "text-black" : "text-[#8B5CF6]"} />
            <span>مسلسلات كاملة</span>
          </div>
          <ChevronLeft size={14} className={activeTab === "series" ? "text-black" : "text-gray-500"} />
        </button>

        <button
          onClick={() => onTabChange("matches")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition-all ${
            activeTab === "matches"
              ? "bg-gradient-to-r from-red-600 to-red-700 text-white shadow-live font-black scale-[1.02]"
              : "text-gray-300 hover:bg-[#161E2E] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Radio size={18} className="text-red-400 animate-pulse" />
            <span>بثوث المباريات والقنوات</span>
          </div>
          <span className="px-2 py-0.5 rounded-full bg-red-600/30 text-red-300 text-[10px] font-black border border-red-500/30">
            LIVE 🔴
          </span>
        </button>

        <button
          onClick={() => onTabChange("anime")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition-all ${
            activeTab === "anime"
              ? "bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black shadow-gold font-black scale-[1.02]"
              : "text-gray-300 hover:bg-[#161E2E] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Sparkles size={18} className={activeTab === "anime" ? "text-black" : "text-[#F97316]"} />
            <span>أنمي وكرتون</span>
          </div>
          <ChevronLeft size={14} className={activeTab === "anime" ? "text-black" : "text-gray-500"} />
        </button>

        <button
          onClick={() => onTabChange("favorites")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition-all ${
            activeTab === "favorites"
              ? "bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black shadow-gold font-black scale-[1.02]"
              : "text-gray-300 hover:bg-[#161E2E] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Heart size={18} className={activeTab === "favorites" ? "text-black fill-black" : "text-[#EF4444]"} />
            <span>المشاهدة لاحقاً (المفضلة)</span>
          </div>
          <ChevronLeft size={14} className={activeTab === "favorites" ? "text-black" : "text-gray-500"} />
        </button>
      </nav>

      {/* ── Filters & Customization Section ──────────────────────────── */}
      <div className="border-t border-[#1D273B] pt-5 flex flex-col gap-3">
        <div className="flex items-center gap-2 text-xs font-black text-gray-300 px-1">
          <SlidersHorizontal size={15} className="text-[#E5B842]" />
          <span>الفلاتر والتخصيص الذكي</span>
        </div>

        {/* Family Safe Mode Button */}
        <button
          onClick={() => alert("الرقابة العائلية مفعلة: المحتوى مفحوص ونظيف بنسبة 100%.")}
          className="w-full flex items-center justify-between px-4 py-3.5 rounded-2xl bg-gradient-to-r from-[#059669] to-[#10B981] text-white font-black text-xs shadow-md hover:opacity-95 transition"
        >
          <div className="flex items-center gap-2.5">
            <ShieldCheck size={18} />
            <div className="text-right">
              <span className="block text-[10px] text-green-100 font-medium">الرقابة العائلية</span>
              <span className="text-xs font-black">محتوى نظيف وآمن</span>
            </div>
          </div>
          <span className="text-xs font-bold">✓ مفعل</span>
        </button>

        {/* Dynamic Category & Filter Buttons */}
        <div className="flex flex-col gap-2 text-xs font-bold">
          <button
            onClick={() => onSelectGenre?.("foreign_movies")}
            className={`w-full flex items-center justify-between p-3 rounded-2xl border transition-all ${
              selectedGenre === "foreign_movies"
                ? "bg-[#E5B842] border-[#E5B842] text-black font-black shadow-gold scale-[1.02]"
                : "bg-[#121824] border-[#1D273B] text-gray-300 hover:border-[#E5B842]/50 hover:bg-[#161E2E]"
            }`}
          >
            <div className="flex items-center gap-2.5">
              <Film size={16} className={selectedGenre === "foreign_movies" ? "text-black" : "text-[#3B82F6]"} />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">سينما عالمية</span>
                <span>أفلام أجنبية مترجمة</span>
              </div>
            </div>
            <ChevronLeft size={14} className={selectedGenre === "foreign_movies" ? "text-black" : "text-gray-500"} />
          </button>

          <button
            onClick={() => onSelectGenre?.("arabic_movies")}
            className={`w-full flex items-center justify-between p-3 rounded-2xl border transition-all ${
              selectedGenre === "arabic_movies"
                ? "bg-[#E5B842] border-[#E5B842] text-black font-black shadow-gold scale-[1.02]"
                : "bg-[#121824] border-[#1D273B] text-gray-300 hover:border-[#E5B842]/50 hover:bg-[#161E2E]"
            }`}
          >
            <div className="flex items-center gap-2.5">
              <Clapperboard size={16} className={selectedGenre === "arabic_movies" ? "text-black" : "text-[#10B981]"} />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">السينما العربية</span>
                <span>أفلام عربية ومصرية</span>
              </div>
            </div>
            <ChevronLeft size={14} className={selectedGenre === "arabic_movies" ? "text-black" : "text-gray-500"} />
          </button>

          <button
            onClick={() => onSelectGenre?.("ramadan")}
            className={`w-full flex items-center justify-between p-3 rounded-2xl border transition-all ${
              selectedGenre === "ramadan"
                ? "bg-[#E5B842] border-[#E5B842] text-black font-black shadow-gold scale-[1.02]"
                : "bg-[#121824] border-[#1D273B] text-gray-300 hover:border-[#E5B842]/50 hover:bg-[#161E2E]"
            }`}
          >
            <div className="flex items-center gap-2.5">
              <Sparkles size={16} className={selectedGenre === "ramadan" ? "text-black" : "text-[#E5B842]"} />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">موسم دراما</span>
                <span>مسلسلات رمضان 2026</span>
              </div>
            </div>
            <ChevronLeft size={14} className={selectedGenre === "ramadan" ? "text-black" : "text-gray-500"} />
          </button>

          <button
            onClick={() => onSelectGenre?.("turkish_series")}
            className={`w-full flex items-center justify-between p-3 rounded-2xl border transition-all ${
              selectedGenre === "turkish_series"
                ? "bg-[#E5B842] border-[#E5B842] text-black font-black shadow-gold scale-[1.02]"
                : "bg-[#121824] border-[#1D273B] text-gray-300 hover:border-[#E5B842]/50 hover:bg-[#161E2E]"
            }`}
          >
            <div className="flex items-center gap-2.5">
              <Tv size={16} className={selectedGenre === "turkish_series" ? "text-black" : "text-[#8B5CF6]"} />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">الدراما التركية</span>
                <span>مسلسلات تركية مدبلجة</span>
              </div>
            </div>
            <ChevronLeft size={14} className={selectedGenre === "turkish_series" ? "text-black" : "text-gray-500"} />
          </button>

          <button
            onClick={() => onSelectGenre?.("arabic_series")}
            className={`w-full flex items-center justify-between p-3 rounded-2xl border transition-all ${
              selectedGenre === "arabic_series"
                ? "bg-[#E5B842] border-[#E5B842] text-black font-black shadow-gold scale-[1.02]"
                : "bg-[#121824] border-[#1D273B] text-gray-300 hover:border-[#E5B842]/50 hover:bg-[#161E2E]"
            }`}
          >
            <div className="flex items-center gap-2.5">
              <Tv size={16} className={selectedGenre === "arabic_series" ? "text-black" : "text-[#EC4899]"} />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">الدراما العربية</span>
                <span>مسلسلات عربية ومصرية</span>
              </div>
            </div>
            <ChevronLeft size={14} className={selectedGenre === "arabic_series" ? "text-black" : "text-gray-500"} />
          </button>

          <button
            onClick={() => onSelectGenre?.("action")}
            className={`w-full flex items-center justify-between p-3 rounded-2xl border transition-all ${
              selectedGenre === "action"
                ? "bg-[#E5B842] border-[#E5B842] text-black font-black shadow-gold scale-[1.02]"
                : "bg-[#121824] border-[#1D273B] text-gray-300 hover:border-[#E5B842]/50 hover:bg-[#161E2E]"
            }`}
          >
            <div className="flex items-center gap-2.5">
              <Flame size={16} className={selectedGenre === "action" ? "text-black" : "text-[#F97316]"} />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">تصنيف الأكشن</span>
                <span>إثارة وأكشن وتشويق</span>
              </div>
            </div>
            <ChevronLeft size={14} className={selectedGenre === "action" ? "text-black" : "text-gray-500"} />
          </button>

          <button
            onClick={() => onSelectGenre?.("horror")}
            className={`w-full flex items-center justify-between p-3 rounded-2xl border transition-all ${
              selectedGenre === "horror"
                ? "bg-[#E5B842] border-[#E5B842] text-black font-black shadow-gold scale-[1.02]"
                : "bg-[#121824] border-[#1D273B] text-gray-300 hover:border-[#E5B842]/50 hover:bg-[#161E2E]"
            }`}
          >
            <div className="flex items-center gap-2.5">
              <Clapperboard size={16} className={selectedGenre === "horror" ? "text-black" : "text-[#EF4444]"} />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">تصنيف الرعب</span>
                <span>أفلام رعب وغموض</span>
              </div>
            </div>
            <ChevronLeft size={14} className={selectedGenre === "horror" ? "text-black" : "text-gray-500"} />
          </button>

          <button
            onClick={() => onSelectGenre?.("wrestling")}
            className={`w-full flex items-center justify-between p-3 rounded-2xl border transition-all ${
              selectedGenre === "wrestling"
                ? "bg-[#E5B842] border-[#E5B842] text-black font-black shadow-gold scale-[1.02]"
                : "bg-[#121824] border-[#1D273B] text-gray-300 hover:border-[#E5B842]/50 hover:bg-[#161E2E]"
            }`}
          >
            <div className="flex items-center gap-2.5">
              <Flame size={16} className={selectedGenre === "wrestling" ? "text-black" : "text-[#EAB308]"} />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">عروض المصارعة</span>
                <span>WWE Raw & SmackDown</span>
              </div>
            </div>
            <ChevronLeft size={14} className={selectedGenre === "wrestling" ? "text-black" : "text-gray-500"} />
          </button>
        </div>
      </div>
    </aside>
  );
}
