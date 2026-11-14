"""
Akwam (أكوام) Scraper.
Domain: https://akw.cam
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class AkwamScraper(BaseScraper):
    provider_id = "akwam"
    provider_name = "أكوام"
    base_url = "https://akw.cam"
    mirror_urls = ["https://akwam.cx"]
    
    movies_path = "/movies?page={page}"
    series_path = "/series?page={page}"
    search_path = "/?s={query}"
    
    card_selectors = [
        "a.br-card-link[href*='/akfilm-']",
        "a.br-card-link[href*='/akseries-']",
        "a[href*='/akfilm-']",
        "a[href*='/akseries-']",
        ".br-card a",
        "div.widget-body a.box",
        ".widget a.box",
        "article.entry-box a"
    ]
    title_selectors = ["h3.br-card-title", "h3", "h2", ".title", ".entry-title"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        html, final_url = self.fetch_html(watch_or_detail_url)
        if not html:
            return []

        soup = self.parse_soup(html)

        # Look for direct watch button or /watch/ link
        watch_btn = soup.find("a", href=re.compile(r'/watch/|/servers/|/download/'))
        if watch_btn:
            watch_url = self.absolute_url(watch_btn["href"], final_url)
            w_html, w_final = self.fetch_html(watch_url, referer=final_url)
            if w_html:
                soup = self.parse_soup(w_html)
                final_url = w_final

        # 1. Video and audio elements or direct stream buttons
        for a in soup.select("a.link-btn[href], a.btn-download[href], a.btn-watch[href]"):
            href = a.get("href")
            if href and href not in seen and href.startswith("http"):
                seen.add(href)
                name = self.clean_text(a.text) or "سيرفر أكوام المباشر"
                resolved = StreamExtractor.resolve(href, referer=final_url)
                servers.append(StreamServer(
                    name=name,
                    embed_url=href,
                    direct_url=resolved["direct_url"] if resolved else href,
                    quality="FHD (Direct Akwam)",
                    server_type="direct"
                ))

        # 2. Embed iframes
        for iframe in soup.find_all("iframe"):
            src = iframe.get("src") or iframe.get("data-src")
            if src and src.startswith("http") and src not in seen:
                seen.add(src)
                resolved = StreamExtractor.resolve(src, referer=final_url)
                servers.append(StreamServer(
                    name=f"سيرفر بديل {len(servers)+1}",
                    embed_url=src,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

        return servers

    def get_episodes(self, series_url: str) -> List[Episode]:
        html, final_url = self.fetch_html(series_url)
        if not html:
            return []
        soup = self.parse_soup(html)
        episodes: List[Episode] = []
        seen_urls = set()

        for a in soup.find_all("a", href=re.compile(r'/episode/|/ep-\d+|-eps-|-ep\d+', re.I)):
            href = a.get("href")
            if not href:
                continue
            full_url = self.absolute_url(href, final_url)
            if full_url in seen_urls:
                continue
            seen_urls.add(full_url)
            title = self.clean_text(a.text) or "حلقة"
            m = re.search(r'\b(?:حلقة|episode|ep)\s*(\d+)\b', title, re.I)
            ep_num = int(m.group(1)) if m else len(episodes) + 1
            episodes.append(Episode(number=ep_num, title=title, url=full_url))

        episodes.sort(key=lambda e: e.number)
        return episodes
