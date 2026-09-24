package com.chetana.keystone.platform.admin.identity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.chetana.keystone.platform.admin.jooq.Tables.USERS;

/**
 * Returns the authenticated caller's profile and effective permissions, and handles their
 * first-login password change.
 */
@Singleton
public final class MeService {

    private final DSLContext dsl;
    private final PermissionResolver permissionResolver;
    private final DateTimeService dateTimeService;
    private final SupabaseAdminClient supabaseAdmin;

    @Inject
    public MeService(@Platform DSLContext dsl, PermissionResolver permissionResolver, DateTimeService dateTimeService, SupabaseAdminClient supabaseAdmin) {
        this.dsl = dsl;
        this.permissionResolver = permissionResolver;
        this.dateTimeService = dateTimeService;
        this.supabaseAdmin = supabaseAdmin;
    }

    public MeDto me(String sub) {
        var user = dsl.selectFrom(USERS).where(USERS.SUB.eq(sub)).fetchOne();
        if (user == null) {
            throw new NotFoundException("User not found: " + sub);
        }
        List<String> permissions = permissionResolver.resolve(sub, user.getTenantId()).stream().sorted().toList();
        return new MeDto(user.getSub(), user.getTenantId(), user.getMustChangePassword(), permissions);
    }

    public void changePassword(String sub, String password) {
        supabaseAdmin.updatePassword(sub, password);
        markPasswordChanged(sub);
    }

    public void markPasswordChanged(String sub) {
        OffsetDateTime now = dateTimeService.now().atOffset(ZoneOffset.UTC);
        int updated = dsl.update(USERS)
                .set(USERS.MUST_CHANGE_PASSWORD, false)
                .set(USERS.UPDATED_AT, now)
                .where(USERS.SUB.eq(sub))
                .execute();
        if (updated == 0) {
            throw new NotFoundException("User not found: " + sub);
        }
    }
}
