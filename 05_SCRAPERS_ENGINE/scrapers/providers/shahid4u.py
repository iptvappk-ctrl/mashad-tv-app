"""
Shahid4U (شاهد فور يو) Scraper.
Domain: https://shahid4u.cam
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class Shahid4UScraper(BaseScraper):
    provider_id = "shahid4u"
    provider_name = "شاهد فور يو"
    base_url = "https://shahid4u.cam"
    mirror_urls = ["https://shahid4u.tv", "https://shahid4u.to"]
    
    movies_path = "/category/movies/page/{page}/"
    series_path = "/category/series/page/{page}/"
    search_path = "/?s={query}"
    
    card_selectors = [
        ".MovieItem a",
        "article.MovieItem a",
        ".GridItem a",
        "article a",
        "a[href*='/movie/']",
        "a[href*='/series/']",
    ]
    title_selectors = ["h3", "h2", ".Title", "strong", "[title]"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        html, final_url = self.fetch_html(watch_or_detail_url)
        if not html:
            return []

        soup = self.parse_soup(html)

        # Watch button
        watch_btn = soup.find("a", href=re.compile(r'/watch/'))
        if watch_btn:
            watch_url = self.absolute_url(watch_btn["href"], final_url)
            w_html, w_final = self.fetch_html(watch_url, referer=final_url)
            if w_html:
                soup = self.parse_soup(w_html)
                final_url = w_final

        # 1. Server lists
        for a in soup.select(".servers-list a, .watch-servers a, [data-embed]"):
            link = a.get("data-embed") or a.get("href")
            if link and link.startswith("http") and link not in seen:
                seen.add(link)
                name = self.clean_text(a.text) or f"سيرفر شاهد {len(servers)+1}"
                resolved = StreamExtractor.resolve(link, referer=final_url)
                servers.append(StreamServer(
                    name=name,
                    embed_url=link,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

        # 2. iframes
        for iframe in soup.find_all("iframe"):
            src = iframe.get("src") or iframe.get("data-src")
            if src and src.startswith("http") and src not in seen:
                seen.add(src)
                resolved = StreamExtractor.resolve(src, referer=final_url)
                servers.append(StreamServer(
                    name=f"مشغل {len(servers)+1}",
                    embed_url=src,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

        return servers
