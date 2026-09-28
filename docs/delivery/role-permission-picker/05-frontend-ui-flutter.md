# Phase 5 — Frontend / UI (Flutter)

**Scope** — `platform/keystone-admin-ui`: a browsable **permission picker** built on the console's existing
list kit, the permission row shared with the Permissions screen, and the create-role dialog's free-text field
replaced by a selection summary. No host-app change (`apps/inventory/frontend` only hosts the console) and no
backend change.

**Artifacts**

| File | Change |
| --- | --- |
| `lib/src/features/admin/permission_picker.dart` **(new)** | `showPermissionPicker(...)` + `_PermissionPickerDialog`: the toolbar (search · owner · sort), the paged list of catalogue rows, the accumulated selection, Clear/Apply. |
| `lib/src/features/admin/permission_row.dart` **(new)** | `PermissionRow` — the one widget that renders a permission (icon, code, `scope · level · owner`), selectable when the picker passes a change callback. |
| `lib/src/features/admin/roles_screen.dart` | `_CreateRoleDialog`: the permissions `TextField` becomes a selection summary + *Choose permissions*; owner/scope changes prune the selection; `_submit` sends the chosen codes. |
| `lib/src/features/admin/permissions_screen.dart` | `_PermissionTile` replaced by the shared `PermissionRow` (no behaviour change), so the two screens cannot drift. |
| `lib/keystone_admin_ui.dart` | Export `permission_picker.dart` and `permission_row.dart` (the barrel exports every other screen/widget file). |

**Dependencies** — phase 1 (the pinned query, the row content) and phase 3 (`ListQuery.permissionsFor`,
`PermissionSelection`, `Me.canGrant`). Consumes, unchanged: `SearchField`, `ListToolbar`, `OwnerFilter`,
`SortSelect`, `PagedListView`, `PaginationBar`, `MessagePanel`, `permissionsPageProvider`,
`tenantByIdProvider`.

**Verification** — `dart format lib test`; `flutter analyze` → no issues; `flutter test` → all green, with the
new `permission_picker_test.dart` covering A1–A10 (phase 7). Manual: sign in as `admin@keystone`, create a
role, search/filter/sort/page the picker, pick across two pages, Apply, Create.

## The picker

```dart
/// Asks which permission codes to grant. Returns the chosen codes (sorted), or null when cancelled.
///
/// [ownerId] and [scope] are the role's owner and scope — they **seed** the query, so every row offered is
/// one the backend would accept, and they are not offered as filters the user could widen past it.
Future<List<String>?> showPermissionPicker(
  BuildContext context, {
  required String? ownerId,
  required String scope,
  required List<String> selected,
});
```

Widget tree (a `Dialog`, not an `AlertDialog`: the body is a list that needs a bounded height, and the
toolbar needs the width):

```
Dialog
└─ ConstrainedBox(maxWidth: 840, maxHeight: 620)          // min(…, MediaQuery) so a narrow screen shrinks
   └─ Column(mainAxisSize: min)
      ├─ ListToolbar(search: SearchField('Search permission code'), filters: [OwnerFilter?], trailing: SortSelect)
      ├─ helper line: 'TENANT scope — only TENANT-scope permissions can be granted'   (the pinned scope, said out loud)
      ├─ Flexible(child: PagedListView<Permission>(…))     // summary + rows + pager, the shared states
      ├─ selection strip: 'n selected' + one removable chip per code + Clear
      └─ actions: Cancel / Apply
```

- **Rows** — `PermissionRow(permission: …, owner: …, selected: …, onChanged: …)`: a `CheckboxListTile` whose
  `secondary` is the same globe/building icon the Permissions screen shows, `title` the code and `subtitle`
  `scope · level · owner` (the owner label resolved through `tenantByIdProvider`, as on that screen). A code
  `Me.canGrant` refuses is rendered **disabled**, with ` · you do not hold this` appended to its subtitle, and
  never toggles (open question 3).
- **Search, filter, sort and paging are the server's**: every change is `_query = …` + `setState` through the
  same `ListQuery` mutators the screens use (`withSearch`, `withTenant`, `withSort`, `withSize`, `onPage`), each
  of which already returns to page 1 (§1.4). The 300 ms debounce, "previous rows stay while the next page
  loads", the two empty states and Retry come from the shared widgets — not re-implemented (§1.9, §1.15).
