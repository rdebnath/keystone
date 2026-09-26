package com.chetana.keystone.platform.admin.config;

import com.chetana.keystone.data.DatabaseConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Resolves the platform admin console's environment-specific configuration.
 *
 * <p>Resources are namespaced under {@code /admin-config/} so they do not collide with a hosting
 * application's own {@code /config/} resources. Resolution order (highest precedence wins):
 * <ol>
 *   <li>Environment variable (e.g. {@code DB_URL}, {@code SUPABASE_URL})</li>
 *   <li>{@code admin-config/application-{env}.yaml} (selected by {@code APP_ENV}, default {@code local})</li>
 *   <li>{@code admin-config/application.yaml} (base defaults)</li>
 * </ol>
 */
public final class AdminConfigLoader {

    private static final String APP_ENV_KEY = "APP_ENV";
    private static final String DEFAULT_ENV = "local";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private AdminConfigLoader() {
    }

    public static AdminConfig load() {
        return load(System.getenv());
    }

    static AdminConfig load(Map<String, String> env) {
        String environment = env.getOrDefault(APP_ENV_KEY, DEFAULT_ENV);
        JsonNode base = readRequired("/admin-config/application.yaml");
        JsonNode overlay = readOptional("/admin-config/application-" + environment + ".yaml");

        return new AdminConfig(
                environment,
                database(env, overlay, base),
                supabase(env, overlay, base),
                security(env, overlay, base),
                bootstrap(env, overlay, base));
    }

    /**
     * Loads only the platform database connection settings, without requiring the Supabase/OIDC/bootstrap
     * configuration. Intended for schema tooling that operates on the database alone (e.g. SchemaTool).
     */
    public static DatabaseConfig databaseConfig() {
        return databaseConfig(System.getenv());
    }

    static DatabaseConfig databaseConfig(Map<String, String> env) {
        String environment = env.getOrDefault(APP_ENV_KEY, DEFAULT_ENV);
        JsonNode base = readRequired("/admin-config/application.yaml");
        JsonNode overlay = readOptional("/admin-config/application-" + environment + ".yaml");
        AdminConfig.Database db = database(env, overlay, base);
        return DatabaseConfig.of(db.url(), db.username(), db.password(), db.maxPoolSize(), db.schema());
    }

    private static AdminConfig.Database database(Map<String, String> env, JsonNode overlay, JsonNode base) {
        String url = string(env, overlay, base, "DB_URL", "", "database", "url");
        String username = string(env, overlay, base, "DB_USERNAME", "", "database", "username");
        String password = string(env, overlay, base, "DB_PASSWORD", "", "database", "password");
        int maxPoolSize = intValue(env, overlay, base, "DB_MAX_POOL_SIZE", 10, "database", "maxPoolSize");
        String schema = string(env, overlay, base, "DB_SCHEMA", "platform", "database", "schema");
        return new AdminConfig.Database(url, username, password, maxPoolSize, schema,
                read(overlay, base, username, password, maxPoolSize));
    }

    /** Read target, resolved from yaml only (no env override); username/password/pool size fall back to the primary. */
    private static AdminConfig.Read read(JsonNode overlay, JsonNode base,
                                         String primaryUsername, String primaryPassword, int primaryMaxPoolSize) {
        String url = fileValue(overlay, base, "database", "read", "url");
        String username = fileValue(overlay, base, "database", "read", "username");
        String password = fileValue(overlay, base, "database", "read", "password");
        int maxPoolSize = fileInt(overlay, base, primaryMaxPoolSize, "database", "read", "maxPoolSize");
        return new AdminConfig.Read(
                url == null ? "" : url,
                username == null || username.isBlank() ? primaryUsername : username,
                password == null || password.isBlank() ? primaryPassword : password,
                maxPoolSize);
    }

    private static int fileInt(JsonNode overlay, JsonNode base, int fallback, String... path) {
        String value = fileValue(overlay, base, path);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }

    private static AdminConfig.Supabase supabase(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AdminConfig.Supabase(
                string(env, overlay, base, "SUPABASE_URL", "", "supabase", "url"),
                string(env, overlay, base, "SUPABASE_SERVICE_ROLE_KEY", "", "supabase", "serviceRoleKey"));
    }

    private static AdminConfig.Security security(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AdminConfig.Security(
                string(env, overlay, base, "OIDC_ISSUER", "", "security", "issuer"),
                string(env, overlay, base, "OIDC_AUDIENCE", "authenticated", "security", "audience"),
                string(env, overlay, base, "OIDC_JWKS_URL", "", "security", "jwksUrl"));
    }

    private static AdminConfig.Bootstrap bootstrap(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AdminConfig.Bootstrap(
                string(env, overlay, base, "BOOTSTRAP_ADMIN_USERNAME", "admin", "bootstrap", "adminUsername"),
                string(env, overlay, base, "BOOTSTRAP_ADMIN_EMAIL", "admin@keystone.com", "bootstrap", "adminEmail"),
                string(env, overlay, base, "BOOTSTRAP_ADMIN_PASSWORD", "changeit", "bootstrap", "adminPassword"));
    }

    private static String string(Map<String, String> env, JsonNode overlay, JsonNode base,
                                 String envKey, String fallback, String... path) {
        String fromEnv = env.get(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromFile = fileValue(overlay, base, path);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile;
        }
        return fallback;
    }

    private static int intValue(Map<String, String> env, JsonNode overlay, JsonNode base,
                                String envKey, int fallback, String... path) {
        String value = string(env, overlay, base, envKey, Integer.toString(fallback), path);
        return Integer.parseInt(value.trim());
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
        try (InputStream in = AdminConfigLoader.class.getResourceAsStream(path)) {
            return in == null ? null : YAML.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read config resource: " + path, e);
        }
    }
}
