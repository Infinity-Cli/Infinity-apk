import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:infinity_apk/services/auth_service.dart';

void main() {
  group('MockAuthService', () {
    late MockAuthService authService;

    setUp(() {
      authService = MockAuthService();
    });

    tearDown(() async {
      authService.dispose();
    });

    test('emits signed-in state after signInAnonymously and null after signOut',
        () async {
      final states = <User?>[];
      final subscription = authService.authStateChanges().listen(states.add);

      await authService.signInAnonymously();
      await Future<void>.value();
      expect(states.last, isA<User>());
      expect(states.last!.uid, 'anonymous-uid');

      await authService.signOut();
      await Future<void>.value();
      expect(states.last, isNull);

      await subscription.cancel();
    });

    test('emits signed-in state after email/password sign in', () async {
      const email = 'test@example.com';
      const password = 'password123';

      final states = <User?>[];
      final subscription = authService.authStateChanges().listen(states.add);

      await authService.signInWithEmailAndPassword(email, password);
      await Future<void>.value();
      expect(states.last, isA<User>());
      expect(states.last!.email, email);

      await subscription.cancel();
    });
  });
}
