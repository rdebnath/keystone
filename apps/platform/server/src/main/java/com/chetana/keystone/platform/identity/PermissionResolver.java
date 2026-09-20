package com.chetana.keystone.platform.identity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.Condition;
import org.jooq.DSLContext;

import java.util.Set;
import java.util.UUID;

import static com.chetana.keystone.platform.jooq.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.jooq.Tables.ROLE_PERMISSIONS;
import static com.chetana.keystone.platform.jooq.Tables.USER_ROLES;
import static com.chetana.keystone.platform.jooq.Tables.USERS;

/**
 * Resolves a user's effective permission codes as {@code user_roles ⋈ role_permissions}, filtered
 * by tenant context (§9.3). The wildcard {@code *} is returned as a regular code; callers treat it
 * as "all permissions".
 */
@Singleton
public final class PermissionResolver {

    private final DSLContext dsl;

    @Inject
    public PermissionResolver(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Set<String> resolve(String sub, UUID tenantId) {
        Condition tenantFilter = tenantId == null
                ? USER_ROLES.TENANT_ID.isNull()
                : USER_ROLES.TENANT_ID.isNull().or(USER_ROLES.TENANT_ID.eq(tenantId));

        return dsl.select(PERMISSIONS.CODE)
                .from(USERS)
                .join(USER_ROLES).on(USER_ROLES.USER_ID.eq(USERS.ID))
                .join(ROLE_PERMISSIONS).on(ROLE_PERMISSIONS.ROLE_ID.eq(USER_ROLES.ROLE_ID))
                .join(PERMISSIONS).on(PERMISSIONS.ID.eq(ROLE_PERMISSIONS.PERMISSION_ID))
                .where(USERS.SUB.eq(sub))
                .and(tenantFilter)
                .fetchSet(PERMISSIONS.CODE);
    }
}
