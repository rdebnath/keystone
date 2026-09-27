// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'requests.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_LoginRequest _$LoginRequestFromJson(Map<String, dynamic> json) =>
    _LoginRequest(
      identifier: json['identifier'] as String,
      password: json['password'] as String,
    );

Map<String, dynamic> _$LoginRequestToJson(_LoginRequest instance) =>
    <String, dynamic>{
      'identifier': instance.identifier,
      'password': instance.password,
    };

_ChangePasswordRequest _$ChangePasswordRequestFromJson(
  Map<String, dynamic> json,
) => _ChangePasswordRequest(
  password: json['password'] as String,
  currentPassword: json['currentPassword'] as String?,
);

Map<String, dynamic> _$ChangePasswordRequestToJson(
  _ChangePasswordRequest instance,
) => <String, dynamic>{
  'password': instance.password,
  'currentPassword': ?instance.currentPassword,
};

_ResetPasswordRequest _$ResetPasswordRequestFromJson(
  Map<String, dynamic> json,
) => _ResetPasswordRequest(
  temporaryPassword: json['temporaryPassword'] as String,
);

Map<String, dynamic> _$ResetPasswordRequestToJson(
  _ResetPasswordRequest instance,
) => <String, dynamic>{'temporaryPassword': instance.temporaryPassword};

_CreateTenantRequest _$CreateTenantRequestFromJson(Map<String, dynamic> json) =>
    _CreateTenantRequest(
      name: json['name'] as String,
      slug: json['slug'] as String,
    );

Map<String, dynamic> _$CreateTenantRequestToJson(
  _CreateTenantRequest instance,
) => <String, dynamic>{'name': instance.name, 'slug': instance.slug};

_UpdateTenantRequest _$UpdateTenantRequestFromJson(Map<String, dynamic> json) =>
    _UpdateTenantRequest(
      name: json['name'] as String,
      slug: json['slug'] as String,
    );

Map<String, dynamic> _$UpdateTenantRequestToJson(
  _UpdateTenantRequest instance,
) => <String, dynamic>{'name': instance.name, 'slug': instance.slug};

_UpdateUserRequest _$UpdateUserRequestFromJson(Map<String, dynamic> json) =>
    _UpdateUserRequest(
      username: json['username'] as String,
      roles:
          (json['roles'] as List<dynamic>?)?.map((e) => e as String).toList() ??
          const <String>[],
    );

Map<String, dynamic> _$UpdateUserRequestToJson(_UpdateUserRequest instance) =>
    <String, dynamic>{'username': instance.username, 'roles': instance.roles};

_CreateRoleRequest _$CreateRoleRequestFromJson(Map<String, dynamic> json) =>
    _CreateRoleRequest(
      code: json['code'] as String,
      scope: json['scope'] as String,
      permissions:
          (json['permissions'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          const <String>[],
    );

Map<String, dynamic> _$CreateRoleRequestToJson(_CreateRoleRequest instance) =>
    <String, dynamic>{
      'code': instance.code,
      'scope': instance.scope,
      'permissions': instance.permissions,
    };

_CreatePermissionRequest _$CreatePermissionRequestFromJson(
  Map<String, dynamic> json,
) => _CreatePermissionRequest(
  code: json['code'] as String,
  scope: json['scope'] as String,
);

Map<String, dynamic> _$CreatePermissionRequestToJson(
  _CreatePermissionRequest instance,
) => <String, dynamic>{'code': instance.code, 'scope': instance.scope};

_CreateUserRequest _$CreateUserRequestFromJson(Map<String, dynamic> json) =>
    _CreateUserRequest(
      username: json['username'] as String,
      tenantId: json['tenantId'] as String?,
      email: json['email'] as String?,
      temporaryPassword: json['temporaryPassword'] as String,
      roles:
          (json['roles'] as List<dynamic>?)?.map((e) => e as String).toList() ??
          const <String>[],
    );

Map<String, dynamic> _$CreateUserRequestToJson(_CreateUserRequest instance) =>
    <String, dynamic>{
      'username': instance.username,
      'tenantId': instance.tenantId,
      'email': instance.email,
      'temporaryPassword': instance.temporaryPassword,
      'roles': instance.roles,
    };
