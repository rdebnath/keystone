package com.chetana.keystone.platform.admin.identity;

import com.chetana.keystone.common.error.ValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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

    /**
     * The level {@code value} names — the code suffix ({@code read-only}, {@code read-write}) rather than the
     * enum name, because that is what a caller filtering the catalog already has: it is the last segment of
     * every permission code.
     */
    public static Access from(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("access must not be blank");
        }
        String suffix = value.trim().toLowerCase(Locale.ROOT);
        for (Access access : values()) {
            if (access.suffix.equals(suffix)) {
                return access;
            }
        }
        throw new ValidationException("access must be " + String.join(" or ", SUFFIXES));
    }

    /**
     * The {@code access} filter of a list request: an absent value means both levels. Parsed like
     * {@link Scope#optional(String)} — absent is not an error, a present-but-invalid value is.
     */
    public static Access optional(String value) {
        return value == null || value.isBlank() ? null : from(value);
    }
}
