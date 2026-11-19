export interface UnifiedMediaItem {
  id: string;
  title: string;
  poster: string;
  pageUrl: string;
  category: string;
  quality?: string;
  year?: string;
  rating?: string;
  isSeries: boolean;
  providerId: string;
  providerName: string;
}

export interface UnifiedServer {
  name: string;
  embedUrl: string;
  providerId?: string;
  quality?: string;
  directStream?: string;
  isHls?: boolean;
}

export interface UnifiedEpisode {
  title: string;
  pageUrl: string;
  episodeNumber?: string;
}

export interface UnifiedSeason {
  title: string;
  pageUrl: string;
  seasonNumber?: string;
}

export interface UnifiedMediaDetail {
  item: UnifiedMediaItem;
  overview: string;
  cast?: string[];
  servers: UnifiedServer[];
  seasons: UnifiedSeason[];
  episodes: UnifiedEpisode[];
  directStream?: string;
  proxiedStream?: string;
}

export interface BaseProvider {
  id: string;
  name: string;
  baseUrl: string;
  isEnabled: boolean;
  search(query: string): Promise<UnifiedMediaItem[]>;
  getCategory(slug: string, page?: number): Promise<UnifiedMediaItem[]>;
  getDetail(pageUrl: string): Promise<UnifiedMediaDetail | null>;
  resolveStream(serverUrl: string): Promise<string | null>;
}
