/// Build-time configuration injected via `--dart-define` (defined by the hosting app).
abstract final class Env {
  static const String apiBaseUrl = String.fromEnvironment('API_BASE_URL');
}
