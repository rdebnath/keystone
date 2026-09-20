package com.chetana.keystone.inventory.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.chetana.keystone.inventory.jooq.Tables.ITEMS;

@Singleton
public final class ItemService {

    private final DSLContext dsl;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;

    @Inject
    public ItemService(DSLContext dsl, IdGenerator idGenerator, DateTimeService dateTimeService) {
        this.dsl = dsl;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
    }

    public List<ItemDto> list() {
        return dsl.selectFrom(ITEMS)
                .orderBy(ITEMS.NAME)
                .fetch()
                .map(r -> new ItemDto(r.getId(), r.getName(), r.getQuantity(), r.getVersion(),
                        r.getCreatedAt(), r.getUpdatedAt()));
    }

    public ItemDto create(CreateItemRequest request) {
        validate(request);
        UUID id = idGenerator.nextId();
        OffsetDateTime now = dateTimeService.now().atOffset(ZoneOffset.UTC);
        dsl.insertInto(ITEMS, ITEMS.ID, ITEMS.NAME, ITEMS.QUANTITY, ITEMS.VERSION, ITEMS.CREATED_AT, ITEMS.UPDATED_AT)
                .values(id, request.name(), request.quantity(), 0L, now, now)
                .execute();
        return new ItemDto(id, request.name(), request.quantity(), 0L, now, now);
    }

    private static void validate(CreateItemRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("name must not be blank");
        }
        if (request.quantity() == null || request.quantity() < 0) {
            throw new ValidationException("quantity must be zero or greater");
        }
    }
}
