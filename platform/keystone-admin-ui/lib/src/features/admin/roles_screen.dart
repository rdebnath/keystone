import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/console.dart';
import '../../core/errors.dart';
import '../../core/lists.dart';
import '../../core/permission_selection.dart';
import '../../core/permissions.dart';
import '../../core/providers.dart';
import '../../models/list_query.dart';
import '../../models/models.dart';
import '../../models/requests.dart';
import 'admin_shell.dart';
import 'owner_filter.dart';
import 'permission_picker.dart';

/// The roles the current console can see.
///
/// One widget serves both planes. On the platform plane a tenant selector narrows the list (the global
/// catalog plus one tenant's own) and a new role may be global; on a tenant's own plane the selector is
/// absent — there is nothing to choose — and a new role always belongs to the caller's tenant.
class RolesScreen extends ConsumerStatefulWidget {
  const RolesScreen({super.key});

  @override
  ConsumerState<RolesScreen> createState() => _RolesScreenState();
}

class _RolesScreenState extends ConsumerState<RolesScreen> {
  /// The list state — search, owner filter, scope filter, page, size and sort — kept in the URL (§1.12).
  ListQuery _query = ListQuery.initial;
  bool _restored = false;

  /// The keys the roles list accepts (`docs/CODING_GUIDELINES_BACKEND.md` §8); the first is the server's
  /// default order — the global catalog first, then each tenant's rows, each by code.
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
      ? AdminRoutes.roles
      : AdminRoutes.tenantRoles;

