"""
Base scraper class with networking, mirror failover, common parsing logic,
and integrated ad/redirect bypass (via adbypass module).
"""
import re
import urllib.parse
from abc import ABC, abstractmethod
from typing import List, Optional, Dict, Any, Tuple
import requests
from bs4 import BeautifulSoup
import urllib3

from .models import MediaItem, MediaDetail, StreamServer, Episode, Season
from .extractors import StreamExtractor
from .adbypass import (
    resolve_page,
    bypass_ad_shortener,
    extract_meta_refresh,
    extract_js_redirect,
    extract_dooplay_params,
    fetch_dooplay_servers,
    fetch_wp_server_link,
    extract_data_links,
    strip_tracking_params,
    make_bypass_session,
)

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

class BaseScraper(ABC):
    provider_id: str = "base"
    provider_name: str = "Base Provider"
    base_url: str = ""
    mirror_urls: List[str] = []
    
    # URL patterns (e.g. "/movies?page={page}", "/search?q={query}")
    movies_path: str = ""
    series_path: str = ""
    search_path: str = ""
    
    # CSS Selectors
    card_selectors: List[str] = []
    title_selectors: List[str] = []

    def __init__(self, timeout: int = 10):
        self.timeout = timeout
        self.session = requests.Session()
        self.session.verify = False
        self.session.headers.update({
            "User-Agent": DEFAULT_UA,
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
            "Accept-Language": "ar,en-US;q=0.9,en;q=0.8",
            "Connection": "keep-alive",
        })
        self._active_base_url: Optional[str] = None

    @property
    def current_base_url(self) -> str:
        if self._active_base_url:
            return self._active_base_url
        return self.base_url

    def get_candidate_urls(self) -> List[str]:
        candidates = [self.base_url]
        for mirror in self.mirror_urls:
            if mirror and mirror not in candidates:
                candidates.append(mirror)
        return candidates

    def build_url(self, path: str, **kwargs) -> str:
        formatted_path = path
        for k, v in kwargs.items():
            formatted_path = formatted_path.replace(f"{{{k}}}", urllib.parse.quote(str(v)))
        if formatted_path.startswith("http"):
            return formatted_path
        base = self.current_base_url.rstrip("/")
        if not formatted_path.startswith("/"):
            formatted_path = "/" + formatted_path
        return base + formatted_path

    def fetch_html(self, url: str, referer: Optional[str] = None, params: Optional[Dict[str, Any]] = None,
                   post_data: Optional[Dict[str, Any]] = None, custom_headers: Optional[Dict[str, str]] = None) -> Tuple[Optional[str], str]:
        """
        Fetches HTML with mirror fallback. Returns (html_string, final_url).
        """
        headers = {}
        if referer:
            headers["Referer"] = referer
        else:
            headers["Referer"] = self.current_base_url + "/"
        if custom_headers:
            headers.update(custom_headers)

        candidates = [url]
        for cand_base in self.get_candidate_urls():
            if cand_base != self.current_base_url:
                alt_url = url.replace(self.current_base_url, cand_base)
                if alt_url not in candidates:
                    candidates.append(alt_url)

        for candidate_url in candidates:
            try:
                if post_data:
                    resp = self.session.post(candidate_url, data=post_data, headers=headers, timeout=self.timeout)
                else:
                    resp = self.session.get(candidate_url, params=params, headers=headers, timeout=self.timeout)

                if resp.status_code == 200 and len(resp.text.strip()) > 100:
                    parsed = urllib.parse.urlparse(candidate_url)
                    cand_base_origin = f"{parsed.scheme}://{parsed.netloc}"
                    if cand_base_origin in self.get_candidate_urls():
                        self._active_base_url = cand_base_origin
                    return resp.text, resp.url
            except Exception:
                continue

        return None, url

    def parse_soup(self, html: str) -> BeautifulSoup:
        return BeautifulSoup(html, "html.parser")

    def clean_text(self, text: Optional[str]) -> str:
        if not text:
            return ""
        return " ".join(text.split()).strip()

    def extract_year(self, text: str) -> Optional[str]:
        if not text:
            return None
        m = re.search(r'\b(19\d\d|20\d\d)\b', text)
        return m.group(1) if m else None

    def extract_quality(self, text: str) -> Optional[str]:
        if not text:
            return None
        m = re.search(r'\b(1080p|720p|480p|4k|2160p|bluray|web-dl|hdcam|hdrip|fhd)\b', text, re.I)
        return m.group(1).upper() if m else None

    def infer_media_type(self, title: str, url: str) -> str:
        text = (title + " " + url).lower()
        if any(w in text for w in ["مسلسل", "serie", "tv", "season", "موسم", "حلقة", "ep-", "eps-", "season-"]):
            return "series"
        return "movie"

    def absolute_url(self, link: str, current_page_url: Optional[str] = None) -> str:
        if not link:
            return ""
        if link.startswith("http://") or link.startswith("https://"):
            return link
        base = current_page_url or self.current_base_url
        return urllib.parse.urljoin(base, link)

    def parse_cards_standard(self, soup: BeautifulSoup, page_url: str, default_type: Optional[str] = None) -> List[MediaItem]:
        """
        Generic robust card parser using configured card_selectors and title_selectors.
        """
        items: List[MediaItem] = []
        seen_urls = set()

        elements = []
        for selector in self.card_selectors:
            found = soup.select(selector)
            if found:
                elements.extend(found)

        if not elements:
            # Fallback: find all links containing movie/series/watch keywords
            elements = soup.find_all("a", href=re.compile(r'/(movie|serie|watch|film|post|video)/', re.I))

        for el in elements:
            href = el.get("href")
            if not href:
                # Might be inside container
                a_tag = el.find("a", href=True)
                if a_tag:
                    href = a_tag["href"]
            if not href or href in ("#", "javascript:void(0);"):
                continue

            full_url = self.absolute_url(href, page_url)
            if full_url in seen_urls:
                continue

            # Ignore taxonomy, category, tag, and author links
            url_lower = full_url.lower()
            if any(tax in url_lower for tax in ["/category/", "/release-year/", "/genre/", "/quality/", "/tag/", "/actor/", "/director/", "/stars/"]):
                continue

            # Extract Title
            title = ""
            for t_sel in self.title_selectors:
                t_el = el.select_one(t_sel)
                if t_el and self.clean_text(t_el.text):
                    title = self.clean_text(t_el.text)
                    break

            if not title:
                title = el.get("title") or el.get("aria-label") or ""
                if not title:
                    img = el.find("img")
                    if img:
                        title = img.get("alt") or img.get("title") or ""
                if not title:
                    title = self.clean_text(el.text)

            title = self.clean_text(title)
            if not title or len(title) < 2 or title in ["مشاهدة", "تحميل", "المزيد", "التالي", "السابق"]:
                continue

            seen_urls.add(full_url)

            # Extract Poster
            poster = None
            img = el.find("img") if el.name != "img" else el
            if img:
                poster = img.get("data-src") or img.get("data-lazy-src") or img.get("src") or img.get("data-original")
                if poster:
                    poster = self.absolute_url(poster, page_url)

            # Extract Year & Quality
            raw_text = el.text
            year = self.extract_year(raw_text) or self.extract_year(title)
            quality = self.extract_quality(raw_text) or self.extract_quality(title)
            media_type = default_type or self.infer_media_type(title, full_url)

            items.append(MediaItem(
                title=title,
                url=full_url,
                provider_id=self.provider_id,
                provider_name=self.provider_name,
                media_type=media_type,
                poster=poster,
                year=year,
                quality=quality,
            ))

        return items

    # ── Abstract Interface ──

    def search(self, query: str, page: int = 1) -> List[MediaItem]:
        if not self.search_path:
            return []
        url = self.build_url(self.search_path, q=query, query=query, page=page)
        html, final_url = self.fetch_html(url)
        if not html:
            return []
        soup = self.parse_soup(html)
        return self.parse_cards_standard(soup, final_url)

    def get_movies(self, page: int = 1) -> List[MediaItem]:
        if not self.movies_path:
            return []
        url = self.build_url(self.movies_path, page=page)
        html, final_url = self.fetch_html(url)
        if not html:
            return []
        soup = self.parse_soup(html)
        return self.parse_cards_standard(soup, final_url, default_type="movie")

    def get_series(self, page: int = 1) -> List[MediaItem]:
        if not self.series_path:
            return []
        url = self.build_url(self.series_path, page=page)
        html, final_url = self.fetch_html(url)
        if not html:
            return []
        soup = self.parse_soup(html)
        return self.parse_cards_standard(soup, final_url, default_type="series")

    def get_details(self, url: str) -> Optional[MediaDetail]:
        html, final_url = self.fetch_html(url)
        if not html:
            return None
        soup = self.parse_soup(html)

        # Title: Prioritize specific title classes, h1, then document title
        title = ""
        for t_sel in [".post-title", ".entry-title", "h1.title", ".movie-title"]:
            t_match = soup.select_one(t_sel)
            if t_match and self.clean_text(t_match.text):
                title = self.clean_text(t_match.text)
                break

        if not title:
            h1 = soup.find("h1")
            if h1 and self.clean_text(h1.text) not in ["القائمه الرئيسيه", "الرئيسية", "القصة", "Home", "Main"]:
                title = self.clean_text(h1.text)

        if not title and soup.title:
            raw_title = soup.title.text.strip()
            # Remove site name suffix like '| ايجى ديد' or '- عرب سيد'
            title = re.sub(r'\s*[-|–—]\s*(ايجي بست|ايجى ديد|عرب سيد|أكوام|وي سيما|فاصل إعلاني|ماي سيما|سيما فور يو|موفيز لاند|لودي نت|قصة عشق|egybest|arabseed|akwam|wecima|c4u|faselhd|egydead|topcinema|lodynet).*$', '', raw_title, flags=re.I).strip()

        # Poster
        poster = None
        poster_el = soup.select_one(".poster img, .movie-poster img, .post-thumbnail img, img.attachment-post-thumbnail")
        if not poster_el:
            poster_el = soup.find("img", src=re.compile(r'uploads|posters|thumbs', re.I))
        if poster_el:
            poster = poster_el.get("data-src") or poster_el.get("src")
            if poster:
                poster = self.absolute_url(poster, final_url)

        # Story
        story = ""
        story_el = soup.select_one(".story, .post-story, .story-content, .entry-content p, .overview, p.story")
        if story_el:
            story = self.clean_text(story_el.text)

        # Cast
        cast = []
        for cast_el in soup.select(".cast a, .actors a, a[href*='/cast/'], a[href*='/actor/']"):
            c_name = self.clean_text(cast_el.text)
            if c_name and c_name not in cast:
                cast.append(c_name)

        # Year & Quality
        year = self.extract_year(soup.text)
        quality = self.extract_quality(soup.text)
        media_type = self.infer_media_type(title, final_url)

        # Extract servers from page or watch button
        servers = self.extract_servers(final_url)

        return MediaDetail(
            title=title or "بدون عنوان",
            url=final_url,
            provider_id=self.provider_id,
            provider_name=self.provider_name,
            media_type=media_type,
            poster=poster,
            year=year,
            story=story,
            quality=quality,
            cast=cast[:10],
            servers=servers
        )

    def extract_servers(self, watch_url: str) -> List[StreamServer]:
        """Default server extraction: extracts all iframes and data-link elements."""
        html, final_url = self.fetch_html(watch_url)
        if not html:
            return []
        soup = self.parse_soup(html)
        servers: List[StreamServer] = []
        seen = set()

        # 1. iframes
        for iframe in soup.find_all("iframe"):
            src = iframe.get("src") or iframe.get("data-src")
            if src and src not in seen and not any(ad in src for ad in ["google", "facebook", "doubleclick"]):
                src = self.absolute_url(src, final_url)
                seen.add(src)
                resolved = StreamExtractor.resolve(src, referer=final_url)
                servers.append(StreamServer(
                    name=f"سيرفر {len(servers)+1}",
                    embed_url=src,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality") if resolved else None,
                    headers=resolved.get("headers") if resolved else {},
                ))

        # 2. data-link elements
        for el in soup.select("[data-link]"):
            link = el.get("data-link")
            if link and link not in seen and link.startswith("http"):
                seen.add(link)
                name = self.clean_text(el.text) or f"سيرفر {len(servers)+1}"
                resolved = StreamExtractor.resolve(link, referer=final_url)
                servers.append(StreamServer(
                    name=name,
                    embed_url=link,
                    direct_url=resolved["direct_url"] if resolved else None,
                    quality=resolved.get("quality") if resolved else None,
                    headers=resolved.get("headers") if resolved else {},
                ))

        return servers

    def extract_servers_smart(self, watch_url: str) -> List[StreamServer]:
        """
        ENHANCED server extraction with full ad/redirect bypass.

        Uses adbypass module to handle:
        - Pattern 1: Ad shortener chains (shot90, fc.lc, ouo.io, …)
        - Pattern 2: Meta refresh redirects
        - Pattern 3: JS window.location redirects
        - Pattern 4: DooPlay AJAX server loading (WordPress)
        - Pattern 7: data-* embed/link HTML attributes
        - Pattern 8: Base64-encoded embed URLs in scripts
        - Standard: iframe src and data-src
        """
        html, final_url = self.fetch_html(watch_url)
        if not html:
            return []

        soup = self.parse_soup(html)
        base = self.current_base_url

        # Run master resolver — handles ALL patterns
        resolved_data = resolve_page(
            url=final_url,
            session=self.session,
            html=html,
            soup=soup,
            base_url=base,
        )

        servers: List[StreamServer] = []
        seen_embeds: set = set()

        for srv in resolved_data["servers"]:
            embed_url = srv.get("embed_url", "")
            if not embed_url or embed_url in seen_embeds:
                continue
            # Skip obvious non-video assets
            if any(embed_url.endswith(ext) for ext in [".jpg", ".png", ".gif", ".svg", ".webp", ".css", ".js"]):
                continue
            # Skip analytics/ad domains without video
            if any(d in embed_url for d in ["google-analytics", "doubleclick", "facebook.com/plugins", "disqus"]):
                continue

            seen_embeds.add(embed_url)

            # Resolve the embed to a direct stream URL
            stream = StreamExtractor.resolve(embed_url, referer=final_url)
            servers.append(StreamServer(
                name=srv.get("name", f"سيرفر {len(servers)+1}"),
                embed_url=embed_url,
                direct_url=stream["direct_url"] if stream else None,
                quality=srv.get("quality") or (stream.get("quality") if stream else "HD"),
                headers=stream.get("headers", {}) if stream else {},
            ))

        return servers

    def get_episodes(self, series_url: str) -> List[Episode]:
        html, final_url = self.fetch_html(series_url)
        if not html:
            return []
        soup = self.parse_soup(html)
        episodes: List[Episode] = []
        seen_urls = set()

        for a in soup.find_all("a", href=re.compile(r'/episode/|/ep-|-eps-|-ep\d+|-halka-', re.I)):
            href = a.get("href")
            if not href:
                continue
            full_url = self.absolute_url(href, final_url)
            if full_url in seen_urls:
                continue
            seen_urls.add(full_url)
            title = self.clean_text(a.text) or a.get("title") or "حلقة"
            m = re.search(r'\b(?:حلقة|episode|ep)\s*(\d+)\b', title, re.I)
            ep_num = int(m.group(1)) if m else len(episodes) + 1
            episodes.append(Episode(number=ep_num, title=title, url=full_url))

        episodes.sort(key=lambda e: e.number)
        return episodes
