package com.chetana.keystone.platform.admin.supabase;

import com.chetana.keystone.common.error.ConflictException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseAdminClientTest {

    @Test
    void should_return_new_sub_when_user_is_created() {
        SupabaseAdminClient client = fake("new-sub", Optional.empty());

        assertThat(client.createOrAdoptUser("a@b.com", "pw")).isEqualTo("new-sub");
    }

    @Test
    void should_adopt_existing_sub_when_creation_conflicts() {
        SupabaseAdminClient client = fake(null, Optional.of("existing-sub"));

        assertThat(client.createOrAdoptUser("a@b.com", "pw")).isEqualTo("existing-sub");
    }

    /**
     * A test double where {@code createdSub == null} makes {@code createUser} throw a
     * {@link ConflictException} (simulating a 409 from Supabase Auth).
     */
    private static SupabaseAdminClient fake(String createdSub, Optional<String> existingSub) {
        return new SupabaseAdminClient() {
            @Override
            public String createUser(String email, String password) {
                if (createdSub == null) {
                    throw new ConflictException("Supabase user already exists: " + email);
                }
                return createdSub;
            }

            @Override
            public Optional<String> findSubByEmail(String email) {
                return existingSub;
            }

            @Override
            public Session login(String email, String password) {
                return null;
            }

            @Override
            public void updatePassword(String sub, String password) {
            }
        };
    }
}
