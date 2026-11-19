"use client";

import React, { useEffect, useState, useCallback, useRef } from "react";
import { Lock, RefreshCw, AlertTriangle, ShieldAlert } from "lucide-react";

interface SystemState {
  status: "active" | "frozen" | "wiped";
  message: string;
  updatedAt?: number;
  wipeTimestamp?: number;
}

const STATUS_ENDPOINTS = [
  "/api/status",
  "https://zaqzouka-dashboard-api.vercel.app/api/status",
  "https://zaqzouka-vercel-hub.vercel.app/api/status",
];

export default function KillSwitchProvider({ children }: { children: React.ReactNode }) {
  const [systemState, setSystemState] = useState<SystemState>({
    status: "active",
    message: "المنظومة نشطة وتعمل بشكل طبيعي",
  });
  const [checking, setChecking] = useState(true);
  const wipeExecutedRef = useRef(false);

  // Clear all cached local data on remote wipe
  const executeRemoteWipe = useCallback(() => {
    if (wipeExecutedRef.current) return;
    wipeExecutedRef.current = true;
    try {
      localStorage.clear();
      sessionStorage.clear();
      // Remove cookies if possible
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

  const checkStatus = useCallback(async () => {
    for (const url of STATUS_ENDPOINTS) {
      try {
        const res = await fetch(url, {
          method: "GET",
          headers: { Accept: "application/json" },
          cache: "no-store",
          signal: AbortSignal.timeout(5000),
        });

        if (res.ok) {
          const data = await res.json();
          const normalizedStatus = (data.status || "active").toLowerCase() as
            | "active"
            | "frozen"
            | "wiped";

          const nextState: SystemState = {
            status: normalizedStatus,
            message: data.message || "المنظومة نشطة وتعمل بشكل طبيعي",
            updatedAt: data.updatedAt,
            wipeTimestamp: data.wipeTimestamp,
          };

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

    // Fail open if offline/unreachable so legitimate users aren't locked out
    setChecking(false);
  }, [executeRemoteWipe]);

  useEffect(() => {
    // Initial check
    checkStatus();

    // Periodic lightweight check every 10s
    const interval = setInterval(checkStatus, 10000);
    return () => clearInterval(interval);
  }, [checkStatus]);

  // Loading Screen
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
            {systemState.message || "المنظومة متوقفة مؤقتاً لأعمال الصيانة والتطوير.\nسنعود للعمل بكامل طاقتنا في أقرب وقت."}
          </p>
          <button
            type="button"
            onClick={() => {
              setChecking(true);
              checkStatus();
            }}
            className="inline-flex items-center justify-center gap-2.5 w-full rounded-2xl bg-gradient-to-r from-amber-400 via-amber-500 to-amber-600 px-6 py-3.5 text-sm font-black text-slate-950 shadow-lg shadow-amber-500/25 transition hover:scale-[1.02] active:scale-[0.98]"
          >
            <RefreshCw size={16} />
            إعادة التحقق الآن
          </button>
          <p className="text-slate-500 text-[11px] font-medium mt-4">يتم التحقق التلقائي كل دقيقة</p>
        </div>
      </div>
    );
  }

  // Active / Normal Operation
  return <>{children}</>;
}