- **The owner filter is conditional** (`OwnerFilter`, 260 px wide): shown only on the platform plane when the
  role has **no** owner and its scope is `TENANT` — the one case where more than one owner is grantable.
  Selecting a tenant narrows to the global catalogue plus that tenant; selecting the global catalogue narrows
  to `tenant_id IS NULL` rows. Everywhere else the owner is already fixed, so the picker states it instead of
  showing a control with a single legal value (§1.6).
- **Selection survives everything**: it is phase 3's `PermissionSelection` held by the dialog's state, so
  paging, a new search or a filter change never clears it, and Apply returns its codes.
- **Apply** pops the codes; **Cancel** pops `null` and the caller keeps what it had.


## The create-role dialog

```dart
final PermissionSelection _selected = PermissionSelection.empty;   // phase 3

InputDecorator(
  decoration: const InputDecoration(
    labelText: 'Permissions',
    helperText: 'Chosen from the catalogue — only permissions this owner and scope may use are offered',
  ),
  child: Text(_selected.isEmpty
      ? 'No permissions selected'
      : '${_selected.length} selected · ${_selected.codes.join(', ')}'),
)
TextButton.icon(
  onPressed: _choosePermissions,
  icon: const Icon(Icons.playlist_add_check),
  label: Text(_selected.isEmpty
      ? 'Choose permissions'
      : 'Change permissions (${_selected.length})'),
)
```

- `_choosePermissions` opens the picker with the dialog's current owner and scope and writes the result back
  into `_selected` (rebuilt from the returned codes — the dialog re-reads nothing from the network to do it).
- **An owner/scope change prunes the selection** (`PermissionSelection.retaining`) and says so in one line —
  `2 permissions removed — they do not match the new scope.` — so a user who switches `TENANT` → `PLATFORM` is
  told, instead of meeting a `422` on Create.
- `_submit` sends `CreateRoleRequest(..., permissions: _selected.codes)`. `splitList` is dropped from this file
  (it existed only for the typed field); `console.createRole` and `showApiError` are unchanged, so a refusal the
  picker could not predict (a tenant admin whose grant set changed since the page loaded) still reaches the user
  as the server's own message.

## Layout note (why a dialog, sized this way)

`ListToolbar` lays its children out in a `Wrap` aligned by their top edges (§1.16), so an 840-px dialog fits
search (320) + owner (260) + sort (240) on one line on a wide screen and wraps cleanly on a narrow one — the
behaviour the list screens already have, guarded by `lists_test`. The height is bounded so `PagedListView`'s
`Column` + `Expanded` has a finite box to fill; both bounds are clamped against `MediaQuery.sizeOf(context)`
so the dialog cannot overflow a small viewport.

## Deviations and decisions (recorded as they are taken)

- **`PermissionRow` carries an optional selection** rather than existing twice (a read-only tile and a checkbox
  tile): one widget cannot drift from itself, and the alternative — a screen showing `TENANT · read-only ·
  Global` while the picker shows something else — is the class of bug the shared widgets exist to prevent. It
  is a `ConsumerWidget` because the owner label needs `tenantByIdProvider`.
- **The picker seeds `ownerId` from the role and, on the tenant plane, passes `null`**: `ConsoleScope` drops
  the tenant for a `!isPlatformPlane` query, and the tenant route has no parameter to name — sending one would
  be a `422` if it ever reached the server.
- **The wildcard row is selectable on the platform plane and disabled on the tenant plane**, matching
  `CallerScope.requireGrantable` exactly (platform exempt; a tenant may never grant `*`).
- **`permissions_screen.dart` keeps its own screen state and URL behaviour** — only its row rendering moves to
  the shared widget.

## Execution record (2026-09-28)

**Shipped**

