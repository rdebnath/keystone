import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/log.dart';
import '../../core/providers.dart';
import '../../models/models.dart';
import '../../models/requests.dart';

class PermissionsScreen extends ConsumerWidget {
  const PermissionsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final permissions = ref.watch(permissionsProvider);
    return Scaffold(
      floatingActionButton: FloatingActionButton(
        onPressed: () => _create(context, ref),
        child: const Icon(Icons.add),
      ),
      body: permissions.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => _error(context, ref),
        data: (items) => items.isEmpty
            ? const Center(child: Text('No permissions yet'))
            : ListView.builder(
                itemCount: items.length,
                itemBuilder: (_, i) => ListTile(
                  title: Text(items[i].code),
                  subtitle: Text(_subtitle(items[i])),
                ),
              ),
      ),
    );
  }

  Future<void> _create(BuildContext context, WidgetRef ref) async {
    var resource = _resources.first;
    var access = PermissionAccess.readOnly;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          title: const Text('Create permission'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              InputDecorator(
                decoration: const InputDecoration(labelText: 'Resource'),
                child: DropdownButton<String>(
                  value: resource.code,
                  isExpanded: true,
                  underline: const SizedBox.shrink(),
                  items: _resources
                      .map(
                        (r) => DropdownMenuItem(
                          value: r.code,
                          child: Text(r.code),
                        ),
                      )
                      .toList(),
                  onChanged: (v) => setState(() {
                    resource = _resourceFor(v);
                  }),
                ),
              ),
              InputDecorator(
                decoration: const InputDecoration(labelText: 'Type'),
                child: DropdownButton<PermissionAccess>(
                  value: access,
                  isExpanded: true,
                  underline: const SizedBox.shrink(),
                  items: PermissionAccess.values
                      .map(
                        (a) => DropdownMenuItem(value: a, child: Text(a.label)),
                      )
                      .toList(),
                  onChanged: (v) => setState(() {
                    access = v ?? PermissionAccess.readOnly;
                  }),
                ),
              ),
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.centerLeft,
                child: Text('Code: ${resource.code}:${access.suffix}'),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Create'),
            ),
          ],
        ),
      ),
    );
    if (ok != true) {
      return;
    }
    try {
      await ref
          .read(apiClientProvider)
          .createPermission(
            CreatePermissionRequest(
              code: '${resource.code}:${access.suffix}',
              scope: resource.scope,
            ),
          );
      ref.invalidate(permissionsProvider);
    } catch (e) {
      log.e('create permission failed', error: e);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Could not create permission.')),
        );
      }
    }
  }

  Widget _error(BuildContext context, WidgetRef ref) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text('Failed to load permissions'),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: () => ref.invalidate(permissionsProvider),
            child: const Text('Retry'),
          ),
        ],
      ),
    );
  }

  /// The scope and the access level a catalog row carries.
  String _subtitle(Permission permission) {
    final level = permission.access?.label ?? 'unknown level';
    return '${permission.scope} · $level';
  }

  static _Resource _resourceFor(String? code) {
    return _resources.firstWhere(
      (r) => r.code == code,
      orElse: () => _resources.first,
    );
  }
}

/// One of the platform-defined catalog resources a permission can be created for. The access level
/// is chosen separately (a permission code is `<resource>:<level>`), so the scope is derived from
/// the resource namespace rather than chosen by the user.
class _Resource {
  const _Resource(this.code, this.scope);

  final String code;
  final String scope;
}

const _resources = <_Resource>[
  _Resource('platform:tenant', 'PLATFORM'),
  _Resource('platform:role', 'PLATFORM'),
  _Resource('platform:permission', 'PLATFORM'),
  _Resource('platform:user', 'PLATFORM'),
  _Resource('tenant:role', 'TENANT'),
  _Resource('tenant:permission', 'TENANT'),
  _Resource('tenant:user', 'TENANT'),
];
