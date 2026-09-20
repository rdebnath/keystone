package com.chetana.keystone.inventory.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Resolves the inventory application's environment-specific configuration.
 *
 * <p>Resolution order (highest precedence wins):
 * <ol>
 *   <li>Environment variable (e.g. {@code DB_URL}, {@code PORT})</li>
 *   <li>{@code config/application-{env}.yaml} (selected by {@code APP_ENV}, default {@code local})</li>
 *   <li>{@code config/application.yaml} (base defaults)</li>
 * </ol>
 *
 * <p>The same image ships to every environment; only {@code APP_ENV} and secrets differ. Secrets
 * (passwords, service-role keys, OIDC keys) are never read from files in a non-local environment —
 * they come from environment variables (Cloud Run → Secret Manager).
 */
public final class ConfigLoader {

    private static final String APP_ENV_KEY = "APP_ENV";
    private static final String DEFAULT_ENV = "local";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private ConfigLoader() {
    }

    /** Loads configuration from the real process environment. */
    public static AppConfig load() {
        return load(System.getenv());
    }

    /** Package-private for tests: resolves config from an explicit environment map. */
    static AppConfig load(Map<String, String> env) {
        String environment = env.getOrDefault(APP_ENV_KEY, DEFAULT_ENV);
        JsonNode base = readRequired("/config/application.yaml");
        JsonNode overlay = readOptional("/config/application-" + environment + ".yaml");

        return new AppConfig(
                environment,
                database(env, overlay, base),
                server(env, overlay, base),
                realtime(env, overlay, base),
                security(env, overlay, base));
    }

    private static AppConfig.Database database(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AppConfig.Database(
                string(env, overlay, base, "DB_URL", "database", "url"),
                string(env, overlay, base, "DB_USERNAME", "database", "username"),
                string(env, overlay, base, "DB_PASSWORD", "database", "password"),
                intValue(env, overlay, base, "DB_MAX_POOL_SIZE", 10, "database", "maxPoolSize"));
    }

    private static AppConfig.Server server(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AppConfig.Server(intValue(env, overlay, base, "PORT", 8080, "server", "port"));
    }

    private static AppConfig.Realtime realtime(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AppConfig.Realtime(
                string(env, overlay, base, "REALTIME_ENDPOINT", "realtime", "endpoint"),
                string(env, overlay, base, "REALTIME_SERVICE_ROLE_KEY", "realtime", "serviceRoleKey"));
    }

    private static AppConfig.Security security(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AppConfig.Security(
                string(env, overlay, base, "OIDC_ISSUER", "security", "issuer"),
                string(env, overlay, base, "OIDC_AUDIENCE", "security", "audience"));
    }

    private static String string(Map<String, String> env, JsonNode overlay, JsonNode base,
                                 String envKey, String... path) {
        String fromEnv = env.get(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromFile = fileValue(overlay, base, path);
        return fromFile == null ? "" : fromFile;
    }

    private static int intValue(Map<String, String> env, JsonNode overlay, JsonNode base,
                                String envKey, int fallback, String... path) {
        String value = string(env, overlay, base, envKey, path);
        return value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }

    private static String fileValue(JsonNode overlay, JsonNode base, String... path) {
        String value = pathValue(overlay, path);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return pathValue(base, path);
    }

    private static String pathValue(JsonNode root, String... path) {
        JsonNode node = root;
        for (String key : path) {
            if (node == null) {
                return null;
            }
            node = node.get(key);
        }
        return node == null || node.isNull() ? null : node.asText();
    }

    private static JsonNode readRequired(String path) {
        JsonNode node = readOptional(path);
        if (node == null) {
            throw new IllegalStateException("Missing required config resource: " + path);
        }
        return node;
    }

    private static JsonNode readOptional(String path) {
        try (InputStream in = ConfigLoader.class.getResourceAsStream(path)) {
            return in == null ? null : YAML.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read config resource: " + path, e);
        }
    }
}
