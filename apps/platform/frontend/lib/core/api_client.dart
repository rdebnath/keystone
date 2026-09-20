import 'package:dio/dio.dart';

import '../models/models.dart';

/// REST client for the platform admin backend. The bearer token is attached from the Supabase
/// session by the interceptor; the UI never talks to `dio` directly.
class ApiClient {
  final Dio dio;

  ApiClient(this.dio);

  Future<Me> me() async {
    final res = await dio.get<Map<String, dynamic>>('/api/v1/me');
    return Me.fromJson(res.data!);
  }

  Future<void> markPasswordChanged() async {
    await dio.post<void>('/api/v1/me/password-changed');
  }

  Future<List<Tenant>> tenants() async {
    final res = await dio.get<List<dynamic>>('/api/v1/tenants');
    return _list(res.data, Tenant.fromJson);
  }

  Future<Tenant> createTenant(String name) async {
    final res = await dio.post<Map<String, dynamic>>('/api/v1/tenants', data: {'name': name});
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

  Future<User> createUser(String email, String temporaryPassword, List<String> roles) async {
    final res = await dio.post<Map<String, dynamic>>('/api/v1/users',
        data: {'email': email, 'temporaryPassword': temporaryPassword, 'roles': roles});
    return User.fromJson(res.data!);
  }

  List<T> _list<T>(List<dynamic>? data, T Function(Map<String, dynamic>) fromJson) {
    return (data ?? const [])
        .map((e) => fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
