package com.chetana.keystone.inventory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Applies the Liquibase changelog at startup.
 */
@Singleton
public final class MigrationRunner {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";

    private final DataSource dataSource;

    @Inject
    public MigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (Exception e) {
            throw new IllegalStateException("Database migration failed", e);
        }
    }
}
