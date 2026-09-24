import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

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
        error: (e, _) => _error(context, ref),
        data: (items) => items.isEmpty
            ? const Center(child: Text('No tenants yet'))
            : ListView.builder(
                itemCount: items.length,
                itemBuilder: (_, i) => ListTile(
                  title: Text(items[i].name),
                  subtitle: Text('${items[i].slug} · ${items[i].id}'),
                ),
              ),
      ),
    );
  }

  Future<void> _create(BuildContext context, WidgetRef ref) async {
    final name = TextEditingController();
    final slug = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Create tenant'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: name,
              decoration: const InputDecoration(labelText: 'Name'),
            ),
            TextField(
              controller: slug,
              decoration: const InputDecoration(
                labelText: 'Slug',
                hintText: 'lowercase-id (used in username@slug)',
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
    );
    if (ok != true) {
      return;
    }
    try {
      await ref.read(apiClientProvider).createTenant(name.text.trim(), slug.text.trim());
      ref.invalidate(tenantsProvider);
    } catch (e) {
      log.e('create tenant failed', error: e);
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Could not create tenant.')));
      }
    }
  }

  Widget _error(BuildContext context, WidgetRef ref) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text('Failed to load tenants'),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: () => ref.invalidate(tenantsProvider),
            child: const Text('Retry'),
          ),
        ],
      ),
    );
  }
}
