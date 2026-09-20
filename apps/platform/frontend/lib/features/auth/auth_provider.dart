import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

/// Reactive snapshot of whether a Supabase Auth session exists.
class AuthState {
  final bool signedIn;

  const AuthState(this.signedIn);
}

/// Streams the auth state, seeded with the current (possibly restored) session.
final authStateProvider = StreamProvider<AuthState>((ref) async* {
  final client = Supabase.instance.client;
  yield AuthState(client.auth.currentSession != null);
  await for (final event in client.auth.onAuthStateChange) {
    yield AuthState(event.session != null);
  }
});

/// Thin wrapper over Supabase Auth so screens stay small and testable.
class AuthService {
  Future<void> signIn(String email, String password) async {
    await Supabase.instance.client.auth
        .signInWithPassword(email: email, password: password);
  }

  Future<void> updatePassword(String password) async {
    await Supabase.instance.client.auth
        .updateUser(UserAttributes(password: password));
  }

  Future<void> signOut() async {
    await Supabase.instance.client.auth.signOut();
  }
}

final authServiceProvider = Provider<AuthService>((ref) => AuthService());
