package io.mango.infra.persistence.starter.datasource;

import io.mango.infra.persistence.api.datasource.PersistenceDataSourceContext;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

/**
 * Mango 多数据源路由。
 */
public class MangoRoutingDataSource extends AbstractRoutingDataSource {

    private static final Object TX_DATASOURCE_RESOURCE_KEY = MangoRoutingDataSource.class.getName() + ".datasource";
    private static final ThreadLocal<String> LAST_ROUTED_DATASOURCE_NAME = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> LAST_ROUTED_CLEANUP_REGISTERED = new ThreadLocal<>();

    private final PersistenceDataSourceRegistry registry;

    static {
        PersistenceDataSourceContext.registerTransactionBoundDataSourceLookup(MangoRoutingDataSource::boundTransactionDataSourceName);
    }

    public MangoRoutingDataSource(PersistenceDataSourceRegistry registry) {
        this.registry = registry;
        setTargetDataSources(registry.targetDataSources());
        setDefaultTargetDataSource(registry.get(registry.primaryName()));
        afterPropertiesSet();
    }

    public PersistenceDataSourceRegistry registry() {
        return registry;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceName = PersistenceDataSourceContext.current().orElse(registry.primaryName());
        if (registry.find(dataSourceName).isEmpty()) {
            throw new IllegalStateException("Mango datasource does not exist: " + dataSourceName);
        }
        LAST_ROUTED_DATASOURCE_NAME.set(dataSourceName);
        bindTransactionDataSource(dataSourceName);
        return dataSourceName;
    }

    static Optional<String> boundTransactionDataSourceName() {
        Object bound = TransactionSynchronizationManager.getResource(TX_DATASOURCE_RESOURCE_KEY);
        if (bound instanceof String dataSourceName) {
            return Optional.of(dataSourceName);
        }
        if (hasRoutingDataSourceTransactionResource()) {
            registerLastRoutedCleanup();
            return Optional.ofNullable(LAST_ROUTED_DATASOURCE_NAME.get());
        }
        return Optional.empty();
    }

    private static boolean hasRoutingDataSourceTransactionResource() {
        for (Object key : TransactionSynchronizationManager.getResourceMap().keySet()) {
            if (key instanceof MangoRoutingDataSource) {
                return true;
            }
        }
        return false;
    }

    private static void registerLastRoutedCleanup() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || Boolean.TRUE.equals(LAST_ROUTED_CLEANUP_REGISTERED.get())) {
            return;
        }
        LAST_ROUTED_CLEANUP_REGISTERED.set(true);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                LAST_ROUTED_DATASOURCE_NAME.remove();
                LAST_ROUTED_CLEANUP_REGISTERED.remove();
            }
        });
    }

    private void bindTransactionDataSource(String dataSourceName) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        Object bound = TransactionSynchronizationManager.getResource(TX_DATASOURCE_RESOURCE_KEY);
        if (bound == null) {
            TransactionSynchronizationManager.bindResource(TX_DATASOURCE_RESOURCE_KEY, dataSourceName);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (TransactionSynchronizationManager.hasResource(TX_DATASOURCE_RESOURCE_KEY)) {
                        TransactionSynchronizationManager.unbindResource(TX_DATASOURCE_RESOURCE_KEY);
                    }
                }
            });
            return;
        }
        if (!dataSourceName.equals(bound)) {
            throw new IllegalStateException("Cannot switch Mango datasource inside one transaction: current="
                    + bound + ", target=" + dataSourceName);
        }
    }
}
