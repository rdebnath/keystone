import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/dialogs.dart';
import '../../core/errors.dart';
import '../../core/panels.dart';
import '../../core/password_field.dart';
import '../../core/providers.dart';
import '../../models/models.dart';
import '../../models/requests.dart';

/// The users of one tenant, with edit and delete actions when [canManage]. Pass the reserved platform
/// tenant id to list the platform users, or null for every user.
class UserList extends ConsumerWidget {
  const UserList({
    super.key,
    required this.tenantId,
    required this.canManage,
    this.showTenant = false,
  });

  final String? tenantId;
  final bool canManage;

  /// Shows the user's tenant in the row subtitle — useful when the list spans tenants.
  final bool showTenant;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final users = ref.watch(usersProvider(tenantId));
    final tenants = ref.watch(tenantsProvider).valueOrNull ?? const <Tenant>[];
    return users.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => MessagePanel(
        icon: Icons.error_outline,
        message: apiErrorMessage(error, 'Failed to load users.'),
        actionLabel: 'Retry',
        onAction: () => ref.invalidate(usersProvider),
      ),
      data: (items) => items.isEmpty
          ? const MessagePanel(
              icon: Icons.people_outline,
              message: 'No users yet.',
            )
          : ListView.builder(
              itemCount: items.length,
              itemBuilder: (_, i) => _UserTile(
                user: items[i],
                canManage: canManage,
                tenantLabel: showTenant
                    ? _tenantName(tenants, items[i].tenantId)
                    : null,
                onEdit: () => showUserEditor(context, existing: items[i]),
                onResetPassword: () =>
                    showResetPasswordDialog(context, items[i]),
                onDelete: () => _delete(context, ref, items[i]),
              ),
            ),
    );
  }

  Future<void> _delete(BuildContext context, WidgetRef ref, User user) async {
    final confirmed = await confirmDialog(
      context,
      title: 'Delete user',
      message:
          'Delete "${user.username}" (${user.email})? '
          'They lose access immediately. This cannot be undone.',
    );
    if (!confirmed) {
      return;
    }
    try {
      await ref.read(apiClientProvider).deleteUser(user.id);
      ref.invalidate(usersProvider);
      if (context.mounted) {
        showApiSuccess(context, 'User deleted.');
      }
    } catch (error) {
      if (context.mounted) {
        showApiError(context, error, 'Could not delete the user.');
      }
    }
  }
}

class _UserTile extends StatelessWidget {
  const _UserTile({
    required this.user,
    required this.canManage,
    required this.onEdit,
    required this.onResetPassword,
    required this.onDelete,
    this.tenantLabel,
  });

  final User user;
  final bool canManage;
  final VoidCallback onEdit;
  final VoidCallback onResetPassword;
  final VoidCallback onDelete;
  final String? tenantLabel;

  @override
  Widget build(BuildContext context) {
    final roles = user.roles.isEmpty ? 'no roles' : user.roles.join(', ');
    final subtitle = [
      if (tenantLabel != null) tenantLabel!,
      roles,
      if (user.mustChangePassword) 'must change password',
    ].join(' · ');
    return ListTile(
      leading: const CircleAvatar(child: Icon(Icons.person_outline)),
      title: Text('${user.username} (${user.email})'),
      subtitle: Text(subtitle),
      trailing: canManage
          ? Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  icon: const Icon(Icons.key_outlined),
                  tooltip: 'Reset password',
                  onPressed: onResetPassword,
                ),
                IconButton(
                  icon: const Icon(Icons.edit_outlined),
                  tooltip: 'Edit roles',
                  onPressed: onEdit,
                ),
                IconButton(
                  icon: const Icon(Icons.delete_outline),
                  tooltip: 'Delete user',
                  onPressed: onDelete,
                ),
              ],
            )
          : null,
    );
  }
}

/// The name of the tenant a user belongs to: the platform tenant row for a user without a tenant.
String _tenantName(List<Tenant> tenants, String? tenantId) {
  final tenant = _userTenant(tenants, tenantId);
  return tenant?.name ?? 'Unknown tenant';
}

