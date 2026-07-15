package io.mango.authorization.support.autoconfigure.sensitive;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.vo.SecurityContextVO;
import io.mango.infra.sensitive.api.ISensitiveRawAccessProvider;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Bridges Mango authorization to sensitive raw output checks.
 */
public class AuthorizationSensitiveRawAccessProvider implements ISensitiveRawAccessProvider {

    private final ISecurityContextProvider securityContextProvider;

    private final Supplier<IAuthorizationProvider> authorizationProviderSupplier;

    public AuthorizationSensitiveRawAccessProvider(ISecurityContextProvider securityContextProvider,
                                                   Supplier<IAuthorizationProvider> authorizationProviderSupplier) {
        this.securityContextProvider = securityContextProvider;
        this.authorizationProviderSupplier = authorizationProviderSupplier;
    }

    @Override
    public boolean canViewRaw(String authority) {
        if (!StringUtils.hasText(authority)) {
            return false;
        }
        SecurityContextVO context = securityContextProvider.currentContext();
        if (context == null || !context.authenticated()) {
            return false;
        }
        AuthorizationQuery query = authorizationQuery(context);
        if (query == null) {
            return false;
        }
        IAuthorizationProvider authorizationProvider = authorizationProviderSupplier.get();
        if (authorizationProvider == null) {
            return false;
        }
        AuthorizationSnapshotVO snapshot = authorizationProvider.load(query);
        return snapshot != null && snapshot.hasAuthority(authority.trim());
    }

    private AuthorizationQuery authorizationQuery(SecurityContextVO context) {
        AuthorizationQuery query = baseQuery(context);
        if (query == null) {
            return null;
        }
        return query.withTenantId(context.tenantId())
                .withSystemCode(context.appCode())
                .withRealm(context.realm())
                .withActorType(context.actorType())
                .withParty(context.partyType(), context.partyId());
    }

    private AuthorizationQuery baseQuery(SecurityContextVO context) {
        if (context.memberId() != null) {
            return AuthorizationQuery.member(context.memberId());
        }
        if (context.userId() != null) {
            return AuthorizationQuery.user(context.userId());
        }
        return null;
    }
}
