package com.chetana.keystone.inventory.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigLoaderTest {

    @Test
    void should_use_dev_defaults_when_no_environment_is_set() {
        AppConfig config = ConfigLoader.load(Map.of("DB_PASSWORD", "dev-secret"));

        assertThat(config.environment()).isEqualTo("dev");
        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:6543/postgres");
        assertThat(config.database().username()).isEqualTo("postgres.gxswbqcbxorfiinfdqke");
        assertThat(config.database().password()).isEqualTo("dev-secret");
        assertThat(config.database().maxPoolSize()).isEqualTo(10);
        assertThat(config.database().schema()).isEqualTo("inventory");
        assertThat(config.platform().schema()).isEqualTo("platform");
        // read target resolves from yaml; blank falls back to the primary (read/write on one instance).
        assertThat(config.database().read().url()).isEqualTo("jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:6543/postgres");
        assertThat(config.database().read().username()).isEqualTo("postgres.gxswbqcbxorfiinfdqke");
        assertThat(config.database().read().password()).isEqualTo("dev-secret");
        assertThat(config.database().read().maxPoolSize()).isEqualTo(10);
        assertThat(config.server().port()).isEqualTo(8080);
        assertThat(config.server().contextPath()).isEqualTo("/inventory");
        assertThat(config.cors().allowedOrigins()).containsExactly("*");
    }

    @Test
    void should_override_only_secret_values_with_environment_variables() {
        AppConfig config = ConfigLoader.load(Map.of(
                "DB_URL", "jdbc:postgresql://prod.internal:5432/postgres",
                "DB_PASSWORD", "s3cret",
                "PORT", "9090"));

        // Secret overridden by the environment.
        assertThat(config.database().password()).isEqualTo("s3cret");
        // Read target's password falls back to the (secret) primary password.
        assertThat(config.database().read().password()).isEqualTo("s3cret");
        // Non-secret values ignore the environment and come from the yaml file.
        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:6543/postgres");
        assertThat(config.server().port()).isEqualTo(8080);
        assertThat(config.database().username()).isEqualTo("postgres.gxswbqcbxorfiinfdqke");
        assertThat(config.database().schema()).isEqualTo("inventory");
        assertThat(config.database().read().url()).isEqualTo("jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:6543/postgres");
        assertThat(config.database().read().username()).isEqualTo("postgres.gxswbqcbxorfiinfdqke");
    }

    @Test
    void should_select_the_environment_specific_file() {
        AppConfig config = ConfigLoader.load(Map.of(
                "APP_ENV", "dev",
                "DB_PASSWORD", "dev-secret"));

        assertThat(config.environment()).isEqualTo("dev");
        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:6543/postgres");
        assertThat(config.database().username()).isEqualTo("postgres.gxswbqcbxorfiinfdqke");
        assertThat(config.database().password()).isEqualTo("dev-secret");
        assertThat(config.database().schema()).isEqualTo("inventory");
    }

    @Test
    void should_select_the_demo_environment_file() {
        AppConfig config = ConfigLoader.load(Map.of(
                "APP_ENV", "demo",
                "DB_PASSWORD", "demo-secret"));

        assertThat(config.environment()).isEqualTo("demo");
        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:6543/postgres");
        assertThat(config.database().username()).isEqualTo("postgres.gxswbqcbxorfiinfdqke");
        assertThat(config.database().password()).isEqualTo("demo-secret");
        assertThat(config.database().schema()).isEqualTo("inventory");
        assertThat(config.cors().allowedOrigins()).isEmpty();
    }

    @Test
    void should_ignore_non_secret_environment_variable_overrides() {
        AppConfig config = ConfigLoader.load(Map.of(
                "APP_ENV", "dev",
                "DB_PASSWORD", "dev-secret",
                "PLATFORM_SCHEMA", "custom-platform"));

        assertThat(config.platform().schema()).isEqualTo("platform");
    }

    @Test
    void should_override_only_realtime_secret_with_environment_variable() {
        AppConfig config = ConfigLoader.load(Map.of(
                "DB_PASSWORD", "dev-secret",
                "REALTIME_ENDPOINT", "wss://realtime.example.com",
                "REALTIME_SERVICE_ROLE_KEY", "role-key"));

        assertThat(config.realtime().serviceRoleKey()).isEqualTo("role-key");
        assertThat(config.realtime().endpoint()).isBlank();
    }

    @Test
    void should_fail_fast_when_required_config_is_missing() {
        // No environment commits a password; without DB_PASSWORD the config must be rejected.
        assertThatThrownBy(() -> ConfigLoader.load(Map.of("APP_ENV", "demo")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("database.password");
    }
}