/// The tenant row carrying [id] (the reserved platform id included), or null when it is unknown.
Tenant? _tenantRow(List<Tenant> tenants, String? id) {
  for (final tenant in tenants) {
    if (tenant.id == id) {
      return tenant;
    }
  }
  return null;
}

/// The synthetic platform tenant row (`users.tenant_id IS NULL`), or null before the list loads.
Tenant? _platformRow(List<Tenant> tenants) {
  for (final tenant in tenants) {
    if (tenant.isPlatform) {
      return tenant;
    }
  }
  return null;
}

/// The tenant a user belongs to — a null `tenantId` means the platform plane.
Tenant? _userTenant(List<Tenant> tenants, String? tenantId) =>
    tenantId == null ? _platformRow(tenants) : _tenantRow(tenants, tenantId);

/// Opens the create/edit user dialog and returns whether a change was saved.
///
/// Pass [existing] to edit a user (rename it and replace its roles), or leave it null to create one.
/// [tenant] fixes the tenant — the tenant drill-down does that — while a null [tenant] lets the user
/// choose from a dropdown (the all-users screen). The dialog performs the API call itself and drops the
/// cached user lists on success.
Future<bool> showUserEditor(
  BuildContext context, {
  Tenant? tenant,
  User? existing,
}) async {
  final saved = await showDialog<bool>(
    context: context,
    builder: (_) => _UserEditorDialog(fixedTenant: tenant, existing: existing),
  );
  return saved ?? false;
}

/// Opens the reset-password dialog for [user]: an administrator sets a temporary password, which the
/// backend writes to Supabase Auth and flags for a forced change on that user's next login. A caller's
/// own password is not resettable here (the backend rejects it) — that goes through the console's
/// "Change password", which asks for the current password.
///
/// Returns whether a password was set.
Future<bool> showResetPasswordDialog(BuildContext context, User user) async {
  final reset = await showDialog<bool>(
    context: context,
    builder: (_) => _ResetPasswordDialog(user: user),
  );
  return reset ?? false;
}

/// The reset form: who it targets (read-only) plus the temporary password. The API answers `204` with no
/// body, so nothing is echoed back — the admin keeps what they typed.
class _ResetPasswordDialog extends ConsumerStatefulWidget {
  const _ResetPasswordDialog({required this.user});

  final User user;

  @override
  ConsumerState<_ResetPasswordDialog> createState() =>
      _ResetPasswordDialogState();
}

class _ResetPasswordDialogState extends ConsumerState<_ResetPasswordDialog> {
  final _formKey = GlobalKey<FormState>();
  final _password = TextEditingController();
  bool _saving = false;

  @override
  void dispose() {
    _password.dispose();
    super.dispose();
  }

