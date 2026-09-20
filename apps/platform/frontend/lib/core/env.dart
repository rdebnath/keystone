/// Build-time configuration injected via `--dart-define`.
///
/// The Supabase anon key and backend URL are public (they ship in the client bundle); the
/// Supabase service-role key must never appear here or in any client bundle.
abstract final class Env {
  static const String supabaseUrl = String.fromEnvironment('SUPABASE_URL');
  static const String supabaseAnonKey = String.fromEnvironment('SUPABASE_ANON_KEY');
  static const String apiBaseUrl = String.fromEnvironment('API_BASE_URL');
}
