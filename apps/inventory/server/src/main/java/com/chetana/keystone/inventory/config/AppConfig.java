package com.chetana.keystone.inventory.config;

import java.util.Objects;

/**
 * Typed, immutable application configuration.
 *
 * <p>Resolved by {@link ConfigLoader} and bound into Guice via {@link ConfigModule}. Every field
 * is non-null; optional sections ({@link Realtime}, {@link Security}) default to blank strings
 * until their feature is wired. Required fields are validated here so a misconfigured deployment
 * fails fast at startup rather than at first request.
 */
public record AppConfig(
        String environment,
        Database database,
        Server server,
        Realtime realtime,
        Security security) {

    public AppConfig {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(realtime, "realtime");
        Objects.requireNonNull(security, "security");

        if (environment.isBlank()) {
            throw new IllegalArgumentException("environment must not be blank");
        }
        if (database.url().isBlank()) {
            throw new IllegalArgumentException("database.url is required (set DB_URL or application-{env}.yaml)");
        }
        if (database.username().isBlank()) {
            throw new IllegalArgumentException("database.username is required (set DB_USERNAME or application-{env}.yaml)");
        }
        if (database.password().isBlank()) {
            throw new IllegalArgumentException("database.password is required (set DB_PASSWORD or application-local.yaml)");
        }
        if (server.port() < 1 || server.port() > 65535) {
            throw new IllegalArgumentException("server.port must be in [1, 65535]: " + server.port());
        }
    }

    public record Database(String url, String username, String password, int maxPoolSize, String schema) {
        public Database {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
            Objects.requireNonNull(schema, "schema");
        }
    }

    public record Server(int port) {
    }

    public record Realtime(String endpoint, String serviceRoleKey) {
        public Realtime {
            Objects.requireNonNull(endpoint, "endpoint");
            Objects.requireNonNull(serviceRoleKey, "serviceRoleKey");
        }
    }

    public record Security(String issuer, String audience) {
        public Security {
            Objects.requireNonNull(issuer, "issuer");
            Objects.requireNonNull(audience, "audience");
        }
    }
}
