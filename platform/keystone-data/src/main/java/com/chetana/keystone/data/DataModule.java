package com.chetana.keystone.data;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;

/**
 * Provides the application's {@link DataSource} (HikariCP) and jOOQ {@link DSLContext}.
 */
public final class DataModule extends AbstractModule {

    private final DatabaseConfig config;

    public DataModule(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    DataSource dataSource() {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(config.maxPoolSize());
        return new HikariDataSource(hikari);
    }

    @Provides
    @Singleton
    DSLContext dslContext(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
