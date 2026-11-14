"""
Stream Resolvers and Decryptors (Stream Extractors).
Extracts raw playable media streams (.m3u8 / .mp4) from embed links.

Integrated with adbypass module for ad shortener / redirect chain resolution
before attempting stream extraction from embed pages.
"""
import re
import base64
import urllib.parse
from typing import Optional, Dict, Any
import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

# Known ad shortener domains (bypass before hitting the embed page)
_AD_SHORTENER_HOSTS = {
    "shot90.com", "shrinkme.io", "adfoc.us", "fc.lc", "bc.vc",
    "za.gl", "ur.ly", "oke.io", "ouo.io", "sh.st", "cpa.st",
    "go2links.net", "earnload.net", "short.pe", "link.tl", "exe.io",
}

def _is_ad_shortener(url: str) -> bool:
    try:
        from urllib.parse import urlparse
        host = urlparse(url).netloc.lower().lstrip("www.")
        return host in _AD_SHORTENER_HOSTS
    except Exception:
        return False

def _resolve_redirect(url: str, referer: Optional[str] = None) -> str:
    """Follow redirect chain (including ad shorteners) to reach the real URL."""
    try:
        # shot90.com base64 decode
        if "shot90.com" in url:
            from urllib.parse import urlparse, parse_qs
            params = parse_qs(urlparse(url).query)
            encoded = params.get("url", [None])[0]
            if encoded:
                padded = encoded + "=" * (-len(encoded) % 4)
                decoded = base64.b64decode(padded).decode("utf-8")
                return decoded

        if _is_ad_shortener(url):
            resp = requests.get(
                url,
                headers={"User-Agent": DEFAULT_UA, "Referer": referer or "https://www.google.com/"},
                allow_redirects=True,
                verify=False,
                timeout=8,
            )
            final = resp.url
            # Strip tracking params
            from urllib.parse import urlparse, parse_qs, urlencode, urlunparse
            parsed = urlparse(final)
            junk = {"utm_source","utm_medium","utm_campaign","ref","referrer","fbclid","gclid"}
            clean = {k: v for k, v in parse_qs(parsed.query).items() if k not in junk}
            return urlunparse(parsed._replace(query=urlencode(clean, doseq=True)))
    except Exception:
        pass
    return url

