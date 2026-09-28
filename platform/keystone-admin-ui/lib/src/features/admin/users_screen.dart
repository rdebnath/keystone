import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/lists.dart';
import '../../core/providers.dart';
import '../../models/list_query.dart';
import '../../models/models.dart';
import 'admin_shell.dart';
import 'owner_filter.dart';
import 'user_editor.dart';

/// The users of the current console.
///
/// One widget serves both planes: the platform plane lists every user (or one tenant's) with a tenant
/// filter, while a tenant console lists exactly its own — there is no filter to offer, because there is
/// nothing the caller may choose.
/// The users of the current console, **one page at a time**, with the search and the tenant filter applied
/// by the server — so a search finds a user who is not on the page being shown (`docs/UX_GUIDELINES.md` §1).
///
/// One widget serves both planes: the platform plane lists every user (or one tenant's) with a tenant
/// filter, while a tenant console lists exactly its own — there is no filter to offer, because there is
/// nothing the caller may choose.
class UsersScreen extends ConsumerStatefulWidget {
  const UsersScreen({super.key});

  @override
  ConsumerState<UsersScreen> createState() => _UsersScreenState();
}

class _UsersScreenState extends ConsumerState<UsersScreen> {
  /// The list state — search, tenant filter, page, size, sort — kept in the URL (§1.12).
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
      _query = ListQueryLocation.read(context);
    }
  }

  void _update(ListQuery query) {
    setState(() => _query = query);
    ListQueryLocation.write(context, _path, query);
  }

  /// The route this screen is showing: a tenant console has its own path, and the URL must keep the user
  /// on it.
  String get _path => ref.read(consoleProvider).isPlatformPlane
      ? AdminRoutes.users
      : AdminRoutes.tenantUsers;

  @override
  Widget build(BuildContext context) {
    final console = ref.watch(consoleProvider);
    final canManage =
        ref.watch(meProvider).valueOrNull?.canWrite(console.userResource) ??
        false;
    final selected = console.isPlatformPlane ? _selectedTenant() : null;
    final canAdd = canManage && (!console.isPlatformPlane || selected != null);
    return Scaffold(
      floatingActionButton: canAdd
          ? FloatingActionButton.extended(
              onPressed: () => showUserEditor(
                context,
                // A filtered platform list fixes the tenant; "All tenants" lets the dialog ask, and a
                // tenant console always creates in the caller's own tenant.
                tenant: console.isPlatformPlane && _query.tenantId != null
                    ? selected
                    : null,
              ),
              icon: const Icon(Icons.person_add_alt),
              label: const Text('Add user'),
            )
          : null,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          ListToolbar(
            search: SearchField(
              value: _query.search,
              hintText: 'Search username, email or phone number',
              onChanged: (value) => _update(_query.withSearch(value)),
            ),
            filters: <Widget>[
              if (console.isPlatformPlane)
                SizedBox(
                  width: 240,
                  child: TenantFilter(
                    value: _query.tenantId,
                    onChanged: (tenantId) =>
                        _update(_query.withTenant(tenantId)),
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
            child: UserList(
              query: _query,
              onQueryChanged: _update,
              canManage: canManage,
              showTenant: console.isPlatformPlane,
            ),
          ),
        ],
      ),
    );
  }

  /// The tenant new users are added to: the filter when one is set, otherwise the first real tenant.
  Tenant? _selectedTenant() {
    final tenants =
        ref.watch(tenantOptionsProvider).valueOrNull?.items ?? const <Tenant>[];
    for (final tenant in tenants) {
      if (tenant.id == _query.tenantId) {
        return tenant;
      }
    }
    for (final tenant in tenants) {
      if (!tenant.isPlatform) {
        return tenant;
      }
    }
    return null;
  }
}
