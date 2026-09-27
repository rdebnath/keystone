package com.chetana.keystone.platform.admin.identity;

import java.util.Arrays;
import java.util.List;

/**
 * Access level of a permission: {@code READ_WRITE} (read, create, update and delete) or
 * {@code READ_ONLY} (read only). A read/write grant also satisfies a read-only check; a read-only
 * grant never satisfies a read/write check.
 */
public enum Access {

    READ_ONLY("read-only"),
    READ_WRITE("read-write");

    private static final List<String> SUFFIXES = Arrays.stream(values()).map(Access::suffix).toList();

    private final String suffix;

    Access(String suffix) {
        this.suffix = suffix;
    }

    /** The code suffix that carries this level, e.g. {@code read-write}. */
    public String suffix() {
        return suffix;
    }

    /** Every code suffix a permission code may end in. */
    public static List<String> suffixes() {
        return SUFFIXES;
    }
}
