import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

import 'features/inventory/home_screen.dart';

/// Declarative router with an auth gate and plane routing: signed-out → login; first login → change
/// password; platform user → the platform console; tenant user with a tenant console permission → the
/// tenant console (the same screens on the tenant plane); any other tenant user → inventory.
final routerProvider = Provider<GoRouter>((ref) {
  final refresh = ValueNotifier<int>(0);
  ref.onDispose(refresh.dispose);
  ref.listen(signedInProvider, (_, __) => refresh.value++);
  ref.listen(meProvider, (_, __) => refresh.value++);

  return GoRouter(
    refreshListenable: refresh,
    initialLocation: '/login',
    redirect: (context, state) {
      final signedIn = ref.read(signedInProvider);
      final atLogin = state.matchedLocation == '/login';
      final atChangePassword = state.matchedLocation == '/change-password';

      if (!signedIn) {
        return atLogin ? null : '/login';
      }
      final me = ref.read(meProvider).valueOrNull;
      if (me == null) {
        return null; // /me still loading
      }
      if (me.mustChangePassword && !atChangePassword) {
        return '/change-password';
      }
      if (!me.mustChangePassword && (atLogin || atChangePassword)) {
        return _home(me);
      }
      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
      GoRoute(
        path: '/change-password',
        builder: (_, __) => const ChangePasswordScreen(),
      ),
      ShellRoute(
        builder: (_, state, child) =>
            AdminShell(location: state.matchedLocation, child: child),
        routes: [
          GoRoute(
            path: AdminRoutes.tenants,
            builder: (_, __) => const TenantsScreen(),
          ),
          GoRoute(
            path: AdminRoutes.tenantUsersPattern,
            builder: (_, state) => TenantUsersScreen(
              tenantId: state.pathParameters['tenantId'] ?? '',
            ),
          ),
          GoRoute(
            path: AdminRoutes.users,
            builder: (_, __) => const UsersScreen(),
          ),
          GoRoute(
            path: AdminRoutes.roles,
            builder: (_, __) => const RolesScreen(),
          ),
          GoRoute(
            path: AdminRoutes.permissions,
            builder: (_, __) => const PermissionsScreen(),
          ),
        ],
      ),
      // The tenant console: the same screens, on the tenant plane, for a tenant that administers itself.
      // The tenant is the caller's — the routes carry no tenant, so there is none to get wrong.
      ShellRoute(
        builder: (_, state, child) => AdminShell(
          location: state.matchedLocation,
          sections: AdminSection.tenantValues,
          child: child,
        ),
        routes: [
          GoRoute(
            path: AdminRoutes.tenantUsers,
            builder: (_, __) => const UsersScreen(),
          ),
          GoRoute(
            path: AdminRoutes.tenantRoles,
            builder: (_, __) => const RolesScreen(),
          ),
          GoRoute(
            path: AdminRoutes.tenantPermissions,
            builder: (_, __) => const PermissionsScreen(),
          ),
        ],
      ),
      GoRoute(
        path: '/inventory',
        builder: (_, __) => const InventoryHomeScreen(),
      ),
    ],
  );
});

/// Where a signed-in caller belongs: the platform console, a tenant's own console, or the app UI when the
/// tenant holds no console permission at all.
String _home(Me me) {
  if (me.isPlatformAdmin) {
    return AdminRoutes.firstAllowed(me);
  }
  return AdminRoutes.firstAllowedTenant(me) ?? '/inventory';
}
