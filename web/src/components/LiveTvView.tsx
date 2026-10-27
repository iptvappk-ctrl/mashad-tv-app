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
  const [categories, setCategories] = useState<YacineCategory[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  const [channels, setChannels] = useState<YacineChannel[]>([]);
  const [matches, setMatches] = useState<FootballMatch[]>([]);
  const [events, setEvents] = useState<YacineEvent[]>([]);

  const [matchFilter, setMatchFilter] = useState<"all" | "live" | "upcoming" | "finished">("all");

  const [loadingCategories, setLoadingCategories] = useState(true);
  const [loadingChannels, setLoadingChannels] = useState(false);
  const [loadingMatches, setLoadingMatches] = useState(true);
  const [resolvingStreamId, setResolvingStreamId] = useState<number | null>(null);

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
      } catch {}
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
          poster: channel.logo,
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
          subtitle: `${event.champions} • المعلق: ${event.commentary || "غير محدد"}`,
          poster: event.team1.logo || event.team2.logo,
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
    <div className="space-y-12 animate-fadeIn pb-24">
      {/* ── 1. Football Live Scoreboard & Schedule ─────────────────────── */}
      <section className="space-y-5">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-mashad-goldDark to-mashad-gold p-0.5 shadow-gold flex items-center justify-center">
              <div className="w-full h-full bg-mashad-bg rounded-[14px] flex items-center justify-center">
                <Trophy size={18} className="text-mashad-gold" />
              </div>
            </div>
            <div>
              <h2 className="text-xl sm:text-2xl font-black text-white">
                مباريات اليوم والنتائج الحية
              </h2>
              <p className="text-xs text-gray-400">الدوريات الكبرى والبطولات العالمية لحظة بلحظة</p>
            </div>
          </div>

          {/* Matches Filter Strip */}
          <div className="flex items-center gap-2 bg-mashad-card/90 p-1 rounded-2xl border border-mashad-border">
            <button
              onClick={() => setMatchFilter("all")}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
                matchFilter === "all"
                  ? "bg-mashad-gold text-black shadow-gold"
                  : "text-gray-400 hover:text-white"
              }`}
            >
              الكل ({matches.length})
            </button>
            <button
              onClick={() => setMatchFilter("live")}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
                matchFilter === "live"
                  ? "bg-red-600 text-white shadow-live"
                  : "text-gray-400 hover:text-white"
              }`}
            >
              <span className="w-2 h-2 rounded-full bg-red-400 animate-pulse" />
              <span>مباشر الآن ({liveCount})</span>
            </button>
            <button
              onClick={() => setMatchFilter("upcoming")}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
                matchFilter === "upcoming"
                  ? "bg-mashad-gold text-black shadow-gold"
                  : "text-gray-400 hover:text-white"
              }`}
            >
              القادمة
            </button>
            <button
              onClick={() => setMatchFilter("finished")}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
                matchFilter === "finished"
                  ? "bg-mashad-gold text-black shadow-gold"
                  : "text-gray-400 hover:text-white"
              }`}
            >
              انتهت
            </button>
          </div>
        </div>

        {/* Matches Grid */}
        {loadingMatches ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-32 rounded-2xl skeleton-shimmer border border-mashad-border/60" />
            ))}
          </div>
        ) : filteredMatches.length === 0 ? (
          <div className="py-12 rounded-2xl bg-mashad-card/40 border border-mashad-border text-center text-gray-400 text-sm">
            لا توجد مباريات مطابقة لهذا الفلتر حالياً.
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {filteredMatches.map((m) => {
              const isLive = m.status === "in";
              return (
                <div
                  key={m.id}
                  className={`glass-card p-4 rounded-2xl flex flex-col justify-between border relative overflow-hidden ${
                    isLive
                      ? "border-red-500/50 bg-gradient-to-b from-red-500/10 via-mashad-card to-mashad-card shadow-live"
                      : "border-mashad-border/80"
                  }`}
                >
                  {/* Top Bar: League & Status */}
                  <div className="flex items-center justify-between text-[11px] font-bold text-gray-400 border-b border-white/5 pb-2.5 mb-2.5">
                    <span className="line-clamp-1 max-w-[65%] text-gray-300 font-extrabold">
                      {m.leagueName}
                    </span>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[10px] font-black tracking-wide ${
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

                  {/* Teams & Scoreboard */}
                  <div className="flex items-center justify-between gap-3 my-1">
                    {/* Home */}
                    <div className="flex items-center gap-2.5 flex-1 min-w-0">
                      {m.homeLogo ? (
                        <img
                          src={m.homeLogo}
                          alt={m.homeTeam}
                          className="w-8 h-8 object-contain flex-shrink-0"
                          onError={(e) => {
                            (e.target as HTMLElement).style.display = "none";
                          }}
                        />
                      ) : (
                        <div className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-xs">
                          ⚽
                        </div>
                      )}
                      <span className="text-xs font-black text-white line-clamp-1">{m.homeTeam}</span>
                    </div>

                    {/* Result */}
                    <div className="px-3 py-1.5 bg-black/70 rounded-xl font-black text-sm tracking-wider text-mashad-gold border border-white/10 min-w-[55px] text-center shadow-inner">
                      {m.homeScore} : {m.awayScore}
                    </div>

                    {/* Away */}
                    <div className="flex items-center justify-end gap-2.5 flex-1 min-w-0">
                      <span className="text-xs font-black text-white line-clamp-1 text-left">
                        {m.awayTeam}
                      </span>
                      {m.awayLogo ? (
                        <img
                          src={m.awayLogo}
                          alt={m.awayTeam}
                          className="w-8 h-8 object-contain flex-shrink-0"
                          onError={(e) => {
                            (e.target as HTMLElement).style.display = "none";
                          }}
                        />
                      ) : (
                        <div className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-xs">
                          ⚽
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* ── 2. Yacine Live Events with Active Video Streams ──────────── */}
      {events.length > 0 && (
        <section className="space-y-5 pt-4 border-t border-mashad-border/60">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-red-500/20 text-red-500 border border-red-500/40 flex items-center justify-center shadow-live">
                <Radio size={20} className="live-badge-glow" />
              </div>
              <div>
                <h2 className="text-xl sm:text-2xl font-black text-white">بثوث المباريات المباشرة الآن</h2>
                <p className="text-xs text-gray-400">سيرفرات بث حي بجودة Full HD مع المعلق العربي</p>
              </div>
            </div>
            <span className="text-xs text-red-400 font-black animate-pulse flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-red-500" />
              {events.length} مباريات تبث الآن
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {events.map((ev) => (
              <div
                key={ev.id}
                onClick={() => handlePlayEvent(ev)}
                className="glass-card p-5 rounded-3xl border border-mashad-border hover:border-mashad-gold/80 cursor-pointer group flex flex-col justify-between transition-all"
              >
                <div className="flex items-center justify-between text-xs font-bold text-mashad-gold mb-3">
                  <span className="px-2.5 py-0.5 rounded-lg bg-mashad-gold/15 border border-mashad-gold/30">
                    {ev.champions}
                  </span>
                  <span className="text-gray-400 font-semibold">{ev.channel}</span>
                </div>

                <div className="flex items-center justify-between py-3">
                  <div className="flex items-center gap-3">
                    <img
                      src={ev.team1.logo}
                      alt={ev.team1.name}
                      className="w-10 h-10 object-contain group-hover:scale-110 transition-transform"
                    />
                    <span className="text-sm font-black text-white">{ev.team1.name}</span>
                  </div>

                  <span className="text-sm font-black text-mashad-gold px-2 gold-glow">VS</span>

                  <div className="flex items-center gap-3">
                    <span className="text-sm font-black text-white">{ev.team2.name}</span>
                    <img
                      src={ev.team2.logo}
                      alt={ev.team2.name}
                      className="w-10 h-10 object-contain group-hover:scale-110 transition-transform"
                    />
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between">
                  <span className="text-xs text-gray-400 flex items-center gap-1">
                    <Volume2 size={13} className="text-mashad-gold" />
                    <span>{ev.commentary ? `تعليق: ${ev.commentary}` : "تعليق عربي مباشر"}</span>
                  </span>

                  <button className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-mashad-gold to-mashad-goldLight text-black text-xs font-black shadow-gold group-hover:scale-105 transition-all">
                    {resolvingStreamId === ev.id ? (
                      <Loader2 size={14} className="animate-spin text-black" />
                    ) : (
                      <Play size={13} className="fill-black ml-0.5" />
                    )}
                    <span>مشاهدة البث</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* ── 3. TV Channels Catalog ───────────────────────────────────── */}
      <section className="space-y-6 pt-4 border-t border-mashad-border/60">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-mashad-goldDark to-mashad-gold p-0.5 shadow-gold flex items-center justify-center">
              <div className="w-full h-full bg-mashad-bg rounded-[14px] flex items-center justify-center">
                <Tv size={18} className="text-mashad-gold" />
              </div>
            </div>
            <div>
              <h2 className="text-xl sm:text-2xl font-black text-white">باقات القنوات المشفرة والمفتوحة</h2>
              <p className="text-xs text-gray-400">beIN Sports, SSC, أبوظبي, والقنوات الترفيهية</p>
            </div>
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
      </section>
    </div>
  );
}
