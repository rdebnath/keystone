import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

import 'features/inventory/home_screen.dart';

/// Declarative router with an auth gate and platform-vs-tenant routing: signed-out → login;
/// first login → change password; platform user → admin dashboard; tenant user → inventory.
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
        return me.isPlatformAdmin ? '/' : '/inventory';
      }
      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
      GoRoute(
        path: '/change-password',
        builder: (_, __) => const ChangePasswordScreen(),
      ),
      GoRoute(path: '/', builder: (_, __) => const DashboardScreen()),
      GoRoute(
        path: '/inventory',
        builder: (_, __) => const InventoryHomeScreen(),
      ),
    ],
  );
});
