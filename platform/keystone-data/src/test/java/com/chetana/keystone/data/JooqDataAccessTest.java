package com.chetana.keystone.data;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JooqDataAccessTest {

    private final DSLContext primary = DSL.using(SQLDialect.POSTGRES);
    private final DSLContext replica = DSL.using(SQLDialect.POSTGRES);
    private final JooqDataAccess data = new JooqDataAccess(primary, replica);

    @Test
    void should_route_reads_to_replica_and_writes_to_primary_by_default() {
        assertThat(data.read()).isSameAs(replica);
        assertThat(data.write()).isSameAs(primary);
    }

    @Test
    void should_route_reads_to_primary_within_read_from_primary_scope() {
        assertThat(data.readFromPrimary(data::read)).isSameAs(primary);
    }

    @Test
    void should_restore_replica_routing_after_read_from_primary_scope() {
        data.readFromPrimary(() -> assertThat(data.read()).isSameAs(primary));
        assertThat(data.read()).isSameAs(replica);
    }
}
