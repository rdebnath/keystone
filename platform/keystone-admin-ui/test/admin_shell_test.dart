import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// A console router over the real [AdminShell] with stand-in section bodies, so the menu, the pane and
/// the permission gate can be exercised without a backend.
GoRouter _router(String initialLocation) {
  return GoRouter(
    initialLocation: initialLocation,
    routes: [
      ShellRoute(
        builder: (_, state, child) =>
            AdminShell(location: state.matchedLocation, child: child),
        routes: [
          GoRoute(
            path: AdminRoutes.tenants,
            builder: (_, __) => const Text('tenants body'),
          ),
          GoRoute(
            path: AdminRoutes.tenantUsersPattern,
            builder: (_, __) => const Text('tenant users body'),
          ),
          GoRoute(
            path: AdminRoutes.users,
            builder: (_, __) => const Text('users body'),
          ),
          GoRoute(
            path: AdminRoutes.roles,
            builder: (_, __) => const Text('roles body'),
          ),
          GoRoute(
            path: AdminRoutes.permissions,
            builder: (_, __) => const Text('permissions body'),
          ),
        ],
      ),
    ],
  );
}

Future<void> _pumpShell(
  WidgetTester tester,
  Me me, {
  String location = AdminRoutes.tenants,
  Size surface = const Size(1400, 900),
}) async {
  // A wide surface keeps the pane inline (below 800 dp it becomes a drawer).
  tester.view.physicalSize = surface;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        meProvider.overrideWith((ref) => me),
        appBrandingProvider.overrideWithValue(
          const AppBranding(title: 'Keystone - Inventory Management'),
        ),
      ],
      child: MaterialApp.router(routerConfig: _router(location)),
    ),
  );
  await tester.pumpAndSettle();
}

Me _me(List<String> permissions) =>
    Me(sub: 'sub-1', username: 'admin', permissions: permissions);

void main() {
  group('AdminShell', () {
    testWidgets('should_list_every_section_for_the_wildcard_holder', (
      tester,
    ) async {
      await _pumpShell(tester, _me([Permission.wildcard]));

      for (final section in AdminSection.values) {
        expect(find.text(section.label), findsWidgets);
      }
      expect(find.text('Platform · admin'), findsOneWidget);
      expect(find.text('tenants body'), findsOneWidget);
    });

    testWidgets('should_list_only_the_sections_the_caller_may_read', (
      tester,
    ) async {
      await _pumpShell(tester, _me(['platform:tenant:read-only']));

      expect(find.text('Tenants'), findsWidgets);
      expect(find.text('Users'), findsNothing);
      expect(find.text('Roles'), findsNothing);
      expect(find.text('Permissions'), findsNothing);
    });

    testWidgets('should_hide_and_show_the_pane_from_the_app_bar', (
      tester,
    ) async {
      await _pumpShell(tester, _me([Permission.wildcard]));
      expect(find.byTooltip('Hide menu'), findsOneWidget);
      expect(
        tester.getSize(find.byKey(AdminShell.paneKey)).width,
        greaterThan(0),
      );

      await tester.tap(find.byTooltip('Hide menu'));
      await tester.pumpAndSettle();

      // The pane collapses to zero width; the section body stays.
      expect(find.byTooltip('Show menu'), findsOneWidget);
      expect(tester.getSize(find.byKey(AdminShell.paneKey)).width, 0);
      expect(find.text('tenants body'), findsOneWidget);

      await tester.tap(find.byTooltip('Show menu'));
      await tester.pumpAndSettle();

      expect(find.byTooltip('Hide menu'), findsOneWidget);
      expect(
        tester.getSize(find.byKey(AdminShell.paneKey)).width,
        greaterThan(0),
      );
      expect(find.text('Sign out'), findsOneWidget);
    });

    testWidgets('should_highlight_the_section_of_a_nested_location', (
      tester,
    ) async {
      await _pumpShell(
        tester,
        _me([Permission.wildcard]),
        location: '/tenants/tenant-1',
      );

      final tile = tester.widget<ListTile>(
        find.ancestor(
          of: find.text('Tenants'),
          matching: find.byType(ListTile),
        ),
      );
      expect(tile.selected, isTrue);
      expect(find.text('tenant users body'), findsOneWidget);
    });

    testWidgets('should_refuse_a_section_the_caller_may_not_read', (
      tester,
    ) async {
      await _pumpShell(
        tester,
        _me(['platform:tenant:read-only']),
        location: AdminRoutes.roles,
      );

      expect(
        find.text('You do not have access to this section.'),
        findsOneWidget,
      );
      expect(find.text('Go to Tenants'), findsOneWidget);
      expect(find.text('roles body'), findsNothing);
    });

    testWidgets('should_explain_itself_when_no_section_is_readable', (
      tester,
    ) async {
      await _pumpShell(tester, _me(const []));

      expect(
        find.textContaining('do not have access to any section'),
        findsOneWidget,
      );
      expect(find.text('Sign out'), findsNothing);
    });

    testWidgets('should_open_the_change_password_dialog_from_the_pane', (
      tester,
    ) async {
      await _pumpShell(tester, _me([Permission.wildcard]));

      await tester.tap(find.text('Change password'));
      await tester.pumpAndSettle();

      // The voluntary flow asks for the current password; the append-only entry is not a section, so a
      // permission-less caller gets it too (see the next test).
      expect(find.text('Current password'), findsOneWidget);
      expect(find.text('New password'), findsOneWidget);
    });

    testWidgets('should_offer_change_password_even_without_any_section', (
      tester,
    ) async {
      await _pumpShell(tester, _me(const []));

      await tester.tap(find.byTooltip('Change password'));
      await tester.pumpAndSettle();

      expect(find.text('Current password'), findsOneWidget);
    });

    testWidgets('should_close_the_drawer_before_opening_the_dialog', (
      tester,
    ) async {
      // Below 800 dp the pane is an overlay drawer, which has to be closed before the dialog.
      await _pumpShell(
        tester,
        _me([Permission.wildcard]),
        surface: const Size(500, 900),
      );

      await tester.tap(find.byTooltip('Show menu'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Change password'));
      await tester.pumpAndSettle();

      expect(find.text('Sign out'), findsNothing);
      expect(find.text('Current password'), findsOneWidget);
    });
  });

  group('AdminRoutes', () {
    test('should_land_on_the_first_readable_section', () {
      expect(
        AdminRoutes.firstAllowed(_me(['platform:user:read-only'])),
        AdminRoutes.users,
      );
      expect(
        AdminRoutes.firstAllowed(_me([Permission.wildcard])),
        AdminRoutes.tenants,
      );
    });

    test('should_build_the_tenant_users_path', () {
      expect(AdminRoutes.forTenant('abc'), '/tenants/abc');
    });
  });
}
