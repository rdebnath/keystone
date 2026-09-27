import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/models.dart';
import 'api_client.dart';
import 'env.dart';
import 'token_storage.dart';

final tokenStorageProvider = Provider<TokenStorage>((ref) => TokenStorage());

/// A `dio` instance wired to the backend base URL with the stored bearer token attached.
final apiClientProvider = Provider<ApiClient>((ref) {
  final dio = Dio(BaseOptions(baseUrl: Env.apiBaseUrl));
  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await ref.read(tokenStorageProvider).accessToken();
        if (token != null && token.isNotEmpty) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      },
    ),
  );
  return ApiClient(dio);
});

/// Whether the user has a session (a stored token). The router refreshes on this.
final signedInProvider = StateProvider<bool>((ref) => false);

/// The authenticated caller's profile, or null when signed out.
final meProvider = FutureProvider<Me?>((ref) async {
  if (!ref.watch(signedInProvider)) {
    return null;
  }
  return ref.watch(apiClientProvider).me();
});

final tenantsProvider = FutureProvider<List<Tenant>>(
  (ref) => ref.watch(apiClientProvider).tenants(),
);

final rolesProvider = FutureProvider<List<Role>>(
  (ref) => ref.watch(apiClientProvider).roles(),
);

final permissionsProvider = FutureProvider<List<Permission>>(
  (ref) => ref.watch(apiClientProvider).permissions(),
);

/// The users of one tenant — the reserved platform tenant id for the platform plane
/// (`users.tenant_id IS NULL`), or null for every user.
final usersProvider = FutureProvider.family<List<User>, String?>(
  (ref, tenantId) => ref.watch(apiClientProvider).users(tenantId: tenantId),
);

/// The tenant with [id] from the loaded tenant list (the synthetic platform tenant included), or null
/// while the list is still loading or when no such tenant exists.
final tenantByIdProvider = Provider.family<Tenant?, String>((ref, id) {
  final tenants = ref.watch(tenantsProvider).valueOrNull ?? const <Tenant>[];
  for (final tenant in tenants) {
    if (tenant.id == id) {
      return tenant;
    }
  }
  return null;
});
