import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:madar_flutter/main.dart';

void main() {
  testWidgets('shows the startup status gate before the app content', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MyApp());
    await tester.pump();

    expect(find.text('جاري التحقق من المنظومة...'), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
