import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'core/providers.dart';
import 'features/admin/dashboard_screen.dart';
import 'features/auth/auth_provider.dart';
import 'features/auth/change_password_screen.dart';
import 'features/auth/login_screen.dart';

/// Declarative router with an auth gate: signed-out → login; first login → change password;
/// otherwise → dashboard.
final routerProvider = Provider<GoRouter>((ref) {
  final refresh = ValueNotifier<int>(0);
  ref.onDispose(refresh.dispose);
  ref.listen(authStateProvider, (_, __) => refresh.value++);
  ref.listen(meProvider, (_, __) => refresh.value++);

  return GoRouter(
    refreshListenable: refresh,
    initialLocation: '/login',
    redirect: (context, state) {
      final signedIn =
          ref.read(authStateProvider).valueOrNull?.signedIn ?? false;
      final me = ref.read(meProvider).valueOrNull;
      final atLogin = state.matchedLocation == '/login';
      final atChangePassword = state.matchedLocation == '/change-password';

      if (!signedIn) {
        return atLogin ? null : '/login';
      }
      if (me != null && me.mustChangePassword && !atChangePassword) {
        return '/change-password';
      }
      if (me != null && !me.mustChangePassword && (atLogin || atChangePassword)) {
        return '/';
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
    ],
  );
});
