package io.mango.authorization.core.service.impl;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.query.LoadUserAuthorizationQuery;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.authorization.core.service.IAuthorizationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 授权快照查询服务实现。 */
@Service
@RequiredArgsConstructor
public class AuthorizationQueryService implements IAuthorizationQueryService {

    private final IAuthorizationProvider authorizationProvider;

    @Override
    public AuthorizationSnapshotVO loadUserAuthorization(LoadUserAuthorizationQuery query) {
        AuthorizationQuery authorizationQuery = AuthorizationQuery.member(query.getSubjectId())
                .withTenantId(query.getTenantId())
                .withSystemCode(query.getSystemCode())
                .withRealm(query.getRealm())
                .withActorType(query.getActorType())
                .withParty(query.getPartyType(), query.getPartyId());
        return authorizationProvider.load(authorizationQuery);
    }
}
