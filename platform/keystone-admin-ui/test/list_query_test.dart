import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// The list query: the value that is at once the provider family key, the URL payload and the "did the
/// query change?" check (`docs/CODING_GUIDELINES_FRONTEND.md` §7.3).
void main() {
  group('ListQuery', () {
    test('should_be_equal_by_value_so_a_rebuild_does_not_refetch', () {
      const a = ListQuery(search: 'acme', page: 2, size: 50, sort: 'name');
      const b = ListQuery(search: 'acme', page: 2, size: 50, sort: 'name');

      // This is what makes `autoDispose.family` cache per *query* instead of per widget instance.
      expect(a, equals(b));
      expect(a.hashCode, equals(b.hashCode));
      expect(a, isNot(equals(b.copyWith(page: 3))));
    });

    test('should_send_only_the_parameters_that_mean_something', () {
      // A blank search is no search, and no sort key means the resource's own default order: sending either
      // would put noise in the URL and in the server's logs.
      expect(const ListQuery().toQueryParameters(), {
        'page': '0',
        'size': '25',
      });

      expect(
        const ListQuery(
          tenantId: 'tenant-1',
          search: 'acme',
          page: 2,
          size: 50,
          sort: 'name',
          order: 'desc',
          scope: 'TENANT',
        ).toQueryParameters(),
        {
          'q': 'acme',
          'page': '2',
          'size': '50',
          'sort': 'name',
          'order': 'desc',
          'tenantId': 'tenant-1',
          'scope': 'TENANT',
        },
      );
    });

    test('should_return_to_the_first_page_when_the_result_set_changes', () {
      const onPageSeven = ListQuery(search: 'acme', page: 6);

      // Leaving the user on page 7 of a now-2-page result set is the bug the guidelines call out (§1.4).
      expect(onPageSeven.withSearch('acme corp').page, 0);
      expect(onPageSeven.withTenant('tenant-1').page, 0);
      expect(onPageSeven.withScope('TENANT').page, 0);
      expect(onPageSeven.withSort('name', 'desc').page, 0);
      expect(onPageSeven.withSize(100).page, 0);
    });

    test('should_keep_the_page_when_only_the_page_moves', () {
      const query = ListQuery(search: 'acme', size: 50);

      // Paging and dropping an unnameable filter are not result-set changes.
      expect(query.onPage(3), const ListQuery(search: 'acme', size: 50, page: 3));
      expect(query.withoutTenant().page, 0);
      expect(const ListQuery(page: 3, size: 50).withoutTenant().page, 3);
    });

    test('should_clear_a_filter_rather_than_leave_it_behind', () {
      const filtered = ListQuery(
        tenantId: 'tenant-1',
        scope: 'TENANT',
        search: 'acme',
        page: 4,
      );

      final cleared = filtered.withTenant(null).withScope(null);

      expect(cleared.tenantId, isNull);
      expect(cleared.scope, isNull);
      // Clearing a filter is a new result set, so it starts at the beginning.
      expect(cleared.page, 0);
      expect(cleared.search, 'acme');
    });

    test('should_seed_a_role_pickers_query_with_the_roles_owner_and_scope', () {
      // The picker's two facts come from the role, never from the user: a scope filter would let them pick a
      // row the backend refuses on save (`RoleService.grantableTo`).
      final owned = ListQuery.permissionsFor(ownerId: 'acme', scope: 'TENANT');

      expect(owned.tenantId, 'acme');
      expect(owned.scope, 'TENANT');
      expect(owned.page, 0);
      expect(owned.size, 25);
      expect(owned.sort, isNull, reason: 'the server\'s catalogue-first order');
      expect(owned.toQueryParameters(), <String, String>{
        'page': '0',
        'size': '25',
        'tenantId': 'acme',
        'scope': 'TENANT',
      });

      // A global role has no owner to name, so it is seeded with the reserved platform tenant: the global
      // catalogue is all it may hold. On the tenant plane that value is dropped by `ConsoleScope` instead,
      // because the route derives the tenant from the caller.
      final global = ListQuery.permissionsFor(scope: 'PLATFORM');
      expect(global.tenantId, Tenant.platformId);
      expect(global.toQueryParameters(), <String, String>{
        'page': '0',
        'size': '25',
        'tenantId': Tenant.platformId,
        'scope': 'PLATFORM',
      });
    });

    test('should_carry_the_access_level_filter_and_return_to_the_first_page', () {
      // The level is the last segment of a code, so the filter is the one every permission row shows.
      expect(
        const ListQuery().withAccess('read-only').toQueryParameters(),
        <String, String>{'page': '0', 'size': '25', 'access': 'read-only'},
      );

      const onPageSeven = ListQuery(
        tenantId: 'acme',
        scope: 'TENANT',
        access: 'read-write',
        search: 'acme',
        page: 6,
      );

      expect(onPageSeven.withAccess('read-only').page, 0);
      expect(onPageSeven.withAccess('read-only').access, 'read-only');
      // …and the other mutators never lose it, exactly as they never lose the scope.
      expect(onPageSeven.withSearch('widget').access, 'read-write');
      expect(onPageSeven.withScope(null).access, 'read-write');
      expect(onPageSeven.withSort('code', 'desc').access, 'read-write');
      expect(onPageSeven.withSize(50).access, 'read-write');
      expect(onPageSeven.withTenant('globex').access, 'read-write');
      expect(onPageSeven.withoutTenant().access, 'read-write');
      expect(onPageSeven.onPage(2).access, 'read-write');
    });
  });

  group('ListQueryLocation', () {
    test('should_read_the_query_a_location_carries', () {
      final query = ListQueryLocation.fromQueryParameters(const {
        'q': 'acme',
        'page': '2',
        'size': '50',
        'sort': 'name',
        'order': 'desc',
        'tenantId': 'tenant-1',
        'scope': 'TENANT',
      });

      expect(query.search, 'acme');
      expect(query.page, 2);
      expect(query.size, 50);
      expect(query.sort, 'name');
      expect(query.order, 'desc');
      expect(query.tenantId, 'tenant-1');
      expect(query.scope, 'TENANT');
    });

    test('should_fall_back_to_the_defaults_for_a_hand_edited_url', () {
      // A URL is user input: a nonsense page or size must not reach the server as nonsense (the backend
      // validates it again anyway).
      final query = ListQueryLocation.fromQueryParameters(const {
        'q': '   ',
        'page': '-4',
        'size': 'zero',
        'sort': '',
        'order': 'sideways',
      });

      expect(query.search, '');
      expect(query.page, 0);
      expect(query.size, 25);
      expect(query.sort, isNull);
      expect(query.order, 'asc');
    });

    test('should_round_trip_a_query_through_the_url', () {
      const original = ListQuery(
        tenantId: 'tenant-1',
        search: 'acme',
        page: 3,
        size: 50,
        sort: 'code',
        order: 'desc',
        scope: 'TENANT',
      );

      final read = ListQueryLocation.fromQueryParameters(
        original.toQueryParameters(),
      );

      expect(read, equals(original));
    });
  });
}
