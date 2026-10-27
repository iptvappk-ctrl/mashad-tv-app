"use client";

import React, { useState, useEffect } from "react";
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
  ChevronLeft,
  ChevronRight,
  ExternalLink,
} from "lucide-react";
import { YacineCategory, YacineChannel, YacineEvent } from "@/lib/yacine";
import { FootballMatch } from "@/lib/football";

interface LiveTvViewProps {
  searchQuery: string;
  onPlayStream: (streamData: {
    title: string;
    subtitle?: string;
    poster?: string;
    streams: { name: string; url: string }[];
  }) => void;
}

export default function LiveTvView({ searchQuery, onPlayStream }: LiveTvViewProps) {
  const [subTab, setSubTab] = useState<"matches" | "channels">("matches");

  const [categories, setCategories] = useState<YacineCategory[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  const [channels, setChannels] = useState<YacineChannel[]>([]);
  const [matches, setMatches] = useState<FootballMatch[]>([]);
  const [events, setEvents] = useState<YacineEvent[]>([]);

  const [matchFilter, setMatchFilter] = useState<"all" | "live" | "upcoming" | "finished">("all");

  const [loadingCategories, setLoadingCategories] = useState(true);
  const [loadingChannels, setLoadingChannels] = useState(false);
  const [loadingMatches, setLoadingMatches] = useState(true);
  const [loadingEvents, setLoadingEvents] = useState(true);
  const [resolvingStreamId, setResolvingStreamId] = useState<number | string | null>(null);

  // Load initial data
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

      // 2. Football matches from ESPN
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

  // Handle Play match from ESPN card (link to corresponding channel)
  const handlePlayEspnMatch = async (m: FootballMatch) => {
    setResolvingStreamId(m.id);
    try {
      // 1. Check if there is an event matching team name
      const matchingEvent = events.find(
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

      // 2. Otherwise open beIN SPORTS 1 default live stream
      const beInCat = categories.find((c) => c.name.toLowerCase().includes("1080") || c.name.includes("beIN"));
      const catId = beInCat ? beInCat.id : 4;

      const channelsRes = await fetch(`/api/yacine/channels?categoryId=${catId}`);
      const channelsData = await channelsRes.json();
      const firstChannel = channelsData.data?.[0];

      if (firstChannel) {
        await handlePlayChannel(firstChannel);
      } else {
        alert(`جاري البحث عن بث لمباراة ${m.homeTeam} vs ${m.awayTeam}. يمكنك اختيار القناة الناقلة من قسم القنوات.`);
      }
    } catch {
      alert("تعذر تشغيل البث المباشر.");
    } finally {
      setResolvingStreamId(null);
    }
  };

  // Filter Matches
  const filteredMatches = matches.filter((m) => {
    if (matchFilter === "live") return m.status === "in";
    if (matchFilter === "upcoming") return m.status === "pre";
    if (matchFilter === "finished") return m.status === "post";
    return true;
  });

  const liveCount = matches.filter((m) => m.status === "in").length;

  const filteredChannels = channels.filter((ch) =>
    searchQuery ? ch.name.toLowerCase().includes(searchQuery.toLowerCase()) : true
  );

  return (
    <div className="space-y-8 animate-fadeIn pb-24">
      {/* ── Sub-Tab Switcher: Matches vs Channels (Same as Android App!) ── */}
      <div className="flex items-center justify-center">
        <div className="flex items-center gap-2 bg-mashad-card/90 p-1.5 rounded-2xl border border-mashad-border/80 shadow-2xl max-w-md w-full backdrop-blur-xl">
          <button
            onClick={() => setSubTab("matches")}
            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-xl text-xs sm:text-sm font-black transition-all duration-300 ${
              subTab === "matches"
                ? "bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black shadow-gold scale-[1.02]"
                : "text-gray-300 hover:text-white hover:bg-white/5"
            }`}
          >
            <Radio size={17} className={subTab === "matches" ? "text-black" : "text-red-500"} />
            <span>⚽ بثوث المباريات الحية</span>
            {events.length > 0 && (
              <span className="px-2 py-0.5 rounded-full bg-red-600 text-white text-[10px] font-black animate-pulse">
                {events.length}
              </span>
            )}
          </button>

          <button
            onClick={() => setSubTab("channels")}
            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-xl text-xs sm:text-sm font-black transition-all duration-300 ${
              subTab === "channels"
                ? "bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black shadow-gold scale-[1.02]"
                : "text-gray-300 hover:text-white hover:bg-white/5"
            }`}
          >
            <Tv size={17} />
            <span>📺 باقات القنوات ({categories.length})</span>
          </button>
        </div>
      </div>

      {/* ── TAB 1: MATCHES (Active Live Broadcasts + Schedule) ─────────── */}
      {subTab === "matches" ? (
        <div className="space-y-10">
          {/* 1. Yacine Live Broadcast Events (TOP PRIORITY - WITH PLAY BUTTON!) */}
          <section className="space-y-4">
            <div className="flex items-center justify-between border-b border-mashad-border/60 pb-3">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-red-500/20 text-red-500 border border-red-500/40 flex items-center justify-center shadow-live">
                  <Radio size={18} className="live-badge-glow" />
                </div>
                <div>
                  <h2 className="text-xl sm:text-2xl font-black text-white flex items-center gap-2">
                    <span>بثوث المباريات المباشرة الآن</span>
                    <span className="w-2.5 h-2.5 rounded-full bg-red-500 live-badge-glow" />
                  </h2>
                  <p className="text-xs text-gray-400">سيرفرات بث فيديو مباشر بجودات متعددة مع التعليق العربي</p>
                </div>
              </div>

              <span className="text-xs text-mashad-gold font-bold">
                {events.length > 0 ? `${events.length} مباريات منقولة لايف` : "جاري التحقق من البثوث"}
              </span>
            </div>

            {loadingEvents ? (
              <div className="py-12 flex flex-col items-center justify-center gap-3">
                <Loader2 size={24} className="animate-spin text-mashad-gold" />
                <p className="text-xs text-gray-400">جاري جلب سيرفرات البث المباشر...</p>
              </div>
            ) : events.length === 0 ? (
              <div className="p-8 rounded-3xl bg-mashad-card/60 border border-mashad-border text-center space-y-2">
                <p className="text-sm font-bold text-gray-300">
                  لا توجد مباريات منقولة بالبث المباشر في هذه اللحظة ⚽
                </p>
                <p className="text-xs text-gray-400">
                  يمكنك متابعة نتائج ومواعيد مباريات اليوم بالأسفل، أو مشاهدة قنوات beIN Sports مباشرة من تبويب "باقات القنوات".
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {events.map((ev) => (
                  <div
                    key={ev.id}
                    onClick={() => handlePlayEvent(ev)}
                    className="glass-card p-5 rounded-3xl border border-mashad-border/90 hover:border-mashad-gold cursor-pointer group flex flex-col justify-between transition-all duration-300 relative overflow-hidden shadow-2xl"
                  >
                    {/* Top League & Channel Badges */}
                    <div className="flex items-center justify-between text-xs font-bold mb-3 border-b border-white/5 pb-2.5">
                      <span className="px-3 py-1 rounded-xl bg-mashad-gold/20 text-mashad-gold border border-mashad-gold/30 font-black">
                        {ev.champions}
                      </span>
                      <span className="px-3 py-1 rounded-xl bg-black/60 text-white font-black border border-white/10 flex items-center gap-1.5">
                        <Tv size={12} className="text-mashad-gold" />
                        {ev.channel}
                      </span>
                    </div>

                    {/* Teams Clash Banner */}
                    <div className="flex items-center justify-between py-3 px-2">
                      {/* Team 1 */}
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

                      {/* VS Center Badge */}
                      <div className="px-3 py-1 rounded-xl bg-red-600/20 border border-red-500/40 text-red-400 font-black text-xs tracking-wider mx-2 live-badge-glow">
                        VS
                      </div>

                      {/* Team 2 */}
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

                    {/* Bottom Action Row */}
                    <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between">
                      <div className="flex items-center gap-1.5 text-xs text-gray-300">
                        <Volume2 size={15} className="text-mashad-gold" />
                        <span>{ev.commentary ? `تعليق: ${ev.commentary}` : "تعليق عربي مباشر"}</span>
                      </div>

                      <button
                        disabled={resolvingStreamId === ev.id}
                        className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black text-xs font-black shadow-gold group-hover:scale-105 active:scale-95 transition-all"
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
            )}
          </section>

          {/* 2. ESPN Schedule & Scores */}
          <section className="space-y-4 pt-4 border-t border-mashad-border/60">
            <div className="flex items-center justify-between flex-wrap gap-4">
              <div className="flex items-center gap-2.5">
                <Trophy size={20} className="text-mashad-gold" />
                <h3 className="text-lg sm:text-xl font-black text-white">نتائج ومواعيد مباريات اليوم</h3>
              </div>

              {/* Status Filter */}
              <div className="flex items-center gap-1.5 bg-mashad-card/90 p-1 rounded-2xl border border-mashad-border">
                <button
                  onClick={() => setMatchFilter("all")}
                  className={`px-3 py-1 rounded-xl text-xs font-bold transition ${
                    matchFilter === "all" ? "bg-mashad-gold text-black shadow-gold" : "text-gray-400 hover:text-white"
                  }`}
                >
                  الكل ({matches.length})
                </button>
                <button
                  onClick={() => setMatchFilter("live")}
                  className={`flex items-center gap-1 px-3 py-1 rounded-xl text-xs font-bold transition ${
                    matchFilter === "live" ? "bg-red-600 text-white shadow-live" : "text-gray-400 hover:text-white"
                  }`}
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-red-400 animate-pulse" />
                  <span>مباشر ({liveCount})</span>
                </button>
                <button
                  onClick={() => setMatchFilter("upcoming")}
                  className={`px-3 py-1 rounded-xl text-xs font-bold transition ${
                    matchFilter === "upcoming" ? "bg-mashad-gold text-black shadow-gold" : "text-gray-400 hover:text-white"
                  }`}
                >
                  القادمة
                </button>
                <button
                  onClick={() => setMatchFilter("finished")}
                  className={`px-3 py-1 rounded-xl text-xs font-bold transition ${
                    matchFilter === "finished" ? "bg-mashad-gold text-black shadow-gold" : "text-gray-400 hover:text-white"
                  }`}
                >
                  انتهت
                </button>
              </div>
            </div>

            {loadingMatches ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3.5">
                {[1, 2, 3, 4].map((i) => (
                  <div key={i} className="h-28 rounded-2xl skeleton-shimmer border border-mashad-border/60" />
                ))}
              </div>
            ) : filteredMatches.length === 0 ? (
              <div className="py-10 text-center text-gray-400 text-xs rounded-2xl bg-mashad-card/30 border border-mashad-border">
                لا توجد مباريات مسجلة لهذا التصنيف حالياً.
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3.5">
                {filteredMatches.map((m) => {
                  const isLive = m.status === "in";
                  return (
                    <div
                      key={m.id}
                      onClick={() => handlePlayEspnMatch(m)}
                      className={`glass-card p-4 rounded-2xl flex flex-col justify-between border cursor-pointer hover:border-mashad-gold transition-all ${
                        isLive
                          ? "border-red-500/50 bg-gradient-to-b from-red-500/10 via-mashad-card to-mashad-card shadow-live"
                          : "border-mashad-border/80"
                      }`}
                    >
                      <div className="flex items-center justify-between text-[11px] font-bold text-gray-400 border-b border-white/5 pb-2 mb-2">
                        <span className="line-clamp-1 max-w-[65%] text-gray-300 font-extrabold">{m.leagueName}</span>
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-black ${
                            isLive
                              ? "bg-red-500 text-white live-badge-glow"
                              : m.status === "post"
                              ? "bg-gray-800 text-gray-300"
                              : "bg-mashad-gold/20 text-mashad-gold border border-mashad-gold/30"
                          }`}
                        >
                          {m.statusText}
                        </span>
                      </div>

                      <div className="flex items-center justify-between gap-2 my-1">
                        <div className="flex items-center gap-2 flex-1 min-w-0">
                          {m.homeLogo ? (
                            <img src={m.homeLogo} alt={m.homeTeam} className="w-7 h-7 object-contain flex-shrink-0" />
                          ) : (
                            <div className="w-7 h-7 rounded-full bg-white/10 flex items-center justify-center text-xs">⚽</div>
                          )}
                          <span className="text-xs font-bold text-white line-clamp-1">{m.homeTeam}</span>
                        </div>

                        <div className="px-2.5 py-1 bg-black/70 rounded-xl font-black text-xs text-mashad-gold border border-white/10 min-w-[50px] text-center">
                          {m.homeScore} : {m.awayScore}
                        </div>

                        <div className="flex items-center justify-end gap-2 flex-1 min-w-0">
                          <span className="text-xs font-bold text-white line-clamp-1 text-left">{m.awayTeam}</span>
                          {m.awayLogo ? (
                            <img src={m.awayLogo} alt={m.awayTeam} className="w-7 h-7 object-contain flex-shrink-0" />
                          ) : (
                            <div className="w-7 h-7 rounded-full bg-white/10 flex items-center justify-center text-xs">⚽</div>
                          )}
                        </div>
                      </div>

                      <div className="mt-2 pt-1.5 border-t border-white/5 flex items-center justify-between text-[10px] text-gray-400">
                        <span>اضغط للمشاهدة والقنوات</span>
                        <Play size={11} className="text-mashad-gold" />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      ) : (
        /* ── TAB 2: CHANNELS (All TV Channels Categorized) ─────────────── */
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl sm:text-2xl font-black text-white">باقات القنوات التلفزيونية المشفرة</h2>
              <p className="text-xs text-gray-400">beIN Sports, SSC, أبوظبي, والباقات الترفيهية</p>
            </div>
          </div>

          {/* Category Pills Bar */}
          {loadingCategories ? (
            <div className="h-12 flex items-center gap-2 overflow-x-auto no-scrollbar">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="w-32 h-11 rounded-2xl skeleton-shimmer flex-shrink-0" />
              ))}
            </div>
          ) : (
            <div className="flex items-center gap-2.5 overflow-x-auto pb-2 no-scrollbar">
              {categories.map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => setSelectedCategory(cat.id)}
                  className={`px-5 py-3 rounded-2xl text-xs font-black whitespace-nowrap transition-all duration-300 flex-shrink-0 ${
                    selectedCategory === cat.id
                      ? "bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black shadow-gold scale-105"
                      : "bg-mashad-card text-gray-300 hover:bg-mashad-elevated hover:text-white border border-mashad-border"
                  }`}
                >
                  {cat.name}
                </button>
              ))}
            </div>
          )}

          {/* Channels Grid */}
          {loadingChannels ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
              {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((i) => (
                <div key={i} className="h-36 rounded-2xl skeleton-shimmer border border-mashad-border/60" />
              ))}
            </div>
          ) : filteredChannels.length === 0 ? (
            <div className="py-20 text-center text-gray-400 text-sm rounded-2xl bg-mashad-card/30 border border-mashad-border">
              لا توجد قنوات متوفرة في هذه الباقة حالياً.
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
              {filteredChannels.map((channel) => (
                <div
                  key={channel.id}
                  onClick={() => handlePlayChannel(channel)}
                  className="glass-card p-4 rounded-3xl flex flex-col items-center justify-center gap-3.5 cursor-pointer group hover:border-mashad-gold/80 transition-all relative overflow-hidden aspect-[4/3]"
                >
                  {/* Channel Logo */}
                  <div className="w-16 h-16 rounded-2xl bg-black/60 border border-white/5 flex items-center justify-center p-2.5 group-hover:scale-110 transition-transform shadow-inner">
                    {channel.logo ? (
                      <img
                        src={channel.logo}
                        alt={channel.name}
                        loading="lazy"
                        className="w-full h-full object-contain"
                        onError={(e) => {
                          (e.target as HTMLElement).style.display = "none";
                        }}
                      />
                    ) : (
                      <Tv size={26} className="text-mashad-gold" />
                    )}
                  </div>

                  {/* Channel Name */}
                  <h3 className="text-xs sm:text-sm font-black text-white text-center line-clamp-1 group-hover:text-mashad-gold transition">
                    {channel.name}
                  </h3>

                  {/* Hover Play Button Overlay */}
                  <div className="absolute inset-0 bg-black/75 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center backdrop-blur-sm">
                    <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-mashad-gold to-mashad-goldLight text-black flex items-center justify-center shadow-gold group-hover:scale-110 transition-transform">
                      {resolvingStreamId === channel.id ? (
                        <Loader2 size={20} className="animate-spin text-black" />
                      ) : (
                        <Play size={20} className="fill-black ml-0.5" />
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
