package com.chetana.keystone.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runs a unit of work inside a jOOQ transaction, so services can keep the transaction
 * boundary explicit without threading a {@code Configuration} through their signatures.
 */
@Singleton
public final class TransactionRunner {

    private final DSLContext dsl;

    @Inject
    public TransactionRunner(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void run(Consumer<DSLContext> action) {
        dsl.transaction(configuration -> action.accept(DSL.using(configuration)));
    }

    public <T> T call(Function<DSLContext, T> action) {
        return dsl.transactionResult(configuration -> action.apply(DSL.using(configuration)));
    }
}
