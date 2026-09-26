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
        Supabase supabase,
        Security security,
        Bootstrap bootstrap) {

    public AdminConfig {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(supabase, "supabase");
        Objects.requireNonNull(security, "security");
        Objects.requireNonNull(bootstrap, "bootstrap");

        if (environment.isBlank()) {
            throw new IllegalArgumentException("environment must not be blank");
        }
        if (supabase.url().isBlank()) {
            throw new IllegalArgumentException("supabase.url is required (set in admin-config/application-{env}.yaml)");
        }
        if (supabase.serviceRoleKey().isBlank()) {
            throw new IllegalArgumentException("supabase.serviceRoleKey is required (set SUPABASE_SERVICE_ROLE_KEY)");
        }
        if (security.issuer().isBlank()) {
            throw new IllegalArgumentException("security.issuer is required (set in admin-config/application-{env}.yaml)");
        }
        if (security.jwksUrl().isBlank()) {
            throw new IllegalArgumentException("security.jwksUrl is required (set in admin-config/application-{env}.yaml)");
        }
        if (bootstrap.adminUsername().isBlank()) {
            throw new IllegalArgumentException("bootstrap.adminUsername is required (set in admin-config/application-{env}.yaml)");
        }
        if (bootstrap.adminEmail().isBlank()) {
            throw new IllegalArgumentException("bootstrap.adminEmail is required (set in admin-config/application-{env}.yaml)");
        }
        if (bootstrap.adminPassword().isBlank()) {
            throw new IllegalArgumentException("bootstrap.adminPassword is required (set BOOTSTRAP_ADMIN_PASSWORD)");
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
