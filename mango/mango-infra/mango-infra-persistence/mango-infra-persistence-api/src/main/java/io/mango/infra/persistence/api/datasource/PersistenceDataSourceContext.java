package io.mango.infra.persistence.api.datasource;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 当前线程数据源上下文。
 */
public final class PersistenceDataSourceContext {

    private static final ThreadLocal<Deque<String>> CONTEXT = ThreadLocal.withInitial(ArrayDeque::new);
    private static volatile Supplier<Optional<String>> transactionBoundDataSourceLookup = Optional::empty;

    private PersistenceDataSourceContext() {
    }

    public static Optional<String> current() {
        Deque<String> stack = CONTEXT.get();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.peek());
    }

    public static Scope use(String dataSourceName) {
        if (!StringUtils.hasText(dataSourceName)) {
            throw new IllegalArgumentException("Datasource name must not be blank");
        }
        String resolvedName = dataSourceName.trim();
        assertTransactionSwitchAllowed(resolvedName);
        CONTEXT.get().push(resolvedName);
        return new Scope(resolvedName);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void registerTransactionBoundDataSourceLookup(Supplier<Optional<String>> lookup) {
        transactionBoundDataSourceLookup = lookup == null ? Optional::empty : lookup;
    }

    private static void assertTransactionSwitchAllowed(String targetDataSourceName) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        Optional<String> currentName = current().or(transactionBoundDataSourceLookup);
        if (currentName.isPresent() && !targetDataSourceName.equals(currentName.get())) {
            throw new IllegalStateException("Cannot switch Mango datasource inside one transaction: current="
                    + currentName.get() + ", target=" + targetDataSourceName);
        }
    }

    public static final class Scope implements AutoCloseable {

        private final String dataSourceName;

        private boolean closed;

        private Scope(String dataSourceName) {
            this.dataSourceName = dataSourceName;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            Deque<String> stack = CONTEXT.get();
            if (stack.isEmpty()) {
                closed = true;
                CONTEXT.remove();
                return;
            }
            String current = stack.pop();
            if (!dataSourceName.equals(current)) {
                CONTEXT.remove();
                throw new IllegalStateException("Mango datasource context stack is corrupted");
            }
            if (stack.isEmpty()) {
                CONTEXT.remove();
            }
            closed = true;
        }
    }
}
