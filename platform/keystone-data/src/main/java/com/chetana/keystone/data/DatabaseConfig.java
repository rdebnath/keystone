package com.chetana.keystone.data;

/**
 * Connection settings for a single application schema within a shared database.
 */
public record DatabaseConfig(String url, String username, String password, int maxPoolSize, String schema) {

    public DatabaseConfig {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("database url must not be blank");
        }
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("maxPoolSize must be positive");
        }
    }

    public static DatabaseConfig of(String url, String username, String password, int maxPoolSize, String schema) {
        return new DatabaseConfig(url, username == null ? "" : username, password == null ? "" : password,
                maxPoolSize, schema == null ? "" : schema);
    }
}
