"use client";

import React, { useState } from "react";
import {
  ArrowRight,
  Check,
  Clapperboard,
  Download,
  Heart,
  LoaderCircle,
  Play,
  Radio,
  Share2,
  Sparkles,
  Star,
  Tv,
} from "lucide-react";
import { ArabseedDetail, ArabseedEpisode, ArabseedItem, ArabseedSeason } from "@/lib/arabseed";

interface DetailViewProps {
  item: ArabseedItem;
  detail: ArabseedDetail | null;
  loading: boolean;
  onBack: () => void;
  onPlayServer: (serverUrl: string, serverName: string) => void;
  resolvingServer: string | null;
  isFavorite: boolean;
  onToggleFavorite: () => void;
  onSelectEpisode: (episode: ArabseedEpisode) => void;
  onSelectPart: (season: ArabseedSeason) => void;
  seriesReference: ArabseedDetail | null;
  episodeReference: ArabseedDetail | null;
  recommendations: ArabseedItem[];
  onSelectRecommendation: (recommendation: ArabseedItem) => void;
}

export default function DetailView({
  item,
  detail,
  loading,
  onBack,
  onPlayServer,
  resolvingServer,
  isFavorite,
  onToggleFavorite,
  onSelectEpisode,
  onSelectPart,
  seriesReference,
  episodeReference,
  recommendations,
  onSelectRecommendation,
}: DetailViewProps) {
  const [downloading, setDownloading] = useState(false);
  const [downloadNotice, setDownloadNotice] = useState<string | null>(null);

  const rawServers = detail?.servers || [];
  const rawSeasons = seriesReference?.seasons?.length ? seriesReference.seasons : detail?.seasons || [];
  const episodes = episodeReference?.episodes?.length ? episodeReference.episodes : detail?.episodes || [];
  const cast = detail?.cast?.length ? detail.cast : seriesReference?.cast || [];

  // Filter out any episode from seasons so "الأجزاء والمواسم الأخرى" only contains true seasons/parts
  const seasons = rawSeasons.filter((s) => {
    const t = (s.title + " " + (s.pageUrl || "")).toLowerCase();
    return !/الحلقة|حلقة|episode|ep[-_/\d]|%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9/i.test(t);
  });

  const currentEpisode = episodes.find((episode) => episode.pageUrl === item.pageUrl);
  const currentEpIdx = episodes.findIndex((episode) => episode.pageUrl === item.pageUrl);

  const handleDirectDownload = async () => {
    setDownloading(true);
    setDownloadNotice("جاري فك تشفير وتجهيز الرابط المباشر...");
    try {
      let downloadUrl = detail?.directStream || detail?.proxiedStream;

      if (!downloadUrl && rawServers.length > 0) {
        const primaryServer = rawServers[0].embedUrl;
        const res = await fetch(`/api/cinema/resolve?serverUrl=${encodeURIComponent(primaryServer)}`);
        const json = await res.json();
        downloadUrl = json.playUrl || json.rawUrl || primaryServer;
      }

      if (!downloadUrl) {
        downloadUrl = item.pageUrl;
      }

      // Trigger browser direct file download
      const safeTitle = `${item.title.replace(/[\/\\?%*:|"<>]/g, "_")}.mp4`;
      const link = document.createElement("a");
      link.href = downloadUrl;
      link.setAttribute("download", safeTitle);
      link.setAttribute("target", "_blank");
      link.setAttribute("rel", "noopener noreferrer");
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      setDownloadNotice("بدأ التحميل بنجاح في جهازك!");
      setTimeout(() => setDownloadNotice(null), 4000);
    } catch (err) {
      console.error("Direct download error:", err);
      setDownloadNotice("تعذر التحميل المباشر من هذا السيرفر، يرجى اختيار سيرفر آخر.");
      setTimeout(() => setDownloadNotice(null), 4000);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="space-y-6 pb-12 sm:space-y-8">
      {/* Top Breadcrumb & Back */}
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0 text-xs font-semibold text-slate-400">
          <button type="button" onClick={onBack} className="hover:text-amber-400 transition">الرئيسية</button>
          <span className="mx-2 text-slate-600">/</span>
          <span>{item.isSeries ? "المسلسلات" : "الأفلام"}</span>
          <span className="mx-2 text-slate-600">/</span>
          <span className="font-extrabold text-amber-300 truncate">{item.title}</span>
        </div>
        <button
          type="button"
          onClick={onBack}
          className="inline-flex shrink-0 items-center gap-2 rounded-2xl border border-amber-500/25 bg-[#12131D] px-4 py-2.5 text-xs font-bold text-amber-300 transition hover:border-amber-400 hover:bg-amber-500/10 shadow-sm"
        >
          <ArrowRight size={15} /> رجوع
        </button>
      </div>

      {/* Main Hero Card */}
      <section className="relative overflow-hidden rounded-[30px] border border-amber-500/20 bg-[#12131D] p-5 shadow-[0_15px_45px_rgba(0,0,0,0.6)] sm:p-7 lg:grid lg:grid-cols-[240px_1fr] lg:gap-8 lg:p-8">
        <div className="relative mx-auto w-40 shrink-0 overflow-hidden rounded-[22px] border border-amber-500/30 bg-[#090A0E] shadow-[0_10px_30px_rgba(212,175,55,0.25)] sm:w-52 lg:mx-0 lg:w-full">
          <img
            src={item.poster || "/logo.jpg"}
            alt={item.title}
            className="aspect-[0.72] w-full object-cover"
            onError={(event) => { event.currentTarget.src = "/logo.jpg"; }}
          />
          {item.quality && (
            <span className="absolute right-2.5 top-2.5 rounded-full border border-amber-500/30 bg-[#090A0E]/90 px-2.5 py-1 text-[10px] font-extrabold text-amber-300 backdrop-blur-md">
              {item.quality}
            </span>
          )}
        </div>

        <div className="flex min-w-0 flex-col justify-center text-center lg:text-right mt-5 lg:mt-0">
          <div className="flex flex-wrap items-center justify-center gap-2 lg:justify-start">
            <span className="rounded-full border border-amber-500/30 bg-amber-500/15 px-3 py-1 text-[11px] font-black text-amber-300">
              {item.isSeries ? "مسلسل" : "فيلم"}
            </span>
            {item.category && (
              <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-[11px] font-bold text-slate-300">
                {item.category}
              </span>
            )}
            {item.year && (
              <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-[11px] font-bold text-slate-300">
                {item.year}
              </span>
            )}
            {item.rating && (
              <span className="inline-flex items-center gap-1 rounded-full border border-amber-500/25 bg-[#090A0E]/80 px-3 py-1 text-[11px] font-extrabold text-amber-400">
                <Star size={12} className="fill-current" />{item.rating}
              </span>
            )}
          </div>

          <h1 className="mt-4 text-2xl font-black leading-tight tracking-tight text-white sm:text-3xl lg:text-4xl">
            {item.title}
          </h1>

          <p className="mx-auto mt-3 max-w-3xl text-sm leading-relaxed text-slate-300 lg:mx-0">
            {loading && !detail ? "جاري تحميل تفاصيل العمل وخيارات المشاهدة..." : detail?.overview || "استمتع بمشاهدة وتحميل هذا العمل بأعلى جودة متوفرة."}
          </p>

          {/* Action Buttons */}
          <div className="mt-6 flex flex-wrap justify-center gap-3 lg:justify-start">
            {rawServers[0] && (
              <button
                type="button"
                onClick={() => onPlayServer(rawServers[0].embedUrl, rawServers[0].name)}
                className="inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-amber-400 via-amber-500 to-amber-600 px-6 py-3.5 text-sm font-black text-slate-950 shadow-lg shadow-amber-500/30 transition hover:scale-105 active:scale-95"
              >
                <Play size={16} className="fill-current" /> شاهد الآن
              </button>
            )}
            {!rawServers.length && detail?.proxiedStream && (
              <button
                type="button"
                onClick={() => onPlayServer(detail.proxiedStream!, "السيرفر المباشر")}
                className="inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-amber-400 via-amber-500 to-amber-600 px-6 py-3.5 text-sm font-black text-slate-950 shadow-lg shadow-amber-500/30"
              >
                <Play size={16} className="fill-current" /> شاهد الآن
              </button>
            )}
            <button
              type="button"
              onClick={handleDirectDownload}
              disabled={downloading}
              className="inline-flex items-center gap-2 rounded-2xl border border-amber-500/40 bg-gradient-to-r from-amber-500/20 to-amber-500/10 px-5 py-3.5 text-sm font-black text-amber-300 transition hover:border-amber-400 hover:text-white"
            >
              {downloading ? <LoaderCircle size={16} className="animate-spin text-amber-400" /> : <Download size={16} className="stroke-[2.5]" />}
              <span>تحميل مباشر</span>
            </button>
            <button
              type="button"
              onClick={onToggleFavorite}
              className={`inline-flex items-center gap-2 rounded-2xl border px-5 py-3.5 text-sm font-bold transition ${
                isFavorite
                  ? "border-rose-500 bg-rose-600 text-white shadow-rose-600/30"
                  : "border-white/10 bg-[#161726] text-slate-300 hover:border-rose-400 hover:text-rose-400"
              }`}
            >
              <Heart size={16} className={isFavorite ? "fill-current" : ""} />
              {isFavorite ? "في قائمتي" : "أضف لقائمتي"}
            </button>
            <button
              type="button"
              onClick={() => { void navigator.share?.({ title: item.title, url: window.location.href }); }}
              className="inline-flex items-center gap-2 rounded-2xl border border-white/10 bg-[#161726] px-5 py-3.5 text-sm font-bold text-slate-300 hover:border-amber-400 hover:text-amber-300 transition"
            >
              <Share2 size={16} /> مشاركة
            </button>
          </div>

          {/* Download Notice Banner */}
          {downloadNotice && (
            <div className="mt-3 inline-flex items-center gap-2 rounded-xl border border-amber-500/30 bg-amber-500/15 px-4 py-2 text-xs font-bold text-amber-300 animate-fadeIn">
              <Sparkles size={14} className="text-amber-400" />
              <span>{downloadNotice}</span>
            </div>
          )}
        </div>
      </section>

      {/* 🚀 PROMINENT DIRECT DOWNLOAD SECTION (Above Servers) */}
      <section className="rounded-[26px] border border-amber-500/30 bg-gradient-to-r from-amber-500/15 via-[#161728] to-[#12131D] p-5 sm:p-6 shadow-[0_8px_30px_rgba(0,0,0,0.5)]">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-gradient-to-br from-amber-400 to-amber-600 text-slate-950 font-black shadow-lg shadow-amber-500/30">
              <Download size={22} className="stroke-[2.5]" />
            </div>
            <div>
              <div className="flex items-center gap-2.5">
                <h3 className="text-base font-black text-white">تحميل مباشر إلى الهاتف والجهاز</h3>
                <span className="rounded-full border border-emerald-500/30 bg-emerald-500/20 px-2.5 py-0.5 text-[10px] font-extrabold text-emerald-400">
                  سريع ومباشر MP4
                </span>
              </div>
              <p className="mt-0.5 text-xs text-slate-400">
                تنزيل فوري بأعلى جودة وبدون إعلانات مزعجة مباشرة إلى ذاكرة جهازك
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleDirectDownload}
            disabled={downloading}
            className="inline-flex shrink-0 items-center justify-center gap-2.5 rounded-2xl bg-gradient-to-r from-amber-400 via-amber-500 to-amber-600 px-6 py-3.5 text-sm font-black text-slate-950 shadow-md shadow-amber-500/25 transition hover:scale-105 active:scale-95 disabled:opacity-50"
          >
            {downloading ? (
              <>
                <LoaderCircle size={17} className="animate-spin text-slate-950" />
                <span>جاري استخراج الرابط...</span>
              </>
            ) : (
              <>
                <Download size={17} className="stroke-[2.5]" />
                <span>تحميل الآن مباشر</span>
              </>
            )}
          </button>
        </div>
      </section>

      {/* Streaming Servers */}
      <section className="rounded-[26px] border border-amber-500/20 bg-[#12131D] p-5 shadow-sm sm:p-6">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-[10px] font-extrabold uppercase tracking-widest text-amber-400">المشاهدة أونلاين</p>
            <h2 className="mt-0.5 flex items-center gap-2 text-lg font-black text-white">
              <Radio size={18} className="text-amber-400" /> سيرفرات المشاهدة
            </h2>
          </div>
          {loading && <LoaderCircle size={18} className="animate-spin text-amber-400" />}
        </div>

        {rawServers.length > 0 ? (
          <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {rawServers.map((server, index) => (
              <button
                key={`${server.embedUrl}-${index}`}
                type="button"
                disabled={resolvingServer === server.embedUrl}
                onClick={() => onPlayServer(server.embedUrl, server.name)}
                className="flex items-center justify-between gap-3 rounded-2xl border border-white/5 bg-[#161726] px-4 py-3.5 text-right transition hover:border-amber-400/60 hover:bg-amber-500/10 hover:shadow-[0_0_20px_rgba(212,175,55,0.15)] disabled:opacity-50 group"
              >
                <span className="flex items-center gap-3">
                  <span className="grid h-10 w-10 place-items-center rounded-xl border border-amber-500/25 bg-amber-500/15 text-amber-400 shadow-sm transition group-hover:scale-105 group-hover:bg-amber-500 group-hover:text-slate-950">
                    <Play size={15} className="fill-current" />
                  </span>
                  <span>
                    <b className="block text-sm font-extrabold text-white group-hover:text-amber-300 transition">
                      {server.name || `سيرفر ${index + 1}`}
                    </b>
                    <small className="text-[10px] font-semibold text-slate-400">بث فائق الجودة</small>
                  </span>
                </span>
                {resolvingServer === server.embedUrl ? (
                  <LoaderCircle size={16} className="animate-spin text-amber-400" />
                ) : (
                  <ArrowRight size={15} className="rotate-180 text-amber-400/60 group-hover:text-amber-400 transition" />
                )}
              </button>
            ))}
          </div>
        ) : (
          <p className="mt-4 rounded-2xl border border-white/5 bg-[#161726] p-4 text-sm text-slate-400">
            {loading ? "جاري تجهيز سيرفرات المشاهدة..." : "لا توجد سيرفرات مشاهدة متاحة حاليًا."}
          </p>
        )}
      </section>

      {/* Seasons / Parts Section (Strictly filtered, no single episodes) */}
      {seasons.length > 0 && (
        <section className="rounded-[26px] border border-amber-500/20 bg-[#12131D] p-5 shadow-sm sm:p-6">
          <div className="mb-4 flex items-center gap-3">
            <span className="grid h-10 w-10 place-items-center rounded-2xl border border-amber-500/30 bg-amber-500/15 text-amber-400">
              <Tv size={19} />
            </span>
            <div>
              <p className="text-[10px] font-extrabold uppercase tracking-widest text-amber-400">الأجزاء والمواسم الأخرى</p>
              <h2 className="text-lg font-black text-white">اختر الموسم أو الجزء</h2>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-4">
            {seasons.map((season, index) => {
              const active = season.pageUrl === item.pageUrl;
              return (
                <button
                  key={`${season.pageUrl}-${index}`}
                  type="button"
                  onClick={() => onSelectPart(season)}
                  className={`flex min-h-14 items-center justify-between gap-2 rounded-2xl border px-4 py-3 text-right transition ${
                    active
                      ? "border-amber-400 bg-amber-500/20 text-amber-300 shadow-[0_0_15px_rgba(212,175,55,0.2)]"
                      : "border-white/5 bg-[#161726] text-slate-300 hover:border-amber-500/40 hover:bg-[#1A1C2E] hover:text-white"
                  }`}
                >
                  <span className="text-sm font-extrabold">{season.title || `الجزء ${index + 1}`}</span>
                  {active ? <Check size={16} className="text-amber-400" /> : <ArrowRight size={15} className="rotate-180 text-slate-500" />}
                </button>
              );
            })}
          </div>
        </section>
      )}

      {/* Available Episodes Section */}
      {episodes.length > 0 && (
        <section className="rounded-[26px] border border-amber-500/20 bg-[#12131D] p-5 shadow-sm sm:p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <span className="grid h-10 w-10 place-items-center rounded-2xl border border-amber-500/30 bg-amber-500/15 text-amber-400">
                <Clapperboard size={19} />
              </span>
              <div>
                <p className="text-[10px] font-extrabold uppercase tracking-widest text-amber-400">
                  {episodeReference?.item.title || detail?.item.title || "حلقات المسلسل"}
                </p>
                <h2 className="text-lg font-black text-white">
                  الحلقات المتاحة <span className="text-sm font-bold text-slate-400">({episodes.length})</span>
                </h2>
              </div>
            </div>

            {/* Quick Next / Prev navigation bar if current episode is detected */}
            {currentEpIdx >= 0 && (
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={currentEpIdx <= 0}
                  onClick={() => onSelectEpisode(episodes[currentEpIdx - 1])}
                  className="rounded-xl border border-amber-500/20 bg-[#161726] px-3 py-1.5 text-xs font-bold text-slate-300 transition hover:border-amber-400 hover:text-amber-300 disabled:opacity-40"
                >
                  الحلقة السابقة
                </button>
                <span className="rounded-lg border border-amber-500/30 bg-amber-500/15 px-2.5 py-1 text-xs font-black text-amber-300">
                  {episodes[currentEpIdx].title}
                </span>
                <button
                  type="button"
                  disabled={currentEpIdx >= episodes.length - 1}
                  onClick={() => onSelectEpisode(episodes[currentEpIdx + 1])}
                  className="rounded-xl border border-amber-500/20 bg-[#161726] px-3 py-1.5 text-xs font-bold text-slate-300 transition hover:border-amber-400 hover:text-amber-300 disabled:opacity-40"
                >
                  الحلقة التالية
                </button>
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
            {episodes.map((episode, index) => {
              const active = episode.pageUrl === item.pageUrl;
              return (
                <button
                  key={`${episode.pageUrl}-${index}`}
                  type="button"
                  onClick={() => onSelectEpisode(episode)}
                  className={`group relative flex items-center justify-between rounded-2xl border p-3.5 text-right transition duration-200 ${
                    active
                      ? "border-amber-400 bg-amber-500/20 text-amber-300 shadow-[0_0_15px_rgba(212,175,55,0.25)]"
                      : "border-white/5 bg-[#161726] text-slate-300 hover:border-amber-500/40 hover:bg-[#1A1C2E] hover:text-white"
                  }`}
                >
                  <span className="flex items-center gap-2.5">
                    <span className={`grid h-7 w-7 place-items-center rounded-lg text-xs font-black transition ${
                      active ? "bg-amber-400 text-slate-950 font-black" : "bg-white/10 text-slate-400 group-hover:bg-amber-500/20 group-hover:text-amber-300"
                    }`}>
                      {episode.episodeNumber || index + 1}
                    </span>
                    <span className="text-xs font-extrabold truncate max-w-[90px] sm:max-w-[110px]">
                      {episode.title}
                    </span>
                  </span>
                  <Play size={13} className={`shrink-0 ${active ? "fill-current text-amber-400" : "text-slate-500 group-hover:text-amber-300"}`} />
                </button>
              );
            })}
          </div>
        </section>
      )}

      {/* Recommendations Section */}
      {recommendations.length > 0 && (
        <section className="space-y-4 pt-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[10px] font-extrabold uppercase tracking-widest text-amber-400">أعمال مشابهة</p>
              <h3 className="text-xl font-black text-white">قد يعجبك أيضاً</h3>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
            {recommendations.slice(0, 6).map((rec) => (
              <article
                key={rec.id}
                onClick={() => onSelectRecommendation(rec)}
                className="group cursor-pointer overflow-hidden rounded-[20px] border border-amber-500/15 bg-[#12131D] shadow-sm transition hover:-translate-y-1 hover:border-amber-400/60"
              >
                <div className="relative aspect-[0.72] overflow-hidden bg-[#0A0B10]">
                  <img
                    src={rec.poster || "/logo.jpg"}
                    alt={rec.title}
                    className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
                    onError={(event) => { event.currentTarget.src = "/logo.jpg"; }}
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-[#090A0E] via-transparent to-transparent" />
                  <span className="absolute bottom-2 inset-x-2 text-xs font-extrabold text-white truncate group-hover:text-amber-300">
                    {rec.title}
                  </span>
                </div>
              </article>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
