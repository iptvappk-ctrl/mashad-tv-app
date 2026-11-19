import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_demo/main.dart';

void main() {
  testWidgets('SuperIptvDemoApp builds smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const SuperIptvDemoApp());
    expect(find.text('مشغل البث وموسم رمضان'), findsOneWidget);
  });
}
