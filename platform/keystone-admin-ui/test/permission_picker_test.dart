import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

const _platformTenantId = '00000000-0000-0000-0000-000000000000';

final _tenants = <Tenant>[
  const Tenant(
    id: _platformTenantId,
    name: 'Keystone',
    slug: 'keystone',
    platform: true,
  ),
  const Tenant(id: 'acme', name: 'Acme', slug: 'acme'),
];

/// The catalogue the fake backend holds: 43 `TENANT`-scope rows (40 global, the user grant, then two Acme
/// owned) and 3 `PLATFORM`-scope ones. 43 is deliberate — a 25-row page then needs a second page, and
/// `tenant:user:read-only` sorts onto it, so a **server-side** search is the only way to reach it.
final _catalogue = <Permission>[
  for (var i = 0; i < 40; i++)
    Permission(
      id: 'g$i',
      code: 'tenant:item${i.toString().padLeft(2, '0')}:read-only',
      scope: 'TENANT',
    ),
  const Permission(
    id: 'g-user',
    code: 'tenant:user:read-only',
    scope: 'TENANT',
  ),
  const Permission(
    id: 'a1',
    code: 'tenant:report:read-only',
    scope: 'TENANT',
    tenantId: 'acme',
  ),
  const Permission(
    id: 'a2',
    code: 'tenant:widget:read-write',
    scope: 'TENANT',
    tenantId: 'acme',
  ),
  for (var i = 0; i < 3; i++)
    Permission(
      id: 'p$i',
      code: 'platform:item$i:read-only',
      scope: 'PLATFORM',
    ),
];

final _auditorRole = Role(
  id: 'r-auditor',
  code: 'auditor',
  scope: 'TENANT',
  tenantId: 'acme',
  permissions: const <String>['tenant:user:read-only'],
);

final _seededRole = const Role(
  id: 'r-platform-admin',
  code: 'platform-admin',
  scope: 'PLATFORM',
  permissions: <String>['*'],
);

/// A backend stand-in that answers both list routes the way the **server** does — filtered, ordered and
/// paged in SQL, with the totals it computed — and records every request, so a test can assert what the
/// console *asked for* instead of what it did locally.
final class _CatalogueAdapter implements HttpClientAdapter {
  _CatalogueAdapter({this.forbidden = false, this.empty = false});

  /// Answers the catalogue with a `403` problem detail, to prove the picker surfaces the server's message.
  final bool forbidden;

  /// Holds no permission at all — the "nothing exists in this scope yet" state.
  final bool empty;

  final List<RequestOptions> requests = <RequestOptions>[];

  List<RequestOptions> get catalogueRequests => requests
      .where((request) => request.uri.path.endsWith('/permissions'))
      .toList(growable: false);