| Artifact | What it holds |
| --- | --- |
| `lib/src/features/admin/permission_picker.dart` **(new)** | `showPermissionPicker(context, {ownerId, scope, selected})` → `PermissionSelection?`, and `_PermissionPickerDialog`: a `Dialog` sized `min(840, width−48) × min(620, height−96)` holding `ListToolbar` (search · conditional owner filter · sort) + the pinned-scope note + `PagedListView<Permission>` + the selection strip (count, one removable `InputChip` per code, *Clear*) + Cancel/Apply. |
| `lib/src/features/admin/permission_row.dart` **(new)** | `PermissionRow` (a `ListTile` on the catalogue screen, a `CheckboxListTile` when the picker passes `onChanged`; `enabled: false` **and** `onChanged: null` when `grantable` is false) + `permissionOwnerLabel(ref, permission)`. |
| `lib/src/features/admin/roles_screen.dart` | `showRoleEditor(context, {existing})` → `bool`, the create/edit dialog (`_RoleEditorDialog`), the tile's *Edit* action (`canEdit: canManage && !isSeededAdminRole(role)`), `_edit(...)`, and the permissions field + prune notice. The old free-text field, `splitList` and the `dialogs.dart` import are gone. |
| `lib/src/features/admin/permissions_screen.dart` | `_PermissionTile` and its `_owner(...)` helper deleted; the list renders the shared `PermissionRow`. |
| `lib/keystone_admin_ui.dart` | Exports `permission_picker.dart`, `permission_row.dart`, `core/permission_selection.dart`. |

**Deviations from the plan, and why**

1. **The dialog is a `Dialog` with a computed size, not an `AlertDialog`.** The body is a list that needs a
   bounded height and a toolbar that needs the width, so `MediaQuery.sizeOf` clamps both — the dialog cannot
   overflow a small viewport, and on a wide one the toolbar keeps its single row (`ListToolbar`'s `Wrap` still
   wraps cleanly if it does not fit).
2. **The picker's query is pinned to *both* facts, and the dialog now enforces the second one too.** Choosing a
   tenant owner sets the scope to `TENANT` in the same gesture (`_ownerPicker`) and the scope dropdown is
   disabled while the owner is a tenant, because `RoleService.requireOwnableScope` refuses `tenant-owned +
   PLATFORM`. The old dialog let a user compose that pair and meet a `422`; the picker made it worth closing.
3. **The dialog performs the API call itself and pops a `bool`**, exactly like `showUserEditor`, instead of
   returning a request for the screen to send. One editor, one shape: create → `createRole(CreateRoleRequest)`,
   edit → `updateRole(id, UpdateRoleRequest)`, `rolesPageProvider` invalidated on success, `showApiError` (the
   server's own `detail`) on failure with the dialog left open. The screen shows `Role created.` / `Role saved.`
4. **Two new private widgets joined the file**: `_ReadOnlyField` (the owner and scope the dialog *states*) and
   `_tenantOwnershipNote()` (the tenant plane's "Owned by your tenant · TENANT scope"), both mirroring
   `user_editor.dart`'s local pattern rather than inventing a shared one.
5. **The chip strip is height-bounded and scrolls** (`ConstrainedBox(maxHeight: 88)` + `SingleChildScrollView`),
   so a twenty-code selection cannot push the pager out of the dialog.
6. **The permission summary is `maxLines: 4` + ellipsis**: the count is the primary information and the chips
   are the full list; a wall of codes in a field would have been worse than either.

**A defect the phase-7 tests caught (and fixed):** `CheckboxListTile` wires its `onTap` from `onChanged`
*independently* of `enabled`, so a "disabled" row still toggled when only `enabled: false` was passed. A
non-grantable row now passes `onChanged: null` **and** `enabled: false`; `permission_row_test` guards it.

**Formatter note (environment).** `dart format` in the installed SDK (Flutter 3.47.5 / Dart 3.13.4) applies the
new "tall" 80-column style, which rewrites whole files — the repository is committed in the older 120-column
style, and *every* existing file differs from this SDK's output. Its unrelated rewrites to files this delivery
does not change (`lists.dart`, `providers.dart`, `owner_filter.dart`, `user_editor.dart`, `envelopes.dart`) were
therefore reverted with `git checkout`, and the churn it introduced in `list_query.dart` and `api_client.dart`
was reverted and re-applied by hand. The three new files keep the formatted style; a repository-wide
`dart format` at some point will normalize everything. Verification here is `flutter analyze` (clean), not
`dart format --set-exit-if-changed` — which no file in the repository would pass under this SDK.

**Verification** — `flutter analyze` → **No issues found** in `platform/keystone-admin-ui` and in
`apps/inventory/frontend`; `flutter test` → **136 passed** (was 96); no manual click-through (no dev
credentials) — recorded in `08-delivery.md` as a follow-up.

## Verification

- `cd platform/keystone-admin-ui && dart format lib test && flutter analyze && flutter test`
  (SDK at `/Users/rajeshdebnath/manual-install/flutter/bin`, not on `PATH`).
- Manual click-through (needs live dev credentials) is recorded as such if it cannot be run.
