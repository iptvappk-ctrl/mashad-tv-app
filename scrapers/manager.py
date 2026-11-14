"""
Unified Scraper Manager.
Orchestrates parallel search, multi-provider aggregation, failover, and extraction.
"""
import time
import urllib.parse
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import List, Dict, Optional, Any, Tuple

from .models import MediaItem, MediaDetail, StreamServer, Episode
from .base import BaseScraper
from .providers import ALL_PROVIDERS

class ScraperManager:
    def __init__(self, max_workers: int = 16, timeout: int = 10):
        self.max_workers = max_workers
        self.timeout = timeout
        self.providers: Dict[str, BaseScraper] = {}
        for pid, cls in ALL_PROVIDERS.items():
            self.providers[pid] = cls(timeout=timeout)

    def get_provider(self, provider_id: str) -> Optional[BaseScraper]:
        return self.providers.get(provider_id.lower())

    def list_providers(self) -> List[Dict[str, Any]]:
        rows = []
        for pid, scraper in self.providers.items():
            rows.append({
                "id": pid,
                "name": scraper.provider_name,
                "base_url": scraper.base_url,
                "current_url": scraper.current_base_url,
                "mirrors": scraper.mirror_urls,
            })
        return rows

    def detect_provider(self, url: str) -> Optional[BaseScraper]:
        """Detects which provider owns the given URL based on hostname."""
        parsed = urllib.parse.urlparse(url)
        host = parsed.netloc.lower()
        if not host:
            return None

        for scraper in self.providers.values():
            candidates = [scraper.base_url] + scraper.mirror_urls
            for c in candidates:
                c_host = urllib.parse.urlparse(c).netloc.lower()
                if c_host and (c_host in host or host in c_host):
                    return scraper

        # Fallback keyword match
        for scraper in self.providers.values():
            if scraper.provider_id in host:
                return scraper

        return None

    def search_all(self, query: str, provider_ids: Optional[List[str]] = None,
                   limit_per_provider: int = 10, max_total: int = 50) -> List[MediaItem]:
        """
        Executes parallel search across all (or selected) providers.
        """
        targets = []
        if provider_ids:
            for pid in provider_ids:
                p = self.get_provider(pid)
                if p:
                    targets.append(p)
        else:
            targets = list(self.providers.values())

        results: List[MediaItem] = []
        seen_titles = set()

        def do_search(scraper: BaseScraper):
            try:
                items = scraper.search(query)
                return scraper.provider_id, items[:limit_per_provider]
            except Exception:
                return scraper.provider_id, []

        with ThreadPoolExecutor(max_workers=min(len(targets), self.max_workers)) as executor:
            futures = [executor.submit(do_search, s) for s in targets]
            for future in as_completed(futures):
                try:
                    pid, items = future.result()
                    for item in items:
                        # Clean title key for deduplication
                        norm_title = "".join(item.title.split()).lower()
                        if norm_title not in seen_titles:
                            seen_titles.add(norm_title)
                            results.append(item)
                except Exception:
                    pass

        return results[:max_total]

    def get_movies(self, provider_id: Optional[str] = None, page: int = 1, limit_per_provider: int = 15) -> List[MediaItem]:
        if provider_id:
            p = self.get_provider(provider_id)
            return p.get_movies(page=page) if p else []

        results: List[MediaItem] = []
        def fetch_p(scraper: BaseScraper):
            try:
                return scraper.get_movies(page=page)[:limit_per_provider]
            except Exception:
                return []

        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            futures = [executor.submit(fetch_p, s) for s in self.providers.values()]
            for future in as_completed(futures):
                try:
                    results.extend(future.result())
                except Exception:
                    pass
        return results

    def get_series(self, provider_id: Optional[str] = None, page: int = 1, limit_per_provider: int = 15) -> List[MediaItem]:
        if provider_id:
            p = self.get_provider(provider_id)
            return p.get_series(page=page) if p else []

        results: List[MediaItem] = []
        def fetch_p(scraper: BaseScraper):
            try:
                return scraper.get_series(page=page)[:limit_per_provider]
            except Exception:
                return []

        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            futures = [executor.submit(fetch_p, s) for s in self.providers.values()]
            for future in as_completed(futures):
                try:
                    results.extend(future.result())
                except Exception:
                    pass
        return results

    def get_details(self, url: str) -> Optional[MediaDetail]:
        scraper = self.detect_provider(url)
        if not scraper:
            # Fallback to Arabseed or base
            scraper = self.providers.get("arabseed")
        if scraper:
            return scraper.get_details(url)
        return None

    def extract_servers(self, url: str) -> List[StreamServer]:
        scraper = self.detect_provider(url)
        if not scraper:
            scraper = self.providers.get("arabseed")
        if scraper:
            return scraper.extract_servers(url)
        return []

    def get_episodes(self, url: str) -> List[Episode]:
        scraper = self.detect_provider(url)
        if scraper:
            return scraper.get_episodes(url)
        return []

    def test_provider(self, provider_id: str, query: str = "عسل اسود") -> Dict[str, Any]:
        scraper = self.get_provider(provider_id)
        if not scraper:
            return {"id": provider_id, "status": "UNKNOWN_PROVIDER", "success": False}

        start_time = time.time()
        try:
            results = scraper.search(query)
            latency = round(time.time() - start_time, 2)
            return {
                "id": scraper.provider_id,
                "name": scraper.provider_name,
                "base_url": scraper.current_base_url,
                "success": len(results) > 0,
                "items_found": len(results),
                "latency_sec": latency,
                "sample_title": results[0].title if results else None,
                "sample_url": results[0].url if results else None,
            }
        except Exception as e:
            latency = round(time.time() - start_time, 2)
            return {
                "id": scraper.provider_id,
                "name": scraper.provider_name,
                "base_url": scraper.current_base_url,
                "success": False,
                "error": str(e),
                "latency_sec": latency,
            }

    def test_all_providers(self, query: str = "2024") -> List[Dict[str, Any]]:
        """Runs health checks concurrently for all 24 providers."""
        reports = []
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            futures = {executor.submit(self.test_provider, pid, query): pid for pid in self.providers}
            for future in as_completed(futures):
                try:
                    reports.append(future.result())
                except Exception as e:
                    pid = futures[future]
                    reports.append({"id": pid, "success": False, "error": str(e)})
        
        # Sort by id
        reports.sort(key=lambda r: r.get("id", ""))
        return reports
