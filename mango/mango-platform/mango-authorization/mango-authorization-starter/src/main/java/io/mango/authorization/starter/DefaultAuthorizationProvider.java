package io.mango.authorization.starter;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.authorization.api.AuthorityContributor;
import io.mango.authorization.api.IAuthorizationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 基于已注册权限贡献器的默认授权提供器。 */
@Component
@RequiredArgsConstructor
public class DefaultAuthorizationProvider implements IAuthorizationProvider {

    private final List<AuthorityContributor> authorityContributors;

    @Override
    public AuthorizationSnapshotVO load(AuthorizationQuery query) {
        AuthorizationSnapshotVO snapshot = AuthorizationSnapshotVO.empty();
        for (AuthorityContributor contributor : authorityContributors) {
            if (contributor.supports(query)) {
                snapshot = snapshot.merge(contributor.contribute(query));
            }
        }
        return snapshot;
    }
}
