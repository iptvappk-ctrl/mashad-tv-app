"""
Fushaar (فشار) Scraper.
Domain: https://fushaar.com
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class FushaarScraper(BaseScraper):
    provider_id = "fushaar"
    provider_name = "فشار"
    base_url = "https://fushaar.com"
    mirror_urls = []
    
    movies_path = "/movies/page/{page}/"
    series_path = "/series/page/{page}/"
    search_path = "/?s={query}"
    
    card_selectors = [
        "article.poster a",
        "article.movie a",
        "article a",
        ".poster a",
    ]
    title_selectors = [".title", "h3", "h2", "[title]", ".entry-title"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        html, final_url = self.fetch_html(watch_or_detail_url)
        if not html:
            return []

        soup = self.parse_soup(html)

        # Fushaar direct mp4 or iframes
        for video in soup.find_all(["video", "source"]):
            src = video.get("src")
            if src and src.startswith("http") and src not in seen:
                seen.add(src)
                servers.append(StreamServer(
                    name="مشغل فشار المباشر",
                    embed_url=src,
                    direct_url=src,
                    quality="1080p FHD",
                    server_type="direct"
                ))

        for iframe in soup.find_all("iframe"):
            src = iframe.get("src") or iframe.get("data-src")
            if src and src.startswith("http") and src not in seen:
                seen.add(src)
                resolved = StreamExtractor.resolve(src, referer=final_url)
                servers.append(StreamServer(
                    name=f"سيرفر {len(servers)+1}",
                    embed_url=src,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

        return servers
