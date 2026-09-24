package com.chetana.keystone.platform.admin.permission;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.identity.Scope;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.Tables.ROLE_PERMISSIONS;

/**
 * Use-case service for the permission catalog (platform- and tenant-scoped).
 */
@Singleton
public final class PermissionService {

    private final DSLContext dsl;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;

    @Inject
    public PermissionService(@Platform DSLContext dsl, IdGenerator idGenerator, DateTimeService dateTimeService) {
        this.dsl = dsl;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
    }

    public List<PermissionDto> list() {
        return dsl.selectFrom(PERMISSIONS)
                .orderBy(PERMISSIONS.CODE)
                .fetch()
                .map(r -> new PermissionDto(r.getId(), r.getCode(), Scope.from(r.getScope()), r.getCreatedAt(), r.getUpdatedAt()));
    }

    public PermissionDto create(PermissionRequest request) {
        validate(request);
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        int inserted = dsl.insertInto(PERMISSIONS, PERMISSIONS.ID, PERMISSIONS.CODE, PERMISSIONS.SCOPE, PERMISSIONS.CREATED_AT, PERMISSIONS.UPDATED_AT)
                .values(id, request.code(), request.scope().name(), now, now)
                .onConflictDoNothing()
                .execute();
        if (inserted == 0) {
            throw new ConflictException("Permission already exists: " + request.code());
        }
        return new PermissionDto(id, request.code(), request.scope(), now, now);
    }

    public void delete(UUID id) {
        dsl.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);
            tx.deleteFrom(ROLE_PERMISSIONS).where(ROLE_PERMISSIONS.PERMISSION_ID.eq(id)).execute();
            tx.deleteFrom(PERMISSIONS).where(PERMISSIONS.ID.eq(id)).execute();
        });
    }

    private OffsetDateTime now() {
        return dateTimeService.now().atOffset(ZoneOffset.UTC);
    }

    private static void validate(PermissionRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new ValidationException("code must not be blank");
        }
        if (request.scope() == null) {
            throw new ValidationException("scope must not be null");
        }
    }
}
