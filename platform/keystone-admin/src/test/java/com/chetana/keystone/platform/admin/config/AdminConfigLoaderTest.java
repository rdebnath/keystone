package com.chetana.keystone.platform.admin.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminConfigLoaderTest {

    @Test
    void should_load_defaults_with_required_env() {
        AdminConfig config = AdminConfigLoader.load(Map.of(
                "SUPABASE_URL", "https://x.supabase.co",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role",
                "OIDC_ISSUER", "https://x.supabase.co",
                "OIDC_JWKS_URL", "https://x.supabase.co/auth/v1/.well-known/jwks.json"));

        assertThat(config.environment()).isEqualTo("local");
        assertThat(config.supabase().url()).isEqualTo("https://x.supabase.co");
        assertThat(config.supabase().serviceRoleKey()).isEqualTo("service-role");
        assertThat(config.security().issuer()).isEqualTo("https://x.supabase.co");
        assertThat(config.security().audience()).isEqualTo("authenticated");
        assertThat(config.security().jwksUrl()).isEqualTo("https://x.supabase.co/auth/v1/.well-known/jwks.json");
        assertThat(config.bootstrap().adminUsername()).isEqualTo("admin");
        assertThat(config.bootstrap().adminEmail()).isEqualTo("admin@keystone.com");
        assertThat(config.bootstrap().adminPassword()).isEqualTo("changeit");
    }

    @Test
    void should_override_bootstrap_with_environment_variables() {
        AdminConfig config = AdminConfigLoader.load(Map.of(
                "SUPABASE_URL", "https://x.supabase.co",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role",
                "OIDC_ISSUER", "https://x.supabase.co",
                "OIDC_JWKS_URL", "https://x.supabase.co/auth/v1/.well-known/jwks.json",
                "BOOTSTRAP_ADMIN_EMAIL", "boss@keystone.com",
                "BOOTSTRAP_ADMIN_PASSWORD", "s3cret"));

        assertThat(config.bootstrap().adminUsername()).isEqualTo("admin");
        assertThat(config.bootstrap().adminEmail()).isEqualTo("boss@keystone.com");
        assertThat(config.bootstrap().adminPassword()).isEqualTo("s3cret");
    }

    @Test
    void should_fail_fast_when_supabase_is_missing() {
        assertThatThrownBy(() -> AdminConfigLoader.load(Map.of(
                "OIDC_ISSUER", "https://x.supabase.co",
                "OIDC_JWKS_URL", "https://x.supabase.co/auth/v1/.well-known/jwks.json")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supabase.url");
    }
}
