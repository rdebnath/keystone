import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../models/envelopes.dart';
import '../../models/models.dart';

/// The platform plane's **owner** filter for the roles and permissions lists: every owner, the global
/// catalog only, or one tenant.
///
/// The synthetic platform tenant is what the backend returns for "the platform plane", so selecting it
/// means the global catalog only. A tenant console has no owner to choose, so it does not render this at
/// all.
///
/// Both pickers here read the tenants' **unpaged options** list, never the paged one: a dropdown must offer
/// every choice, and a page would silently offer only the first (`docs/UX_GUIDELINES.md` §1.13).
class OwnerFilter extends ConsumerWidget {
  const OwnerFilter({super.key, required this.value, required this.onChanged});

  final String? value;
  final ValueChanged<String?> onChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tenants = _tenants(ref);
    return DropdownButtonFormField<String?>(
      initialValue: value,
      isExpanded: true,
      decoration: InputDecoration(
        labelText: 'Owner',
        helperText: _hint(
          tenants,
          'The global catalog is visible to every tenant',
        ),
      ),
      items: <DropdownMenuItem<String?>>[
        const DropdownMenuItem<String?>(value: null, child: Text('All owners')),
        ...tenants.items.map(
          (tenant) => DropdownMenuItem<String?>(
            value: tenant.id,
            child: Text(
              tenant.isPlatform
                  ? '${tenant.name} (global catalog)'
                  : tenant.name,
            ),
          ),
        ),
      ],
      onChanged: onChanged,
    );
  }
}

/// The platform plane's **tenant** filter for the users list: every tenant, or one.
class TenantFilter extends ConsumerWidget {
  const TenantFilter({super.key, required this.value, required this.onChanged});

  final String? value;
  final ValueChanged<String?> onChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tenants = _tenants(ref);
    return DropdownButtonFormField<String?>(
      initialValue: value,
      isExpanded: true,
      decoration: InputDecoration(
        labelText: 'Tenant',
        helperText: _hint(tenants, 'Keystone holds the platform users'),
      ),
      items: <DropdownMenuItem<String?>>[
        const DropdownMenuItem<String?>(
          value: null,
          child: Text('All tenants'),
        ),
        ...tenants.items.map(
          (tenant) => DropdownMenuItem<String?>(
            value: tenant.id,
            child: Text(
              tenant.isPlatform ? '${tenant.name} (platform)' : tenant.name,
            ),
          ),
        ),
      ],
      onChanged: onChanged,
    );
  }
}

/// The **scope** filter of the roles and permissions lists: every scope, `PLATFORM` (a cross-tenant
/// capability) or `TENANT` (within one customer).
///
/// Only the platform plane offers it: a tenant's own rows are always `TENANT` scope, so there is nothing
/// for a tenant console to choose.
class ScopeFilter extends StatelessWidget {
  const ScopeFilter({super.key, required this.value, required this.onChanged});

  final String? value;
  final ValueChanged<String?> onChanged;

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<String?>(
      initialValue: value,
      isExpanded: true,
      decoration: const InputDecoration(
        labelText: 'Scope',
        helperText: 'PLATFORM spans tenants; TENANT stays within one',
      ),
      items: const <DropdownMenuItem<String?>>[
        DropdownMenuItem<String?>(value: null, child: Text('All scopes')),
        DropdownMenuItem<String?>(value: 'PLATFORM', child: Text('PLATFORM')),
        DropdownMenuItem<String?>(value: 'TENANT', child: Text('TENANT')),
      ],
      onChanged: onChanged,
    );
  }
}

/// The **access-level** filter of the permissions lists: every level, `read-only` or `read/write`.
///
/// The level is the last segment of a permission code and every row shows it, so this is a filter whose
/// effect the user can see in the rows (`docs/UX_GUIDELINES.md` §1.6). The server matches it as a suffix on
/// the code, so a code that carries no level — the wildcard `*` — is in neither level's set.
///
/// Both planes offer it: the level is a property of the code, not of the owner that defined it.
class AccessFilter extends StatelessWidget {
  const AccessFilter({super.key, required this.value, required this.onChanged});

  /// The `access` parameter the server accepts (`PermissionAccess.suffix`), or null for every level.
  final String? value;
  final ValueChanged<String?> onChanged;

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<String?>(
      initialValue: value,
      isExpanded: true,
      decoration: const InputDecoration(
        labelText: 'Level',
        helperText: 'The level a code ends in',
      ),
      items: <DropdownMenuItem<String?>>[
        const DropdownMenuItem<String?>(value: null, child: Text('All levels')),
        ...PermissionAccess.values.map(
          (access) => DropdownMenuItem<String?>(
            value: access.suffix,
            child: Text(access.label),
          ),
        ),
      ],
      onChanged: onChanged,
    );
  }
}

/// The tenants a picker offers: empty while the options list is loading — or when the caller may not read
/// tenants at all, in which case the dropdown shows only its "all" entry, the same graceful degradation the
/// owner labels already rely on.
OptionList<Tenant> _tenants(WidgetRef ref) =>
    ref.watch(tenantOptionsProvider).valueOrNull ??
    const OptionList<Tenant>(items: <Tenant>[], truncated: false);

/// Explains the picker, and says so honestly when the backend's cap cut the set short rather than
/// presenting a truncated list as complete.
String _hint(OptionList<Tenant> tenants, String base) => tenants.truncated
    ? 'Showing the first ${tenants.items.length} tenants — more exist'
    : base;

