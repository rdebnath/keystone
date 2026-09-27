import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/dialogs.dart';
import '../../core/log.dart';
import '../../core/providers.dart';
import '../../models/requests.dart';

class RolesScreen extends ConsumerWidget {
  const RolesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final roles = ref.watch(rolesProvider);
    return Scaffold(
      floatingActionButton: FloatingActionButton(
        onPressed: () => _create(context, ref),
        child: const Icon(Icons.add),
      ),
      body: roles.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => _error(context, ref),
        data: (items) => items.isEmpty
            ? const Center(child: Text('No roles yet'))
            : ListView.builder(
                itemCount: items.length,
                itemBuilder: (_, i) => ListTile(
                  title: Text(items[i].code),
                  subtitle: Text(
                    '${items[i].scope} · ${items[i].permissions.join(', ')}',
                  ),
                ),
              ),
      ),
    );
  }

  Future<void> _create(BuildContext context, WidgetRef ref) async {
    final code = TextEditingController();
    final perms = TextEditingController();
    var scope = 'PLATFORM';
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          title: const Text('Create role'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: code,
                decoration: const InputDecoration(labelText: 'Code'),
              ),
              InputDecorator(
                decoration: const InputDecoration(labelText: 'Scope'),
                child: DropdownButton<String>(
                  value: scope,
                  isExpanded: true,
                  underline: const SizedBox.shrink(),
                  items: const ['PLATFORM', 'TENANT']
                      .map((s) => DropdownMenuItem(value: s, child: Text(s)))
                      .toList(),
                  onChanged: (v) => setState(() => scope = v ?? 'PLATFORM'),
                ),
              ),
              TextField(
                controller: perms,
                decoration: const InputDecoration(
                  labelText: 'Permissions (comma-separated)',
                  hintText:
                      'platform:tenant:read-only, platform:role:read-write',
                ),
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
          .createRole(
            CreateRoleRequest(
              code: code.text.trim(),
              scope: scope,
              permissions: splitList(perms.text),
            ),
          );
      ref.invalidate(rolesProvider);
    } catch (e) {
      log.e('create role failed', error: e);
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Could not create role.')));
      }
    }
  }

  Widget _error(BuildContext context, WidgetRef ref) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text('Failed to load roles'),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: () => ref.invalidate(rolesProvider),
            child: const Text('Retry'),
          ),
        ],
      ),
    );
  }
}
