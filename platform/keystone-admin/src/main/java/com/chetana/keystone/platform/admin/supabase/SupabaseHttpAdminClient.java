package com.chetana.keystone.platform.admin.supabase;

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
import java.util.Optional;

/**
 * Supabase Auth Admin API client (GoTrue) using the service-role key. The service-role key must
 * never leave the backend.
 *
 * <p>Both directions are typed: request bodies are the wire records in this package
 * ({@link CreateUserRequest}, {@link PasswordGrantRequest}, {@link UpdatePasswordRequest}) and
 * responses are parsed straight into their record ({@link GoTrueUser}, {@link ListUsersPage},
 * {@link TokenResponse}). A {@code JsonNode} never leaves this class (see
 * docs/CODING_GUIDELINES_BACKEND.md §13).
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
    public String createUser(String email, String password) {
        try {
            String body = objectMapper.writeValueAsString(new CreateUserRequest(email, password, true));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + ADMIN_USERS_PATH))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 409) {
                throw new ConflictException("Supabase user already exists: " + email);
            }
            if (status >= 300) {
                throw new IllegalStateException("Supabase admin create returned HTTP " + status);
            }
            GoTrueUser created = objectMapper.readValue(response.body(), GoTrueUser.class);
            if (created.id() == null || created.id().isBlank()) {
                throw new IllegalStateException("Supabase admin create returned no user id for " + email);
            }
            return created.id();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Supabase admin request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Supabase admin request failed", e);
        }
    }

    @Override
    public Optional<String> findSubByEmail(String email) {
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
            return findSub(objectMapper.readValue(response.body(), ListUsersPage.class), email);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Supabase admin request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Supabase admin request failed", e);
        }
    }

    /**
     * The Supabase user {@code id} whose email matches (case-insensitively) in a parsed GoTrue
     * "list users" page, or empty when no user matches.
     */
    static Optional<String> findSub(ListUsersPage page, String email) {
        return page.users().stream()
                .filter(user -> email.equalsIgnoreCase(user.email()))
                .map(GoTrueUser::id)
                .findFirst();
    }

    @Override
    public Session login(String email, String password) {
        try {
            String body = objectMapper.writeValueAsString(new PasswordGrantRequest(email, password));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + TOKEN_PATH))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new AccessDeniedException("Invalid username, tenant, or password.");
            }
            TokenResponse token = objectMapper.readValue(response.body(), TokenResponse.class);
            if (token.accessToken().isBlank()) {
                throw new IllegalStateException("Supabase token response carried no access token");
            }
            return new Session(
                    token.accessToken(),
                    token.refreshToken(),
                    token.tokenType(),
                    token.expiresIn());
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
            String body = objectMapper.writeValueAsString(new UpdatePasswordRequest(password));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + ADMIN_USERS_PATH + "/" + sub))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
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
