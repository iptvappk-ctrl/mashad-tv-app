"""
Ad Bypass & Redirect Handler for Arabic Streaming Sites.
========================================================

This module handles ALL known ad/redirect patterns found in Arabic movie/series sites:

Pattern 1  — Short URL Redirect Chains
         (shot90.com, shrinkme.io, fc.lc, bc.vc, adfoc.us, ouo.io, etc.)
         These encode the real URL as a base64 or query param.
         Resolution: decode param → follow real URL.

Pattern 2  — Meta Refresh Redirect
         <meta http-equiv="refresh" content="5; url=https://real.site/">
         Resolution: parse meta tag and follow the url.

Pattern 3  — JavaScript window.location Redirect
         window.location = "https://real.site/";
         Resolution: regex extract and follow.

Pattern 4  — DooPlay AJAX Server Loading (WordPress sites)
         POST /wp-admin/admin-ajax.php  action=doo_player_ajax  post=ID  nonce=XXX
         Returns JSON with embed iframe src.
         Used by: SpaceMov, MovieWatcher, Cima4Up, and many more.

Pattern 5  — FaselHD AJAX Embed
         POST /ajax/embed   data-embed=ID
         Returns JSON {"link": "https://embed.server/xxx"}

Pattern 6  — WeCima / Akwam Server Tab AJAX
         POST /?ajax=server  or  /wp-admin/admin-ajax.php  action=get_server_link
         Returns embed URL.

Pattern 7  — data-link / data-url / data-embed encoded attributes
         <li data-link="https://embed.server/xxx" data-quality="1080p">
         Resolution: direct extraction from HTML attributes.

Pattern 8  — Base64-Encoded Embed Links
         Some sites encode embed URLs in base64 inside a script or attribute.
         Resolution: detect and decode base64 blobs that decode to URLs.

Pattern 9  — Cloudflare / Bot Protection (CANNOT bypass without browser)
         faselhd.top, shahid4u.cam
         These require a real browser session. Marked as BROWSER_REQUIRED.

Pattern 10 — Hostname-only Redirect (301/302 chain through ad domain)
         Resolution: requests.get(..., allow_redirects=True) handles this natively.
         Extra: strip ad-network tracking params from final URL.
"""

import re
import base64
import time
import random
import hashlib
import json
from urllib.parse import urlparse, parse_qs, urlencode, urljoin, unquote
from typing import Optional, Dict, Tuple, List
import requests
import warnings

warnings.filterwarnings("ignore")

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

DEFAULT_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

MOBILE_UA = (
    "Mozilla/5.0 (Linux; Android 14; Pixel 7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.6422.112 Mobile Safari/537.36"
)

# Known ad/shortener domains whose `url=` or path encodes the real destination
AD_SHORTENER_DOMAINS = {
    "shot90.com",          # Base64 in ?url= param
    "shrinkme.io",         # GET → 302 to real URL after solving
    "adfoc.us",            # Requires JS; follow 302
    "fc.lc",               # GET → 302 chain
    "bc.vc",               # GET → 302 chain
    "za.gl",               # GET → 302 chain
    "ur.ly",               # GET → 302 chain
    "oke.io",              # GET → 302 chain
    "ouo.io",              # POST with token
    "sh.st",               # GET + cookie
    "cpa.st",              # 302 chain
    "go2links.net",        # 302 chain
    "earnload.net",        # 302 chain
    "short.pe",            # 302 chain
    "link.tl",             # 302 chain
    "exe.io",              # 302 chain
    "linkvertise.com",     # Requires Cloudflare bypass — SKIP
    "up-load.io",          # 302 chain
}

# Domains that are definitely NOT ad networks (don't follow these)
VIDEO_HOST_DOMAINS = {
    "vidmoly.to", "voe.sx", "uqload.co", "uqload.com",
    "dood.watch", "doodstream.com", "streamtape.com",
    "streamtape.to", "streamtape.net", "stream.biz",
    "okru.com", "ok.ru", "dailymotion.com",
    "youtube.com", "youtu.be",
    "mp4upload.com", "filemoon.sx", "mixdrop.ag",
}

