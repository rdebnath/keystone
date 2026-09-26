package com.chetana.keystone.platform.admin.supabase;

import com.chetana.keystone.common.error.ConflictException;

import java.util.Optional;

/**
 * Port for provisioning identities and authenticating users in Supabase Auth using the service-role
 * key (server-only). The backend never stores or checks passwords itself — it only obtains the Auth
 * {@code sub} (on provisioning) or a {@link Session} (on login).
 */
public interface SupabaseAdminClient {

    /**
     * Creates a Supabase Auth user with the given email and temporary password, returning its Auth
     * {@code sub}. Throws {@link ConflictException} if the email already exists in Auth.
     */
    String createUser(String email, String password);

    /**
     * Returns the Auth {@code sub} for the given email, or empty if no such user exists.
     */
    Optional<String> findSubByEmail(String email);

    /**
     * Creates the Auth user, or — if the email already exists in Auth (e.g. a partially-completed
     * provisioning) — returns the existing user's {@code sub}. Idempotent.
     */
    default String createOrAdoptUser(String email, String password) {
        try {
            return createUser(email, password);
        } catch (ConflictException e) {
            return findSubByEmail(email)
                    .orElseThrow(() -> new ConflictException("Supabase user already exists: " + email));
        }
    }

    /**
     * Exchanges email + password for a Supabase Auth {@link Session} (password grant). Throws on
     * invalid credentials.
     */
    Session login(String email, String password);

    /**
     * Sets the user's password in Supabase Auth (used by the backend-proxied change-password flow).
     */
    void updatePassword(String sub, String password);
}
