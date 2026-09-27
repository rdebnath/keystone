package com.chetana.keystone.platform.admin.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two access levels of a permission and the code suffixes that carry them.
 */
class AccessTest {

    @Test
    void should_expose_the_read_only_and_read_write_suffixes() {
        assertThat(Access.READ_ONLY.suffix()).isEqualTo("read-only");
        assertThat(Access.READ_WRITE.suffix()).isEqualTo("read-write");
    }

    @Test
    void should_list_exactly_the_two_level_suffixes() {
        assertThat(Access.suffixes()).containsExactly("read-only", "read-write");
    }
}
