package com.chetana.keystone.platform.admin.config;

import com.chetana.keystone.data.DatabaseConfig;
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
        // read target resolves from yaml; blank falls back to the primary (read/write on one instance).
        assertThat(config.database().read().url()).isEqualTo("jdbc:postgresql://localhost:5432/keystone");
        assertThat(config.database().read().username()).isEqualTo("keystone");
        assertThat(config.database().read().password()).isEqualTo("keystone");
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
        // read target is yaml-only (not affected by the DB_URL env override).
        assertThat(config.database().read().url()).isEqualTo("jdbc:postgresql://localhost:5432/keystone");
        assertThat(config.database().read().password()).isEqualTo("s3cret");
        assertThat(config.database().read().username()).isEqualTo("keystone");
        assertThat(config.bootstrap().adminEmail()).isEqualTo("boss@keystone.com");
    }

    @Test
    void should_load_demo_environment_with_required_env() {
        AdminConfig config = AdminConfigLoader.load(Map.of(
                "APP_ENV", "demo",
                "DB_PASSWORD", "demo-secret",
                "SUPABASE_URL", "https://x.supabase.co",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role",
                "OIDC_ISSUER", "https://x.supabase.co",
                "OIDC_JWKS_URL", "https://x.supabase.co/auth/v1/.well-known/jwks.json"));

        assertThat(config.environment()).isEqualTo("demo");
        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:6543/keystone");
        assertThat(config.database().username()).isEqualTo("deployment@chetanatech.com");
        assertThat(config.database().password()).isEqualTo("demo-secret");
        assertThat(config.database().schema()).isEqualTo("platform");
    }

    @Test
    void should_load_dev_environment_with_required_env() {
        AdminConfig config = AdminConfigLoader.load(Map.of(
                "APP_ENV", "dev",
                "DB_PASSWORD", "dev-secret",
                "SUPABASE_URL", "https://x.supabase.co",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role",
                "OIDC_ISSUER", "https://x.supabase.co",
                "OIDC_JWKS_URL", "https://x.supabase.co/auth/v1/.well-known/jwks.json"));

        assertThat(config.environment()).isEqualTo("dev");
        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:6543/keystone");
        assertThat(config.database().username()).isEqualTo("deployment@chetanatech.com");
        assertThat(config.database().password()).isEqualTo("dev-secret");
        assertThat(config.database().schema()).isEqualTo("platform");
    }

    @Test
    void should_load_only_database_config_without_requiring_supabase_or_oidc() {
        DatabaseConfig db = AdminConfigLoader.databaseConfig(Map.of(
                "APP_ENV", "demo",
                "DB_PASSWORD", "demo-secret"));

        assertThat(db.url()).isEqualTo("jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:6543/keystone");
        assertThat(db.username()).isEqualTo("deployment@chetanatech.com");
        assertThat(db.password()).isEqualTo("demo-secret");
        assertThat(db.schema()).isEqualTo("platform");
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
