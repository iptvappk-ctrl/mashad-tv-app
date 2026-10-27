import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        mashad: {
          bg:       "#0A0A0F",
          surface:  "#111118",
          card:     "#16161F",
          elevated: "#1C1C28",
          border:   "#2A2A3A",
          gold:     "#E5B842",
          goldLight:"#F5D061",
          goldDark: "#B88E28",
          text:     "#F0F0F5",
          muted:    "#8888A0",
          live:     "#FF4444",
          green:    "#22C55E",
          blue:     "#3B82F6",
        },
      },
      fontFamily: {
        sans: ["Cairo", "system-ui", "sans-serif"],
      },
      boxShadow: {
        gold:    "0 0 30px -5px rgba(229,184,66,0.4)",
        "gold-lg":"0 0 60px -10px rgba(229,184,66,0.5)",
        card:    "0 8px 40px rgba(0,0,0,0.7)",
        live:    "0 0 20px -3px rgba(255,68,68,0.6)",
        glow:    "0 0 40px rgba(229,184,66,0.2)",
      },
      backgroundImage: {
        "gold-gradient":    "linear-gradient(135deg,#F5D061 0%,#E5B842 50%,#B88E28 100%)",
        "dark-gradient":    "linear-gradient(180deg,transparent 0%,rgba(10,10,15,0.8) 60%,#0A0A0F 100%)",
        "hero-gradient":    "linear-gradient(90deg,#0A0A0F 0%,rgba(10,10,15,0.85) 40%,rgba(10,10,15,0.3) 70%,transparent 100%)",
        "card-gradient":    "linear-gradient(180deg,transparent 0%,rgba(10,10,15,0.95) 100%)",
        "shine-gradient":   "linear-gradient(105deg,transparent 40%,rgba(255,255,255,0.07) 50%,transparent 60%)",
      },
      keyframes: {
        shimmer: {
          "0%":   { backgroundPosition: "-700px 0" },
          "100%": { backgroundPosition:  "700px 0" },
        },
        fadeIn: {
          from: { opacity: "0" },
          to:   { opacity: "1" },
        },
        slideUp: {
          from: { opacity: "0", transform: "translateY(24px)" },
          to:   { opacity: "1", transform: "translateY(0)" },
        },
        slideRight: {
          from: { opacity: "0", transform: "translateX(-20px)" },
          to:   { opacity: "1", transform: "translateX(0)" },
        },
        scaleIn: {
          from: { opacity: "0", transform: "scale(0.92)" },
          to:   { opacity: "1", transform: "scale(1)" },
        },
        pulseLive: {
          "0%,100%": { opacity: "1",  boxShadow: "0 0 0 0 rgba(255,68,68,0.4)" },
          "50%":     { opacity: "0.6",boxShadow: "0 0 0 6px rgba(255,68,68,0)" },
        },
        glowPulse: {
          "0%,100%": { boxShadow: "0 0 20px rgba(229,184,66,0.2)" },
          "50%":     { boxShadow: "0 0 40px rgba(229,184,66,0.5)" },
        },
        spin: {
          to: { transform: "rotate(360deg)" },
        },
      },
      animation: {
        shimmer:    "shimmer 1.6s infinite linear",
        fadeIn:     "fadeIn 0.4s ease-out",
        slideUp:    "slideUp 0.5s ease-out",
        slideRight: "slideRight 0.4s ease-out",
        scaleIn:    "scaleIn 0.3s ease-out",
        pulseLive:  "pulseLive 2s infinite",
        glowPulse:  "glowPulse 2.5s infinite ease-in-out",
        spin:       "spin 1s linear infinite",
      },
    },
  },
  plugins: [],
};

export default config;
