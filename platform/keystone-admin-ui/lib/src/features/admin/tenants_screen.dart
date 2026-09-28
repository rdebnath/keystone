import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/dialogs.dart';
import '../../core/errors.dart';
import '../../core/lists.dart';
import '../../core/permissions.dart';
import '../../core/providers.dart';
import '../../models/list_query.dart';
import '../../models/models.dart';
import '../../models/requests.dart';
import 'admin_shell.dart';

/// The tenants of the platform, **one page at a time**, with the synthetic `Keystone` platform row pinned
/// first. Searching, filtering and paging happen on the server (`docs/UX_GUIDELINES.md` §1), so a search
/// finds a tenant that is not on the page the console happens to be showing.
///
/// Tapping a row opens that tenant's users; the row menu renames or deletes it (the platform tenant has
/// neither).
class TenantsScreen extends ConsumerStatefulWidget {
  const TenantsScreen({super.key});

  @override
  ConsumerState<TenantsScreen> createState() => _TenantsScreenState();
}

class _TenantsScreenState extends ConsumerState<TenantsScreen> {
  /// The list state — search, page, size, sort — mirrored into the URL, so a filtered list survives a
  /// refresh and can be linked (`docs/UX_GUIDELINES.md` §1.12).
  ListQuery _query = ListQuery.initial;
  bool _restored = false;

  /// The keys the tenants list accepts (`docs/CODING_GUIDELINES_BACKEND.md` §8); the first is the server's
  /// default order, which is what the list shows until the user chooses otherwise.
  static const List<SortOption> _sortOptions = <SortOption>[
    SortOption(null, 'Name (default)'),
    SortOption('slug', 'Slug'),
    SortOption('country', 'Country'),
    SortOption('createdAt', 'Created'),
    SortOption('updatedAt', 'Updated'),
  ];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Read the URL once: after that the screen owns the query (the URL is written back from it below).
    if (!_restored) {
      _restored = true;
      _query = ListQueryLocation.read(context);
    }
  }

  /// Applies a new query and puts it in the URL, so the list on screen is the list the location describes.
  void _update(ListQuery query) {
    setState(() => _query = query);
    ListQueryLocation.write(context, AdminRoutes.tenants, query);
  }

  @override
  Widget build(BuildContext context) {
    final tenants = ref.watch(tenantsPageProvider(_query));
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
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          ListToolbar(
            search: SearchField(
              value: _query.search,
              hintText: 'Search name, slug or country',
              onChanged: (value) => _update(_query.withSearch(value)),
            ),
            trailing: SortSelect(
              query: _query,
              onQueryChanged: _update,
              options: _sortOptions,
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: PagedListView<Tenant>(
              value: tenants,
              query: _query,
              onQueryChanged: _update,
              onRetry: () => ref.invalidate(tenantsPageProvider(_query)),
              emptyIcon: Icons.apartment_outlined,
              emptyMessage: 'No tenants yet.',
              itemBuilder: (context, tenant) => _TenantTile(
                tenant: tenant,
                canManage: canManage,
                onOpen: () => context.go(AdminRoutes.forTenant(tenant.id)),
                onEdit: () => _save(context, ref, existing: tenant),
                onDelete: () => _delete(context, ref, tenant),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Creates (no [existing]) or renames a tenant through one dialog.
  Future<void> _save(
    BuildContext context,
    WidgetRef ref, {
    Tenant? existing,
  }) async {
    final result = await showDialog<(String, String, String?)>(
      context: context,
      builder: (_) => _TenantFormDialog(existing: existing),
    );
    if (result == null) {
      return;
    }
    final (name, slug, country) = result;
    final api = ref.read(apiClientProvider);
    try {
      if (existing == null) {
        await api.createTenant(
          CreateTenantRequest(name: name, slug: slug, country: country),
        );
      } else {
        await api.updateTenant(
          existing.id,
          UpdateTenantRequest(name: name, slug: slug, country: country),
        );
      }
      ref.invalidate(tenantsPageProvider);
      ref.invalidate(tenantOptionsProvider);
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
      ref.invalidate(tenantsPageProvider);
      ref.invalidate(tenantOptionsProvider);
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
            // The country is only worth showing when the tenant recorded one.
            : [
                tenant.slug,
                if (tenant.country != null) tenant.country!,
                tenant.id,
              ].join(' · '),
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

/// The create/rename form; pops the entered `(name, slug, country)` triple, the country null when the
/// field was left empty.
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
  late final TextEditingController _country = TextEditingController(
    text: widget.existing?.country ?? '',
  );

  @override
  void dispose() {
    _name.dispose();
    _slug.dispose();
    _country.dispose();
    super.dispose();
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    Navigator.pop(context, (
      _name.text.trim(),
      _slug.text.trim(),
      _countryCode(),
    ));
  }

  /// The entered country in the canonical shape the backend stores, or null when the field is empty —
  /// a blank field clears the recorded country instead of sending a value the backend would reject.
  String? _countryCode() {
    final code = _country.text.trim().toUpperCase();
    return code.isEmpty ? null : code;
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
                textInputAction: TextInputAction.next,
                decoration: const InputDecoration(
                  labelText: 'Slug',
                  helperText: 'Lowercase id used in username@slug',
                ),
                validator: (value) =>
                    value == null || value.trim().isEmpty ? 'Required' : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _country,
                textInputAction: TextInputAction.done,
                onFieldSubmitted: (_) => _submit(),
                textCapitalization: TextCapitalization.characters,
                decoration: const InputDecoration(
                  labelText: 'Country',
                  helperText: 'Optional ISO 3166-1 alpha-2 code, e.g. IN',
                ),
                // Only a shape check: whether the code exists is the backend's rule (it validates
                // against the ISO list), so this just rules out a value that could not be a code.
                validator: (value) {
                  final code = value?.trim() ?? '';
                  if (code.isEmpty || RegExp(r'^[A-Za-z]{2}$').hasMatch(code)) {
                    return null;
                  }
                  return 'Two letters, e.g. IN';
                },
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
