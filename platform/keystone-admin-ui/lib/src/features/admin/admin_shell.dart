import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/branding.dart';
import '../../core/panels.dart';
import '../../core/permissions.dart';
import '../../core/providers.dart';
import '../../models/models.dart';
import '../auth/auth_service.dart';
import '../auth/change_password_dialog.dart';

/// Width of the expanded left pane.
const double _paneWidth = 264;

/// Below this width the pane is an overlay drawer instead of an inline column.
const double _inlineMinWidth = 800;

/// Animation of the pane's show/hide transition.
const Duration _paneDuration = Duration(milliseconds: 160);

/// A section of the platform console: its route, its menu label and the permission that makes it
/// visible. The backend enforces the same permission — the menu only mirrors it.
enum AdminSection {
  tenants(
    path: '/tenants',
    label: 'Tenants',
    icon: Icons.apartment_outlined,
    resource: PlatformResource.tenant,
  ),
  users(
    path: '/users',
    label: 'Users',
    icon: Icons.people_outline,
    resource: PlatformResource.user,
  ),
  roles(
    path: '/roles',
    label: 'Roles',
    icon: Icons.workspace_premium_outlined,
    resource: PlatformResource.role,
  ),
  permissions(
    path: '/permissions',
    label: 'Permissions',
    icon: Icons.key_outlined,
    resource: PlatformResource.permission,
  );

  const AdminSection({
    required this.path,
    required this.label,
    required this.icon,
    required this.resource,
  });

  /// Route path of the section.
  final String path;

  /// Menu label, also used as the AppBar title.
  final String label;

  final IconData icon;

  /// The resource whose read permission reveals the section.
  final String resource;

  /// The section [location] belongs to (a prefix match, so `/tenants/<id>` selects Tenants), or null
  /// when the location is not a console section.
  static AdminSection? forLocation(String location) {
    for (final section in values) {
      if (location == section.path || location.startsWith('${section.path}/')) {
        return section;
      }
    }
    return null;
  }
}

/// The console's route paths, shared with the hosting application's router so the menu and the routes
/// cannot drift apart.
abstract final class AdminRoutes {
  static const String tenants = '/tenants';
  static const String tenantUsersPattern = '/tenants/:tenantId';
  static const String users = '/users';
  static const String roles = '/roles';
  static const String permissions = '/permissions';

  /// The concrete path of one tenant's users.
  static String forTenant(String tenantId) => '$tenants/$tenantId';

  /// The first section [me] may read — the landing route after login. When the caller may read nothing
  /// the shell renders a single explanatory panel instead of an empty menu.
  static String firstAllowed(Me me) {
    for (final section in AdminSection.values) {
      if (me.allowsResource(section.resource)) {
        return section.path;
      }
    }
    return tenants;
  }
}

/// The platform console shell: a hideable left pane listing the sections [Me] permits, an AppBar naming
/// the current section, and the section body built by the router.
///
/// The pane is an inline column on wide layouts (toggled between [_paneWidth] and zero width by the
/// AppBar button) and an overlay `Drawer` on narrow ones, so the same menu works on web and phones.
class AdminShell extends ConsumerStatefulWidget {
  const AdminShell({super.key, required this.location, required this.child});

  /// Identifies the inline pane, whose width the show/hide toggle animates between `0` and the pane
  /// width — so its size is what "the pane is hidden" means.
  static const Key paneKey = Key('admin-shell-pane');

  /// The current router location (`state.matchedLocation`); it selects the highlighted menu entry.
  final String location;

  /// The section body, built by the hosting router.
  final Widget child;

  @override
  ConsumerState<AdminShell> createState() => _AdminShellState();
}

class _AdminShellState extends ConsumerState<AdminShell> {
  bool _paneVisible = true;

