import 'dart:async';

import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

import '../services/auth_service.dart';

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key, required this.authService});

  final AuthService authService;

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  StreamSubscription<User?>? _authSubscription;

  bool _isLoading = false;
  bool _showEmailForm = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _authSubscription =
        widget.authService.authStateChanges().listen(_onAuthStateChanged);
  }

  @override
  void dispose() {
    _authSubscription?.cancel();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _onAuthStateChanged(User? user) {
    if (user != null && mounted) {
      Navigator.of(context).pushReplacementNamed('/dashboard');
    }
  }

  void _setLoading(bool value) {
    if (mounted) {
      setState(() => _isLoading = value);
    }
  }

  void _setError(Object? error) {
    if (mounted) {
      setState(() => _error = error?.toString());
    }
  }

  Future<void> _signInAnonymously() async {
    _setLoading(true);
    _setError(null);
    try {
      await widget.authService.signInAnonymously();
    } on Exception catch (e) {
      _setError(e);
    } finally {
      _setLoading(false);
    }
  }

  Future<void> _signInWithEmail() async {
    _setLoading(true);
    _setError(null);
    try {
      await widget.authService.signInWithEmailAndPassword(
        _emailController.text.trim(),
        _passwordController.text,
      );
    } on Exception catch (e) {
      _setError(e);
    } finally {
      _setLoading(false);
    }
  }

  Widget _buildEmailForm() {
    return Column(
      children: [
        TextField(
          controller: _emailController,
          decoration: const InputDecoration(labelText: 'Email'),
          keyboardType: TextInputType.emailAddress,
          autocorrect: false,
        ),
        TextField(
          controller: _passwordController,
          decoration: const InputDecoration(labelText: 'Password'),
          obscureText: true,
          autocorrect: false,
        ),
        const SizedBox(height: 16),
        ElevatedButton(
          onPressed: _isLoading ? null : _signInWithEmail,
          child: const Text('Sign in with email/password'),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Sign In'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (_isLoading) const LinearProgressIndicator(),
            if (_error != null)
              Text(
                _error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ElevatedButton(
              onPressed: _isLoading ? null : _signInAnonymously,
              child: const Text('Sign in anonymously'),
            ),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: _isLoading
                  ? null
                  : () => setState(() => _showEmailForm = !_showEmailForm),
              child: const Text('Toggle email/password form'),
            ),
            if (_showEmailForm) _buildEmailForm(),
          ],
        ),
      ),
    );
  }
}
