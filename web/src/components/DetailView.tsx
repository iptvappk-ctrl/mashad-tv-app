"use client";

import React, { useState } from "react";
import {
  Play,
  Heart,
  AlertTriangle,
  Download,
  Share2,
  Tv,
  Film,
  Calendar,
  Clock,
  Star,
  Globe,
  Loader2,
  Check,
  Send,
  MessageCircle,
} from "lucide-react";
import { ArabseedDetail, ArabseedItem } from "@/lib/arabseed";

interface DetailViewProps {
  item: ArabseedItem;
  detail: ArabseedDetail | null;
  loading: boolean;
  onBack: () => void;
  onPlayServer: (serverUrl: string, serverName: string) => void;
  resolvingServer: string | null;
  isFavorite: boolean;
  onToggleFavorite: () => void;
  onSelectEpisode: (epUrl: string, epTitle: string) => void;
  recommendations: ArabseedItem[];
  onSelectRecommendation: (rec: ArabseedItem) => void;
}

const SERVER_COLORS = [
  "bg-[#2563EB] hover:bg-[#1D4ED8]", // Server 1: Blue
  "bg-[#8B5CF6] hover:bg-[#7C3AED]", // Server 2: Purple
  "bg-[#F97316] hover:bg-[#EA580C]", // Server 3: Orange
  "bg-[#EF4444] hover:bg-[#DC2626]", // Server 4: Red
  "bg-[#F59E0B] hover:bg-[#D97706]", // Server 5: Amber
  "bg-[#06B6D4] hover:bg-[#0891B2]", // Server 6: Cyan
];

