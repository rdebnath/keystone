import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/errors.dart';
import '../../core/panels.dart';
import '../../core/permissions.dart';
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
        error: (error, _) => MessagePanel(
          icon: Icons.error_outline,
          message: apiErrorMessage(error, 'Failed to load permissions.'),
          actionLabel: 'Retry',
          onAction: () => ref.invalidate(permissionsProvider),
        ),
        data: (items) => items.isEmpty
            ? const MessagePanel(
                icon: Icons.key_outlined,
                message: 'No permissions yet.',
              )
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
    var resource = catalogResources.first;
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
                  items: catalogResources
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
                child: Text('Code: ${resource.codeFor(access)}'),
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
              code: resource.codeFor(access),
              scope: resource.scope,
            ),
          );
      ref.invalidate(permissionsProvider);
      if (context.mounted) {
        showApiSuccess(context, 'Permission created.');
      }
    } catch (error) {
      if (context.mounted) {
        showApiError(context, error, 'Could not create permission.');
      }
    }
  }

  /// The scope and the access level a catalog row carries.
  String _subtitle(Permission permission) {
    final level = permission.access?.label ?? 'unknown level';
    return '${permission.scope} · $level';
  }

  static CatalogResource _resourceFor(String? code) {
    return catalogResources.firstWhere(
      (r) => r.code == code,
      orElse: () => catalogResources.first,
    );
  }
}
