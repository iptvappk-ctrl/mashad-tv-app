import 'package:flutter_test/flutter_test.dart';
import 'package:madar_flutter/main.dart';

MediaItem _item({
  required String title,
  required String pageUrl,
  String parentSeriesPageUrl = '',
}) => MediaItem(
  id: pageUrl,
  title: title,
  pageUrl: pageUrl,
  isSeries: true,
  parentSeriesPageUrl: parentSeriesPageUrl,
);

void main() {
  test('marks an unlinked MySeed episode page as a partial listing', () {
    final item = _item(
      title: 'Doctor X: Mafia in White الموسم الأول الحلقة 1',
      pageUrl: 'https://m.myseed.tv/doctor-x-mafia-in-white-s1-eps1/',
    );

    expect(isUnlinkedEpisodeResult(item), isTrue);
  });

  test('marks an unlinked episode route as a partial listing', () {
    final item = _item(
      title: 'Doctor X: Mafia in White',
      pageUrl: 'https://provider.test/episodes/doctor-x-episode-1',
    );

    expect(isUnlinkedEpisodeResult(item), isTrue);
  });

  test('keeps a canonical series page eligible for a total count', () {
    final item = _item(
      title: 'Doctor X: Mafia in White',
      pageUrl: 'https://provider.test/series/doctor-x-mafia-in-white',
    );

    expect(isUnlinkedEpisodeResult(item), isFalse);
  });

  test('a linked episode with a canonical parent is not marked partial', () {
    final item = _item(
      title: 'Doctor X: Mafia in White الموسم الأول الحلقة 1',
      pageUrl: 'https://provider.test/episode/doctor-x-1',
      parentSeriesPageUrl: 'https://provider.test/series/doctor-x',
    );

    expect(isUnlinkedEpisodeResult(item), isFalse);
  });

  test(
    'does not present a URL-only season token as a confirmed anime season',
    () {
      final item = _item(
        title: 'مسلسل انمي One Piece الحلقة 1178 مترجمة',
        pageUrl: 'https://m.myseed.tv/one-piece-s1-eps1178/',
      );

      expect(isSeasonGroupingUnverified(item), isTrue);
      expect(
        episodeCollectionLabel(
          item: item,
          seasonNumber: 1,
          sourceTitle: 'الموسم الأول',
        ),
        'التجميعة 1 من المصدر',
      );
    },
  );

  test('keeps a season label confirmed by the direct episode title', () {
    final item = _item(
      title: 'Doctor X: Mafia in White الموسم الأول الحلقة 1',
      pageUrl: 'https://m.myseed.tv/doctor-x-mafia-in-white-s1-eps1/',
    );

    expect(isSeasonGroupingUnverified(item), isFalse);
    expect(
      episodeCollectionLabel(
        item: item,
        seasonNumber: 1,
        sourceTitle: 'الموسم الأول',
      ),
      'الموسم الأول',
    );
  });

  test('does not infer an episode number into a different selected season', () {
    expect(
      canInferEpisodeForSelectedSeason(
        episodeSeasonNumber: 4,
        selectedSeasonNumber: 3,
        availableSeasonCount: 3,
      ),
      isFalse,
    );
  });

  test('allows inference when the current and selected seasons match', () {
    expect(
      canInferEpisodeForSelectedSeason(
        episodeSeasonNumber: 4,
        selectedSeasonNumber: 4,
        availableSeasonCount: 3,
      ),
      isTrue,
    );
  });

  test('only infers an unknown season when there is one season', () {
    expect(
      canInferEpisodeForSelectedSeason(
        episodeSeasonNumber: null,
        selectedSeasonNumber: 1,
        availableSeasonCount: 1,
      ),
      isTrue,
    );
    expect(
      canInferEpisodeForSelectedSeason(
        episodeSeasonNumber: null,
        selectedSeasonNumber: 3,
        availableSeasonCount: 3,
      ),
      isFalse,
    );
  });
}
