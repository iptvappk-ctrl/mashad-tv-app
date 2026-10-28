import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "مَشْهَدْ TV | منصة السينما والبث المباشر",
  description: "أقوى منصة لمشاهدة أحدث الأفلام والمسلسلات ومباريات كرة القدم الحية بجودة عالية وبدون إعلانات",
  manifest: "/manifest.json",
  icons: {
    icon: "/logo.jpg",
    apple: "/logo.jpg",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ar" dir="rtl">
      <body className="min-h-screen antialiased">
        {children}
      </body>
    </html>
  );
}
