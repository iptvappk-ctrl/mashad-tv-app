"""
CimaWbas (سيما وبس) Scraper.
Domain: https://cimawbas.rest
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class CimaWbasScraper(BaseScraper):
    provider_id = "cimawbas"
    provider_name = "سيما وبس"
    base_url = "https://cimawbas.rest"
    mirror_urls = ["https://cimawebas.com", "https://cima-wbas.net"]
    
    movies_path = "/movies.php?page={page}"
    series_path = "/all-series.php?page={page}"
    search_path = "/search.php?keywords={query}"
    
    card_selectors = [
        "li.pm-li-video a",
        "div.pm-li-category a",
        "a[href*='watch.php?vid=']",
        "ul.pm-ul-browse-videos li a",
    ]
    title_selectors = ["h3 a", "span.pm-video-title", "h3", "h2", "[title]"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        html, final_url = self.fetch_html(watch_or_detail_url)
        if not html:
            return []

        soup = self.parse_soup(html)

        # Tabbed server embeds with data-embed
        for el in soup.select("ul.list_servers li, [data-embed], a[data-video]"):
            embed = el.get("data-embed") or el.get("data-video")
            if embed and embed.startswith("http") and embed not in seen:
                seen.add(embed)
                name = self.clean_text(el.text) or f"سيرفر {len(servers)+1}"
                resolved = StreamExtractor.resolve(embed, referer=final_url)
                servers.append(StreamServer(
                    name=name,
                    embed_url=embed,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

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
