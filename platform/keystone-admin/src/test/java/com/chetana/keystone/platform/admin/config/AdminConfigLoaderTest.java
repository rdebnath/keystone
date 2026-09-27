package com.chetana.keystone.platform.admin.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminConfigLoaderTest {

    @Test
    void should_resolve_non_secrets_from_yaml_and_secret_from_env() {
        AdminConfig config = AdminConfigLoader.load(Map.of(
                "APP_ENV", "test",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role"));

        assertThat(config.environment()).isEqualTo("test");
        assertThat(config.supabase().url()).isEqualTo("https://test.supabase.co");
        assertThat(config.supabase().serviceRoleKey()).isEqualTo("service-role");
        assertThat(config.security().issuer()).isEqualTo("https://test.supabase.co/auth/v1");
        assertThat(config.security().audience()).isEqualTo("authenticated");
        assertThat(config.security().jwksUrl()).isEqualTo("https://test.supabase.co/auth/v1/.well-known/jwks.json");
        assertThat(config.bootstrap().adminUsername()).isEqualTo("admin");
        assertThat(config.bootstrap().adminEmail()).isEqualTo("admin@keystone.com");
        assertThat(config.bootstrap().adminPassword()).isEqualTo("changeit");
    }

    @Test
    void should_override_only_bootstrap_password_secret_with_environment_variable() {
        AdminConfig config = AdminConfigLoader.load(Map.of(
                "APP_ENV", "test",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role",
                "BOOTSTRAP_ADMIN_EMAIL", "boss@keystone.com",
                "BOOTSTRAP_ADMIN_PASSWORD", "s3cret"));

        // Secret overridden by the environment.
        assertThat(config.bootstrap().adminPassword()).isEqualTo("s3cret");
        // Non-secret bootstrap values ignore the environment and come from yaml.
        assertThat(config.bootstrap().adminEmail()).isEqualTo("admin@keystone.com");
        assertThat(config.bootstrap().adminUsername()).isEqualTo("admin");
    }

    @Test
    void should_bootstrap_by_default() {
        AdminConfig config = AdminConfigLoader.load(Map.of(
                "APP_ENV", "test",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role"));

        assertThat(config.bootstrap().enabled()).isTrue();
    }

    @Test
    void should_disable_the_bootstrap_from_the_environment() {
        AdminConfig config = AdminConfigLoader.load(Map.of(
                "APP_ENV", "test",
                "SUPABASE_SERVICE_ROLE_KEY", "service-role",
                "BOOTSTRAP_ON_START", "false"));

        assertThat(config.bootstrap().enabled()).isFalse();
    }

    @Test
    void should_fail_fast_when_non_secret_supabase_url_is_missing() {
        // No application-prod.yaml: the base yaml leaves supabase.url blank -> rejected.
        assertThatThrownBy(() -> AdminConfigLoader.load(Map.of("APP_ENV", "prod")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supabase.url");
    }

    @Test
    void should_fail_fast_when_service_role_key_secret_is_missing() {
        assertThatThrownBy(() -> AdminConfigLoader.load(Map.of("APP_ENV", "test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supabase.serviceRoleKey");
    }
}
