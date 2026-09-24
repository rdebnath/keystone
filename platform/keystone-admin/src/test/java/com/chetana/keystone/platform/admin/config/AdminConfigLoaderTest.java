package com.chetana.keystone.platform.admin.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminConfigLoaderTest {

    @Test
    void should_load_local_defaults_with_required_env() {
        AdminConfig config = AdminConfigLoader.load(Map.of(
                "SUPABASE_URL", "https://x.supabase.co",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role",
                "OIDC_ISSUER", "https://x.supabase.co",
                "OIDC_JWKS_URL", "https://x.supabase.co/auth/v1/.well-known/jwks.json"));

        assertThat(config.environment()).isEqualTo("local");
        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://localhost:5432/keystone");
        assertThat(config.database().username()).isEqualTo("keystone");
        assertThat(config.database().password()).isEqualTo("keystone");
        assertThat(config.database().schema()).isEqualTo("platform");
        assertThat(config.security().audience()).isEqualTo("authenticated");
        assertThat(config.bootstrap().adminUsername()).isEqualTo("admin");
        assertThat(config.bootstrap().adminEmail()).isEqualTo("admin@keystone.com");
        assertThat(config.bootstrap().adminPassword()).isEqualTo("changeit");
    }

    @Test
    void should_override_file_values_with_environment_variables() {
        AdminConfig config = AdminConfigLoader.load(Map.of(
                "SUPABASE_URL", "https://x.supabase.co",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role",
                "OIDC_ISSUER", "https://x.supabase.co",
                "OIDC_JWKS_URL", "https://x.supabase.co/auth/v1/.well-known/jwks.json",
                "DB_URL", "jdbc:postgresql://prod.internal:5432/postgres",
                "DB_PASSWORD", "s3cret",
                "BOOTSTRAP_ADMIN_EMAIL", "boss@keystone.com"));

        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://prod.internal:5432/postgres");
        assertThat(config.database().password()).isEqualTo("s3cret");
        assertThat(config.database().username()).isEqualTo("keystone");
        assertThat(config.database().schema()).isEqualTo("platform");
        assertThat(config.bootstrap().adminEmail()).isEqualTo("boss@keystone.com");
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
