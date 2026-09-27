package com.chetana.keystone.platform.admin.supabase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The GoTrue wire contracts, asserted on the records that replaced the ad-hoc maps
 * (docs/CODING_GUIDELINES_BACKEND.md §13): a renamed or missing JSON field would otherwise break
 * login/provisioning silently.
 */
class GoTrueWireRecordsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_serialize_create_user_request_as_snake_case() throws Exception {
        String json = objectMapper.writeValueAsString(new CreateUserRequest("a@b.com", "pw", true));

        assertThat(json).isEqualTo("{\"email\":\"a@b.com\",\"password\":\"pw\",\"email_confirm\":true}");
    }

    @Test
    void should_serialize_password_grant_request() throws Exception {
        String json = objectMapper.writeValueAsString(new PasswordGrantRequest("a@b.com", "pw"));

        assertThat(json).isEqualTo("{\"email\":\"a@b.com\",\"password\":\"pw\"}");
    }

    @Test
    void should_serialize_update_password_request() throws Exception {
        String json = objectMapper.writeValueAsString(new UpdatePasswordRequest("pw"));

        assertThat(json).isEqualTo("{\"password\":\"pw\"}");
    }

    @Test
    void should_parse_snake_case_token_response_and_ignore_unknown_fields() throws Exception {
        TokenResponse token = objectMapper.readValue("""
                {"access_token":"access","token_type":"bearer","expires_in":3600,"expires_at":1,\
                "refresh_token":"refresh","user":{"id":"u"}}
                """, TokenResponse.class);

        assertThat(token.accessToken()).isEqualTo("access");
        assertThat(token.refreshToken()).isEqualTo("refresh");
        assertThat(token.tokenType()).isEqualTo("bearer");
        assertThat(token.expiresIn()).isEqualTo(3600);
    }

    @Test
    void should_tolerate_a_list_users_page_without_a_users_array() throws Exception {
        ListUsersPage page = objectMapper.readValue(
                "{\"aud\":\"authenticated\",\"next_page\":null}", ListUsersPage.class);

        assertThat(page.users()).isEmpty();
    }
}
