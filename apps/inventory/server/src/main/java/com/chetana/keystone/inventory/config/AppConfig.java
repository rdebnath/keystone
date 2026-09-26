package com.chetana.keystone.inventory.config;

import java.util.List;
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
        Platform platform,
        Server server,
        Cors cors,
        Realtime realtime,
        Security security) {

    public AppConfig {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(cors, "cors");
        Objects.requireNonNull(realtime, "realtime");
        Objects.requireNonNull(security, "security");

        if (environment.isBlank()) {
            throw new IllegalArgumentException("environment must not be blank");
        }
        if (database.url().isBlank()) {
            throw new IllegalArgumentException("database.url is required (set in application-{env}.yaml)");
        }
        if (database.username().isBlank()) {
            throw new IllegalArgumentException("database.username is required (set in application-{env}.yaml)");
        }
        if (database.password().isBlank()) {
            throw new IllegalArgumentException("database.password is required (set DB_PASSWORD)");
        }
        if (database.read().url().isBlank()) {
            throw new IllegalArgumentException("database.read.url is required (set in application-{env}.yaml)");
        }
        if (platform.schema().isBlank()) {
            throw new IllegalArgumentException("platform.schema must not be blank (set in application-{env}.yaml)");
        }
        if (server.port() < 1 || server.port() > 65535) {
            throw new IllegalArgumentException("server.port must be in [1, 65535]: " + server.port());
        }
        if (!server.contextPath().isBlank() && !server.contextPath().startsWith("/")) {
            throw new IllegalArgumentException("server.contextPath must start with '/': " + server.contextPath());
        }
    }

    public record Database(String url, String username, String password, int maxPoolSize, String schema, Read read) {
        public Database {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
            Objects.requireNonNull(schema, "schema");
            Objects.requireNonNull(read, "read");
        }
    }

    /**
     * Read target — a read replica, or the primary for read/write on one instance. Resolved by
     * {@link ConfigLoader} from {@code database.read.*} (yaml only, no env override); blank values
     * fall back to the primary {@link Database}.
     */
    public record Read(String url, String username, String password, int maxPoolSize) {
        public Read {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
        }
    }

    public record Platform(String schema) {
        public Platform {
            Objects.requireNonNull(schema, "schema");
        }
    }

    public record Server(int port, String contextPath) {
        public Server {
            Objects.requireNonNull(contextPath, "contextPath");
        }
    }

    /**
     * Cross-origin policy for browser clients (the Flutter web frontend). An empty list disables
     * CORS; the wildcard {@code "*"} allows any origin; any other entry is an allowed origin.
     */
    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = List.copyOf(allowedOrigins);
        }
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
