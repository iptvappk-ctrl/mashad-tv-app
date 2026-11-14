"""
SpaceMov (سبيس موف) Scraper.
Domain: https://spacemov.xyz
"""
import re
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class SpaceMovScraper(BaseScraper):
    provider_id = "spacemov"
    provider_name = "سبيس موف"
    base_url = "https://spacemov.xyz"
    mirror_urls = ["https://spacemov-tv.com"]
    
    movies_path = "/movies/page/{page}/"
    series_path = "/tv-series/page/{page}/"
    search_path = "/?s={query}"
    
    card_selectors = [
        "article.item.movies div.poster a",
        "article.item.tvshows div.poster a",
        "article.item a",
        "div.items article a",
    ]
    title_selectors = ["div.data h3 a", "h3", "h2", "[title]", ".title"]

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

        for el in soup.select("#playeroptionsul li, .dooplay_player_option[data-type]"):
            opt_id = el.get("data-post")
            opt_num = el.get("data-nume")
            name = self.clean_text(el.text) or f"سيرفر {len(servers)+1}"
            
            # DooPlay player AJAX endpoint
            if opt_id and opt_num:
                try:
                    admin_ajax = f"{self.current_base_url}/wp-admin/admin-ajax.php"
                    post_data = {
                        "action": "doo_player_ajax",
                        "post": opt_id,
                        "nume": opt_num,
                        "type": el.get("data-type", "movie")
                    }
                    post_headers = {
                        "X-Requested-With": "XMLHttpRequest",
                        "Referer": final_url
                    }
                    resp_html, _ = self.fetch_html(admin_ajax, post_data=post_data, custom_headers=post_headers)
                    if resp_html:
                        m = re.search(r'src=["\']([^"\']+)["\']', resp_html)
                        if m:
                            embed_url = m.group(1)
                            if embed_url not in seen:
                                seen.add(embed_url)
                                resolved = StreamExtractor.resolve(embed_url, referer=final_url)
                                servers.append(StreamServer(
                                    name=name,
                                    embed_url=embed_url,
                                    direct_url=resolved["direct_url"] if resolved else None,
                                    quality=resolved.get("quality", "HD") if resolved else "HD",
                                ))
                except Exception:
                    pass

        return servers
