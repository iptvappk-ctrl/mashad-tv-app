path = r"c:\Users\hassa\Desktop\all project\iptv app\web\src\components\CinemaView.tsx"

with open(path, "r", encoding="utf-8") as f:
    text = f.read()

badge_pat = """        {item.providerName && (
          <span className={`absolute bottom-3 right-3 inline-flex items-center gap-1 rounded-full px-2 py-1 text-[9px] font-bold shadow-sm ${providerTints[item.providerId || ""] || "bg-white/95 text-slate-700"}`}>
            <Globe2 size={10} /> {item.providerName}
          </span>
        )}"""

if badge_pat in text:
    text = text.replace(badge_pat, "")
    print("Badge pattern removed!")
else:
    print("Warning: badge pattern not found")

tints_pat = """const providerTints: Record<string, string> = {
  arabseed: "bg-blue-50 text-blue-700",
  akwam: "bg-emerald-50 text-emerald-700",
  wecima: "bg-violet-50 text-violet-700",
  faselhd: "bg-orange-50 text-orange-700",
  prestige: "bg-pink-50 text-pink-700",
  esheeq: "bg-cyan-50 text-cyan-700",
  egybest: "bg-rose-50 text-rose-700",
};"""

if tints_pat in text:
    text = text.replace(tints_pat, "")
    print("Tints pattern removed!")
else:
    print("Warning: tints pattern not found")

text = text.replace("  Globe2,\n", "")

with open(path, "w", encoding="utf-8") as f:
    f.write(text)

print("CinemaView.tsx updated successfully!")
