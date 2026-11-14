#!/usr/bin/env python3
"""
Root entry point to run the unified scrapers CLI or API.
Usage:
    python run_scraper.py search "Inception"
    python run_scraper.py movies --provider akwam
    python run_scraper.py series --provider arabseed
    python run_scraper.py watch "<url>"
    python run_scraper.py test-all
    python run_scraper.py serve --port 8000
"""
from scrapers.cli import main

if __name__ == "__main__":
    main()
