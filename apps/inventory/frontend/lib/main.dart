import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

import 'app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(
    ProviderScope(
      // Brand the hosted platform UI for this application.
      overrides: [appBrandingProvider.overrideWithValue(appBranding)],
      child: const App(),
    ),
  );
}
