import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/auth_provider.dart';
import 'permissions_screen.dart';
import 'roles_screen.dart';
import 'tenants_screen.dart';
import 'users_screen.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return DefaultTabController(
      length: 4,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Platform Admin'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Tenants'),
              Tab(text: 'Roles'),
              Tab(text: 'Permissions'),
              Tab(text: 'Users'),
            ],
          ),
          actions: [
            IconButton(
              icon: const Icon(Icons.logout),
              tooltip: 'Sign out',
              onPressed: () => ref.read(authServiceProvider).signOut(),
            ),
          ],
        ),
        body: const TabBarView(
          children: [
            TenantsScreen(),
            RolesScreen(),
            PermissionsScreen(),
            UsersScreen(),
          ],
        ),
      ),
    );
  }
}
