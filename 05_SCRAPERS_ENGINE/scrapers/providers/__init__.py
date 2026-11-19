"""
Providers package: exports all 24 scraping providers.
"""
from typing import Dict, Type
from ..base import BaseScraper

from .arabseed import ArabseedScraper
from .akwam import AkwamScraper
from .wecima import WeCimaScraper
from .faselhd import FaselHdScraper
from .egybest import EgyBestScraper
from .shahid4u import Shahid4UScraper
from .cima4u import Cima4UScraper
from .fushaar import FushaarScraper
from .topcinema import TopCinemaScraper
from .arablionz import ArabLionzScraper
from .lodynet import LodynetScraper
from .esheeq import EsheeqScraper
from .movizland import MovizlandScraper
from .prestige import PrestigeScraper
from .cimaclub import CimaClubScraper
from .egydead import EgyDeadScraper
from .cimanow import CimaNowScraper
from .cimawbas import CimaWbasScraper
from .cima4up import Cima4UpScraper
from .cinemaforall import CinemaForAllScraper
from .movies4u import Movies4UScraper
from .moviewatcher import MovieWatcherScraper
from .spacemov import SpaceMovScraper
from .moviflex import MoviflexScraper

ALL_PROVIDERS: Dict[str, Type[BaseScraper]] = {
    "arabseed": ArabseedScraper,
    "akwam": AkwamScraper,
    "wecima": WeCimaScraper,
    "faselhd": FaselHdScraper,
    "egybest": EgyBestScraper,
    "shahid4u": Shahid4UScraper,
    "cima4u": Cima4UScraper,
    "fushaar": FushaarScraper,
    "topcinema": TopCinemaScraper,
    "arablionz": ArabLionzScraper,
    "lodynet": LodynetScraper,
    "esheeq": EsheeqScraper,
    "movizland": MovizlandScraper,
    "prestige": PrestigeScraper,
    "cimaclub": CimaClubScraper,
    "egydead": EgyDeadScraper,
    "cimanow": CimaNowScraper,
    "cimawbas": CimaWbasScraper,
    "cima4up": Cima4UpScraper,
    "cinemaforall": CinemaForAllScraper,
    "movies4u": Movies4UScraper,
    "moviewatcher": MovieWatcherScraper,
    "spacemov": SpaceMovScraper,
    "moviflex": MoviflexScraper,
}

__all__ = [
    "ALL_PROVIDERS",
    "ArabseedScraper",
    "AkwamScraper",
    "WeCimaScraper",
    "FaselHdScraper",
    "EgyBestScraper",
    "Shahid4UScraper",
    "Cima4UScraper",
    "FushaarScraper",
    "TopCinemaScraper",
    "ArabLionzScraper",
    "LodynetScraper",
    "EsheeqScraper",
    "MovizlandScraper",
    "PrestigeScraper",
    "CimaClubScraper",
    "EgyDeadScraper",
    "CimanowScraper",
    "CimaWbasScraper",
    "Cima4UpScraper",
    "CinemaForAllScraper",
    "Movies4UScraper",
    "MovieWatcherScraper",
    "SpaceMovScraper",
    "MoviflexScraper",
]
