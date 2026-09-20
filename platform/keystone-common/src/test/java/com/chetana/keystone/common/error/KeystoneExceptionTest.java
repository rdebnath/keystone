package com.chetana.keystone.common.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeystoneExceptionTest {

    @Test
    void should_carry_code_and_message_when_created() {
        var exception = new NotFoundException("Order 42 was not found");

        assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("Order 42 was not found");
    }

    @Test
    void should_expose_conflict_code_when_conflict_exception_raised() {
        var exception = new ConflictException("already exists");

        assertThat(exception.code()).isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void should_expose_validation_code_when_validation_exception_raised() {
        var exception = new ValidationException("name must not be blank");

        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION);
    }

    @Test
    void should_expose_access_denied_code_when_access_denied_exception_raised() {
        var exception = new AccessDeniedException("forbidden");

        assertThat(exception.code()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }
}
