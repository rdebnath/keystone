package com.chetana.keystone.platform.admin.supabase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.platform.admin.config.AdminConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Supabase Auth Admin API client (GoTrue) using the service-role key. The service-role key must
 * never leave the backend.
 */
@Singleton
public final class SupabaseHttpAdminClient implements SupabaseAdminClient {

    private static final String ADMIN_USERS_PATH = "/auth/v1/admin/users";
    private static final String TOKEN_PATH = "/auth/v1/token?grant_type=password";

    private final String baseUrl;
    private final String serviceRoleKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Inject
    public SupabaseHttpAdminClient(AdminConfig.Supabase supabase, ObjectMapper objectMapper) {
        this.baseUrl = stripTrailingSlash(supabase.url());
        this.serviceRoleKey = supabase.serviceRoleKey();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String ensureUser(String email, String password) {
        String existing = findUserByEmail(email);
        if (existing != null) {
            return existing;
        }
        return createUser(email, password);
    }

    private String findUserByEmail(String email) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + ADMIN_USERS_PATH + "?page=1&per_page=100"))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Supabase admin list returned HTTP " + response.statusCode());
            }
            JsonNode users = objectMapper.readTree(response.body());
            for (JsonNode user : users) {
                if (email.equalsIgnoreCase(user.path("email").asText())) {
                    return user.path("id").asText();
                }
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Supabase admin request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Supabase admin request failed", e);
        }
    }

    private String createUser(String email, String password) {
        try {
            Map<String, Object> body = Map.of(
                    "email", email,
                    "password", password,
                    "email_confirm", true);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + ADMIN_USERS_PATH))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new ConflictException("Supabase user already exists or could not be created: " + email);
            }
            return objectMapper.readTree(response.body()).path("id").asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Supabase admin request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Supabase admin request failed", e);
        }
    }
    @Override
    public Session login(String email, String password) {
        try {
            Map<String, Object> body = Map.of("email", email, "password", password);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + TOKEN_PATH))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new AccessDeniedException("Invalid username, tenant, or password.");
            }
            JsonNode json = objectMapper.readTree(response.body());
            return new Session(
                    json.path("access_token").asText(),
                    json.path("refresh_token").asText(),
                    json.path("token_type").asText(),
                    json.path("expires_in").asLong());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Supabase auth request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Supabase auth request failed", e);
        }
    }

    @Override
    public void updatePassword(String sub, String password) {
        try {
            Map<String, Object> body = Map.of("password", password);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + ADMIN_USERS_PATH + "/" + sub))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Supabase admin update returned HTTP " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Supabase admin request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Supabase admin request failed", e);
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
