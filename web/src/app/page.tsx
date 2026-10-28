"use client";

import React, { useEffect, useRef, useState } from "react";
import Navbar from "@/components/Navbar";
import CinemaView from "@/components/CinemaView";
import DetailView from "@/components/DetailView";
import LiveTvView from "@/components/LiveTvView";
import PlayerModal, { StreamSource } from "@/components/PlayerModal";
import { ArabseedDetail, ArabseedEpisode, ArabseedItem, ArabseedSeason } from "@/lib/arabseed";
import { Bookmark, Star, Trash2 } from "lucide-react";
import { trackCompletedSearch, trackContentOpen } from "@/lib/analytics";

type Tab = "home" | "movies" | "series" | "anime" | "matches" | "favorites";
interface DetailSnapshot {
  item: ArabseedItem;
  detail: ArabseedDetail | null;
  seriesReference: ArabseedDetail | null;
  episodeReference: ArabseedDetail | null;
}

export default function HomePage() {
  const [activeTab, setActiveTab] = useState<Tab>("home");
  const [searchQuery, setSearchQuery] = useState("");
  const [favorites, setFavorites] = useState<ArabseedItem[]>([]);
  const [liveMatchesCount, setLiveMatchesCount] = useState(0);
  const [selectedDetailItem, setSelectedDetailItem] = useState<ArabseedItem | null>(null);
  const [selectedDetailData, setSelectedDetailData] = useState<ArabseedDetail | null>(null);
  const [seriesReference, setSeriesReference] = useState<ArabseedDetail | null>(null);
  const [episodeReference, setEpisodeReference] = useState<ArabseedDetail | null>(null);
  const [detailHistory, setDetailHistory] = useState<DetailSnapshot[]>([]);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [resolvingServer, setResolvingServer] = useState<string | null>(null);
  const [recommendations, setRecommendations] = useState<ArabseedItem[]>([]);
  const detailRequestId = useRef(0);
  const [playerState, setPlayerState] = useState<{
    isOpen: boolean;
    title: string;
    subtitle?: string;
    poster?: string;
    streams: StreamSource[];
  }>({ isOpen: false, title: "", streams: [] });

  useEffect(() => {
    const query = searchQuery.trim();
    if (query.length < 2) return;
    const timer = window.setTimeout(() => trackCompletedSearch(query), 800);
    return () => window.clearTimeout(timer);
  }, [searchQuery]);

  useEffect(() => {
    try {
      const saved = localStorage.getItem("mashad_favorites");
      if (saved) setFavorites(JSON.parse(saved));
    } catch { /* Ignore malformed local data and keep the list usable. */ }
  }, []);

  const handleToggleFavorite = (item: ArabseedItem) => {
    setFavorites((previous) => {
      const updated = previous.some((saved) => saved.id === item.id)
        ? previous.filter((saved) => saved.id !== item.id)
        : [item, ...previous];
      try { localStorage.setItem("mashad_favorites", JSON.stringify(updated)); } catch { /* Storage may be disabled. */ }
      return updated;
    });
  };

  useEffect(() => {
    let cancelled = false;
    async function fetchRecommendations() {
      try {
        const response = await fetch("/api/cinema/movies?page=1");
        const result = await response.json();
        if (!cancelled && result.success && Array.isArray(result.data)) setRecommendations(result.data.slice(0, 10));
      } catch { /* Recommendations are supplementary. */ }
    }
    void fetchRecommendations();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    let cancelled = false;
    async function fetchMatchesCount() {
      try {
        const response = await fetch("/api/football/matches");
        const result = await response.json();
        if (!cancelled && result.success && Array.isArray(result.data)) {
          setLiveMatchesCount(result.data.filter((match: { status?: string }) => match.status === "in").length);
        }
      } catch { /* The badge remains at zero when match data is unavailable. */ }
    }
    void fetchMatchesCount();
    const interval = setInterval(fetchMatchesCount, 45000);
    return () => { cancelled = true; clearInterval(interval); };
  }, []);

  const fetchDetail = async (item: ArabseedItem, requestId: number) => {
    try {
      const response = await fetch(`/api/cinema/detail?url=${encodeURIComponent(item.pageUrl)}`);
      const result = await response.json();
      if (requestId === detailRequestId.current && result.success && result.data) {
        const nextDetail = result.data as ArabseedDetail;
        setSelectedDetailData(nextDetail);
        return nextDetail;
      }
    } catch (error) {
      console.error("Failed to load media detail:", error);
    } finally {
      if (requestId === detailRequestId.current) setLoadingDetail(false);
    }
    return null;
  };

  const handleSelectItem = async (item: ArabseedItem) => {
    trackContentOpen(item.title, item.isSeries);
    const requestId = ++detailRequestId.current;
    setDetailHistory([]);
    setSelectedDetailItem(item);
    setSelectedDetailData(null);
    setSeriesReference(null);
    setEpisodeReference(null);
    setLoadingDetail(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
    const result = await fetchDetail(item, requestId);
    if (result && requestId === detailRequestId.current && (item.isSeries || result.item.isSeries)) {
      setSeriesReference(result);
      setEpisodeReference(result);
    }
  };

  const pushCurrentDetail = () => {
    if (!selectedDetailItem) return;
    setDetailHistory((previous) => [...previous, {
      item: selectedDetailItem,
      detail: selectedDetailData,
      seriesReference,
      episodeReference,
    }]);
  };

  const handleSelectPart = async (season: ArabseedSeason) => {
    if (!selectedDetailItem) return;
    pushCurrentDetail();
    const requestId = ++detailRequestId.current;
    const item = { ...selectedDetailItem, title: season.title, pageUrl: season.pageUrl, isSeries: true };
    setSelectedDetailItem(item);
    setSelectedDetailData(null);
    setLoadingDetail(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
    const result = await fetchDetail(item, requestId);
    if (result && requestId === detailRequestId.current) setEpisodeReference(result);
  };

  const handleSelectEpisode = async (episode: ArabseedEpisode) => {
    if (!selectedDetailItem) return;
    pushCurrentDetail();
    const requestId = ++detailRequestId.current;
    const parentEpisodeList = selectedDetailData?.episodes?.length ? selectedDetailData : episodeReference;
    if (parentEpisodeList) setEpisodeReference(parentEpisodeList);
    const item = { ...selectedDetailItem, title: episode.title, pageUrl: episode.pageUrl, isSeries: true };
    setSelectedDetailItem(item);
    setSelectedDetailData(null);
    setLoadingDetail(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
    await fetchDetail(item, requestId);
  };

  const handleBackFromDetail = () => {
    detailRequestId.current += 1;
    setLoadingDetail(false);
    const previous = detailHistory[detailHistory.length - 1];
    if (previous) {
      setDetailHistory((history) => history.slice(0, -1));
      setSelectedDetailItem(previous.item);
      setSelectedDetailData(previous.detail);
      setSeriesReference(previous.seriesReference);
      setEpisodeReference(previous.episodeReference);
      return;
    }
    setSelectedDetailItem(null);
    setSelectedDetailData(null);
    setSeriesReference(null);
    setEpisodeReference(null);
  };

  const handlePlayStream = (data: { title: string; subtitle?: string; poster?: string; streams: StreamSource[] }) => {
    setPlayerState({ isOpen: true, title: data.title, subtitle: data.subtitle, poster: data.poster, streams: data.streams });
  };

  const handlePlayServer = async (serverUrl: string, serverName: string) => {
    setResolvingServer(serverUrl);
    try {
      if (serverUrl.includes("/api/proxy/stream") || serverUrl.endsWith(".m3u8") || serverUrl.endsWith(".mp4")) {
        handlePlayStream({
          title: selectedDetailItem?.title || "مشاهدة العمل",
          subtitle: serverName,
          poster: selectedDetailItem?.poster,
          streams: [{ name: serverName, url: serverUrl }],
        });
        return;
      }
      const response = await fetch(`/api/cinema/resolve?serverUrl=${encodeURIComponent(serverUrl)}`);
      const result = await response.json();
      const playUrl = result.playUrl || result.data?.playUrl || serverUrl;
      handlePlayStream({
        title: selectedDetailItem?.title || "مشاهدة العمل",
        subtitle: serverName,
        poster: selectedDetailItem?.poster,
        streams: [{ name: serverName, url: playUrl }],
      });
    } catch {
      handlePlayStream({
        title: selectedDetailItem?.title || "مشاهدة العمل",
        subtitle: serverName,
        poster: selectedDetailItem?.poster,
        streams: [{ name: serverName, url: serverUrl }],
      });
    } finally {
      setResolvingServer(null);
    }
  };

  const changeTab = (tab: Tab) => {
    detailRequestId.current += 1;
    setActiveTab(tab);
    setSelectedDetailItem(null);
    setSelectedDetailData(null);
    setSeriesReference(null);
    setEpisodeReference(null);
    setDetailHistory([]);
    setSearchQuery("");
  };

  return (
    <div dir="rtl" className="min-h-screen bg-[#f5f7fb] font-sans text-slate-800 selection:bg-indigo-200 selection:text-indigo-950">
      <Navbar activeTab={activeTab} onTabChange={changeTab} searchQuery={searchQuery} onSearchChange={setSearchQuery} liveMatchesCount={liveMatchesCount} />
      <main className="mx-auto min-h-[calc(100vh-78px)] w-full max-w-[1440px] px-4 pb-28 pt-5 sm:px-6 sm:pt-8 lg:px-8 lg:pb-12">
        {selectedDetailItem ? (
          <DetailView
            item={selectedDetailItem}
            detail={selectedDetailData}
            loading={loadingDetail}
            onBack={handleBackFromDetail}
            onPlayServer={handlePlayServer}
            resolvingServer={resolvingServer}
            isFavorite={favorites.some((favorite) => favorite.id === selectedDetailItem.id)}
            onToggleFavorite={() => handleToggleFavorite(selectedDetailItem)}
            onSelectEpisode={handleSelectEpisode}
            onSelectPart={handleSelectPart}
            seriesReference={seriesReference}
            episodeReference={episodeReference}
            recommendations={recommendations}
            onSelectRecommendation={(recommendation) => { void handleSelectItem(recommendation); }}
          />
        ) : searchQuery.trim() ? (
          <CinemaView
            activeTab={activeTab === "movies" || activeTab === "series" || activeTab === "anime" ? activeTab : "home"}
            searchQuery={searchQuery}
            onSelectItem={(item) => { void handleSelectItem(item); }}
            onPlayStream={handlePlayStream}
            favorites={favorites}
            onToggleFavorite={handleToggleFavorite}
            onBrowseTab={changeTab}
          />
        ) : activeTab === "matches" ? (
          <LiveTvView searchQuery={searchQuery} onPlayStream={handlePlayStream} />
        ) : activeTab === "favorites" ? (
          <section className="space-y-5">
            <div className="flex items-end justify-between gap-3"><div><p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-rose-600">محفوظاتك</p><h1 className="mt-1 text-2xl font-black text-slate-900">قائمتي <span className="text-base font-bold text-slate-400">({favorites.length})</span></h1></div>{favorites.length > 0 && <button type="button" onClick={() => { setFavorites([]); localStorage.removeItem("mashad_favorites"); }} className="inline-flex items-center gap-2 rounded-xl border border-rose-100 bg-white px-3.5 py-2.5 text-xs font-bold text-rose-600 hover:bg-rose-50"><Trash2 size={14} /> مسح القائمة</button>}</div>
            {favorites.length === 0 ? <div className="rounded-[26px] border border-dashed border-slate-300 bg-white px-5 py-16 text-center"><Bookmark size={38} className="mx-auto text-slate-300" /><h2 className="mt-3 font-extrabold text-slate-800">لسه ما حفظتش أعمال</h2><p className="mt-1 text-sm text-slate-500">اضغط على القلب في أي فيلم أو مسلسل عشان تلاقيه هنا.</p><button onClick={() => changeTab("home")} className="mt-5 rounded-xl bg-indigo-600 px-4 py-2.5 text-xs font-extrabold text-white">اكتشف المكتبة</button></div> : <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-5 xl:grid-cols-6">{favorites.map((item) => <article key={item.id} onClick={() => { void handleSelectItem(item); }} className="group cursor-pointer overflow-hidden rounded-[20px] border border-slate-200 bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-lg"><div className="relative"><img src={item.poster || "/logo.jpg"} alt={item.title} className="aspect-[0.72] w-full object-cover" onError={(event) => { event.currentTarget.src = "/logo.jpg"; }} /><span className="absolute right-2 top-2 rounded-full bg-white/95 px-2 py-1 text-[9px] font-extrabold text-slate-700">{item.quality || (item.isSeries ? "مسلسل" : "فيلم")}</span><button type="button" onClick={(event) => { event.stopPropagation(); handleToggleFavorite(item); }} aria-label="إزالة من قائمتي" className="absolute left-2 top-2 grid h-8 w-8 place-items-center rounded-full bg-white text-rose-600 shadow-sm"><Trash2 size={14} /></button><span className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-slate-950/85 to-transparent p-3 pt-10 text-xs font-extrabold text-white">{item.title}</span></div><div className="flex items-center justify-between p-3 text-[10px] font-bold text-slate-500"><span>{item.category || (item.isSeries ? "مسلسل" : "فيلم")}</span>{item.rating && <span className="inline-flex items-center gap-1"><Star size={11} className="fill-amber-400 text-amber-400" />{item.rating}</span>}</div></article>)}</div>}
          </section>
        ) : (
          <CinemaView activeTab={activeTab} searchQuery={searchQuery} onSelectItem={(item) => { void handleSelectItem(item); }} onPlayStream={handlePlayStream} favorites={favorites} onToggleFavorite={handleToggleFavorite} onBrowseTab={changeTab} />
        )}
      </main>
      {playerState.isOpen && <PlayerModal title={playerState.title} subtitle={playerState.subtitle} poster={playerState.poster} streams={playerState.streams} onClose={() => setPlayerState((state) => ({ ...state, isOpen: false }))} />}
    </div>
  );
}
