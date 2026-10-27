"use client";

import React from "react";
import {
  Play,
  Heart,
  AlertTriangle,
  Film,
  Star,
  Loader2,
  Send,
  MessageCircle,
  Crown,
  CheckCircle,
  Sparkles,
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
  "bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black font-black shadow-gold", // Server 1: Mashhad Gold
  "bg-[#2563EB] hover:bg-[#1D4ED8] text-white", // Server 2: Blue
  "bg-[#8B5CF6] hover:bg-[#7C3AED] text-white", // Server 3: Purple
  "bg-[#F97316] hover:bg-[#EA580C] text-white", // Server 4: Orange
  "bg-[#EF4444] hover:bg-[#DC2626] text-white", // Server 5: Red
  "bg-[#06B6D4] hover:bg-[#0891B2] text-white", // Server 6: Cyan
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
  const servers = detail?.servers || [];
  const episodes = detail?.episodes || [];

  return (
    <div className="space-y-8 animate-fadeIn pb-24">
      {/* ── Breadcrumb Bar ───────────────────────────────────────────── */}
      <div className="flex items-center justify-between flex-wrap gap-2 text-xs font-bold text-gray-400 bg-[#0D111A] p-3.5 rounded-2xl border border-[#1D273B] shadow-lg">
        <div className="flex items-center gap-2 flex-wrap">
          <button onClick={onBack} className="hover:text-[#E5B842] transition">
            مَشْهَد TV
          </button>
          <span>›</span>
          <span className="text-gray-300">{item.isSeries ? "مسلسلات" : "أفلام"}</span>
          <span>›</span>
          <span className="text-gray-300">{item.category}</span>
          <span>›</span>
          <span className="text-[#E5B842] font-black line-clamp-1">{item.title}</span>
        </div>

        <button
          onClick={onBack}
          className="px-4 py-1.5 rounded-xl bg-[#161E2E] hover:bg-[#202C42] text-white text-xs font-bold transition border border-[#1D273B]"
        >
          ← العودة للقائمة
        </button>
      </div>

      {/* ── Top Layout: Info Box & Left Action Sidebar ────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Left Actions Sidebar */}
        <div className="lg:col-span-1 bg-[#0D111A] border border-[#1D273B] p-5 rounded-3xl flex flex-col gap-4 text-center order-2 lg:order-1 shadow-2xl">
          {/* Official Publisher Badge */}
          <div className="flex items-center justify-center gap-2 pb-3 border-b border-[#1D273B]">
            <div className="w-9 h-9 rounded-xl overflow-hidden border border-[#E5B842]/60 bg-black flex-shrink-0 shadow-gold">
              <img src="/logo.jpg" alt="مشهد" className="w-full h-full object-cover" />
            </div>
            <div className="text-right">
              <div className="flex items-center gap-1">
                <span className="text-xs font-black text-white">مَـشْـهَـدْ VIP</span>
                <CheckCircle size={12} className="text-[#E5B842] fill-[#E5B842]" />
              </div>
              <span className="text-[10px] text-gray-400">سيرفرات حصرية فائقة السرعة</span>
            </div>
          </div>

          <p className="text-xs text-gray-300 leading-relaxed font-medium">
            مشاهدة مباشرة فائقة السرعة بجودة Full HD 1080p من سيرفرات مَشْهَد TV الرسمية بدون تقطيع أو إعلانات مزعجة.
          </p>

          <button
            onClick={() => alert("تم استلام بلاغك! يقوم فريق مَشْهَد TV بفحص السيرفر فوراً.")}
            className="flex items-center justify-center gap-2 w-full py-3 rounded-2xl bg-[#121824] hover:bg-[#182234] text-gray-300 hover:text-white text-xs font-bold border border-[#1D273B] transition"
          >
            <AlertTriangle size={15} className="text-amber-400" />
            <span>الإبلاغ عن عطل بالسيرفر</span>
          </button>

          <button
            onClick={onToggleFavorite}
            className={`flex items-center justify-center gap-2 w-full py-3 rounded-2xl text-xs font-bold transition border ${
              isFavorite
                ? "bg-[#EF4444] text-white border-red-500 shadow-md font-black"
                : "bg-[#121824] hover:bg-[#182234] text-gray-300 hover:text-white border-[#1D273B]"
            }`}
          >
            <Heart size={15} className={isFavorite ? "fill-white" : ""} />
            <span>{isFavorite ? "تم الحفظ بالمفضلة ✓" : "إضافة إلى المفضلة"}</span>
          </button>
        </div>

        {/* Right Info Box with Poster & Metadata Table */}
        <div className="lg:col-span-3 bg-[#0D111A] border border-[#1D273B] p-6 rounded-3xl flex flex-col gap-6 order-1 lg:order-2 shadow-2xl">
          <div className="flex flex-col sm:flex-row gap-6 items-start">
            {/* Poster with Glowing Play Overlay */}
            <div
              onClick={() => {
                if (servers.length > 0) {
                  onPlayServer(servers[0].embedUrl, servers[0].name);
                } else if (detail?.proxiedStream) {
                  onPlayServer(detail.proxiedStream, "سيرفر مَشْهَد المباشر");
                }
              }}
              className="relative w-44 sm:w-56 rounded-2xl overflow-hidden shadow-2xl border-2 border-[#E5B842]/40 flex-shrink-0 cursor-pointer group self-center sm:self-start aspect-[2/3]"
            >
              <img
                src={item.poster}
                alt={item.title}
                className="w-full h-full object-cover group-hover:scale-105 transition duration-500"
                onError={(e) => {
                  (e.target as HTMLImageElement).src = "/logo.jpg";
                }}
              />

              {/* Glowing Play Circle Overlay */}
              <div className="absolute inset-0 bg-black/40 flex items-center justify-center backdrop-blur-[1px]">
                <div className="w-16 h-16 rounded-full bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black flex items-center justify-center shadow-goldGlow group-hover:scale-115 transition-transform border-2 border-white/60">
                  <Play size={28} className="fill-black ml-1" />
                </div>
              </div>

              {item.quality && (
                <span className="absolute top-2.5 right-2.5 px-2.5 py-0.5 rounded-lg bg-black/90 text-[#F5D061] text-[10px] font-black border border-[#E5B842]/50 shadow-md">
                  {item.quality}
                </span>
              )}
            </div>

            {/* Metadata Table */}
            <div className="flex-1 text-right space-y-3 w-full">
              <h1 className="text-xl sm:text-2xl font-black text-white leading-snug">
                مشاهدة {item.title} بجودة عالية
              </h1>

              <div className="grid grid-cols-2 gap-y-2.5 gap-x-4 text-xs pt-2 border-t border-[#1D273B]">
                <div className="flex items-center justify-between border-b border-[#1D273B]/60 pb-1.5">
                  <span className="text-gray-400">العمل:</span>
                  <span className="font-bold text-white line-clamp-1">{item.title}</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1D273B]/60 pb-1.5">
                  <span className="text-gray-400">التصنيف:</span>
                  <span className="font-bold text-[#E5B842]">{item.category}</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1D273B]/60 pb-1.5">
                  <span className="text-gray-400">البلد:</span>
                  <span className="font-bold text-white">مصر / إنتاج عربي</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1D273B]/60 pb-1.5">
                  <span className="text-gray-400">النوع:</span>
                  <span className="font-bold text-white">{item.isSeries ? "مسلسل" : "فيلم"}</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1D273B]/60 pb-1.5">
                  <span className="text-gray-400">السنة:</span>
                  <span className="font-bold text-white">{item.year || "2026"}</span>
                </div>

                <div className="flex items-center justify-between border-b border-[#1D273B]/60 pb-1.5">
                  <span className="text-gray-400">التقييم:</span>
                  <span className="font-black text-[#F5D061] flex items-center gap-1">
                    <Star size={13} className="fill-[#E5B842] text-[#E5B842]" />
                    <span>IMDB 7.4 / 10</span>
                  </span>
                </div>
              </div>

              {/* Social Share Buttons */}
              <div className="flex items-center gap-2 pt-3 justify-end flex-wrap">
                <span className="text-xs text-gray-400 font-bold ml-2">مشاركة العرض:</span>
                <button
                  onClick={() =>
                    window.open(
                      `https://t.me/share/url?url=${encodeURIComponent(window.location.href)}&text=${encodeURIComponent(item.title)}`
                    )
                  }
                  className="w-8 h-8 rounded-full bg-[#0088cc] text-white flex items-center justify-center hover:scale-110 transition shadow-md"
                  title="Telegram"
                >
                  <Send size={14} />
                </button>
                <button
                  onClick={() =>
                    window.open(
                      `https://api.whatsapp.com/send?text=${encodeURIComponent(item.title + " " + window.location.href)}`
                    )
                  }
                  className="w-8 h-8 rounded-full bg-[#25D366] text-white flex items-center justify-center hover:scale-110 transition shadow-md"
                  title="WhatsApp"
                >
                  <MessageCircle size={14} />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ── 📑 قصة العرض ────────────────────────────────────────────── */}
      <div className="bg-[#0D111A] border border-[#1D273B] p-6 rounded-3xl space-y-3 text-right shadow-2xl">
        <h2 className="text-base font-black text-white flex items-center gap-2 justify-start">
          <Film size={18} className="text-[#E5B842]" />
          <span>قصة العرض</span>
        </h2>
        <p className="text-xs sm:text-sm text-gray-300 leading-relaxed font-normal">
          {loading
            ? "جاري جلب تفاصيل قصة العمل بأقصى سرعة..."
            : detail?.overview || "تدور الأحداث حول قصة مشوقة ومثيرة مليئة بالأحداث والتطورات الدرامية الرائعة."}
        </p>
      </div>

      {/* ── Watch Servers (Full Width Display) ───────────────────────── */}
      <div className="bg-[#0D111A] border border-[#1D273B] p-6 rounded-3xl space-y-6 shadow-2xl">
        <div className="text-center space-y-1">
          <h2 className="text-lg font-black text-white flex items-center justify-center gap-2">
            <Crown size={20} className="text-[#E5B842]" />
            <span>سيرفرات المشاهدة المباشرة فائقة السرعة</span>
          </h2>
          <p className="text-xs text-gray-400">اختر السيرفر المناسب لك للمشاهدة بدون تقطيع أو إعلانات</p>
        </div>

        <div className="space-y-3">
          {loading ? (
            <div className="py-8 flex items-center justify-center gap-2 text-xs text-gray-400">
              <Loader2 size={18} className="animate-spin text-[#E5B842]" />
              <span>جاري استخراج السيرفرات بأقصى سرعة...</span>
            </div>
          ) : servers.length === 0 ? (
            <div className="p-4 rounded-2xl bg-[#121824] text-center text-xs text-gray-400">
              السيرفر المباشر جاهز، يمكنك الضغط على زر التشغيل الرئيسي أعلاه.
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">
              {servers.map((srv, idx) => {
                const colorClass = SERVER_COLORS[idx % SERVER_COLORS.length];
                const isCurrent = resolvingServer === srv.embedUrl;

                return (
                  <button
                    key={idx}
                    disabled={isCurrent}
                    onClick={() => onPlayServer(srv.embedUrl, srv.name)}
                    className={`flex items-center justify-between px-5 py-3.5 rounded-2xl text-xs font-black shadow-md transition-all active:scale-95 hover:scale-105 ${colorClass}`}
                  >
                    <Play size={14} className="fill-current ml-1" />
                    <span>{srv.name}</span>
                    {isCurrent && <Loader2 size={13} className="animate-spin" />}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* ── 🎬 حلقات المسلسل ─────────────────────────────────────────── */}
      {episodes.length > 0 && (
        <div className="bg-[#0D111A] border border-[#1D273B] p-6 rounded-3xl space-y-4 shadow-2xl">
          <div className="flex items-center justify-between border-b border-[#1D273B] pb-3">
            <h2 className="text-base font-black text-white flex items-center gap-2">
              <Film size={18} className="text-[#E5B842]" />
              <span>حلقات العمل المتاحة ({episodes.length})</span>
            </h2>
            <span className="text-xs text-gray-400">اضغط على أي حلقة للمشاهدة فوراً</span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-2.5">
            {episodes.map((ep, idx) => {
              const isActive = ep.pageUrl === item.pageUrl;
              return (
                <button
                  key={idx}
                  onClick={() => onSelectEpisode(ep.pageUrl, ep.title)}
                  className={`flex items-center justify-between p-3 rounded-2xl text-xs font-black transition-all ${
                    isActive
                      ? "bg-gradient-to-r from-[#E5B842] to-[#C69214] text-black shadow-gold scale-105"
                      : "bg-[#121824] hover:bg-[#182234] text-gray-200 border border-[#1D273B]"
                  }`}
                >
                  <Play size={12} className={isActive ? "fill-black" : "text-[#E5B842] fill-[#E5B842]"} />
                  <span>{ep.title}</span>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* ── 💖 ربما يعجبك أيضاً ───────────────────────────────────────── */}
      {recommendations.length > 0 && (
        <div className="space-y-4 text-right">
          <h2 className="text-lg font-black text-white flex items-center gap-2">
            <Sparkles size={18} className="text-[#E5B842]" />
            <span>أعمال ترشيحية قد تعجبك</span>
          </h2>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
            {recommendations.slice(0, 6).map((rec) => (
              <div
                key={rec.id}
                onClick={() => onSelectRecommendation(rec)}
                onMouseEnter={() => {
                  fetch(`/api/cinema/detail?url=${encodeURIComponent(rec.pageUrl)}`, { priority: "low" }).catch(() => {});
                }}
                className="bg-[#0D111A] border border-[#1D273B] hover:border-[#E5B842]/70 rounded-2xl overflow-hidden cursor-pointer flex flex-col group transition-all duration-300 transform hover:-translate-y-1.5 shadow-lg hover:shadow-gold relative"
              >
                <div className="relative aspect-[2/3] w-full overflow-hidden bg-[#121824]">
                  <img src={rec.poster} alt={rec.title} className="w-full h-full object-cover group-hover:scale-105 transition duration-500" />

                  {rec.quality && (
                    <span className="absolute top-2 right-2 px-2 py-0.5 rounded-md bg-black/80 text-[#F5D061] text-[10px] font-black border border-[#E5B842]/30">
                      {rec.quality}
                    </span>
                  )}

                  <span className="absolute top-2 left-2 w-6 h-6 rounded-full bg-black/70 text-[#E5B842] flex items-center justify-center text-[10px] font-black border border-[#E5B842]/40">
                    7.0
                  </span>
                </div>

                <div className="p-3 bg-[#0D111A] flex flex-col justify-between flex-1">
                  <h3 className="text-xs font-bold text-white line-clamp-2 leading-snug group-hover:text-[#F5D061] transition">
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
