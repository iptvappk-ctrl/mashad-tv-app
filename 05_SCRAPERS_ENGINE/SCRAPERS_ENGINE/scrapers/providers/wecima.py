"""
WeCima / MyCima (وي سيما / ماي سيما) Scraper.
Domain: https://wecima.cam
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class WeCimaScraper(BaseScraper):
    provider_id = "wecima"
    provider_name = "وي سيما"
    base_url = "https://mywecima.shop"
    mirror_urls = ["https://wecima.watch"]
    
    movies_path = "/movies.php?page={page}"
    series_path = "/all-series.php?page={page}"
    search_path = "/?s={query}"
    
    card_selectors = [
        "a[href*='watch.php?vid=']",
        ".Thumb--GridItem a",
        ".GridItem a",
        "div.Block--Item a",
        "li.pm-li-video a",
        "article a",
        "a[href*='/watch/']",
        ".Grid--WecimaPosts a"
    ]
    title_selectors = ["strong.hasyear", "h3", "h2", ".Title", "[title]", "span.title"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        # Check if URL has /watch/
        watch_url = watch_or_detail_url
        if "/watch/" not in watch_url:
            html_init, _ = self.fetch_html(watch_or_detail_url)
            if html_init:
                soup_init = self.parse_soup(html_init)
                btn = soup_init.find("a", href=re.compile(r'/watch/'))
                if btn:
                    watch_url = self.absolute_url(btn["href"], watch_or_detail_url)

        html, final_url = self.fetch_html(watch_url, referer=watch_or_detail_url)
        if not html:
            return []

        soup = self.parse_soup(html)

        # 1. Download/watch tabs list: ul.List--Download--Wecima--Single
        for a in soup.select("ul.List--Download--Wecima--Single a[href], .WatchServersList a[href], [data-url]"):
            link = a.get("href") or a.get("data-url")
            if link and link not in seen and link.startswith("http"):
                seen.add(link)
                name = self.clean_text(a.text) or f"سيرفر وي سيما {len(servers)+1}"
                resolved = StreamExtractor.resolve(link, referer=final_url)
                servers.append(StreamServer(
                    name=name,
                    embed_url=link,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                    server_type=resolved.get("type", "embed") if resolved else "embed"
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
