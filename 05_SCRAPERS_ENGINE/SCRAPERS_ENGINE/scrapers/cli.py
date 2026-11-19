"""
Command Line Interface (CLI) for the unified scrapers.
"""
import sys
import json
import argparse
from typing import List

from .manager import ScraperManager
from .models import MediaItem

def print_table(headers: List[str], rows: List[List[str]]):
    """Simple clean ASCII table printer."""
    col_widths = [len(h) for h in headers]
    for row in rows:
        for i, val in enumerate(row):
            col_widths[i] = max(col_widths[i], len(str(val)))

    sep = "+-" + "-+-".join("-" * w for w in col_widths) + "-+"
    header_str = "| " + " | ".join(h.ljust(col_widths[i]) for i, h in enumerate(headers)) + " |"

    print(sep)
    print(header_str)
    print(sep)
    for row in rows:
        line = "| " + " | ".join(str(val).ljust(col_widths[i]) for i, val in enumerate(row)) + " |"
        print(line)
    print(sep)

def main():
    parser = argparse.ArgumentParser(description="Unified IPTV & Streaming Scraper CLI (24 Providers)")
    subparsers = parser.add_subparsers(dest="command", help="Command to execute")

    # 1. search
    p_search = subparsers.add_parser("search", help="Search movies and series across all sites")
    p_search.add_argument("query", type=str, help="Search query (Arabic or English)")
    p_search.add_argument("--providers", "-p", type=str, help="Comma-separated provider IDs (e.g. arabseed,akwam)")
    p_search.add_argument("--limit", "-l", type=int, default=30, help="Max total items")
    p_search.add_argument("--json", action="store_true", help="Output raw JSON")

    # 2. movies
    p_movies = subparsers.add_parser("movies", help="Fetch latest movies")
    p_movies.add_argument("--provider", "-p", type=str, help="Provider ID (default: all)")
    p_movies.add_argument("--page", type=int, default=1, help="Page number")
    p_movies.add_argument("--json", action="store_true", help="Output raw JSON")

    # 3. series
    p_series = subparsers.add_parser("series", help="Fetch latest series")
    p_series.add_argument("--provider", "-p", type=str, help="Provider ID (default: all)")
    p_series.add_argument("--page", type=int, default=1, help="Page number")
    p_series.add_argument("--json", action="store_true", help="Output raw JSON")

    # 4. details
    p_detail = subparsers.add_parser("details", help="Fetch media details by URL")
    p_detail.add_argument("url", type=str, help="Content page URL")
    p_detail.add_argument("--json", action="store_true", help="Output raw JSON")

    # 5. watch / extract
    p_watch = subparsers.add_parser("watch", help="Extract video servers and direct stream links")
    p_watch.add_argument("url", type=str, help="Content or watch page URL")
    p_watch.add_argument("--json", action="store_true", help="Output raw JSON")

    # 6. episodes
    p_ep = subparsers.add_parser("episodes", help="List episodes of a series")
    p_ep.add_argument("url", type=str, help="Series page URL")
    p_ep.add_argument("--json", action="store_true", help="Output raw JSON")

    # 7. providers
    p_prov = subparsers.add_parser("providers", help="List all 24 registered providers")
    p_prov.add_argument("--json", action="store_true", help="Output raw JSON")

    # 8. test-all
    p_test = subparsers.add_parser("test-all", help="Smoke test all 24 providers")
    p_test.add_argument("--query", "-q", type=str, default="2024", help="Test search keyword")
    p_test.add_argument("--json", action="store_true", help="Output raw JSON")

    # 9. serve
    p_serve = subparsers.add_parser("serve", help="Run FastAPI local REST API")
    p_serve.add_argument("--port", type=int, default=8000, help="Port to run on")
    p_serve.add_argument("--host", type=str, default="127.0.0.1", help="Host to bind")

    args = parser.parse_args()
    if not args.command:
        parser.print_help()
        sys.exit(0)

    manager = ScraperManager()

    if args.command == "providers":
        provs = manager.list_providers()
        if args.json:
            print(json.dumps(provs, ensure_ascii=False, indent=2))
        else:
            rows = [[p["id"], p["name"], p["base_url"]] for p in provs]
            print_table(["ID", "Name", "Base URL"], rows)

    elif args.command == "search":
        p_ids = [p.strip() for p in args.providers.split(",")] if args.providers else None
        print(f"[*] Searching for '{args.query}' across {len(p_ids) if p_ids else len(manager.providers)} providers...")
        items = manager.search_all(args.query, provider_ids=p_ids, max_total=args.limit)
        if args.json:
            print(json.dumps([item.to_dict() for item in items], ensure_ascii=False, indent=2))
        else:
            print(f"[+] Found {len(items)} results:\n")
            rows = [[str(i+1), it.provider_name, it.media_type, it.year or "-", it.title[:45], it.url[:50]] for i, it in enumerate(items)]
            print_table(["#", "Provider", "Type", "Year", "Title", "URL"], rows)

    elif args.command == "movies":
        items = manager.get_movies(provider_id=args.provider, page=args.page)
        if args.json:
            print(json.dumps([item.to_dict() for item in items], ensure_ascii=False, indent=2))
        else:
            print(f"[+] Found {len(items)} movies:\n")
            rows = [[str(i+1), it.provider_name, it.year or "-", it.quality or "-", it.title[:45]] for i, it in enumerate(items)]
            print_table(["#", "Provider", "Year", "Quality", "Title"], rows)

    elif args.command == "series":
        items = manager.get_series(provider_id=args.provider, page=args.page)
        if args.json:
            print(json.dumps([item.to_dict() for item in items], ensure_ascii=False, indent=2))
        else:
            print(f"[+] Found {len(items)} series:\n")
            rows = [[str(i+1), it.provider_name, it.year or "-", it.title[:50]] for i, it in enumerate(items)]
            print_table(["#", "Provider", "Year", "Title"], rows)

    elif args.command == "details":
        print(f"[*] Fetching details for: {args.url}")
        det = manager.get_details(args.url)
        if not det:
            print("[-] Could not fetch details.")
            return
        if args.json:
            print(json.dumps(det.to_dict(), ensure_ascii=False, indent=2))
        else:
            print(f"\n==================== {det.title} ====================")
            print(f"Provider: {det.provider_name} ({det.provider_id})")
            print(f"Type:     {det.media_type.upper()} | Year: {det.year or 'N/A'} | Quality: {det.quality or 'N/A'}")
            print(f"Poster:   {det.poster or 'N/A'}")
            if det.story:
                print(f"\nStory:\n{det.story}\n")
            if det.cast:
                print(f"Cast: {', '.join(det.cast)}")
            if det.servers:
                print(f"\nServers ({len(det.servers)}):")
                for i, s in enumerate(det.servers):
                    print(f"  [{i+1}] {s.name} | Quality: {s.quality or 'N/A'}")
                    if s.direct_url:
                        print(f"      Direct Stream: {s.direct_url}")
                    else:
                        print(f"      Embed: {s.embed_url}")

    elif args.command == "watch":
        print(f"[*] Extracting servers for: {args.url}")
        servers = manager.extract_servers(args.url)
        if args.json:
            print(json.dumps([s.to_dict() for s in servers], ensure_ascii=False, indent=2))
        else:
            print(f"[+] Found {len(servers)} servers:\n")
            for i, s in enumerate(servers):
                print(f"[{i+1}] {s.name} ({s.quality or 'Auto'})")
                print(f"    Embed URL:  {s.embed_url}")
                if s.direct_url:
                    print(f"    DIRECT URL: {s.direct_url}")
                print()

    elif args.command == "episodes":
        print(f"[*] Extracting episodes for: {args.url}")
        eps = manager.get_episodes(args.url)
        if args.json:
            print(json.dumps([e.to_dict() for e in eps], ensure_ascii=False, indent=2))
        else:
            print(f"[+] Found {len(eps)} episodes:\n")
            rows = [[str(e.number), e.title[:45], e.url[:60]] for e in eps]
            print_table(["Ep #", "Title", "URL"], rows)

    elif args.command == "test-all":
        print(f"[*] Running live smoke test across all {len(manager.providers)} providers with query: '{args.query}'...\n")
        reports = manager.test_all_providers(query=args.query)
        if args.json:
            print(json.dumps(reports, ensure_ascii=False, indent=2))
        else:
            rows = []
            success_count = 0
            for r in reports:
                status_str = "OK" if r.get("success") else "FAILED"
                if r.get("success"):
                    success_count += 1
                items_str = str(r.get("items_found", 0))
                lat_str = f"{r.get('latency_sec', 0)}s"
                rows.append([r.get("id"), r.get("name", ""), status_str, items_str, lat_str, r.get("base_url")])
            print_table(["ID", "Name", "Status", "Items", "Latency", "Base URL"], rows)
            print(f"\n[+] Total Active Providers: {success_count}/{len(reports)}")

    elif args.command == "serve":
        import uvicorn
        from .api import app
        print(f"[*] Launching Scraper REST API on http://{args.host}:{args.port} ...")
        uvicorn.run(app, host=args.host, port=args.port)

if __name__ == "__main__":
    main()
