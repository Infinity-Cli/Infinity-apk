import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:infinity_apk/main.dart';
import 'package:infinity_apk/providers/api_provider.dart';
import 'package:infinity_apk/services/api_service.dart';
import 'package:infinity_apk/services/auth_service.dart';

void main() {
  testWidgets('App shows the auth screen', (WidgetTester tester) async {
    final authService = MockAuthService();
    final apiProvider = ApiProvider(api: MockInfinityApi());
    await tester.pumpWidget(MyApp(
      authService: authService,
      apiProvider: apiProvider,
    ));

    expect(find.text('Sign In'), findsOneWidget);
    expect(find.text('Sign in anonymously'), findsOneWidget);
    expect(find.text('Toggle email/password form'), findsOneWidget);

    addTearDown(authService.dispose);
  });
}