  @override
  Widget build(BuildContext context) {
    final console = ref.watch(consoleProvider);
    final canManage =
        ref.watch(meProvider).valueOrNull?.canWrite(console.roleResource) ??
        false;
    final roles = ref.watch(rolesPageProvider(_query));
    return Scaffold(
      floatingActionButton: canManage
          ? FloatingActionButton.extended(
              onPressed: () => _edit(context, null),
              icon: const Icon(Icons.add),
              label: const Text('Add role'),
            )
          : null,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          ListToolbar(
            search: SearchField(
              value: _query.search,
              hintText: 'Search role code',
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
            ],
            trailing: SortSelect(
              query: _query,
              onQueryChanged: _update,
              options: _sortOptions,
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: PagedListView<Role>(
              value: roles,
              query: _query,
              onQueryChanged: _update,
              onRetry: () => ref.invalidate(rolesPageProvider(_query)),
              emptyIcon: Icons.workspace_premium_outlined,
              emptyMessage: 'No roles yet.',
              itemBuilder: (context, role) => _RoleTile(
                role: role,
                owner: _owner(ref, role),
                // A seeded administrative role is immutable, and a read-only plane has no write affordance:
                // neither is offered an action the backend would refuse.
                canEdit: canManage && !isSeededAdminRole(role),
                onEdit: () => _edit(context, role),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// The owner of [role], named for the platform plane: "Global" for the catalog, the tenant's name for
  /// a role a tenant defined. On the tenant plane every owned role is the caller's own.
  String _owner(WidgetRef ref, Role role) {
    if (role.isGlobal) {
      return 'Global';
    }
    return ref.watch(tenantByIdProvider(role.tenantId!))?.name ?? 'Tenant role';
  }

  /// Opens the role editor — creating a role ([existing] is null) or changing one — and reports the outcome.
  ///
  /// The dialog performs the API call, so a failure leaves it open with the server's own message; this only
  /// reports what happened.
  Future<void> _edit(BuildContext context, Role? existing) async {
    final saved = await showRoleEditor(context, existing: existing);
    if (saved && context.mounted) {
      showApiSuccess(
        context,
        existing == null ? 'Role created.' : 'Role saved.',
      );
    }
  }
}

/// The roles screen's owner filter lives in `owner_filter.dart`, shared with the permissions screen.

class _RoleTile extends StatelessWidget {
  const _RoleTile({
    required this.role,
    required this.owner,
    required this.canEdit,
    required this.onEdit,
  });

  final Role role;
  final String owner;

  /// Whether the row offers *Edit*. Two things withhold it: a plane the caller may not write, and a **seeded
  /// administrative role**, which the backend refuses to change at all (`PermissionCatalog.isSeededAdminRole`
  /// — "without this a single role-write holder could delete the only role that grants them back in").
  final bool canEdit;
  final VoidCallback onEdit;

  @override
  Widget build(BuildContext context) {
    final permissions = role.permissions.isEmpty
        ? 'no permissions'
        : role.permissions.join(', ');
    final theme = Theme.of(context);
    return ListTile(
      leading: Icon(
        role.isGlobal ? Icons.public : Icons.apartment_outlined,
        color: role.isGlobal
            ? theme.colorScheme.primary
            : theme.colorScheme.outline,
      ),
      title: Text(role.code),
      subtitle: Text('${role.scope} · $owner · $permissions'),
      trailing: canEdit
          ? IconButton(
              icon: const Icon(Icons.edit_outlined),
              tooltip: 'Edit role',
              onPressed: onEdit,
            )
          : null,
    );
  }
}

/// Opens the create/edit-role dialog and returns whether a change was saved.
///
/// Pass [existing] to change a role's code, scope and grants, or leave it null to create one. The dialog
/// performs the API call itself and drops the cached role lists on success — the same shape as
/// `showUserEditor`.
Future<bool> showRoleEditor(BuildContext context, {Role? existing}) async {
  final saved = await showDialog<bool>(
    context: context,
    builder: (_) => _RoleEditorDialog(existing: existing),
  );
  return saved ?? false;
}

/// The create/edit-role dialog.
///
/// The **owner** and the **scope** are choices for a *new* role on the platform plane: a tenant-owned role is
/// always `TENANT` scope (the backend pins it — `RoleService.requireOwnableScope`) and a tenant console has no
/// owner to choose, because it is the caller's tenant. Editing reuses the dialog, where a role's owner is
/// immutable and a tenant-owned role's scope is fixed — both are *stated* rather than offered, and the scope
/// is offered only while the role is global.
///
/// Permissions are **picked, never typed**: the field opens the browsable catalogue (`showPermissionPicker`),
/// and whatever comes back is pruned whenever the owner or the scope changes, so the dialog cannot send a
/// pair the service would refuse.
class _RoleEditorDialog extends ConsumerStatefulWidget {
  const _RoleEditorDialog({this.existing});

  /// The role being changed, or null to create one.
  final Role? existing;

  @override
  ConsumerState<_RoleEditorDialog> createState() => _RoleEditorDialogState();
}

class _RoleEditorDialogState extends ConsumerState<_RoleEditorDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _code = TextEditingController(
    text: widget.existing?.code ?? '',
  );

  /// The grants: what the role already holds when editing, nothing when creating.
  late PermissionSelection _selection = widget.existing == null
      ? PermissionSelection.empty
      : PermissionSelection.ofRole(widget.existing!);

  /// The codes the last owner/scope change dropped, so the dialog says so instead of quietly forgetting them.
  List<String> _dropped = const <String>[];

  late String? _tenantId = widget.existing?.tenantId;
  late String _scope = widget.existing?.scope ?? 'PLATFORM';
  bool _saving = false;

  /// The plane this dialog acts in. It is fixed for the session (`/me` decides it), so it is read once
  /// rather than threaded through every getter below.
  late final bool _platformPlane = ref.read(consoleProvider).isPlatformPlane;

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  bool get _creating => widget.existing == null;

  /// Whether the scope is a choice: never on the tenant plane, never for a tenant-owned role, and never for a
  /// new role whose owner is a tenant.
  bool get _scopeChoosable => _platformPlane
      ? (_creating ? _tenantId == null : widget.existing!.isGlobal)
      : false;

  /// The scope the role will carry — the chosen one, or the only one a tenant-owned role may have.
  String get _scopeOf => _scopeChoosable ? _scope : 'TENANT';

  /// The role's owner: the chosen tenant, or null for a global role. A tenant console never names one — the
  /// backend derives it from the caller, and `ConsoleScope` drops it from the picker's query too.
  String? get _ownerId => _platformPlane ? _tenantId : null;

  @override
  Widget build(BuildContext context) {
    final tenants = _platformPlane
        ? ref.watch(tenantOptionsProvider).valueOrNull?.items ?? const <Tenant>[]
        : const <Tenant>[];
    final console = ref.read(consoleProvider);
    // Reading the catalogue is the *permission* resource's read grant, which the Roles screen itself is not
    // gated on: the affordance is withheld when it is missing, instead of opening a picker that must fail.
    final me = ref.watch(meProvider).valueOrNull;
    final canReadCatalogue =
        me?.allowsResource(console.permissionResource) ?? false;
    return AlertDialog(
      title: Text(_creating ? 'Create role' : 'Edit role'),
      content: SizedBox(
        width: 460,
        child: SingleChildScrollView(
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                TextFormField(
                  controller: _code,
                  autofocus: _creating,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(labelText: 'Code'),
                  validator: (value) =>
                      value == null || value.trim().isEmpty ? 'Required' : null,
                ),
                const SizedBox(height: 12),
                if (_platformPlane) ...<Widget>[
                  if (_creating)
                    _ownerPicker(tenants)
                  else
                    _ownerStatement(tenants),
                  const SizedBox(height: 12),
                  if (_scopeChoosable) _scopePicker() else _scopeStatement(),
                ] else
                  _tenantOwnershipNote(),
                const SizedBox(height: 12),
                _permissionsField(canReadCatalogue, console),
                if (_dropped.isNotEmpty) _droppedNote(),
              ],
            ),
          ),
        ),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: _saving ? null : () => Navigator.pop(context, false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _saving ? null : _save,
          child: Text(_creating ? 'Create' : 'Save'),
        ),
      ],
    );
  }

  /// The platform plane's owner picker — a new role only. Choosing a tenant fixes the scope to `TENANT` in
  /// the same gesture, because a tenant-owned role may not carry a cross-tenant capability and offering the
  /// choice would only produce a refusal.
  Widget _ownerPicker(List<Tenant> tenants) {
    return InputDecorator(
      decoration: const InputDecoration(
        labelText: 'Owner',
        helperText: 'Global roles are usable by every tenant',
      ),
      child: DropdownButton<String?>(
        value: _tenantId,
        isExpanded: true,
        underline: const SizedBox.shrink(),
        items: <DropdownMenuItem<String?>>[
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
        onChanged: (value) => setState(() {
          _tenantId = value;
          if (value != null) {
            _scope = 'TENANT';
          }
          _prune();
        }),
      ),
    );
  }

  /// The owner, named, when it is not a choice: an existing role's owner never changes.
  Widget _ownerStatement(List<Tenant> tenants) {
    final ownerId = widget.existing?.tenantId;
    return _ReadOnlyField(
      label: 'Owner',
      value: ownerId == null
          ? 'Global (platform-defined)'
          : _tenantName(tenants, ownerId),
      hint: 'A role\'s owner never changes',
    );
  }

  String _tenantName(List<Tenant> tenants, String tenantId) {
    for (final tenant in tenants) {
      if (tenant.id == tenantId) {
        return tenant.name;
      }
    }
    return 'Tenant role';
  }

  /// The scope picker: `PLATFORM` (a cross-tenant capability) or `TENANT` (within one customer).
  Widget _scopePicker() {
    return InputDecorator(
      decoration: const InputDecoration(
        labelText: 'Scope',
        helperText: 'PLATFORM spans tenants; TENANT stays within one',
      ),
      child: DropdownButton<String>(
        value: _scope,
        isExpanded: true,
        underline: const SizedBox.shrink(),
        items: const <String>['PLATFORM', 'TENANT']
            .map(
              (scope) =>
                  DropdownMenuItem<String>(value: scope, child: Text(scope)),
            )
            .toList(growable: false),
        onChanged: (value) => setState(() {
          _scope = value ?? 'PLATFORM';
          _prune();
        }),
      ),
    );
  }

  /// The scope, stated rather than offered: a tenant-owned role is `TENANT` scope, by the backend's own rule.
  Widget _scopeStatement() {
    return const _ReadOnlyField(
      label: 'Scope',
      value: 'TENANT',
      hint: 'A tenant-owned role cannot be PLATFORM scope',
    );
  }

  /// The tenant plane: the owner and the scope both come from the caller, so there is nothing to choose —
  /// only to state.
  Widget _tenantOwnershipNote() {
    return const Align(
      alignment: Alignment.centerLeft,
      child: Padding(
        padding: EdgeInsets.symmetric(vertical: 8),
        child: Text('Owned by your tenant · TENANT scope'),
      ),
    );
  }

  /// The permissions field: **what is picked**, and the way to change it. Nothing is typed — the catalogue
  /// is browsed, and the codes shown here are what the picker returned, pruned to the current owner and
  /// scope.
  Widget _permissionsField(bool canReadCatalogue, ConsoleScope console) {
    final count = _selection.length;
    final chosen = _selection.codes;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        InputDecorator(
          decoration: InputDecoration(
            labelText: 'Permissions',
            helperText: canReadCatalogue
                ? 'Chosen from the catalogue — only permissions this owner '
                      'and scope can use are offered'
                : 'You need ${console.permissionResource}:read-only (or read/write) '
                      'to choose permissions',
          ),
          child: Text(
            count == 0
                ? 'No permissions selected'
                : '$count selected · ${chosen.join(', ')}',
            maxLines: 4,
            overflow: TextOverflow.ellipsis,
          ),
        ),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            onPressed: canReadCatalogue && !_saving ? _choosePermissions : null,
            icon: const Icon(Icons.playlist_add_check),
            label: Text(
              count == 0 ? 'Choose permissions' : 'Change permissions ($count)',
            ),
          ),
        ),
      ],
    );
  }

  /// Says what an owner or scope change removed, rather than letting the user meet it as a `422` on save.
  Widget _droppedNote() {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Text(
        '${_dropped.length} permission(s) removed — they do not match the new '
        'owner or scope: ${_dropped.join(', ')}',
        style: theme.textTheme.bodySmall?.copyWith(
          color: theme.colorScheme.error,
        ),
      ),
    );
  }

  /// Opens the browsable catalogue and takes back what was picked — nothing at all on cancel.
  Future<void> _choosePermissions() async {
    final picked = await showPermissionPicker(
      context,
      ownerId: _ownerId,
      scope: _scopeOf,
      selected: _selection,
    );
    if (picked == null || !mounted) {
      return;
    }
    setState(() {
      _selection = picked;
      _dropped = const <String>[];
    });
  }

  /// Applies the backend's own grant rule locally: a pick whose scope or owner no longer matches the role is
  /// dropped, and **named**, so the dialog can never send what the service would refuse.
  void _prune() {
    final result = _selection.retaining(ownerId: _ownerId, scope: _scopeOf);
    _selection = result.selection;
    _dropped = result.dropped;
  }

  /// Creates the role, or replaces its code, scope and grants — a role's owner is never part of the request.
  Future<void> _save() async {
    if (_saving || !_formKey.currentState!.validate()) {
      return;
    }
    setState(() => _saving = true);
    final console = ref.read(consoleProvider);
    final existing = widget.existing;
    try {
      if (existing == null) {
        await console.createRole(
          CreateRoleRequest(
            code: _code.text.trim(),
            scope: _scopeOf,
            tenantId: _ownerId,
            permissions: _selection.codes,
          ),
        );
      } else {
        await console.updateRole(
          existing.id,
          UpdateRoleRequest(
            code: _code.text.trim(),
            scope: _scopeOf,
            permissions: _selection.codes,
          ),
        );
      }
      ref.invalidate(rolesPageProvider);
      if (mounted) {
        Navigator.pop(context, true);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _saving = false);
        showApiError(
          context,
          error,
          existing == null
              ? 'Could not create the role.'
              : 'Could not save the role.',
        );
      }
    }
  }
}

/// A read-only field: the value plus why it cannot be changed — how the dialog *states* the owner and the
/// scope it does not offer.
class _ReadOnlyField extends StatelessWidget {
  const _ReadOnlyField({required this.label, required this.value, this.hint});

  final String label;
  final String value;
  final String? hint;

  @override
  Widget build(BuildContext context) {
    return InputDecorator(
      decoration: InputDecoration(labelText: label, helperText: hint),
      child: Text(value),
    );
  }
}
