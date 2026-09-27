import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/dialogs.dart';
import '../../core/errors.dart';
import '../../core/panels.dart';
import '../../core/permissions.dart';
import '../../core/providers.dart';
import '../../models/models.dart';
import '../../models/requests.dart';
import 'admin_shell.dart';

/// The tenants of the platform, with the synthetic `Keystone` platform tenant first. Tapping a row
/// opens that tenant's users; the row menu renames or deletes it (the platform tenant has neither).
class TenantsScreen extends ConsumerWidget {
  const TenantsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tenants = ref.watch(tenantsProvider);
    final canManage =
        ref.watch(meProvider).valueOrNull?.canWrite(PlatformResource.tenant) ??
        false;
    return Scaffold(
      floatingActionButton: canManage
          ? FloatingActionButton.extended(
              onPressed: () => _save(context, ref),
              icon: const Icon(Icons.add),
              label: const Text('Add tenant'),
            )
          : null,
      body: tenants.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => MessagePanel(
          icon: Icons.error_outline,
          message: apiErrorMessage(error, 'Failed to load tenants.'),
          actionLabel: 'Retry',
          onAction: () => ref.invalidate(tenantsProvider),
        ),
        data: (items) => items.isEmpty
            ? const MessagePanel(
                icon: Icons.apartment_outlined,
                message: 'No tenants yet.',
              )
            : ListView.builder(
                itemCount: items.length,
                itemBuilder: (_, i) => _TenantTile(
                  tenant: items[i],
                  canManage: canManage,
                  onOpen: () => context.go(AdminRoutes.forTenant(items[i].id)),
                  onEdit: () => _save(context, ref, existing: items[i]),
                  onDelete: () => _delete(context, ref, items[i]),
                ),
              ),
      ),
    );
  }

  /// Creates (no [existing]) or renames a tenant through one dialog.
  Future<void> _save(
    BuildContext context,
    WidgetRef ref, {
    Tenant? existing,
  }) async {
    final result = await showDialog<(String, String)>(
      context: context,
      builder: (_) => _TenantFormDialog(existing: existing),
    );
    if (result == null) {
      return;
    }
    final (name, slug) = result;
    final api = ref.read(apiClientProvider);
    try {
      if (existing == null) {
        await api.createTenant(CreateTenantRequest(name: name, slug: slug));
      } else {
        await api.updateTenant(
          existing.id,
          UpdateTenantRequest(name: name, slug: slug),
        );
      }
      ref.invalidate(tenantsProvider);
      if (context.mounted) {
        showApiSuccess(
          context,
          existing == null ? 'Tenant created.' : 'Tenant updated.',
        );
      }
    } catch (error) {
      if (context.mounted) {
        showApiError(context, error, 'Could not save the tenant.');
      }
    }
  }

  Future<void> _delete(
    BuildContext context,
    WidgetRef ref,
    Tenant tenant,
  ) async {
    final confirmed = await confirmDialog(
      context,
      title: 'Delete tenant',
      message:
          'Delete "${tenant.name}"? Its users must be removed first. '
          'This cannot be undone.',
    );
    if (!confirmed) {
      return;
    }
    try {
      await ref.read(apiClientProvider).deleteTenant(tenant.id);
      ref.invalidate(tenantsProvider);
      if (context.mounted) {
        showApiSuccess(context, 'Tenant deleted.');
      }
    } catch (error) {
      if (context.mounted) {
        showApiError(context, error, 'Could not delete the tenant.');
      }
    }
  }
}

enum _TenantAction { edit, delete }

class _TenantTile extends StatelessWidget {
  const _TenantTile({
    required this.tenant,
    required this.canManage,
    required this.onOpen,
    required this.onEdit,
    required this.onDelete,
  });

  final Tenant tenant;
  final bool canManage;
  final VoidCallback onOpen;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: tenant.isPlatform
            ? theme.colorScheme.primaryContainer
            : theme.colorScheme.surfaceContainerHighest,
        child: Icon(
          tenant.isPlatform ? Icons.shield_outlined : Icons.apartment_outlined,
        ),
      ),
      title: Text(tenant.name),
      subtitle: Text(
        tenant.isPlatform
            ? 'Platform plane · platform users'
            : '${tenant.slug} · ${tenant.id}',
      ),
      trailing: canManage && !tenant.isPlatform
          ? PopupMenuButton<_TenantAction>(
              tooltip: 'Tenant actions',
              onSelected: (action) => switch (action) {
                _TenantAction.edit => onEdit(),
                _TenantAction.delete => onDelete(),
              },
              itemBuilder: (_) => const [
                PopupMenuItem(value: _TenantAction.edit, child: Text('Edit')),
                PopupMenuItem(
                  value: _TenantAction.delete,
                  child: Text('Delete'),
                ),
              ],
            )
          : const Icon(Icons.chevron_right),
      onTap: onOpen,
    );
  }
}

/// The create/rename form; pops the entered `(name, slug)` pair.
class _TenantFormDialog extends StatefulWidget {
  const _TenantFormDialog({this.existing});

  final Tenant? existing;

  @override
  State<_TenantFormDialog> createState() => _TenantFormDialogState();
}

class _TenantFormDialogState extends State<_TenantFormDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _name = TextEditingController(
    text: widget.existing?.name ?? '',
  );
  late final TextEditingController _slug = TextEditingController(
    text: widget.existing?.slug ?? '',
  );

  @override
  void dispose() {
    _name.dispose();
    _slug.dispose();
    super.dispose();
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    Navigator.pop(context, (_name.text.trim(), _slug.text.trim()));
  }

  @override
  Widget build(BuildContext context) {
    final creating = widget.existing == null;
    return AlertDialog(
      title: Text(creating ? 'Add tenant' : 'Edit tenant'),
      content: SizedBox(
        width: 380,
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _name,
                autofocus: true,
                textInputAction: TextInputAction.next,
                decoration: const InputDecoration(labelText: 'Name'),
                validator: (value) =>
                    value == null || value.trim().isEmpty ? 'Required' : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _slug,
                textInputAction: TextInputAction.done,
                onFieldSubmitted: (_) => _submit(),
                decoration: const InputDecoration(
                  labelText: 'Slug',
                  helperText: 'Lowercase id used in username@slug',
                ),
                validator: (value) =>
                    value == null || value.trim().isEmpty ? 'Required' : null,
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _submit,
          child: Text(creating ? 'Create' : 'Save'),
        ),
      ],
    );
  }
}