  RequestOptions get lastCatalogueRequest => catalogueRequests.last;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    final path = options.uri.path;
    if (path.endsWith('/permissions')) {
      return forbidden
          ? _json(
              <String, dynamic>{
                'detail': 'Not allowed to read platform:permission',
              },
              status: 403,
            )
          : _json(_cataloguePage(options.uri.queryParameters));
    }
    if (path.endsWith('/options')) {
      return _json(<String, dynamic>{
        'items': _tenants.map((tenant) => tenant.toJson()).toList(),
        'truncated': false,
      });
    }
    if (path.endsWith('/roles') && options.method == 'POST') {
      return _json((options.data as Map<String, dynamic>), status: 201);
    }
    if (path.contains('/roles/')) {
      return _json(_auditorRole.toJson());
    }
    final roles = <Role>[_seededRole, _auditorRole];
    return _json(<String, dynamic>{
      'items': roles.map((role) => role.toJson()).toList(),
      'page': 0,
      'size': 25,
      'totalElements': roles.length,
      'totalPages': 1,
      'hasNext': false,
      'hasPrevious': false,
    });
  }

  /// The one page the server would answer for [query]: the same filters, order and window, and the totals
  /// derived from the whole catalogue rather than from the page.
  Map<String, dynamic> _cataloguePage(Map<String, String> query) {
    final scope = query['scope'];
    final owner = query['tenantId'];
    final access = query['access'];
    final search = (query['q'] ?? '').toLowerCase();
    final size = int.parse(query['size'] ?? '25');
    final page = int.parse(query['page'] ?? '0');
    final matching = (empty ? <Permission>[] : _catalogue).where((row) {
      if (scope != null && row.scope != scope) {
        return false;
      }
      if (owner == _platformTenantId && !row.isGlobal) {
        return false;
      }
      if (owner != null &&
          owner != _platformTenantId &&
          !row.isGlobal &&
          row.tenantId != owner) {
        return false;
      }
      // The level filter is a suffix match on the code — the wildcard carries no level, so it is in neither
      // level's set, exactly as `PermissionService.accessFilter` decides it.
      if (access != null && !row.code.endsWith(':$access')) {
        return false;
      }
      return search.isEmpty || row.code.toLowerCase().contains(search);
    }).toList()..sort((a, b) {
      // The resource's default order: the global catalogue first, then code — as `PermissionService` does.
      if (a.isGlobal != b.isGlobal) {
        return a.isGlobal ? -1 : 1;
      }
      return a.code.compareTo(b.code);
    });
    final totalPages = (matching.length / size).ceil();
    final window = matching.skip(page * size).take(size).toList();
    return <String, dynamic>{
      'items': window.map((row) => row.toJson()).toList(),
      'page': page,
      'size': size,
      'totalElements': matching.length,
      'totalPages': totalPages,
      'hasNext': page + 1 < totalPages,
      'hasPrevious': page > 0,
    };
  }

  ResponseBody _json(Map<String, dynamic> body, {int status = 200}) =>
      ResponseBody.fromString(
        jsonEncode(body),
        status,
        headers: <String, List<String>>{
          Headers.contentTypeHeader: <String>[Headers.jsonContentType],
        },
      );

  @override
  void close({bool force = false}) {}
}

/// A platform admin: it may read the catalogue (which the picker needs) and write roles.
final _platformAdmin = Me(
  sub: 's-platform',
  username: 'admin',
  permissions: <String>['platform:role:read-write', 'platform:permission:read-only'],
);

/// A tenant admin that holds only some of its plane's grants, so the picker can be shown refusing what it may
/// not hand out — and honouring write-implies-read for what it does.
final _tenantAdmin = Me(
  sub: 's-tenant',
  username: 'acme-admin',
  tenantId: 'acme',
  permissions: <String>[
    'tenant:role:read-write',
    'tenant:permission:read-only',
    // Only this one item's write grant: `tenant:item00:read-only` follows from it, nothing else does.
    'tenant:item00:read-write',
  ],
);

/// The picker's host: the fake backend that recorded the requests, and what the picker returned —
/// `cancelled` when it was dismissed instead.
typedef _PickerHost = ({_CatalogueAdapter adapter, ValueNotifier<String> applied});

