package com.chetana.keystone.inventory;

import com.chetana.keystone.data.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class SchemaToolTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void should_migrate_drop_and_reset_schemas() throws Exception {
        DatabaseConfig inventoryDb = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "inventory");
        DatabaseConfig platformDb = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "platform");

        SchemaTool tool = new SchemaTool();

        // migrate — creates both schemas and their tables.
        tool.run(SchemaTool.Command.MIGRATE, inventoryDb, platformDb);
        assertThat(schemaExists("inventory")).isTrue();
        assertThat(schemaExists("platform")).isTrue();
        assertThat(tableExists("inventory", "items")).isTrue();
        assertThat(tableExists("platform", "tenants")).isTrue();

        // drop — removes both schemas (tables + Liquibase tracking).
        tool.run(SchemaTool.Command.DROP, inventoryDb, platformDb);
        assertThat(schemaExists("inventory")).isFalse();
        assertThat(schemaExists("platform")).isFalse();

        // reset — drop then migrate, back to a clean migrated state.
        tool.run(SchemaTool.Command.RESET, inventoryDb, platformDb);
        assertThat(schemaExists("inventory")).isTrue();
        assertThat(schemaExists("platform")).isTrue();
        assertThat(tableExists("inventory", "items")).isTrue();
        assertThat(tableExists("platform", "tenants")).isTrue();
    }

    @Test
    void should_migrate_idempotently_after_external_initialization() throws Exception {
        DatabaseConfig inventoryDb = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "inventory");
        DatabaseConfig platformDb = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "platform");

        // Simulate an externally-initialized database (objects exist WITHOUT Liquibase tracking).
        try (Connection connection = POSTGRES.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA inventory");
            statement.execute("SET search_path TO inventory");
            statement.execute("CREATE TABLE items (id UUID NOT NULL, name VARCHAR(255) NOT NULL, "
                    + "quantity INT NOT NULL, version BIGINT DEFAULT 0 NOT NULL, "
                    + "created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, "
                    + "CONSTRAINT items_pkey PRIMARY KEY (id))");

            statement.execute("CREATE SCHEMA platform");
            statement.execute("SET search_path TO platform");
            statement.execute("CREATE TABLE tenants (id UUID NOT NULL, name VARCHAR(255) NOT NULL, "
                    + "created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, "
                    + "CONSTRAINT tenants_pkey PRIMARY KEY (id), UNIQUE (name))");
            statement.execute("ALTER TABLE tenants ADD slug VARCHAR(255) NOT NULL");
        }

        SchemaTool tool = new SchemaTool();

        // Must not fail — preConditions mark the already-applied changesets as ran.
        tool.run(SchemaTool.Command.MIGRATE, inventoryDb, platformDb);

        assertThat(tableExists("inventory", "items")).isTrue();
        assertThat(tableExists("platform", "tenants")).isTrue();
    }

    private static boolean schemaExists(String schema) throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT 1 FROM pg_namespace WHERE nspname = '" + schema + "'")) {
            return rs.next();
        }
    }

    private static boolean tableExists(String schema, String table) throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT 1 FROM information_schema.tables WHERE table_schema = '"
                             + schema + "' AND table_name = '" + table + "'")) {
            return rs.next();
        }
    }
}
