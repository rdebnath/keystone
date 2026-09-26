package com.chetana.keystone.platform.admin.config;

import java.util.Objects;

/**
 * Typed, immutable configuration for the platform admin console library.
 *
 * <p>Resolved by {@link AdminConfigLoader} and bound into Guice via {@link AdminConfigModule}.
 * Required fields are validated here so a misconfigured deployment fails fast at startup rather
 * than at first request.
 */
public record AdminConfig(
        String environment,
        Database database,
        Supabase supabase,
        Security security,
        Bootstrap bootstrap) {

    public AdminConfig {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(supabase, "supabase");
        Objects.requireNonNull(security, "security");
        Objects.requireNonNull(bootstrap, "bootstrap");

        if (environment.isBlank()) {
            throw new IllegalArgumentException("environment must not be blank");
        }
        if (database.url().isBlank()) {
            throw new IllegalArgumentException("database.url is required (set DB_URL or admin-config/application-{env}.yaml)");
        }
        if (database.username().isBlank()) {
            throw new IllegalArgumentException("database.username is required (set DB_USERNAME or admin-config/application-{env}.yaml)");
        }
        if (database.password().isBlank()) {
            throw new IllegalArgumentException("database.password is required (set DB_PASSWORD or admin-config/application-local.yaml)");
        }
        if (database.read().url().isBlank()) {
            throw new IllegalArgumentException("database.read.url is required (set in admin-config/application-{env}.yaml)");
        }
        if (supabase.url().isBlank()) {
            throw new IllegalArgumentException("supabase.url is required (set SUPABASE_URL)");
        }
        if (supabase.serviceRoleKey().isBlank()) {
            throw new IllegalArgumentException("supabase.serviceRoleKey is required (set SUPABASE_SERVICE_ROLE_KEY)");
        }
        if (security.issuer().isBlank()) {
            throw new IllegalArgumentException("security.issuer is required (set OIDC_ISSUER)");
        }
        if (security.jwksUrl().isBlank()) {
            throw new IllegalArgumentException("security.jwksUrl is required (set OIDC_JWKS_URL)");
        }
        if (bootstrap.adminUsername().isBlank()) {
            throw new IllegalArgumentException("bootstrap.adminUsername is required (set BOOTSTRAP_ADMIN_USERNAME)");
        }
        if (bootstrap.adminEmail().isBlank()) {
            throw new IllegalArgumentException("bootstrap.adminEmail is required (set BOOTSTRAP_ADMIN_EMAIL)");
        }
        if (bootstrap.adminPassword().isBlank()) {
            throw new IllegalArgumentException("bootstrap.adminPassword is required (set BOOTSTRAP_ADMIN_PASSWORD)");
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
     * {@link AdminConfigLoader} from {@code database.read.*} (yaml only, no env override); blank
     * values fall back to the primary {@link Database}.
     */
    public record Read(String url, String username, String password, int maxPoolSize) {
        public Read {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
        }
    }

    public record Supabase(String url, String serviceRoleKey) {
        public Supabase {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(serviceRoleKey, "serviceRoleKey");
        }
    }

    public record Security(String issuer, String audience, String jwksUrl) {
        public Security {
            Objects.requireNonNull(issuer, "issuer");
            Objects.requireNonNull(audience, "audience");
            Objects.requireNonNull(jwksUrl, "jwksUrl");
        }
    }

    public record Bootstrap(String adminUsername, String adminEmail, String adminPassword) {
        public Bootstrap {
            Objects.requireNonNull(adminUsername, "adminUsername");
            Objects.requireNonNull(adminEmail, "adminEmail");
            Objects.requireNonNull(adminPassword, "adminPassword");
        }
    }
}
