"use client";

import React, { useState, useEffect, useMemo } from "react";
import {
  Tv,
  Play,
  Radio,
  Trophy,
  Clock,
  Loader2,
  Sparkles,
  Calendar,
  Layers,
  Search,
  Flame,
  Volume2,
  CheckCircle2,
  History,
  AlertCircle,
  RefreshCw,
} from "lucide-react";
import { YacineCategory, YacineChannel, YacineEvent } from "@/lib/yacine";
import { FootballMatch, TOP_LEAGUES } from "@/lib/football";

interface LiveTvViewProps {
  searchQuery: string;
  onPlayStream: (streamData: {
    title: string;
    subtitle?: string;
    poster?: string;
    streams: { name: string; url: string }[];
  }) => void;
}

// Strictly European Top 5 + Champions League event filter
function isTopEuropeanEvent(ev: YacineEvent): boolean {
  const champ = ev.champions || "";
  const blacklist = [
    "النخبة", "آسيا", "اسيا", "الكويت", "قطر", "البحرين", "عمان", "العراق",
    "الجزائر", "تونس", "المغرب", "إفريقيا", "افريقيا", "الخليج", "أبطال آسيا", "ابطال اسيا"
  ];
  if (blacklist.some((b) => champ.includes(b))) return false;

  const whitelist = [
    "أبطال أوروبا", "ابطال اوروبا", "أوروبا", "اوروبا",
    "إنجليزي", "انجليزي", "إسباني", "اسباني", "إيطالي", "ايطالي",
    "ألماني", "الماني", "فرنسي",
    "Champions League", "Premier League", "La Liga", "Serie A", "Bundesliga", "Ligue 1"
  ];
  if (whitelist.some((w) => champ.includes(w))) return true;

  const t1 = ev.team1?.name || "";
  const t2 = ev.team2?.name || "";
  const europeanTeams = [
    "ريال مدريد", "برشلونة", "أتلتيكو", "ليفربول", "مانشستر", "آرسنال", "تشيلسي",
    "توتنهام", "بايرن", "دورتموند", "ليفركوزن", "ميلان", "إنتر", "يوفنتوس",
    "نابولي", "روما", "باريس", "موناكو", "مارسيليا"
  ];
  return europeanTeams.some((t) => t1.includes(t) || t2.includes(t));
}

const LEAGUE_FILTERS = [
  { id: "all", label: "جميع الدوريات الكبرى" },
  { id: "uefa.champions", label: "دوري أبطال أوروبا" },
  { id: "eng.1", label: "الدوري الإنجليزي" },
  { id: "esp.1", label: "الدوري الإسباني" },
  { id: "ita.1", label: "الدوري الإيطالي" },
  { id: "ger.1", label: "الدوري الألماني" },
  { id: "fra.1", label: "الدوري الفرنسي" },
];

