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
          bg: "#090A0E",
          surface: "#12131D",
          card: "#161726",
          cardHover: "#1E2034",
          elevated: "#23263B",
          border: "rgba(212, 175, 55, 0.15)",
          borderLight: "rgba(255, 255, 255, 0.08)",
          gold: "#D4AF37",
          goldLight: "#F59E0B",
          goldDark: "#B45309",
          primary: "#D4AF37",
          accent: "#F59E0B",
          blue: "#3B82F6",
          text: "#F9FAFB",
          muted: "#9CA3AF",
          live: "#EF4444",
          green: "#10B981",
          purple: "#8B5CF6",
          orange: "#F97316",
        },
      },
      fontFamily: {
        sans: ["Cairo", "system-ui", "-apple-system", "sans-serif"],
      },
      boxShadow: {
        gold: "0 5px 20px rgba(212, 175, 55, 0.25)",
        goldGlow: "0 8px 30px rgba(212, 175, 55, 0.35)",
        card: "0 10px 30px rgba(0, 0, 0, 0.5)",
        live: "0 0 16px rgba(239, 68, 68, 0.35)",
      },
    },
  },
  plugins: [],
};

export default config;
