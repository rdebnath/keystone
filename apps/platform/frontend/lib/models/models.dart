/// Immutable domain models parsed from the platform REST API (no raw maps leak into the UI).
library;

class Me {
  final String sub;
  final String? tenantId;
  final bool mustChangePassword;
  final List<String> permissions;

  const Me({
    required this.sub,
    this.tenantId,
    required this.mustChangePassword,
    required this.permissions,
  });

  factory Me.fromJson(Map<String, dynamic> json) => Me(
        sub: json['sub'] as String,
        tenantId: json['tenantId'] as String?,
        mustChangePassword: json['mustChangePassword'] as bool? ?? false,
        permissions: (json['permissions'] as List<dynamic>? ?? const []).cast<String>(),
      );
}

class Tenant {
  final String id;
  final String name;
  final String createdAt;
  final String updatedAt;

  const Tenant({
    required this.id,
    required this.name,
    required this.createdAt,
    required this.updatedAt,
  });

  factory Tenant.fromJson(Map<String, dynamic> json) => Tenant(
        id: json['id'] as String,
        name: json['name'] as String,
        createdAt: json['createdAt'] as String? ?? '',
        updatedAt: json['updatedAt'] as String? ?? '',
      );
}

class Role {
  final String id;
  final String code;
  final String scope;
  final List<String> permissions;
  final String createdAt;
  final String updatedAt;

  const Role({
    required this.id,
    required this.code,
    required this.scope,
    required this.permissions,
    required this.createdAt,
    required this.updatedAt,
  });

  factory Role.fromJson(Map<String, dynamic> json) => Role(
        id: json['id'] as String,
        code: json['code'] as String,
        scope: json['scope'] as String? ?? '',
        permissions: (json['permissions'] as List<dynamic>? ?? const []).cast<String>(),
        createdAt: json['createdAt'] as String? ?? '',
        updatedAt: json['updatedAt'] as String? ?? '',
      );
}

class Permission {
  final String id;
  final String code;
  final String scope;
  final String createdAt;
  final String updatedAt;

  const Permission({
    required this.id,
    required this.code,
    required this.scope,
    required this.createdAt,
    required this.updatedAt,
  });

  factory Permission.fromJson(Map<String, dynamic> json) => Permission(
        id: json['id'] as String,
        code: json['code'] as String,
        scope: json['scope'] as String? ?? '',
        createdAt: json['createdAt'] as String? ?? '',
        updatedAt: json['updatedAt'] as String? ?? '',
      );
}

class User {
  final String id;
  final String sub;
  final String email;
  final String? tenantId;
  final bool mustChangePassword;
  final List<String> roles;
  final String createdAt;
  final String updatedAt;

  const User({
    required this.id,
    required this.sub,
    required this.email,
    this.tenantId,
    required this.mustChangePassword,
    required this.roles,
    required this.createdAt,
    required this.updatedAt,
  });

  factory User.fromJson(Map<String, dynamic> json) => User(
        id: json['id'] as String,
        sub: json['sub'] as String,
        email: json['email'] as String,
        tenantId: json['tenantId'] as String?,
        mustChangePassword: json['mustChangePassword'] as bool? ?? false,
        roles: (json['roles'] as List<dynamic>? ?? const []).cast<String>(),
        createdAt: json['createdAt'] as String? ?? '',
        updatedAt: json['updatedAt'] as String? ?? '',
      );
}
