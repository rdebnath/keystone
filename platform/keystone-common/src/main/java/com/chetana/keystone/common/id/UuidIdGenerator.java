package com.chetana.keystone.common.id;

import java.util.UUID;

public final class UuidIdGenerator implements IdGenerator {

    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
