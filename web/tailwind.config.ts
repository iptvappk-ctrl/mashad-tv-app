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
          bg: "#F5F7FB",
          surface: "#FFFFFF",
          card: "#FFFFFF",
          cardHover: "#F7F8FC",
          elevated: "#F0F3F9",
          border: "#E3E8F0",
          borderLight: "#D7DEEA",
          gold: "#5364CC",
          goldLight: "#4052B8",
          goldDark: "#34439C",
          primary: "#5364CC",
          accent: "#4D5FD1",
          blue: "#4D5FD1",
          text: "#1F293B",
          muted: "#68758A",
          live: "#E14F5A",
          green: "#16866F",
          purple: "#7654C8",
          orange: "#C56C25",
        },
        wecima: {
          bg: "#F5F7FB",
          surface: "#FFFFFF",
          card: "#FFFFFF",
          cardHover: "#F7F8FC",
          border: "#E3E8F0",
          borderLight: "#D7DEEA",
          primary: "#5364CC",
          primaryHover: "#4052B8",
          accent: "#4D5FD1",
          gold: "#5364CC",
          green: "#16866F",
          purple: "#7654C8",
          orange: "#C56C25",
          red: "#E14F5A",
        },
      },
      fontFamily: {
        sans: ["Cairo", "system-ui", "-apple-system", "sans-serif"],
      },
      boxShadow: {
        gold: "0 5px 16px rgba(77, 95, 209, 0.18)",
        goldGlow: "0 8px 24px rgba(77, 95, 209, 0.22)",
        card: "0 8px 28px rgba(35, 49, 78, 0.08)",
        live: "0 0 14px rgba(225, 79, 90, 0.22)",
      },
    },
  },
  plugins: [],
};

export default config;
