"""
ArabSeed (عرب سيد) Scraper.
Domain: https://m.myseed.tv
"""
import re
from typing import List, Optional
from bs4 import BeautifulSoup
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class ArabseedScraper(BaseScraper):
    provider_id = "arabseed"
    provider_name = "عرب سيد"
    base_url = "https://m.myseed.tv"
    mirror_urls = ["https://m.arabseed.site", "https://arabseed.show"]
    
    movies_path = "/movies/page/{page}/"
    series_path = "/series/page/{page}/"
    search_path = "/find/?word={query}"
    
    card_selectors = [
        "a[href*='%d9%81%d9%8a%d9%84%d9%85-']",
        "a[href*='%d9%85%d8%b3%d9%84%d8%b3%d9%84-']",
        "a[href*='/%d9%81%d9%8a%d9%84%d9%85']",
        "a[href*='/%d9%85%d8%b3%d9%84%d8%b3%d9%84']",
        "ul.Blocks-Ul li.BlockItem",
        "div.MovieBlock",
        "a.MovieItem",
        "a.BlockItem-Link",
        "div.BlockItem",
    ]
    title_selectors = ["span", "h4.Title", ".BlockTitle", "strong", "h3", "h2", ".title"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        # Step 1: Normalize watch url
        watch_url = watch_or_detail_url
        if "/watch/" not in watch_url:
            watch_url = watch_url.rstrip("/") + "/watch/"

        html, final_url = self.fetch_html(watch_url, referer=watch_or_detail_url)
        if not html:
            # Fallback to direct url
            html, final_url = self.fetch_html(watch_or_detail_url)
            if not html:
                return []

        soup = self.parse_soup(html)

        # 1. MySeed internal fast gateway: vids.php?t=... or vids.php?id=...
        vids_matches = re.findall(r'(https?://[^\'"]+vids\.php\?[^\'"]+)', html)
        if not vids_matches:
            token_m = re.search(r'vids\.php\?t=([a-f0-9]+)', html)
            if token_m:
                vids_matches = [f"{self.current_base_url}/vids.php?t={token_m.group(1)}"]

        for vids_url in vids_matches:
            if vids_url in seen:
                continue
            seen.add(vids_url)
            resolved = StreamExtractor.extract_myseed_gateway(vids_url, referer=final_url)
            servers.append(StreamServer(
                name="سيرفر عرب سيد الصاروخي (MySeed VIP)",
                embed_url=vids_url,
                direct_url=resolved["direct_url"] if resolved else None,
                quality=resolved.get("quality", "1080p FHD") if resolved else "1080p FHD",
                server_type="myseed_gateway"
            ))

        # 2. Buttons with data-link
        for btn in soup.select("[data-link], .watch-server-button, .servers-list li"):
            link = btn.get("data-link") or btn.get("data-url")
            name = self.clean_text(btn.text) or f"سيرفر {len(servers)+1}"
            if link and link not in seen and link.startswith("http"):
                seen.add(link)
                resolved = StreamExtractor.resolve(link, referer=final_url)
                servers.append(StreamServer(
                    name=name,
                    embed_url=link,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                    server_type=resolved.get("type", "embed") if resolved else "embed"
                ))

        # 3. Iframes
        for iframe in soup.find_all("iframe"):
            src = iframe.get("src") or iframe.get("data-src")
            if src and src.startswith("http") and src not in seen:
                seen.add(src)
                resolved = StreamExtractor.resolve(src, referer=final_url)
                servers.append(StreamServer(
                    name=f"سيرفر خارجي {len(servers)+1}",
                    embed_url=src,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

        return servers
