package com.chetana.keystone.platform.admin.supabase;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.platform.admin.config.AdminConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parsing of the GoTrue "list users" page that provisioning uses to adopt an existing Auth user, and of
 * the GoTrue failure body that a rejected password grant logs. The bodies are parsed into their records
 * rather than walked as a {@code JsonNode} (docs/CODING_GUIDELINES_BACKEND.md §13) — including the
 * unknown {@code aud}/{@code next_page} fields that must be tolerated.
 */
class SupabaseHttpAdminClientTest {

    private static final String PAGE = """
            {"users":[{"id":"abc-123","email":"admin@keystone.com","aud":"authenticated"}],\
            "aud":"authenticated","next_page":null}
            """;

    /** A rejection as GoTrue actually writes it (observed on the dev project, 2026-09-27). */
    private static final String FAILURE = """
            {"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_find_user_sub_by_email_case_insensitively() throws Exception {
        ListUsersPage page = objectMapper.readValue(PAGE, ListUsersPage.class);

        assertThat(SupabaseHttpAdminClient.findSub(page, "ADMIN@keystone.com")).contains("abc-123");
    }

    @Test
    void should_return_empty_when_no_user_matches() throws Exception {
        ListUsersPage page = objectMapper.readValue(PAGE, ListUsersPage.class);

        assertThat(SupabaseHttpAdminClient.findSub(page, "someone@example.com")).isEmpty();
    }

    @Test
    void should_report_the_gotrue_error_code_for_a_rejected_grant() {
        SupabaseHttpAdminClient client = client("http://localhost:1");

        assertThat(client.failureCode(FAILURE)).isEqualTo("invalid_credentials");
    }

    @Test
    void should_fall_back_to_the_numeric_code_when_error_code_is_absent() {
        assertThat(client("http://localhost:1").failureCode("{\"code\":429}")).isEqualTo("429");
    }

    @Test
    void should_report_a_non_json_failure_body_as_unparsable() {
        assertThat(client("http://localhost:1").failureCode("<html>502 Bad Gateway</html>"))
                .isEqualTo("unparsable");
    }

    @Test
    void should_log_the_failure_code_without_the_credentials_and_still_answer_invalid_credentials() throws Exception {
        HttpServer gotrue = gotrue(400, FAILURE);
        ListAppender<ILoggingEvent> appender = attachLogAppender();
        try {
            SupabaseHttpAdminClient client = client(baseUrl(gotrue));

            assertThatThrownBy(() -> client.login("admin@keystone.com", "typo-password"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Invalid username, tenant, or password.");
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getFormattedMessage())
                    .contains("HTTP 400", "invalid_credentials")
                    .doesNotContain("admin@keystone.com", "typo-password", "service-key");
        } finally {
            logger().detachAppender(appender);
            gotrue.stop(0);
        }
    }

    @Test
    void should_return_the_session_when_gotrue_accepts_the_grant() throws Exception {
        HttpServer gotrue = gotrue(200, """
                {"access_token":"access-token","refresh_token":"refresh-token",\
                "token_type":"bearer","expires_in":3600}
                """);
        try {
            Session session = client(baseUrl(gotrue)).login("admin@keystone.com", "changeit");

            assertThat(session.accessToken()).isEqualTo("access-token");
            assertThat(session.refreshToken()).isEqualTo("refresh-token");
            assertThat(session.expiresIn()).isEqualTo(3600);
        } finally {
            gotrue.stop(0);
        }
    }

    /** The slice's real dependency: a socket that answers like GoTrue, with no Supabase project involved. */
    private static HttpServer gotrue(int status, String body) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/auth/v1/token", exchange -> {
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, payload.length);
            try (var out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.start();
        return server;
    }

    private static String baseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static SupabaseHttpAdminClient client(String baseUrl) {
        return new SupabaseHttpAdminClient(
                new AdminConfig.Supabase(baseUrl, "service-key"), new ObjectMapper());
    }

    private static ListAppender<ILoggingEvent> attachLogAppender() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger().addAppender(appender);
        return appender;
    }

    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(SupabaseHttpAdminClient.class);
    }
}

