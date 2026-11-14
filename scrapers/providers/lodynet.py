"""
Lodynet (لودي نت) Scraper.
Domain: https://lodynet.watch
Specializes in Indian & Asian Drama.
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class LodynetScraper(BaseScraper):
    provider_id = "lodynet"
    provider_name = "لودي نت"
    base_url = "https://lodynet.watch"
    mirror_urls = ["https://lodynet.com"]
    
    movies_path = "/category/افلام-هندية/page/{page}/"
    series_path = "/category/مسلسلات-هنديه/page/{page}/"
    search_path = "/?s={query}"
    
    card_selectors = [
        "a[href*='%d9%85%d8%b3%d9%84%d8%b3%d9%84-']",
        "a[href*='%d9%81%d9%8a%d9%84%d9%85-']",
        "article.bt a",
        ".content-box article a",
        "h2.entry-title a",
        "article a",
    ]
    title_selectors = ["h2", "h3", ".entry-title", ".title", "[title]"]

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
