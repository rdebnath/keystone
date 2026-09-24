package com.chetana.keystone.platform.admin.supabase;

/**
 * Port for provisioning identities and authenticating users in Supabase Auth using the service-role
 * key (server-only). The backend never stores or checks passwords itself — it only obtains the Auth
 * {@code sub} (on provisioning) or a {@link Session} (on login).
 */
public interface SupabaseAdminClient {

    /**
     * Returns the Auth {@code sub} for the given email, creating the user with the provided
     * temporary password if it does not yet exist. Idempotent.
     */
    String ensureUser(String email, String password);

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
