package com.chetana.keystone.data;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.table;

@Testcontainers(disabledWithoutDocker = true)
class DataModuleTest {

    @Container
    static final PostgreSQLContainer<?> PRIMARY = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final PostgreSQLContainer<?> REPLICA = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void should_route_reads_to_replica_and_writes_to_primary() {
        createTable(PRIMARY, "routed_items");
        createTable(REPLICA, "routed_items");

        DataAccess data = injector(PRIMARY, REPLICA);

        data.write().execute("insert into routed_items (id, name) values (1, 'widget')");

        // The replica is a separate instance with no replication, so the just-written row is not
        // visible there — proving reads go to the replica, not the primary.
        assertThat(data.read().fetchCount(table("routed_items"))).isZero();
        // read-your-writes forces the read back onto the primary.
        assertThat(data.readFromPrimary(() -> data.read().fetchCount(table("routed_items")))).isEqualTo(1);
    }

    @Test
    void should_route_reads_and_writes_to_same_instance_when_no_replica_configured() {
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new DataModule(config(PRIMARY)));
        DataAccess data = injector.getInstance(DataAccess.class);

        createTable(PRIMARY, "aliased_items");
        data.write().execute("insert into aliased_items (id, name) values (1, 'widget')");
        assertThat(data.read().fetchCount(table("aliased_items"))).isEqualTo(1);
    }

    private static DataAccess injector(PostgreSQLContainer<?> primary, PostgreSQLContainer<?> replica) {
        Injector injector = Guice.createInjector(Stage.PRODUCTION,
                new DataModule(config(primary), config(replica)));
        return injector.getInstance(DataAccess.class);
    }

    private static DatabaseConfig config(PostgreSQLContainer<?> container) {
        return DatabaseConfig.of(container.getJdbcUrl(), container.getUsername(), container.getPassword(), 5, "");
    }

    private static void createTable(PostgreSQLContainer<?> container, String table) {
        try (Connection connection = container.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.execute("create table " + table + " (id bigint primary key, name text)");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create table " + table, e);
        }
    }
}
