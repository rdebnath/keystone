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
 * The one non-secret exception is the operational startup switch {@code bootstrap.enabled}, which
 * an environment variable ({@code BOOTSTRAP_ON_START}) may override so the same image can be
 * deployed with or without the first-user bootstrap.
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

    /**
     * Bootstrap policy and the first admin's identity.
     *
     * <p>The username and email are **non-secret, per-deployment values**: they resolve from yaml only
     * ({@code admin-config/application-{env}.yaml}, with {@code application.yaml} holding a blank
     * placeholder), and a missing value fails fast in {@link AdminConfig} instead of falling back to a
     * built-in default — so each deployment decides who its first admin is. The password is the one
     * secret here ({@code BOOTSTRAP_ADMIN_PASSWORD}, then yaml, then the yaml-provided default).
     */
    private static AdminConfig.Bootstrap bootstrap(Map<String, String> env, JsonNode overlay, JsonNode base) {
        return new AdminConfig.Bootstrap(
                flag(env, overlay, base, "BOOTSTRAP_ON_START", true, "bootstrap", "enabled"),
                fileString(overlay, base, "", "bootstrap", "adminUsername"),
                fileString(overlay, base, "", "bootstrap", "adminEmail"),
                secret(env, overlay, base, "BOOTSTRAP_ADMIN_PASSWORD", "", "bootstrap", "adminPassword"));
    }

    /**
     * Operational on/off switches: the environment variable wins, then yaml, then the code default —
     * so a deployment can flip a startup behavior (e.g. the first-user bootstrap) without editing the
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
        JsonNode node = pathNode(root, path);
        return node == null || node.isNull() ? null : node.asText();
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
