import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// Answers every request with a saved user and records it, so the dialog can be driven without a
/// backend.
final class _RecordingAdapter implements HttpClientAdapter {
  final List<RequestOptions> requests = [];
  final List<String> methods = [];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    methods.add(options.method);
    return ResponseBody.fromString(
      jsonEncode({
        'id': 'user-1',
        'sub': 'sub-1',
        'username': 'alice-b',
        'email': 'alice@acme.com',
        'tenantId': 'tenant-1',
        'mustChangePassword': false,
        'roles': <String>[],
      }),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

const _platformTenantId = '00000000-0000-0000-0000-000000000000';

final _tenants = <Tenant>[
  const Tenant(
    id: _platformTenantId,
    name: 'Keystone',
    slug: 'keystone',
    platform: true,
  ),
  const Tenant(id: 'tenant-1', name: 'Acme', slug: 'acme'),
];

final _roles = <Role>[
  const Role(id: 'r1', code: 'platform-admin', scope: 'PLATFORM'),
  const Role(
    id: 'r2',
    code: 'tenant-viewer',
    scope: 'TENANT',
    permissions: ['tenant:user:read-only'],
  ),
  const Role(id: 'r3', code: 'tenant-editor', scope: 'TENANT'),
];

final _alice = User(
  id: 'user-1',
  sub: 'sub-1',
  username: 'alice',
  email: 'alice@acme.com',
  phoneNumber: '+919876543210',
  tenantId: 'tenant-1',
  roles: const ['tenant-viewer'],
);

/// The picker sets and pages the screens read, in the shapes the backend returns
/// (`docs/CODING_GUIDELINES_BACKEND.md` §8). A picker input is **not** a page: it must carry every choice.
final _tenantOptions = OptionList<Tenant>(items: _tenants, truncated: false);
final _roleOptions = OptionList<Role>(items: _roles, truncated: false);
final _emptyUsers = Paged<User>(
  items: const <User>[],
  page: 0,
  size: 25,
  totalElements: 0,
  totalPages: 0,
  hasNext: false,
  hasPrevious: false,
);

/// One page holding [items], as a list read returns it.
Paged<T> _pageOf<T>(List<T> items) => Paged<T>(
  items: items,
  page: 0,
  size: 25,
  totalElements: items.length,
  totalPages: items.isEmpty ? 0 : 1,
  hasNext: false,
  hasPrevious: false,
);

Future<_RecordingAdapter> _pumpEditor(
  WidgetTester tester, {
  Tenant? tenant,
  User? existing,
}) async {
  tester.view.physicalSize = const Size(1200, 1000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final adapter = _RecordingAdapter();
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/inventory'))
    ..httpClientAdapter = adapter;

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        apiClientProvider.overrideWithValue(ApiClient(dio)),
        tenantOptionsProvider.overrideWith((ref) async => _tenantOptions),
        roleOptionsProvider.overrideWith((ref, tenantId) async => _roleOptions),
        usersPageProvider.overrideWith((ref, query) async => _emptyUsers),
      ],
      child: MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => Center(
              child: ElevatedButton(
                onPressed: () =>
                    showUserEditor(context, tenant: tenant, existing: existing),
                child: const Text('open editor'),
              ),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.text('open editor'));
  await tester.pumpAndSettle();
  return adapter;
}

/// Pumps the user list — the host of the "Reset password" row action.
Future<_RecordingAdapter> _pumpUserList(
  WidgetTester tester, {
  bool canManage = true,
}) async {
  tester.view.physicalSize = const Size(1200, 1000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final adapter = _RecordingAdapter();
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/inventory'))
    ..httpClientAdapter = adapter;

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        apiClientProvider.overrideWithValue(ApiClient(dio)),
        tenantOptionsProvider.overrideWith((ref) async => _tenantOptions),
        usersPageProvider.overrideWith((ref, query) async => _pageOf([_alice])),
      ],
      child: MaterialApp(
        home: Scaffold(
          body: UserList(
            query: const ListQuery(),
            onQueryChanged: (_) {},
            canManage: canManage,
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return adapter;
}

void main() {
  group('User editor', () {
    testWidgets('should_offer_only_the_roles_of_the_users_plane', (
      tester,
    ) async {
      await _pumpEditor(tester, existing: _alice);

      expect(find.text('Edit user'), findsOneWidget);
      expect(find.text('tenant-viewer'), findsOneWidget);
      expect(find.text('tenant-editor'), findsOneWidget);
      expect(find.text('platform-admin'), findsNothing);
      expect(
        find.text('Supabase Auth identity — not editable'),
        findsOneWidget,
      );
    });

    testWidgets('should_send_one_patch_with_the_new_username_and_roles', (
      tester,
    ) async {
      final adapter = await _pumpEditor(tester, existing: _alice);

      await tester.enterText(find.byType(TextFormField).first, 'alice-b');
      await tester.tap(find.text('tenant-editor'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Save'));
      await tester.pumpAndSettle();

      expect(adapter.methods, ['PATCH']);
      final request = adapter.requests.single;
      expect(request.uri.path, '/inventory/api/v1/users/user-1');
      expect(request.data, {
        'username': 'alice-b',
        // The stored number is part of the form and travels back unchanged.
        'phoneNumber': '+919876543210',
        'roles': ['tenant-editor', 'tenant-viewer'],
      });
    });

    testWidgets('should_create_in_the_platform_plane_without_a_tenant_id', (
      tester,
    ) async {
      final adapter = await _pumpEditor(tester, tenant: _tenants.first);

      expect(find.text('Add user'), findsOneWidget);
      expect(find.text('Platform users — no tenant'), findsOneWidget);
      // The platform plane takes platform roles only.
      expect(find.text('platform-admin'), findsOneWidget);
      expect(find.text('tenant-viewer'), findsNothing);

      await tester.enterText(find.byType(TextFormField).at(0), 'bob');
      // 0 username, 1 email, 2 phone number, 3 temporary password.
      await tester.enterText(find.byType(TextFormField).at(3), 'temporary');
      await tester.tap(find.text('Create'));
      await tester.pumpAndSettle();

      expect(adapter.methods, ['POST']);
      expect(adapter.requests.single.data, {
        'username': 'bob',
        'tenantId': null,
        'email': null,
        'phoneNumber': null,
        'temporaryPassword': 'temporary',
        'roles': <String>[],
      });
    });

    testWidgets('should_send_the_phone_number_it_recorded', (tester) async {
      final adapter = await _pumpEditor(tester, tenant: _tenants.last);

      await tester.enterText(find.byType(TextFormField).at(0), 'bob');
      // The separators a human types are the backend's to normalize; the form sends what was entered.
      await tester.enterText(
        find.byType(TextFormField).at(2),
        '+91 98765 43210',
      );
      await tester.enterText(find.byType(TextFormField).at(3), 'temporary');
      await tester.tap(find.text('Create'));
      await tester.pumpAndSettle();

      expect(adapter.requests.single.data, {
        'username': 'bob',
        'tenantId': 'tenant-1',
        'email': null,
        'phoneNumber': '+91 98765 43210',
        'temporaryPassword': 'temporary',
        'roles': <String>[],
      });
    });

    testWidgets('should_send_the_phone_number_it_edited', (tester) async {
      final adapter = await _pumpEditor(tester, existing: _alice);

      await tester.enterText(find.byType(TextFormField).at(1), '+14155552671');
      await tester.tap(find.text('Save'));
      await tester.pumpAndSettle();

      expect(adapter.requests.single.data, {
        'username': 'alice',
        'phoneNumber': '+14155552671',
        'roles': ['tenant-viewer'],
      });
    });

    testWidgets('should_refuse_a_number_without_the_country_code_prefix', (
      tester,
    ) async {
      final adapter = await _pumpEditor(tester, existing: _alice);

      await tester.enterText(find.byType(TextFormField).at(1), '9876543210');
      await tester.tap(find.text('Save'));
      await tester.pumpAndSettle();

      // The rejected value stays in the open dialog and nothing is sent (the backend never sees it).
      expect(find.text('Start with + and the country code'), findsOneWidget);
      expect(adapter.requests, isEmpty);
    });
  });

  group('User row', () {
    testWidgets('should_show_the_phone_number_when_one_is_recorded', (
      tester,
    ) async {
      await _pumpUserList(tester);

      expect(find.textContaining('+919876543210'), findsOneWidget);
    });
  });

  group('Reset password', () {
    testWidgets('should_not_offer_the_action_without_the_write_grant', (
      tester,
    ) async {
      await _pumpUserList(tester, canManage: false);

      expect(find.byTooltip('Reset password'), findsNothing);
    });

    testWidgets('should_put_the_temporary_password_for_that_user', (
      tester,
    ) async {
      final adapter = await _pumpUserList(tester);

      await tester.tap(find.byTooltip('Reset password'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField), 'temp-secret');
      await tester.tap(find.text('Reset'));
      await tester.pumpAndSettle();

      expect(adapter.methods, ['PUT']);
      final request = adapter.requests.single;
      expect(request.uri.path, '/inventory/api/v1/users/user-1/password');
      expect(request.data, {'temporaryPassword': 'temp-secret'});
      // The dialog closes on success; the API returns no body, so nothing is echoed back.
      expect(find.text('Reset password'), findsNothing);
    });
  });
}
