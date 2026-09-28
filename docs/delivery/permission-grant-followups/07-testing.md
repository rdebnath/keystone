# Phase 7 — Testing

**Scope** — prove the filter, the guard and the console's mirrors. Both sides already had a suite to extend.

**Artifacts**

| File | Change |
| --- | --- |
| `platform/keystone-admin/src/test/.../identity/AccessTest.java` | +3 cases: the suffix parse (case and whitespace tolerant), the refusal of anything else, the absent filter. |
| `.../AdminIntegrationTest.java` | +a block: `?access=read-only` / `?access=read-write` / `?access=sideways`; a tenant-owned permission created, granted to that tenant's role (`201`) and refused to a global one (`422` with "global catalog only"). |
| `.../TenantSelfServiceIntegrationTest.java` | +a test: the tenant route's level filter over a permission the tenant defined, plus the invalid-level `422`. |
| `platform/keystone-admin-ui/test/permission_picker_test.dart` | the fake backend now filters by `access` exactly as the server does; `should_filter_the_catalogue_by_access_level`; `should_open_a_global_role_on_the_global_catalogue`; the owner-filter test replaced (the control is gone). |
| `.../test/permission_selection_test.dart` | `should_keep_only_the_catalogue_when_the_role_has_no_owner` (was `should_keep_any_owner_when_the_role_has_none`). |
| `.../test/list_query_test.dart` | the `permissionsFor` seed for a no-owner role, and the `access` field's round trip through every mutator. |

**Dependencies** — phases 3–5.

**Verification**

```
cd platform/keystone-admin       && mvn -pl platform/keystone-admin -am test
    → Tests run: 103, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS      (was 99)
cd platform/keystone-admin-ui    && flutter analyze → No issues found!
                                    flutter test    → 138 passed              (was 136)
cd apps/inventory/frontend       && flutter analyze → No issues found!
```

(`JAVA_HOME` pinned to JDK 25 at
`/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home`; Flutter 3.47.5 / Dart 3.13.4 at
`/Users/rajeshdebnath/manual-install/flutter/bin`. Docker was up, so the Testcontainers suites really ran — no
test was skipped.)

**Notes**

- The integration suites are the click-through that the environment cannot otherwise provide: real PostgreSQL,
  real Liquibase migrations, real Javalin routes, real `PermissionGuard`.
- The console's tests assert the **requests** the picker makes against a fake backend that implements the same
  filter, so a client-side "filter the page I hold" implementation could not pass
  (`should_filter_the_catalogue_by_access_level` asserts the rendered rows are the server's answer for
  `access=read-write`).
- Nothing was removed: the four previously existing tests that touch these code paths were updated to the new
  rule, not deleted, and the two suites' other cases still pass unchanged.
