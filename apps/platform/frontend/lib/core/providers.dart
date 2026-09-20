import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart' hide User;

import '../features/auth/auth_provider.dart';
import '../models/models.dart';
import 'api_client.dart';
import 'env.dart';

/// A `dio` instance wired to the backend base URL with the Supabase bearer token attached.
final apiClientProvider = Provider<ApiClient>((ref) {
  final dio = Dio(BaseOptions(baseUrl: Env.apiBaseUrl));
  dio.interceptors.add(InterceptorsWrapper(onRequest: (options, handler) async {
    final token = Supabase.instance.client.auth.currentSession?.accessToken;
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }));
  return ApiClient(dio);
});

/// The authenticated caller's profile, or null when signed out.
final meProvider = FutureProvider<Me?>((ref) async {
  final signedIn = ref.watch(authStateProvider).valueOrNull?.signedIn ?? false;
  if (!signedIn) {
    return null;
  }
  return ref.watch(apiClientProvider).me();
});

final tenantsProvider = FutureProvider<List<Tenant>>(
    (ref) => ref.watch(apiClientProvider).tenants());

final rolesProvider = FutureProvider<List<Role>>(
    (ref) => ref.watch(apiClientProvider).roles());

final permissionsProvider = FutureProvider<List<Permission>>(
    (ref) => ref.watch(apiClientProvider).permissions());

final usersProvider = FutureProvider<List<User>>(
    (ref) => ref.watch(apiClientProvider).users());
