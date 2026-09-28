import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../models/models.dart';

/// One permission as a list row: the code, then `scope · level · owner`.
///
/// This is the **only** place a catalogue row is rendered, so the Permissions screen and the role editor's
/// picker cannot drift apart — the same code, the same globe/building icon for a global or tenant-owned row,
/// the same wording. The row is read-only unless the caller passes [onChanged], which is what the picker
/// does; a code the caller may not grant is then **disabled and says why** rather than offered and refused
/// on save (`CallerScope.requireGrantable`, mirrored by `Me.canGrant`).
class PermissionRow extends ConsumerWidget {
  const PermissionRow({
    super.key,
    required this.permission,
    this.selected = false,
    this.onChanged,
    this.grantable = true,
  });

  final Permission permission;

  /// The picker's checkbox state; meaningless on the read-only catalogue screen, where [onChanged] is null.
  final bool selected;

  /// The picker's toggle. Null on the catalogue screen, which renders a plain [ListTile].
  final ValueChanged<bool>? onChanged;

  /// Whether the caller may grant this code. A row that cannot be granted renders disabled and says why —
  /// including one the role **already holds** (changing a role that carries a code the caller no longer
  /// holds shows the truth rather than silently dropping the grant; the backend refuses such a set with
  /// "Cannot grant a permission you do not hold").
  final bool grantable;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final level = permission.access?.label ?? 'unknown level';
    final subtitle = StringBuffer('${permission.scope} · $level · ')
      ..write(permissionOwnerLabel(ref, permission));
    if (!grantable) {
      subtitle.write(' · you do not hold this');
    }
    final icon = Icon(
      permission.isGlobal ? Icons.public : Icons.apartment_outlined,
      color: permission.isGlobal
          ? theme.colorScheme.primary
          : theme.colorScheme.outline,
    );
    final toggle = onChanged;
    if (toggle == null) {
      return ListTile(
        leading: icon,
        title: Text(permission.code),
        subtitle: Text(subtitle.toString()),
      );
    }
    return CheckboxListTile(
      value: selected,
      // A null callback is what makes the control visibly and actually disabled.
      onChanged: grantable ? (value) => toggle(value ?? false) : null,
      controlAffinity: ListTileControlAffinity.leading,
      secondary: icon,
      title: Text(permission.code),
      subtitle: Text(subtitle.toString()),
      enabled: grantable,
    );
  }
}

/// The owner of [permission], named for the console: "Global" for the platform catalogue, the tenant's name
/// for a row a tenant defined for itself. The name comes from the unpaged tenant options, so it resolves
/// even while a page of tenants is not what is on screen — and degrades to a plain label when the caller may
/// not read tenants at all.
String permissionOwnerLabel(WidgetRef ref, Permission permission) {
  if (permission.isGlobal) {
    return 'Global';
  }
  return ref.watch(tenantByIdProvider(permission.tenantId!))?.name ??
      'Tenant permission';
}
