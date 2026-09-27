package com.chetana.keystone.platform.admin;

import com.chetana.keystone.platform.admin.identity.Access;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The catalog carries exactly two access levels per resource plus the wildcard, and nothing else.
 */
class PermissionCatalogTest {

    @Test
    void should_seed_exactly_two_levels_per_resource() {
        assertThat(PermissionCatalog.PERMISSIONS).hasSize(PermissionCatalog.RESOURCES.size() * 2 + 1);

        for (PermissionCatalog.Resource resource : PermissionCatalog.RESOURCES) {
            assertThat(codesFor(resource)).containsExactly(
                    PermissionCatalog.code(resource.code(), Access.READ_ONLY),
                    PermissionCatalog.code(resource.code(), Access.READ_WRITE));
        }
    }

    @Test
    void should_seed_unique_codes_that_all_carry_a_level() {
        assertThat(codes()).doesNotHaveDuplicates();
        assertThat(codes()).allSatisfy(code -> assertThat(
                        code.equals(PermissionCatalog.WILDCARD) || PermissionCatalog.hasAccessLevel(code))
                .isTrue());
    }

    @Test
    void should_take_each_codes_scope_from_its_resource() {
        for (PermissionCatalog.Resource resource : PermissionCatalog.RESOURCES) {
            List<PermissionCatalog.Permission> seeded = PermissionCatalog.PERMISSIONS.stream()
                    .filter(permission -> permission.code().startsWith(resource.code() + ":"))
                    .toList();

            assertThat(seeded).isNotEmpty();
            assertThat(seeded).allSatisfy(
                    permission -> assertThat(permission.scope()).isEqualTo(resource.scope()));
        }
    }

    @Test
    void should_accept_the_read_only_and_read_write_codes_for_a_read_check() {
        assertThat(PermissionCatalog.acceptedCodes(PermissionCatalog.PLATFORM_TENANT, Access.READ_ONLY))
                .containsExactly("platform:tenant:read-only", "platform:tenant:read-write");
        assertThat(PermissionCatalog.acceptedCodes(PermissionCatalog.PLATFORM_TENANT, Access.READ_WRITE))
                .containsExactly("platform:tenant:read-write");
    }

    @Test
    void should_recognise_only_the_two_level_suffixes() {
        assertThat(PermissionCatalog.hasAccessLevel("platform:tenant:read-only")).isTrue();
        assertThat(PermissionCatalog.hasAccessLevel("platform:tenant:read-write")).isTrue();
        assertThat(PermissionCatalog.hasAccessLevel("platform:tenant:create")).isFalse();
        assertThat(PermissionCatalog.hasAccessLevel("platform:tenant:read")).isFalse();
        assertThat(PermissionCatalog.hasAccessLevel("platform:tenant")).isFalse();
        assertThat(PermissionCatalog.hasAccessLevel(PermissionCatalog.WILDCARD)).isFalse();
    }

    @Test
    void should_seed_only_the_two_access_levels_and_the_wildcard() {
        assertThat(codes()).containsExactlyInAnyOrder(
                PermissionCatalog.WILDCARD,
                "platform:tenant:read-only",
                "platform:tenant:read-write",
                "platform:role:read-only",
                "platform:role:read-write",
                "platform:permission:read-only",
                "platform:permission:read-write",
                "platform:user:read-only",
                "platform:user:read-write",
                "tenant:role:read-only",
                "tenant:role:read-write",
                "tenant:permission:read-only",
                "tenant:permission:read-write",
                "tenant:user:read-only",
                "tenant:user:read-write");
    }

    private static List<String> codesFor(PermissionCatalog.Resource resource) {
        return codes().stream().filter(code -> code.startsWith(resource.code() + ":")).toList();
    }

    private static List<String> codes() {
        return PermissionCatalog.PERMISSIONS.stream().map(PermissionCatalog.Permission::code).toList();
    }
}
