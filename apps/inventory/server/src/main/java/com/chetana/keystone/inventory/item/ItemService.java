package com.chetana.keystone.inventory.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.data.DataAccess;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.chetana.keystone.inventory.jooq.inventory.Tables.ITEMS;

@Singleton
public final class ItemService {

    private final DataAccess data;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;

    @Inject
    public ItemService(DataAccess data, IdGenerator idGenerator, DateTimeService dateTimeService) {
        this.data = data;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
    }

    public List<ItemDto> list() {
        return data.read().selectFrom(ITEMS)
                .orderBy(ITEMS.NAME)
                .fetch()
                .map(r -> new ItemDto(r.getId(), r.getName(), r.getQuantity(), r.getVersion(),
                        r.getCreatedAt(), r.getUpdatedAt()));
    }

    public ItemDto create(CreateItemRequest request) {
        validate(request);
        UUID id = idGenerator.nextId();
        OffsetDateTime now = dateTimeService.now().atOffset(ZoneOffset.UTC);
        data.write().insertInto(ITEMS, ITEMS.ID, ITEMS.NAME, ITEMS.QUANTITY, ITEMS.VERSION, ITEMS.CREATED_AT, ITEMS.UPDATED_AT)
                .values(id, request.name(), request.quantity(), 0L, now, now)
                .execute();
        // Read back from the primary so the response reflects the just-committed write regardless
        // of replica lag (read-your-writes).
        return data.readFromPrimary(() -> fetch(id));
    }

    private ItemDto fetch(UUID id) {
        var record = data.read().selectFrom(ITEMS).where(ITEMS.ID.eq(id)).fetchOne();
        return new ItemDto(record.getId(), record.getName(), record.getQuantity(), record.getVersion(),
                record.getCreatedAt(), record.getUpdatedAt());
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