  Future<void> _reset() async {
    if (_saving || !_formKey.currentState!.validate()) {
      return;
    }
    setState(() => _saving = true);
    try {
      await ref
          .read(apiClientProvider)
          .resetUserPassword(
            widget.user.id,
            ResetPasswordRequest(temporaryPassword: _password.text),
          );
      // The row now shows "must change password".
      ref.invalidate(usersProvider);
      if (mounted) {
        Navigator.pop(context, true);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _saving = false);
        showApiError(context, error, 'Could not reset the password.');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = widget.user;
    return AlertDialog(
      title: const Text('Reset password'),
      content: SizedBox(
        width: 420,
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _ReadOnlyField(
                label: 'User',
                value: '${user.username} (${user.email})',
                hint: 'They must change this password on their next login',
              ),
              const SizedBox(height: 12),
              PasswordField(
                controller: _password,
                autofocus: true,
                textInputAction: TextInputAction.done,
                onFieldSubmitted: (_) => _reset(),
                labelText: 'Temporary password',
                validator: (value) =>
                    value == null || value.isEmpty ? 'Required' : null,
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _saving ? null : () => Navigator.pop(context, false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _saving ? null : _reset,
          child: const Text('Reset'),
        ),
      ],
    );
  }
}

/// The user form. Roles offered are those of the user's plane — `PLATFORM` roles for a platform user,
/// `TENANT` roles for a tenant user — which is exactly the rule the backend enforces, so the form
/// cannot compose an assignment the server would reject.
class _UserEditorDialog extends ConsumerStatefulWidget {
  const _UserEditorDialog({this.fixedTenant, this.existing});

  final Tenant? fixedTenant;
  final User? existing;

  @override
  ConsumerState<_UserEditorDialog> createState() => _UserEditorDialogState();
}

class _UserEditorDialogState extends ConsumerState<_UserEditorDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _username = TextEditingController(
    text: widget.existing?.username ?? '',
  );
  final _email = TextEditingController();
  final _password = TextEditingController();
  late final Set<String> _checkedRoles = {...?widget.existing?.roles};
  bool _saving = false;

  /// The tenant chosen in the dropdown (the all-users screen), or null before the user picks one.
  String? _selectedTenantId;

  bool get _creating => widget.existing == null;

  @override
  void dispose() {
    _username.dispose();
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  /// The tenant the user belongs to — the fixed context when editing or when the drill-down supplied
  /// one, otherwise the dropdown selection (defaulting to the first real tenant, platform otherwise).
  Tenant? _tenant(List<Tenant> tenants) {
    final existing = widget.existing;
    if (existing != null) {
      return _userTenant(tenants, existing.tenantId);
    }
    if (widget.fixedTenant != null) {
      return widget.fixedTenant;
    }
    return _tenantRow(tenants, _selectedTenantId) ?? _defaultTenant(tenants);
  }

  static Tenant? _defaultTenant(List<Tenant> tenants) {
    for (final tenant in tenants) {
      if (!tenant.isPlatform) {
        return tenant;
      }
    }
    return tenants.isEmpty ? null : tenants.first;
  }

  Future<void> _save(Tenant tenant) async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    setState(() => _saving = true);
    final api = ref.read(apiClientProvider);
    final roles = _checkedRoles.toList()..sort();
    try {
      if (_creating) {
        final email = _email.text.trim();
        await api.createUser(
          CreateUserRequest(
            username: _username.text.trim(),
            tenantId: tenant.isPlatform ? null : tenant.id,
            email: email.isEmpty ? null : email,
            temporaryPassword: _password.text,
            roles: roles,
          ),
        );
      } else {
        await api.updateUser(
          widget.existing!.id,
          UpdateUserRequest(username: _username.text.trim(), roles: roles),
        );
      }
      ref.invalidate(usersProvider);
      if (mounted) {
        Navigator.pop(context, true);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _saving = false);
        showApiError(context, error, 'Could not save the user.');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final tenants = ref.watch(tenantsProvider).valueOrNull ?? const <Tenant>[];
    final tenant = _tenant(tenants);
    return AlertDialog(
      title: Text(_creating ? 'Add user' : 'Edit user'),
      content: SizedBox(
        width: 440,
        child: SingleChildScrollView(
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextFormField(
                  controller: _username,
                  autofocus: true,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: 'Username',
                    helperText: 'Login local-part: username@tenantid',
                  ),
                  validator: (value) =>
                      value == null || value.trim().isEmpty ? 'Required' : null,
                ),
                const SizedBox(height: 12),
                if (_creating)
                  ..._createFields(tenants, tenant)
                else
                  ..._editFields(),
                const SizedBox(height: 16),
                Text('Roles', style: Theme.of(context).textTheme.titleSmall),
                const SizedBox(height: 4),
                _RoleChecklist(
                  tenant: tenant,
                  checked: _checkedRoles,
                  onChanged: (code, checked) => setState(() {
                    if (checked) {
                      _checkedRoles.add(code);
                    } else {
                      _checkedRoles.remove(code);
                    }
                  }),
                ),
              ],
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _saving ? null : () => Navigator.pop(context, false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _saving || tenant == null ? null : () => _save(tenant),
          child: Text(_creating ? 'Create' : 'Save'),
        ),
      ],
    );
  }

  /// Create-only fields: the tenant (dropdown or fixed context), the optional email and the temporary
  /// password.
  List<Widget> _createFields(List<Tenant> tenants, Tenant? tenant) {
    return [
      if (widget.fixedTenant != null)
        _ReadOnlyField(
          label: 'Tenant',
          value: widget.fixedTenant!.name,
          hint: widget.fixedTenant!.isPlatform
              ? 'Platform users — no tenant'
              : 'username@${widget.fixedTenant!.slug}',
        )
      else
        DropdownButtonFormField<String>(
          initialValue: tenant?.id,
          decoration: const InputDecoration(
            labelText: 'Tenant',
            helperText: 'Platform (Keystone) or a customer tenant',
          ),
          items: tenants
              .map(
                (row) => DropdownMenuItem(
                  value: row.id,
                  child: Text(
                    row.isPlatform ? '${row.name} (platform)' : row.name,
                  ),
                ),
              )
              .toList(),
          onChanged: (value) => setState(() => _selectedTenantId = value),
        ),
      const SizedBox(height: 12),
      TextFormField(
        controller: _email,
        textInputAction: TextInputAction.next,
        decoration: InputDecoration(
          labelText: 'Email',
          helperText: tenant == null
              ? 'Optional'
              : 'Optional — blank becomes username@${tenant.isPlatform ? 'keystone' : tenant.slug}.com',
        ),
      ),
      const SizedBox(height: 12),
      PasswordField(
        controller: _password,
        textInputAction: TextInputAction.done,
        labelText: 'Temporary password',
        helperText: 'The user must change it on first login',
        validator: (value) =>
            value == null || value.isEmpty ? 'Required' : null,
      ),
    ];
  }

  /// Edit-only fields: the tenant and the email are the user's identity and are shown read-only. The
  /// email is the Supabase Auth account the login flow authenticates with, so it is not editable here.
  List<Widget> _editFields() {
    final user = widget.existing!;
    return [
      _ReadOnlyField(
        label: 'Email',
        value: user.email,
        hint: 'Supabase Auth identity — not editable',
      ),
      if (user.mustChangePassword) ...[
        const SizedBox(height: 12),
        const ListTile(
          dense: true,
          contentPadding: EdgeInsets.zero,
          leading: Icon(Icons.lock_clock),
          title: Text('Must change password on next login'),
        ),
      ],
    ];
  }
}

/// A read-only field: the value plus why it cannot be changed.
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

/// The roles of the user's plane as a checkbox list. Roles are platform- or tenant-scoped and the
/// backend rejects a cross-plane assignment, so only the matching scope is offered.
class _RoleChecklist extends ConsumerWidget {
  const _RoleChecklist({
    required this.tenant,
    required this.checked,
    required this.onChanged,
  });

