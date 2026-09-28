import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// Records every request and answers a **page envelope** (a list read), an **options envelope** (a picker
/// read) or a valid object (a write), so a scope's routing can be asserted without a backend.
final class _RecordingAdapter implements HttpClientAdapter {
  final List<RequestOptions> requests = [];

  /// A superset of what `Role`, `Permission` and `User` need to deserialize a write's response; unknown
  /// fields are ignored.
  static const Map<String, dynamic> _writeBody = {
    'id': 'id-1',
    'sub': 'sub-1',
    'username': 'someone',
    'email': 'someone@example.test',
    'code': 'code-1',
    'scope': 'TENANT',
    'permissions': <String>[],
  };

  /// An empty page, in the shape the list contract returns
  /// (`docs/CODING_GUIDELINES_BACKEND.md` §8) — a bare array is no longer a valid list response.
  static const Map<String, dynamic> _page = {
    'items': <dynamic>[],
    'page': 0,
    'size': 25,
    'totalElements': 0,
    'totalPages': 0,
    'hasNext': false,
    'hasPrevious': false,
  };

  /// An empty picker set, in the shape the `/options` routes return.
  static const Map<String, dynamic> _options = {
    'items': <dynamic>[],
    'truncated': false,
  };

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    final isRead = options.method == 'GET';
    return ResponseBody.fromString(
      jsonEncode(
        isRead
            ? (options.path.endsWith('/options') ? _options : _page)
            : _writeBody,
      ),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  late _RecordingAdapter adapter;
  late ApiClient platformClient;
  late ApiClient tenantClient;

  setUp(() {
    adapter = _RecordingAdapter();
    final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/inventory'))
      ..httpClientAdapter = adapter;
    platformClient = ApiClient(dio);
    tenantClient = ApiClient(dio, basePath: '/api/v1/tenant');
  });

  group('ConsoleScope', () {
    test(
      'should_filter_the_platform_role_list_by_the_owner_it_is_given',
      () async {
        await ConsoleScope.platform(
          platformClient,
        ).roles(const ListQuery(tenantId: 'tenant-1'));

        expect(adapter.requests.single.uri.path, '/inventory/api/v1/roles');
        expect(adapter.requests.single.queryParameters, {
          'tenantId': 'tenant-1',
          'page': '0',
          'size': '25',
        });
      },
    );

    test('should_ask_the_server_for_a_page_rather_than_a_whole_list', () async {
      await ConsoleScope.platform(platformClient).roles(const ListQuery());

      expect(adapter.requests.single.uri.path, '/inventory/api/v1/roles');
      // The paging parameters are what make the search server-side: the client never slices a list it
      // fetched whole (`docs/UX_GUIDELINES.md` §1.1).
      expect(adapter.requests.single.queryParameters, {
        'page': '0',
        'size': '25',
      });
    });

    test('should_read_a_picker_set_from_the_unpaged_options_route', () async {
      await ConsoleScope.platform(platformClient).roleOptions();

      expect(
        adapter.requests.single.uri.path,
        '/inventory/api/v1/roles/options',
      );
    });

    test('should_use_the_tenant_routes_and_drop_a_tenant_parameter', () async {
      final scope = ConsoleScope.tenant(tenantClient, 'tenant-1');
      // A filter cannot be smuggled into a tenant route, and asking for page 3 must keep asking for page 3:
      // dropping the tenant must not reset the page.
      const filtered = ListQuery(tenantId: 'another-tenant', page: 2, size: 50);

      await scope.roles(filtered);
      expect(adapter.requests.last.uri.path, '/inventory/api/v1/tenant/roles');
      expect(adapter.requests.last.queryParameters, {
        'page': '2',
        'size': '50',
      });

      await scope.users(filtered);
      expect(adapter.requests.last.uri.path, '/inventory/api/v1/tenant/users');
      expect(adapter.requests.last.queryParameters, {
        'page': '2',
        'size': '50',
      });

      await scope.permissions(filtered);
      expect(
        adapter.requests.last.uri.path,
        '/inventory/api/v1/tenant/permissions',
      );
      expect(adapter.requests.last.queryParameters, {
        'page': '2',
        'size': '50',
      });
    });

    test('should_never_send_an_owner_on_a_tenant_write', () async {
      final scope = ConsoleScope.tenant(tenantClient, 'tenant-1');

      await scope.createRole(
        const CreateRoleRequest(
          code: 'manager',
          scope: 'TENANT',
          tenantId: 'another-tenant',
        ),
      );
      expect(adapter.requests.last.uri.path, '/inventory/api/v1/tenant/roles');
      expect(adapter.requests.last.data, {
        'code': 'manager',
        'scope': 'TENANT',
        'tenantId': null,
        'permissions': <String>[],
      });

      await scope.createPermission(
        const CreatePermissionRequest(
          code: 'tenant:user:read-only',
          scope: 'TENANT',
          tenantId: 'another-tenant',
        ),
      );
      expect(
        adapter.requests.last.uri.path,
        '/inventory/api/v1/tenant/permissions',
      );
      expect(adapter.requests.last.data, {
        'code': 'tenant:user:read-only',
        'scope': 'TENANT',
        'tenantId': null,
      });
    });
  });
}
