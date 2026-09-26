package com.chetana.keystone.inventory;

import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.inventory.config.AppConfig;
import com.chetana.keystone.inventory.config.ConfigLoader;
import com.chetana.keystone.platform.admin.AdminMigrationRunner;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 * Command-line schema management for the shared Supabase/PostgreSQL database.
 *
 * <p>Commands:
 * <ul>
 *   <li>{@code migrate} — create/update the {@code inventory} and {@code platform} schemas
 *       (Liquibase, forward-only).</li>
 *   <li>{@code drop} — drop both schemas (tables, indexes, and Liquibase tracking). Destructive.</li>
 *   <li>{@code reset} — drop, then migrate.</li>
 * </ul>
 *
 * <p>Environment selection works like the server: {@code APP_ENV} (default {@code dev}) picks the
 * per-environment config file, and {@code DB_PASSWORD} supplies the password. Supabase/OIDC/bootstrap
 * variables are NOT required. Example:
 *
 * <pre>
 * APP_ENV=demo DB_PASSWORD=... java -cp "apps/inventory/server/target/classes:$(cat apps/inventory/server/target/classpath.txt)" com.chetana.keystone.inventory.SchemaTool migrate
 * </pre>
 */
public final class SchemaTool {

    public static void main(String[] args) {
        new SchemaTool().execute(args);
    }

    /** CLI plumbing: parse args, load configuration, and dispatch to {@link #run}. */
    void execute(String[] args) {
        if (args.length != 1) {
            usage();
            System.exit(2);
            return;
        }

        Command command;
        try {
            command = Command.parse(args[0]);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            usage();
            System.exit(2);
            return;
        }

        try {
            AppConfig inventory = ConfigLoader.load();
            DatabaseConfig inventoryDb = DatabaseConfig.of(
                    inventory.database().url(),
                    inventory.database().username(),
                    inventory.database().password(),
                    inventory.database().maxPoolSize(),
                    inventory.database().schema());
            DatabaseConfig platformDb = DatabaseConfig.of(
                    inventory.database().url(),
                    inventory.database().username(),
                    inventory.database().password(),
                    inventory.database().maxPoolSize(),
                    inventory.platform().schema());

            run(command, inventoryDb, platformDb);
            System.out.println("SchemaTool " + command.name().toLowerCase(Locale.ROOT)
                    + " completed for schemas: " + inventoryDb.schema() + ", " + platformDb.schema());
        } catch (Exception e) {
            System.err.println("SchemaTool failed: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /** Runs the requested command against the two schema-scoped databases. Package-private for tests. */
    void run(Command command, DatabaseConfig inventoryDb, DatabaseConfig platformDb) throws Exception {
        try (HikariDataSource inventoryDs = dataSource(inventoryDb);
             HikariDataSource platformDs = dataSource(platformDb)) {

            MigrationRunner inventoryMigration = new MigrationRunner(inventoryDs, inventoryDb);
            AdminMigrationRunner platformMigration = new AdminMigrationRunner(platformDs, platformDb);

            switch (command) {
                case MIGRATE -> {
                    inventoryMigration.migrate();
                    platformMigration.migrate();
                }
                case DROP -> {
                    dropSchema(inventoryDs, inventoryDb.schema());
                    dropSchema(platformDs, platformDb.schema());
                }
                case RESET -> {
                    dropSchema(inventoryDs, inventoryDb.schema());
                    dropSchema(platformDs, platformDb.schema());
                    inventoryMigration.migrate();
                    platformMigration.migrate();
                }
            }
        }
    }

    private HikariDataSource dataSource(DatabaseConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(config.maxPoolSize());
        // Supabase's connection pooler (PgBouncer, transaction mode) does not support server-side
        // prepared statements; force client-side prepared statements to avoid Liquibase's
        // "prepared statement already exists" failures.
        hikari.addDataSourceProperty("prepareThreshold", "0");
        return new HikariDataSource(hikari);
    }

    /** Drops a schema and everything in it (tables, indexes, sequences, and Liquibase tracking). */
    void dropSchema(DataSource dataSource, String schema) throws SQLException {
        if (schema == null || schema.isBlank()) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + quoteIdentifier(schema) + " CASCADE");
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private void usage() {
        System.err.println("Usage: SchemaTool <migrate|drop|reset>");
    }

    enum Command {
        MIGRATE,
        DROP,
        RESET;

        static Command parse(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing command; expected migrate, drop or reset.");
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Unknown command '" + value + "'; expected migrate, drop or reset.");
            }
        }
    }
}
