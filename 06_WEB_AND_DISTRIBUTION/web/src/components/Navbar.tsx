"use client";

import React, { useState, useEffect } from "react";
import {
  Clapperboard,
  Film,
  Heart,
  Home,
  Radio,
  Search,
  Sparkles,
  Tv,
  Download,
  Smartphone,
  ExternalLink,
  X,
  PlaySquare,
} from "lucide-react";

export type Tab =
  | "home"
  | "movies"
  | "series"
  | "anime"
  | "recaps"
  | "matches"
  | "favorites";

interface NavbarProps {
  activeTab: Tab;
  onTabChange: (tab: Tab) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  liveMatchesCount?: number;
}

const NAV_LINKS: { id: Tab; label: string; icon: typeof Home }[] = [
  { id: "home", label: "الرئيسية", icon: Home },
  { id: "movies", label: "أفلام", icon: Film },
  { id: "series", label: "مسلسلات", icon: Tv },
  { id: "anime", label: "أنمي", icon: Sparkles },
  { id: "recaps", label: "ملخصات", icon: Clapperboard },
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
  const [showDownloadModal, setShowDownloadModal] = useState(false);
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);

  // Keyboard shortcut (Ctrl+K or /) to focus search input
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey && e.key.toLowerCase() === "k") || (e.key === "/" && document.activeElement?.tagName !== "INPUT")) {
        e.preventDefault();
        const input = document.getElementById("global-search-input") as HTMLInputElement | null;
        input?.focus();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  return (
    <>
      <header className="sticky top-0 z-40 border-b border-amber-500/15 bg-[#090A0E]/95 shadow-[0_4px_30px_rgba(0,0,0,0.6)] backdrop-blur-xl">
        <div className="mx-auto flex max-w-[1536px] items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8">
          {/* Logo & Brand */}
          <button
            type="button"
            onClick={() => onTabChange("home")}
            className="group flex shrink-0 items-center gap-3 rounded-2xl text-right transition"
            aria-label="العودة للرئيسية"
          >
            <span className="relative grid h-11 w-11 place-items-center overflow-hidden rounded-2xl border border-amber-500/40 bg-gradient-to-br from-amber-500/20 via-black to-[#151624] p-0.5 shadow-[0_0_20px_rgba(212,175,55,0.3)] transition duration-300 group-hover:scale-105 group-hover:border-amber-400">
              <img
                src="/logo.jpg"
                alt="شعار زقزوقة"
                className="h-full w-full rounded-[14px] object-cover"
                onError={(e) => {
                  e.currentTarget.src = "/logo.png";
                }}
              />
            </span>
            <div className="hidden sm:block">
              <span className="block text-xl font-black leading-tight tracking-tight bg-gradient-to-r from-amber-200 via-amber-400 to-amber-500 bg-clip-text text-transparent">
                زَقْزُوقَة TV
              </span>
              <span className="block text-[10px] font-bold text-amber-400/80">
                سينما وبث مباشر فائق الجودة
              </span>
            </div>
          </button>

          {/* Desktop Navigation Links */}
          <nav className="hidden items-center gap-1 xl:gap-1.5 lg:flex" aria-label="التنقل الرئيسي">
            {NAV_LINKS.map(({ id, label, icon: Icon }) => {
              const active = activeTab === id;
              return (
                <button
                  key={id}
                  type="button"
                  onClick={() => onTabChange(id)}
                  className={`relative inline-flex items-center gap-2 rounded-xl px-3.5 py-2 text-xs font-bold transition duration-200 ${
                    active
                      ? "border border-amber-500/40 bg-gradient-to-r from-amber-500/25 to-amber-500/10 text-amber-300 shadow-[0_0_15px_rgba(212,175,55,0.2)]"
                      : "text-slate-300 hover:bg-white/5 hover:text-amber-300"
                  }`}
                >
                  <Icon size={15} className={active ? "text-amber-400" : "text-slate-400"} />
                  <span>{label}</span>
                  {id === "matches" && liveMatchesCount > 0 && (
                    <span className="flex items-center gap-1 rounded-full bg-red-600 px-1.5 py-0.5 text-[9px] font-black text-white shadow-sm shadow-red-500/50 animate-pulse">
                      <span className="h-1.5 w-1.5 rounded-full bg-white" />
                      {liveMatchesCount} مباشر
                    </span>
                  )}
                </button>
              );
            })}
          </nav>

          {/* Search Bar (PC & Tablet) */}
          <div className="hidden min-w-[220px] max-w-xs flex-1 md:block lg:max-w-sm">
            <label className="relative block w-full">
              <Search size={16} className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-amber-400/70" />
              <input
                id="global-search-input"
                type="search"
                value={searchQuery}
                onChange={(e) => onSearchChange(e.target.value)}
                placeholder="ابحث عن فيلم، مسلسل، أو قناة..."
                className="h-10 w-full rounded-2xl border border-amber-500/20 bg-[#12131D] pr-10 pl-14 text-xs font-semibold text-white outline-none transition placeholder:font-normal placeholder:text-slate-400 focus:border-amber-400 focus:bg-[#161726] focus:ring-2 focus:ring-amber-500/20"
              />
              <kbd className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 rounded border border-white/10 bg-white/5 px-1.5 py-0.5 text-[9px] font-mono text-slate-400">
                Ctrl+K
              </kbd>
            </label>
          </div>

          {/* Right Actions: App Download Pill & Mobile Search Toggle */}
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setMobileSearchOpen(!mobileSearchOpen)}
              className="grid h-10 w-10 place-items-center rounded-2xl border border-amber-500/20 bg-[#12131D] text-amber-400 md:hidden transition hover:bg-[#161726]"
              aria-label="بحث"
            >
              <Search size={17} />
            </button>

            <button
              type="button"
              onClick={() => setShowDownloadModal(true)}
              className="inline-flex items-center gap-2 rounded-2xl border border-amber-500/35 bg-gradient-to-r from-amber-500/20 via-amber-500/10 to-transparent px-3.5 py-2 text-xs font-black text-amber-300 shadow-sm transition duration-200 hover:border-amber-400 hover:scale-[1.02] active:scale-[0.98]"
            >
              <Smartphone size={15} className="text-amber-400 animate-pulse" />
              <span className="hidden sm:inline">حمّل التطبيق</span>
              <span className="sm:hidden">التطبيق</span>
            </button>
          </div>
        </div>

        {/* Mobile Search Input Drawer */}
        {mobileSearchOpen && (
          <div className="border-t border-amber-500/15 bg-[#0D0E15] px-4 py-2.5 md:hidden animate-in slide-in-from-top-2 duration-200">
            <div className="relative flex items-center gap-2">
              <label className="relative flex-1">
                <Search size={16} className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-amber-400" />
                <input
                  type="search"
                  autoFocus
                  value={searchQuery}
                  onChange={(e) => onSearchChange(e.target.value)}
                  placeholder="ابحث عن فيلم، مسلسل، أو قناة..."
                  className="h-10 w-full rounded-xl border border-amber-500/30 bg-[#141524] pr-10 pl-3 text-xs font-semibold text-white outline-none focus:border-amber-400"
                />
              </label>
              <button
                type="button"
                onClick={() => setMobileSearchOpen(false)}
                className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-white/5 text-slate-400 hover:text-white"
              >
                <X size={16} />
              </button>
            </div>
          </div>
        )}
      </header>

      {/* Mobile Bottom Navigation Bar */}
      <nav
        aria-label="التنقل للموبايل"
        className="fixed inset-x-0 bottom-0 z-40 grid grid-cols-7 border-t border-amber-500/15 bg-[#090A0E]/95 px-1 pb-[max(6px,env(safe-area-inset-bottom))] pt-2 shadow-[0_-8px_30px_rgba(0,0,0,0.7)] backdrop-blur-xl lg:hidden"
      >
        {NAV_LINKS.map(({ id, label, icon: Icon }) => {
          const active = activeTab === id;
          return (
            <button
              key={id}
              type="button"
              onClick={() => onTabChange(id)}
              className={`relative flex min-w-0 flex-col items-center gap-1 rounded-xl py-1 text-[10px] font-bold transition ${
                active ? "text-amber-300" : "text-slate-400 hover:text-slate-200"
              }`}
            >
              <span
                className={`grid h-8 w-10 place-items-center rounded-xl transition duration-200 ${
                  active
                    ? "bg-amber-500/20 border border-amber-500/30 text-amber-400 shadow-[0_0_12px_rgba(212,175,55,0.25)]"
                    : ""
                }`}
              >
                <Icon size={17} />
              </span>
              <span className="truncate">{label}</span>
              {id === "matches" && liveMatchesCount > 0 && (
                <span className="absolute right-1/2 top-0 mr-1.5 h-2 w-2 rounded-full bg-red-500 animate-ping" />
              )}
            </button>
          );
        })}
      </nav>

      {/* Download Apps Modal */}
      {showDownloadModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 grid place-items-center bg-black/80 p-4 backdrop-blur-md animate-in fade-in duration-200"
          onClick={() => setShowDownloadModal(false)}
        >
          <div
            dir="rtl"
            className="relative w-full max-w-xl max-h-[90vh] overflow-y-auto rounded-3xl border border-amber-500/30 bg-[#0F101A] p-5 sm:p-7 shadow-[0_0_60px_rgba(212,175,55,0.2)] text-right"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setShowDownloadModal(false)}
              className="absolute left-4 top-4 grid h-9 w-9 place-items-center rounded-full bg-white/5 text-slate-400 hover:bg-white/10 hover:text-white transition"
            >
              <X size={17} />
            </button>

            <div className="flex items-center gap-3.5 mb-5">
              <div className="grid h-12 w-12 place-items-center rounded-2xl bg-amber-500/10 border border-amber-500/30 text-amber-400">
                <Smartphone size={24} />
              </div>
              <div>
                <h3 className="text-lg font-black text-white">تطبيقات منظومة زقزوقة الرسمية</h3>
                <p className="text-xs text-amber-400/80 font-semibold">حزمة تطبيقات الأندرويد الرسمية بصيغة APK جاهزة للتثبيت المباشر</p>
              </div>
            </div>

            <div className="space-y-3 mb-6">
              {/* App 1: Zaqzouka Client App */}
              <a
                href="https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.85-92/zaqzouka-1.0.85-92.apk"
                download="zaqzouka-1.0.85-92.apk"
                target="_blank"
                rel="noopener noreferrer"
                className="group flex items-center justify-between gap-3 sm:gap-4 rounded-2xl border border-amber-500/25 bg-[#141525] p-3.5 sm:p-4 transition hover:border-amber-400 hover:bg-[#181A2D]"
              >
                <div className="flex items-center gap-3">
                  <div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400 group-hover:scale-105 transition">
                    <PlaySquare size={20} />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-extrabold text-white text-sm">تطبيق زقزوقة TV الرئيسي</span>
                      <span className="rounded bg-amber-500/15 px-1.5 py-0.5 text-[9px] font-bold text-amber-300">v1.0.85</span>
                    </div>
                    <p className="text-[11px] text-slate-400 mt-0.5">التطبيق الرئيسي للمسلسلات، الأفلام، والرياضة المباشرة</p>
                  </div>
                </div>
                <div className="flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-amber-400 to-amber-600 px-3.5 py-2 text-xs font-black text-slate-950 shadow-md shrink-0">
                  <Download size={14} />
                  تحميل APK
                </div>
              </a>

              {/* App 2: ZAG Player */}
              <a
                href="https://github.com/iptvappk-ctrl/madartv-updates/releases/download/v1.0.85-92/zag-player-1.1.37.apk"
                download="zag-player-1.1.37.apk"
                target="_blank"
                rel="noopener noreferrer"
                className="group flex items-center justify-between gap-3 sm:gap-4 rounded-2xl border border-white/10 bg-[#141525] p-3.5 sm:p-4 transition hover:border-amber-400 hover:bg-[#181A2D]"
              >
                <div className="flex items-center gap-3">
                  <div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-white/5 border border-white/10 text-slate-300 group-hover:scale-105 transition">
                    <Film size={20} />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-extrabold text-white text-sm">مشغل ZAG Player الأصلي</span>
                      <span className="rounded bg-white/10 px-1.5 py-0.5 text-[9px] font-bold text-slate-300">v1.1.37</span>
                    </div>
                    <p className="text-[11px] text-slate-400 mt-0.5">مشغل فيديو فائق الخفة والسرعة يدعم كافة صيغ HLS و MP4 والترجمة</p>
                  </div>
                </div>
                <div className="flex items-center gap-1.5 rounded-xl border border-amber-500/30 bg-amber-500/10 px-3.5 py-2 text-xs font-bold text-amber-300 shrink-0">
                  <Download size={14} />
                  تحميل APK
                </div>
              </a>
            </div>

            <div className="rounded-xl border border-white/5 bg-white/[0.02] p-3 text-[11px] text-slate-400 leading-relaxed text-center">
              💡 جميع التطبيقات موقعة ومطابقة لأحدث إصدارات المشروع، تعمل مباشرة على أجهزة Android و TV Box.
            </div>
          </div>
        </div>
      )}
    </>
  );
}
