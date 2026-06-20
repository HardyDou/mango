package io.mango.infra.persistence.starter.context;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.api.context.PersistenceContext;
import io.mango.infra.persistence.api.context.PersistenceContextProvider;

/**
 * 基于 MangoContext 的持久化上下文提供者。
 */
public class MangoContextPersistenceContextProvider implements PersistenceContextProvider {

    @Override
    public PersistenceContext currentContext() {
        MangoContextSnapshot snapshot = MangoContextHolder.get();
        if (snapshot == null || snapshot.isEmpty()) {
            return PersistenceContext.empty();
        }
        return new PersistenceContext(
                snapshot.userId(),
                snapshot.principalName(),
                snapshot.tenantId(),
                snapshot.realm(),
                snapshot.actorType(),
                snapshot.partyType(),
                snapshot.partyId(),
                resolveOrgId(snapshot),
                snapshot.appCode()
        );
    }

    private Long resolveOrgId(MangoContextSnapshot snapshot) {
        if (snapshot.partyId() == null || snapshot.partyType() == null) {
            return null;
        }
        return "org".equalsIgnoreCase(snapshot.partyType()) ? snapshot.partyId() : null;
    }
}
