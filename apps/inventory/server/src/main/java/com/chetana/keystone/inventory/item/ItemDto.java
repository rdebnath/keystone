package com.chetana.keystone.inventory.item;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ItemDto(UUID id, String name, Integer quantity, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
