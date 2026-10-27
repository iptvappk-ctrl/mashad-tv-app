"use client";

import React from "react";
import {
  Search,
  Home,
  Film,
  Tv,
  Sparkles,
  Swords,
  Radio,
  Heart,
  ChevronLeft,
  Calendar,
  Layers,
  Globe,
  SlidersHorizontal,
  ShieldCheck,
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
    <aside className="w-full lg:w-72 bg-[#0E1522] border-l border-[#1A2536] p-4 flex flex-col gap-6 flex-shrink-0 select-none">
      {/* ── Search Input Box ────────────────────────────────────────── */}
      <div className="relative">
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="ابحث في مَشْهَد TV..."
          className="w-full bg-[#151E2D] border border-[#212E42] text-xs text-white placeholder-gray-400 py-3 pr-10 pl-4 rounded-2xl focus:outline-none focus:border-[#2563EB] focus:ring-1 focus:ring-[#2563EB] transition"
        />
        <Search size={16} className="absolute right-3.5 top-3.5 text-gray-400 pointer-events-none" />
      </div>

      {/* ── Primary Navigation Links ─────────────────────────────────── */}
      <nav className="flex flex-col gap-1.5">
        <button
          onClick={() => onTabChange("home")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition ${
            activeTab === "home"
              ? "bg-[#2563EB] text-white shadow-md font-black"
              : "text-gray-300 hover:bg-[#151E2D] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Home size={18} className="text-[#3B82F6]" />
            <span>الصفحة الرئيسية</span>
          </div>
          <ChevronLeft size={14} className="text-gray-500" />
        </button>

        <button
          onClick={() => onTabChange("movies")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition ${
            activeTab === "movies"
              ? "bg-[#2563EB] text-white shadow-md font-black"
              : "text-gray-300 hover:bg-[#151E2D] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Film size={18} className="text-[#EF4444]" />
            <span>أفلام</span>
          </div>
          <ChevronLeft size={14} className="text-gray-500" />
        </button>

        <button
          onClick={() => onTabChange("series")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition ${
            activeTab === "series"
              ? "bg-[#2563EB] text-white shadow-md font-black"
              : "text-gray-300 hover:bg-[#151E2D] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Tv size={18} className="text-[#8B5CF6]" />
            <span>مسلسلات</span>
          </div>
          <ChevronLeft size={14} className="text-gray-500" />
        </button>

        <button
          onClick={() => onTabChange("matches")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition ${
            activeTab === "matches"
              ? "bg-[#2563EB] text-white shadow-md font-black"
              : "text-gray-300 hover:bg-[#151E2D] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Radio size={18} className="text-[#EF4444] animate-pulse" />
            <span>بثوث المباريات والقنوات</span>
          </div>
          <span className="px-2 py-0.5 rounded-full bg-red-600/30 text-red-400 text-[10px] font-black border border-red-500/30">
            مباشر
          </span>
        </button>

        <button
          onClick={() => onTabChange("anime")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition ${
            activeTab === "anime"
              ? "bg-[#2563EB] text-white shadow-md font-black"
              : "text-gray-300 hover:bg-[#151E2D] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Sparkles size={18} className="text-[#F59E0B]" />
            <span>أنمي و كرتون</span>
          </div>
          <ChevronLeft size={14} className="text-gray-500" />
        </button>

        <button
          onClick={() => onTabChange("favorites")}
          className={`flex items-center justify-between px-4 py-3 rounded-2xl text-xs font-bold transition ${
            activeTab === "favorites"
              ? "bg-[#2563EB] text-white shadow-md font-black"
              : "text-gray-300 hover:bg-[#151E2D] hover:text-white"
          }`}
        >
          <div className="flex items-center gap-3">
            <Heart size={18} className="text-[#EC4899]" />
            <span>المشاهدة لاحقاً (المفضلة)</span>
          </div>
          <ChevronLeft size={14} className="text-gray-500" />
        </button>
      </nav>

      {/* ── Filters & Customization Section (From Wecima Screenshot!) ── */}
      <div className="border-t border-[#1A2536] pt-5 flex flex-col gap-3">
        <div className="flex items-center gap-2 text-xs font-black text-gray-400 px-1">
          <SlidersHorizontal size={14} className="text-[#3B82F6]" />
          <span>الفلاتر و التخصيص</span>
        </div>

        {/* Age Rating Button (Green Badge matching Wecima!) */}
        <button
          onClick={() => alert("الرقابة العائلية مفعلة: المحتوى آمن ونظيف.")}
          className="w-full flex items-center justify-between px-4 py-3.5 rounded-2xl bg-gradient-to-r from-[#059669] to-[#10B981] text-white font-extrabold text-xs shadow-md hover:opacity-95 transition"
        >
          <div className="flex items-center gap-2.5">
            <ShieldCheck size={18} />
            <div className="text-right">
              <span className="block text-[10px] text-green-100">الفئة العمرية</span>
              <span className="text-xs font-black">الرقابة على المشاهد</span>
            </div>
          </div>
          <span className="text-xs">✓</span>
        </button>

        {/* Filter Buttons */}
        <div className="flex flex-col gap-2 text-xs font-bold">
          <button
            onClick={() => onSelectGenre?.("action")}
            className={`w-full flex items-center justify-between p-3.5 rounded-2xl border transition ${
              selectedGenre === "action"
                ? "bg-[#2563EB] border-[#3B82F6] text-white"
                : "bg-[#141C2B] border-[#1E293B] text-gray-300 hover:bg-[#1C263A]"
            }`}
          >
            <div className="flex items-center gap-2.5">
              <Film size={16} className="text-[#3B82F6]" />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">نوع العرض</span>
                <span>إختر نكهتك السينمائية</span>
              </div>
            </div>
            <ChevronLeft size={14} className="text-gray-500" />
          </button>

          <button
            onClick={() => onSelectGenre?.("year")}
            className="w-full flex items-center justify-between p-3.5 rounded-2xl bg-[#141C2B] border border-[#1E293B] text-gray-300 hover:bg-[#1C263A] transition"
          >
            <div className="flex items-center gap-2.5">
              <Calendar size={16} className="text-[#F59E0B]" />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">سنة الإنتاج</span>
                <span>تخصيص سنة الإنتاج</span>
              </div>
            </div>
            <ChevronLeft size={14} className="text-gray-500" />
          </button>

          <button
            onClick={() => onSelectGenre?.("category")}
            className="w-full flex items-center justify-between p-3.5 rounded-2xl bg-[#141C2B] border border-[#1E293B] text-gray-300 hover:bg-[#1C263A] transition"
          >
            <div className="flex items-center gap-2.5">
              <Layers size={16} className="text-[#8B5CF6]" />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">التصنيف</span>
                <span>إختيار تصنيف مخصص</span>
              </div>
            </div>
            <ChevronLeft size={14} className="text-gray-500" />
          </button>

          <button
            onClick={() => onSelectGenre?.("country")}
            className="w-full flex items-center justify-between p-3.5 rounded-2xl bg-[#141C2B] border border-[#1E293B] text-gray-300 hover:bg-[#1C263A] transition"
          >
            <div className="flex items-center gap-2.5">
              <Globe size={16} className="text-[#06B6D4]" />
              <div className="text-right">
                <span className="block text-[10px] text-gray-400">بلد الإنتاج</span>
                <span>عروض من بلد محددة</span>
              </div>
            </div>
            <ChevronLeft size={14} className="text-gray-500" />
          </button>
        </div>
      </div>
    </aside>
  );
}