# Tracking/analytics query parameters to strip from final URLs
JUNK_PARAMS = {
    "utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term",
    "ref", "referrer", "fbclid", "gclid", "msclkid", "ttclid",
    "_ga", "source", "from", "via",
}


# ===========================================================================
# Pattern 1 — Short URL / Ad Redirect Chain
# ===========================================================================

def _is_ad_shortener(url: str) -> bool:
    """Return True if url passes through a known ad shortener."""
    try:
        host = urlparse(url).netloc.lower().lstrip("www.")
        return host in AD_SHORTENER_DOMAINS
    except Exception:
        return False


def _decode_shot90(url: str) -> Optional[str]:
    """
    shot90.com encodes the real URL as base64 in the `url=` query parameter.
    Example: https://shot90.com/article/xxx?url=aHR0cHM6Ly9xZXNzZXQubmV0Lw==
    """
    try:
        parsed = urlparse(url)
        params = parse_qs(parsed.query)
        encoded = params.get("url", [None])[0]
        if not encoded:
            return None
        padded = encoded + "=" * (-len(encoded) % 4)
        return base64.b64decode(padded).decode("utf-8")
    except Exception:
        return None


def bypass_ad_shortener(url: str, session: requests.Session, referer: str = "") -> str:
    """
    Follow an ad shortener redirect chain and return the final real URL.
    Uses domain-specific decoders first, then falls back to HTTP following.

    Args:
        url:     The ad/shortener URL to resolve.
        session: A requests.Session (with SSL verify=False, timeout set).
        referer: Optional referer header.

    Returns:
        The resolved real URL, or the original url if resolution fails.
    """
    if not _is_ad_shortener(url):
        return url

    host = urlparse(url).netloc.lower().lstrip("www.")

    # shot90.com — base64 decode
    if host == "shot90.com":
        decoded = _decode_shot90(url)
        if decoded:
            return decoded

    # Generic: follow 301/302 redirect chain (requests does this automatically)
    # We set allow_redirects=True and return the final URL after hops
    try:
        resp = session.get(
            url,
            headers={
                "User-Agent": MOBILE_UA,
                "Referer": referer or "https://www.google.com/",
                "Accept": "text/html,application/xhtml+xml,*/*",
            },
            allow_redirects=True,
            timeout=8,
        )
        final = resp.url
        # Strip junk tracking params from final URL
        final = strip_tracking_params(final)
        return final
    except Exception:
        return url


def strip_tracking_params(url: str) -> str:
    """Remove known analytics/tracking query parameters from a URL."""
    try:
        from urllib.parse import urlunparse, parse_qs, urlencode
        parsed = urlparse(url)
        params = parse_qs(parsed.query)
        clean = {k: v for k, v in params.items() if k.lower() not in JUNK_PARAMS}
        new_query = urlencode(clean, doseq=True)
        return urlunparse(parsed._replace(query=new_query))
    except Exception:
        return url


# ===========================================================================
# Pattern 2 — Meta Refresh Redirect
# ===========================================================================

META_REFRESH_RE = re.compile(
    r'<meta[^>]+(?:http-equiv=["\']refresh["\'])[^>]+content=["\'][^"\']*url=([^"\'\s>]+)',
    re.I,
)
META_REFRESH_RE2 = re.compile(
    r'content=["\'][0-9]+;\s*url=(https?://[^"\'\s>]+)',
    re.I,
)


def extract_meta_refresh(html: str) -> Optional[str]:
    """Extract the redirect URL from a meta refresh tag."""
    for pattern in (META_REFRESH_RE, META_REFRESH_RE2):
        m = pattern.search(html)
        if m:
            return unquote(m.group(1).strip())
    return None


# ===========================================================================
# Pattern 3 — JavaScript window.location Redirect
# ===========================================================================

