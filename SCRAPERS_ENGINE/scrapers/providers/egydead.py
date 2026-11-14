"""
EgyDead (إيجي ديد) Scraper.
Domain: https://egidead.cam
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class EgyDeadScraper(BaseScraper):
    provider_id = "egydead"
    provider_name = "إيجي ديد"
    base_url = "https://egidead.cam"
    mirror_urls = ["https://egydead.surf"]
    
    movies_path = "/movies/page/{page}/"
    series_path = "/series/page/{page}/"
    search_path = "/?s={query}"
    
    card_selectors = [
        "li.movieItem a",
        ".movieItem a",
        "a[href*='%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-']",
        "a[href*='%d9%81%d9%8a%d9%84%d9%85-']",
        "a[href*='%d9%85%d8%b3%d9%84%d8%b3%d9%84-']",
        "article.post-item a",
        "div.movie-card a",
        "a.box",
        "div.post a",
        "article a",
    ]
    title_selectors = ["h2.title", "span.movie-title", "h2", "h3", "[title]"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        # EgyDead direct watch link usually has ?watch=1
        watch_url = watch_or_detail_url
        if "?watch=" not in watch_url:
            watch_url = watch_url.rstrip("/") + "/?watch=1"

        html, final_url = self.fetch_html(watch_url, referer=watch_or_detail_url)
        if not html:
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
                    name=f"سيرفر إيجي ديد {len(servers)+1}",
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
