import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/dialogs.dart';
import '../../core/log.dart';
import '../../core/providers.dart';

class TenantsScreen extends ConsumerWidget {
  const TenantsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tenants = ref.watch(tenantsProvider);
    return Scaffold(
      floatingActionButton: FloatingActionButton(
        onPressed: () => _create(context, ref),
        child: const Icon(Icons.add),
      ),
      body: tenants.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => _error(context, ref, 'tenants'),
        data: (items) => items.isEmpty
            ? const Center(child: Text('No tenants yet'))
            : ListView.builder(
                itemCount: items.length,
                itemBuilder: (_, i) => ListTile(
                  title: Text(items[i].name),
                  subtitle: Text(items[i].id),
                ),
              ),
      ),
    );
  }

  Future<void> _create(BuildContext context, WidgetRef ref) async {
    final name = await promptText(context, title: 'Create tenant', label: 'Name');
    if (name == null || name.trim().isEmpty) {
      return;
    }
    try {
      await ref.read(apiClientProvider).createTenant(name.trim());
      ref.invalidate(tenantsProvider);
    } catch (e) {
      log.e('create tenant failed', error: e);
      if (context.mounted) {
        _snack(context, 'Could not create tenant.');
      }
    }
  }

  Widget _error(BuildContext context, WidgetRef ref, String label) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('Failed to load $label'),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: () => ref.invalidate(tenantsProvider),
            child: const Text('Retry'),
          ),
        ],
      ),
    );
  }

  void _snack(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }
}