JS_LOCATION_RE = re.compile(
    r'(?:window\.location(?:\.href)?\s*=\s*|location\.replace\s*\()'
    r'["\']+(https?://[^"\']+)["\']',
    re.I,
)

JS_LOCATION_RE2 = re.compile(
    r'(?:window\.location(?:\.href)?\s*=\s*|location\.replace\s*\()'
    r'["\']+(//[^"\']+)["\']',
    re.I,
)


def extract_js_redirect(html: str, base_url: str = "") -> Optional[str]:
    """Extract redirect URL from JavaScript window.location assignment."""
    m = JS_LOCATION_RE.search(html)
    if m:
        return m.group(1)
    m = JS_LOCATION_RE2.search(html)
    if m:
        url = "https:" + m.group(1)
        return url
    return None


# ===========================================================================
# Pattern 4 — DooPlay WordPress AJAX Server Loading
# ===========================================================================
# Sites using DooPlay theme: SpaceMov, MovieWatcher, Cima4Up, and many more.
# Workflow:
#   1. Parse nonce and post ID from detail page HTML.
#   2. POST /wp-admin/admin-ajax.php with action=doo_player_ajax.
#   3. Response JSON: {"embed": "<iframe src='...'>"} or {"link": "..."}
#   4. Extract iframe src from embed HTML.

DOOPLAY_NONCE_RE = re.compile(r'"nonce"\s*:\s*"([a-f0-9]+)"')
DOOPLAY_POSTID_RE = re.compile(r'"post_id"\s*:\s*"?(\d+)"?|"postid"\s*:\s*"?(\d+)"?')
DOOPLAY_NUMID_RE  = re.compile(r'data-post=["\'](\d+)["\']')


def extract_dooplay_params(html: str) -> Tuple[Optional[str], Optional[str]]:
    """Extract nonce and post_id from a DooPlay detail page."""
    nonce = None
    post_id = None

    m = DOOPLAY_NONCE_RE.search(html)
    if m:
        nonce = m.group(1)

    m = DOOPLAY_POSTID_RE.search(html)
    if m:
        post_id = m.group(1) or m.group(2)

    if not post_id:
        m = DOOPLAY_NUMID_RE.search(html)
        if m:
            post_id = m.group(1)

    return nonce, post_id


def fetch_dooplay_servers(
    base_url: str,
    post_id: str,
    nonce: str,
    session: requests.Session,
    referer: str = "",
) -> List[Dict]:
    """
    Call the DooPlay AJAX endpoint to retrieve embed server list.
    Returns list of dicts: [{"name": str, "embed_url": str, "quality": str}]
    """
    ajax_url = base_url.rstrip("/") + "/wp-admin/admin-ajax.php"
    headers = {
        "User-Agent": DEFAULT_UA,
        "Referer": referer or base_url,
        "X-Requested-With": "XMLHttpRequest",
        "Content-Type": "application/x-www-form-urlencoded",
        "Accept": "application/json, */*",
    }
    payload = {
        "action": "doo_player_ajax",
        "post": post_id,
        "nonce": nonce,
    }

    servers = []
    try:
        resp = session.post(ajax_url, data=payload, headers=headers, timeout=12)
        data = resp.json()

        # Response schema varies; try common keys
        embed_html = data.get("embed") or data.get("html") or data.get("link") or ""
        if embed_html:
            # Extract iframe src from HTML snippet
            m = re.search(r'<iframe[^>]+src=["\']([^"\']+)["\']', embed_html, re.I)
            if m:
                servers.append({"name": "DooPlay Server 1", "embed_url": m.group(1), "quality": "HD"})

        # Some responses return an array of servers
        if "servers" in data:
            for srv in data["servers"]:
                link = srv.get("embed") or srv.get("link") or srv.get("url")
                if link:
                    servers.append({
                        "name": srv.get("name", "Server"),
                        "embed_url": link,
                        "quality": srv.get("quality", "HD"),
                    })
    except Exception:
        pass

    return servers


