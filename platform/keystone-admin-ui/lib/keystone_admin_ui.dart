/// Keystone platform admin console UI — a shared Flutter package hosted by applications.
///
/// Exposes the login/change-password flows, the navigable admin console (tenants, users, roles,
/// permissions) and the REST/data providers. Auth is backend-proxied: the client never talks to
/// Supabase directly; it POSTs `username@tenantid` + password to the Java backend and stores the
/// returned session tokens in secure storage.
library;

export 'src/core/api_client.dart';
export 'src/core/branding.dart';
export 'src/core/errors.dart';
export 'src/core/panels.dart';
export 'src/core/password_field.dart';
export 'src/core/permissions.dart';
export 'src/core/providers.dart';
export 'src/core/token_storage.dart';
export 'src/features/admin/admin_shell.dart';
export 'src/features/admin/permissions_screen.dart';
export 'src/features/admin/roles_screen.dart';
export 'src/features/admin/tenant_users_screen.dart';
export 'src/features/admin/tenants_screen.dart';
export 'src/features/admin/user_editor.dart';
export 'src/features/admin/users_screen.dart';
export 'src/features/auth/auth_service.dart';
export 'src/features/auth/change_password_dialog.dart';
export 'src/features/auth/change_password_screen.dart';
export 'src/features/auth/login_screen.dart';
export 'src/models/models.dart';
export 'src/models/requests.dart';
