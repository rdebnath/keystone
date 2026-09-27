import 'package:dio/dio.dart';

import '../models/models.dart';
import '../models/requests.dart';

/// REST client for the Keystone backend (inventory + platform admin). The bearer token is
/// attached by the interceptor; the UI never talks to `dio` directly.
///
/// Request bodies are the typed models in `models/requests.dart`, and a response is parsed
/// through its model's `fromJson` in the same statement, so no `Map<String, dynamic>` outlives the
/// call (`docs/CODING_GUIDELINES_FRONTEND.md` §14).
class ApiClient {
  final Dio dio;

  ApiClient(this.dio);

  Future<Session> login(LoginRequest request) async {
    final res = await dio.post<Map<String, dynamic>>(
      '/api/v1/auth/login',
      data: request.toJson(),
    );
    return Session.fromJson(res.data!);
  }

  Future<Me> me() async {
    final res = await dio.get<Map<String, dynamic>>('/api/v1/me');
    return Me.fromJson(res.data!);
  }

  Future<void> changePassword(ChangePasswordRequest request) async {
    await dio.post<void>('/api/v1/me/password', data: request.toJson());
  }

  /// Sets another user's temporary password (`204`, no body): the backend writes it to Supabase Auth and
  /// forces a change on that user's next login. The caller's own password goes through
  /// [changePassword], which the backend only accepts with the current password.
  Future<void> resetUserPassword(
    String id,
    ResetPasswordRequest request,
  ) async {
    await dio.put<void>('/api/v1/users/$id/password', data: request.toJson());
  }

  Future<List<Tenant>> tenants() async {
    final res = await dio.get<List<dynamic>>('/api/v1/tenants');
    return _list(res.data, Tenant.fromJson);
  }

  Future<Tenant> createTenant(CreateTenantRequest request) async {
    final res = await dio.post<Map<String, dynamic>>(
      '/api/v1/tenants',
      data: request.toJson(),
    );
    return Tenant.fromJson(res.data!);
  }

  Future<Tenant> updateTenant(String id, UpdateTenantRequest request) async {
    final res = await dio.patch<Map<String, dynamic>>(
      '/api/v1/tenants/$id',
      data: request.toJson(),
    );
    return Tenant.fromJson(res.data!);
  }

  Future<void> deleteTenant(String id) async {
    await dio.delete<void>('/api/v1/tenants/$id');
  }

  Future<List<Role>> roles() async {
    final res = await dio.get<List<dynamic>>('/api/v1/roles');
    return _list(res.data, Role.fromJson);
  }

  Future<Role> createRole(CreateRoleRequest request) async {
    final res = await dio.post<Map<String, dynamic>>(
      '/api/v1/roles',
      data: request.toJson(),
    );
    return Role.fromJson(res.data!);
  }

  Future<List<Permission>> permissions() async {
    final res = await dio.get<List<dynamic>>('/api/v1/permissions');
    return _list(res.data, Permission.fromJson);
  }

  Future<Permission> createPermission(CreatePermissionRequest request) async {
    final res = await dio.post<Map<String, dynamic>>(
      '/api/v1/permissions',
      data: request.toJson(),
    );
    return Permission.fromJson(res.data!);
  }

  /// Lists users: every user when [tenantId] is null, one tenant's users otherwise (the reserved
  /// platform tenant id lists the platform users, `users.tenant_id IS NULL`).
  Future<List<User>> users({String? tenantId}) async {
    final res = await dio.get<List<dynamic>>(
      '/api/v1/users',
      queryParameters: tenantId == null
          ? null
          : <String, String>{'tenantId': tenantId},
    );
    return _list(res.data, User.fromJson);
  }

  Future<User> createUser(CreateUserRequest request) async {
    final res = await dio.post<Map<String, dynamic>>(
      '/api/v1/users',
      data: request.toJson(),
    );
    return User.fromJson(res.data!);
  }

  Future<User> updateUser(String id, UpdateUserRequest request) async {
    final res = await dio.patch<Map<String, dynamic>>(
      '/api/v1/users/$id',
      data: request.toJson(),
    );
    return User.fromJson(res.data!);
  }

  Future<void> deleteUser(String id) async {
    await dio.delete<void>('/api/v1/users/$id');
  }

  List<T> _list<T>(
    List<dynamic>? data,
    T Function(Map<String, dynamic>) fromJson,
  ) {
    return (data ?? const [])
        .map((e) => fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
