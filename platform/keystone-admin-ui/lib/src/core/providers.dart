import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/envelopes.dart';
import '../models/list_query.dart';
import '../models/models.dart';
import 'api_client.dart';
import 'console.dart';
import 'env.dart';
import 'token_storage.dart';

final tokenStorageProvider = Provider<TokenStorage>((ref) => TokenStorage());

/// The single `dio` instance: the backend base URL with the stored bearer token attached, shared by
/// both API planes so a token refresh applies to each of them.
final dioProvider = Provider<Dio>((ref) {
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
  return dio;
});

/// The **platform** plane client (`/api/v1`).
final apiClientProvider = Provider<ApiClient>(
  (ref) => ApiClient(ref.watch(dioProvider)),
);

/// The **tenant self-service** plane client (`/api/v1/tenant`): identical routes, scoped by the backend
/// to the signed-in caller's own tenant.
final tenantApiClientProvider = Provider<ApiClient>(
  (ref) => ApiClient(ref.watch(dioProvider), basePath: '/api/v1/tenant'),
);

/// Whether the user has a session (a stored token). The router refreshes on this.
final signedInProvider = StateProvider<bool>((ref) => false);

/// The authenticated caller's profile, or null when signed out.
final meProvider = FutureProvider<Me?>((ref) async {
  if (!ref.watch(signedInProvider)) {
    return null;
  }
  return ref.watch(apiClientProvider).me();
});

/// The console the signed-in caller administers: the platform plane for a platform user, the caller's
/// own tenant otherwise. The platform plane is also the fallback while `/me` is in flight, so a screen
/// is never left without a console.
final consoleProvider = Provider<ConsoleScope>((ref) {
  final tenantId = ref.watch(meProvider).valueOrNull?.tenantId;
  if (tenantId == null) {
    return ConsoleScope.platform(ref.watch(apiClientProvider));
  }
  return ConsoleScope.tenant(ref.watch(tenantApiClientProvider), tenantId);
});

/// The **complete** tenant set a platform caller can address: the tenant filter, the tenant and owner
/// dropdowns, and the lookup of one tenant by id.
///
/// It reads the unpaged `/options` route on purpose — a picker must offer every choice, and a page would
/// silently offer only the first one ([OptionList.truncated] says the backend's cap was reached). A tenant
/// console has no use for it, and no route for it, so this stays a platform-plane concern.
final tenantOptionsProvider = FutureProvider<OptionList<Tenant>>(
  (ref) => ref.watch(apiClientProvider).tenantOptions(),
);

/// The tenants of the platform, **one page at a time**, filtered and ordered by the server.
///
/// The four paged lists ([tenantsPageProvider], [rolesPageProvider], [permissionsPageProvider],
/// [usersPageProvider]) are `autoDispose` families keyed by the whole [ListQuery]: the key is a
/// *user-driven* query, so a cache per query would be held for the whole session — one per term the caller
/// ever typed, one per tenant they ever selected — and nothing would give it back. `autoDispose` keeps only
/// what a mounted screen watches; a screen that needs a list again refetches it. The single-key providers
/// above stay alive on purpose (the caller's own session does), and `signOut` invalidates them explicitly.
final tenantsPageProvider = FutureProvider.autoDispose
    .family<Paged<Tenant>, ListQuery>(
      (ref, query) => ref.watch(apiClientProvider).tenants(query),
    );

/// One page of the roles the current console can see. The query's `tenantId` is a **platform-plane
/// filter** — null is everything, the reserved platform id the global catalog, a tenant id the global
/// catalog plus that tenant's own; the tenant console never sends one.
final rolesPageProvider = FutureProvider.autoDispose
    .family<Paged<Role>, ListQuery>(
      (ref, query) => ref.watch(consoleProvider).roles(query),
    );

/// **Every** role the current console may assign — the user editor's checklist, which must offer all of
/// them. Keyed by the platform-plane tenant filter, or null for every owner.
final roleOptionsProvider = FutureProvider.autoDispose
    .family<OptionList<Role>, String?>(
      (ref, tenantId) => ref.watch(consoleProvider).roleOptions(tenantId: tenantId),
    );

/// One page of the permissions the current console can see, with the same filter rule as
/// [rolesPageProvider].
final permissionsPageProvider = FutureProvider.autoDispose
    .family<Paged<Permission>, ListQuery>(
      (ref, query) => ref.watch(consoleProvider).permissions(query),
    );

/// One page of users. The query's `tenantId` is a platform-plane filter — the reserved platform tenant id
/// for the platform plane's own users (`users.tenant_id IS NULL`), or null for every user; the tenant
/// console always lists exactly its own, because its routes carry no tenant.
final usersPageProvider = FutureProvider.autoDispose
    .family<Paged<User>, ListQuery>(
      (ref, query) => ref.watch(consoleProvider).users(query),
    );

/// The tenant with [id] — the synthetic platform tenant included — or null while the set is loading, or
/// when no such tenant exists. Sourced from the unpaged options list, so a deep link (`/tenants/{id}`)
/// resolves even though the tenants *screen* now shows one page at a time.
final tenantByIdProvider = Provider.family<Tenant?, String>((ref, id) {
  final tenants =
      ref.watch(tenantOptionsProvider).valueOrNull?.items ?? const <Tenant>[];
  for (final tenant in tenants) {
    if (tenant.id == id) {
      return tenant;
    }
  }
  return null;
});
