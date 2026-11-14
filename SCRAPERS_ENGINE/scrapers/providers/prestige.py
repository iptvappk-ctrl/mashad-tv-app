"""
Prestige (بريستيج) Scraper.
Domain: https://prstej.asia
Specializes in Arabic & Ramadan series.
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class PrestigeScraper(BaseScraper):
    provider_id = "prestige"
    provider_name = "بريستيج"
    base_url = "https://prstej.asia"
    mirror_urls = ["https://prstej.lat", "https://brstej.com"]
    
    movies_path = "/movies"
    series_path = "/series"
    search_path = "/?s={query}"
    
    card_selectors = [
        ".pgh-card a",
        ".pgh-series-card a",
        ".ptoday-card a",
        ".movie__block a",
        "article a",
        "a[href*='/movie/']",
        "a[href*='/series/']",
        "a[href*='/episode/']",
    ]
    title_selectors = [".pgh-card-title", ".pgh-series-name", "h2", "h3", ".Title", "[title]"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        html, final_url = self.fetch_html(watch_or_detail_url)
        if not html:
            return []

        soup = self.parse_soup(html)

        for iframe in soup.find_all("iframe"):
            src = iframe.get("src") or iframe.get("data-src")
            if src and src.startswith("http") and src not in seen:
                seen.add(src)
                resolved = StreamExtractor.resolve(src, referer=final_url)
                servers.append(StreamServer(
                    name=f"سيرفر بريستيج {len(servers)+1}",
                    embed_url=src,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

        for el in soup.select("ul.servers a, .server--item a, [data-link]"):
            link = el.get("data-link") or el.get("href")
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
