// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_Me _$MeFromJson(Map<String, dynamic> json) => _Me(
  sub: json['sub'] as String,
  username: json['username'] as String? ?? '',
  tenantId: json['tenantId'] as String?,
  mustChangePassword: json['mustChangePassword'] as bool? ?? false,
  permissions:
      (json['permissions'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList() ??
      const <String>[],
);

Map<String, dynamic> _$MeToJson(_Me instance) => <String, dynamic>{
  'sub': instance.sub,
  'username': instance.username,
  'tenantId': instance.tenantId,
  'mustChangePassword': instance.mustChangePassword,
  'permissions': instance.permissions,
};

_Tenant _$TenantFromJson(Map<String, dynamic> json) => _Tenant(
  id: json['id'] as String,
  name: json['name'] as String,
  slug: json['slug'] as String? ?? '',
  country: json['country'] as String?,
  platform: json['platform'] as bool? ?? false,
  createdAt: json['createdAt'] as String? ?? '',
  updatedAt: json['updatedAt'] as String? ?? '',
);

Map<String, dynamic> _$TenantToJson(_Tenant instance) => <String, dynamic>{
  'id': instance.id,
  'name': instance.name,
  'slug': instance.slug,
  'country': instance.country,
  'platform': instance.platform,
  'createdAt': instance.createdAt,
  'updatedAt': instance.updatedAt,
};

_Role _$RoleFromJson(Map<String, dynamic> json) => _Role(
  id: json['id'] as String,
  code: json['code'] as String,
  scope: json['scope'] as String? ?? '',
  tenantId: json['tenantId'] as String?,
  permissions:
      (json['permissions'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList() ??
      const <String>[],
  createdAt: json['createdAt'] as String? ?? '',
  updatedAt: json['updatedAt'] as String? ?? '',
);

Map<String, dynamic> _$RoleToJson(_Role instance) => <String, dynamic>{
  'id': instance.id,
  'code': instance.code,
  'scope': instance.scope,
  'tenantId': instance.tenantId,
  'permissions': instance.permissions,
  'createdAt': instance.createdAt,
  'updatedAt': instance.updatedAt,
};

_Permission _$PermissionFromJson(Map<String, dynamic> json) => _Permission(
  id: json['id'] as String,
  code: json['code'] as String,
  scope: json['scope'] as String? ?? '',
  tenantId: json['tenantId'] as String?,
  createdAt: json['createdAt'] as String? ?? '',
  updatedAt: json['updatedAt'] as String? ?? '',
);

Map<String, dynamic> _$PermissionToJson(_Permission instance) =>
    <String, dynamic>{
      'id': instance.id,
      'code': instance.code,
      'scope': instance.scope,
      'tenantId': instance.tenantId,
      'createdAt': instance.createdAt,
      'updatedAt': instance.updatedAt,
    };

_User _$UserFromJson(Map<String, dynamic> json) => _User(
  id: json['id'] as String,
  sub: json['sub'] as String,
  username: json['username'] as String? ?? '',
  email: json['email'] as String,
  phoneNumber: json['phoneNumber'] as String?,
  tenantId: json['tenantId'] as String?,
  mustChangePassword: json['mustChangePassword'] as bool? ?? false,
  roles:
      (json['roles'] as List<dynamic>?)?.map((e) => e as String).toList() ??
      const <String>[],
  createdAt: json['createdAt'] as String? ?? '',
  updatedAt: json['updatedAt'] as String? ?? '',
);

Map<String, dynamic> _$UserToJson(_User instance) => <String, dynamic>{
  'id': instance.id,
  'sub': instance.sub,
  'username': instance.username,
  'email': instance.email,
  'phoneNumber': instance.phoneNumber,
  'tenantId': instance.tenantId,
  'mustChangePassword': instance.mustChangePassword,
  'roles': instance.roles,
  'createdAt': instance.createdAt,
  'updatedAt': instance.updatedAt,
};

_Session _$SessionFromJson(Map<String, dynamic> json) => _Session(
  accessToken: json['accessToken'] as String,
  refreshToken: json['refreshToken'] as String? ?? '',
  tokenType: json['tokenType'] as String? ?? 'bearer',
  expiresIn: (json['expiresIn'] as num?)?.toInt() ?? 0,
);

Map<String, dynamic> _$SessionToJson(_Session instance) => <String, dynamic>{
  'accessToken': instance.accessToken,
  'refreshToken': instance.refreshToken,
  'tokenType': instance.tokenType,
  'expiresIn': instance.expiresIn,
};
