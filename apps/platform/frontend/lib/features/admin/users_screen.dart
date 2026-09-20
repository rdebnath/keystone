import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/dialogs.dart';
import '../../core/log.dart';
import '../../core/providers.dart';

class UsersScreen extends ConsumerWidget {
  const UsersScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final users = ref.watch(usersProvider);
    return Scaffold(
      floatingActionButton: FloatingActionButton(
        onPressed: () => _create(context, ref),
        child: const Icon(Icons.add),
      ),
      body: users.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => _error(context, ref),
        data: (items) => items.isEmpty
            ? const Center(child: Text('No users yet'))
            : ListView.builder(
                itemCount: items.length,
                itemBuilder: (_, i) => ListTile(
                  title: Text(items[i].email),
                  subtitle: Text(items[i].roles.join(', ')),
                ),
              ),
      ),
    );
  }

  Future<void> _create(BuildContext context, WidgetRef ref) async {
    final email = TextEditingController();
    final password = TextEditingController();
    final roles = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Create user'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: email,
              decoration: const InputDecoration(labelText: 'Email'),
            ),
            TextField(
              controller: password,
              obscureText: true,
              decoration: const InputDecoration(labelText: 'Temporary password'),
            ),
            TextField(
              controller: roles,
              decoration: const InputDecoration(
                labelText: 'Roles (comma-separated codes)',
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
      await ref.read(apiClientProvider).createUser(
            email.text.trim(),
            password.text,
            splitList(roles.text),
          );
      ref.invalidate(usersProvider);
    } catch (e) {
      log.e('create user failed', error: e);
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Could not create user.')));
      }
    }
  }

  Widget _error(BuildContext context, WidgetRef ref) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text('Failed to load users'),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: () => ref.invalidate(usersProvider),
            child: const Text('Retry'),
          ),
        ],
      ),
    );
  }
}
