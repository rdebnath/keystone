/// Typed request bodies for the Keystone REST API.
///
/// Every POST body is a model rather than an inline map: the wire field names live here and
/// nowhere else, and call sites pass named arguments instead of order-sensitive positionals
/// (`docs/CODING_GUIDELINES_FRONTEND.md` §14). The classes mirror the backend's request records
/// (`docs/CODING_GUIDELINES_BACKEND.md` §13).
library;

import 'package:freezed_annotation/freezed_annotation.dart';

part 'requests.freezed.dart';
part 'requests.g.dart';

/// `POST /api/v1/auth/login` — the backend-proxied login body.
@freezed
abstract class LoginRequest with _$LoginRequest {
  const factory LoginRequest({
    required String identifier,
    required String password,
  }) = _LoginRequest;

  factory LoginRequest.fromJson(Map<String, dynamic> json) =>
      _$LoginRequestFromJson(json);
}

/// `POST /api/v1/me/password` — sets the authenticated caller's own password. `currentPassword` is
/// required (and verified by the backend) unless the caller is in the forced first-login state, where
/// the password they just signed in with is the proof. It is omitted when null, so the forced flow's
/// body is unchanged.
@freezed
abstract class ChangePasswordRequest with _$ChangePasswordRequest {
  const factory ChangePasswordRequest({
    required String password,
    @JsonKey(includeIfNull: false) String? currentPassword,
  }) = _ChangePasswordRequest;

  factory ChangePasswordRequest.fromJson(Map<String, dynamic> json) =>
      _$ChangePasswordRequestFromJson(json);
}

/// `PUT /api/v1/users/{id}/password` — sets another user's temporary password; the backend hands it to
/// Supabase Auth and forces a change on the target's next login. A caller changing their *own*
/// password uses [ChangePasswordRequest] instead.
@freezed
abstract class ResetPasswordRequest with _$ResetPasswordRequest {
  const factory ResetPasswordRequest({required String temporaryPassword}) =
      _ResetPasswordRequest;

  factory ResetPasswordRequest.fromJson(Map<String, dynamic> json) =>
      _$ResetPasswordRequestFromJson(json);
}

/// `POST /api/v1/tenants` — creates a tenant. `country` is the tenant's ISO 3166-1 alpha-2 code
/// (`IN`, `DE`); null records none, and the backend uppercases and validates it.
@freezed
abstract class CreateTenantRequest with _$CreateTenantRequest {
  const factory CreateTenantRequest({
    required String name,
    required String slug,
    String? country,
  }) = _CreateTenantRequest;

  factory CreateTenantRequest.fromJson(Map<String, dynamic> json) =>
      _$CreateTenantRequestFromJson(json);
}

/// `PATCH /api/v1/tenants/{id}` — renames a tenant and sets its slug and country. The backend accepts
/// the same body as create (its `TenantRequest`), but the intent is explicit here. A null/omitted
/// `country` clears the recorded country, exactly as the full body does for `slug`.
@freezed
abstract class UpdateTenantRequest with _$UpdateTenantRequest {
  const factory UpdateTenantRequest({
    required String name,
    required String slug,
    String? country,
  }) = _UpdateTenantRequest;

  factory UpdateTenantRequest.fromJson(Map<String, dynamic> json) =>
      _$UpdateTenantRequestFromJson(json);
}

/// `PATCH /api/v1/users/{id}` — renames a user, sets its phone number and replaces its roles (mirrors
/// the backend's `UserUpdateRequest`). The email is not part of the request: it is the virtual Supabase
/// Auth identity bound to the user's `sub` and is not editable from the console. A null/omitted
/// `phoneNumber` clears the recorded number.
@freezed
abstract class UpdateUserRequest with _$UpdateUserRequest {
  const factory UpdateUserRequest({
    required String username,
    String? phoneNumber,
    @Default(<String>[]) List<String> roles,
  }) = _UpdateUserRequest;

  factory UpdateUserRequest.fromJson(Map<String, dynamic> json) =>
      _$UpdateUserRequestFromJson(json);
}

/// `POST /api/v1/roles` — creates a role with its permission codes. `tenantId` is the optional
/// **owner**: omitted (null) on the platform plane creates a global role; the tenant plane always
/// creates its own and never sends one.
@freezed
abstract class CreateRoleRequest with _$CreateRoleRequest {
  const factory CreateRoleRequest({
    required String code,
    required String scope,
    String? tenantId,
    @Default(<String>[]) List<String> permissions,
  }) = _CreateRoleRequest;

  factory CreateRoleRequest.fromJson(Map<String, dynamic> json) =>
      _$CreateRoleRequestFromJson(json);
}

/// `PATCH /api/v1/roles/{id}` — replaces a role's code, scope and grants.
///
/// The **owner** is not part of the request: a role's `tenant_id` is immutable, so the backend ignores it
/// (its `RoleRequest` carries it for create only, and a tenant-owned role must stay `TENANT` scope). The
/// intent is explicit here, exactly as `UpdateTenantRequest` is explicit about the create-shaped body it
/// shares with `CreateTenantRequest`.
@freezed
abstract class UpdateRoleRequest with _$UpdateRoleRequest {
  const factory UpdateRoleRequest({
    required String code,
    required String scope,
    @Default(<String>[]) List<String> permissions,
  }) = _UpdateRoleRequest;

  factory UpdateRoleRequest.fromJson(Map<String, dynamic> json) =>
      _$UpdateRoleRequestFromJson(json);
}

/// `POST /api/v1/permissions` — creates a permission in a scope. `tenantId` is the optional owner, as
/// for roles.
@freezed
abstract class CreatePermissionRequest with _$CreatePermissionRequest {
  const factory CreatePermissionRequest({
    required String code,
    required String scope,
    String? tenantId,
  }) = _CreatePermissionRequest;

  factory CreatePermissionRequest.fromJson(Map<String, dynamic> json) =>
      _$CreatePermissionRequestFromJson(json);
}

/// `POST /api/v1/users` — provisions a user; `tenantId` is null for a platform user, `email` defaults
/// to `username@tenantid.com` on the backend when omitted, and `phoneNumber` is null when none is
/// recorded (the backend stores it in E.164 form, tolerating the separators a human types).
@freezed
abstract class CreateUserRequest with _$CreateUserRequest {
  const factory CreateUserRequest({
    required String username,
    String? tenantId,
    String? email,
    String? phoneNumber,
    required String temporaryPassword,
    @Default(<String>[]) List<String> roles,
  }) = _CreateUserRequest;

  factory CreateUserRequest.fromJson(Map<String, dynamic> json) =>
      _$CreateUserRequestFromJson(json);
}