  @override
  Widget build(BuildContext context) {
    final me = ref.watch(meProvider).valueOrNull;
    if (me == null) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    final branding = ref.watch(appBrandingProvider);
    final narrow = MediaQuery.sizeOf(context).width < _inlineMinWidth;
    final sections = AdminSection.values
        .where((section) => me.allowsResource(section.resource))
        .toList(growable: false);
    final current = AdminSection.forLocation(widget.location);

    if (sections.isEmpty) {
      return Scaffold(
        appBar: AppBar(
          title: Text(branding.title),
          actions: [
            // No section means no menu — but changing your own password is not a permission, so it stays
            // reachable here (this branch has no drawer, so there is nothing to close first).
            IconButton(
              icon: const Icon(Icons.password_outlined),
              tooltip: 'Change password',
              onPressed: () => _changePassword(narrow: false),
            ),
          ],
        ),
        body: const MessagePanel(
          icon: Icons.lock_outline,
          message:
              'You do not have access to any section of the platform console. '
              'Ask a platform administrator to grant you a read permission.',
        ),
      );
    }

    final menu = _AdminMenu(
      branding: branding,
      me: me,
      current: current,
      sections: sections,
      onSignOut: _signOut,
      onChangePassword: () => _changePassword(narrow: narrow),
      onSelected: narrow ? () => Navigator.of(context).pop() : null,
    );

    if (narrow) {
      return Scaffold(
        appBar: AppBar(
          leading: Builder(
            builder: (inner) => IconButton(
              icon: const Icon(Icons.menu),
              tooltip: 'Show menu',
              onPressed: () => Scaffold.of(inner).openDrawer(),
            ),
          ),
          title: Text(current?.label ?? branding.title),
        ),
        drawer: Drawer(child: SafeArea(child: menu)),
        body: _body(sections, current),
      );
    }

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: Icon(_paneVisible ? Icons.menu_open : Icons.menu),
          tooltip: _paneVisible ? 'Hide menu' : 'Show menu',
          onPressed: () => setState(() => _paneVisible = !_paneVisible),
        ),
        title: Text(current?.label ?? branding.title),
      ),
      body: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          AnimatedContainer(
            key: AdminShell.paneKey,
            duration: _paneDuration,
            curve: Curves.easeOut,
            width: _paneVisible ? _paneWidth : 0,
            // A Material inside the animated box (rather than a color on the box itself) keeps the
            // menu's ink effects above their own background.
            child: Material(
              color: Theme.of(context).colorScheme.surfaceContainerLow,
              child: LayoutBuilder(
                builder: (context, constraints) => ClipRect(
                  child: OverflowBox(
                    alignment: Alignment.topLeft,
                    minWidth: _paneWidth,
                    maxWidth: _paneWidth,
                    minHeight: constraints.maxHeight,
                    maxHeight: constraints.maxHeight,
                    child: SizedBox(
                      width: _paneWidth,
                      height: constraints.maxHeight,
                      child: menu,
                    ),
                  ),
                ),
              ),
            ),
          ),
          const VerticalDivider(width: 1, thickness: 1),
          Expanded(child: _body(sections, current)),
        ],
      ),
    );
  }

  /// The section body, or the placeholder that keeps an unreadable deep link from calling the API.
  Widget _body(List<AdminSection> sections, AdminSection? current) {
    if (current != null && sections.contains(current)) {
      return widget.child;
    }
    final fallback = sections.first;
    return MessagePanel(
      icon: Icons.lock_outline,
      message: 'You do not have access to this section.',
      actionLabel: 'Go to ${fallback.label}',
      onAction: () => context.go(fallback.path),
    );
  }

  /// Opens the voluntary change-password dialog. This is not the forced first-login gate, so the dialog
  /// asks for the current password; every signed-in caller may use it, whatever their permissions.
  Future<void> _changePassword({required bool narrow}) async {
    if (narrow) {
      // The drawer is a local history entry on this route, so this pops the drawer, not the console.
      Navigator.of(context).pop();
    }
    await showChangePasswordDialog(context);
  }

  Future<void> _signOut() async {
    await ref.read(authServiceProvider).signOut();
    ref.read(signedInProvider.notifier).state = false;
    ref.invalidate(meProvider);
    // Drop every cached list so the next user does not see this one's data.
    ref.invalidate(tenantsProvider);
    ref.invalidate(rolesProvider);
    ref.invalidate(permissionsProvider);
    ref.invalidate(usersProvider);
  }
}

/// The pane's content: product branding and the signed-in identity, the permitted sections, and sign
/// out. Rendered inline on wide layouts and inside the `Scaffold.drawer` on narrow ones.
class _AdminMenu extends StatelessWidget {
  const _AdminMenu({
    required this.branding,
    required this.me,
    required this.current,
    required this.sections,
    required this.onSignOut,
    required this.onChangePassword,
    this.onSelected,
  });

  final AppBranding branding;
  final Me me;
  final AdminSection? current;
  final List<AdminSection> sections;
  final VoidCallback onSignOut;

  /// Opens the change-password dialog (the caller's own password, whatever their permissions).
  final VoidCallback onChangePassword;

  /// Called after a section is chosen — the drawer closes itself.
  final VoidCallback? onSelected;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                branding.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleSmall,
              ),
              const SizedBox(height: 6),
              Text(
                me.isPlatformAdmin
                    ? 'Platform · ${me.username}'
                    : 'Tenant · ${me.username}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.outline,
                ),
              ),
            ],
          ),
        ),
        const Divider(height: 1),
        const SizedBox(height: 8),
        for (final section in sections)
          _AdminMenuEntry(
            section: section,
            selected: section == current,
            onSelected: onSelected,
          ),
        const Spacer(),
        const Divider(height: 1),
        ListTile(
          leading: const Icon(Icons.password_outlined),
          title: const Text('Change password'),
          onTap: onChangePassword,
        ),
        ListTile(
          leading: const Icon(Icons.logout),
          title: const Text('Sign out'),
          onTap: onSignOut,
        ),
      ],
    );
  }
}

class _AdminMenuEntry extends StatelessWidget {
  const _AdminMenuEntry({
    required this.section,
    required this.selected,
    this.onSelected,
  });

  final AdminSection section;
  final bool selected;
  final VoidCallback? onSelected;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      child: ListTile(
        leading: Icon(section.icon),
        title: Text(section.label),
        selected: selected,
        selectedTileColor: theme.colorScheme.secondaryContainer,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        onTap: () {
          context.go(section.path);
          onSelected?.call();
        },
      ),
    );
  }
}
