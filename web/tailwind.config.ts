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
          bg: "#07090E",
          surface: "#0D111A",
          card: "#121824",
          cardHover: "#192233",
          elevated: "#161E2E",
          border: "#1D273B",
          borderLight: "#283750",
          gold: "#E5B842",
          goldLight: "#F5D061",
          goldDark: "#C69214",
          primary: "#E5B842",
          accent: "#3B82F6",
          blue: "#2563EB",
          text: "#F3F4F6",
          muted: "#94A3B8",
          live: "#EF4444",
          green: "#10B981",
          purple: "#8B5CF6",
          orange: "#F97316",
        },
        wecima: {
          bg: "#07090E",
          surface: "#0D111A",
          card: "#121824",
          cardHover: "#192233",
          border: "#1D273B",
          borderLight: "#283750",
          primary: "#E5B842",
          primaryHover: "#D4A017",
          accent: "#3B82F6",
          gold: "#E5B842",
          green: "#10B981",
          purple: "#8B5CF6",
          orange: "#F97316",
          red: "#EF4444",
        },
      },
      fontFamily: {
        sans: ["Cairo", "system-ui", "-apple-system", "sans-serif"],
      },
      boxShadow: {
        gold: "0 0 20px rgba(229, 184, 66, 0.35)",
        goldGlow: "0 0 30px rgba(245, 208, 97, 0.4)",
        card: "0 8px 30px rgba(0, 0, 0, 0.7)",
        live: "0 0 15px rgba(239, 68, 68, 0.6)",
      },
    },
  },
  plugins: [],
};

export default config;
