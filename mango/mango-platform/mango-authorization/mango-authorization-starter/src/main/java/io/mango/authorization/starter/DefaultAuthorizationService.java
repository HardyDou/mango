package io.mango.authorization.starter;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.AuthorityContributor;
import io.mango.authorization.api.IAuthorizationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default authorization service backed by registered authority contributors.
 */
@Component
@RequiredArgsConstructor
public class DefaultAuthorizationService implements IAuthorizationProvider {

    private final List<AuthorityContributor> authorityContributors;

    @Override
    public AuthorizationSnapshot load(AuthorizationQuery query) {
        AuthorizationSnapshot snapshot = AuthorizationSnapshot.empty();
        for (AuthorityContributor contributor : authorityContributors) {
            if (contributor.supports(query)) {
                snapshot = snapshot.merge(contributor.contribute(query));
            }
        }
        return snapshot;
    }
}
