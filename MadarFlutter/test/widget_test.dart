import 'package:flutter_test/flutter_test.dart';

import 'package:madar_flutter/main.dart';

void main() {
  testWidgets('shows the branded intro and the app navigation', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MyApp());
    await tester.pump();

    expect(find.text('زقزوقة'), findsWidgets);
    expect(find.text('تخطي'), findsOneWidget);

    await tester.tap(find.text('تخطي'));
    await tester.pump(const Duration(milliseconds: 350));
    await tester.pump(const Duration(milliseconds: 350));

    expect(find.text('أفلام ومسلسلات'), findsOneWidget);
    expect(find.text('بثات الكورة'), findsOneWidget);
    expect(find.text('المفضلة'), findsOneWidget);
  });
}