# ===========================================================================
# Pattern 5 — FaselHD-style AJAX Embed (data-embed ID)
# ===========================================================================

def fetch_faselhd_embed(
    base_url: str,
    embed_id: str,
    session: requests.Session,
    referer: str = "",
) -> Optional[str]:
    """
    FaselHD uses POST /ajax/embed with data-embed ID.
    Returns the actual embed URL.
    NOTE: faselhd.top is currently Cloudflare-protected; this may fail.
    """
    ajax_url = base_url.rstrip("/") + "/ajax/embed"
    headers = {
        "User-Agent": DEFAULT_UA,
        "Referer": referer or base_url,
        "X-Requested-With": "XMLHttpRequest",
    }
    try:
        resp = session.post(ajax_url, data={"id": embed_id}, headers=headers, timeout=10)
        data = resp.json()
        return data.get("link") or data.get("url") or data.get("embed")
    except Exception:
        return None


# ===========================================================================
# Pattern 6 — WordPress get_server_link AJAX
# ===========================================================================
# Used by WeCima, Akwam and similar WordPress-based sites.
# Workflow: find server li elements with data-id, POST to admin-ajax.php.

WPAJAX_ID_RE = re.compile(r'data-(?:id|post-id|server-id)=["\'](\d+)["\']')


def fetch_wp_server_link(
    base_url: str,
    server_id: str,
    nonce: str,
    session: requests.Session,
    referer: str = "",
    action: str = "get_server_link",
) -> Optional[str]:
    """
    Generic WordPress AJAX call to retrieve an embed/server link by ID.
    """
    ajax_url = base_url.rstrip("/") + "/wp-admin/admin-ajax.php"
    headers = {
        "User-Agent": DEFAULT_UA,
        "Referer": referer or base_url,
        "X-Requested-With": "XMLHttpRequest",
    }
    payload = {"action": action, "id": server_id, "nonce": nonce}
    try:
        resp = session.post(ajax_url, data=payload, headers=headers, timeout=10)
        data = resp.json()
        link = data.get("link") or data.get("url") or data.get("embed")
        if link and "<iframe" in link:
            m = re.search(r'src=["\']([^"\']+)["\']', link)
            if m:
                link = m.group(1)
        return link
    except Exception:
        return None


# ===========================================================================
# Pattern 7 — data-link / data-url / data-embed HTML Attributes
# ===========================================================================

DATA_LINK_ATTRS = ["data-link", "data-url", "data-embed", "data-src", "data-href", "data-redirect"]


def extract_data_links(soup, base_url: str = "") -> List[Dict]:
    """
    Extract all server links from data-* HTML attributes.
    Returns list of dicts: [{"name": str, "embed_url": str, "quality": str}]
    """
    results = []
    seen = set()

    for attr in DATA_LINK_ATTRS:
        for el in soup.find_all(attrs={attr: True}):
            val = el.get(attr, "")
            if not val or val in seen:
                continue
            # Skip image/asset links
            if any(val.endswith(ext) for ext in [".jpg", ".png", ".gif", ".svg", ".webp", ".css", ".js"]):
                continue
            if not val.startswith("http") and base_url:
                val = urljoin(base_url, val)
            if val.startswith("http"):
                seen.add(val)
                quality = el.get("data-quality") or el.get("data-res") or "HD"
                name = (el.get_text(strip=True) or el.get("data-name") or f"Server {len(results)+1}")[:60]
                results.append({"name": name, "embed_url": val, "quality": quality})

    return results


# ===========================================================================
# Pattern 8 — Base64-Encoded Embed/Stream URLs
# ===========================================================================

BASE64_URL_RE = re.compile(r'["\']([A-Za-z0-9+/]{30,}={0,2})["\']')


