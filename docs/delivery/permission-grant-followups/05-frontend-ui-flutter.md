# Phase 5 — Frontend / UI (Flutter)

**Scope** — the *Level* control on the two permission lists, and the picker's seed following the tightened grant
rule.

**Artifacts**

| File | Change |
| --- | --- |
| `lib/src/models/list_query.dart` | `access` carried by **every** mutator (a field that only some mutators kept would silently reset the filter), `withAccess(...)` returning to page 1, `toQueryParameters()`, and `permissionsFor` seeding `ownerId ?? Tenant.platformId`. |
| `lib/src/models/models.dart` | `Tenant.platformId`. |
| `lib/src/core/lists.dart` | `ListQueryLocation.fromQueryParameters` reads `access`; `toQueryParameters` writes it. |
| `lib/src/features/admin/owner_filter.dart` | `AccessFilter` (All levels / read-only / read/write), beside `OwnerFilter` and `ScopeFilter`. |
| `lib/src/features/admin/permissions_screen.dart` | the filter in the toolbar — outside the `isPlatformPlane` block, so a tenant console gets it too. |
| `lib/src/features/admin/permission_picker.dart` | the same filter in the picker's toolbar; the conditional `OwnerFilter` and `_ownerFilterable` removed; the pinned-scope note now reads "no owner — a global role may hold the global catalog only". |
| `lib/src/core/permission_selection.dart` | `retaining` uses the role-owner rule. |

**Dependencies** — phases 3 and 4.

**Verification** — `permission_picker_test` → `should_filter_the_catalogue_by_access_level` (the request carries
`access=read-write`, the server's answer is what renders, and a read-only row is gone) and
`should_open_a_global_role_on_the_global_catalogue` (the seed is `Tenant.platformId`, no owner filter, another
tenant's row absent); `list_query_test` → the `access` field survives every mutator and `withAccess` resets the
page; `permission_selection_test` → a global role drops a tenant-owned pick. `flutter analyze` clean.

## Notes

- **The filter's value is the code suffix**, so the control and the request speak the same language as the rows:
  *read/write* is the label (`PermissionAccess.label`), `read-write` is the parameter.
- **Both planes show it** — the `PermissionsScreen` puts it outside the platform-plane block on purpose.
- **The picker's owner filter is gone, not hidden**: with the seed pinned to the role's own owner there is no
  second owner to narrow to, and the control would have offered exactly the choices `RoleService.grantableTo`
  refuses (`docs/UX_GUIDELINES.md` §1.6).
- **A test caught the ambiguity this introduced**: `SortSelect` and `AccessFilter` are both
  `DropdownButtonFormField<String?>` widgets, so the sort test now scopes its finder to `SortSelect` — a good
  reminder that a new control in a shared toolbar can make an existing finder ambiguous.
