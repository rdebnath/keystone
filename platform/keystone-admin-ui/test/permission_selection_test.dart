import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// The picks a role editor holds: keyed by code, kept across paging and searching, and pruned to what the
/// backend's own grant rule (`RoleService.grantPermissions`) would accept for the role's owner and scope.
void main() {
  const globalRow = Permission(
    id: 'p1',
    code: 'tenant:user:read-only',
    scope: 'TENANT',
  );
  const acmeRow = Permission(
    id: 'p2',
    code: 'tenant:user:read-write',
    scope: 'TENANT',
    tenantId: 'acme',
  );
  const platformRow = Permission(
    id: 'p3',
    code: 'platform:tenant:read-only',
    scope: 'PLATFORM',
  );

  group('PermissionSelection', () {
    test('should_start_empty', () {
      expect(PermissionSelection.empty.isEmpty, isTrue);
      expect(PermissionSelection.empty.isNotEmpty, isFalse);
      expect(PermissionSelection.empty.codes, isEmpty);
    });

    test('should_pick_and_unpick_by_code', () {
      final picked = PermissionSelection.empty.toggle(globalRow, true);

      expect(picked.contains('tenant:user:read-only'), isTrue);
      expect(picked.length, 1);

      final unpicked = picked.toggle(globalRow, false);

      expect(unpicked.isEmpty, isTrue);
      // Immutable: the previous value keeps its pick.
      expect(picked.length, 1);
    });

    test('should_order_the_codes_so_the_body_ignores_the_click_order', () {
      final selection = PermissionSelection.empty
          .toggle(platformRow, true)
          .toggle(globalRow, true);

      expect(selection.codes, <String>[
        'platform:tenant:read-only',
        'tenant:user:read-only',
      ]);
    });

    test('should_remove_one_pick_for_a_chip_action', () {
      final selection = PermissionSelection.of(<Permission>[
        globalRow,
        platformRow,
      ]);

      expect(selection.remove('platform:tenant:read-only').codes, <String>[
        'tenant:user:read-only',
      ]);
    });

    test('should_keep_a_pick_the_role_may_still_hold', () {
      final result = PermissionSelection.of(<Permission>[
        acmeRow,
      ]).retaining(ownerId: 'acme', scope: 'TENANT');

      expect(result.selection.codes, <String>['tenant:user:read-write']);
      expect(result.dropped, isEmpty);
    });

    test('should_keep_a_global_pick_for_every_owner', () {
      // The catalogue is grantable to every owner: `ownerFilter` matches `tenant_id IS NULL` in every case.
      final selection = PermissionSelection.of(<Permission>[globalRow]);

      expect(
        selection.retaining(ownerId: 'acme', scope: 'TENANT').selection.length,
        1,
      );
      expect(
        selection.retaining(ownerId: null, scope: 'TENANT').selection.length,
        1,
      );
    });

    test('should_keep_only_the_catalogue_when_the_role_has_no_owner', () {
      // A global role's grants are held by every tenant, so a tenant-owned permission may not travel with it
      // (`RoleService.grantableTo`) — the picker never offers one, and a stale pick is dropped.
      final selection = PermissionSelection.of(<Permission>[acmeRow]);
      final result = selection.retaining(ownerId: null, scope: 'TENANT');

      expect(result.selection.isEmpty, isTrue);
      expect(result.dropped, <String>['tenant:user:read-write']);

      final catalogue = PermissionSelection.of(<Permission>[globalRow]);
      expect(
        catalogue.retaining(ownerId: null, scope: 'TENANT').selection.codes,
        <String>['tenant:user:read-only'],
      );
    });

    test('should_drop_a_pick_owned_by_another_tenant', () {
      final result = PermissionSelection.of(<Permission>[
        acmeRow,
      ]).retaining(ownerId: 'globex', scope: 'TENANT');

      expect(result.selection.isEmpty, isTrue);
      expect(result.dropped, <String>['tenant:user:read-write']);
    });

    test('should_drop_a_pick_of_another_scope', () {
      final result = PermissionSelection.of(<Permission>[
        globalRow,
      ]).retaining(ownerId: null, scope: 'PLATFORM');

      expect(result.selection.isEmpty, isTrue);
      expect(result.dropped, <String>['tenant:user:read-only']);
    });

    test('should_read_the_grants_a_role_already_holds', () {
      final role = Role(
        id: 'r1',
        code: 'auditor',
        scope: 'TENANT',
        tenantId: 'acme',
        permissions: const <String>['tenant:user:read-only'],
      );

      final selection = PermissionSelection.ofRole(role);

      expect(selection.codes, <String>['tenant:user:read-only']);
      // Held by *that* role, so still valid for it — and not for a role of another scope.
      expect(
        selection.retaining(ownerId: 'acme', scope: 'TENANT').dropped,
        isEmpty,
      );
      final otherScope = selection.retaining(
        ownerId: 'acme',
        scope: 'PLATFORM',
      );
      expect(otherScope.dropped, <String>['tenant:user:read-only']);
    });
  });
}
