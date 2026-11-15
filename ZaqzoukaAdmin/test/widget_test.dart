import 'package:flutter_test/flutter_test.dart';
import 'package:zaqzouka_admin/main.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const ZaqzoukaAdminApp());
    expect(find.byType(ZaqzoukaAdminApp), findsOneWidget);
  });
}
