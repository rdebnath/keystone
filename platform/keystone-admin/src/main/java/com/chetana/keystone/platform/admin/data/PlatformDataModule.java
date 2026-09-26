package com.chetana.keystone.platform.admin.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.data.JooqDataAccess;
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
 * Provides a {@link Platform}-qualified read-write {@link DataSource}/{@link DSLContext} and a
 * {@link PlatformReplica}-qualified read-replica pair targeting the platform schema, plus a
 * {@link Platform}-qualified {@link DataAccess} facade.
 *
 * <p>{@link Platform} remains the read-write (primary) qualifier for backward compatibility with
 * existing {@code @Platform DataSource}/{@code @Platform DSLContext} injections (e.g.
 * {@code AdminMigrationRunner}). When no replica config is supplied, the replica bindings alias
 * the primary.
 */
public final class PlatformDataModule extends AbstractModule {

    private final DatabaseConfig primary;
    private final DatabaseConfig replica;

    public PlatformDataModule(DatabaseConfig primary) {
        this(primary, null);
    }

    public PlatformDataModule(DatabaseConfig primary, DatabaseConfig replica) {
        this.primary = primary;
        this.replica = replica;
    }

    @Override
    protected void configure() {
        bind(DatabaseConfig.class).annotatedWith(Platform.class).toInstance(primary);
        if (replica != null) {
            bind(DatabaseConfig.class).annotatedWith(PlatformReplica.class).toInstance(replica);
        }
    }

    @Provides
    @Singleton
    @Platform
    DataSource platformDataSource() {
        return dataSource(primary, false);
    }

    @Provides
    @Singleton
    @Platform
    DSLContext platformDslContext(@Platform DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES, renderSettings());
    }

    @Provides
    @Singleton
    @PlatformReplica
    DataSource platformReplicaDataSource(@Platform DataSource primaryDataSource) {
        if (replica == null || replica.url().equals(primary.url())) {
            return primaryDataSource;
        }
        return dataSource(replica, true);
    }

    @Provides
    @Singleton
    @PlatformReplica
    DSLContext platformReplicaDslContext(@PlatformReplica DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES, renderSettings());
    }

    @Provides
    @Singleton
    @Platform
    DataAccess platformDataAccess(@Platform DSLContext primary, @PlatformReplica DSLContext replica) {
        return new JooqDataAccess(primary, replica);
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
     * Defensive fallback: renders any *unqualified* table reference as {@code "platform"."table"}.
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

