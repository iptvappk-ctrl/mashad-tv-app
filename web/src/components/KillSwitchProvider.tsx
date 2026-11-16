"use client";
import { useEffect, useState } from "react";
import { checkKillSwitch } from "@/lib/analytics";

export default function KillSwitchProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<"loading" | "alive" | "killed">("loading");
  const [msg, setMsg] = useState("الموقع متوقف مؤقتاً.\nجاري الصيانة، سنعود قريباً.");

  useEffect(() => {
    checkKillSwitch().then((result) => {
      if (result.allowed) {
        setState("alive");
      } else {
        setMsg(result.message || msg);
        setState("killed");
      }
    });
  }, []);

  if (state === "loading") {
    return (
      <div className="min-h-screen bg-[#F2E6D7] flex items-center justify-center">
        <div className="text-center">
          <div className="w-8 h-8 border-2 border-[#B94E3C] border-t-transparent rounded-full animate-spin mx-auto mb-3" />
          <p className="text-[#806D78] text-sm">جاري التحقق...</p>
        </div>
      </div>
    );
  }

  if (state === "killed") {
    return (
      <div className="min-h-screen bg-[#1A0E0E] flex items-center justify-center">
        <div className="text-center px-8 max-w-md">
          <div className="text-6xl mb-6">🔒</div>
          <h1 className="text-white text-2xl font-bold mb-4">زقزوقة TV</h1>
          <p className="text-[#CCB0A0] text-base leading-relaxed whitespace-pre-line mb-8">{msg}</p>
          <button
            onClick={() => { setState("loading"); checkKillSwitch().then(r => { if (r.allowed) setState("alive"); else { setMsg(r.message || msg); setState("killed"); } }); }}
            className="text-[#B94E3C] border border-[#B94E3C] rounded-lg px-6 py-2 text-sm hover:bg-[#B94E3C]/10 transition"
          >
            إعادة المحاولة
          </button>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
