import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Stores the OIDC session tokens in secure storage (Keychain/Keystore) — never in
/// `shared_preferences`.
class TokenStorage {
  static const String _accessKey = 'access_token';
  static const String _refreshKey = 'refresh_token';

  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  Future<void> save(String accessToken, String refreshToken) async {
    await _storage.write(key: _accessKey, value: accessToken);
    await _storage.write(key: _refreshKey, value: refreshToken);
  }

  Future<String?> accessToken() => _storage.read(key: _accessKey);

  Future<void> clear() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }
}
