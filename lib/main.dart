import 'dart:io';

import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'providers/api_provider.dart';
import 'screens/auth_screen.dart';
import 'screens/dashboard_screen.dart';
import 'services/api_service.dart';
import 'services/auth_service.dart';

// FirebaseAuthService exposes signInAnonymously, signInWithEmailAndPassword,
// and signOut through the AuthService interface.
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  AuthService authService;
  try {
    await Firebase.initializeApp(
      options: Platform.isAndroid
          ? const FirebaseOptions(
              apiKey: 'your-android-api-key',
              appId: 'your-android-app-id',
              messagingSenderId: 'your-sender-id',
              projectId: 'infinity-apk',
            )
          : null,
    );
    authService = FirebaseAuthService();
  } catch (e) {
    debugPrint('Firebase initialization failed; using mock auth: $e');
    authService = MockAuthService();
  }
  const apiBaseUrl = String.fromEnvironment(
    'INFINITY_API_URL',
    defaultValue: 'http://10.0.2.2:8000',
  );
  final apiProvider = ApiProvider(
    api: HttpInfinityApi(baseUrl: apiBaseUrl),
  );
  runApp(MyApp(authService: authService, apiProvider: apiProvider));
}

class MyApp extends StatelessWidget {
  const MyApp({
    super.key,
    required this.authService,
    required this.apiProvider,
  });

  final AuthService authService;
  final ApiProvider apiProvider;

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider<ApiProvider>.value(
      value: apiProvider,
      child: MaterialApp(
        title: 'Infinity',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
          useMaterial3: true,
        ),
        home: AuthScreen(authService: authService),
        routes: {
          '/dashboard': (context) => const DashboardScreen(),
        },
      ),
    );
  }
}
