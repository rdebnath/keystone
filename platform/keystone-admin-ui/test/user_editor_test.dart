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
  tenantId: 'tenant-1',
  roles: const ['tenant-viewer'],
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
        tenantsProvider.overrideWith((ref) async => _tenants),
        rolesProvider.overrideWith((ref) async => _roles),
        usersProvider.overrideWith((ref, tenantId) async => <User>[]),
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
        tenantsProvider.overrideWith((ref) async => _tenants),
        usersProvider.overrideWith((ref, tenantId) async => [_alice]),
      ],
      child: MaterialApp(
        home: Scaffold(body: UserList(tenantId: null, canManage: canManage)),
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
      await tester.enterText(find.byType(TextFormField).at(2), 'temporary');
      await tester.tap(find.text('Create'));
      await tester.pumpAndSettle();

      expect(adapter.methods, ['POST']);
      expect(adapter.requests.single.data, {
        'username': 'bob',
        'tenantId': null,
        'email': null,
        'temporaryPassword': 'temporary',
        'roles': <String>[],
      });
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
