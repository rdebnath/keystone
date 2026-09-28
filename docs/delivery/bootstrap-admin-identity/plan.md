# Small Change — Bootstrap admin identity from yaml

**Slug:** `bootstrap-admin-identity`
**Level:** Platform-level (`platform/keystone-admin` resources + tests, `docs/`).

## Sizing decision

**Skip the phased plan** (delivery skill, Step 1): this is a config change in one loader plus its
tests and comments — no new capability, no schema change, no API contract change, no security
change. It is recorded here only so the request is tracked; there are no phase files.

## The problem

`bootstrap.adminUsername` and `bootstrap.adminEmail` *are* read from
`admin-config/application.yaml` / `admin-config/application-{env}.yaml`, but
`AdminConfigLoader.bootstrap(...)` also carries **hardcoded Java fallbacks**
(`"admin"`, `"admin@keystone.com"`, `"changeit"`):

```java
fileString(overlay, base, "admin", "bootstrap", "adminUsername"),
fileString(overlay, base, "admin@keystone.com", "bootstrap", "adminEmail"),
secret(env, overlay, base, "BOOTSTRAP_ADMIN_PASSWORD", "changeit", "bootstrap", "adminPassword"));
```

So the values look hardcoded in the code, yaml is not the single source of truth, and a missing or
misspelled key silently falls back instead of failing loudly.

## The change

1. `AdminConfigLoader.bootstrap(...)` — drop the Java literals: empty-string fallback for
   `adminUsername` / `adminEmail` (yaml is authoritative) and for the password (the yaml value is
   picked up before the fallback is ever reached).
   - The existing `AdminConfig` validation already fails fast when the bootstrap is enabled and a
     value is blank, and its messages already point at
     `admin-config/application-{env}.yaml` / `BOOTSTRAP_ADMIN_PASSWORD`.
2. `admin-config/application.yaml` — keep `admin` / `admin@keystone.com` / `changeit` as the shipped
   **defaults**, with a comment stating they are defaults to override per environment (that is how
   you point the bootstrap at a different email or username).
3. `admin-config/application-{env}.yaml` — add a commented `bootstrap:` block to `dev`/`demo`
   showing the override (a different `adminUsername` / `adminEmail`) so the per-environment override
   is discoverable, not just documented.
4. Tests (`AdminConfigLoaderTest`) — add: a per-env yaml value wins over the base yaml, and a
   missing `bootstrap.adminUsername`/`adminEmail` while enabled fails fast (`IllegalArgumentException`).
   Existing test `should_override_only_bootstrap_password_secret_with_environment_variable` keeps
   asserting that `BOOTSTRAP_ADMIN_EMAIL` is ignored in favour of yaml — unchanged behaviour.
5. `docs/ARCHITECTURE.md` §8 — one line: the bootstrap admin username/email are non-secret yaml
   values (per environment); only the password is a secret (`BOOTSTRAP_ADMIN_PASSWORD`).
6. `CHANGELOG.md` — one bullet under Unreleased → Changed.

**Not changing:** the password mechanism (env `BOOTSTRAP_ADMIN_PASSWORD` wins, yaml is the
fallback). The request was about username/email; the password stays a secret by design.

## Verification

- `mvn -pl platform/keystone-admin test` — `AdminConfigLoaderTest` green (new cases included).
- `mvn -pl apps/inventory/server test -Dtest=BootstrapToolTest` — unchanged (it passes
  `AdminConfig.Bootstrap` directly).
- Manual: set a different `adminUsername`/`adminEmail` in `admin-config/application-dev.yaml`,
  run the bootstrap, and confirm the seeded platform user carries the new values.

## Confirmation state

- [x] Applied (2026-09-27).
- [x] `mvn -pl platform/keystone-admin -am test` → **64 tests, 0 failures, 0 skipped** (the new
      `AdminConfigLoaderTest` cases included).

## What was done

| File | Change |
| --- | --- |
| `…/admin/config/AdminConfigLoader.java` | dropped the `"admin"` / `"admin@keystone.com"` / `"changeit"` Java fallbacks: the bootstrap identity resolves from yaml only, and a missing value reaches `AdminConfig`'s fail-fast validation. Javadoc records the rule. |
| `admin-config/application.yaml` | `adminUsername` / `adminEmail` are now blank, documented placeholders ("required per environment"); `adminPassword` stays `changeit` as the development default. |
| `admin-config/application-dev.yaml`, `application-demo.yaml` | carry the previous values explicitly (`admin` / `admin@keystone.com`) with a comment saying they are per-deployment — **this is the file to edit to change the bootstrap user**. |
| `…/config/AdminConfigLoaderTest.java` | added `should_take_the_bootstrap_identity_from_the_environment_yaml` (an env file naming `root` / `root@acme.test` is used — proving nothing is built in) and `should_fail_fast_when_the_environment_yaml_omits_the_bootstrap_identity`. |
| `src/test/resources/admin-config/application-override.yaml`, `application-nobootstrap.yaml` | new test environments for those two cases. |
| `docs/ARCHITECTURE.md` §8 | the bootstrap admin identity is listed as a non-secret per-environment yaml value with no built-in default. |
| `CHANGELOG.md` | Unreleased → Changed. |

**Behaviour:** unchanged for dev/demo (same `admin` / `admin@keystone.com`), but the values now come
from the environment file rather than from a code default, and an environment that omits them fails
fast at startup instead of silently bootstrapping the built-in administrator.

**Not changed:** the password mechanism — `BOOTSTRAP_ADMIN_PASSWORD` (secret) wins, yaml is the
fallback, and the base file keeps `changeit` so the scripts' documented default still holds.

**To use a different bootstrap user now:** edit `bootstrap.adminUsername` / `bootstrap.adminEmail` in
`platform/keystone-admin/src/main/resources/admin-config/application-{env}.yaml`.