const CAST_MEMBERS = [
  { name: "أحمد حلمي", role: "ممثل" },
  { name: "منى زكي", role: "ممثلة" },
  { name: "ماجد الكدواني", role: "ممثل" },
  { name: "كندة علوش", role: "ممثلة" },
  { name: "معتز هشام", role: "ممثل" },
  { name: "ياسمين العبد", role: "ممثلة" },
];

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
  recommendations,
  onSelectRecommendation,
}: DetailViewProps) {
  const [selectedServerIndex, setSelectedServerIndex] = useState(0);

  const servers = detail?.servers || [];
  const episodes = detail?.episodes || [];

  return (
    <div className="space-y-8 animate-fadeIn pb-24">
      {/* ── Breadcrumb Bar (Matching Wecima Image 1) ────────────────── */}
      <div className="flex items-center justify-between flex-wrap gap-2 text-xs font-bold text-gray-400 bg-[#121A28] p-3 rounded-2xl border border-[#1E293B]">
        <div className="flex items-center gap-2 flex-wrap">
          <button onClick={onBack} className="hover:text-white transition">
            مَشْهَد TV
          </button>
          <span>›</span>
          <span className="text-gray-300">{item.isSeries ? "مسلسلات" : "أفلام"}</span>
          <span>›</span>
          <span className="text-gray-300">{item.category}</span>
          <span>›</span>
          <span className="text-[#3B82F6] font-black line-clamp-1">{item.title}</span>
        </div>

        <button
          onClick={onBack}
          className="px-3 py-1 rounded-lg bg-[#1E293B] hover:bg-[#283850] text-white text-xs font-bold transition"
        >
          ← العودة للقائمة
        </button>
      </div>

      {/* ── Top Layout: Info Box & Left Action Sidebar ────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Left Actions Sidebar (Wecima Style) */}
        <div className="lg:col-span-1 bg-[#121A28] border border-[#1E293B] p-5 rounded-3xl flex flex-col gap-4 text-center order-2 lg:order-1">
          {/* Publisher Badge */}
          <div className="flex items-center justify-center gap-2 pb-3 border-b border-[#1E293B]">
            <div className="w-8 h-8 rounded-full bg-[#2563EB] text-white font-black flex items-center justify-center text-xs">
              M
            </div>
            <div className="text-right">
              <span className="block text-xs font-black text-white">Mashhad TV ✓</span>
              <span className="text-[10px] text-gray-400">منذ 3 أيام</span>
            </div>
          </div>

          <p className="text-xs text-gray-300 leading-relaxed font-medium">
            مشاهدة وتحميل مباشر فائق السرعة بجودة Full HD 1080p من جميع سيرفرات مَشْهَد TV الحصرية بدون إعلانات.
          </p>

          <button
            onClick={() => alert("تم استلام التقرير وسيتم فحص السيرفر فوراً!")}
            className="flex items-center justify-center gap-2 w-full py-3 rounded-2xl bg-[#1C2638] hover:bg-[#24324A] text-gray-300 hover:text-white text-xs font-bold border border-[#233148] transition"
          >
            <AlertTriangle size={15} className="text-amber-400" />
            <span>الإبلاغ عن مشكلة</span>
          </button>

          <button
            onClick={onToggleFavorite}
            className={`flex items-center justify-center gap-2 w-full py-3 rounded-2xl text-xs font-bold transition border ${
              isFavorite
                ? "bg-[#EF4444] text-white border-red-500 shadow-md font-black"
                : "bg-[#1C2638] hover:bg-[#24324A] text-gray-300 hover:text-white border-[#233148]"
            }`}
          >
            <Heart size={15} className={isFavorite ? "fill-white" : ""} />
            <span>{isFavorite ? "تم الحفظ بالمفضلة ✓" : "إضافة الي المفضلة"}</span>
          </button>
        </div>

        {/* Right Info Box with Poster & Metadata Table (Wecima Style) */}
        <div className="lg:col-span-3 bg-[#121A28] border border-[#1E293B] p-6 rounded-3xl flex flex-col gap-6 order-1 lg:order-2">
          <div className="flex flex-col sm:flex-row gap-6 items-start">
            {/* Poster with Big Red Play Overlay (Matching Image 1!) */}
            <div
              onClick={() => {
                if (servers.length > 0) {
                  onPlayServer(servers[0].embedUrl, servers[0].name);
                } else if (detail?.proxiedStream) {
                  onPlayServer(detail.proxiedStream, "سيرفر رئيسي");
                }
              }}
              className="relative w-44 sm:w-56 rounded-2xl overflow-hidden shadow-2xl border border-[#233148] flex-shrink-0 cursor-pointer group self-center sm:self-start aspect-[2/3]"
            >
              <img src={item.poster} alt={item.title} className="w-full h-full object-cover group-hover:scale-105 transition duration-500" />

              {/* Big Red Play Circle Overlay */}
              <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                <div className="w-14 h-14 rounded-full bg-[#EF4444]/90 text-white flex items-center justify-center shadow-2xl group-hover:scale-115 transition-transform border-2 border-white/40">
                  <Play size={24} className="fill-white ml-0.5" />
                </div>
              </div>

              {item.quality && (
                <span className="absolute top-2.5 right-2.5 px-2.5 py-0.5 rounded-lg bg-black/80 text-yellow-400 text-[10px] font-black border border-yellow-400/30">
                  {item.quality}
                </span>
              )}
            </div>

            {/* Metadata Table */}
            <div className="flex-1 text-right space-y-3 w-full">
              <h1 className="text-xl sm:text-2xl font-black text-white leading-snug">
                مشاهدة {item.title} بجودة عالية
              </h1>

              <div className="grid grid-cols-2 gap-y-2.5 gap-x-4 text-xs pt-2 border-t border-[#1E293B]">
                <div className="flex items-center justify-between border-b border-[#1E293B]/60 pb-1.5">
                  <span className="text-gray-400">العمل:</span>
                  <span className="font-bold text-white line-clamp-1">{item.title}</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1E293B]/60 pb-1.5">
                  <span className="text-gray-400">التصنيف:</span>
                  <span className="font-bold text-[#3B82F6]">{item.category}</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1E293B]/60 pb-1.5">
                  <span className="text-gray-400">البلد:</span>
                  <span className="font-bold text-white">مصر / عربي</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1E293B]/60 pb-1.5">
                  <span className="text-gray-400">النوع:</span>
                  <span className="font-bold text-white">{item.isSeries ? "مسلسل" : "فيلم"}</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1E293B]/60 pb-1.5">
                  <span className="text-gray-400">السنة:</span>
                  <span className="font-bold text-white">{item.year || "2026"}</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1E293B]/60 pb-1.5">
                  <span className="text-gray-400">المدة:</span>
                  <span className="font-bold text-white">45 دقيقة</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1E293B]/60 pb-1.5 col-span-2">
                  <span className="text-gray-400">التقييم:</span>
                  <span className="font-black text-yellow-400 flex items-center gap-1">
                    <Star size={14} className="fill-yellow-400" />
                    <span>IMDB 6.8 / 10</span>
                  </span>
                </div>
              </div>

              {/* Social Share Buttons */}
              <div className="flex items-center gap-2 pt-3 justify-end flex-wrap">
                <span className="text-xs text-gray-400 font-bold ml-2">مشاركة:</span>
                <button
                  onClick={() => window.open(`https://t.me/share/url?url=${encodeURIComponent(window.location.href)}&text=${encodeURIComponent(item.title)}`)}
                  className="w-8 h-8 rounded-full bg-[#0088cc] text-white flex items-center justify-center hover:scale-110 transition"
                  title="Telegram"
                >
                  <Send size={14} />
                </button>
                <button
                  onClick={() => window.open(`https://twitter.com/intent/tweet?text=${encodeURIComponent(item.title)}&url=${encodeURIComponent(window.location.href)}`)}
                  className="w-8 h-8 rounded-full bg-black text-white flex items-center justify-center hover:scale-110 transition border border-white/20"
                  title="Twitter / X"
                >
                  <span className="font-black text-xs">𝕏</span>
                </button>
                <button
                  onClick={() => window.open(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(window.location.href)}`)}
                  className="w-8 h-8 rounded-full bg-[#1877F2] text-white flex items-center justify-center hover:scale-110 transition"
                  title="Facebook"
                >
                  <span className="font-black text-xs">f</span>
                </button>
                <button
                  onClick={() => window.open(`https://api.whatsapp.com/send?text=${encodeURIComponent(item.title + ' ' + window.location.href)}`)}
                  className="w-8 h-8 rounded-full bg-[#25D366] text-white flex items-center justify-center hover:scale-110 transition"
                  title="WhatsApp"
                >
                  <MessageCircle size={14} />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ── 📑 قصة العرض (Full Synopsis Box - Matching Image 1) ──────── */}
      <div className="bg-[#121A28] border border-[#1E293B] p-6 rounded-3xl space-y-3 text-right">
        <h2 className="text-base font-black text-white flex items-center gap-2 justify-start">
          <Film size={18} className="text-[#3B82F6]" />
          <span>قصة العرض</span>
        </h2>
        <p className="text-xs sm:text-sm text-gray-300 leading-relaxed font-normal">
          {loading
            ? "جاري جلب تفاصيل قصة العمل من السيرفر..."
            : detail?.overview || "تدور الأحداث حول قصة مشوقة ومثيرة مليئة بالأحداث والتطورات الدرامية الرائعة."}
        </p>
      </div>

      {/* ── Watch Servers & Download Qualities Section (Image 1 & 2) ─── */}
      <div className="bg-[#121A28] border border-[#1E293B] p-6 rounded-3xl space-y-6">
        <div className="text-center space-y-1">
          <h2 className="text-lg font-black text-white">سيرفرات المشاهدة المباشرة والتحميل السريع</h2>
          <p className="text-xs text-gray-400">اختر السيرفر المناسب لك للمشاهدة بأعلى سرعة وبدون تقطيع</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Colorful Watch Servers (Right Column in RTL) */}
          <div className="space-y-3">
            <h3 className="text-xs font-black text-gray-300 text-right">سيرفرات المشاهدة المباشرة:</h3>
            {loading ? (
              <div className="py-8 flex items-center justify-center gap-2 text-xs text-gray-400">
                <Loader2 size={18} className="animate-spin text-[#3B82F6]" />
                <span>جاري فك تشفير سيرفرات المشاهدة...</span>
              </div>
            ) : servers.length === 0 ? (
              <div className="p-4 rounded-2xl bg-[#151E2D] text-center text-xs text-gray-400">
                السيرفر المباشر جاهز، يمكنك الضغط على زر التشغيل الرئيسي أعلاه.
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                {servers.map((srv, idx) => {
                  const colorClass = SERVER_COLORS[idx % SERVER_COLORS.length];
                  const isCurrent = resolvingServer === srv.embedUrl;

                  return (
                    <button
                      key={idx}
                      disabled={isCurrent}
                      onClick={() => onPlayServer(srv.embedUrl, srv.name)}
                      className={`flex items-center justify-between px-4 py-3 rounded-2xl text-white text-xs font-black shadow-md transition-all active:scale-95 ${colorClass}`}
                    >
                      <Play size={14} className="fill-white ml-1" />
                      <span>{srv.name}</span>
                      {isCurrent && <Loader2 size={13} className="animate-spin text-white" />}
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Green Download Qualities Buttons (Left Column in RTL - Matching Image 1 & 2!) */}
          <div className="space-y-3">
            <h3 className="text-xs font-black text-gray-300 text-right">روابط التحميل المباشر بجودات متعددة:</h3>
            <div className="flex flex-col gap-2">
              <button
                onClick={() => {
                  if (servers[0]) onPlayServer(servers[0].embedUrl, "تحميل 1080p");
                }}
                className="flex items-center justify-between px-4 py-2.5 rounded-2xl bg-[#10B981] hover:bg-[#059669] text-white text-xs font-black transition shadow-md"
              >
                <div className="flex items-center gap-2">
                  <Download size={16} />
                  <span>تحميل مباشر</span>
                </div>
                <div className="text-left font-mono">
                  <span>Full HD 1080p</span>
                  <span className="text-[10px] opacity-80 block">WEB-DL 931 MB</span>
                </div>
              </button>

              <button
                onClick={() => {
                  if (servers[0]) onPlayServer(servers[0].embedUrl, "تحميل 720p");
                }}
                className="flex items-center justify-between px-4 py-2.5 rounded-2xl bg-[#10B981] hover:bg-[#059669] text-white text-xs font-black transition shadow-md"
              >
                <div className="flex items-center gap-2">
                  <Download size={16} />
                  <span>تحميل مباشر</span>
                </div>
                <div className="text-left font-mono">
                  <span>HD 720p</span>
                  <span className="text-[10px] opacity-80 block">WEB-DL 379 MB</span>
                </div>
              </button>

              <button
                onClick={() => {
                  if (servers[0]) onPlayServer(servers[0].embedUrl, "تحميل 480p");
                }}
                className="flex items-center justify-between px-4 py-2.5 rounded-2xl bg-[#10B981] hover:bg-[#059669] text-white text-xs font-black transition shadow-md"
              >
                <div className="flex items-center gap-2">
                  <Download size={16} />
                  <span>تحميل مباشر</span>
                </div>
                <div className="text-left font-mono">
                  <span>SD 480p</span>
                  <span className="text-[10px] opacity-80 block">WEB-DL 139 MB</span>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* ── 📺 حلقات المسلسل (Episodes Grid - Matching Image 2!) ─────── */}
      {episodes.length > 0 && (
        <div className="bg-[#121A28] border border-[#1E293B] p-6 rounded-3xl space-y-4 text-right">
          <div className="flex items-center justify-between border-b border-[#1E293B] pb-3">
            <h2 className="text-base font-black text-white flex items-center gap-2">
              <Tv size={18} className="text-[#3B82F6]" />
              <span>حلقات {item.title}</span>
            </h2>
            <span className="text-xs text-[#3B82F6] font-bold">
              {episodes.length} حلقة متوفرة
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7 gap-2.5">
            {episodes.map((ep, idx) => {
              const isCurrent = idx === 0;
              return (
                <button
                  key={idx}
                  onClick={() => onSelectEpisode(ep.pageUrl, ep.title)}
                  className={`flex items-center justify-between p-3 rounded-2xl text-xs font-black transition-all ${
                    isCurrent
                      ? "bg-[#2563EB] text-white shadow-md scale-102"
                      : "bg-[#182333] hover:bg-[#202E42] text-gray-200 border border-[#233148]"
                  }`}
                >
                  <Play size={13} className={isCurrent ? "fill-white" : "text-gray-400"} />
                  <span className="line-clamp-1">{ep.title}</span>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* ── 👥 فريق العمل (Cast & Crew - Matching Image 3!) ──────────── */}
      <div className="bg-[#121A28] border border-[#1E293B] p-6 rounded-3xl space-y-4 text-right">
        <h2 className="text-base font-black text-white flex items-center gap-2">
          <span>👥 فريق العمل</span>
        </h2>

        <div className="flex items-center gap-4 overflow-x-auto pb-2 no-scrollbar justify-start">
          {CAST_MEMBERS.map((cast, idx) => (
            <div key={idx} className="flex items-center gap-3 bg-[#162030] p-2.5 pr-3.5 rounded-2xl border border-[#212E42] flex-shrink-0">
              <div className="w-10 h-10 rounded-full bg-[#202D42] text-gray-300 font-black flex items-center justify-center text-sm border border-white/10">
                {cast.name[0]}
              </div>
              <div className="text-right">
                <span className="block text-xs font-black text-white">{cast.name}</span>
                <span className="text-[10px] text-gray-400">{cast.role}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── 💖 ربما يعجبك أيضاً (Recommendations - Matching Image 3!) ── */}
      {recommendations.length > 0 && (
        <div className="space-y-4 text-right">
          <h2 className="text-lg font-black text-white flex items-center gap-2">
            <span>ربما يعجبك أيضاً</span>
          </h2>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
            {recommendations.slice(0, 6).map((rec) => (
              <div
                key={rec.id}
                onClick={() => onSelectRecommendation(rec)}
                className="bg-[#121A28] border border-[#1E293B] rounded-2xl overflow-hidden cursor-pointer flex flex-col group wecima-card-hover relative"
              >
                <div className="relative aspect-[2/3] w-full overflow-hidden bg-[#151E2D]">
                  <img src={rec.poster} alt={rec.title} className="w-full h-full object-cover group-hover:scale-105 transition duration-500" />
                  
                  {rec.quality && (
                    <span className="absolute top-2 right-2 px-2 py-0.5 rounded-md bg-black/80 text-yellow-400 text-[10px] font-black border border-yellow-400/30">
                      {rec.quality}
                    </span>
                  )}

                  <span className="absolute top-2 left-2 w-6 h-6 rounded-full bg-black/70 text-yellow-400 flex items-center justify-center text-[10px] font-black border border-yellow-400/40">
                    7.0
                  </span>
                </div>

                <div className="p-3 bg-[#121A28] flex flex-col justify-between flex-1">
                  <h3 className="text-xs font-bold text-white line-clamp-2 leading-snug group-hover:text-[#3B82F6] transition">
                    {rec.title}
                  </h3>
                  <span className="text-[10px] text-gray-400 mt-2 block">{rec.category}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
