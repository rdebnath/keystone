package com.chetana.keystone.platform.supabase;

/**
 * Port for provisioning identities in Supabase Auth using the service-role key (server-only). The
 * backend never stores or checks passwords — it only obtains the Auth {@code sub}.
 */
@FunctionalInterface
public interface SupabaseAdminClient {

    /**
     * Returns the Auth {@code sub} for the given email, creating the user with the provided
     * temporary password if it does not yet exist. Idempotent.
     */
    String ensureUser(String email, String password);
}
