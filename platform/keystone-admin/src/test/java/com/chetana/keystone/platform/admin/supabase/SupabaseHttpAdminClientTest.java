package com.chetana.keystone.platform.admin.supabase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parsing of the GoTrue "list users" page that provisioning uses to adopt an existing Auth user.
 * The body is parsed into its record rather than walked as a {@code JsonNode}
 * (docs/CODING_GUIDELINES_BACKEND.md §13) — including the unknown {@code aud}/{@code next_page}
 * fields that must be tolerated.
 */
class SupabaseHttpAdminClientTest {

    private static final String PAGE = """
            {"users":[{"id":"abc-123","email":"admin@keystone.com","aud":"authenticated"}],\
            "aud":"authenticated","next_page":null}
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
}

