package com.chetana.keystone.platform.admin.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailsTest {

    @Test
    void should_derive_default_email_from_username_and_slug() {
        assertThat(Emails.derive("alice", "acme", null)).isEqualTo("alice@acme.com");
    }

    @Test
    void should_use_provided_email_lowercased() {
        assertThat(Emails.derive("alice", "acme", "  Alice@AcmeCorp.com ")).isEqualTo("alice@acmecorp.com");
    }

    @Test
    void should_ignore_blank_provided_email() {
        assertThat(Emails.derive("alice", "acme", "   ")).isEqualTo("alice@acme.com");
    }
}
