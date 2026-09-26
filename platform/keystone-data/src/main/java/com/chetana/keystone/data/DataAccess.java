package com.chetana.keystone.data;

import org.jooq.DSLContext;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Read/write database access facade. Routes writes and transactions to the primary (read-write)
 * instance and reads to the read replica, while allowing a caller to force a single operation's
 * reads back onto the primary (read-your-writes) via {@link #readFromPrimary(Supplier)} /
 * {@link #readFromPrimary(Runnable)}.
 *
 * <p>Callers must use {@link #read()} for reads and {@link #write()} for writes; the replica pool
 * is configured read-only, so an accidental write on a replica connection is rejected by the
 * database.
 */
public interface DataAccess {

    /** Returns a context for reads, backed by the read replica (or the primary when scoped). */
    DSLContext read();

    /** Returns a context for writes, always backed by the primary instance. */
    DSLContext write();

    /** Runs a unit of work inside a transaction on the primary instance. */
    void transaction(Consumer<DSLContext> action);

    /** Runs a unit of work inside a transaction on the primary instance and returns its result. */
    <T> T transactionResult(Function<DSLContext, T> action);

    /**
     * Runs {@code action} with reads forced onto the primary instance, for operations that write
     * and then immediately read the result (read-your-writes).
     */
    <T> T readFromPrimary(Supplier<T> action);

    /** See {@link #readFromPrimary(Supplier)}. */
    void readFromPrimary(Runnable action);
}
