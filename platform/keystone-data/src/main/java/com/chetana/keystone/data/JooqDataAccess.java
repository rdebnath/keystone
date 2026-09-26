package com.chetana.keystone.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * jOOQ-backed {@link DataAccess}. {@link #read()} consults a {@link ScopedValue} so that a
 * {@link #readFromPrimary(Supplier)} / {@link #readFromPrimary(Runnable)} scope redirects reads to
 * the primary without threading any context through call signatures (and composes correctly with
 * virtual threads).
 */
@Singleton
public final class JooqDataAccess implements DataAccess {

    private static final Logger log = LoggerFactory.getLogger(JooqDataAccess.class);
    private static final ScopedValue<Boolean> READ_FROM_PRIMARY = ScopedValue.newInstance();

    private final DSLContext primary;
    private final DSLContext replica;

    @Inject
    public JooqDataAccess(@Primary DSLContext primary, @Replica DSLContext replica) {
        this.primary = primary;
        this.replica = replica;
    }

    @Override
    public DSLContext read() {
        boolean fromPrimary = READ_FROM_PRIMARY.isBound() && READ_FROM_PRIMARY.get();
        if (log.isDebugEnabled()) {
            log.debug("Read routed to {}", fromPrimary ? "primary" : "replica");
        }
        return fromPrimary ? primary : replica;
    }

    @Override
    public DSLContext write() {
        return primary;
    }

    @Override
    public void transaction(Consumer<DSLContext> action) {
        primary.transaction(configuration -> action.accept(DSL.using(configuration)));
    }

    @Override
    public <T> T transactionResult(Function<DSLContext, T> action) {
        return primary.transactionResult(configuration -> action.apply(DSL.using(configuration)));
    }

    @Override
    public <T> T readFromPrimary(Supplier<T> action) {
        var result = new Object() {
            T value;
        };
        ScopedValue.where(READ_FROM_PRIMARY, Boolean.TRUE).run(() -> result.value = action.get());
        return result.value;
    }

    @Override
    public void readFromPrimary(Runnable action) {
        ScopedValue.where(READ_FROM_PRIMARY, Boolean.TRUE).run(action);
    }
}
