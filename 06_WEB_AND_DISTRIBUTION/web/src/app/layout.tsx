import type { Metadata, Viewport } from "next";
import "./globals.css";
import KillSwitchProvider from "@/components/KillSwitchProvider";

export const metadata: Metadata = {
  title: "زقزوقة | أفلام ومسلسلات ومباريات",
  description: "اكتشف الأفلام والمسلسلات والمواسم والحلقات، وتابع نتائج ومواعيد المباريات من واجهة زقزوقة.",
  manifest: "/manifest.json",
  icons: {
    icon: "/logo.jpg",
    apple: "/logo.jpg",
  },
};

export const viewport: Viewport = {
  themeColor: "#F2E6D7",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ar" dir="rtl">
      <body className="min-h-screen bg-[#F2E6D7] text-[#342633] antialiased">
        <KillSwitchProvider>
          {children}
        </KillSwitchProvider>
      </body>
    </html>
  );
}
