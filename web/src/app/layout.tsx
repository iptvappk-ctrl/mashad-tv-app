import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "زَقْزُوقَة TV | منصة السينما والبث المباشر",
  description: "أقوى منصة لمشاهدة أحدث الأفلام والمسلسلات ومباريات كرة القدم الحية بجودة عالية وبدون إعلانات",
  manifest: "/manifest.json",
  icons: {
    icon: "/logo.jpg",
    apple: "/logo.jpg",
  },
};

export const viewport: Viewport = {
  themeColor: "#090A0E",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ar" dir="rtl" className="dark bg-[#090A0E]">
      <body className="min-h-screen bg-[#090A0E] text-slate-100 antialiased selection:bg-amber-500/30 selection:text-amber-300">
        {children}
      </body>
    </html>
  );
}
