"""
FaselHD (فاصل إعلاني) Scraper.
Domain: https://faselhd.top
Features AJAX embed resolving endpoint: POST /ajax/embed with id={data-embed}
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class FaselHdScraper(BaseScraper):
    provider_id = "faselhd"
    provider_name = "فاصل إعلاني"
    base_url = "https://faselhd.top"
    mirror_urls = ["https://www.fasel-hd.co", "https://faselhd.club"]
    
    movies_path = "/movies/?page={page}"
    series_path = "/series/?page={page}"
    search_path = "/?s={query}"
    
    card_selectors = [
        ".postDiv a",
        "div.list-movie a",
        "a.list-title",
        "a[href*='/movie/']",
        "a[href*='/serie/']",
        "article.postDiv a",
    ]
    title_selectors = [".list-title", ".post__title", "h2", "h3", ".title"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        html, final_url = self.fetch_html(watch_or_detail_url)
        if not html:
            return []

        soup = self.parse_soup(html)

        # 1. FaselHD uses button[data-embed] or [data-url]
        buttons = soup.select("button.btn-service[data-embed], [data-embed], .player-servers li[data-embed]")
        ajax_url = f"{self.current_base_url}/ajax/embed"

        for btn in buttons:
            embed_id = btn.get("data-embed")
            if not embed_id:
                continue
            name = self.clean_text(btn.text) or f"سيرفر فاصل {len(servers)+1}"

            # Request POST /ajax/embed
            try:
                post_headers = {
                    "X-Requested-With": "XMLHttpRequest",
                    "Referer": final_url,
                    "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                }
                resp_html, _ = self.fetch_html(
                    ajax_url,
                    referer=final_url,
                    post_data={"id": embed_id},
                    custom_headers=post_headers
                )
                if resp_html:
                    iframe_m = re.search(r'src=["\']([^"\']+)["\']', resp_html)
                    if iframe_m:
                        real_url = iframe_m.group(1)
                        if real_url.startswith("//"):
                            real_url = "https:" + real_url
                        if real_url not in seen:
                            seen.add(real_url)
                            resolved = StreamExtractor.resolve(real_url, referer=final_url)
                            servers.append(StreamServer(
                                name=name,
                                embed_url=real_url,
                                direct_url=resolved["direct_url"] if resolved else None,
                                quality=resolved.get("quality", "1080p") if resolved else "1080p",
                                server_type=resolved.get("type", "fasel_ajax") if resolved else "fasel_ajax"
                            ))
            except Exception:
                continue

        # 2. Any direct iframes present
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
