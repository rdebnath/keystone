package com.chetana.keystone.platform.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigLoaderTest {

    @Test
    void should_load_local_database_defaults_with_required_env() {
        AppConfig config = ConfigLoader.load(Map.of(
                "SUPABASE_URL", "https://x.supabase.co",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role",
                "OIDC_ISSUER", "https://x.supabase.co",
                "OIDC_JWKS_URL", "https://x.supabase.co/auth/v1/.well-known/jwks.json"));

        assertThat(config.environment()).isEqualTo("local");
        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://localhost:5432/platform");
        assertThat(config.database().username()).isEqualTo("keystone");
        assertThat(config.database().password()).isEqualTo("keystone");
        assertThat(config.server().port()).isEqualTo(8080);
        assertThat(config.security().audience()).isEqualTo("authenticated");
        assertThat(config.bootstrap().adminEmail()).isEqualTo("admin@keystone.local");
        assertThat(config.bootstrap().adminPassword()).isEqualTo("changeit");
    }

    @Test
    void should_override_file_values_with_environment_variables() {
        AppConfig config = ConfigLoader.load(Map.of(
                "SUPABASE_URL", "https://x.supabase.co",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role",
                "OIDC_ISSUER", "https://x.supabase.co",
                "OIDC_JWKS_URL", "https://x.supabase.co/auth/v1/.well-known/jwks.json",
                "DB_URL", "jdbc:postgresql://prod.internal:5432/platform",
                "DB_PASSWORD", "s3cret",
                "PORT", "9090"));

        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://prod.internal:5432/platform");
        assertThat(config.database().password()).isEqualTo("s3cret");
        assertThat(config.server().port()).isEqualTo(9090);
        assertThat(config.database().username()).isEqualTo("keystone");
    }

    @Test
    void should_fail_fast_when_security_is_missing() {
        assertThatThrownBy(() -> ConfigLoader.load(Map.of(
                "SUPABASE_URL", "https://x.supabase.co",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("security.issuer");
    }

    @Test
    void should_fail_fast_when_supabase_is_missing() {
        assertThatThrownBy(() -> ConfigLoader.load(Map.of(
                "OIDC_ISSUER", "https://x.supabase.co",
                "OIDC_JWKS_URL", "https://x.supabase.co/auth/v1/.well-known/jwks.json")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supabase.url");
    }
}
