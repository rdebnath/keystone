import 'package:dio/dio.dart';

import '../models/models.dart';

/// A Supabase Auth session returned by the backend-proxied login.
class Session {
  final String accessToken;
  final String refreshToken;
  final String tokenType;
  final int expiresIn;

  const Session({
    required this.accessToken,
    required this.refreshToken,
    required this.tokenType,
    required this.expiresIn,
  });

  factory Session.fromJson(Map<String, dynamic> json) => Session(
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String? ?? '',
        tokenType: json['tokenType'] as String? ?? 'bearer',
        expiresIn: json['expiresIn'] as int? ?? 0,
      );
}

/// REST client for the Keystone backend (inventory + platform admin). The bearer token is
/// attached by the interceptor; the UI never talks to `dio` directly.
class ApiClient {
  final Dio dio;

  ApiClient(this.dio);

  Future<Session> login(String identifier, String password) async {
    final res = await dio.post<Map<String, dynamic>>('/api/v1/auth/login',
        data: {'identifier': identifier, 'password': password});
    return Session.fromJson(res.data!);
  }

  Future<Me> me() async {
    final res = await dio.get<Map<String, dynamic>>('/api/v1/me');
    return Me.fromJson(res.data!);
  }

  Future<void> changePassword(String password) async {
    await dio.post<void>('/api/v1/me/password', data: {'password': password});
  }

  Future<List<Tenant>> tenants() async {
    final res = await dio.get<List<dynamic>>('/api/v1/tenants');
    return _list(res.data, Tenant.fromJson);
  }

  Future<Tenant> createTenant(String name, String slug) async {
    final res = await dio.post<Map<String, dynamic>>('/api/v1/tenants',
        data: {'name': name, 'slug': slug});
    return Tenant.fromJson(res.data!);
  }

  Future<List<Role>> roles() async {
    final res = await dio.get<List<dynamic>>('/api/v1/roles');
    return _list(res.data, Role.fromJson);
  }

  Future<Role> createRole(String code, String scope, List<String> permissions) async {
    final res = await dio.post<Map<String, dynamic>>('/api/v1/roles',
        data: {'code': code, 'scope': scope, 'permissions': permissions});
    return Role.fromJson(res.data!);
  }

  Future<List<Permission>> permissions() async {
    final res = await dio.get<List<dynamic>>('/api/v1/permissions');
    return _list(res.data, Permission.fromJson);
  }

  Future<Permission> createPermission(String code, String scope) async {
    final res = await dio.post<Map<String, dynamic>>('/api/v1/permissions',
        data: {'code': code, 'scope': scope});
    return Permission.fromJson(res.data!);
  }

  Future<List<User>> users() async {
    final res = await dio.get<List<dynamic>>('/api/v1/users');
    return _list(res.data, User.fromJson);
  }

  Future<User> createUser(
      String username, String? tenantId, String? email, String temporaryPassword, List<String> roles) async {
    final res = await dio.post<Map<String, dynamic>>('/api/v1/users', data: {
      'username': username,
      'tenantId': tenantId,
      'email': email,
      'temporaryPassword': temporaryPassword,
      'roles': roles,
    });
    return User.fromJson(res.data!);
  }

  List<T> _list<T>(List<dynamic>? data, T Function(Map<String, dynamic>) fromJson) {
    return (data ?? const [])
        .map((e) => fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
