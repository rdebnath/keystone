/// Keystone platform admin console UI — a shared Flutter package hosted by applications.
///
/// Exposes the login/change-password flows, the admin dashboard (tenants, roles, permissions,
/// users), and the REST/data providers. Auth is backend-proxied: the client never talks to
/// Supabase directly; it POSTs `username@tenantid` + password to the Java backend and stores the
/// returned session tokens in secure storage.
library;

export 'src/core/api_client.dart';
export 'src/core/providers.dart';
export 'src/core/token_storage.dart';
export 'src/features/admin/dashboard_screen.dart';
export 'src/features/auth/auth_service.dart';
export 'src/features/auth/change_password_screen.dart';
export 'src/features/auth/login_screen.dart';
export 'src/models/models.dart';
