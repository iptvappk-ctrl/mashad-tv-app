import type { Metadata, Viewport } from "next";
import "./globals.css";
import KillSwitchProvider from "@/components/KillSwitchProvider";

export const metadata: Metadata = {
  metadataBase: new URL("https://zaqzouka.masarplatform.org"),
  title: "زَقْزُوقَة TV | سينما وبث مباشر فائق الجودة",
  description: "المنصة الشاملة للأفلام، المسلسلات، الأنمي، ملخصات السينما، والبث المباشر لأهم المباريات والقنوات الرياضية بجودة فائقة السرعة وبدون تقطيع.",
  manifest: "/manifest.json",
  icons: {
    icon: "/logo.jpg",
    apple: "/logo.jpg",
  },
  openGraph: {
    title: "زَقْزُوقَة TV | منصة السينما والبث المباشر",
    description: "أحدث الأفلام والمسلسلات العربية والأجنبية والتركية، بث مباشر للقنوات الرياضية ومباريات اليوم.",
    images: [{ url: "/logo.jpg" }],
  },
};

export const viewport: Viewport = {
  themeColor: "#090A0E",
  width: "device-width",
  initialScale: 1,
  maximumScale: 5,
  viewportFit: "cover",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ar" dir="rtl" className="dark">
      <body className="min-h-screen bg-[#090A0E] text-slate-100 antialiased selection:bg-amber-500/30 selection:text-amber-300">
        <KillSwitchProvider>
          {children}
        </KillSwitchProvider>
      </body>
    </html>
  );
}
