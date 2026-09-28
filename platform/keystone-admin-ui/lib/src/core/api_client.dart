import 'package:dio/dio.dart';

import '../models/envelopes.dart';
import '../models/list_query.dart';
import '../models/models.dart';
import '../models/requests.dart';

/// REST client for one Keystone API plane (inventory + platform admin). The bearer token is attached by
/// the interceptor; the UI never talks to `dio` directly.
///
/// [basePath] is what makes one client serve both consoles: `/api/v1` is the platform plane and
/// `/api/v1/tenant` the tenant self-service plane, whose routes are otherwise identical. The console
/// decides which to use (`ConsoleScope`), so a screen never builds a URL itself.
///
/// Request bodies are the typed models in `models/requests.dart`, and a response is parsed through its
/// model's `fromJson` in the same statement, so no `Map<String, dynamic>` outlives the call
/// (`docs/CODING_GUIDELINES_FRONTEND.md` §14).
class ApiClient {
  ApiClient(this.dio, {this.basePath = '/api/v1'});

  final Dio dio;

  /// The API root this client talks to, without a trailing slash.
  final String basePath;

  Future<Session> login(LoginRequest request) async {
    final res = await dio.post<Map<String, dynamic>>(
      '$basePath/auth/login',
      data: request.toJson(),
    );
    return Session.fromJson(res.data!);
  }

  Future<Me> me() async {
    final res = await dio.get<Map<String, dynamic>>('$basePath/me');
    return Me.fromJson(res.data!);
  }

  Future<void> changePassword(ChangePasswordRequest request) async {
    await dio.post<void>('$basePath/me/password', data: request.toJson());
  }

  /// Sets another user's temporary password (`204`, no body): the backend writes it to Supabase Auth and
  /// forces a change on that user's next login. The caller's own password goes through
  /// [changePassword], which the backend only accepts with the current password.
  Future<void> resetUserPassword(
    String id,
    ResetPasswordRequest request,
  ) async {
    await dio.put<void>('$basePath/users/$id/password', data: request.toJson());
  }

  /// One page of tenants, filtered and ordered **by the server** (`docs/CODING_GUIDELINES_BACKEND.md` §8),
  /// so a search covers every tenant rather than the page the console happens to hold.
  Future<Paged<Tenant>> tenants(ListQuery query) async {
    final res = await dio.get<Map<String, dynamic>>(
      '$basePath/tenants',
      queryParameters: query.toQueryParameters(),
    );
    return _paged(res.data, Tenant.fromJson);
  }

  /// **Every** tenant, for the console's pickers (the tenant filter, the tenant and owner dropdowns).
  ///
  /// A picker cannot be fed by a page — it would silently offer only the first one — so this reads the
  /// resource's unpaged options route. [OptionList.truncated] says the backend's cap was reached.
  Future<OptionList<Tenant>> tenantOptions() async {
    final res = await dio.get<Map<String, dynamic>>('$basePath/tenants/options');
    return _options(res.data, Tenant.fromJson);
  }

  Future<Tenant> createTenant(CreateTenantRequest request) async {
    final res = await dio.post<Map<String, dynamic>>(
      '$basePath/tenants',
      data: request.toJson(),
    );
    return Tenant.fromJson(res.data!);
  }

  Future<Tenant> updateTenant(String id, UpdateTenantRequest request) async {
    final res = await dio.patch<Map<String, dynamic>>(
      '$basePath/tenants/$id',
      data: request.toJson(),
    );
    return Tenant.fromJson(res.data!);
  }

  Future<void> deleteTenant(String id) async {
    await dio.delete<void>('$basePath/tenants/$id');
  }

  /// One page of roles, filtered and ordered by the server. [ListQuery.tenantId] is an optional
  /// **platform-plane** filter: null is everything, the reserved platform id is the global catalog only, a
  /// tenant id is the global catalog plus that tenant's own rows.
  Future<Paged<Role>> roles(ListQuery query) async {
    final res = await dio.get<Map<String, dynamic>>(
      '$basePath/roles',
      queryParameters: query.toQueryParameters(),
    );
    return _paged(res.data, Role.fromJson);
  }

  /// **Every** assignable role, for the user editor's checklist — a picker, so not a page (see [roles] for
  /// what the filter means).
  Future<OptionList<Role>> roleOptions({String? tenantId}) async {
    final res = await dio.get<Map<String, dynamic>>(
      '$basePath/roles/options',
      queryParameters: tenantId == null
          ? null
          : <String, String>{'tenantId': tenantId},
    );
    return _options(res.data, Role.fromJson);
  }

  Future<Role> createRole(CreateRoleRequest request) async {
    final res = await dio.post<Map<String, dynamic>>(
      '$basePath/roles',
      data: request.toJson(),
    );
    return Role.fromJson(res.data!);
  }

  /// Replaces a role's code, scope and grants (`PATCH`). The **owner** is immutable and the request carries
  /// none, so neither plane has a tenant parameter to add or drop here.
  Future<Role> updateRole(String id, UpdateRoleRequest request) async {
    final res = await dio.patch<Map<String, dynamic>>(
      '$basePath/roles/$id',
      data: request.toJson(),
    );
    return Role.fromJson(res.data!);
  }

  /// One page of permissions, with the same filter rule as [roles].
  Future<Paged<Permission>> permissions(ListQuery query) async {
    final res = await dio.get<Map<String, dynamic>>(
      '$basePath/permissions',
      queryParameters: query.toQueryParameters(),
    );
    return _paged(res.data, Permission.fromJson);
  }

  Future<Permission> createPermission(CreatePermissionRequest request) async {
    final res = await dio.post<Map<String, dynamic>>(
      '$basePath/permissions',
      data: request.toJson(),
    );
    return Permission.fromJson(res.data!);
  }

  /// One page of users. [ListQuery.tenantId] is the platform-plane filter: null is every user, the reserved
  /// platform tenant id is the platform users (`users.tenant_id IS NULL`), any other id that tenant's; the
  /// tenant plane sends no filter, because its routes answer with the caller's own tenant.
  Future<Paged<User>> users(ListQuery query) async {
    final res = await dio.get<Map<String, dynamic>>(
      '$basePath/users',
      queryParameters: query.toQueryParameters(),
    );
    return _paged(res.data, User.fromJson);
  }

  Future<User> createUser(CreateUserRequest request) async {
    final res = await dio.post<Map<String, dynamic>>(
      '$basePath/users',
      data: request.toJson(),
    );
    return User.fromJson(res.data!);
  }

  Future<User> updateUser(String id, UpdateUserRequest request) async {
    final res = await dio.patch<Map<String, dynamic>>(
      '$basePath/users/$id',
      data: request.toJson(),
    );
    return User.fromJson(res.data!);
  }

  Future<void> deleteUser(String id) async {
    await dio.delete<void>('$basePath/users/$id');
  }

  /// Parses a page envelope. The elements go through their model's `fromJson` in the same statement, so no
  /// map outlives the call (`docs/CODING_GUIDELINES_FRONTEND.md` §14).
  Paged<T> _paged<T>(
    Map<String, dynamic>? data,
    T Function(Map<String, dynamic>) fromJson,
  ) {
    return Paged<T>.fromJson(data ?? const <String, dynamic>{}, fromJson);
  }

  /// Parses a picker envelope, as [Paged] parses a page.
  OptionList<T> _options<T>(
    Map<String, dynamic>? data,
    T Function(Map<String, dynamic>) fromJson,
  ) {
    return OptionList<T>.fromJson(data ?? const <String, dynamic>{}, fromJson);
  }
}
