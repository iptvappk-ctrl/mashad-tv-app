"""
Data models for the unified streaming scrapers.
"""
from dataclasses import dataclass, field, asdict
from typing import List, Optional, Dict, Any

@dataclass
class StreamServer:
    name: str
    embed_url: str
    direct_url: Optional[str] = None
    quality: Optional[str] = None
    headers: Dict[str, str] = field(default_factory=dict)
    server_type: Optional[str] = None  # e.g. "vidmoly", "voe", "uqload", "direct", "gateway"

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

@dataclass
class Episode:
    number: int
    title: str
    url: str
    season_number: int = 1
    poster: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

@dataclass
class Season:
    number: int
    title: str
    episodes: List[Episode] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "number": self.number,
            "title": self.title,
            "episodes": [ep.to_dict() for ep in self.episodes]
        }

@dataclass
class MediaItem:
    title: str
    url: str
    provider_id: str
    provider_name: str
    media_type: str  # 'movie' or 'series'
    poster: Optional[str] = None
    year: Optional[str] = None
    quality: Optional[str] = None
    rating: Optional[str] = None
    genre: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

@dataclass
class MediaDetail:
    title: str
    url: str
    provider_id: str
    provider_name: str
    media_type: str
    poster: Optional[str] = None
    year: Optional[str] = None
    story: Optional[str] = None
    genre: Optional[str] = None
    quality: Optional[str] = None
    rating: Optional[str] = None
    duration: Optional[str] = None
    cast: List[str] = field(default_factory=list)
    servers: List[StreamServer] = field(default_factory=list)
    seasons: List[Season] = field(default_factory=list)
    episodes: List[Episode] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "title": self.title,
            "url": self.url,
            "provider_id": self.provider_id,
            "provider_name": self.provider_name,
            "media_type": self.media_type,
            "poster": self.poster,
            "year": self.year,
            "story": self.story,
            "genre": self.genre,
            "quality": self.quality,
            "rating": self.rating,
            "duration": self.duration,
            "cast": self.cast,
            "servers": [s.to_dict() for s in self.servers],
            "seasons": [sn.to_dict() for sn in self.seasons],
            "episodes": [ep.to_dict() for ep in self.episodes],
        }
