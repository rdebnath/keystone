import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api_client.dart';
import '../../core/providers.dart';
import '../../core/token_storage.dart';

/// Thin wrapper over the backend-proxied auth endpoints so screens stay small and testable.
class AuthService {
  final ApiClient api;
  final TokenStorage storage;

  AuthService(this.api, this.storage);

  Future<void> signIn(String identifier, String password) async {
    final session = await api.login(identifier, password);
    await storage.save(session.accessToken, session.refreshToken);
  }

  Future<void> changePassword(String password) => api.changePassword(password);

  Future<void> signOut() => storage.clear();
}

final authServiceProvider = Provider<AuthService>((ref) =>
    AuthService(ref.watch(apiClientProvider), ref.watch(tokenStorageProvider)));
