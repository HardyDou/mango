package io.mango.infra.persistence.starter.context;

import io.mango.infra.persistence.api.context.PersistenceContextProvider;
import io.mango.infra.persistence.api.scope.TenantProvider;

import java.util.Optional;

/**
 * 基于持久化上下文的租户提供者。
 */
public class MangoContextTenantProvider implements TenantProvider {

    private final PersistenceContextProvider contextProvider;

    public MangoContextTenantProvider(PersistenceContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    @Override
    public Optional<String> currentTenantId() {
        if (contextProvider == null || contextProvider.currentContext() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(contextProvider.currentContext().tenantId());
    }
}
