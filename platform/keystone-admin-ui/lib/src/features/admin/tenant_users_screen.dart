import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/errors.dart';
import '../../core/lists.dart';
import '../../core/panels.dart';
import '../../core/permissions.dart';
import '../../core/providers.dart';
import '../../models/list_query.dart';
import '../../models/models.dart';
import 'admin_shell.dart';
import 'user_editor.dart';

/// The users of one tenant — the destination of a tenant row in [TenantsScreen]. The synthetic
/// `Keystone` tenant lists the platform users (`users.tenant_id IS NULL`).
class TenantUsersScreen extends ConsumerWidget {
  const TenantUsersScreen({super.key, required this.tenantId});

  final String tenantId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tenants = ref.watch(tenantOptionsProvider);
    final canManage =
        ref.watch(meProvider).valueOrNull?.canWrite(PlatformResource.user) ??
        false;
    return tenants.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => MessagePanel(
        icon: Icons.error_outline,
        message: apiErrorMessage(error, 'Failed to load the tenant.'),
        actionLabel: 'Retry',
        onAction: () => ref.invalidate(tenantOptionsProvider),
      ),
      data: (_) {
        final tenant = ref.watch(tenantByIdProvider(tenantId));
        if (tenant == null) {
          return MessagePanel(
            icon: Icons.help_outline,
            message: 'This tenant no longer exists.',
            actionLabel: 'Back to tenants',
            onAction: () => context.go(AdminRoutes.tenants),
          );
        }
        return _TenantUsersBody(tenant: tenant, canManage: canManage);
      },
    );
  }
}

/// One tenant's users, **one page at a time**, with the search applied by the server. The tenant is the
/// route's, so every query here carries it — and it is never something the user types.
class _TenantUsersBody extends ConsumerStatefulWidget {
  const _TenantUsersBody({required this.tenant, required this.canManage});

  final Tenant tenant;
  final bool canManage;

  @override
  ConsumerState<_TenantUsersBody> createState() => _TenantUsersBodyState();
}

class _TenantUsersBodyState extends ConsumerState<_TenantUsersBody> {
  ListQuery _query = ListQuery.initial;
  bool _restored = false;

  /// The keys the users list accepts (`docs/CODING_GUIDELINES_BACKEND.md` §8).
  static const List<SortOption> _sortOptions = <SortOption>[
    SortOption(null, 'Username (default)'),
    SortOption('email', 'Email'),
    SortOption('createdAt', 'Created'),
    SortOption('updatedAt', 'Updated'),
  ];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_restored) {
      _restored = true;
      // The tenant is this screen's path parameter, so it is part of **every** query this screen makes —
      // set here, once, and preserved by every subsequent change.
      _query = ListQueryLocation.read(context).withTenant(widget.tenant.id);
    }
  }

  void _update(ListQuery query) {
    setState(() => _query = query);
    // The tenant is already in the path, so it is not repeated in the query string.
    ListQueryLocation.write(
      context,
      AdminRoutes.forTenant(widget.tenant.id),
      query.withoutTenant(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: widget.canManage
          ? FloatingActionButton.extended(
              onPressed: () => showUserEditor(context, tenant: widget.tenant),
              icon: const Icon(Icons.person_add_alt),
              label: const Text('Add user'),
            )
          : null,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _TenantHeader(tenant: widget.tenant),
          ListToolbar(
            search: SearchField(
              value: _query.search,
              hintText: 'Search username, email or phone number',
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
            child: UserList(
              query: _query,
              onQueryChanged: _update,
              canManage: widget.canManage,
            ),
          ),
        ],
      ),
    );
  }
}

class _TenantHeader extends StatelessWidget {
  const _TenantHeader({required this.tenant});

  final Tenant tenant;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor: tenant.isPlatform
                ? theme.colorScheme.primaryContainer
                : theme.colorScheme.surfaceContainerHighest,
            child: Icon(
              tenant.isPlatform
                  ? Icons.shield_outlined
                  : Icons.apartment_outlined,
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(tenant.name, style: theme.textTheme.titleMedium),
                const SizedBox(height: 2),
                Text(
                  tenant.isPlatform
                      ? 'Platform users — no tenant (login as username@keystone)'
                      : '${tenant.slug} · login as username@${tenant.slug}',
                  style: theme.textTheme.bodySmall,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
