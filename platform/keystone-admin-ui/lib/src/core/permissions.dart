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
