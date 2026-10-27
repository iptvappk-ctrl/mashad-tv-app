"use client";

import React from "react";
import {
  Home,
  Film,
  Tv,
  Sparkles,
  Flame,
  Radio,
  User,
  Search,
  Bookmark,
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
    <header className="sticky top-0 z-40 w-full bg-[#101725] border-b border-[#1E293B] shadow-lg">
      <div className="w-full px-4 sm:px-6 lg:px-8 h-18 py-3 flex items-center justify-between gap-4">
        {/* Right Section: Logo + Breadcrumbs/Nav Links */}
        <div className="flex items-center gap-6 overflow-x-auto no-scrollbar">
          {/* Logo */}
          <div
            onClick={() => onTabChange("home")}
            className="flex items-center gap-3 cursor-pointer flex-shrink-0 group"
          >
            <div className="w-11 h-11 rounded-xl overflow-hidden border border-[#2563EB]/50 shadow-md flex-shrink-0 bg-black">
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
              <div className="flex items-center gap-1.5">
                <span className="text-xl sm:text-2xl font-black text-white tracking-wider">
                  MASHHAD
                </span>
                <span className="px-1.5 py-0.2 rounded bg-[#2563EB] text-white text-[10px] font-black uppercase">
                  TV
                </span>
              </div>
              <span className="text-[10px] text-gray-400 font-medium">
                مشاهدة افلام ومسلسلات وبث مباشر
              </span>
            </div>
          </div>

          {/* Navigation Links (Wecima Style with > separators) */}
          <nav className="hidden lg:flex items-center gap-3 text-xs font-bold text-gray-300">
            <button
              onClick={() => onTabChange("home")}
              className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg transition ${
                activeTab === "home" ? "text-[#3B82F6] font-black" : "hover:text-white"
              }`}
            >
              <Home size={15} />
              <span>الرئيسية</span>
            </button>
            <span className="text-gray-600">›</span>

            <button
              onClick={() => onTabChange("movies")}
              className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg transition ${
                activeTab === "movies" ? "text-[#3B82F6] font-black" : "hover:text-white"
              }`}
            >
              <span>افلام</span>
            </button>
            <span className="text-gray-600">›</span>

            <button
              onClick={() => onTabChange("series")}
              className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg transition ${
                activeTab === "series" ? "text-[#3B82F6] font-black" : "hover:text-white"
              }`}
            >
              <span>مسلسلات</span>
            </button>
            <span className="text-gray-600">›</span>

            <button
              onClick={() => onTabChange("anime")}
              className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg transition ${
                activeTab === "anime" ? "text-[#3B82F6] font-black" : "hover:text-white"
              }`}
            >
              <span>انمي و كرتون</span>
            </button>
            <span className="text-gray-600">›</span>

            <button
              onClick={() => onTabChange("matches")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl transition ${
                activeTab === "matches"
                  ? "bg-[#2563EB] text-white shadow-md font-black"
                  : "bg-[#1E293B] text-gray-200 hover:bg-[#283850]"
              }`}
            >
              <Radio size={14} className={activeTab === "matches" ? "text-white" : "text-red-500"} />
              <span>بثوث المباريات والقنوات</span>
              {liveMatchesCount > 0 && (
                <span className="px-1.5 py-0.2 rounded-full bg-red-600 text-white text-[9px] font-black animate-pulse">
                  {liveMatchesCount}
                </span>
              )}
            </button>
            <span className="text-gray-600">›</span>

            <button
              onClick={() => onTabChange("home")}
              className="flex items-center gap-1 px-2.5 py-1.5 text-orange-400 hover:text-orange-300 font-bold"
            >
              <Flame size={14} />
              <span>تريند</span>
            </button>
          </nav>
        </div>

        {/* Left Section: Club Button & Favorites */}
        <div className="flex items-center gap-3">
          <button
            onClick={() => onTabChange("favorites")}
            className={`hidden sm:flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold transition border border-[#1E293B] ${
              activeTab === "favorites"
                ? "bg-[#2563EB] text-white"
                : "bg-[#131B2A] text-gray-300 hover:text-white hover:bg-[#1C263A]"
            }`}
          >
            <Bookmark size={15} />
            <span>المفضلة</span>
          </button>

          <button
            onClick={() => alert("مرحباً بك في نادي مَشْهَد TV! جميع السيرفرات والأفلام والمباريات مجانية بالكامل وبدون إعلانات.")}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#1D4ED8] to-[#2563EB] hover:from-[#2563EB] hover:to-[#3B82F6] text-white text-xs font-extrabold shadow-md transition-all active:scale-95"
          >
            <User size={15} />
            <span>دخول نادي مشهد</span>
          </button>
        </div>
      </div>
    </header>
  );
}
