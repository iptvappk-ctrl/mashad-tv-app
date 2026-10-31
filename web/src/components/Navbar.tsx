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
      <Search size={18} className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-amber-400/60" />
      <input
        type="search"
        value={searchQuery}
        onChange={(event) => onSearchChange(event.target.value)}
        placeholder="ابحث عن فيلم، مسلسل، أو ممثل..."
        aria-label="ابحث عن فيلم أو مسلسل"
        className="h-11 w-full rounded-2xl border border-amber-500/20 bg-[#12131D] pr-11 pl-4 text-sm font-semibold text-white outline-none transition placeholder:font-normal placeholder:text-slate-400 focus:border-amber-400 focus:bg-[#161726] focus:ring-2 focus:ring-amber-500/20"
      />
    </label>
  );

  return (
    <>
      <header className="sticky top-0 z-40 border-b border-amber-500/15 bg-[#090A0E]/95 shadow-[0_4px_30px_rgba(0,0,0,0.6)] backdrop-blur-xl">
        <div className="mx-auto flex max-w-[1440px] items-center gap-5 px-4 py-3 sm:px-6 lg:px-8">
          <button
            type="button"
            onClick={() => onTabChange("home")}
            className="group flex shrink-0 items-center gap-3 rounded-2xl text-right transition"
            aria-label="العودة للرئيسية"
          >
            <span className="relative grid h-11 w-11 place-items-center overflow-hidden rounded-2xl border border-amber-500/35 bg-gradient-to-br from-amber-500/20 to-black p-0.5 shadow-[0_0_15px_rgba(212,175,55,0.25)] transition group-hover:scale-105 group-hover:border-amber-400">
              <img
                src="/logo.jpg"
                alt="شعار زقزوقة"
                className="h-full w-full rounded-[14px] object-cover"
                onError={(event) => {
                  event.currentTarget.src = "/logo.png";
                }}
              />
            </span>
            <span className="hidden sm:block">
              <span className="block text-xl font-black leading-tight tracking-tight bg-gradient-to-r from-amber-200 via-amber-400 to-amber-500 bg-clip-text text-transparent">
                زَقْزُوقَة
              </span>
              <span className="block text-[10px] font-bold text-amber-400/70">
                سينما وبث مباشر فائق الجودة
              </span>
            </span>
          </button>

          <nav className="hidden items-center gap-1.5 lg:flex" aria-label="التنقل الرئيسي">
            {links.map(({ id, label, icon: Icon }) => {
              const active = activeTab === id;
              return (
                <button
                  key={id}
                  type="button"
                  onClick={() => onTabChange(id)}
                  className={`relative inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-xs font-bold transition duration-200 ${
                    active
                      ? "border border-amber-500/35 bg-gradient-to-r from-amber-500/20 to-amber-500/10 text-amber-300 shadow-[0_0_15px_rgba(212,175,55,0.15)]"
                      : "text-slate-300 hover:bg-white/5 hover:text-amber-300"
                  }`}
                >
                  <Icon size={16} className={active ? "text-amber-400" : "text-slate-400"} />
                  {label}
                  {id === "matches" && liveMatchesCount > 0 && (
                    <span className="min-w-5 rounded-full bg-red-600 px-1.5 py-0.5 text-center text-[10px] font-black text-white shadow-sm shadow-red-500/50 animate-pulse">
                      {liveMatchesCount}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>

          <div className="hidden min-w-48 flex-1 md:block lg:max-w-sm">{search}</div>

          <span className="mr-auto hidden items-center gap-2 rounded-full border border-amber-500/25 bg-amber-500/10 px-3 py-1.5 text-[11px] font-bold text-amber-400 sm:inline-flex">
            <span className="h-2 w-2 rounded-full bg-emerald-500 animate-ping" />
            <span className="h-2 w-2 rounded-full bg-emerald-500 -mr-4" />
            مكتبة متجددة لحظياً
          </span>
        </div>
        <div className="border-t border-amber-500/10 px-4 py-2.5 md:hidden">{search}</div>
      </header>

      <nav
        aria-label="التنقل للموبايل"
        className="fixed inset-x-0 bottom-0 z-40 grid grid-cols-6 border-t border-amber-500/15 bg-[#090A0E]/95 px-1 pb-[max(6px,env(safe-area-inset-bottom))] pt-2 shadow-[0_-8px_30px_rgba(0,0,0,0.7)] backdrop-blur-xl lg:hidden"
      >
        {links.map(({ id, label, icon: Icon }) => {
          const active = activeTab === id;
          return (
            <button
              key={id}
              type="button"
              onClick={() => onTabChange(id)}
              className={`relative flex min-w-0 flex-col items-center gap-1 rounded-xl py-1.5 text-[10px] font-bold transition ${
                active ? "text-amber-300" : "text-slate-400 hover:text-slate-200"
              }`}
            >
              <span className={`grid h-8 w-10 place-items-center rounded-xl transition ${
                active ? "bg-amber-500/20 border border-amber-500/30 text-amber-400" : ""
              }`}>
                <Icon size={18} />
              </span>
              <span>{label}</span>
              {id === "matches" && liveMatchesCount > 0 && (
                <span className="absolute right-1/2 top-0 mr-1.5 h-2 w-2 rounded-full bg-red-500 animate-ping" />
              )}
            </button>
          );
        })}
      </nav>
    </>
  );
}
