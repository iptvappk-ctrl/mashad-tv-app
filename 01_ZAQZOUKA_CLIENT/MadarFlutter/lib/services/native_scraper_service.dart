import 'package:flutter/services.dart';

/// Typed Dart adapter for the catalog and server-candidate methods already
/// exposed by FlutterMediaBridge through `madar/native`.
///
/// This service returns catalog records and server page candidates only. A
/// server page is never treated as a playable video URL; playback resolution
/// and verification remain in the existing native playback pipeline.
class NativeScraperService {
  const NativeScraperService({
    this._channel = const MethodChannel('madar/native'),
  });

  final MethodChannel _channel;

  Future<List<Map<String, dynamic>>> searchAllProviders(
    String query, {
    bool forceRefresh = false,
  }) => _list('catalog.search.providers', {
    'query': query.trim(),
    'force': forceRefresh,
  }, timeout: const Duration(seconds: 12));

  Future<List<Map<String, dynamic>>> fetchProviderCatalog({
    required String path,
    required String tab,
    required String categoryId,
    required int page,
    bool forceRefresh = false,
  }) => _list('catalog.providers.fetch', {
    'path': path,
    'tab': tab,
    'categoryId': categoryId,
    'page': page,
    'force': forceRefresh,
  }, timeout: const Duration(seconds: 18));

  Future<List<Map<String, dynamic>>> fetchServerCandidates({
    required String pageUrl,
    required String provider,
  }) => _list('media.servers', {
    'url': pageUrl,
    'provider': provider,
  }, timeout: const Duration(seconds: 12));

  Future<List<Map<String, dynamic>>> findServerCandidatesAcrossProviders({
    required String title,
    required String pageUrl,
    required bool isSeries,
    String year = '',
  }) => _list('media.servers.all', {
    'title': title,
    'url': pageUrl,
    'type': isSeries ? 'series' : 'movie',
    'year': year,
  }, timeout: const Duration(seconds: 18));

  Future<List<Map<String, dynamic>>> _list(
    String method,
    Map<String, Object?> arguments, {
    required Duration timeout,
  }) async {
    final rows = await _channel
        .invokeListMethod<dynamic>(method, arguments)
        .timeout(timeout);
    return (rows ?? const <dynamic>[])
        .whereType<Map>()
        .map((row) => Map<String, dynamic>.from(row))
        .toList(growable: false);
  }
}
