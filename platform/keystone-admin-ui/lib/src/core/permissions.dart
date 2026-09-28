/// The platform-defined permission catalog as the console sees it.
///
/// A permission code is `<scope>:<resource>:<level>` and the catalog is seeded by the backend
/// (`PermissionCatalog`), so the resource namespaces and the two access levels are mirrored here — the
/// console composes and checks codes, it never invents them
/// (`docs/ARCHITECTURE.md` §9.3, `docs/CODING_GUIDELINES_FRONTEND.md` §2).
library;

import '../models/models.dart';

/// The resource namespaces of the platform plane: the `resource` part of a `platform:<resource>:<level>`
/// code. A section of the console is rendered only when the caller holds one of these levels.
abstract final class PlatformResource {
  static const String tenant = 'platform:tenant';
  static const String role = 'platform:role';
  static const String permission = 'platform:permission';
  static const String user = 'platform:user';
}

/// The resource namespaces of the **tenant self-service** plane: what a tenant administers about
/// itself, and the resources the tenant console's sections are gated on.
abstract final class TenantResource {
  static const String user = 'tenant:user';
  static const String role = 'tenant:role';
  static const String permission = 'tenant:permission';
}

/// The **seeded administrative role** codes, mirrored from the backend's `PermissionCatalog` so the console
/// can tell an immutable role from an editable one without asking the server.
abstract final class SeededRole {
  /// The global role that holds the wildcard, seeded by the platform bootstrap.
  static const String platformAdmin = 'platform-admin';

  /// The role every tenant is seeded with, holding its own plane's read/write grants.
  static const String tenantAdmin = 'admin';
}

/// Whether [role] is one of the **seeded administrative roles** — the platform's global `platform-admin`, or
/// a tenant's own `admin`.
///
/// Both are immutable server-side (`PermissionCatalog.isSeededAdminRole`): "without this a single role-write
/// holder could delete the only role that grants them back in". The console therefore offers no *Edit*
/// action for them at all, instead of letting a user compose a change the backend will refuse with a `403`.
bool isSeededAdminRole(Role role) => role.isGlobal
    ? role.code == SeededRole.platformAdmin
    : role.code == SeededRole.tenantAdmin;

/// One catalog resource a permission can be created for. The access level is chosen separately (a code
/// is `<resource>:<level>`), so the scope is derived from the resource namespace rather than chosen by
/// the user — which is what keeps the screen from composing an invalid `scope` + `code` pair.
class CatalogResource {
  const CatalogResource(this.code, this.scope);

  final String code;
  final String scope;

  /// The code that grants [access] on this resource.
  String codeFor(PermissionAccess access) => '$code:${access.suffix}';
}

const List<CatalogResource> catalogResources = <CatalogResource>[
  CatalogResource(PlatformResource.tenant, 'PLATFORM'),
  CatalogResource(PlatformResource.role, 'PLATFORM'),
  CatalogResource(PlatformResource.permission, 'PLATFORM'),
  CatalogResource(PlatformResource.user, 'PLATFORM'),
  CatalogResource('tenant:role', 'TENANT'),
  CatalogResource('tenant:permission', 'TENANT'),
  CatalogResource('tenant:user', 'TENANT'),
];
