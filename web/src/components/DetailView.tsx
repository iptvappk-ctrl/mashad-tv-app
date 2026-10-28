"use client";

import React from "react";
import {
  ArrowRight,
  Check,
  Clapperboard,
  Heart,
  LoaderCircle,
  Play,
  Radio,
  Share2,
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
  const servers = detail?.servers || [];
  const seasons = seriesReference?.seasons?.length ? seriesReference.seasons : detail?.seasons || [];
  const episodes = episodeReference?.episodes?.length ? episodeReference.episodes : detail?.episodes || [];
  const cast = detail?.cast?.length ? detail.cast : seriesReference?.cast || [];
  const currentEpisode = episodes.find((episode) => episode.pageUrl === item.pageUrl);

  return (
    <div className="space-y-5 pb-10 sm:space-y-7">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0 text-xs font-semibold text-slate-500">
          <button type="button" onClick={onBack} className="hover:text-indigo-700">الرئيسية</button>
          <span className="mx-2 text-slate-300">/</span>
          <span>{item.isSeries ? "المسلسلات" : "الأفلام"}</span>
          <span className="mx-2 text-slate-300">/</span>
          <span className="font-extrabold text-slate-800">{item.title}</span>
        </div>
        <button type="button" onClick={onBack} className="inline-flex shrink-0 items-center gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-xs font-bold text-slate-600 transition hover:border-indigo-200 hover:text-indigo-700">
          <ArrowRight size={15} /> رجوع
        </button>
      </div>

      <section className="grid gap-5 rounded-[28px] border border-slate-200 bg-white p-4 shadow-[0_10px_34px_rgba(35,49,78,0.06)] sm:p-6 lg:grid-cols-[210px_1fr] lg:gap-8 lg:p-8">
        <div className="relative mx-auto w-36 shrink-0 overflow-hidden rounded-[20px] bg-slate-100 shadow-lg sm:w-44 lg:mx-0 lg:w-full">
          <img src={item.poster || "/logo.jpg"} alt={item.title} className="aspect-[0.72] w-full object-cover" onError={(event) => { event.currentTarget.src = "/logo.jpg"; }} />
          {item.quality && <span className="absolute right-2.5 top-2.5 rounded-full bg-white/95 px-2.5 py-1 text-[10px] font-extrabold text-slate-700">{item.quality}</span>}
        </div>
        <div className="flex min-w-0 flex-col justify-center text-center lg:text-right">
          <div className="flex flex-wrap items-center justify-center gap-2 lg:justify-start">
            <span className="rounded-full bg-indigo-50 px-3 py-1.5 text-[11px] font-extrabold text-indigo-700">{item.isSeries ? "مسلسل" : "فيلم"}</span>
            {item.category && <span className="rounded-full bg-slate-100 px-3 py-1.5 text-[11px] font-bold text-slate-600">{item.category}</span>}
            {item.year && <span className="rounded-full bg-slate-100 px-3 py-1.5 text-[11px] font-bold text-slate-600">{item.year}</span>}
            {item.rating && <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-3 py-1.5 text-[11px] font-extrabold text-amber-700"><Star size={12} className="fill-current" />{item.rating}</span>}
          </div>
          <h1 className="mt-4 text-2xl font-black leading-tight tracking-tight text-slate-900 sm:text-3xl lg:text-4xl">{item.title}</h1>
          <p className="mx-auto mt-3 max-w-3xl text-sm leading-7 text-slate-600 lg:mx-0">
            {loading && !detail ? "بنحمّل تفاصيل العمل والحلقات..." : detail?.overview || "معلومات القصة غير متاحة من المصدر حاليًا."}
          </p>
          <div className="mt-5 flex flex-wrap justify-center gap-2.5 lg:justify-start">
            {servers[0] && (
              <button type="button" onClick={() => onPlayServer(servers[0].embedUrl, servers[0].name)} className="inline-flex items-center gap-2 rounded-2xl bg-indigo-600 px-5 py-3 text-sm font-extrabold text-white shadow-md shadow-indigo-200 transition hover:bg-indigo-700">
                <Play size={16} className="fill-current" /> شاهد الآن
              </button>
            )}
            {!servers.length && detail?.proxiedStream && (
              <button type="button" onClick={() => onPlayServer(detail.proxiedStream!, "السيرفر المباشر")} className="inline-flex items-center gap-2 rounded-2xl bg-indigo-600 px-5 py-3 text-sm font-extrabold text-white"><Play size={16} className="fill-current" /> شاهد الآن</button>
            )}
            <button type="button" onClick={onToggleFavorite} className={`inline-flex items-center gap-2 rounded-2xl border px-4 py-3 text-sm font-bold transition ${isFavorite ? "border-rose-200 bg-rose-50 text-rose-600" : "border-slate-200 bg-white text-slate-700 hover:border-rose-200 hover:text-rose-600"}`}>
              <Heart size={16} className={isFavorite ? "fill-current" : ""} />{isFavorite ? "في قائمتي" : "أضف لقائمتي"}
            </button>
            <button type="button" onClick={() => { void navigator.share?.({ title: item.title, url: window.location.href }); }} className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-bold text-slate-700 hover:border-indigo-200 hover:text-indigo-700">
              <Share2 size={16} /> مشاركة
            </button>
          </div>
        </div>
      </section>

      <section className="rounded-[26px] border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-indigo-600">اختار الجودة المناسبة</p>
            <h2 className="mt-1 flex items-center gap-2 text-lg font-black text-slate-900"><Radio size={18} className="text-indigo-600" /> سيرفرات المشاهدة</h2>
          </div>
          {loading && <LoaderCircle size={18} className="animate-spin text-indigo-600" />}
        </div>
        {servers.length > 0 ? (
          <div className="mt-5 grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-3">
            {servers.map((server, index) => (
              <button key={`${server.embedUrl}-${index}`} type="button" disabled={resolvingServer === server.embedUrl} onClick={() => onPlayServer(server.embedUrl, server.name)} className="flex items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3.5 text-right transition hover:border-indigo-200 hover:bg-indigo-50 disabled:opacity-60">
                <span className="flex items-center gap-3"><span className="grid h-9 w-9 place-items-center rounded-xl bg-white text-indigo-700 shadow-sm"><Play size={14} className="fill-current" /></span><span><b className="block text-sm font-extrabold text-slate-800">{server.name || `سيرفر ${index + 1}`}</b><small className="text-[10px] font-semibold text-slate-500">تشغيل مباشر</small></span></span>
                {resolvingServer === server.embedUrl ? <LoaderCircle size={16} className="animate-spin text-indigo-600" /> : <ArrowRight size={15} className="rotate-180 text-slate-400" />}
              </button>
            ))}
          </div>
        ) : (
          <p className="mt-4 rounded-2xl bg-slate-50 p-4 text-sm text-slate-500">{loading ? "جاري تجهيز خيارات المشاهدة..." : "مفيش سيرفرات متاحة للعمل ده حاليًا."}</p>
        )}
      </section>

      {seasons.length > 0 && (
        <section className="rounded-[26px] border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
          <div className="mb-4 flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-2xl bg-violet-50 text-violet-700"><Tv size={19} /></span><div><p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-violet-600">الأجزاء والمواسم</p><h2 className="text-lg font-black text-slate-900">اختار الجزء</h2></div></div>
          <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-4">
            {seasons.map((season, index) => {
              const active = season.pageUrl === item.pageUrl;
              return (
                <button key={`${season.pageUrl}-${index}`} type="button" onClick={() => onSelectPart(season)} className={`flex min-h-14 items-center justify-between gap-2 rounded-2xl border px-4 py-3 text-right transition ${active ? "border-indigo-200 bg-indigo-50 text-indigo-800" : "border-slate-200 bg-slate-50 text-slate-700 hover:border-indigo-200 hover:bg-white"}`}>
                  <span className="text-sm font-extrabold">{season.title || `الجزء ${index + 1}`}</span>
                  {active ? <Check size={16} /> : <ArrowRight size={15} className="rotate-180 text-slate-400" />}
                </button>
              );
            })}
          </div>
        </section>
      )}

      {episodes.length > 0 && (
        <section className="rounded-[26px] border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
          <div className="mb-4 flex items-center justify-between gap-3"><div className="flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-2xl bg-emerald-50 text-emerald-700"><Clapperboard size={19} /></span><div><p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-emerald-700">{episodeReference?.item.title || detail?.item.title || "الحلقات"}</p><h2 className="text-lg font-black text-slate-900">الحلقات المتاحة <span className="text-sm font-bold text-slate-400">({episodes.length})</span></h2></div></div>{currentEpisode && <span className="hidden rounded-full bg-emerald-50 px-3 py-1.5 text-[11px] font-bold text-emerald-700 sm:block">بتتفرج على {currentEpisode.title}</span>}</div>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {episodes.map((episode, index) => {
              const active = episode.pageUrl === item.pageUrl;
              return <button key={`${episode.pageUrl}-${index}`} type="button" onClick={() => onSelectEpisode(episode)} className={`flex min-h-14 items-center gap-2.5 rounded-2xl border px-3 py-2.5 text-right transition ${active ? "border-emerald-200 bg-emerald-50 text-emerald-800" : "border-slate-200 bg-white text-slate-700 hover:border-emerald-200 hover:bg-emerald-50/60"}`}><span className={`grid h-8 w-8 shrink-0 place-items-center rounded-xl text-xs font-black ${active ? "bg-emerald-600 text-white" : "bg-slate-100 text-slate-500"}`}>{episode.episodeNumber || index + 1}</span><span className="line-clamp-2 text-xs font-extrabold leading-5">{episode.title}</span></button>;
            })}
          </div>
        </section>
      )}

      <section className="rounded-[26px] border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
        <h2 className="flex items-center gap-2 text-lg font-black text-slate-900"><Clapperboard size={18} className="text-indigo-600" /> طاقم العمل</h2>
        {cast.length ? <div className="mt-4 flex flex-wrap gap-2">{cast.map((member, index) => <span key={`${member}-${index}`} className="rounded-full bg-slate-100 px-3.5 py-2 text-xs font-bold text-slate-700">{member}</span>)}</div> : <p className="mt-2 text-sm text-slate-500">تفاصيل طاقم العمل غير متاحة من مصدر العمل حاليًا.</p>}
      </section>

      {recommendations.length > 0 && (
        <section className="space-y-4"><div><p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-indigo-600">ممكن يعجبك</p><h2 className="mt-1 text-xl font-black text-slate-900">أعمال قريبة من اختيارك</h2></div><div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">{recommendations.slice(0, 5).map((recommendation) => <button key={`${recommendation.id}-${recommendation.providerId || ""}`} type="button" onClick={() => onSelectRecommendation(recommendation)} className="group overflow-hidden rounded-[20px] border border-slate-200 bg-white text-right shadow-sm transition hover:-translate-y-1 hover:shadow-lg"><img src={recommendation.poster || "/logo.jpg"} alt={recommendation.title} className="aspect-[0.75] w-full object-cover" onError={(event) => { event.currentTarget.src = "/logo.jpg"; }} /><span className="block p-3 text-xs font-extrabold leading-5 text-slate-800 group-hover:text-indigo-700">{recommendation.title}</span></button>)}</div></section>
      )}
    </div>
  );
}
