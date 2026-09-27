import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api_client.dart';
import '../../core/providers.dart';
import '../../core/token_storage.dart';
import '../../models/requests.dart';

/// Thin wrapper over the backend-proxied auth endpoints so screens stay small and testable.
class AuthService {
  final ApiClient api;
  final TokenStorage storage;

  AuthService(this.api, this.storage);

  Future<void> signIn(String identifier, String password) async {
    final session = await api.login(
      LoginRequest(identifier: identifier, password: password),
    );
    await storage.save(session.accessToken, session.refreshToken);
  }

  /// Changes the signed-in caller's own password. [currentPassword] is required unless the backend has
  /// the caller in the forced first-login state (`Me.mustChangePassword`), where the password just used
  /// to sign in is the proof.
  Future<void> changePassword(String password, {String? currentPassword}) =>
      api.changePassword(
        ChangePasswordRequest(
          password: password,
          currentPassword: currentPassword,
        ),
      );

  Future<void> signOut() => storage.clear();
}

final authServiceProvider = Provider<AuthService>(
  (ref) => AuthService(
    ref.watch(apiClientProvider),
    ref.watch(tokenStorageProvider),
  ),
);
