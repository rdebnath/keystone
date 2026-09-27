package com.chetana.keystone.platform.admin;

import com.chetana.keystone.common.id.UuidIdGenerator;
import com.chetana.keystone.common.time.SystemDateTimeService;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.platform.admin.config.AdminConfig;
import com.chetana.keystone.platform.admin.supabase.Session;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The bootstrap is optional ({@code bootstrap.enabled} / {@code BOOTSTRAP_ON_START}); when it is
 * disabled it must be a no-op that touches neither the platform schema nor Supabase Auth. The
 * collaborators below fail on any use, so a missing guard shows up as a failure rather than as a
 * quiet write.
 */
class BootstrapRunnerTest {

    @Test
    void should_touch_neither_the_database_nor_supabase_when_disabled() {
        BootstrapRunner runner = new BootstrapRunner(
                new UnusableSupabaseAdminClient(),
                new AdminConfig.Bootstrap(false, "admin", "admin@keystone.com", "changeit"),
                new UnusableDataAccess(),
                new UuidIdGenerator(),
                new SystemDateTimeService());

        assertThatCode(runner::bootstrap).doesNotThrowAnyException();
    }

    /** Fails on any Auth call: a disabled bootstrap must not provision or adopt a Supabase user. */
    private static final class UnusableSupabaseAdminClient implements SupabaseAdminClient {

        @Override
        public String createUser(String email, String password) {
            throw new AssertionError("createUser must not be called");
        }

        @Override
        public Optional<String> findSubByEmail(String email) {
            throw new AssertionError("findSubByEmail must not be called");
        }

        @Override
        public Session login(String email, String password) {
            throw new AssertionError("login must not be called");
        }

        @Override
        public void updatePassword(String sub, String password) {
            throw new AssertionError("updatePassword must not be called");
        }
    }

    /** Fails on any query: a disabled bootstrap must not read or write the platform schema. */
    private static final class UnusableDataAccess implements DataAccess {

        @Override
        public DSLContext read() {
            throw new AssertionError("read must not be called");
        }

        @Override
        public DSLContext write() {
            throw new AssertionError("write must not be called");
        }

        @Override
        public void transaction(Consumer<DSLContext> action) {
            throw new AssertionError("transaction must not be called");
        }

        @Override
        public <T> T transactionResult(Function<DSLContext, T> action) {
            throw new AssertionError("transactionResult must not be called");
        }

        @Override
        public <T> T readFromPrimary(Supplier<T> action) {
            throw new AssertionError("readFromPrimary must not be called");
        }

        @Override
        public void readFromPrimary(Runnable action) {
            throw new AssertionError("readFromPrimary must not be called");
        }
    }
}
