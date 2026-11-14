"""
Cima4Up (سيما فور أب) Scraper.
Domain: https://cima4up-official.cyou
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class Cima4UpScraper(BaseScraper):
    provider_id = "cima4up"
    provider_name = "سيما فور أب"
    base_url = "https://cima4up-official.cyou"
    mirror_urls = []
    
    movies_path = "/movies/page/{page}/"
    series_path = "/series/page/{page}/"
    search_path = "/?s={query}"

    # Confirmed via live probe: articles with href /title/<slug>/
    card_selectors = [
        "article a[href*='/title/']",
        "article a",
        "a[href*='/title/']",
        "div.movie-card a",
    ]
    title_selectors = ["h2", "h3", "span.title", ".card-title", "img[alt]", "[title]"]

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

        for el in soup.select("ul.servers-tabs li[data-server], a.server-btn, [data-link]"):
            link = el.get("data-server") or el.get("data-link") or el.get("href")
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
