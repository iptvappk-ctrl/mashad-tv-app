"""
FastAPI REST API for the unified scrapers.
Allows Flutter, Web Dashboard, and external clients to consume scrapers seamlessly.
"""
from typing import Optional, List
from fastapi import FastAPI, Query, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from .manager import ScraperManager

app = FastAPI(
    title="MadarTV Unified Scraper API",
    description="High-performance scraping and video stream extraction API for 24 platforms.",
    version="2.0.0"
)

# Enable CORS for Flutter & Next.js web frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

manager = ScraperManager()

@app.get("/")
def root():
    return {
        "status": "online",
        "service": "MadarTV Unified Scraper API",
        "providers_count": len(manager.providers),
        "docs": "/docs"
    }

@app.get("/api/providers")
def get_providers():
    return manager.list_providers()

@app.get("/api/search")
def search_content(
    q: str = Query(..., description="Search keyword"),
    providers: Optional[str] = Query(None, description="Comma-separated provider IDs"),
    limit: int = Query(30, description="Max results")
):
    provider_ids = [p.strip() for p in providers.split(",")] if providers else None
    results = manager.search_all(query=q, provider_ids=provider_ids, max_total=limit)
    return [it.to_dict() for it in results]

@app.get("/api/movies")
def get_movies(
    provider: Optional[str] = Query(None, description="Provider ID (optional)"),
    page: int = Query(1, description="Page number")
):
    items = manager.get_movies(provider_id=provider, page=page)
    return [it.to_dict() for it in items]

@app.get("/api/series")
def get_series(
    provider: Optional[str] = Query(None, description="Provider ID (optional)"),
    page: int = Query(1, description="Page number")
):
    items = manager.get_series(provider_id=provider, page=page)
    return [it.to_dict() for it in items]

@app.get("/api/details")
def get_details(url: str = Query(..., description="Content page URL")):
    det = manager.get_details(url)
    if not det:
        raise HTTPException(status_code=404, detail="Content not found or could not be parsed.")
    return det.to_dict()

@app.get("/api/servers")
def get_servers(url: str = Query(..., description="Content or watch page URL")):
    servers = manager.extract_servers(url)
    return [s.to_dict() for s in servers]

@app.get("/api/episodes")
def get_episodes(url: str = Query(..., description="Series page URL")):
    episodes = manager.get_episodes(url)
    return [ep.to_dict() for ep in episodes]

@app.get("/api/health")
def health_check(query: str = Query("2024", description="Test query")):
    return manager.test_all_providers(query=query)
