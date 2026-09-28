import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// The tenant labels the row resolves through the **unpaged** options list, as the console does.
final _tenants = OptionList<Tenant>(
  items: <Tenant>[
    const Tenant(id: 'acme', name: 'Acme', slug: 'acme'),
  ],
  truncated: false,
);

const _globalRow = Permission(
  id: 'p1',
  code: 'tenant:user:read-only',
  scope: 'TENANT',
);

const _acmeRow = Permission(
  id: 'p2',
  code: 'tenant:report:read-only',
  scope: 'TENANT',
  tenantId: 'acme',
);

Future<void> _pumpRow(WidgetTester tester, PermissionRow row) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        tenantOptionsProvider.overrideWith((ref) async => _tenants),
      ],
      child: MaterialApp(home: Scaffold(body: row)),
    ),
  );
  await tester.pumpAndSettle();
}

/// The catalogue row, shared by the Permissions screen and the role editor's picker — so the two cannot drift.
void main() {
  group('PermissionRow', () {
    testWidgets('should_render_the_code_scope_level_and_owner', (tester) async {
      await _pumpRow(
        tester,
        const PermissionRow(permission: _globalRow),
      );

      expect(find.text('tenant:user:read-only'), findsOneWidget);
      expect(find.text('TENANT · read-only · Global'), findsOneWidget);
      expect(find.byIcon(Icons.public), findsOneWidget);
      // The catalogue screen's row is read-only: no checkbox anywhere.
      expect(find.byType(CheckboxListTile), findsNothing);
    });

    testWidgets('should_name_the_tenant_that_owns_the_row', (tester) async {
      await _pumpRow(tester, const PermissionRow(permission: _acmeRow));

      expect(find.text('TENANT · read-only · Acme'), findsOneWidget);
      expect(find.byIcon(Icons.apartment_outlined), findsOneWidget);
    });

    testWidgets('should_report_a_toggle_when_it_is_selectable', (tester) async {
      bool? reported;
      await _pumpRow(
        tester,
        PermissionRow(
          permission: _globalRow,
          selected: true,
          onChanged: (value) => reported = value,
        ),
      );

      final tile = tester.widget<CheckboxListTile>(find.byType(CheckboxListTile));
      expect(tile.value, isTrue);
      expect(tile.onChanged, isNotNull);

      await tester.tap(find.byType(CheckboxListTile));
      await tester.pumpAndSettle();

      expect(reported, isFalse);
    });

    testWidgets('should_render_a_row_the_caller_may_not_grant_as_disabled', (
      tester,
    ) async {
      bool? reported;
      await _pumpRow(
        tester,
        PermissionRow(
          permission: _acmeRow,
          onChanged: (value) => reported = value,
          grantable: false,
        ),
      );

      expect(find.text('TENANT · read-only · Acme · you do not hold this'), findsOneWidget);
      final tile = tester.widget<CheckboxListTile>(find.byType(CheckboxListTile));
      // Both are needed: `CheckboxListTile` wires a tap handler from `onChanged` alone, so a disabled row
      // must pass null there or it would still toggle.
      expect(tile.onChanged, isNull);
      expect(tile.enabled, isFalse);

      await tester.tap(find.byType(CheckboxListTile));
      await tester.pumpAndSettle();

      expect(reported, isNull);
    });
  });
}
