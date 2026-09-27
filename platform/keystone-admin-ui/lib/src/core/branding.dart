import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Product branding rendered by the hosted platform UI.
///
/// The platform UI is application-agnostic: it shows whatever the hosting application injects
/// through [appBrandingProvider]. An application overrides that provider once, at its
/// composition root, so the shared screens are branded for that application:
///
/// ```dart
/// ProviderScope(
///   overrides: [
///     appBrandingProvider.overrideWithValue(
///       const AppBranding(title: 'Keystone - Inventory Management'),
///     ),
///   ],
///   child: const App(),
/// );
/// ```
class AppBranding {
  const AppBranding({required this.title});

  /// Full product title shown to the user, e.g. `Keystone - Inventory Management`.
  final String title;

  /// Branding used when the hosting application injects none.
  static const AppBranding defaults = AppBranding(title: 'Keystone');
}

/// Branding shown by the shared platform screens; the hosting application overrides it.
final appBrandingProvider = Provider<AppBranding>(
  (ref) => AppBranding.defaults,
);
