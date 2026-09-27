package com.chetana.keystone.platform.admin.identity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USERS;

/**
 * Returns the authenticated caller's profile and effective permissions, and handles their
 * first-login password change.
 */
@Singleton
public final class MeService {

    private final DataAccess data;
    private final PermissionResolver permissionResolver;
    private final DateTimeService dateTimeService;
    private final SupabaseAdminClient supabaseAdmin;

    @Inject
    public MeService(@Platform DataAccess data, PermissionResolver permissionResolver, DateTimeService dateTimeService, SupabaseAdminClient supabaseAdmin) {
        this.data = data;
        this.permissionResolver = permissionResolver;
        this.dateTimeService = dateTimeService;
        this.supabaseAdmin = supabaseAdmin;
    }

    public MeDto me(String sub) {
        var user = data.read().selectFrom(USERS).where(USERS.SUB.eq(sub)).fetchOne();
        if (user == null) {
            throw new NotFoundException("User not found: " + sub);
        }
        List<String> permissions = permissionResolver.resolve(sub, user.getTenantId()).stream().sorted().toList();
        return new MeDto(user.getSub(), user.getUsername(), user.getTenantId(), user.getMustChangePassword(), permissions);
    }

    /**
     * Changes the caller's own password.
     *
     * <p>Outside the forced first-login state the caller must prove the current password first: the
     * endpoint is authenticated by a bearer token only, and a stolen token must not be enough to take
     * the account over. The proof is a Supabase password grant for the caller's own Auth identity — the
     * same check {@code LoginService} performs; the platform itself never sees a stored credential.
     * Inside the forced state the proof is skipped, because the caller authenticated with exactly that
     * password moments ago (that is what put them in this state).
     */
    public void changePassword(String sub, ChangePasswordRequest request) {
        var user = data.read().selectFrom(USERS).where(USERS.SUB.eq(sub)).fetchOne();
        if (user == null) {
            throw new NotFoundException("User not found: " + sub);
        }
        requireNewPassword(request.password());
        if (!user.getMustChangePassword()) {
            requireCurrentPassword(user.getEmail(), request.currentPassword());
        }
        supabaseAdmin.updatePassword(sub, request.password());
        markPasswordChanged(sub);
    }

    /** A blank new password is a rejected value (422), never a silent no-op. */
    private static void requireNewPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("password must not be blank");
        }
    }

    /** Proves the caller knows their current password, or fails as a rejected value (422). */
    private void requireCurrentPassword(String email, String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ValidationException("currentPassword is required to change your password");
        }
        try {
            supabaseAdmin.login(email, currentPassword);
        } catch (AccessDeniedException e) {
            throw new ValidationException("Current password is incorrect.");
        }
    }

    public void markPasswordChanged(String sub) {
        OffsetDateTime now = dateTimeService.now().atOffset(ZoneOffset.UTC);
        int updated = data.write().update(USERS)
                .set(USERS.MUST_CHANGE_PASSWORD, false)
                .set(USERS.UPDATED_AT, now)
                .where(USERS.SUB.eq(sub))
                .execute();
        if (updated == 0) {
            throw new NotFoundException("User not found: " + sub);
        }
    }
}
