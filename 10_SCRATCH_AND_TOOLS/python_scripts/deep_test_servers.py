#!/usr/bin/env python3
"""Deep test: what does the watch page return for data-link elements?"""
import urllib.request
import urllib.parse
import re
import base64

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.6668.100 Mobile Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "ar,en;q=0.9",
    "Referer": "https://m.myseed.tv/",
    "Connection": "keep-alive",
}

def fetch(url, referer=None):
    if referer:
        HEADERS["Referer"] = referer
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=20) as resp:
        data = resp.read().decode("utf-8", errors="replace")
        return data, resp.geturl()

# The episode URL (حب اسود eps20)
ep_url = "https://m.myseed.tv/%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%ad%d8%a8-%d8%a3%d8%b3%d9%88%d8%af-s1-eps20/"
watch_url = ep_url.rstrip('/') + "/watch/"

print(f"Step 1: Visiting episode page: {urllib.parse.unquote(ep_url)}")
try:
    html1, url1 = fetch(ep_url, referer="https://m.myseed.tv/")
    print(f"  OK, len={len(html1)}, final={url1}")
except Exception as e:
    print(f"  ERROR: {e}")

print(f"\nStep 2: Visiting watch page with episode referer: {urllib.parse.unquote(watch_url)}")
try:
    HEADERS["Referer"] = ep_url
    html2, url2 = fetch(watch_url)
    print(f"  OK, len={len(html2)}, final={url2}")
    
    # Find data-link
    data_links = re.findall(r'data-link=["\']([^"\']+)["\']', html2)
    server_names = re.findall(r'data-server-name=["\']([^"\']+)["\']', html2)
    
    print(f"\n  data-link elements: {len(data_links)}")
    for i, (link, name) in enumerate(zip(data_links, server_names + ['?']*10)):
        decoded_link = link
        if 'id=' in link and 'http' in link:
            try:
                b64 = link.split('id=')[-1].split('&')[0]
                decoded_link = base64.b64decode(b64 + '==').decode('utf-8', errors='replace').strip()
            except:
                pass
        print(f"  [{i}] name={name[:30]} link={decoded_link[:80]}")
    
    if not data_links:
        # Check what's actually in the HTML
        print("\n  NO data-link found! Checking page...")
        if "cloudflare" in html2.lower() or "cf-browser-verification" in html2.lower():
            print("  !! CLOUDFLARE CHALLENGE detected!")
        if "403" in html2[:200] or "404" in html2[:200]:
            print("  !! 403/404 error!")
        # Show first 1000 chars of body
        body_start = html2.find("<body")
        if body_start > 0:
            print(f"\n  Body preview:")
            body = html2[body_start:body_start+1000]
            body_clean = re.sub(r'\s+', ' ', body)
            print(f"  {body_clean[:500]}")
        
except Exception as e:
    print(f"  ERROR: {e}")
    import traceback; traceback.print_exc()
