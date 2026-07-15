package io.mango.infra.persistence.api.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class PersistenceDataSourceContextTest {

    @AfterEach
    void cleanThreadState() {
        PersistenceDataSourceContext.clear();
        PersistenceDataSourceContext.registerTransactionBoundDataSourceLookup(Optional::empty);
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void nestedScopesRestorePreviousDataSource() {
        assertThat(PersistenceDataSourceContext.current()).isEmpty();

        try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use(" primary ")) {
            assertThat(PersistenceDataSourceContext.current()).contains("primary");
            try (PersistenceDataSourceContext.Scope nested = PersistenceDataSourceContext.use("archive")) {
                assertThat(PersistenceDataSourceContext.current()).contains("archive");
            }
            assertThat(PersistenceDataSourceContext.current()).contains("primary");
        }

        assertThat(PersistenceDataSourceContext.current()).isEmpty();
    }

    @Test
    void rejectsBlankDataSourceNames() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PersistenceDataSourceContext.use("  "))
                .withMessage("Datasource name must not be blank");
    }

    @Test
    void preventsSwitchingThreadContextInsideTransaction() {
        try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use("primary")) {
            TransactionSynchronizationManager.setActualTransactionActive(true);

            assertThatIllegalStateException()
                    .isThrownBy(() -> PersistenceDataSourceContext.use("archive"))
                    .withMessageContaining("current=primary, target=archive");
            assertThat(PersistenceDataSourceContext.current()).contains("primary");
        }
    }

    @Test
    void preventsSwitchingTransactionBoundDataSourceWithoutThreadContext() {
        PersistenceDataSourceContext.registerTransactionBoundDataSourceLookup(() -> Optional.of("primary"));
        TransactionSynchronizationManager.setActualTransactionActive(true);

        assertThatIllegalStateException()
                .isThrownBy(() -> PersistenceDataSourceContext.use("archive"))
                .withMessageContaining("current=primary, target=archive");
        assertThat(PersistenceDataSourceContext.current()).isEmpty();
    }

    @Test
    void detectsOutOfOrderScopeClosureAndClearsCorruptedContext() {
        PersistenceDataSourceContext.Scope primary = PersistenceDataSourceContext.use("primary");
        PersistenceDataSourceContext.Scope archive = PersistenceDataSourceContext.use("archive");

        assertThatIllegalStateException()
                .isThrownBy(primary::close)
                .withMessage("Mango datasource context stack is corrupted");
        assertThat(PersistenceDataSourceContext.current()).isEmpty();

        archive.close();
    }
}
