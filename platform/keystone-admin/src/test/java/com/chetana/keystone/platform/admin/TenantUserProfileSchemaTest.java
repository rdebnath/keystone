package com.chetana.keystone.platform.admin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.data.PlatformDataModule;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.TENANTS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USERS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The optional profile columns {@code 0004} adds, asserted against a real PostgreSQL rather than assumed:
 * both exist, both are nullable, and a row written without them is valid — so migrating a populated
 * database cannot fail on a missing value, and neither field has to be supplied to create a tenant or a
 * user.
 */
@Testcontainers(disabledWithoutDocker = true)
class TenantUserProfileSchemaTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private DSLContext db;

    @BeforeEach
    void migrate() {
        DatabaseConfig config = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "platform");
        Injector injector = Guice.createInjector(new PlatformDataModule(config));
        injector.getInstance(AdminMigrationRunner.class).migrate();
        db = injector.getInstance(Key.get(DSLContext.class, Platform.class));
    }

    @Test
    void should_carry_a_nullable_two_character_country_on_tenants() {
        assertThat(isNullable("tenants", "country")).isTrue();
        assertThat(maxLength("tenants", "country")).isEqualTo(2);
    }

    @Test
    void should_carry_a_nullable_phone_number_with_room_for_e164_on_users() {
        // E.164 is at most 16 characters ('+' + 15 digits); the column is wider so a legacy value cannot
        // be silently truncated by the database.
        assertThat(isNullable("users", "phone_number")).isTrue();
        assertThat(maxLength("users", "phone_number")).isEqualTo(32);
    }

    @Test
    void should_accept_a_tenant_written_without_a_country() {
        UUID id = insertTenantWithoutProfileFields();

        assertThat(db.select(TENANTS.COUNTRY).from(TENANTS).where(TENANTS.ID.eq(id)).fetchOne(TENANTS.COUNTRY))
                .isNull();
    }

    @Test
    void should_accept_a_user_written_without_a_phone_number_and_clear_it_again() {
        UUID id = insertTenantWithoutProfileFields();
        UUID userId = insertUserWithoutProfileFields(id);

        assertThat(phoneNumber(userId)).isNull();

        // A number can be recorded and cleared, which is what a blank value in the API means.
        updatePhoneNumber(userId, "+919876543210");
        assertThat(phoneNumber(userId)).isEqualTo("+919876543210");

        updatePhoneNumber(userId, null);
        assertThat(phoneNumber(userId)).isNull();
    }

    private UUID insertTenantWithoutProfileFields() {
        UUID id = UUID.randomUUID();
        String slug = "profile-" + UUID.randomUUID().toString().substring(0, 8);
        db.insertInto(TENANTS, TENANTS.ID, TENANTS.NAME, TENANTS.SLUG, TENANTS.CREATED_AT, TENANTS.UPDATED_AT)
                .values(id, slug, slug, NOW, NOW)
                .execute();
        return id;
    }

    private UUID insertUserWithoutProfileFields(UUID tenantId) {
        UUID id = UUID.randomUUID();
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        db.insertInto(USERS, USERS.ID, USERS.SUB, USERS.USERNAME, USERS.EMAIL, USERS.TENANT_ID,
                        USERS.MUST_CHANGE_PASSWORD, USERS.CREATED_AT, USERS.UPDATED_AT)
                .values(id, "sub:" + username, username, username + "@profile.test", tenantId, false, NOW, NOW)
                .execute();
        return id;
    }

    private void updatePhoneNumber(UUID userId, String phoneNumber) {
        db.update(USERS)
                .set(USERS.PHONE_NUMBER, phoneNumber)
                .set(USERS.UPDATED_AT, NOW)
                .where(USERS.ID.eq(userId))
                .execute();
    }

    private String phoneNumber(UUID userId) {
        return db.select(USERS.PHONE_NUMBER).from(USERS).where(USERS.ID.eq(userId)).fetchOne(USERS.PHONE_NUMBER);
    }

    private boolean isNullable(String table, String column) {
        List<String> nullable = db.fetch("""
                select is_nullable from information_schema.columns
                 where table_schema = 'platform' and table_name = ? and column_name = ?
                """, table, column).into(String.class);
        return "YES".equals(nullable.getFirst());
    }

    private Integer maxLength(String table, String column) {
        List<Integer> lengths = db.fetch("""
                select character_maximum_length from information_schema.columns
                 where table_schema = 'platform' and table_name = ? and column_name = ?
                """, table, column).into(Integer.class);
        return lengths.getFirst();
    }
}
