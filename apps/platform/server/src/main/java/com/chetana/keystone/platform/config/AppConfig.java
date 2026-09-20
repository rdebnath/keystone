package com.chetana.keystone.platform.config;

import java.util.Objects;

/**
 * Typed, immutable application configuration.
 *
 * <p>Resolved by {@link ConfigLoader} and bound into Guice via {@link ConfigModule}. Required
 * fields are validated here so a misconfigured deployment fails fast at startup rather than at
 * first request.
 */
public record AppConfig(
        String environment,
        Database database,
        Server server,
        Security security,
        Supabase supabase,
        Bootstrap bootstrap) {

    public AppConfig {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(security, "security");
        Objects.requireNonNull(supabase, "supabase");
        Objects.requireNonNull(bootstrap, "bootstrap");

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
        if (security.issuer().isBlank()) {
            throw new IllegalArgumentException("security.issuer is required (set OIDC_ISSUER)");
        }
        if (security.jwksUrl().isBlank()) {
            throw new IllegalArgumentException("security.jwksUrl is required (set OIDC_JWKS_URL)");
        }
        if (supabase.url().isBlank()) {
            throw new IllegalArgumentException("supabase.url is required (set SUPABASE_URL)");
        }
        if (supabase.serviceRoleKey().isBlank()) {
            throw new IllegalArgumentException("supabase.serviceRoleKey is required (set SUPABASE_SERVICE_ROLE_KEY)");
        }
        if (bootstrap.adminEmail().isBlank()) {
            throw new IllegalArgumentException("bootstrap.adminEmail is required (set BOOTSTRAP_ADMIN_EMAIL)");
        }
        if (bootstrap.adminPassword().isBlank()) {
            throw new IllegalArgumentException("bootstrap.adminPassword is required (set BOOTSTRAP_ADMIN_PASSWORD)");
        }
    }

    public record Database(String url, String username, String password, int maxPoolSize) {
        public Database {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
        }
    }

    public record Server(int port) {
    }

    public record Security(String issuer, String audience, String jwksUrl) {
        public Security {
            Objects.requireNonNull(issuer, "issuer");
            Objects.requireNonNull(audience, "audience");
            Objects.requireNonNull(jwksUrl, "jwksUrl");
        }
    }

    public record Supabase(String url, String serviceRoleKey) {
        public Supabase {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(serviceRoleKey, "serviceRoleKey");
        }
    }

    public record Bootstrap(String adminEmail, String adminPassword) {
        public Bootstrap {
            Objects.requireNonNull(adminEmail, "adminEmail");
            Objects.requireNonNull(adminPassword, "adminPassword");
        }
    }
}
