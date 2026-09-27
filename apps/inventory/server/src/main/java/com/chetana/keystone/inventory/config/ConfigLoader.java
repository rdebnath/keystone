package com.chetana.keystone.inventory.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves the inventory application's environment-specific configuration.
 *
 * <p>Resolution order (highest precedence wins):
 * <ol>
 *   <li>Secret environment variable (e.g. {@code DB_PASSWORD}, {@code REALTIME_SERVICE_ROLE_KEY})</li>
 *   <li>{@code config/application-{env}.yaml} (selected by {@code APP_ENV}, default {@code dev})</li>
 *   <li>{@code config/application.yaml} (base defaults)</li>
 * </ol>
 *
 * <p>Only secrets (database password, realtime service-role key) are read from environment
 * variables (Cloud Run → Secret Manager). Every non-secret value is resolved from the yaml files.
 * The one non-secret exception is the operational startup switch {@code startup.migrateOnStart},
 * which an environment variable ({@code MIGRATE_ON_START}) may override so the same image can be
 * deployed with or without automatic migrations. The same image ships to every environment; only
 * {@code APP_ENV}, secrets and that switch differ.
 */
public final class ConfigLoader {

    private static final String APP_ENV_KEY = "APP_ENV";
    private static final String DEFAULT_ENV = "dev";
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
                platform(overlay, base),
                startup(env, overlay, base),
                server(overlay, base),
                cors(overlay, base),
                realtime(env, overlay, base),
                security(overlay, base));
    }

    private static AppConfig.Database database(Map<String, String> env, JsonNode overlay, JsonNode base) {
        String url = fileString(overlay, base, "database", "url");
        String username = fileString(overlay, base, "database", "username");
        String password = secret(env, overlay, base, "DB_PASSWORD", "database", "password");
        int maxPoolSize = fileInt(overlay, base, 10, "database", "maxPoolSize");
        String schema = fileString(overlay, base, "database", "schema");
        return new AppConfig.Database(url, username, password, maxPoolSize, schema,
                read(overlay, base, username, password, maxPoolSize));
    }

    /** Read target, resolved from yaml only (no env override); username/password/pool size fall back to the primary. */
    private static AppConfig.Read read(JsonNode overlay, JsonNode base,
                                       String primaryUsername, String primaryPassword, int primaryMaxPoolSize) {
        String url = fileValue(overlay, base, "database", "read", "url");
        String username = fileValue(overlay, base, "database", "read", "username");
        String password = fileValue(overlay, base, "database", "read", "password");
        int maxPoolSize = fileInt(overlay, base, primaryMaxPoolSize, "database", "read", "maxPoolSize");
        return new AppConfig.Read(
                url == null ? "" : url,
                username == null || username.isBlank() ? primaryUsername : username,
                password == null || password.isBlank() ? primaryPassword : password,
                maxPoolSize);
    }

    private static int fileInt(JsonNode overlay, JsonNode base, int fallback, String... path) {
        String value = fileValue(overlay, base, path);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }

    private static AppConfig.Platform platform(JsonNode overlay, JsonNode base) {
        return new AppConfig.Platform(fileString(overlay, base, "platform", "schema"));
    }

    private static AppConfig.Startup startup(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AppConfig.Startup(
                flag(env, overlay, base, "MIGRATE_ON_START", true, "startup", "migrateOnStart"));
    }

    /**
     * Operational on/off switches: the environment variable wins, then yaml, then the code default —
     * so a deployment can flip a startup behavior (e.g. automatic Liquibase) without editing the
     * environment file. Only values meant to be flipped per deployment belong here.
     */
    private static boolean flag(Map<String, String> env, JsonNode overlay, JsonNode base,
                                String envKey, boolean fallback, String... path) {
        String fromEnv = env.get(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return parseFlag(envKey, fromEnv);
        }
        JsonNode node = pathNode(overlay, path);
        if (node == null || node.isNull()) {
            node = pathNode(base, path);
        }
        return node == null || node.isNull() ? fallback : node.asBoolean(fallback);
    }

    private static boolean parseFlag(String envKey, String value) {
        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return false;
        }
        throw new IllegalArgumentException(envKey + " must be true or false: " + value);
    }

    private static AppConfig.Server server(JsonNode overlay, JsonNode base) {
        return new AppConfig.Server(
                fileInt(overlay, base, 8080, "server", "port"),
                fileString(overlay, base, "server", "contextPath"));
    }

    private static AppConfig.Cors cors(JsonNode overlay, JsonNode base) {
        return new AppConfig.Cors(fileList(overlay, base, "cors", "allowedOrigins"));
    }

    private static AppConfig.Realtime realtime(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AppConfig.Realtime(
                fileString(overlay, base, "realtime", "endpoint"),
                secret(env, overlay, base, "REALTIME_SERVICE_ROLE_KEY", "realtime", "serviceRoleKey"));
    }

    private static AppConfig.Security security(JsonNode overlay, JsonNode base) {
        return new AppConfig.Security(
                fileString(overlay, base, "security", "issuer"),
                fileString(overlay, base, "security", "audience"));
    }

    /** Secret values: environment variable wins, then yaml, then blank. */
    private static String secret(Map<String, String> env, JsonNode overlay, JsonNode base,
                                 String envKey, String... path) {
        String fromEnv = env.get(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromFile = fileValue(overlay, base, path);
        return fromFile == null ? "" : fromFile;
    }

    /** Non-secret values: yaml only (no environment override). */
    private static String fileString(JsonNode overlay, JsonNode base, String... path) {
        String fromFile = fileValue(overlay, base, path);
        return fromFile == null ? "" : fromFile;
    }

    private static String fileValue(JsonNode overlay, JsonNode base, String... path) {
        String value = pathValue(overlay, path);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return pathValue(base, path);
    }

    private static JsonNode pathNode(JsonNode root, String... path) {
        JsonNode node = root;
        for (String key : path) {
            if (node == null) {
                return null;
            }
            node = node.get(key);
        }
        return node;
    }

    private static String pathValue(JsonNode root, String... path) {
        JsonNode node = pathNode(root, path);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static List<String> fileList(JsonNode overlay, JsonNode base, String... path) {
        JsonNode node = pathNode(overlay, path);
        if (node == null || !node.isArray()) {
            node = pathNode(base, path);
        }
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode element : node) {
            values.add(element.asText());
        }
        return List.copyOf(values);
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
