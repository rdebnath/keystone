package com.chetana.keystone.platform.identity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.time.DateTimeService;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.chetana.keystone.platform.jooq.Tables.USERS;

/**
 * Returns the authenticated caller's profile and effective permissions, and marks their
 * first-login password change complete.
 */
@Singleton
public final class MeService {

    private final DSLContext dsl;
    private final PermissionResolver permissionResolver;
    private final DateTimeService dateTimeService;

    @Inject
    public MeService(DSLContext dsl, PermissionResolver permissionResolver, DateTimeService dateTimeService) {
        this.dsl = dsl;
        this.permissionResolver = permissionResolver;
        this.dateTimeService = dateTimeService;
    }

    public MeDto me(String sub) {
        var user = dsl.selectFrom(USERS).where(USERS.SUB.eq(sub)).fetchOne();
        if (user == null) {
            throw new NotFoundException("User not found: " + sub);
        }
        List<String> permissions = permissionResolver.resolve(sub, null).stream().sorted().toList();
        return new MeDto(user.getSub(), user.getTenantId(), user.getMustChangePassword(), permissions);
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
