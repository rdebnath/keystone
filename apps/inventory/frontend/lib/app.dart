import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

import 'router.dart';

/// App-specific branding injected into the hosted Keystone platform UI (`main.dart`).
const appBranding = AppBranding(title: 'Keystone - Inventory Management');

class App extends ConsumerWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    return MaterialApp.router(
      title: appBranding.title,
      theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
      routerConfig: router,
    );
  }
}
