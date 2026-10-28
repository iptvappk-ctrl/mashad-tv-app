"use client";

import React from "react";
import { Film, Heart, Home, Radio, Search, Sparkles, Tv } from "lucide-react";

type Tab = "home" | "movies" | "series" | "anime" | "matches" | "favorites";

interface NavbarProps {
  activeTab: Tab;
  onTabChange: (tab: Tab) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  liveMatchesCount?: number;
}

const links: { id: Tab; label: string; icon: typeof Home }[] = [
  { id: "home", label: "الرئيسية", icon: Home },
  { id: "movies", label: "أفلام", icon: Film },
  { id: "series", label: "مسلسلات", icon: Tv },
  { id: "anime", label: "أنمي", icon: Sparkles },
  { id: "matches", label: "مباشر", icon: Radio },
  { id: "favorites", label: "قائمتي", icon: Heart },
];

export default function Navbar({
  activeTab,
  onTabChange,
  searchQuery,
  onSearchChange,
  liveMatchesCount = 0,
}: NavbarProps) {
  const search = (
    <label className="relative block w-full">
      <Search size={18} className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-slate-400" />
      <input
        type="search"
        value={searchQuery}
        onChange={(event) => onSearchChange(event.target.value)}
        placeholder="دوّر على فيلم أو مسلسل..."
        aria-label="ابحث عن فيلم أو مسلسل"
        className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 pr-11 pl-4 text-sm font-semibold text-slate-800 outline-none transition placeholder:font-normal placeholder:text-slate-400 focus:border-indigo-300 focus:bg-white focus:ring-4 focus:ring-indigo-100"
      />
    </label>
  );

  return (
    <>
      <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/95 shadow-[0_3px_20px_rgba(27,39,69,0.04)] backdrop-blur-xl">
        <div className="mx-auto flex max-w-[1440px] items-center gap-5 px-4 py-3 sm:px-6 lg:px-8">
          <button
            type="button"
            onClick={() => onTabChange("home")}
            className="flex shrink-0 items-center gap-3 rounded-2xl text-right"
            aria-label="العودة للرئيسية"
          >
            <span className="grid h-11 w-11 place-items-center overflow-hidden rounded-2xl border border-indigo-100 bg-indigo-50 shadow-sm">
              <img
                src="/logo.jpg"
                alt=""
                className="h-full w-full object-cover"
                onError={(event) => {
                  event.currentTarget.src = "/logo.png";
                }}
              />
            </span>
            <span className="hidden sm:block">
              <span className="block text-lg font-black leading-tight tracking-tight text-slate-900">مَشْهَد</span>
              <span className="block text-[10px] font-semibold text-slate-500">اختار حكايتك</span>
            </span>
          </button>

          <nav className="hidden items-center gap-1 lg:flex" aria-label="التنقل الرئيسي">
            {links.map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                type="button"
                onClick={() => onTabChange(id)}
                className={`relative inline-flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-xs font-bold transition ${
                  activeTab === id
                    ? "bg-indigo-50 text-indigo-700"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                }`}
              >
                <Icon size={16} />
                {label}
                {id === "matches" && liveMatchesCount > 0 && (
                  <span className="min-w-5 rounded-full bg-rose-500 px-1.5 py-0.5 text-center text-[10px] font-black text-white">
                    {liveMatchesCount}
                  </span>
                )}
              </button>
            ))}
          </nav>

          <div className="hidden min-w-48 flex-1 md:block lg:max-w-sm">{search}</div>
          <span className="mr-auto hidden items-center gap-2 rounded-full bg-emerald-50 px-3 py-2 text-[11px] font-bold text-emerald-700 sm:inline-flex">
            <span className="h-2 w-2 rounded-full bg-emerald-500" />
            مكتبة بتتجدد
          </span>
        </div>
        <div className="border-t border-slate-100 px-4 py-2.5 md:hidden">{search}</div>
      </header>

      <nav
        aria-label="التنقل للموبايل"
        className="fixed inset-x-0 bottom-0 z-40 grid grid-cols-6 border-t border-slate-200 bg-white/95 px-1 pb-[max(6px,env(safe-area-inset-bottom))] pt-2 shadow-[0_-8px_28px_rgba(31,43,72,0.08)] backdrop-blur-xl lg:hidden"
      >
        {links.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            type="button"
            onClick={() => onTabChange(id)}
            className={`relative flex min-w-0 flex-col items-center gap-1 rounded-xl py-1.5 text-[10px] font-bold ${
              activeTab === id ? "text-indigo-700" : "text-slate-500"
            }`}
          >
            <span className={`grid h-8 w-10 place-items-center rounded-xl ${activeTab === id ? "bg-indigo-50" : ""}`}>
              <Icon size={18} />
            </span>
            <span>{label}</span>
            {id === "matches" && liveMatchesCount > 0 && (
              <span className="absolute right-1/2 top-0 mr-1.5 h-2 w-2 rounded-full bg-rose-500" />
            )}
          </button>
        ))}
      </nav>
    </>
  );
}
