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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The indexes the paged lists rely on exist after {@code 0005} — paging turns every list into
 * {@code ORDER BY <default order> LIMIT n OFFSET m}, so a missing index means a sort of the whole table per
 * page. Asserted against a real PostgreSQL, like the other schema tests: a changelog that never ran and a
 * changelog that ran are indistinguishable from the source alone.
 */
@Testcontainers(disabledWithoutDocker = true)
class PagingIndexesSchemaTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private DSLContext db;

    @BeforeEach
    void migrate() {
        DatabaseConfig config = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "platform");
        Injector injector = Guice.createInjector(new PlatformDataModule(config));
        // Run twice: the changesets are guarded, so a second run must be a no-op rather than an error.
        injector.getInstance(AdminMigrationRunner.class).migrate();
        injector.getInstance(AdminMigrationRunner.class).migrate();
        db = injector.getInstance(Key.get(DSLContext.class, Platform.class));
    }

    @Test
    void should_index_the_default_order_of_the_tenants_list() {
        assertThat(indexNames()).contains("idx_tenants_name");
    }

    @Test
    void should_index_the_default_orders_of_the_users_lists() {
        assertThat(indexNames()).contains("idx_users_username", "idx_users_tenant_username");
    }

    @Test
    void should_lead_the_composite_users_index_with_the_tenant() {
        // The composite serves the tenant-scoped list (`WHERE tenant_id = ? ORDER BY username`), so the
        // tenant must come first; the plain index serves the platform plane's unfiltered list.
        assertThat(indexDefinition("idx_users_tenant_username")).contains("(tenant_id, username)");
    }

    private List<String> indexNames() {
        return db.fetch("select indexname from pg_indexes where schemaname = current_schema()")
                .map(record -> record.get("indexname", String.class));
    }

    private String indexDefinition(String indexName) {
        return db.fetch("select indexdef from pg_indexes where indexname = ?", indexName)
                .map(record -> record.get("indexdef", String.class))
                .getFirst();
    }
}
