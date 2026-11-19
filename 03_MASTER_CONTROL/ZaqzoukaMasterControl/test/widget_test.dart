import 'package:flutter_test/flutter_test.dart';
import 'package:zaqzouka_master_control/main.dart';

void main() {
  testWidgets('Master control app smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const ZaqzoukaMasterApp());
    expect(find.text('لوحة التحكم الإداري الأعلى'), findsOneWidget);
  });
}
