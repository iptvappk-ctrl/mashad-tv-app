"use client";

import React, { useEffect, useState, useCallback, useRef } from "react";
import { Lock, RefreshCw, ShieldAlert } from "lucide-react";

interface SystemState {
  status: "active" | "frozen" | "wiped";
  message: string;
  updatedAt?: number;
  wipeTimestamp?: number;
}

const STATUS_ENDPOINTS = [
  "https://dashboard-api-indol-eight.vercel.app/api/status",
  "https://dashboard-api-amber-chi.vercel.app/api/system_control",
  "https://zaafilm.com/api/status",
  "https://madartv-default-rtdb.firebaseio.com/system_control.json",
];

const STORAGE_KEY = "zaq_sys_state_v2";
const CACHE_TTL_MS = 120000; // 2 minutes client cache to preserve server & Firebase quota

export default function KillSwitchProvider({ children }: { children: React.ReactNode }) {
  // Read initial cache to avoid blocking UI for returning users
  const getInitialState = (): { state: SystemState; needsCheck: boolean } => {
    if (typeof window === "undefined") {
      return {
        state: { status: "active", message: "المنظومة نشطة وتعمل بكامل طاقتها." },
        needsCheck: true,
      };
    }
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Date.now() - (parsed._savedAt || 0) < CACHE_TTL_MS) {
          return { state: parsed, needsCheck: false };
        }
      }
    } catch {
      // ignore
    }
    return {
      state: { status: "active", message: "المنظومة نشطة وتعمل بكامل طاقتها." },
      needsCheck: true,
    };
  };

  const initial = getInitialState();
  const [systemState, setSystemState] = useState<SystemState>(initial.state);
  const [checking, setChecking] = useState<boolean>(initial.needsCheck);
  const wipeExecutedRef = useRef(false);
  const lastCheckTimeRef = useRef<number>(initial.needsCheck ? 0 : Date.now());

  // Clear all cached local data on remote wipe
  const executeRemoteWipe = useCallback(() => {
    if (wipeExecutedRef.current) return;
    wipeExecutedRef.current = true;
    try {
      localStorage.clear();
      sessionStorage.clear();
      document.cookie.split(";").forEach((c) => {
        document.cookie = c
          .replace(/^ +/, "")
          .replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
      });
      console.warn("[KillSwitch] Remote Wipe Executed. All local storage cleared.");
    } catch {
      // Ignore security errors in sandboxes
    }
  }, []);

  const checkStatus = useCallback(async (force = false) => {
    const now = Date.now();
    // Throttle checks if not forced
    if (!force && now - lastCheckTimeRef.current < 60000) {
      setChecking(false);
      return;
    }

    lastCheckTimeRef.current = now;

    for (const url of STATUS_ENDPOINTS) {
      try {
        const res = await fetch(url, {
          method: "GET",
          headers: { Accept: "application/json" },
          signal: AbortSignal.timeout(4000),
        });

        if (res.ok) {
          const data = await res.json();
          let normalizedStatus = (data.status || "active").toLowerCase() as
            | "active"
            | "frozen"
            | "wiped";

          const comps = data.components || {};
          if (comps.web_player_enabled === false || comps.website_enabled === false) {
            if (normalizedStatus === "active") {
              normalizedStatus = "frozen";
            }
          }

          const nextState: SystemState = {
            status: normalizedStatus,
            message: data.message || "المنظومة نشطة وتعمل بشكل طبيعي",
            updatedAt: data.updatedAt,
            wipeTimestamp: data.wipeTimestamp,
          };

          try {
            sessionStorage.setItem(
              STORAGE_KEY,
              JSON.stringify({ ...nextState, _savedAt: Date.now() })
            );
          } catch {
            // ignore
          }

          if (normalizedStatus === "wiped") {
            executeRemoteWipe();
          }

          setSystemState(nextState);
          setChecking(false);
          return;
        }
      } catch {
        // Try next fallback endpoint
      }
    }

    // Fail open if offline/unreachable so users aren't locked out
    setChecking(false);
  }, [executeRemoteWipe]);

  useEffect(() => {
    // Initial check if needed
    if (initial.needsCheck) {
      checkStatus(true);
    }

    // Smart adaptive polling: only poll when document is visible
    let timer: NodeJS.Timeout | null = null;

    const scheduleNextPoll = () => {
      // Jitter between 180s and 240s (3-4 min) to keep Vercel & Cloudflare quotas safe
      const jitter = 180000 + Math.floor(Math.random() * 60000);
      timer = setTimeout(() => {
        if (typeof document !== "undefined" && document.visibilityState === "visible") {
          checkStatus(false);
        }
        scheduleNextPoll();
      }, jitter);
    };

    scheduleNextPoll();

    // Check when user refocuses tab after being away
    const onVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        if (Date.now() - lastCheckTimeRef.current > CACHE_TTL_MS) {
          checkStatus(false);
        }
      }
    };

    document.addEventListener("visibilitychange", onVisibilityChange);

    return () => {
      if (timer) clearTimeout(timer);
      document.removeEventListener("visibilitychange", onVisibilityChange);
    };
  }, [checkStatus, initial.needsCheck]);

  // Loading Screen (Only on first cold visit before cache is established)
  if (checking) {
    return (
      <div dir="rtl" className="min-h-screen bg-[#090A0E] flex flex-col items-center justify-center p-6 text-center">
        <div className="relative mb-6">
          <div className="w-16 h-16 rounded-2xl border-2 border-amber-500/30 border-t-amber-400 animate-spin" />
          <div className="absolute inset-0 grid place-items-center">
            <span className="text-amber-400 font-black text-xs">ZQ</span>
          </div>
        </div>
        <h2 className="text-white font-bold text-lg mb-2">زقزوقة TV</h2>
        <p className="text-slate-400 text-xs animate-pulse">جاري فحص الاتصال بالمنظومة السحابية...</p>
      </div>
    );
  }

  // Wiped Screen (Emergency Zero-State / Destruction)
  if (systemState.status === "wiped") {
    return (
      <div dir="rtl" className="min-h-screen bg-[#050507] flex flex-col items-center justify-center p-6 text-center select-none">
        <div className="max-w-md w-full bg-[#0D0E15] border border-rose-500/30 rounded-3xl p-8 sm:p-10 shadow-[0_0_50px_rgba(244,63,94,0.15)]">
          <div className="w-20 h-20 rounded-3xl bg-rose-500/10 border border-rose-500/30 grid place-items-center mx-auto mb-6 shadow-inner">
            <ShieldAlert size={40} className="text-rose-500" />
          </div>
          <span className="inline-block px-3 py-1 rounded-full bg-rose-500/10 border border-rose-500/30 text-rose-400 text-[11px] font-black tracking-wider uppercase mb-3">
            حالة الطوارئ القصوى
          </span>
          <h1 className="text-white text-2xl sm:text-3xl font-black mb-3">الخدمة غير متاحة</h1>
          <p className="text-slate-400 text-sm leading-relaxed mb-8">
            {systemState.message || "عذراً، تم إغلاق وتصفير المنظومة نهائياً بأمر الإدارة."}
          </p>
          <div className="p-3.5 rounded-2xl bg-rose-950/20 border border-rose-500/20 text-xs text-rose-300 font-semibold">
            تم مسح جميع البيانات المؤقتة والسيرفرات من جهازك تلقائياً.
          </div>
        </div>
      </div>
    );
  }

  // Frozen Screen (Maintenance / Standby)
  if (systemState.status === "frozen") {
    return (
      <div dir="rtl" className="min-h-screen bg-[#090A0E] flex flex-col items-center justify-center p-6 text-center select-none">
        <div className="max-w-md w-full bg-[#12131D] border border-amber-500/30 rounded-3xl p-8 sm:p-10 shadow-[0_0_50px_rgba(212,175,55,0.15)]">
          <div className="w-20 h-20 rounded-3xl bg-amber-500/10 border border-amber-500/30 grid place-items-center mx-auto mb-6 shadow-inner">
            <Lock size={38} className="text-amber-400" />
          </div>
          <span className="inline-block px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/30 text-amber-300 text-[11px] font-black tracking-wider uppercase mb-3">
            صيانة مؤقتة
          </span>
          <h1 className="text-white text-2xl sm:text-3xl font-black mb-3">زقزوقة TV</h1>
          <p className="text-slate-300 text-sm leading-relaxed whitespace-pre-line mb-8">
            {systemState.message || "المنظومة متوقفة مؤقتاً لأعمال الصيانة والتطوير.\nسنعود للعمل بكامل طاقتها في أقرب وقت."}
          </p>
          <button
            type="button"
            onClick={() => {
              setChecking(true);
              checkStatus(true);
            }}
            className="inline-flex items-center justify-center gap-2.5 w-full rounded-2xl bg-gradient-to-r from-amber-400 via-amber-500 to-amber-600 px-6 py-3.5 text-sm font-black text-slate-950 shadow-lg shadow-amber-500/25 transition hover:scale-[1.02] active:scale-[0.98]"
          >
            <RefreshCw size={16} />
            إعادة التحقق الآن
          </button>
        </div>
      </div>
    );
  }

  // Active / Normal App
  return <>{children}</>;
}