def extract_base64_urls(html: str) -> List[str]:
    """
    Find base64-encoded blobs in HTML/JS that decode to HTTP URLs.
    Returns list of decoded URLs.
    """
    results = []
    for m in BASE64_URL_RE.finditer(html):
        blob = m.group(1)
        try:
            padded = blob + "=" * (-len(blob) % 4)
            decoded = base64.b64decode(padded).decode("utf-8", errors="ignore")
            if decoded.startswith("http") and "." in decoded:
                results.append(decoded)
        except Exception:
            pass
    return results


# ===========================================================================
# Master Resolver — ties everything together
# ===========================================================================

def resolve_page(
    url: str,
    session: requests.Session,
    html: str,
    soup,
    base_url: str = "",
) -> Dict:
    """
    Given a detail/watch page URL + its HTML, run all bypass patterns and
    return a unified dict:

    {
      "servers": [{"name": str, "embed_url": str, "quality": str}],
      "redirected_to": str | None,   # if page itself was a redirect
      "dooplay": bool,               # True if DooPlay AJAX was used
    }
    """
    result = {
        "servers": [],
        "redirected_to": None,
        "dooplay": False,
    }

    # --- Check for page-level redirect first ---
    meta_url = extract_meta_refresh(html)
    js_url   = extract_js_redirect(html, base_url)
    redirect_target = meta_url or js_url

    if redirect_target and redirect_target != url:
        result["redirected_to"] = redirect_target
        # Follow and re-process
        try:
            resp = session.get(redirect_target, headers={"User-Agent": DEFAULT_UA, "Referer": url}, timeout=10)
            new_html = resp.text
            from bs4 import BeautifulSoup
            new_soup = BeautifulSoup(new_html, "lxml")
            inner = resolve_page(redirect_target, session, new_html, new_soup, base_url=base_url)
            result["servers"].extend(inner["servers"])
            result["dooplay"] = inner["dooplay"]
            return result
        except Exception:
            pass

    # --- Pattern 7: data-* attribute links (always first — fastest) ---
    data_links = extract_data_links(soup, base_url)
    for dl in data_links:
        # Resolve ad shortener if present
        dl["embed_url"] = bypass_ad_shortener(dl["embed_url"], session, referer=url)
        result["servers"].append(dl)

    # --- Pattern 4: DooPlay AJAX ---
    nonce, post_id = extract_dooplay_params(html)
    if nonce and post_id and base_url:
        doo_servers = fetch_dooplay_servers(base_url, post_id, nonce, session, referer=url)
        if doo_servers:
            result["servers"].extend(doo_servers)
            result["dooplay"] = True

    # --- Pattern 8: Base64 URLs in scripts ---
    b64_urls = extract_base64_urls(html)
    seen_b64 = {s["embed_url"] for s in result["servers"]}
    for bu in b64_urls:
        if bu not in seen_b64 and not any(bu.endswith(e) for e in [".jpg", ".png", ".gif"]):
            result["servers"].append({"name": "Base64 Server", "embed_url": bu, "quality": "HD"})
            seen_b64.add(bu)

    # --- Standard iframes (always check) ---
    seen_iframes = {s["embed_url"] for s in result["servers"]}
    for iframe in soup.find_all("iframe"):
        src = iframe.get("src") or iframe.get("data-src") or ""
        if src and src.startswith("http") and src not in seen_iframes:
            src = bypass_ad_shortener(src, session, referer=url)
            result["servers"].append({
                "name": "Iframe Server",
                "embed_url": src,
                "quality": iframe.get("data-quality", "HD"),
            })
            seen_iframes.add(src)

    return result


# ===========================================================================
# Convenience session factory
# ===========================================================================

def make_bypass_session(verify_ssl: bool = False, timeout: int = 15) -> requests.Session:
    """Create a requests.Session pre-configured for ad/redirect bypassing."""
    session = requests.Session()
    session.verify = verify_ssl
    session.headers.update({
        "User-Agent": DEFAULT_UA,
        "Accept-Language": "ar,en-US;q=0.9,en;q=0.8",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "DNT": "1",
    })
    return session