  final Tenant? tenant;
  final Set<String> checked;
  final void Function(String code, bool checked) onChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final roles = ref.watch(rolesProvider);
    return roles.when(
      loading: () => const Padding(
        padding: EdgeInsets.all(16),
        child: Center(child: CircularProgressIndicator()),
      ),
      error: (error, _) =>
          Text(apiErrorMessage(error, 'Could not load roles.')),
      data: (all) {
        final platform = tenant?.isPlatform ?? true;
        final offered = all
            .where((role) => role.isPlatformScope == platform)
            .toList(growable: false);
        if (offered.isEmpty) {
          return const Padding(
            padding: EdgeInsets.symmetric(vertical: 8),
            child: Text('No roles are defined for this plane yet.'),
          );
        }
        return SizedBox(
          height: 200,
          child: ListView.builder(
            itemCount: offered.length,
            itemBuilder: (_, i) {
              final role = offered[i];
              return CheckboxListTile(
                dense: true,
                controlAffinity: ListTileControlAffinity.leading,
                value: checked.contains(role.code),
                title: Text(role.code),
                subtitle: Text(
                  role.permissions.isEmpty
                      ? 'no permissions'
                      : role.permissions.join(', '),
                ),
                onChanged: (value) => onChanged(role.code, value ?? false),
              );
            },
          ),
        );
      },
    );
  }
}
