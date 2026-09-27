import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/permissions.dart';
import '../../core/providers.dart';
import '../../models/models.dart';
import 'user_editor.dart';

/// Every user of the platform, with a tenant filter. The tenant drill-down ([TenantsScreen] →
/// [TenantUsersScreen]) is the usual path; this screen is the cross-tenant view.
class UsersScreen extends ConsumerStatefulWidget {
  const UsersScreen({super.key});

  @override
  ConsumerState<UsersScreen> createState() => _UsersScreenState();
}

class _UsersScreenState extends ConsumerState<UsersScreen> {
  /// The selected tenant id, or null for every tenant.
  String? _tenantId;

  @override
  Widget build(BuildContext context) {
    final tenants = ref.watch(tenantsProvider).valueOrNull ?? const <Tenant>[];
    final canManage =
        ref.watch(meProvider).valueOrNull?.canWrite(PlatformResource.user) ??
        false;
    final selected = _selectedTenant(tenants);
    return Scaffold(
      floatingActionButton: canManage && selected != null
          ? FloatingActionButton.extended(
              onPressed: () => showUserEditor(
                context,
                // A filtered list fixes the tenant; "All tenants" lets the dialog ask.
                tenant: _tenantId == null ? null : selected,
              ),
              icon: const Icon(Icons.person_add_alt),
              label: const Text('Add user'),
            )
          : null,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
            child: DropdownButtonFormField<String?>(
              initialValue: _tenantId,
              decoration: const InputDecoration(
                labelText: 'Tenant',
                helperText:
                    'Filter by tenant — Keystone holds the platform users',
              ),
              items: [
                const DropdownMenuItem<String?>(
                  value: null,
                  child: Text('All tenants'),
                ),
                ...tenants.map(
                  (tenant) => DropdownMenuItem<String?>(
                    value: tenant.id,
                    child: Text(
                      tenant.isPlatform
                          ? '${tenant.name} (platform)'
                          : tenant.name,
                    ),
                  ),
                ),
              ],
              onChanged: (value) => setState(() => _tenantId = value),
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: UserList(
              tenantId: _tenantId,
              canManage: canManage,
              showTenant: true,
            ),
          ),
        ],
      ),
    );
  }

  /// The tenant new users are added to: the filter when one is set, otherwise the first real tenant.
  Tenant? _selectedTenant(List<Tenant> tenants) {
    for (final tenant in tenants) {
      if (tenant.id == _tenantId) {
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
