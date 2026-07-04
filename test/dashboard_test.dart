import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:infinity_apk/providers/api_provider.dart';
import 'package:infinity_apk/screens/dashboard_screen.dart';
import 'package:infinity_apk/services/api_service.dart';
import 'package:infinity_apk/services/websocket_service.dart';
import 'package:provider/provider.dart';

void main() {
  group('DashboardScreen', () {
    late MockInfinityApi api;
    late MockWebSocketService webSocket;
    late ApiProvider provider;

    setUp(() {
      api = MockInfinityApi();
      webSocket = MockWebSocketService();
      provider = ApiProvider(api: api);
      provider.setCurrentRuntimeId('runtime-1');
    });

    Future<void> pumpDashboard(WidgetTester tester) async {
      await tester.pumpWidget(
        ChangeNotifierProvider<ApiProvider>.value(
          value: provider,
          child: MaterialApp(
            home: DashboardScreen(
              api: api,
              webSocket: webSocket,
            ),
          ),
        ),
      );
      // Allow the initial runtime load and connection poller to settle.
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 200));
    }

    testWidgets('renders control buttons', (WidgetTester tester) async {
      await pumpDashboard(tester);

      expect(find.byKey(const Key('pauseButton')), findsOneWidget);
      expect(find.byKey(const Key('resumeButton')), findsOneWidget);
      expect(find.byKey(const Key('startButton')), findsOneWidget);
    });

    testWidgets('pause sends pause command', (WidgetTester tester) async {
      await pumpDashboard(tester);

      await tester.tap(find.byKey(const Key('pauseButton')));
      await tester.pump();

      expect(api.sentCommands, isNotEmpty);
      expect(api.sentCommands.last['type'], 'pause');
    });

    testWidgets('resume sends resume command', (WidgetTester tester) async {
      await pumpDashboard(tester);

      await tester.tap(find.byKey(const Key('resumeButton')));
      await tester.pump();

      expect(api.sentCommands, isNotEmpty);
      expect(api.sentCommands.last['type'], 'resume');
    });

    testWidgets('start sends start command', (WidgetTester tester) async {
      await pumpDashboard(tester);

      await tester.tap(find.byKey(const Key('startButton')));
      await tester.pump();

      expect(api.sentCommands, isNotEmpty);
      expect(api.sentCommands.last['type'], 'start');
    });

    testWidgets('indicator toggles with connection changes',
        (WidgetTester tester) async {
      webSocket.simulateConnectionChange(true);
      await pumpDashboard(tester);

      // The indicator should reflect the online state.
      expect(find.text('Online'), findsWidgets);

      webSocket.simulateConnectionChange(false);
      // Wait for the poller and stream listener to propagate the change.
      await tester.pump(const Duration(milliseconds: 300));

      expect(find.text('Offline'), findsWidgets);

      webSocket.simulateConnectionChange(true);
      await tester.pump(const Duration(milliseconds: 300));

      expect(find.text('Online'), findsWidgets);
    });
  });
}
