import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/console.dart';
import '../../core/errors.dart';
import '../../core/lists.dart';
import '../../core/permissions.dart';
import '../../core/providers.dart';
import '../../models/list_query.dart';
import '../../models/models.dart';
import '../../models/requests.dart';
import 'admin_shell.dart';
import 'owner_filter.dart';
import 'permission_row.dart';

/// The permission catalog the current console can see.
///
/// One widget serves both planes: the platform plane may filter by owner and add a global catalog
/// permission, while a tenant console sees the global catalog plus what it defined itself and adds rows
/// owned by its own tenant only.
class PermissionsScreen extends ConsumerStatefulWidget {
  const PermissionsScreen({super.key});

  @override
  ConsumerState<PermissionsScreen> createState() => _PermissionsScreenState();
}

class _PermissionsScreenState extends ConsumerState<PermissionsScreen> {
  /// The list state — search, owner filter, scope filter, page, size and sort — kept in the URL (§1.12),
  /// so a filtered catalog survives a refresh and can be shared.
  ListQuery _query = ListQuery.initial;
  bool _restored = false;

  /// The keys the permissions list accepts (`docs/CODING_GUIDELINES_BACKEND.md` §8); the first is the
  /// server's default order — the global catalog first, then each tenant's rows, each by code.
  static const List<SortOption> _sortOptions = <SortOption>[
    SortOption(null, 'Catalog first, then code (default)'),
    SortOption('code', 'Code'),
    SortOption('createdAt', 'Created'),
    SortOption('updatedAt', 'Updated'),
  ];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_restored) {
      _restored = true;
      _query = ListQueryLocation.read(context);
    }
  }

  void _update(ListQuery query) {
    setState(() => _query = query);
    ListQueryLocation.write(context, _path, query);
  }

  /// The route this screen is showing, so the URL keeps the user on the same plane.
  String get _path => ref.read(consoleProvider).isPlatformPlane
      ? AdminRoutes.permissions
      : AdminRoutes.tenantPermissions;

  @override
  Widget build(BuildContext context) {
    final console = ref.watch(consoleProvider);
    final canManage =
        ref
            .watch(meProvider)
            .valueOrNull
            ?.canWrite(console.permissionResource) ??
        false;
    final permissions = ref.watch(permissionsPageProvider(_query));
    return Scaffold(
      floatingActionButton: canManage
          ? FloatingActionButton.extended(
              onPressed: () => _create(context, ref),
              icon: const Icon(Icons.add),
              label: const Text('Add permission'),
            )
          : null,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          ListToolbar(
            search: SearchField(
              value: _query.search,
              hintText: 'Search permission code',
              onChanged: (value) => _update(_query.withSearch(value)),
            ),
            filters: <Widget>[
              if (console.isPlatformPlane) ...<Widget>[
                SizedBox(
                  width: 260,
                  child: OwnerFilter(
                    value: _query.tenantId,
                    onChanged: (tenantId) =>
                        _update(_query.withTenant(tenantId)),
                  ),
                ),
                SizedBox(
                  width: 180,
                  child: ScopeFilter(
                    value: _query.scope,
                    onChanged: (scope) => _update(_query.withScope(scope)),
                  ),
                ),
              ],
              // The level a code carries is a property of the code, not of the owner — so both planes offer
              // it, and every row shows it.
              SizedBox(
                width: 180,
                child: AccessFilter(
                  value: _query.access,
                  onChanged: (access) => _update(_query.withAccess(access)),
                ),
              ),
            ],
            trailing: SortSelect(
              query: _query,
              onQueryChanged: _update,
              options: _sortOptions,
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: PagedListView<Permission>(
              value: permissions,
              query: _query,
              onQueryChanged: _update,
              onRetry: () => ref.invalidate(permissionsPageProvider(_query)),
              emptyIcon: Icons.key_outlined,
              emptyMessage: 'No permissions yet.',
              itemBuilder: (context, permission) =>
                  PermissionRow(permission: permission),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _create(BuildContext context, WidgetRef ref) async {
    final console = ref.read(consoleProvider);
    final request = await showDialog<CreatePermissionRequest>(
      context: context,
      builder: (_) => _CreatePermissionDialog(console: console),
    );
    if (request == null) {
      return;
    }
    try {
      await ref.read(consoleProvider).createPermission(request);
      ref.invalidate(permissionsPageProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Permission created.')));
      }
    } catch (error) {
      if (context.mounted) {
        showApiError(context, error, 'Could not create the permission.');
      }
    }
  }
}

/// The create-permission dialog. Only the plane's own resources are offered — a tenant cannot define a
/// cross-tenant (`platform:*`) capability — and the owner is chosen only on the platform plane, since a
/// tenant console always defines for itself.
class _CreatePermissionDialog extends ConsumerStatefulWidget {
  const _CreatePermissionDialog({required this.console});

  final ConsoleScope console;

  @override
  ConsumerState<_CreatePermissionDialog> createState() =>
      _CreatePermissionDialogState();
}

class _CreatePermissionDialogState
    extends ConsumerState<_CreatePermissionDialog> {
  late final List<CatalogResource> _resources = catalogResources
      .where(
        (resource) => widget.console.isPlatformPlane
            ? resource.scope == 'PLATFORM'
            : resource.scope == 'TENANT',
      )
      .toList(growable: false);
  late CatalogResource _resource = _resources.first;
  PermissionAccess _access = PermissionAccess.readOnly;
  String? _tenantId;

  @override
  Widget build(BuildContext context) {
    final platformPlane = widget.console.isPlatformPlane;
    final tenants = platformPlane
        ? ref.watch(tenantOptionsProvider).valueOrNull?.items ??
              const <Tenant>[]
        : const <Tenant>[];
    return AlertDialog(
      title: const Text('Create permission'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _ResourceField(
              resources: _resources,
              value: _resource,
              onChanged: (value) => setState(() => _resource = value),
            ),
            if (platformPlane)
              InputDecorator(
                decoration: const InputDecoration(
                  labelText: 'Owner',
                  helperText: 'Global permissions are usable by every tenant',
                ),
                child: DropdownButton<String?>(
                  value: _tenantId,
                  isExpanded: true,
                  underline: const SizedBox.shrink(),
                  items: [
                    const DropdownMenuItem<String?>(
                      value: null,
                      child: Text('Global (platform-defined)'),
                    ),
                    ...tenants
                        .where((tenant) => !tenant.isPlatform)
                        .map(
                          (tenant) => DropdownMenuItem<String?>(
                            value: tenant.id,
                            child: Text(tenant.name),
                          ),
                        ),
                  ],
                  onChanged: (value) => setState(() => _tenantId = value),
                ),
              ),
            InputDecorator(
              decoration: const InputDecoration(labelText: 'Type'),
              child: DropdownButton<PermissionAccess>(
                value: _access,
                isExpanded: true,
                underline: const SizedBox.shrink(),
                items: PermissionAccess.values
                    .map(
                      (access) => DropdownMenuItem(
                        value: access,
                        child: Text(access.label),
                      ),
                    )
                    .toList(),
                onChanged: (value) => setState(() {
                  _access = value ?? PermissionAccess.readOnly;
                }),
              ),
            ),
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerLeft,
              child: Text('Code: ${_resource.codeFor(_access)}'),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(
            context,
            CreatePermissionRequest(
              code: _resource.codeFor(_access),
              scope: _resource.scope,
              tenantId: platformPlane ? _tenantId : null,
            ),
          ),
          child: const Text('Create'),
        ),
      ],
    );
  }
}

/// The catalog resource picker: the code namespace a permission is created in.
class _ResourceField extends StatelessWidget {
  const _ResourceField({
    required this.resources,
    required this.value,
    required this.onChanged,
  });

  final List<CatalogResource> resources;
  final CatalogResource value;
  final ValueChanged<CatalogResource> onChanged;

  @override
  Widget build(BuildContext context) {
    return InputDecorator(
      decoration: const InputDecoration(labelText: 'Resource'),
      child: DropdownButton<String>(
        value: value.code,
        isExpanded: true,
        underline: const SizedBox.shrink(),
        items: resources
            .map(
              (resource) => DropdownMenuItem(
                value: resource.code,
                child: Text(resource.code),
              ),
            )
            .toList(),
        onChanged: (code) => onChanged(
          resources.firstWhere(
            (resource) => resource.code == code,
            orElse: () => resources.first,
          ),
        ),
      ),
    );
  }
}
