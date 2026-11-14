"""
CimaNow (سيما ناو) Scraper.
Domain: https://cimanow.cc
Features token resolution via POST /watch/resolve
"""
import re
import json
from typing import List, Optional
from ..base import BaseScraper
from ..models import MediaItem, MediaDetail, StreamServer, Episode
from ..extractors import StreamExtractor

class CimaNowScraper(BaseScraper):
    provider_id = "cimanow"
    provider_name = "سيما ناو"
    base_url = "https://cimanow.cc"
    mirror_urls = ["https://cimanow.net", "https://cimanow.info"]
    
    movies_path = "/category/الافلام/page/{page}/"
    series_path = "/category/المسلسلات/page/{page}/"
    search_path = "/?s={query}"
    
    card_selectors = [
        "a[href*='%d9%81%d9%8a%d9%84%d9%85-']",
        "a[href*='%d9%85%d8%b3%d9%84%d8%b3%d9%84-']",
        "div.card a[href*='/film/']",
        "div.card a[href*='/episode/']",
        "div.movie-box a",
        "a.stretched-link",
        "div.card a",
    ]
    title_selectors = ["h3.card-title", "div.title", "h3", "h2", "[title]", "span.title"]

    def extract_servers(self, watch_or_detail_url: str) -> List[StreamServer]:
        servers: List[StreamServer] = []
        seen = set()

        # CimaNow watch page replaces /film/ or /episode/ with /watch/
        watch_url = watch_or_detail_url.replace("/film/", "/watch/").replace("/episode/", "/watch/")

        html, final_url = self.fetch_html(watch_url, referer=watch_or_detail_url)
        if not html:
            html, final_url = self.fetch_html(watch_or_detail_url)
            if not html:
                return []

        soup = self.parse_soup(html)

        # 1. Parse JSON servers array in script
        # const servers = [ {"id": 345, "name": "...", "resolve_token": "..."} ];
        servers_match = re.search(r'const\s+servers\s*=\s*(\[[^\]]+\]);', html)
        csrf_match = re.search(r'csrfToken\s*=\s*["\']([^"\']+)["\']', html)
        key_match = re.search(r'watchResolveKey\s*=\s*["\']([^"\']+)["\']', html)

        if servers_match:
            try:
                server_rows = json.loads(servers_match.group(1))
                csrf_token = csrf_match.group(1) if csrf_match else ""
                resolve_key = key_match.group(1) if key_match else ""

                resolve_endpoint = f"{self.current_base_url}/watch/resolve"
                for s_data in server_rows:
                    s_name = s_data.get("name", "سيرفر")
                    s_token = s_data.get("resolve_token", "")
                    if s_token:
                        try:
                            headers = {
                                "Accept": "application/json",
                                "Content-Type": "application/json",
                                "X-CSRF-TOKEN": csrf_token,
                                "Referer": final_url,
                            }
                            payload = {"token": s_token, "key": resolve_key}
                            resp = self.session.post(resolve_endpoint, json=payload, headers=headers, timeout=5)
                            if resp.status_code == 200:
                                r_json = resp.json()
                                embed_link = r_json.get("embed_url") or r_json.get("url") or r_json.get("stream")
                                if embed_link and embed_link not in seen:
                                    seen.add(embed_link)
                                    resolved = StreamExtractor.resolve(embed_link, referer=final_url)
                                    servers.append(StreamServer(
                                        name=f"سيما ناو {s_name}",
                                        embed_url=embed_link,
                                        direct_url=resolved["direct_url"] if resolved else None,
                                        quality=resolved.get("quality", "1080p FHD") if resolved else "1080p FHD",
                                        server_type="cimanow_resolve"
                                    ))
                        except Exception:
                            continue
            except Exception:
                pass

        # 2. Direct iframes as fallback
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
