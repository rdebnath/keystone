import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/dialogs.dart';
import '../../core/errors.dart';
import '../../core/lists.dart';
import '../../core/password_field.dart';
import '../../core/providers.dart';
import '../../models/list_query.dart';
import '../../models/models.dart';
import '../../models/requests.dart';

/// The users of one page: the list body of both users screens, paged, searchable and filterable **by the
/// server**, with edit, reset-password and delete actions when [canManage].
///
/// The query is owned by the screen (which also keeps it in the URL); this widget renders it and reports
/// every change back through [onQueryChanged]. [showTenant] names each user's tenant — which is only
/// meaningful when the list spans tenants.
class UserList extends ConsumerWidget {
  const UserList({
    super.key,
    required this.query,
    required this.onQueryChanged,
    required this.canManage,
    this.showTenant = false,
  });

  final ListQuery query;
  final ValueChanged<ListQuery> onQueryChanged;
  final bool canManage;
  final bool showTenant;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final users = ref.watch(usersPageProvider(query));
    // Only a cross-tenant list names the tenant, and the names come from the unpaged options list: a page
    // would label only the tenants it happened to hold (`docs/UX_GUIDELINES.md` §1.13).
    final tenants = showTenant
        ? ref.watch(tenantOptionsProvider).valueOrNull?.items ?? const <Tenant>[]
        : const <Tenant>[];
    return PagedListView<User>(
      value: users,
      query: query,
      onQueryChanged: onQueryChanged,
      onRetry: () => ref.invalidate(usersPageProvider(query)),
      emptyIcon: Icons.people_outline,
      emptyMessage: 'No users yet.',
      itemBuilder: (context, user) => _UserTile(
        user: user,
        canManage: canManage,
        tenantLabel: showTenant ? _tenantName(tenants, user.tenantId) : null,
        onEdit: () => showUserEditor(context, existing: user),
        onResetPassword: () => showResetPasswordDialog(context, user),
        onDelete: () => _delete(context, ref, user),
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
      await ref.read(consoleProvider).deleteUser(user.id);
      ref.invalidate(usersPageProvider);
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
      if (user.phoneNumber != null) user.phoneNumber!,
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
          .read(consoleProvider)
          .resetUserPassword(
            widget.user.id,
            ResetPasswordRequest(temporaryPassword: _password.text),
          );
      // The row now shows "must change password".
      ref.invalidate(usersPageProvider);
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
  late final TextEditingController _phone = TextEditingController(
    text: widget.existing?.phoneNumber ?? '',
  );
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
    _phone.dispose();
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

  /// The entered phone number, or null when the field is empty — a blank field clears the recorded
  /// number rather than sending an empty string the backend would reject.
  String? _phoneNumberOrNull() {
    final number = _phone.text.trim();
    return number.isEmpty ? null : number;
  }

  /// The phone-number field, shared by the create and the edit form. Only the shape is checked here
  /// (every E.164 number starts with `+`); the rule itself — the length and the digits — is the
  /// backend's, which answers 422 for a value it rejects.
  Widget _phoneField() {
    return TextFormField(
      controller: _phone,
      textInputAction: TextInputAction.next,
      keyboardType: TextInputType.phone,
      decoration: const InputDecoration(
        labelText: 'Phone number',
        helperText: 'Optional E.164 number, e.g. +919876543210',
      ),
      validator: (value) {
        final number = value?.trim() ?? '';
        if (number.isEmpty || number.startsWith('+')) {
          return null;
        }
        return 'Start with + and the country code';
      },
    );
  }

  /// [tenant] is the target tenant on the platform plane; a tenant console has none to pass — the backend
  /// scopes a new user to the caller, so the request carries no `tenantId` at all.
  Future<void> _save(Tenant? tenant) async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    setState(() => _saving = true);
    final console = ref.read(consoleProvider);
    final roles = _checkedRoles.toList()..sort();
    final phoneNumber = _phoneNumberOrNull();
    try {
      if (_creating) {
        final email = _email.text.trim();
        await console.createUser(
          CreateUserRequest(
            username: _username.text.trim(),
            tenantId: tenant == null || tenant.isPlatform ? null : tenant.id,
            email: email.isEmpty ? null : email,
            phoneNumber: phoneNumber,
            temporaryPassword: _password.text,
            roles: roles,
          ),
        );
      } else {
        await console.updateUser(
          widget.existing!.id,
          UpdateUserRequest(
            username: _username.text.trim(),
            phoneNumber: phoneNumber,
            roles: roles,
          ),
        );
      }
      ref.invalidate(usersPageProvider);
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
    final console = ref.watch(consoleProvider);
    // Only the platform plane names a tenant, so only it needs — or can read — the tenant options.
    final tenants = console.isPlatformPlane
        ? ref.watch(tenantOptionsProvider).valueOrNull?.items ?? const <Tenant>[]
        : const <Tenant>[];
    final tenant = console.isPlatformPlane ? _tenant(tenants) : null;
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
          onPressed: _saving || (console.isPlatformPlane && tenant == null)
              ? null
              : () => _save(tenant),
          child: Text(_creating ? 'Create' : 'Save'),
        ),
      ],
    );
  }

  /// Create-only fields: the tenant (dropdown or fixed context), the optional email and the temporary
  /// password.
  List<Widget> _createFields(List<Tenant> tenants, Tenant? tenant) {
    // The tenant is the platform plane's choice; a tenant console always creates for itself.
    final platformPlane = ref.read(consoleProvider).isPlatformPlane;
    return [
      if (platformPlane) ...[
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
      ],
      TextFormField(
        controller: _email,
        textInputAction: TextInputAction.next,
        decoration: InputDecoration(
          labelText: 'Email',
          helperText: tenant == null
              ? 'Optional — blank becomes username@<your tenant>.com'
              : 'Optional — blank becomes username@${tenant.isPlatform ? 'keystone' : tenant.slug}.com',
        ),
      ),
      const SizedBox(height: 12),
      _phoneField(),
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
  /// email is the Supabase Auth account the login flow authenticates with, so it is not editable here;
  /// the phone number is plain profile data, so it is.
  List<Widget> _editFields() {
    final user = widget.existing!;
    return [
      _ReadOnlyField(
        label: 'Email',
        value: user.email,
        hint: 'Supabase Auth identity — not editable',
      ),
      const SizedBox(height: 12),
      _phoneField(),
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
    // Every role the console may assign. This is the **unpaged** options list on purpose: a checklist must
    // offer all of them, and a page would hide the rest; the plane filter below picks the ones this user
    // may hold.
    final roles = ref.watch(roleOptionsProvider(null));
    return roles.when(
      loading: () => const Padding(
        padding: EdgeInsets.all(16),
        child: Center(child: CircularProgressIndicator()),
      ),
      error: (error, _) =>
          Text(apiErrorMessage(error, 'Could not load roles.')),
      data: (options) {
        // The roles offered belong to the *user's* plane, not the console's: on the platform plane that is
        // the chosen tenant's plane, and in a tenant console every user is in the caller's own tenant.
        final platform = ref.watch(consoleProvider).isPlatformPlane
            ? (tenant?.isPlatform ?? true)
            : false;
        final offered = options.items
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