export default function LiveTvView({ searchQuery, onPlayStream }: LiveTvViewProps) {
  // 3 Primary Tabs: Upcoming/Live | Results | Channels
  const [activeTab, setActiveTab] = useState<"upcoming" | "results" | "channels">("upcoming");
  const [selectedLeagueFilter, setSelectedLeagueFilter] = useState("all");

  const [categories, setCategories] = useState<YacineCategory[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | number | null>(null);
  const [channels, setChannels] = useState<YacineChannel[]>([]);
  const [matches, setMatches] = useState<FootballMatch[]>([]);
  const [events, setEvents] = useState<YacineEvent[]>([]);

  const [loadingCategories, setLoadingCategories] = useState(true);
  const [loadingChannels, setLoadingChannels] = useState(false);
  const [loadingMatches, setLoadingMatches] = useState(true);
  const [loadingEvents, setLoadingEvents] = useState(true);
  const [resolvingStreamId, setResolvingStreamId] = useState<number | string | null>(null);

  // Initial load
  useEffect(() => {
    async function loadData() {
      // 1. Categories
      try {
        const catRes = await fetch("/api/yacine/categories");
        const catData = await catRes.json();
        if (catData.success && catData.data.length > 0) {
          setCategories(catData.data);
          setSelectedCategory(catData.data[0].id);
        }
      } catch (err) {
        console.error("Categories fetch error:", err);
      } finally {
        setLoadingCategories(false);
      }

      // 2. Football matches (ESPN Top 5 + UCL)
      try {
        const matchRes = await fetch("/api/football/matches");
        const matchData = await matchRes.json();
        if (matchData.success) {
          setMatches(matchData.data || []);
        }
      } catch (err) {
        console.error("Matches fetch error:", err);
      } finally {
        setLoadingMatches(false);
      }

      // 3. Yacine Events
      try {
        const evRes = await fetch("/api/yacine/events");
        const evData = await evRes.json();
        if (evData.success) {
          setEvents(evData.data || []);
        }
      } catch (err) {
        console.error("Events fetch error:", err);
      } finally {
        setLoadingEvents(false);
      }
    }

    loadData();
  }, []);

  // Fetch Channels on Category Change
  useEffect(() => {
    if (!selectedCategory) return;
    let isCancelled = false;

    async function loadChannels() {
      setLoadingChannels(true);
      try {
        const res = await fetch(`/api/yacine/channels?categoryId=${selectedCategory}`);
        const data = await res.json();
        if (!isCancelled && data.success) {
          setChannels(data.data || []);
        }
      } catch (err) {
        console.error("Channels fetch error:", err);
      } finally {
        if (!isCancelled) setLoadingChannels(false);
      }
    }

    loadChannels();
    return () => {
      isCancelled = true;
    };
  }, [selectedCategory]);

  // Filtered Yacine live events (Top European Only)
  const filteredEvents = useMemo(() => {
    return events.filter(isTopEuropeanEvent);
  }, [events]);

  // Split matches into Upcoming/Live vs Finished
  const upcomingMatches = useMemo(() => {
    return matches.filter((m) => m.status === "in" || m.status === "pre");
  }, [matches]);

  const finishedMatches = useMemo(() => {
    return matches.filter((m) => m.status === "post");
  }, [matches]);

  const liveMatchesCount = useMemo(() => {
    return matches.filter((m) => m.status === "in").length;
  }, [matches]);

  // Filter by selected league
  const visibleUpcomingMatches = useMemo(() => {
    if (selectedLeagueFilter === "all") return upcomingMatches;
    return upcomingMatches.filter((m) => m.leagueCode === selectedLeagueFilter);
  }, [upcomingMatches, selectedLeagueFilter]);

  const visibleFinishedMatches = useMemo(() => {
    if (selectedLeagueFilter === "all") return finishedMatches;
    return finishedMatches.filter((m) => m.leagueCode === selectedLeagueFilter);
  }, [finishedMatches, selectedLeagueFilter]);

  const filteredChannels = useMemo(() => {
    return channels.filter((ch) =>
      searchQuery ? ch.name.toLowerCase().includes(searchQuery.toLowerCase()) : true
    );
  }, [channels, searchQuery]);

  // Handle Play Channel
  const handlePlayChannel = async (channel: YacineChannel) => {
    setResolvingStreamId(channel.id);
    try {
      const res = await fetch(`/api/yacine/stream?type=channel&id=${channel.id}`);
      const data = await res.json();

      if (data.success && data.data?.length > 0) {
        const streams = data.data.map((s: any) => ({
          name: s.name || "سيرفر البث",
          url: s.playUrl,
        }));

        onPlayStream({
          title: channel.name,
          subtitle: "بث حي ومباشر للقناة",
          poster: channel.logo || "/logo.jpg",
          streams,
        });
      } else {
        alert("البث غير متاح لهذه القناة حالياً.");
      }
    } catch {
      alert("حدث خطأ أثناء تحميل البث.");
    } finally {
      setResolvingStreamId(null);
    }
  };

  // Handle Play Event
  const handlePlayEvent = async (event: YacineEvent) => {
    setResolvingStreamId(event.id);
    try {
      const res = await fetch(`/api/yacine/stream?type=event&id=${event.id}`);
      const data = await res.json();

      if (data.success && data.data?.length > 0) {
        const streams = data.data.map((s: any) => ({
          name: s.name || "سيرفر البث",
          url: s.playUrl,
        }));

        onPlayStream({
          title: `${event.team1.name} ⚔️ ${event.team2.name}`,
          subtitle: `${event.champions} • القناة: ${event.channel} • المعلق: ${event.commentary || "غير محدد"}`,
          poster: event.team1.logo || event.team2.logo || "/logo.jpg",
          streams,
        });
      } else {
        alert("سيرفر المباراة غير متوفر حالياً.");
      }
    } catch {
      alert("حدث خطأ في تحميل بث المباراة.");
    } finally {
      setResolvingStreamId(null);
    }
  };

  // Handle Play Match Card (Connects to Yacine event or opens beIN SPORTS 1)
  const handlePlayMatchCard = async (m: FootballMatch) => {
    setResolvingStreamId(m.id);
    try {
      const matchingEvent = filteredEvents.find(
        (ev) =>
          ev.team1.name.includes(m.homeTeam) ||
          ev.team2.name.includes(m.awayTeam) ||
          m.homeTeam.includes(ev.team1.name) ||
          m.awayTeam.includes(ev.team2.name)
      );

      if (matchingEvent) {
        await handlePlayEvent(matchingEvent);
        return;
      }

      // Fast-path: open beIN SPORTS 1 HD
      const res = await fetch("/api/yacine/stream?type=channel&id=7972915523234028871");
      const data = await res.json();

      if (data.success && data.data?.length > 0) {
        const streams = data.data.map((s: any) => ({
          name: s.name || "سيرفر البث الرئيسي",
          url: s.playUrl,
        }));

        onPlayStream({
          title: `${m.homeTeam} ⚔️ ${m.awayTeam}`,
          subtitle: `بث مباشر • beIN SPORTS 1 HD • ${m.leagueName}`,
          poster: m.homeLogo || m.awayLogo || "/logo.jpg",
          streams,
        });
      } else {
        alert(`جاري تجهيز البث لمباراة ${m.homeTeam} ضد ${m.awayTeam}. يمكنك اختيار القناة الناقلة من قسم القنوات.`);
      }
    } catch {
      alert("تعذر تشغيل البث المباشر.");
    } finally {
      setResolvingStreamId(null);
    }
  };

  const handleRefreshMatches = async () => {
    setLoadingMatches(true);
    try {
      const matchRes = await fetch("/api/football/matches");
      const matchData = await matchRes.json();
      if (matchData.success) {
        setMatches(matchData.data || []);
      }
    } catch {}
    setLoadingMatches(false);
  };

  return (
    <div className="live-tv-light space-y-8 animate-fadeIn pb-24">
      {/* ── Top Primary Navigation Tabs (3 Tabs as Requested) ── */}
      <div className="flex items-center justify-center">
        <div className="flex items-center gap-2 bg-zakzouka-card/90 p-1.5 rounded-2xl border border-zakzouka-border/80 shadow-2xl max-w-2xl w-full backdrop-blur-xl">
          {/* Tab 1: Upcoming & Live */}
          <button
            onClick={() => setActiveTab("upcoming")}
            className={`flex-1 flex items-center justify-center gap-2 py-3 px-3 rounded-xl text-xs sm:text-sm font-black transition-all duration-300 ${
              activeTab === "upcoming"
                ? "bg-gradient-to-r from-zakzouka-gold to-zakzouka-goldLight text-black shadow-gold scale-[1.02]"
                : "text-gray-300 hover:text-white hover:bg-white/5"
            }`}
          >
            <Radio size={16} className={activeTab === "upcoming" ? "text-black" : "text-red-500"} />
            <span>المباريات القادمة والمباشرة</span>
            {liveMatchesCount > 0 && (
              <span className="px-2 py-0.5 rounded-full bg-red-600 text-white text-[10px] font-black animate-pulse">
                {liveMatchesCount} لايف
              </span>
            )}
          </button>

          {/* Tab 2: Finished Match Results */}
          <button
            onClick={() => setActiveTab("results")}
            className={`flex-1 flex items-center justify-center gap-2 py-3 px-3 rounded-xl text-xs sm:text-sm font-black transition-all duration-300 ${
              activeTab === "results"
                ? "bg-gradient-to-r from-zakzouka-gold to-zakzouka-goldLight text-black shadow-gold scale-[1.02]"
                : "text-gray-300 hover:text-white hover:bg-white/5"
            }`}
          >
            <History size={16} />
            <span>نتائج المباريات</span>
            {finishedMatches.length > 0 && (
              <span className="px-1.5 py-0.5 rounded-full bg-white/10 text-[10px] font-bold">
                {finishedMatches.length}
              </span>
            )}
          </button>

          {/* Tab 3: Channels */}
          <button
            onClick={() => setActiveTab("channels")}
            className={`flex-1 flex items-center justify-center gap-2 py-3 px-3 rounded-xl text-xs sm:text-sm font-black transition-all duration-300 ${
              activeTab === "channels"
                ? "bg-gradient-to-r from-zakzouka-gold to-zakzouka-goldLight text-black shadow-gold scale-[1.02]"
                : "text-gray-300 hover:text-white hover:bg-white/5"
            }`}
          >
            <Tv size={16} />
            <span>باقات القنوات</span>
            <span className="px-1.5 py-0.5 rounded-full bg-white/10 text-[10px] font-bold">
              {categories.length}
            </span>
          </button>
        </div>
      </div>

      {/* ── TAB 1: UPCOMING & LIVE MATCHES (Top 5 + UCL Only) ─────────── */}
      {activeTab === "upcoming" && (
        <div className="space-y-8">
          {/* League Filter Strip */}
          <div className="flex items-center justify-between flex-wrap gap-3 pb-2 border-b border-zakzouka-border/40">
            <div className="flex items-center gap-2 overflow-x-auto pb-1 max-w-full">
              {LEAGUE_FILTERS.map((f) => {
                const isSelected = selectedLeagueFilter === f.id;
                return (
                  <button
                    key={f.id}
                    onClick={() => setSelectedLeagueFilter(f.id)}
                    className={`whitespace-nowrap px-3.5 py-1.5 rounded-xl text-xs font-black transition-all duration-200 ${
                      isSelected
                        ? "bg-zakzouka-gold text-black shadow-gold"
                        : "bg-zakzouka-card text-gray-400 hover:text-white border border-zakzouka-border/60 hover:border-zakzouka-gold/50"
                    }`}
                  >
                    {f.label}
                  </button>
                );
              })}
            </div>

            <button
              onClick={handleRefreshMatches}
              className="flex items-center gap-1.5 text-xs text-zakzouka-gold hover:text-zakzouka-goldLight px-3 py-1.5 rounded-xl bg-zakzouka-card border border-zakzouka-border/60"
            >
              <RefreshCw size={13} className={loadingMatches ? "animate-spin" : ""} />
              <span>تحديث المواعيد</span>
            </button>
          </div>

          {/* Active Live Broadcasts for European Top Matches (If any streaming) */}
          {filteredEvents.length > 0 && (
            <section className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-xl bg-red-500/20 text-red-500 border border-red-500/40 flex items-center justify-center">
                    <Radio size={16} className="animate-pulse" />
                  </div>
                  <div>
                    <h2 className="text-lg sm:text-xl font-black text-white flex items-center gap-2">
                      <span>بثوث حية منقولة الآن 🔴</span>
                    </h2>
                    <p className="text-xs text-gray-400">سيرفرات نقل مباشر بجودة عالية للمباريات الأوروبية الجارية</p>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {filteredEvents.map((ev) => (
                  <div
                    key={ev.id}
                    onClick={() => handlePlayEvent(ev)}
                    className="glass-card p-5 rounded-3xl border border-zakzouka-border/90 hover:border-zakzouka-gold cursor-pointer group flex flex-col justify-between transition-all duration-300 relative overflow-hidden shadow-2xl"
                  >
                    <div className="flex items-center justify-between text-xs font-bold mb-3 border-b border-white/5 pb-2.5">
                      <span className="px-3 py-1 rounded-xl bg-zakzouka-gold/20 text-zakzouka-gold border border-zakzouka-gold/30 font-black">
                        {ev.champions}
                      </span>
                      <span className="px-3 py-1 rounded-xl bg-black/60 text-white font-black border border-white/10 flex items-center gap-1.5">
                        <Tv size={12} className="text-zakzouka-gold" />
                        {ev.channel}
                      </span>
                    </div>

                    <div className="flex items-center justify-between py-2 px-2">
                      <div className="flex items-center gap-3 flex-1">
                        <div className="w-12 h-12 rounded-2xl bg-black/60 p-2 border border-white/10 flex items-center justify-center flex-shrink-0 group-hover:scale-110 transition-transform">
                          <img
                            src={ev.team1.logo}
                            alt={ev.team1.name}
                            className="w-full h-full object-contain"
                            onError={(e) => {
                              (e.target as HTMLElement).style.display = "none";
                            }}
                          />
                        </div>
                        <span className="text-sm sm:text-base font-black text-white">{ev.team1.name}</span>
                      </div>

                      <div className="px-3 py-1 rounded-xl bg-red-600/20 border border-red-500/40 text-red-400 font-black text-xs tracking-wider mx-2">
                        VS
                      </div>

                      <div className="flex items-center justify-end gap-3 flex-1">
                        <span className="text-sm sm:text-base font-black text-white text-left">{ev.team2.name}</span>
                        <div className="w-12 h-12 rounded-2xl bg-black/60 p-2 border border-white/10 flex items-center justify-center flex-shrink-0 group-hover:scale-110 transition-transform">
                          <img
                            src={ev.team2.logo}
                            alt={ev.team2.name}
                            className="w-full h-full object-contain"
                            onError={(e) => {
                              (e.target as HTMLElement).style.display = "none";
                            }}
                          />
                        </div>
                      </div>
                    </div>

                    <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between">
                      <div className="flex items-center gap-1.5 text-xs text-gray-300">
                        <Volume2 size={15} className="text-zakzouka-gold" />
                        <span>{ev.commentary ? `تعليق: ${ev.commentary}` : "تعليق عربي مباشر"}</span>
                      </div>

                      <button
                        disabled={resolvingStreamId === ev.id}
                        className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-gradient-to-r from-zakzouka-gold to-zakzouka-goldLight text-black text-xs font-black shadow-gold group-hover:scale-105 active:scale-95 transition-all"
                      >
                        {resolvingStreamId === ev.id ? (
                          <Loader2 size={16} className="animate-spin text-black" />
                        ) : (
                          <Play size={16} className="fill-black ml-0.5" />
                        )}
                        <span>مشاهدة البث الآن</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Upcoming Matches Grid */}
          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <Calendar size={18} className="text-zakzouka-gold" />
                <h3 className="text-lg font-black text-white">
                  جدول المباريات القادمة (الدوريات الخمس الكبرى ودوري الأبطال)
                </h3>
              </div>
              <span className="text-xs text-gray-400 font-bold">
                {visibleUpcomingMatches.length} مباراة مجدولة
              </span>
            </div>

            {loadingMatches ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3.5">
                {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
                  <div key={i} className="h-32 rounded-2xl skeleton-shimmer border border-zakzouka-border/60" />
                ))}
              </div>
            ) : visibleUpcomingMatches.length === 0 ? (
              <div className="p-8 rounded-3xl bg-zakzouka-card/60 border border-zakzouka-border text-center space-y-2">
                <AlertCircle size={28} className="text-zakzouka-gold mx-auto" />
                <p className="text-sm font-bold text-gray-300">
                  لا توجد مباريات قادمة مسجلة لهذا الدوري حالياً
                </p>
                <p className="text-xs text-gray-400">
                  يمكنك مراجعة نتائج الجولة السابقة من تبويب "نتائج المباريات"، أو مشاهدة القنوات الرياضية مباشرة.
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3.5">
                {visibleUpcomingMatches.map((m) => {
                  const isLive = m.status === "in";
                  return (
                    <div
                      key={m.id}
                      onClick={() => handlePlayMatchCard(m)}
                      className={`glass-card p-4 rounded-2xl flex flex-col justify-between border cursor-pointer hover:border-zakzouka-gold transition-all duration-300 ${
                        isLive ? "border-red-500/60 shadow-live bg-red-950/10" : "border-zakzouka-border/60"
                      }`}
                    >
                      {/* Top: League Name & Time Status */}
                      <div className="flex items-center justify-between text-[11px] mb-3 pb-2 border-b border-white/5">
                        <span className="text-zakzouka-gold font-black truncate max-w-[140px]">
                          {m.leagueName}
                        </span>
                        <span
                          className={`px-2 py-0.5 rounded-lg font-bold flex items-center gap-1 ${
                            isLive
                              ? "bg-red-500 text-white animate-pulse"
                              : "bg-white/5 text-gray-300 border border-white/10"
                          }`}
                        >
                          {isLive && <span className="w-1.5 h-1.5 rounded-full bg-white" />}
                          {m.statusText}
                        </span>
                      </div>

                      {/* Teams & Score/VS */}
                      <div className="flex items-center justify-between py-2">
                        {/* Home Team */}
                        <div className="flex flex-col items-center flex-1 text-center gap-1.5">
                          <div className="w-10 h-10 rounded-xl bg-black/40 p-1.5 border border-white/10 flex items-center justify-center">
                            {m.homeLogo ? (
                              <img src={m.homeLogo} alt={m.homeTeam} className="w-full h-full object-contain" />
                            ) : (
                              <div className="w-5 h-5 rounded-full bg-gray-700" />
                            )}
                          </div>
                          <span className="text-xs font-bold text-white line-clamp-1 max-w-[90px]">
                            {m.homeTeam}
                          </span>
                        </div>

                        {/* Center: Live Score or VS */}
                        <div className="px-3 flex flex-col items-center">
                          {isLive ? (
                            <div className="text-lg font-black text-zakzouka-gold tracking-widest">
                              {m.homeScore} - {m.awayScore}
                            </div>
                          ) : (
                            <div className="px-2.5 py-1 rounded-lg bg-black/50 text-[11px] text-gray-400 font-black border border-white/5">
                              VS
                            </div>
                          )}
                        </div>

                        {/* Away Team */}
                        <div className="flex flex-col items-center flex-1 text-center gap-1.5">
                          <div className="w-10 h-10 rounded-xl bg-black/40 p-1.5 border border-white/10 flex items-center justify-center">
                            {m.awayLogo ? (
                              <img src={m.awayLogo} alt={m.awayTeam} className="w-full h-full object-contain" />
                            ) : (
                              <div className="w-5 h-5 rounded-full bg-gray-700" />
                            )}
                          </div>
                          <span className="text-xs font-bold text-white line-clamp-1 max-w-[90px]">
                            {m.awayTeam}
                          </span>
                        </div>
                      </div>

                      {/* Action Bar */}
                      <div className="mt-3 pt-2 border-t border-white/5 flex items-center justify-between text-[11px]">
                        <span className="text-gray-400">
                          {isLive ? "بث مباشر عبر beIN" : "متاحة للبث عند الانطلاق"}
                        </span>
                        <span className="text-zakzouka-gold font-bold flex items-center gap-1 group-hover:translate-x-[-2px] transition-transform">
                          <Play size={12} className="fill-zakzouka-gold" />
                          <span>{isLive ? "شاهد الآن" : "البث المباشر"}</span>
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      )}

      {/* ── TAB 2: FINISHED MATCH RESULTS (Top 5 + UCL Only) ─────────── */}
      {activeTab === "results" && (
        <div className="space-y-6">
          {/* League Filter Strip */}
          <div className="flex items-center justify-between flex-wrap gap-3 pb-2 border-b border-zakzouka-border/40">
            <div className="flex items-center gap-2 overflow-x-auto pb-1 max-w-full">
              {LEAGUE_FILTERS.map((f) => {
                const isSelected = selectedLeagueFilter === f.id;
                return (
                  <button
                    key={f.id}
                    onClick={() => setSelectedLeagueFilter(f.id)}
                    className={`whitespace-nowrap px-3.5 py-1.5 rounded-xl text-xs font-black transition-all duration-200 ${
                      isSelected
                        ? "bg-zakzouka-gold text-black shadow-gold"
                        : "bg-zakzouka-card text-gray-400 hover:text-white border border-zakzouka-border/60 hover:border-zakzouka-gold/50"
                    }`}
                  >
                    {f.label}
                  </button>
                );
              })}
            </div>

            <span className="text-xs text-gray-400 font-bold">
              {visibleFinishedMatches.length} نتيجة مسجلة
            </span>
          </div>

          {loadingMatches ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3.5">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="h-28 rounded-2xl skeleton-shimmer border border-zakzouka-border/60" />
              ))}
            </div>
          ) : visibleFinishedMatches.length === 0 ? (
            <div className="p-10 rounded-3xl bg-zakzouka-card/60 border border-zakzouka-border text-center space-y-2">
              <CheckCircle2 size={32} className="text-zakzouka-gold mx-auto" />
              <p className="text-sm font-bold text-gray-300">
                لا توجد نتائج مسجلة لهذا الدوري في هذه الفترة
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3.5">
              {visibleFinishedMatches.map((m) => (
                <div
                  key={m.id}
                  className="glass-card p-4 rounded-2xl flex flex-col justify-between border border-zakzouka-border/60 hover:border-zakzouka-gold/60 transition-all duration-300"
                >
                  {/* Top Bar: League Name & "Finished" Badge */}
                  <div className="flex items-center justify-between text-[11px] mb-2.5 pb-2 border-b border-white/5">
                    <span className="text-zakzouka-gold font-black truncate max-w-[150px]">
                      {m.leagueName}
                    </span>
                    <span className="px-2 py-0.5 rounded-lg bg-white/5 text-gray-400 text-[10px] font-bold border border-white/10">
                      انتهت المباراة ✓
                    </span>
                  </div>

                  {/* Teams & Final Score */}
                  <div className="flex items-center justify-between py-2">
                    {/* Home Team */}
                    <div className="flex flex-col items-center flex-1 text-center gap-1.5">
                      <div className="w-10 h-10 rounded-xl bg-black/40 p-1.5 border border-white/10 flex items-center justify-center">
                        {m.homeLogo ? (
                          <img src={m.homeLogo} alt={m.homeTeam} className="w-full h-full object-contain" />
                        ) : (
                          <div className="w-5 h-5 rounded-full bg-gray-700" />
                        )}
                      </div>
                      <span className="text-xs font-bold text-white line-clamp-1 max-w-[90px]">
                        {m.homeTeam}
                      </span>
                    </div>

                    {/* Final Score */}
                    <div className="px-3 flex flex-col items-center">
                      <div className="px-3 py-1 rounded-xl bg-black/60 border border-zakzouka-gold/30 text-zakzouka-gold font-black text-base sm:text-lg tracking-wider">
                        {m.homeScore} - {m.awayScore}
                      </div>
                      <span className="text-[10px] text-gray-400 mt-1 font-semibold">نتيجة نهائية</span>
                    </div>

                    {/* Away Team */}
                    <div className="flex flex-col items-center flex-1 text-center gap-1.5">
                      <div className="w-10 h-10 rounded-xl bg-black/40 p-1.5 border border-white/10 flex items-center justify-center">
                        {m.awayLogo ? (
                          <img src={m.awayLogo} alt={m.awayTeam} className="w-full h-full object-contain" />
                        ) : (
                          <div className="w-5 h-5 rounded-full bg-gray-700" />
                        )}
                      </div>
                      <span className="text-xs font-bold text-white line-clamp-1 max-w-[90px]">
                        {m.awayTeam}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ── TAB 3: CHANNELS BROWSER ─────────────────────────────────── */}
      {activeTab === "channels" && (
        <section className="space-y-6">
          {/* Category Chips Bar */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Layers size={18} className="text-zakzouka-gold" />
              <h3 className="text-lg font-black text-white">اختر باقة القنوات</h3>
            </div>

            {loadingCategories ? (
              <div className="flex items-center gap-3 overflow-x-auto py-2">
                {[1, 2, 3, 4, 5].map((i) => (
                  <div key={i} className="h-10 w-28 rounded-xl skeleton-shimmer" />
                ))}
              </div>
            ) : (
              <div className="flex items-center gap-2.5 overflow-x-auto pb-2 scrollbar-none">
                {categories.map((cat) => {
                  const isSelected = selectedCategory === cat.id;
                  return (
                    <button
                      key={cat.id}
                      onClick={() => setSelectedCategory(cat.id)}
                      className={`whitespace-nowrap px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-2 border ${
                        isSelected
                          ? "bg-gradient-to-r from-zakzouka-gold to-zakzouka-goldLight text-black border-transparent shadow-gold font-black scale-105"
                          : "bg-zakzouka-card text-gray-300 border-zakzouka-border/80 hover:border-zakzouka-gold/60 hover:text-white"
                      }`}
                    >
                      <Tv size={14} className={isSelected ? "text-black" : "text-zakzouka-gold"} />
                      <span>{cat.name}</span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Channels Grid */}
          <div className="space-y-4 pt-2">
            <div className="flex items-center justify-between">
              <span className="text-xs text-gray-400">
                {filteredChannels.length} قناة متاحة في هذه الباقة
              </span>
            </div>

            {loadingChannels ? (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3.5">
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((i) => (
                  <div key={i} className="aspect-[4/3] rounded-2xl skeleton-shimmer" />
                ))}
              </div>
            ) : filteredChannels.length === 0 ? (
              <div className="py-16 text-center text-gray-400 rounded-3xl bg-zakzouka-card/40 border border-zakzouka-border">
                <p className="text-sm font-bold">لا توجد قنوات مطابقة لبحثك</p>
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3.5">
                {filteredChannels.map((channel) => (
                  <div
                    key={channel.id}
                    onClick={() => handlePlayChannel(channel)}
                    className="glass-card group p-3 rounded-2xl border border-zakzouka-border/80 hover:border-zakzouka-gold cursor-pointer transition-all duration-300 flex flex-col items-center justify-between aspect-[4/3] relative overflow-hidden shadow-lg hover:shadow-gold hover:scale-[1.03]"
                  >
                    {/* Live Pulse Dot */}
                    <div className="absolute top-2.5 right-2.5 flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-black/60 border border-white/10 text-[9px] text-red-400 font-bold">
                      <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-ping" />
                      LIVE
                    </div>

                    {/* Channel Logo */}
                    <div className="flex-1 flex items-center justify-center p-2 w-full">
                      {channel.logo ? (
                        <img
                          src={channel.logo}
                          alt={channel.name}
                          className="max-h-14 max-w-full object-contain filter group-hover:brightness-110 transition-all"
                          onError={(e) => {
                            (e.target as HTMLElement).style.display = "none";
                          }}
                        />
                      ) : (
                        <Tv size={32} className="text-zakzouka-muted group-hover:text-zakzouka-gold transition-colors" />
                      )}
                    </div>

                    {/* Channel Name */}
                    <div className="w-full text-center border-t border-white/5 pt-1.5">
                      <p className="text-xs font-bold text-white truncate group-hover:text-zakzouka-gold transition-colors">
                        {channel.name}
                      </p>
                    </div>

                    {/* Hover Play Button Overlay */}
                    <div className="absolute inset-0 bg-black/60 backdrop-blur-sm opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-zakzouka-gold to-zakzouka-goldLight text-black flex items-center justify-center shadow-gold scale-75 group-hover:scale-100 transition-transform">
                        {resolvingStreamId === channel.id ? (
                          <Loader2 size={18} className="animate-spin text-black" />
                        ) : (
                          <Play size={18} className="fill-black ml-0.5" />
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      )}
    </div>
  );
}
