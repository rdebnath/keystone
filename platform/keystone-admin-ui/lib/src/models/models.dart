/// Immutable domain models parsed from the platform REST API (no raw maps leak into the UI).
///
/// Models are `freezed` — immutable, value-typed, with `copyWith` — and carry
/// `json_serializable` `fromJson`/`toJson` (`docs/CODING_GUIDELINES_FRONTEND.md` §14). Optional or
/// defaulted wire fields are declared with `@Default(...)`, which also becomes the JSON default so
/// a server-first deploy cannot break an older client.
library;

import 'package:freezed_annotation/freezed_annotation.dart';

part 'models.freezed.dart';
part 'models.g.dart';

/// The authenticated caller's identity and effective permissions (`GET /api/v1/me`).
@freezed
abstract class Me with _$Me {
  const Me._();

  const factory Me({
    required String sub,
    @Default('') String username,
    String? tenantId,
    @Default(false) bool mustChangePassword,
    @Default(<String>[]) List<String> permissions,
  }) = _Me;

  factory Me.fromJson(Map<String, dynamic> json) => _$MeFromJson(json);

  /// No tenant means the platform plane (the platform admin console).
  bool get isPlatformAdmin => tenantId == null;

  /// Whether the caller holds [code]; the wildcard grants everything.
  bool allows(String code) =>
      permissions.contains(Permission.wildcard) || permissions.contains(code);

  /// Whether the caller can read [resource] — its read-only code, its read/write code, or the
  /// wildcard. Mirrors the backend's accepted codes for a read check, so the console never renders a
  /// section the caller would be denied (`docs/ARCHITECTURE.md` §9.6: this is UX, not security).
  bool allowsResource(String resource) =>
      allows('$resource:${PermissionAccess.readOnly.suffix}') ||
      allows('$resource:${PermissionAccess.readWrite.suffix}');

  /// Whether the caller can create, update and delete [resource] — its read/write code, or the
  /// wildcard.
  bool canWrite(String resource) =>
      allows('$resource:${PermissionAccess.readWrite.suffix}');

  /// Whether the caller may **grant** [code] to a role — the client mirror of the backend's
  /// `CallerScope.requireGrantable` (`docs/ARCHITECTURE.md` §9.5, *grant only what you hold*), so the
  /// role editor's picker disables a row the server would refuse instead of offering it and failing on save.
  ///
  /// The platform plane is unrestricted (the backend exempts it); a tenant caller may never grant the
  /// wildcard, and may grant a code only if it holds it — where "holds" follows the guard's implication, so
  /// a `…:read-write` grant covers the `…:read-only` code of the same resource.
  ///
  /// UX, not security: the backend refuses what this predicts, and this is never the boundary.
  bool canGrant(String code) {
    if (isPlatformAdmin) {
      return true;
    }
    return code != Permission.wildcard && _holds(code);
  }

  /// Whether the caller holds [code], exactly as the backend's `CallerScope.holds` decides it — including
  /// that a wildcard in the set does **not** make a concrete code held (the guard only checks the code and
  /// its read/write sibling), so a prediction here never outruns the server's.
  bool _holds(String code) {
    if (permissions.contains(code)) {
      return true;
    }
    final level = PermissionAccess.fromCode(code);
    if (level == null || level == PermissionAccess.readWrite) {
      return false;
    }
    final separator = code.lastIndexOf(':');
    return permissions.contains(
      '${code.substring(0, separator)}:${PermissionAccess.readWrite.suffix}',
    );
  }
}

/// A tenant (`GET /api/v1/tenants`). `createdAt`/`updatedAt` stay the wire's ISO-8601 strings.
/// `country` is the tenant's ISO 3166-1 alpha-2 code (`IN`, `DE`, uppercased by the backend), or null
/// when none is recorded.
@freezed
abstract class Tenant with _$Tenant {
  const Tenant._();

  /// The reserved id of the **platform plane**, mirrored from the backend's
  /// `PlatformSchema.PLATFORM_TENANT_ID`: an all-zero UUID, which a real tenant (a random v4) cannot collide
  /// with, and which is never persisted.
  ///
  /// `GET /api/v1/tenants` surfaces it as the synthetic [isPlatform] row, the owner filters use it for *"the
  /// global catalog only"*, and a **global role** is queried with it because the catalog is all such a role
  /// may hold (`RoleService.grantableTo`).
  static const String platformId = '00000000-0000-0000-0000-000000000000';

  const factory Tenant({
    required String id,
    required String name,
    @Default('') String slug,
    String? country,
    @Default(false) bool platform,
    @Default('') String createdAt,
    @Default('') String updatedAt,
  }) = _Tenant;

  factory Tenant.fromJson(Map<String, dynamic> json) => _$TenantFromJson(json);

