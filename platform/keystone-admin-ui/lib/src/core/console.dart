import '../models/envelopes.dart';
import '../models/list_query.dart';
import '../models/models.dart';
import '../models/requests.dart';
import 'api_client.dart';
import 'permissions.dart';

/// The plane a signed-in caller administers, and the REST calls that plane exposes.
///
/// The platform console and a tenant's own console render the **same screens**; what differs is the
/// routes they call — and that a tenant route cannot name a tenant at all. Everything a screen needs to
/// know about "which console am I in" lives here, so no screen is duplicated and no screen can offer a
/// tenant the backend would refuse.
class ConsoleScope {
  ConsoleScope._(this._api, {required this.isPlatformPlane, this.tenantId});

  /// The platform plane: global rows are editable here and every tenant is visible.
  factory ConsoleScope.platform(ApiClient api) =>
      ConsoleScope._(api, isPlatformPlane: true);

  /// One tenant's own plane. The tenant is the caller's, never a parameter.
  factory ConsoleScope.tenant(ApiClient api, String tenantId) =>
      ConsoleScope._(api, isPlatformPlane: false, tenantId: tenantId);

  final ApiClient _api;

  /// Whether this is the platform console rather than a tenant's own.
  final bool isPlatformPlane;

  /// The tenant this console acts in — null on the platform plane.
  final String? tenantId;

  /// The permission resource that guards **roles in this plane**. A section and its write affordances are
  /// gated on the plane's own codes, never the other plane's: holding `platform:role:read-write` says
  /// nothing about a tenant's roles, and vice versa.
  String get roleResource =>
      isPlatformPlane ? PlatformResource.role : TenantResource.role;

  /// The resource that guards **permissions in this plane**, as [roleResource].
  String get permissionResource =>
      isPlatformPlane ? PlatformResource.permission : TenantResource.permission;

  /// The resource that guards **users in this plane**, as [roleResource].
  String get userResource =>
      isPlatformPlane ? PlatformResource.user : TenantResource.user;

  /// One page of roles. The query may carry a **platform-plane** tenant filter (the global catalog plus
  /// that tenant's own); on the tenant plane the filter is dropped entirely — its route has no tenant to
  /// name, so there is none for a caller to get wrong.
  Future<Paged<Role>> roles(ListQuery query) =>
      _api.roles(isPlatformPlane ? query : query.withoutTenant());

  /// **Every** role this plane may assign, for the user editor's checklist — a picker, so not a page.
  Future<OptionList<Role>> roleOptions({String? tenantId}) =>
      _api.roleOptions(tenantId: isPlatformPlane ? tenantId : null);

  Future<Role> createRole(CreateRoleRequest request) => _api.createRole(
    isPlatformPlane ? request : request.copyWith(tenantId: null),
  );

  /// Replaces a role's code, scope and grants. Nothing is plane-specific: the owner is immutable, so the
  /// request carries none, and the backend scopes the row to the caller on the tenant plane.
  Future<Role> updateRole(String id, UpdateRoleRequest request) =>
      _api.updateRole(id, request);

  /// One page of permissions, with the same filter rule as [roles].
  Future<Paged<Permission>> permissions(ListQuery query) =>
      _api.permissions(isPlatformPlane ? query : query.withoutTenant());

  Future<Permission> createPermission(CreatePermissionRequest request) =>
      _api.createPermission(
        isPlatformPlane ? request : request.copyWith(tenantId: null),
      );

  /// One page of users, with the same filter rule as [roles].
  Future<Paged<User>> users(ListQuery query) =>
      _api.users(isPlatformPlane ? query : query.withoutTenant());

  Future<User> createUser(CreateUserRequest request) =>
      _api.createUser(request);

  Future<User> updateUser(String id, UpdateUserRequest request) =>
      _api.updateUser(id, request);

  Future<void> deleteUser(String id) => _api.deleteUser(id);

  Future<void> resetUserPassword(String id, ResetPasswordRequest request) =>
      _api.resetUserPassword(id, request);
}
