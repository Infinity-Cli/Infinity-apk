import 'dart:async';

import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:infinity_apk/screens/auth_screen.dart';
import 'package:infinity_apk/services/auth_service.dart';

class _LoadingAuthService implements AuthService {
  final StreamController<User?> _controller = StreamController<User?>.broadcast();
  Completer<void>? _completer;

  @override
  Stream<User?> authStateChanges() => _controller.stream;

  @override
  Future<void> signInAnonymously() async {
    _completer = Completer<void>();
    await _completer!.future;
    _controller.add(MockUser(uid: 'anonymous-uid'));
  }

  @override
  Future<void> signInWithEmailAndPassword(String email, String password) async {
    _completer = Completer<void>();
    await _completer!.future;
    _controller.add(MockUser(uid: 'email-uid', email: email));
  }

  @override
  Future<void> signOut() async {}

  void complete() => _completer?.complete();

  void dispose() => _controller.close();
}

class _FailingAuthService implements AuthService {
  @override
  Stream<User?> authStateChanges() => const Stream<User?>.empty();

  @override
  Future<void> signInAnonymously() async {
    throw Exception('anonymous sign in failed');
  }

  @override
  Future<void> signInWithEmailAndPassword(String email, String password) async {
    throw Exception('email sign in failed');
  }

  @override
  Future<void> signOut() async {}
}

void main() {
  group('AuthScreen', () {
    testWidgets('displays sign-in options', (WidgetTester tester) async {
      final authService = MockAuthService();
      await tester.pumpWidget(
        MaterialApp(
          home: AuthScreen(authService: authService),
        ),
      );

      expect(find.text('Sign in anonymously'), findsOneWidget);
      expect(find.text('Toggle email/password form'), findsOneWidget);
      expect(find.byType(TextField), findsNothing);

      addTearDown(authService.dispose);
    });

    testWidgets('toggles email/password form', (WidgetTester tester) async {
      final authService = MockAuthService();
      await tester.pumpWidget(
        MaterialApp(
          home: AuthScreen(authService: authService),
        ),
      );

      await tester.tap(find.text('Toggle email/password form'));
      await tester.pump();

      expect(find.byType(TextField), findsNWidgets(2));
      expect(find.text('Sign in with email/password'), findsOneWidget);

      addTearDown(authService.dispose);
    });

    testWidgets('shows loading indicator while authenticating',
        (WidgetTester tester) async {
      final authService = _LoadingAuthService();
      await tester.pumpWidget(
        MaterialApp(
          home: AuthScreen(authService: authService),
        ),
      );

      await tester.tap(find.text('Sign in anonymously'));
      await tester.pump();

      expect(find.byType(LinearProgressIndicator), findsOneWidget);

      authService.complete();
      await tester.pump();

      addTearDown(authService.dispose);
    });

    testWidgets('displays error on failed sign in', (WidgetTester tester) async {
      final authService = _FailingAuthService();
      await tester.pumpWidget(
        MaterialApp(
          home: AuthScreen(authService: authService),
        ),
      );

      await tester.tap(find.text('Sign in anonymously'));
      await tester.pump();

      expect(find.text('Exception: anonymous sign in failed'), findsOneWidget);
    });

    testWidgets('navigates to dashboard after anonymous sign in',
        (WidgetTester tester) async {
      final authService = MockAuthService();
      await tester.pumpWidget(
        MaterialApp(
          home: AuthScreen(authService: authService),
          routes: {
            '/dashboard': (context) => const Scaffold(body: Text('Dashboard')),
          },
        ),
      );

      await tester.tap(find.text('Sign in anonymously'));
      await tester.pump();
      await tester.pump();

      expect(find.text('Dashboard'), findsOneWidget);

      addTearDown(authService.dispose);
    });

    testWidgets('navigates to dashboard after email/password sign in',
        (WidgetTester tester) async {
      final authService = MockAuthService();
      await tester.pumpWidget(
        MaterialApp(
          home: AuthScreen(authService: authService),
          routes: {
            '/dashboard': (context) => const Scaffold(body: Text('Dashboard')),
          },
        ),
      );

      await tester.tap(find.text('Toggle email/password form'));
      await tester.pump();

      await tester.enterText(
        find.widgetWithText(TextField, 'Email'),
        'user@example.com',
      );
      await tester.enterText(
        find.widgetWithText(TextField, 'Password'),
        'secret',
      );

      await tester.tap(find.text('Sign in with email/password'));
      await tester.pump();
      await tester.pump();

      expect(find.text('Dashboard'), findsOneWidget);

      addTearDown(authService.dispose);
    });
  });
}
