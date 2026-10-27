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
        wecima: {
          bg: "#0B0F17",
          surface: "#101622",
          card: "#141C2B",
          cardHover: "#1B2538",
          sidebar: "#0E1420",
          border: "#1C273B",
          borderLight: "#283750",
          primary: "#2563EB",
          primaryHover: "#1D4ED8",
          accent: "#3B82F6",
          gold: "#F59E0B",
          green: "#10B981",
          purple: "#8B5CF6",
          orange: "#F97316",
          red: "#EF4444",
          cyan: "#06B6D4",
          text: "#F3F4F6",
          textMuted: "#94A3B8",
        },
      },
      fontFamily: {
        sans: ["Cairo", "system-ui", "-apple-system", "sans-serif"],
      },
      boxShadow: {
        wecima: "0 8px 30px rgba(0, 0, 0, 0.6)",
        server: "0 4px 15px rgba(37, 99, 235, 0.3)",
      },
    },
  },
  plugins: [],
};

export default config;
