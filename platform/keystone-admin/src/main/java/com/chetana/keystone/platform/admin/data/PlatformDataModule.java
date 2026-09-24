package com.chetana.keystone.platform.admin.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.chetana.keystone.data.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;

/**
 * Provides a {@link Platform}-qualified {@link DataSource} (HikariCP) and jOOQ {@link DSLContext}
 * targeting the platform schema, so the admin library can coexist with a hosting application that
 * owns its own (differently-schema'd) {@code DataSource}.
 */
public final class PlatformDataModule extends AbstractModule {

    private final DatabaseConfig config;

    public PlatformDataModule(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(DatabaseConfig.class).annotatedWith(Platform.class).toInstance(config);
    }

    @Provides
    @Singleton
    @Platform
    DataSource platformDataSource() {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(config.maxPoolSize());
        if (config.schema() != null && !config.schema().isBlank()) {
            hikari.setSchema(config.schema());
        }
        return new HikariDataSource(hikari);
    }

    @Provides
    @Singleton
    @Platform
    DSLContext platformDslContext(@Platform DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
