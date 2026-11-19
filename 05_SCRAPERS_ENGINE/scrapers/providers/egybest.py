"""
EgyBest (إيجي بست) Scraper.
Domain: https://egybest.live
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class EgyBestScraper(BaseScraper):
    provider_id = "egybest"
    provider_name = "إيجي بست"
    base_url = "https://egybest.live"
    mirror_urls = []
    
    movies_path = "/browse.php?type=movie&page={page}&lang=ar"
    series_path = "/browse.php?type=tv&page={page}&lang=ar"
    search_path = "/search.php?q={query}&lang=ar"
    
    card_selectors = [
        "a.movie-card",
        "a.movie",
        ".movies-list-full a.movie",
        ".content-box a",
        "div.movie-card a",
        "div.movies a",
    ]
    title_selectors = [".card-title", "h3", "h2", ".title", ".movie-title", "span.title"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        html, final_url = self.fetch_html(watch_or_detail_url)
        if not html:
            return []

        soup = self.parse_soup(html)

        # Check for watch button or /watch/ link
        watch_btn = soup.find("a", href=re.compile(r'/watch/'))
        if watch_btn:
            watch_url = self.absolute_url(watch_btn["href"], final_url)
            w_html, w_final = self.fetch_html(watch_url, referer=final_url)
            if w_html:
                soup = self.parse_soup(w_html)
                final_url = w_final

        # 1. iframes
        for iframe in soup.find_all("iframe"):
            src = iframe.get("src") or iframe.get("data-src")
            if src and src.startswith("http") and src not in seen:
                seen.add(src)
                resolved = StreamExtractor.resolve(src, referer=final_url)
                servers.append(StreamServer(
                    name=f"سيرفر إيجي بست {len(servers)+1}",
                    embed_url=src,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "1080p") if resolved else "1080p",
                ))

        # 2. Server links with data-url or data-link
        for el in soup.select("[data-url], [data-link], .server-item a"):
            link = el.get("data-url") or el.get("data-link") or el.get("href")
            if link and link.startswith("http") and link not in seen:
                seen.add(link)
                name = self.clean_text(el.text) or f"سيرفر {len(servers)+1}"
                resolved = StreamExtractor.resolve(link, referer=final_url)
                servers.append(StreamServer(
                    name=name,
                    embed_url=link,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

        return servers
