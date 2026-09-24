import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/log.dart';
import '../../core/providers.dart';

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
                  subtitle: Text(items[i].scope),
                ),
              ),
      ),
    );
  }

  Future<void> _create(BuildContext context, WidgetRef ref) async {
    final code = TextEditingController();
    var scope = 'PLATFORM';
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          title: const Text('Create permission'),
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
      await ref.read(apiClientProvider).createPermission(code.text.trim(), scope);
      ref.invalidate(permissionsProvider);
    } catch (e) {
      log.e('create permission failed', error: e);
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Could not create permission.')));
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
}