class StreamExtractor:
    @staticmethod
    def get_headers(referer: Optional[str] = None) -> Dict[str, str]:
        headers = {
            "User-Agent": DEFAULT_UA,
            "Accept": "*/*",
            "Accept-Language": "ar,en-US;q=0.9,en;q=0.8",
        }
        if referer:
            headers["Referer"] = referer
        return headers

    @classmethod
    def resolve(cls, embed_url: str, referer: Optional[str] = None, timeout: int = 8) -> Optional[Dict[str, Any]]:
        """
        Main entry point: detects server type and extracts direct playable link.

        Steps:
        1. Resolve any ad-shortener redirect chain first (shot90, fc.lc, etc.)
        2. Detect embed host type from final URL
        3. Run the appropriate extractor
        4. Fall back to generic JS/regex extraction

        Returns dict: {"direct_url": ..., "type": "hls"|"mp4", "headers": {...}, "quality": "..."}
        """
        if not embed_url or not embed_url.startswith("http"):
            return None

        # Step 1: Resolve ad shortener / redirect chain BEFORE anything else
        resolved_url = _resolve_redirect(embed_url, referer)
        url_lower = resolved_url.lower()

        # Direct media link already
        if any(ext in url_lower for ext in [".m3u8", ".mp4"]):
            return {
                "direct_url": resolved_url,
                "type": "hls" if ".m3u8" in url_lower else "mp4",
                "headers": cls.get_headers(referer),
                "quality": "Direct"
            }

        try:
            if "vidmoly" in url_lower:
                return cls.extract_vidmoly(resolved_url, referer, timeout)
            elif "voe" in url_lower:
                return cls.extract_voe(resolved_url, referer, timeout)
            elif "uqload" in url_lower:
                return cls.extract_uqload(resolved_url, referer, timeout)
            elif "dood" in url_lower or "ds2play" in url_lower or "doood" in url_lower:
                return cls.extract_dood(resolved_url, referer, timeout)
            elif "streamtape" in url_lower:
                return cls.extract_streamtape(resolved_url, referer, timeout)
            elif "myseed" in url_lower or "player-gateway" in url_lower or "vids.php" in url_lower:
                return cls.extract_myseed_gateway(resolved_url, referer, timeout)
            elif "filemoon" in url_lower or "moonplayer" in url_lower:
                return cls.extract_generic(resolved_url, referer, timeout)
            elif "mixdrop" in url_lower:
                return cls.extract_generic(resolved_url, referer, timeout)
            elif "mp4upload" in url_lower:
                return cls.extract_generic(resolved_url, referer, timeout)
            elif "ok.ru" in url_lower or "okru" in url_lower:
                return cls.extract_generic(resolved_url, referer, timeout)
            else:
                return cls.extract_generic(resolved_url, referer, timeout)
        except Exception:
            return None

    @classmethod
    def extract_vidmoly(cls, embed_url: str, referer: Optional[str] = None, timeout: int = 8) -> Optional[Dict[str, Any]]:
        headers = cls.get_headers(referer or "https://vidmoly.to/")
        resp = requests.get(embed_url, headers=headers, verify=False, timeout=timeout)
        html = resp.text

        if "404 Not Found" in html or "Video deleted" in html or "file was deleted" in html:
            return None

        # Look for file: "https://...m3u8..." or sources: [{file:...}]
        m = re.search(r'(?:file|sources)\s*:\s*(?:\[\s*\{\s*)?[\'"]?(?:file)?[\'"]?\s*:\s*[\'"](https?://[^\'"]+\.m3u8[^\'"]*)[\'"]', html, re.I)
        if not m:
            m = re.search(r'[\'"](https?://[^\'"]+\.m3u8[^\'"]*)[\'"]', html, re.I)

        if m:
            return {
                "direct_url": m.group(1),
                "type": "hls",
                "headers": {"User-Agent": DEFAULT_UA, "Referer": embed_url},
                "quality": "Auto (Master HLS)"
            }
        return None

    @classmethod
    def extract_voe(cls, embed_url: str, referer: Optional[str] = None, timeout: int = 8) -> Optional[Dict[str, Any]]:
        headers = cls.get_headers(referer or "https://voe.sx/")
        resp = requests.get(embed_url, headers=headers, verify=False, timeout=timeout)
        html = resp.text

        # 1. Plain HLS match
        m = re.search(r'[\'"]hls[\'"]\s*:\s*[\'"](https?://[^\'"]+\.m3u8[^\'"]*)[\'"]', html)
        if not m:
            m = re.search(r'[\'"](https?://[^\'"]+\.m3u8[^\'"]*)[\'"]', html)

        if m:
            return {
                "direct_url": m.group(1),
                "type": "hls",
                "headers": {"User-Agent": DEFAULT_UA, "Referer": embed_url},
                "quality": "Auto (HLS)"
            }

        # 2. Base64 encoded atob match
        b64_matches = re.findall(r'atob\([\'"]([A-Za-z0-9+/=]+)[\'"]\)', html)
        for b64 in b64_matches:
            try:
                decoded = base64.b64decode(b64).decode('utf-8', errors='ignore')
                m_dec = re.search(r'(https?://[^\'"]+\.m3u8[^\'"]*)', decoded)
                if m_dec:
                    return {
                        "direct_url": m_dec.group(1),
                        "type": "hls",
                        "headers": {"User-Agent": DEFAULT_UA, "Referer": embed_url},
                        "quality": "Auto (HLS)"
                    }
            except Exception:
                continue

        return None

    @classmethod
    def extract_uqload(cls, embed_url: str, referer: Optional[str] = None, timeout: int = 8) -> Optional[Dict[str, Any]]:
        headers = cls.get_headers(referer or "https://uqload.to/")
        resp = requests.get(embed_url, headers=headers, verify=False, timeout=timeout)
        html = resp.text

        m = re.search(r'sources\s*:\s*\[\s*[\'"](https?://[^\'"]+\.mp4[^\'"]*)[\'"]', html)
        if not m:
            m = re.search(r'[\'"](https?://[^\'"]+\.mp4[^\'"]*)[\'"]', html)

        if m:
            return {
                "direct_url": m.group(1),
                "type": "mp4",
                "headers": {"User-Agent": DEFAULT_UA, "Referer": embed_url},
                "quality": "HD (Direct MP4)"
            }
        return None

    @classmethod
    def extract_dood(cls, embed_url: str, referer: Optional[str] = None, timeout: int = 8) -> Optional[Dict[str, Any]]:
        headers = cls.get_headers(referer or "https://doodstream.com/")
        resp = requests.get(embed_url, headers=headers, verify=False, timeout=timeout)
        html = resp.text

        m_pass = re.search(r'/pass_md5/([a-zA-Z0-9_\-]+)', html)
        if m_pass:
            domain = urllib.parse.urlparse(embed_url).netloc
            pass_url = f"https://{domain}/pass_md5/{m_pass.group(1)}"
            headers["Referer"] = embed_url
            r2 = requests.get(pass_url, headers=headers, verify=False, timeout=timeout)
            if r2.status_code == 200 and r2.text.strip():
                final_link = r2.text.strip() + "1234567890?token=" + m_pass.group(1)
                return {
                    "direct_url": final_link,
                    "type": "mp4",
                    "headers": {"User-Agent": DEFAULT_UA, "Referer": embed_url},
                    "quality": "HD (Dood)"
                }
        return None

    @classmethod
    def extract_streamtape(cls, embed_url: str, referer: Optional[str] = None, timeout: int = 8) -> Optional[Dict[str, Any]]:
        headers = cls.get_headers(referer or "https://streamtape.com/")
        resp = requests.get(embed_url, headers=headers, verify=False, timeout=timeout)
        html = resp.text

        # Streamtape obfuscates link across script lines
        m = re.search(r'document\.getElementById\([\'"]robotlink[\'"]\)\.innerHTML\s*=\s*[\'"]([^\'"]+)[\'"]\s*\+\s*\([\'"]([^\'"]+)[\'"]\)', html)
        if m:
            token = m.group(1) + m.group(2)
            if not token.startswith("http"):
                token = "https:" + token
            direct_url = token + "&stream=1"
            return {
                "direct_url": direct_url,
                "type": "mp4",
                "headers": {"User-Agent": DEFAULT_UA, "Referer": embed_url},
                "quality": "HD (Streamtape)"
            }
        return None

    @classmethod
    def extract_myseed_gateway(cls, embed_url: str, referer: Optional[str] = None, timeout: int = 8) -> Optional[Dict[str, Any]]:
        headers = cls.get_headers(referer or "https://m.myseed.tv/")
        # Step 1: if token in url
        if "vids.php" in embed_url:
            resp = requests.get(embed_url, headers=headers, verify=False, timeout=timeout)
            m_gw = re.search(r'src=["\']([^"\']+player-gateway[^"\']+)["\']', resp.text)
            if m_gw:
                gw_url = m_gw.group(1).replace('&amp;', '&')
                headers["Referer"] = embed_url
                resp2 = requests.get(gw_url, headers=headers, verify=False, timeout=timeout)
                m_src = re.search(r'<source\s+[^>]*src=["\']([^"\']+\.(?:mp4|m3u8)(?:\?[^"\']*)?)["\']', resp2.text)
                if m_src:
                    direct = m_src.group(1)
                    return {
                        "direct_url": direct,
                        "type": "hls" if ".m3u8" in direct else "mp4",
                        "headers": {"User-Agent": DEFAULT_UA, "Referer": gw_url},
                        "quality": "1080p (MySeed Fast)"
                    }
        elif "player-gateway" in embed_url:
            resp = requests.get(embed_url, headers=headers, verify=False, timeout=timeout)
            m_src = re.search(r'<source\s+[^>]*src=["\']([^"\']+\.(?:mp4|m3u8)(?:\?[^"\']*)?)["\']', resp.text)
            if m_src:
                direct = m_src.group(1)
                return {
                    "direct_url": direct,
                    "type": "hls" if ".m3u8" in direct else "mp4",
                    "headers": {"User-Agent": DEFAULT_UA, "Referer": embed_url},
                    "quality": "1080p (MySeed Fast)"
                }
        return None

    @classmethod
    def extract_generic(cls, embed_url: str, referer: Optional[str] = None, timeout: int = 8) -> Optional[Dict[str, Any]]:
        headers = cls.get_headers(referer)
        try:
            resp = requests.get(embed_url, headers=headers, verify=False, timeout=timeout)
            html = resp.text
            # check for video sources
            m = re.search(r'<source\s+[^>]*src=[\'"](https?://[^\'"]+\.(?:m3u8|mp4)[^\'"]*)[\'"]', html, re.I)
            if not m:
                m = re.search(r'[\'"](https?://[^\'"]+\.(?:m3u8|mp4)(?:\?[^\'"]*)?)[\'"]', html, re.I)
            if m:
                direct = m.group(1)
                return {
                    "direct_url": direct,
                    "type": "hls" if ".m3u8" in direct else "mp4",
                    "headers": {"User-Agent": DEFAULT_UA, "Referer": embed_url},
                    "quality": "HD (Auto)"
                }
        except Exception:
            pass
        return None