  /// The platform plane (`users.tenant_id IS NULL`) is listed as a tenant so its users can be managed
  /// like a tenant's; it carries the reserved id and cannot be renamed or deleted.
  bool get isPlatform => platform;

  /// The label shown for a user that belongs to this tenant.
  String get userPlane => isPlatform ? 'Platform' : slug;
}

/// A role with its granted permission codes.
///
/// `tenantId` is the role's **owner**: `null` is the global, platform-defined catalog (usable by the
/// platform plane and every tenant), any other value is the tenant that owns the row. `scope` is
/// independent of the owner and says what the role is *about* (`PLATFORM` = cross-tenant, `TENANT` =
/// within one customer).
@freezed
abstract class Role with _$Role {
  const Role._();

  const factory Role({
    required String id,
    required String code,
    @Default('') String scope,
    String? tenantId,
    @Default(<String>[]) List<String> permissions,
    @Default('') String createdAt,
    @Default('') String updatedAt,
  }) = _Role;

  factory Role.fromJson(Map<String, dynamic> json) => _$RoleFromJson(json);

  /// Platform roles span tenants; tenant roles belong to one customer and may not be granted to a
  /// platform user (`Scope` on the backend).
  bool get isPlatformScope => scope == 'PLATFORM';

  /// A global role: defined by the platform, usable by every tenant and the platform plane.
  bool get isGlobal => tenantId == null;

  /// The owner's label: a tenant-owned role, or the global catalog.
  String get ownerLabel => isGlobal ? 'Global' : 'This tenant';
}

/// The two access levels a permission can carry — read/write (`…:read-write`: read, create, update
/// and delete) and read-only (`…:read-only`: read only) — as the last segment of the permission
/// code (`docs/ARCHITECTURE.md` §9.3).
enum PermissionAccess {
  readOnly('read-only', 'read-only'),
  readWrite('read-write', 'read/write');

  const PermissionAccess(this.suffix, this.label);

  /// The code suffix that carries this level.
  final String suffix;

  /// Human-readable name of the level, for the admin UI.
  final String label;

  /// Parses the level from a permission code's last segment; null when the code carries neither
  /// suffix (the server validates the level, so this only happens for a stale catalog row).
  static PermissionAccess? fromCode(String code) {
    final separator = code.lastIndexOf(':');
    if (separator < 0 || separator == code.length - 1) {
      return null;
    }
    final suffix = code.substring(separator + 1);
    for (final level in values) {
      if (level.suffix == suffix) {
        return level;
      }
    }
    return null;
  }
}

/// A permission in a scope (`platform` or `tenant`), owned globally or by one tenant.
@freezed
abstract class Permission with _$Permission {
  const Permission._();

  /// The wildcard permission: grants everything, and is held only by `platform-admin`.
  static const String wildcard = '*';

  const factory Permission({
    required String id,
    required String code,
    @Default('') String scope,
    String? tenantId,
    @Default('') String createdAt,
    @Default('') String updatedAt,
  }) = _Permission;

  factory Permission.fromJson(Map<String, dynamic> json) =>
      _$PermissionFromJson(json);

  /// The access level the code carries. The wildcard grants everything, so it reads as read/write;
  /// null when a code carries no level at all (a stale catalog row from before the two levels).
  PermissionAccess? get access => code == wildcard
      ? PermissionAccess.readWrite
      : PermissionAccess.fromCode(code);

  /// A global permission: part of the platform-defined catalog, assignable by every tenant.
  bool get isGlobal => tenantId == null;

  /// The owner's label: a tenant-defined permission, or the global catalog.
  String get ownerLabel => isGlobal ? 'Global' : 'This tenant';
}

/// A user; `tenantId` is null for a platform user, and `phoneNumber` is null when none is recorded
/// (the backend stores it in E.164 form).
@freezed
abstract class User with _$User {
  const factory User({
    required String id,
    required String sub,
    @Default('') String username,
    required String email,
    String? phoneNumber,
    String? tenantId,
    @Default(false) bool mustChangePassword,
    @Default(<String>[]) List<String> roles,
    @Default('') String createdAt,
    @Default('') String updatedAt,
  }) = _User;

  factory User.fromJson(Map<String, dynamic> json) => _$UserFromJson(json);
}

/// A Supabase Auth session returned by the backend-proxied login
/// (`POST /api/v1/auth/login`).
@freezed
abstract class Session with _$Session {
  const factory Session({
    required String accessToken,
    @Default('') String refreshToken,
    @Default('bearer') String tokenType,
    @Default(0) int expiresIn,
  }) = _Session;

  factory Session.fromJson(Map<String, dynamic> json) =>
      _$SessionFromJson(json);
}
