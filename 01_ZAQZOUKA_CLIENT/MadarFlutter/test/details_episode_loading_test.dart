import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:madar_flutter/main.dart';

const _channel = MethodChannel('madar/native');

Map<String, dynamic> _episode(int number, {int season = 1}) => {
  'number': number,
  'title': 'الحلقة $number',
  'pageUrl': 'https://media.example/series/one-piece-s$season-episode-$number',
  'seasonNumber': season,
  'providerName': 'اختبار',
};

void _expectNoScaryErrorUi() {
  expect(find.text('إعادة فحص الحلقات'), findsNothing);
  expect(find.text('قائمة الحلقات غير مكتملة'), findsNothing);
  expect(
    find.text('قائمة الحلقات غير مكتملة؛ نخفيها حتى لا يظهر ترتيب خاطئ.'),
    findsNothing,
  );
  expect(find.text('الحلقات المدرجة من صفحة المصدر'), findsNothing);
  expect(find.textContaining('مصادر لم تكتمل استجابتها'), findsNothing);
  expect(find.textContaining('تسلسل الحلقات ما زال ناقصًا'), findsNothing);
  expect(find.text('المصدر لم يرجع حلقات لهذا الجزء.'), findsNothing);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('opens details and renders cached episodes immediately without blocking', (
    tester,
  ) async {
    final completeBundle = <String, dynamic>{
      'seasons': [
        {
          'number': 1,
          'title': 'الموسم الأول',
          'pageUrl': 'https://media.example/series/one-piece-season-1',
          'episodes': List.generate(65, (index) => _episode(index + 1)),
        },
      ],
      'relatedParts': <dynamic>[],
    };

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, (call) async {
          switch (call.method) {
            case 'media.detail.cached':
              return null;
            case 'media.seasons.cached':
              return completeBundle;
            case 'media.story.fetch':
              return {'overview': '', 'cast': <dynamic>[]};
            case 'media.seasons.fetch':
              return {
                ...completeBundle,
                'backgroundRefreshing': false,
                'providerSweepComplete': true,
                'pendingProviderIds': <String>[],
                'episodeGaps': <String>[],
              };
            case 'media.seasons.status':
              return {
                'backgroundRefreshing': false,
                'providerSweepComplete': true,
                'pendingProviderIds': <String>[],
                'episodeGaps': <String>[],
              };
            case 'media.servers':
            case 'cache.read':
            case 'cache.write':
              return null;
            case 'media.season.episodes.more':
              return {
                'seasons': [
                  {
                    'number': 1,
                    'title': 'الموسم الأول',
                    'pageUrl':
                        'https://media.example/series/one-piece-season-1',
                    'episodes': List.generate(
                      5,
                      (index) => _episode(index + 66),
                    ),
                  },
                ],
                'relatedParts': <dynamic>[],
                'nextPageUrl': '',
              };
            default:
              return null;
          }
        });

    await tester.pumpWidget(
      MaterialApp(
        home: DetailPage(
          item: const MediaItem(
            id: 'one-piece',
            title: 'One Piece',
            pageUrl: 'https://media.example/series/one-piece',
            isSeries: true,
          ),
          onOpenItem: (_) {},
        ),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));

    // Cached episodes render immediately — no skeleton blocking.
    expect(find.textContaining('One Piece'), findsWidgets);
    expect(find.text('الحلقة 1'), findsOneWidget);
    expect(find.text('الحلقة 60'), findsOneWidget);
    expect(find.text('الحلقة 61'), findsNothing);
    _expectNoScaryErrorUi();

    await tester.pumpAndSettle();

    final moreButton = find.textContaining('عرض 5');
    for (var attempt = 0; attempt < 8; attempt++) {
      final rect = tester.getRect(moreButton);
      if (rect.top >= 0 && rect.bottom <= 600) break;
      await tester.dragFrom(const Offset(400, 520), const Offset(0, -420));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 350));
    }
    await tester.tap(moreButton);
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 350));

    expect(find.text('الحلقة 70'), findsOneWidget);
    expect(find.text('عرض 5 حلقة وجلب التالية'), findsNothing);
    _expectNoScaryErrorUi();

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, null);
  });

  testWidgets('renders available episodes even when provider sweep is partial or slow', (
    tester,
  ) async {
    final partialBundle = <String, dynamic>{
      'seasons': [
        {
          'number': 1,
          'title': 'الموسم الأول',
          'pageUrl': 'https://media.example/series/one-piece-season-1',
          'episodes': [
            for (final number in [881, 882, 890, 891, 1166, 1177])
              _episode(number),
          ],
        },
      ],
      'relatedParts': <dynamic>[],
    };

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, (call) async {
          switch (call.method) {
            case 'media.detail.cached':
            case 'media.seasons.cached':
            case 'cache.read':
              return null;
            case 'media.story.fetch':
              return {'overview': '', 'cast': <dynamic>[]};
            case 'media.seasons.fetch':
              return {
                ...partialBundle,
                'backgroundRefreshing': false,
                'providerSweepComplete': false,
                'pendingProviderIds': <String>['prestige'],
                'episodeGaps': ['S1:892-1165'],
              };
            case 'media.servers':
              return <dynamic>[];
            default:
              return null;
          }
        });

    await tester.pumpWidget(
      MaterialApp(
        home: DetailPage(
          item: const MediaItem(
            id: 'one-piece',
            title: 'One Piece',
            pageUrl: 'https://media.example/series/one-piece',
            isSeries: true,
          ),
          onOpenItem: (_) {},
        ),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 350));

    // Never show broken diagnostic UI; always show available episodes cleanly.
    _expectNoScaryErrorUi();
    expect(find.text('الحلقة 881'), findsOneWidget);
    expect(find.text('الحلقة 1177'), findsOneWidget);

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, null);
  });

  testWidgets(
    'never shows unlinked episode disclaimers on direct episode cards',
    (tester) async {
      final staleBundle = <String, dynamic>{
        'seasons': [
          {
            'number': 1,
            'title': 'الموسم الأول',
            'pageUrl': 'https://media.example/series/one-piece-season-1',
            'episodes': List.generate(8, (index) => _episode(index + 1)),
          },
        ],
        'relatedParts': <dynamic>[],
      };

      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(_channel, (call) async {
            switch (call.method) {
              case 'media.detail.cached':
                return null;
              case 'media.seasons.cached':
                return staleBundle;
              case 'media.story.fetch':
                return {'overview': '', 'cast': <dynamic>[]};
              case 'media.seasons.fetch':
                return {
                  ...staleBundle,
                  'backgroundRefreshing': false,
                  'providerSweepComplete': true,
                  'pendingProviderIds': <String>[],
                  'episodeGaps': <String>[],
                };
              case 'media.servers':
                return <dynamic>[];
              case 'cache.read':
              case 'cache.write':
                return null;
              default:
                return null;
            }
          });

      await tester.pumpWidget(
        MaterialApp(
          home: DetailPage(
            item: const MediaItem(
              id: 'one-piece-1178',
              title: 'One Piece الحلقة 1178',
              pageUrl: 'https://media.example/series/one-piece-s1-eps1178',
              isSeries: false,
            ),
            onOpenItem: (_) {},
          ),
        ),
      );
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 350));

      _expectNoScaryErrorUi();
      expect(find.text('الحلقة 1'), findsOneWidget);
      expect(find.text('الحلقة 8'), findsOneWidget);

      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(_channel, null);
    },
  );

  testWidgets('keeps cached episodes visible and merges background updates seamlessly', (
    tester,
  ) async {
    var backgroundRefreshing = true;
    final initialBundle = <String, dynamic>{
      'seasons': [
        {
          'number': 1,
          'title': 'الموسم الأول',
          'pageUrl': 'https://media.example/series/one-piece-season-1',
          'episodes': [
            for (final number in [881, 882, 1166]) _episode(number),
          ],
        },
      ],
      'relatedParts': <dynamic>[],
    };
    final enrichedBundle = <String, dynamic>{
      'seasons': [
        {
          'number': 1,
          'title': 'الموسم الأول',
          'pageUrl': 'https://media.example/series/one-piece-season-1',
          'episodes': [
            for (final number in [881, 882, 891, 1166]) _episode(number),
          ],
        },
      ],
      'relatedParts': <dynamic>[],
    };

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, (call) async {
          switch (call.method) {
            case 'media.detail.cached':
              return null;
            case 'media.seasons.cached':
              return backgroundRefreshing ? initialBundle : enrichedBundle;
            case 'media.story.fetch':
              return {'overview': '', 'cast': <dynamic>[]};
            case 'media.seasons.fetch':
              return {
                ...initialBundle,
                'backgroundRefreshing': true,
                'providerSweepComplete': false,
                'pendingProviderIds': ['prestige'],
                'episodeGaps': <String>[],
              };
            case 'media.seasons.status':
              return {
                'backgroundRefreshing': backgroundRefreshing,
                'providerSweepComplete': false,
                'pendingProviderIds': ['prestige'],
                'episodeGaps': <String>[],
              };
            case 'media.servers':
            case 'cache.read':
              return <dynamic>[];
            case 'cache.write':
              return null;
            default:
              return null;
          }
        });

    await tester.pumpWidget(
      MaterialApp(
        home: DetailPage(
          item: const MediaItem(
            id: 'one-piece',
            title: 'One Piece',
            pageUrl: 'https://media.example/series/one-piece',
            isSeries: true,
          ),
          onOpenItem: (_) {},
        ),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));

    // Cached episodes are immediately visible while background sync runs.
    expect(find.text('الحلقة 881'), findsOneWidget);
    expect(find.text('الحلقة 1166'), findsOneWidget);
    expect(find.text('الحلقة 891'), findsNothing);
    _expectNoScaryErrorUi();

    backgroundRefreshing = false;
    await tester.pump(const Duration(milliseconds: 450));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 350));

    // New episode from background enrichment appears cleanly; no error UI.
    expect(find.text('الحلقة 891'), findsOneWidget);
    _expectNoScaryErrorUi();

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, null);
  });

  testWidgets('prefers first non-empty season and never shows broken empty-season warnings', (
    tester,
  ) async {
    final mixedEmptySeasonBundle = <String, dynamic>{
      'seasons': [
        {
          'number': 1,
          'title': 'الموسم الأول',
          'pageUrl': 'https://media.example/selary/s1',
          'episodes': <dynamic>[],
        },
        {
          'number': 2,
          'title': 'الموسم الثاني',
          'pageUrl': 'https://media.example/selary/s2',
          'episodes': List.generate(3, (i) => _episode(i + 1, season: 2)),
        },
      ],
      'relatedParts': <dynamic>[],
    };

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, (call) async {
          switch (call.method) {
            case 'media.detail.cached':
            case 'cache.read':
              return null;
            case 'media.seasons.cached':
              return mixedEmptySeasonBundle;
            case 'media.story.fetch':
              return {'overview': '', 'cast': <dynamic>[]};
            case 'media.seasons.fetch':
            case 'media.seasons.status':
              return {
                ...mixedEmptySeasonBundle,
                'backgroundRefreshing': false,
                'providerSweepComplete': true,
                'pendingProviderIds': <String>[],
                'episodeGaps': <String>[],
              };
            case 'media.servers':
              return <dynamic>[];
            default:
              return null;
          }
        });

    await tester.pumpWidget(
      MaterialApp(
        home: DetailPage(
          item: const MediaItem(
            id: 'mehmed-broken',
            title: 'محمد الفاتح',
            pageUrl: 'https://m.myseed.tv/series/mehmed',
            isSeries: true,
          ),
          onOpenItem: (_) {},
        ),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 350));

    _expectNoScaryErrorUi();
    // Automatically picks Season 2 since Season 1 has no episodes yet.
    expect(find.text('الموسم الثاني'), findsOneWidget);
    expect(find.text('الحلقة 1'), findsOneWidget);

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, null);
  });

  testWidgets('terminates empty series fetch calmly without infinite skeleton or errors', (
    tester,
  ) async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, (call) async {
          switch (call.method) {
            case 'media.detail.cached':
            case 'media.seasons.cached':
            case 'cache.read':
              return null;
            case 'media.story.fetch':
              return {'overview': '', 'cast': <dynamic>[]};
            case 'media.seasons.fetch':
              return {
                'seasons': <dynamic>[],
                'relatedParts': <dynamic>[],
                'backgroundRefreshing': false,
                'providerSweepComplete': false,
                'pendingProviderIds': ['prestige'],
                'episodeGaps': <String>[],
              };
            case 'media.servers':
              return <dynamic>[];
            default:
              return null;
          }
        });

    await tester.pumpWidget(
      MaterialApp(
        home: DetailPage(
          item: const MediaItem(
            id: 'empty-show',
            title: 'مسلسل غير متوفر',
            pageUrl: 'https://media.example/series/empty-show',
            isSeries: true,
          ),
          onOpenItem: (_) {},
        ),
      ),
    );
    await tester.pumpAndSettle();

    _expectNoScaryErrorUi();
    expect(find.text('لم يتم العثور على حلقات حالياً.'), findsOneWidget);

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, null);
  });

  testWidgets(
    'keeps loading skeleton instead of flashing 2 partial episodes until full episodes arrive',
    (tester) async {
      final storyCompleter = Completer<Map<String, dynamic>>();
      var statusCalls = 0;
      final twoEpisodePartialBundle = <String, dynamic>{
        'seasons': [
          {
            'number': 1,
            'title': 'الموسم الأول',
            'pageUrl': 'https://media.example/series/doctor-x',
            'episodes': [_episode(1), _episode(2)],
          },
        ],
        'relatedParts': <dynamic>[],
      };
      final fullBundle = <String, dynamic>{
        'seasons': [
          {
            'number': 1,
            'title': 'الموسم الأول',
            'pageUrl': 'https://media.example/series/doctor-x',
            'episodes': List.generate(10, (index) => _episode(index + 1)),
          },
        ],
        'relatedParts': <dynamic>[],
      };

      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(_channel, (call) async {
            switch (call.method) {
              case 'media.detail.cached':
              case 'media.seasons.cached':
              case 'cache.read':
              case 'cache.write':
                return null;
              case 'media.story.fetch':
                return storyCompleter.future;
              case 'media.seasons.fetch':
                return {
                  ...twoEpisodePartialBundle,
                  'backgroundRefreshing': true,
                  'providerSweepComplete': false,
                  'pendingProviderIds': ['arabseed'],
                  'episodeGaps': <String>[],
                };
              case 'media.seasons.status':
                statusCalls++;
                if (statusCalls == 1) {
                  return {
                    ...twoEpisodePartialBundle,
                    'backgroundRefreshing': true,
                    'providerSweepComplete': false,
                    'pendingProviderIds': ['arabseed'],
                    'episodeGaps': <String>[],
                  };
                }
                return {
                  ...fullBundle,
                  'backgroundRefreshing': false,
                  'providerSweepComplete': true,
                  'pendingProviderIds': <String>[],
                  'episodeGaps': <String>[],
                };
              case 'media.servers':
                return <dynamic>[];
              default:
                return null;
            }
          });

      await tester.pumpWidget(
        MaterialApp(
          home: DetailPage(
            item: const MediaItem(
              id: 'doctor-x',
              title: 'Doctor X',
              pageUrl: 'https://media.example/series/doctor-x',
              isSeries: true,
            ),
            onOpenItem: (_) {},
          ),
        ),
      );

      // Initial render + first background status tick: must keep skeleton
      // visible and NOT flash the partial 2-episode list.
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));
      expect(find.textContaining('القصة كاملة'), findsOneWidget);
      expect(find.text('جاري تحميل الحلقات…'), findsOneWidget);
      expect(find.text('الحلقة 1'), findsNothing);
      expect(find.text('الحلقة 2'), findsNothing);
      expect(find.text('تحميل مباشر إلى الهاتف'), findsNothing);

      // Advance past first 350ms poll (still refreshing with 2 episodes).
      await tester.pump(const Duration(milliseconds: 350));
      expect(find.text('جاري تحميل الحلقات…'), findsOneWidget);
      expect(find.text('الحلقة 1'), findsNothing);

      // Complete story and advance to second 350ms poll returning all 10 episodes.
      storyCompleter.complete({'overview': 'قصة طبية مشوقة', 'cast': <dynamic>[]});
      await tester.pump(const Duration(milliseconds: 400));
      await tester.pumpAndSettle();

      _expectNoScaryErrorUi();
      expect(find.text('قصة طبية مشوقة'), findsOneWidget);
      expect(find.text('جاري تحميل الحلقات…'), findsNothing);
      expect(find.text('10 حلقة'), findsOneWidget);
      expect(find.text('الحلقة 1'), findsOneWidget);
      expect(find.text('الحلقة 10'), findsOneWidget);

      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(_channel, null);
    },
  );
}
