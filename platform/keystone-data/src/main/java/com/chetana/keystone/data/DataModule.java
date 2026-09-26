package com.chetana.keystone.data;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import javax.sql.DataSource;

/**
 * Provides the application's primary (read-write) and replica (read-only) {@link DataSource}s
 * (HikariCP) and jOOQ {@link DSLContext}s, plus the read/write {@link DataAccess} facade.
 *
 * <p>Constructing with a single {@link DatabaseConfig} keeps the previous behavior: the replica
 * bindings alias the primary, so reads and writes hit the same instance (local dev, tests, or a
 * deployment without a read replica). Passing a second config enables a real read replica.
 */
public final class DataModule extends AbstractModule {

    private final DatabaseConfig primary;
    private final DatabaseConfig replica;

    public DataModule(DatabaseConfig primary) {
        this(primary, null);
    }

    public DataModule(DatabaseConfig primary, DatabaseConfig replica) {
        this.primary = primary;
        this.replica = replica;
    }

    @Override
    protected void configure() {
        bind(DatabaseConfig.class).toInstance(primary);
        if (replica != null) {
            bind(Key.get(DatabaseConfig.class, Replica.class)).toInstance(replica);
        }
        // Backward compatibility: the unnamed DataSource/DSLContext remain the read-write primary,
        // so migrations (MigrationRunner) and any not-yet-migrated code keep writing to the primary.
        bind(DataSource.class).to(Key.get(DataSource.class, Primary.class));
        bind(DSLContext.class).to(Key.get(DSLContext.class, Primary.class));

        bind(DataAccess.class).to(JooqDataAccess.class);
    }

    @Provides
    @Singleton
    @Primary
    DataSource primaryDataSource() {
        return dataSource(primary, false);
    }

    @Provides
    @Singleton
    @Primary
    DSLContext primaryDslContext(@Primary DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES, renderSettings());
    }

    @Provides
    @Singleton
    @Replica
    DataSource replicaDataSource(@Primary DataSource primaryDataSource) {
        if (replica == null || replica.url().equals(primary.url())) {
            return primaryDataSource;
        }
        return dataSource(replica, true);
    }

    @Provides
    @Singleton
    @Replica
    DSLContext replicaDslContext(@Replica DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES, renderSettings());
    }

    private static DataSource dataSource(DatabaseConfig config, boolean readOnly) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(config.maxPoolSize());
        hikari.setReadOnly(readOnly);
        // Supabase's connection pooler (PgBouncer, transaction mode) does not support server-side
        // prepared statements; force client-side prepared statements to avoid Liquibase's
        // "prepared statement already exists" failures.
        hikari.addDataSourceProperty("prepareThreshold", "0");
        if (config.schema() != null && !config.schema().isBlank()) {
            hikari.setSchema(config.schema());
        }
        return new HikariDataSource(hikari);
    }

    /**
     * Defensive fallback: renders any *unqualified* table reference as {@code "schema"."table"}.
     * Generated tables already carry their own schema (so cross-schema joins render correctly), but
     * this mapping guarantees unqualified SQL never silently depends on the connection's
     * {@code search_path}, which Supabase's PgBouncer (transaction mode, port 6543) does not
     * reliably maintain across transactions.
     */
    private Settings renderSettings() {
        String schema = primary.schema();
        if (schema == null || schema.isBlank()) {
            return new Settings();
        }
        return new Settings().withRenderMapping(
                new RenderMapping().withSchemata(
                        new MappedSchema().withInput("").withOutput(schema)));
    }
}
