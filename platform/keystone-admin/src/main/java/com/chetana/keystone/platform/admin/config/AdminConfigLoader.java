package com.chetana.keystone.platform.admin.config;

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
 *   <li>Secret environment variable (e.g. {@code SUPABASE_SERVICE_ROLE_KEY})</li>
 *   <li>{@code admin-config/application-{env}.yaml} (selected by {@code APP_ENV}, default {@code dev})</li>
 *   <li>{@code admin-config/application.yaml} (base defaults)</li>
 * </ol>
 *
 * <p>Only the Supabase service-role key and the bootstrap admin password are secrets read from
 * environment variables (Cloud Run → Secret Manager); all non-secret values resolve from yaml.
 *
 * <p>The platform database connection is not resolved here — it is inherited from the hosting
 * application (same database, different schema), which passes a {@code DatabaseConfig} directly
 * into {@code PlatformDataModule}.
 */
public final class AdminConfigLoader {

    private static final String APP_ENV_KEY = "APP_ENV";
    private static final String DEFAULT_ENV = "dev";
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
                supabase(env, overlay, base),
                security(overlay, base),
                bootstrap(env, overlay, base));
    }

    private static AdminConfig.Supabase supabase(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AdminConfig.Supabase(
                fileString(overlay, base, "", "supabase", "url"),
                secret(env, overlay, base, "SUPABASE_SERVICE_ROLE_KEY", "", "supabase", "serviceRoleKey"));
    }

    private static AdminConfig.Security security(JsonNode overlay, JsonNode base) {
        return new AdminConfig.Security(
                fileString(overlay, base, "", "security", "issuer"),
                fileString(overlay, base, "authenticated", "security", "audience"),
                fileString(overlay, base, "", "security", "jwksUrl"));
    }

    private static AdminConfig.Bootstrap bootstrap(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AdminConfig.Bootstrap(
                fileString(overlay, base, "admin", "bootstrap", "adminUsername"),
                fileString(overlay, base, "admin@keystone.com", "bootstrap", "adminEmail"),
                secret(env, overlay, base, "BOOTSTRAP_ADMIN_PASSWORD", "changeit", "bootstrap", "adminPassword"));
    }

    /** Secret values: environment variable wins, then yaml, then fallback. */
    private static String secret(Map<String, String> env, JsonNode overlay, JsonNode base,
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

    /** Non-secret values: yaml only (no environment override). */
    private static String fileString(JsonNode overlay, JsonNode base, String fallback, String... path) {
        String fromFile = fileValue(overlay, base, path);
        return fromFile == null || fromFile.isBlank() ? fallback : fromFile;
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
