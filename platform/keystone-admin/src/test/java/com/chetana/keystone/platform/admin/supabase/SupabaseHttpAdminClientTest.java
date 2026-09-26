package com.chetana.keystone.platform.admin.supabase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseHttpAdminClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_find_user_sub_by_email_case_insensitively() throws Exception {
        JsonNode page = objectMapper.readTree("""
                {"users":[{"id":"abc-123","email":"admin@keystone.com"}],"aud":"authenticated","next_page":null}
                """);

        assertThat(SupabaseHttpAdminClient.findSubInPage(page, "ADMIN@keystone.com")).isEqualTo("abc-123");
    }

    @Test
    void should_return_null_when_no_user_matches() throws Exception {
        JsonNode page = objectMapper.readTree("""
                {"users":[{"id":"abc-123","email":"someone@example.com"}],"aud":"authenticated","next_page":null}
                """);

        assertThat(SupabaseHttpAdminClient.findSubInPage(page, "admin@keystone.com")).isNull();
    }
}
