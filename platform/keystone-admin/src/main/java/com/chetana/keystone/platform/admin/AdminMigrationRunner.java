package com.chetana.keystone.platform.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.platform.admin.data.Platform;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Applies the platform-admin Liquibase changelog at startup, targeting the {@code platform} schema
 * in the shared database.
 */
@Singleton
public final class AdminMigrationRunner {

    private static final String CHANGELOG = "admin-db/changelog/db.changelog-master.xml";

    private final DataSource dataSource;
    private final String schema;

    @Inject
    public AdminMigrationRunner(@Platform DataSource dataSource, @Platform DatabaseConfig config) {
        this.dataSource = dataSource;
        this.schema = config.schema();
    }

    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            createSchemaIfAbsent(connection);
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            if (schema != null && !schema.isBlank()) {
                database.setDefaultSchemaName(schema);
            }
            Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (Exception e) {
            throw new IllegalStateException("Database migration failed", e);
        }
    }

    private void createSchemaIfAbsent(Connection connection) throws SQLException {
        if (schema == null || schema.isBlank()) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(schema));
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
