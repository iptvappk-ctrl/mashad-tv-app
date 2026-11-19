"""
Cima4U (سيما فور يو) Scraper.
Domain: https://c4u.cam
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class Cima4UScraper(BaseScraper):
    provider_id = "cima4u"
    provider_name = "سيما فور يو"
    base_url = "https://c4u.cam"
    mirror_urls = ["https://cima4u.watch", "https://cima4u4k.site"]
    
    movies_path = "/movies/?page={page}"
    series_path = "/series/?page={page}"
    search_path = "/?s={query}"
    
    card_selectors = [
        ".MovieBlock a",
        ".Block--Movie a",
        "article.MovieBlock a",
        "a[href*='/watch/']",
        ".movies-list a",
        "article a",
    ]
    title_selectors = ["h3", "h2", ".Title", "strong", "[title]", "h4"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        html, final_url = self.fetch_html(watch_or_detail_url)
        if not html:
            return []

        soup = self.parse_soup(html)

        # Tab embeds and iframe sources
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

        for btn in soup.select("ul.servers-tabs li[data-server], a.server-btn[href], [data-url]"):
            link = btn.get("data-server") or btn.get("href") or btn.get("data-url")
            if link and link.startswith("http") and link not in seen:
                seen.add(link)
                name = self.clean_text(btn.text) or f"سيرفر {len(servers)+1}"
                resolved = StreamExtractor.resolve(link, referer=final_url)
                servers.append(StreamServer(
                    name=name,
                    embed_url=link,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

        return servers
