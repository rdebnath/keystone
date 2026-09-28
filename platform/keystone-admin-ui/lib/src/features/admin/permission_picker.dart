import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/console.dart';
import '../../core/lists.dart';
import '../../core/permission_selection.dart';
import '../../core/providers.dart';
import '../../models/list_query.dart';
import '../../models/models.dart';
import 'owner_filter.dart';
import 'permission_row.dart';

/// Asks which permissions a role should hold — as a **browsable list of the catalogue**, never as a text box.
///
/// [ownerId] and [scope] are the *role's* owner and scope: they seed the query
/// ([ListQuery.permissionsFor]) so every row offered is one the backend accepts for that role
/// (`RoleService.grantPermissions`), and they are not offered as filters the user could widen past it — the
/// scope is stated as text instead, because a filter whose only effect is a refused save is worse than no
/// filter (`docs/UX_GUIDELINES.md` §1.6). Everything else is the console's own list rules, because the
/// picker **is** a list: a debounced server-side search over the whole catalogue, a sort control, a pager,
/// the shared empty/error/retry states — and a selection that survives paging and searching, so no choice is
/// unreachable (`docs/UX_GUIDELINES.md` §1.17).
///
/// Returns the chosen permissions — the codes with the facts the grant rule needs, so the caller can keep
/// pruning them — or null when the user cancelled. Passing [selected] makes the dialog an *edit* of an
/// existing grant set.
Future<PermissionSelection?> showPermissionPicker(
  BuildContext context, {
  required String? ownerId,
  required String scope,
  PermissionSelection selected = PermissionSelection.empty,
}) {
  return showDialog<PermissionSelection>(
    context: context,
    builder: (_) => _PermissionPickerDialog(
      ownerId: ownerId,
      scope: scope,
      selected: selected,
    ),
  );
}

/// The picker itself: a toolbar, the paged catalogue, the accumulated selection and Apply.
class _PermissionPickerDialog extends ConsumerStatefulWidget {
  const _PermissionPickerDialog({
    required this.ownerId,
    required this.scope,
    required this.selected,
  });

  final String? ownerId;
  final String scope;
  final PermissionSelection selected;

  @override
  ConsumerState<_PermissionPickerDialog> createState() =>
      _PermissionPickerDialogState();
}

class _PermissionPickerDialogState
    extends ConsumerState<_PermissionPickerDialog> {
  /// The keys the permissions list accepts (`docs/CODING_GUIDELINES_BACKEND.md` §8); the first is the
  /// server's default order — the global catalogue first, then each tenant's rows, each by code.
  static const List<SortOption> _sortOptions = <SortOption>[
    SortOption(null, 'Catalog first, then code (default)'),
    SortOption('code', 'Code'),
    SortOption('createdAt', 'Created'),
    SortOption('updatedAt', 'Updated'),
  ];

  /// The list state: the seeded owner and scope, plus whatever the user searches, filters, sorts and pages.
  late ListQuery _query = ListQuery.permissionsFor(
    ownerId: widget.ownerId,
    scope: widget.scope,
  );

  /// The picks, kept across pages and searches because they belong to the dialog rather than to the list.
  late PermissionSelection _selection = widget.selected;

  void _update(ListQuery query) => setState(() => _query = query);

  void _toggle(Permission permission, bool selected) {
    setState(() => _selection = _selection.toggle(permission, selected));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final console = ref.watch(consoleProvider);
    final me = ref.watch(meProvider).valueOrNull;
    final permissions = ref.watch(permissionsPageProvider(_query));
    final size = MediaQuery.sizeOf(context);
    return Dialog(
      child: SizedBox(
        width: math.min(840, math.max(320, size.width - 48)),
        height: math.min(620, math.max(320, size.height - 96)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 20, 24, 0),
              child: Text(
                'Grant permissions',
                style: theme.textTheme.titleLarge,
              ),
            ),
            ListToolbar(
              search: SearchField(
                value: _query.search,
                hintText: 'Search permission code',
                onChanged: (value) => _update(_query.withSearch(value)),
              ),
              filters: <Widget>[
                SizedBox(
                  width: 180,
                  child: AccessFilter(
                    value: _query.access,
                    onChanged: (access) => _update(_query.withAccess(access)),
                  ),
                ),
              ],
              trailing: SortSelect(
                query: _query,
                onQueryChanged: _update,
                options: _sortOptions,
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
              child: Text(
                'Scope ${widget.scope} · ${_ownerNote(ref, console)} — '
                'every permission below can be granted to this role.',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.outline,
                ),
              ),
            ),
            Expanded(
              child: PagedListView<Permission>(
                value: permissions,
                query: _query,
                onQueryChanged: _update,
                onRetry: () => ref.invalidate(permissionsPageProvider(_query)),
                emptyIcon: Icons.key_outlined,
                emptyMessage: 'No permissions in this scope yet.',
                itemBuilder: (context, permission) => PermissionRow(
                  permission: permission,
                  selected: _selection.contains(permission.code),
                  onChanged: (selected) => _toggle(permission, selected),
                  // An unknown identity does not lock the list: `/me` is loaded before the console
                  // renders, and this prediction is UX only — the backend still refuses what it should.
                  grantable: me?.canGrant(permission.code) ?? true,
                ),
              ),
            ),
            _selectionStrip(theme),
            _actions(),
          ],
        ),
      ),
    );
  }

  /// Whose catalogue this is, in words — the control that is *not* offered as a filter, said out loud
  /// instead of being hidden behind a value the user cannot see.
  String _ownerNote(WidgetRef ref, ConsoleScope console) {
    if (!console.isPlatformPlane) {
      return 'owned by your tenant';
    }
    final ownerId = widget.ownerId;
    if (ownerId == null) {
      return 'no owner — a global role may hold the global catalog only';
    }
    final tenants =
        ref.watch(tenantOptionsProvider).valueOrNull?.items ?? const <Tenant>[];
    for (final tenant in tenants) {
      if (tenant.id == ownerId) {
        return 'owned by ${tenant.name}';
      }
    }
    return 'owned by the selected tenant';
  }

  /// What has been picked so far: the count, one removable chip per code, and Clear. The chips are what make
  /// a selection gathered across several pages reviewable before it is saved.
  Widget _selectionStrip(ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Text(
                _selection.isEmpty
                    ? 'No permissions selected'
                    : '${_selection.length} selected',
                style: theme.textTheme.titleSmall,
              ),
              const Spacer(),
              TextButton(
                onPressed: _selection.isEmpty ? null : _clear,
                child: const Text('Clear'),
              ),
            ],
          ),
          if (_selection.isNotEmpty)
            ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 88),
              child: SingleChildScrollView(
                child: Wrap(
                  spacing: 8,
                  runSpacing: 4,
                  children: <Widget>[
                    for (final code in _selection.codes)
                      InputChip(
                        label: Text(code),
                        deleteButtonTooltipMessage: 'Remove $code',
                        onDeleted: () => setState(
                          () => _selection = _selection.remove(code),
                        ),
                      ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _actions() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: <Widget>[
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          const SizedBox(width: 8),
          FilledButton(onPressed: _apply, child: const Text('Apply')),
        ],
      ),
    );
  }

  void _clear() => setState(() => _selection = PermissionSelection.empty);

  void _apply() => Navigator.pop(context, _selection);
}
