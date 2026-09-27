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

/// `POST /api/v1/me/password` — sets the authenticated caller's own password.
@freezed
abstract class ChangePasswordRequest with _$ChangePasswordRequest {
  const factory ChangePasswordRequest({required String password}) =
      _ChangePasswordRequest;

  factory ChangePasswordRequest.fromJson(Map<String, dynamic> json) =>
      _$ChangePasswordRequestFromJson(json);
}

/// `POST /api/v1/tenants` — creates a tenant.
@freezed
abstract class CreateTenantRequest with _$CreateTenantRequest {
  const factory CreateTenantRequest({
    required String name,
    required String slug,
  }) = _CreateTenantRequest;

  factory CreateTenantRequest.fromJson(Map<String, dynamic> json) =>
      _$CreateTenantRequestFromJson(json);
}

/// `POST /api/v1/roles` — creates a role with its permission codes.
@freezed
abstract class CreateRoleRequest with _$CreateRoleRequest {
  const factory CreateRoleRequest({
    required String code,
    required String scope,
    @Default(<String>[]) List<String> permissions,
  }) = _CreateRoleRequest;

  factory CreateRoleRequest.fromJson(Map<String, dynamic> json) =>
      _$CreateRoleRequestFromJson(json);
}

/// `POST /api/v1/permissions` — creates a permission in a scope.
@freezed
abstract class CreatePermissionRequest with _$CreatePermissionRequest {
  const factory CreatePermissionRequest({
    required String code,
    required String scope,
  }) = _CreatePermissionRequest;

  factory CreatePermissionRequest.fromJson(Map<String, dynamic> json) =>
      _$CreatePermissionRequestFromJson(json);
}

/// `POST /api/v1/users` — provisions a user; `tenantId` is null for a platform user and `email`
/// defaults to `username@tenantid.com` on the backend when omitted.
@freezed
abstract class CreateUserRequest with _$CreateUserRequest {
  const factory CreateUserRequest({
    required String username,
    String? tenantId,
    String? email,
    required String temporaryPassword,
    @Default(<String>[]) List<String> roles,
  }) = _CreateUserRequest;

  factory CreateUserRequest.fromJson(Map<String, dynamic> json) =>
      _$CreateUserRequestFromJson(json);
}
