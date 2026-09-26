package com.chetana.keystone.platform.admin.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.common.error.KeystoneException;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.supabase.Session;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;

import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.Tables.USERS;

/**
 * Backend-proxied login: parses {@code username@tenantid}, resolves the tenant and user, then
 * authenticates against Supabase (password grant) on the user's behalf. Every failure — unknown
 * tenant, unknown user, or bad password — surfaces as the same {@code AccessDeniedException} so
 * the endpoint cannot be used to enumerate accounts.
 */
@Singleton
public final class LoginService {

    private final DataAccess data;
    private final TenantResolver tenantResolver;
    private final SupabaseAdminClient supabaseAdmin;

    @Inject
    public LoginService(@Platform DataAccess data, TenantResolver tenantResolver, SupabaseAdminClient supabaseAdmin) {
        this.data = data;
        this.tenantResolver = tenantResolver;
        this.supabaseAdmin = supabaseAdmin;
    }

    public Session login(LoginRequest request) {
        try {
            LoginIdentifier credentials = LoginIdentifier.parse(request.identifier());
            UUID tenantId = tenantResolver.resolveBySlug(credentials.tenantSlug());
            String email = resolveEmail(credentials.username(), tenantId);
            return supabaseAdmin.login(email, request.password());
        } catch (KeystoneException e) {
            throw new AccessDeniedException("Invalid username, tenant, or password.");
        }
    }

    private String resolveEmail(String username, UUID tenantId) {
        var user = data.read().select(USERS.EMAIL)
                .from(USERS)
                .where(USERS.USERNAME.eq(username))
                .and(tenantId == null ? USERS.TENANT_ID.isNull() : USERS.TENANT_ID.eq(tenantId))
                .fetchOne();
        if (user == null) {
            throw new AccessDeniedException("Invalid username, tenant, or password.");
        }
        return user.value1();
    }
}
