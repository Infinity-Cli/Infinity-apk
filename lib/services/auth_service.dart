import 'dart:async';

import 'package:firebase_auth/firebase_auth.dart';

/// Interface for authentication operations.
///
/// Implementations must provide signInAnonymously, signInWithEmailAndPassword,
/// signOut, and a stream of auth state changes.
abstract class AuthService {
  Stream<User?> authStateChanges();

  Future<void> signInAnonymously();

  Future<void> signInWithEmailAndPassword(String email, String password);

  Future<void> signOut();
}

/// Production implementation backed by Firebase Authentication.
class FirebaseAuthService implements AuthService {
  final FirebaseAuth _auth;

  FirebaseAuthService({FirebaseAuth? auth}) : _auth = auth ?? FirebaseAuth.instance;

  @override
  Stream<User?> authStateChanges() => _auth.authStateChanges();

  @override
  Future<void> signInAnonymously() async {
    await _auth.signInAnonymously();
  }

  @override
  Future<void> signInWithEmailAndPassword(String email, String password) async {
    await _auth.signInWithEmailAndPassword(email: email, password: password);
  }

  @override
  Future<void> signOut() async => _auth.signOut();
}

/// In-memory implementation for testing and local development.
class MockAuthService implements AuthService {
  final StreamController<User?> _controller = StreamController<User?>.broadcast();

  @override
  Stream<User?> authStateChanges() => _controller.stream;

  @override
  Future<void> signInAnonymously() async {
    _controller.add(MockUser(uid: 'anonymous-uid'));
  }

  @override
  Future<void> signInWithEmailAndPassword(String email, String password) async {
    _controller.add(MockUser(uid: 'email-uid', email: email));
  }

  @override
  Future<void> signOut() async {
    _controller.add(null);
  }

  void dispose() => _controller.close();
}

/// Minimal [User] stand-in for [MockAuthService].
class MockUser implements User {
  MockUser({required this.uid, this.email});

  @override
  final String uid;

  @override
  final String? email;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