/// Pumps a host that opens the picker **directly**, so the picker's own controls are the only ones in the
/// tree and every finder below is unambiguous.
Future<_PickerHost> _pumpPicker(
  WidgetTester tester, {
  String? ownerId,
  String scope = 'TENANT',
  Me? me,
  bool forbidden = false,
  bool empty = false,
  PermissionSelection selected = PermissionSelection.empty,
}) async {
  tester.view.physicalSize = const Size(1400, 1000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final adapter = _CatalogueAdapter(forbidden: forbidden, empty: empty);
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/inventory'))
    ..httpClientAdapter = adapter;
  final applied = ValueNotifier<String>('none');

  await tester.pumpWidget(
    ProviderScope(
      overrides: <Override>[
        apiClientProvider.overrideWithValue(ApiClient(dio)),
        tenantApiClientProvider.overrideWithValue(
          ApiClient(dio, basePath: '/api/v1/tenant'),
        ),
        meProvider.overrideWith((ref) => me ?? _platformAdmin),
        tenantOptionsProvider.overrideWith(
          (ref) async => OptionList<Tenant>(items: _tenants, truncated: false),
        ),
      ],
      child: MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  ElevatedButton(
                    onPressed: () async {
                      final picked = await showPermissionPicker(
                        context,
                        ownerId: ownerId,
                        scope: scope,
                        selected: selected,
                      );
                      applied.value = picked == null
                          ? 'cancelled'
                          : picked.codes.join(',');
                    },
                    child: const Text('open picker'),
                  ),
                  ValueListenableBuilder<String>(
                    valueListenable: applied,
                    builder: (context, value, _) => Text('applied: $value'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.text('open picker'));
  await tester.pumpAndSettle();
  return (adapter: adapter, applied: applied);
}

/// The picker's search box — the only [TextField] the picker itself owns.
Finder get _pickerSearch => find.descendant(
  of: find.byType(SearchField),
  matching: find.byType(TextField),
);

/// The catalogue row for [code].
Finder _rowFor(String code) => find.widgetWithText(CheckboxListTile, code);



/// The catalogue row for [code], as the picker rendered it.
CheckboxListTile _catalogueTile(WidgetTester tester, String code) =>
    tester.widget<CheckboxListTile>(_rowFor(code));

void main() {
  group('Role permission picker', () {
    testWidgets('should_seed_the_catalogue_with_the_roles_owner_and_scope', (
      tester,
    ) async {
      final host = await _pumpPicker(tester, ownerId: 'acme');

      expect(
        host.adapter.lastCatalogueRequest.uri.queryParameters,
        <String, String>{
          'scope': 'TENANT',
          'page': '0',
          'size': '25',
          'tenantId': 'acme',
        },
      );
      expect(find.text('tenant:item00:read-only'), findsOneWidget);
      // A PLATFORM row is not in a TENANT role's catalogue, and the query never asked for it.
      expect(find.text('platform:item0:read-only'), findsNothing);
    });

    testWidgets('should_search_the_whole_catalogue_on_the_server', (
      tester,
    ) async {
      final host = await _pumpPicker(tester, ownerId: 'acme');
      // `tenant:user:read-only` sorts onto page 2, so only a search the **server** ran can produce it.
      expect(find.text('tenant:user:read-only'), findsNothing);

      await tester.enterText(_pickerSearch, 'user');
      await tester.pump(const Duration(milliseconds: 400));
      await tester.pumpAndSettle();

      expect(
        host.adapter.lastCatalogueRequest.uri.queryParameters['q'],
        'user',
      );
      expect(find.text('tenant:user:read-only'), findsOneWidget);
      expect(find.text('1–1 of 1 · Page 1 of 1'), findsOneWidget);
      expect(find.text('tenant:item00:read-only'), findsNothing);
    });

    testWidgets('should_describe_the_result_set_and_page_through_it', (
      tester,
    ) async {
      final host = await _pumpPicker(tester, ownerId: 'acme');

      expect(find.text('1–25 of 43 · Page 1 of 2'), findsOneWidget);

      await tester.tap(find.byTooltip('Next page'));
      await tester.pumpAndSettle();

      expect(
        host.adapter.lastCatalogueRequest.uri.queryParameters['page'],
        '1',
      );
      expect(find.text('26–43 of 43 · Page 2 of 2'), findsOneWidget);
      // The window really moved: page 2 starts where page 1 ended.
      expect(find.text('tenant:item00:read-only'), findsNothing);
      expect(find.text('tenant:item25:read-only'), findsOneWidget);
    });

    testWidgets('should_page_by_the_chosen_size', (tester) async {
      final host = await _pumpPicker(tester, ownerId: 'acme');

      await tester.tap(find.byType(DropdownButton<int>));
      await tester.pumpAndSettle();
      await tester.tap(find.text('50 / page').last);
      await tester.pumpAndSettle();

      final query = host.adapter.lastCatalogueRequest.uri.queryParameters;
      expect(query['size'], '50');
      // A new size is a new result set, so it starts at the beginning (§1.4).
      expect(query['page'], '0');
      expect(find.text('1–43 of 43 · Page 1 of 1'), findsOneWidget);
    });

    testWidgets('should_sort_on_the_server', (tester) async {
      final host = await _pumpPicker(tester, ownerId: 'acme');

      await tester.tap(
        // Scoped to the sort control: the level filter beside it is the same widget type.
        find.descendant(
          of: find.byType(SortSelect),
          matching: find.byType(DropdownButtonFormField<String?>),
        ),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.text('Code').last);
      await tester.pumpAndSettle();

      expect(
        host.adapter.lastCatalogueRequest.uri.queryParameters['sort'],
        'code',
      );

      await tester.tap(find.byTooltip('Sort descending'));
      await tester.pumpAndSettle();

      final query = host.adapter.lastCatalogueRequest.uri.queryParameters;
      expect(query['sort'], 'code');
      expect(query['order'], 'desc');
    });

    testWidgets('should_hide_the_owner_filter_for_a_role_that_has_one', (
      tester,
    ) async {
      await _pumpPicker(tester, ownerId: 'acme');

      // The owner is fixed by the role, so a control with one legal value would only be noise (§1.6).
      expect(find.byType(OwnerFilter), findsNothing);
      expect(find.textContaining('owned by Acme'), findsOneWidget);
    });

    testWidgets('should_open_a_global_role_on_the_global_catalogue', (
      tester,
    ) async {
      final host = await _pumpPicker(tester, scope: 'TENANT');

      // A role with no owner may hold the catalogue only (`RoleService.grantableTo`), so the picker is seeded
      // with the reserved platform tenant — and never offers another tenant's own permissions.
      expect(
        host.adapter.lastCatalogueRequest.uri.queryParameters['tenantId'],
        Tenant.platformId,
      );
      expect(find.byType(OwnerFilter), findsNothing);
      expect(find.textContaining('global catalog only'), findsOneWidget);
      expect(find.text('tenant:report:read-only'), findsNothing);
      expect(find.text('tenant:item00:read-only'), findsOneWidget);
    });

    testWidgets('should_filter_the_catalogue_by_access_level', (tester) async {
      final host = await _pumpPicker(tester, ownerId: 'acme');
      expect(find.byType(AccessFilter), findsOneWidget);

      await tester.tap(
        find.descendant(
          of: find.byType(AccessFilter),
          matching: find.byType(DropdownButtonFormField<String?>),
        ),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.text('read/write').last);
      await tester.pumpAndSettle();

      expect(
        host.adapter.lastCatalogueRequest.uri.queryParameters['access'],
        'read-write',
      );
      // The filter is the server's: only the read/write rows come back, and they are what is rendered.
      expect(find.text('tenant:widget:read-write'), findsOneWidget);
      expect(find.text('tenant:item00:read-only'), findsNothing);
      expect(find.text('1–1 of 1 · Page 1 of 1'), findsOneWidget);
    });

    testWidgets('should_state_the_scope_instead_of_offering_it_as_a_filter', (
      tester,
    ) async {
      await _pumpPicker(tester, ownerId: 'acme');

      // There is no scope filter to widen the picker past what the role may hold.
      expect(find.text('Scope'), findsNothing);
      expect(find.textContaining('Scope TENANT · owned by Acme'), findsOneWidget);
    });

    testWidgets('should_keep_a_pick_made_on_another_page', (tester) async {
      await _pumpPicker(tester, ownerId: 'acme');

      await tester.tap(_rowFor('tenant:item00:read-only'));
      await tester.pumpAndSettle();
      expect(find.text('1 selected'), findsOneWidget);

      await tester.tap(find.byTooltip('Next page'));
      await tester.pumpAndSettle();
      await tester.tap(_rowFor('tenant:item25:read-only'));
      await tester.pumpAndSettle();

      expect(find.text('2 selected'), findsOneWidget);

      await tester.tap(find.text('Apply'));
      await tester.pumpAndSettle();

      // Both picks come back, ordered by code — paging never dropped the first one.
      expect(
        find.text('applied: tenant:item00:read-only,tenant:item25:read-only'),
        findsOneWidget,
      );
    });

    testWidgets('should_remove_a_pick_from_its_chip', (tester) async {
      await _pumpPicker(tester, ownerId: 'acme');
      await tester.tap(_rowFor('tenant:item00:read-only'));
      await tester.pumpAndSettle();

      await tester.tap(find.byTooltip('Remove tenant:item00:read-only'));
      await tester.pumpAndSettle();

      expect(find.text('No permissions selected'), findsOneWidget);

      await tester.tap(find.text('Apply'));
      await tester.pumpAndSettle();

      expect(find.text('applied: '), findsOneWidget);
    });

    testWidgets('should_disable_a_permission_the_caller_may_not_grant', (
      tester,
    ) async {
      final host = await _pumpPicker(tester, me: _tenantAdmin);

      // A tenant console names no tenant: its route derives it from the caller.
      expect(
        host.adapter.lastCatalogueRequest.uri.queryParameters.containsKey(
          'tenantId',
        ),
        isFalse,
      );
      // Held through write-implies-read (`tenant:item00:read-write`), so it may be handed on.
      expect(
        _catalogueTile(tester, 'tenant:item00:read-only').onChanged,
        isNotNull,
      );
      // Not held at all: offered **disabled, with the reason**, instead of selected and then refused.
      expect(
        _catalogueTile(tester, 'tenant:item01:read-only').onChanged,
        isNull,
      );
      expect(
        find.text('TENANT · read-only · Global · you do not hold this'),
        findsWidgets,
      );
    });

    testWidgets('should_show_the_servers_message_with_a_retry', (tester) async {
      final host = await _pumpPicker(tester, forbidden: true);

      expect(find.text('Not allowed to read platform:permission'), findsOneWidget);
      final before = host.adapter.catalogueRequests.length;

      await tester.tap(find.text('Retry'));
      await tester.pumpAndSettle();

      expect(host.adapter.catalogueRequests.length, before + 1);
    });

    testWidgets('should_show_the_empty_catalogue_state', (tester) async {
      await _pumpPicker(tester, empty: true);

      // The collection's own empty state — not the "nothing matches" one, which offers to clear a search.
      expect(find.text('No permissions in this scope yet.'), findsOneWidget);
    });

  });

  group('Roles screen', () {
    testWidgets('should_pick_the_permissions_instead_of_typing_them', (
      tester,
    ) async {
      await _pumpRolesScreen(tester);

      await tester.tap(find.text('Add role'));
      await tester.pumpAndSettle();

      expect(find.text('Permissions (comma-separated)'), findsNothing);
      expect(find.text('No permissions selected'), findsOneWidget);
      expect(find.text('Choose permissions'), findsOneWidget);
    });

    testWidgets('should_create_a_role_with_the_permissions_it_picked', (
      tester,
    ) async {
      final adapter = await _pumpRolesScreen(tester);

      await tester.tap(find.text('Add role'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField), 'catalogue-auditor');
      await tester.tap(find.text('Choose permissions'));
      await tester.pumpAndSettle();
      // The default scope on the platform plane is PLATFORM, whose whole catalogue is three global rows.
      await tester.tap(_rowFor('platform:item1:read-only'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Apply'));
      await tester.pumpAndSettle();

      expect(find.text('1 selected · platform:item1:read-only'), findsOneWidget);

      await tester.tap(find.text('Create'));
      await tester.pumpAndSettle();

      final post = adapter.requests.lastWhere(
        (request) => request.method == 'POST',
      );
      expect(post.uri.path, '/inventory/api/v1/roles');
      expect(post.data, <String, dynamic>{
        'code': 'catalogue-auditor',
        'scope': 'PLATFORM',
        'tenantId': null,
        'permissions': <String>['platform:item1:read-only'],
      });
    });

    testWidgets('should_drop_a_pick_the_new_scope_cannot_use_and_say_so', (
      tester,
    ) async {
      final adapter = await _pumpRolesScreen(tester);

      await tester.tap(find.text('Add role'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField), 'auditor');
      await tester.tap(find.text('Choose permissions'));
      await tester.pumpAndSettle();
      await tester.tap(_rowFor('platform:item0:read-only'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Apply'));
      await tester.pumpAndSettle();

      // A PLATFORM pick cannot ride along into a TENANT-scope role: it is dropped, and named.
      await tester.tap(find.byType(DropdownButton<String>));
      await tester.pumpAndSettle();
      await tester.tap(find.text('TENANT').last);
      await tester.pumpAndSettle();

      expect(find.textContaining('1 permission(s) removed'), findsOneWidget);
      expect(find.text('No permissions selected'), findsOneWidget);

      await tester.tap(find.text('Create'));
      await tester.pumpAndSettle();

      final post = adapter.requests.lastWhere(
        (request) => request.method == 'POST',
      );
      final body = post.data as Map<String, dynamic>;
      expect(body['scope'], 'TENANT');
      expect(body['permissions'], isEmpty);
    });

    testWidgets('should_edit_a_role_and_send_only_what_may_change', (
      tester,
    ) async {
      final adapter = await _pumpRolesScreen(tester);

      await tester.tap(find.byTooltip('Edit role'));
      await tester.pumpAndSettle();

      // Prefilled, with the grants it holds — and with the owner and scope *stated*, since both are fixed.
      expect(find.widgetWithText(TextFormField, 'auditor'), findsOneWidget);
      expect(find.text('Acme'), findsOneWidget);
      expect(find.text('TENANT'), findsOneWidget);
      expect(find.text('1 selected · tenant:user:read-only'), findsOneWidget);
      expect(find.text('Change permissions (1)'), findsOneWidget);

      await tester.tap(find.text('Save'));
      await tester.pumpAndSettle();

      final patch = adapter.requests.lastWhere(
        (request) => request.method == 'PATCH',
      );
      expect(patch.uri.path, '/inventory/api/v1/roles/r-auditor');
      // No `tenantId`: a role's owner is immutable and never part of the update.
      expect(patch.data, <String, dynamic>{
        'code': 'auditor',
        'scope': 'TENANT',
        'permissions': <String>['tenant:user:read-only'],
      });
    });

    testWidgets('should_not_offer_edit_for_a_seeded_role', (tester) async {
      await _pumpRolesScreen(tester);

      // Only the editable role has the action: the seeded one is immutable server-side.
      expect(find.byTooltip('Edit role'), findsOneWidget);
      expect(
        find.descendant(
          of: find.widgetWithText(ListTile, 'platform-admin'),
          matching: find.byTooltip('Edit role'),
        ),
        findsNothing,
      );
    });

    testWidgets('should_withhold_the_picker_without_the_read_grant', (
      tester,
    ) async {
      final adapter = await _pumpRolesScreen(
        tester,
        me: Me(
          sub: 's-writer',
          username: 'role-writer',
          permissions: <String>['platform:role:read-write'],
        ),
      );

      await tester.tap(find.text('Add role'));
      await tester.pumpAndSettle();

      final button = tester.widget<TextButton>(
        find.ancestor(
          of: find.text('Choose permissions'),
          matching: find.byType(TextButton),
        ),
      );
      expect(button.onPressed, isNull);
      expect(
        find.textContaining(
          'You need platform:permission:read-only (or read/write)',
        ),
        findsOneWidget,
      );
      // Nothing was asked for, so a caller that cannot read the catalogue never sees a failing request.
      expect(adapter.catalogueRequests, isEmpty);
    });

  });

}

/// Pumps the **real** Roles screen, so the create/edit dialog, its owner and scope controls and the picker's
/// wiring are exercised together. The screen's own list toolbar and pager are in the tree as well, so a
/// finder that must be unique is scoped by type — or by `.last`, which is the dialog's copy.
Future<_CatalogueAdapter> _pumpRolesScreen(
  WidgetTester tester, {
  Me? me,
}) async {
  tester.view.physicalSize = const Size(1400, 1000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final adapter = _CatalogueAdapter();
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/inventory'))
    ..httpClientAdapter = adapter;

  await tester.pumpWidget(
    ProviderScope(
      overrides: <Override>[
        apiClientProvider.overrideWithValue(ApiClient(dio)),
        tenantApiClientProvider.overrideWithValue(
          ApiClient(dio, basePath: '/api/v1/tenant'),
        ),
        meProvider.overrideWith((ref) => me ?? _platformAdmin),
        tenantOptionsProvider.overrideWith(
          (ref) async => OptionList<Tenant>(items: _tenants, truncated: false),
        ),
      ],
      child: const MaterialApp(home: RolesScreen()),
    ),
  );
  await tester.pumpAndSettle();
  return adapter;
}

