/// The permissions picked for one role, as the role editor's picker accumulates them.
library;

import '../models/models.dart';

/// One pick: the code, plus the two facts the grant rule cares about — the permission's own `scope` and its
/// owner (`tenantId`, null for the global catalogue).
typedef PermissionPick = ({String code, String scope, String? tenantId});

/// The permissions chosen in the role editor's picker, keyed by **code**.
///
/// Keeping the code *and* what the grant rule needs (rather than the bare string) is what makes the picker's
/// rules possible:
///
/// - a code picked on page 1 is still picked after a page change, a new search or a filter change — the
///   selection belongs to the dialog, not to the list, so paging never clears it;
/// - changing the role's owner or scope can ask what each pick's own scope and owner are ([retaining])
///   without another request, so a pick the backend would now refuse is dropped **before** it is sent.
///
/// Immutable: every operation returns a new selection.
final class PermissionSelection {
  const PermissionSelection._(this._picks);

  /// The empty selection — what a create dialog starts from.
  static const PermissionSelection empty = PermissionSelection._(
    <String, PermissionPick>{},
  );

  /// The selection holding [permissions], keyed by code (a code repeated is one pick, as one
  /// `role_permissions` row is one grant).
  factory PermissionSelection.of(Iterable<Permission> permissions) =>
      PermissionSelection._(<String, PermissionPick>{
        for (final permission in permissions)
          permission.code: _pickOf(permission),
      });

  /// The selection a role **already holds** — what an edit starts from.
  ///
  /// `Role` carries its grants as codes, and a role's granted codes are by construction of its own scope and
  /// visible to its owner (the backend enforces both on every write), so each pick is described by the role's
  /// own scope and owner: enough to show it, count it, and to drop it if the user changes either.
  factory PermissionSelection.ofRole(Role role) =>
      PermissionSelection._(<String, PermissionPick>{
        for (final code in role.permissions)
          code: (code: code, scope: role.scope, tenantId: role.tenantId),
      });

  final Map<String, PermissionPick> _picks;

  bool get isEmpty => _picks.isEmpty;
  bool get isNotEmpty => _picks.isNotEmpty;

  /// How many permissions are picked.
  int get length => _picks.length;

  /// Whether [code] is among the picks — what a picker row's checkbox asks.
  bool contains(String code) => _picks.containsKey(code);

  /// The selection with [permission] added (`selected`) or removed.
  PermissionSelection toggle(Permission permission, bool selected) {
    final next = Map<String, PermissionPick>.of(_picks);
    if (selected) {
      next[permission.code] = _pickOf(permission);
    } else {
      next.remove(permission.code);
    }
    return PermissionSelection._(next);
  }

  /// The selection with [code] removed — what a selected chip's remove action does, without needing the row
  /// again.
  PermissionSelection remove(String code) {
    final next = Map<String, PermissionPick>.of(_picks)..remove(code);
    return PermissionSelection._(next);
  }

  /// The picks that survive a role whose owner is [ownerId] and whose scope is [scope], together with the
  /// codes that did **not** — which the dialog says out loud rather than sending a pair the backend would
  /// refuse.
  ///
  /// The rule is `RoleService.grantableTo`'s, evaluated before the request instead of by it: a permission is
  /// grantable when its `scope` is the role's, and when its owner is the role's — or it is global (the
  /// catalogue is grantable to every owner). A role with **no** owner may hold the catalogue only, since its
  /// grants are handed to every tenant that holds it.
  ({PermissionSelection selection, List<String> dropped}) retaining({
    required String? ownerId,
    required String scope,
  }) {
    final kept = <String, PermissionPick>{};
    final dropped = <String>[];
    for (final entry in _picks.entries) {
      final pick = entry.value;
      if (pick.scope == scope && _grantableTo(pick, ownerId)) {
        kept[entry.key] = pick;
      } else {
        dropped.add(entry.key);
      }
    }
    dropped.sort();
    return (selection: PermissionSelection._(kept), dropped: dropped);
  }

  /// Whether a role owned by [ownerId] may hold [pick]: the catalogue is grantable to every owner, and a
  /// tenant-owned row only to the tenant that owns it.
  static bool _grantableTo(PermissionPick pick, String? ownerId) {
    if (pick.tenantId == null) {
      return true;
    }
    return ownerId != null && pick.tenantId == ownerId;
  }

  /// The picked codes, ordered by code, so a summary reads the same way every time and the request body does
  /// not depend on the order the user clicked — what `RoleRequest.permissions` carries.
  List<String> get codes {
    final sorted = _picks.keys.toList()..sort();
    return List<String>.unmodifiable(sorted);
  }

  static PermissionPick _pickOf(Permission permission) => (
    code: permission.code,
    scope: permission.scope,
    tenantId: permission.tenantId,
  );
}
