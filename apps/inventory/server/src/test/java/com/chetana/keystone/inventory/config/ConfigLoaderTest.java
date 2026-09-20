package com.chetana.keystone.inventory.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigLoaderTest {

    @Test
    void should_use_local_defaults_when_no_environment_is_set() {
        AppConfig config = ConfigLoader.load(Map.of());

        assertThat(config.environment()).isEqualTo("local");
        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://localhost:5432/inventory");
        assertThat(config.database().username()).isEqualTo("keystone");
        assertThat(config.database().password()).isEqualTo("keystone");
        assertThat(config.database().maxPoolSize()).isEqualTo(10);
        assertThat(config.server().port()).isEqualTo(8080);
    }

    @Test
    void should_override_file_values_with_environment_variables() {
        AppConfig config = ConfigLoader.load(Map.of(
                "DB_URL", "jdbc:postgresql://prod.internal:5432/inventory",
                "DB_PASSWORD", "s3cret",
                "PORT", "9090"));

        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://prod.internal:5432/inventory");
        assertThat(config.database().password()).isEqualTo("s3cret");
        assertThat(config.server().port()).isEqualTo(9090);
        // untouched values still come from the local file
        assertThat(config.database().username()).isEqualTo("keystone");
    }

    @Test
    void should_select_the_environment_specific_file() {
        AppConfig config = ConfigLoader.load(Map.of(
                "APP_ENV", "dev",
                "DB_PASSWORD", "dev-secret"));

        assertThat(config.environment()).isEqualTo("dev");
        assertThat(config.database().url()).isEqualTo("jdbc:postgresql://db.inventory-dev.internal:5432/inventory");
        assertThat(config.database().password()).isEqualTo("dev-secret");
    }

    @Test
    void should_fail_fast_when_required_config_is_missing() {
        // prod never commits a password; without DB_PASSWORD the config must be rejected.
        assertThatThrownBy(() -> ConfigLoader.load(Map.of("APP_ENV", "prod")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("database.password");
    }
}
