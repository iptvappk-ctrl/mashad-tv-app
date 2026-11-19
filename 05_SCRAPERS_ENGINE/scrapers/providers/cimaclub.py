"""
CimaClub (سيما كلوب) Scraper.
Domain: https://cimaclub.live
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class CimaClubScraper(BaseScraper):
    provider_id = "cimaclub"
    provider_name = "سيما كلوب"
    base_url = "https://cimaclub.live"
    mirror_urls = ["https://cimaclub.living"]
    
    movies_path = "/category/movies/page/{page}/"
    series_path = "/category/series/page/{page}/"
    search_path = "/?s={query}"
    
    card_selectors = [
        "article.MovieItem a",
        ".MovieItem a",
        "div.GridItem a",
        "article a",
        "h3.Title a",
        "a[href*='/watch/']",
    ]
    title_selectors = ["h3.Title", "h3", "h2", ".Title", "[title]"]

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
                    name=f"سيرفر {len(servers)+1}",
                    embed_url=src,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

        for el in soup.select("ul.servers-list a, .watch-servers a, [data-link]"):
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
