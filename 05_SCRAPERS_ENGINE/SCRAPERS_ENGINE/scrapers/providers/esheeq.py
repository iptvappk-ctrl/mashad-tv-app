"""
Esheeq / قصة عشق Scraper.
Domain: https://qesset.net
Specializes in Turkish Series dubbed in Arabic.

Note: qesset.net uses shot90.com redirect links with base64-encoded real URLs.
Real URL extraction: base64 decode the `url=` param from shot90.com links.
"""
import re
import base64
from urllib.parse import urlparse, parse_qs, unquote
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor


class EsheeqScraper(BaseScraper):
    provider_id = "esheeq"
    provider_name = "قصة عشق"
    base_url = "https://qesset.net"
    mirror_urls = ["https://qeseh.net"]

    movies_path = "/category/yeni-filmler/page/{page}/"
    series_path = "/son-bolumler/page/{page}/"
    search_path = "/?s={query}"

    # Articles with shot90.com redirect links
    card_selectors = [
        "article a[href*='shot90.com']",
        "article a",
        "a[href*='qesset.net']",
    ]
    title_selectors = ["h2", "h3", ".title", ".entry-title", "img[alt]"]

    def _decode_shot90(self, shot_url: str) -> str:
        """Decode shot90.com base64 redirect to real qesset.net URL."""
        try:
            parsed = urlparse(shot_url)
            params = parse_qs(parsed.query)
            encoded = params.get('url', [None])[0]
            if encoded:
                # Add padding if needed
                padded = encoded + '=' * (-len(encoded) % 4)
                decoded = base64.b64decode(padded).decode('utf-8')
                return decoded
        except Exception:
            pass
        return shot_url

    def get_movies(self, page: int = 1) -> List[MediaItem]:
        items = super().get_movies(page)
        return self._decode_items(items)

    def get_series(self, page: int = 1) -> List[MediaItem]:
        items = super().get_series(page)
        return self._decode_items(items)

    def search(self, query: str, page: int = 1) -> List[MediaItem]:
        items = super().search(query, page)
        return self._decode_items(items)

    def _decode_items(self, items: List[MediaItem]) -> List[MediaItem]:
        """Resolve shot90.com redirects to real URLs."""
        for item in items:
            if item.url and 'shot90.com' in item.url:
                item.url = self._decode_shot90(item.url)
        return items

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
                    name=f"سيرفر قصة عشق {len(servers)+1}",
                    embed_url=src,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality", "HD") if resolved else "HD",
                ))

        return servers
