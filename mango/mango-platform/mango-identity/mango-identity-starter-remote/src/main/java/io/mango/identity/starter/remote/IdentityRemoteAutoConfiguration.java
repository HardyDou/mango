package io.mango.identity.starter.remote;

import io.mango.common.result.R;
import io.mango.identity.api.AuthIdentitySecurityProvider;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.query.AuthUsernameQuery;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.identity.api.vo.TenantMemberInfo;
import io.mango.identity.api.vo.TenantMemberOrgRelationInfo;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.util.Collection;
import java.util.List;

/**
 * 身份模块远程自动配置。
 */
@AutoConfiguration
@EnableFeignClients(basePackages = "io.mango.identity.starter.remote")
public class IdentityRemoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthUserProvider authUserProvider(ObjectProvider<AuthIdentityFeignClient> authIdentityFeignClient) {
        return new RemoteAuthUserProvider(authIdentityFeignClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthIdentitySecurityProvider authIdentitySecurityProvider(
            ObjectProvider<AuthIdentityFeignClient> authIdentityFeignClient) {
        return new RemoteAuthIdentitySecurityProvider(authIdentityFeignClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantMemberProvider tenantMemberProvider(ObjectProvider<TenantMemberFeignClient> tenantMemberFeignClient) {
        return new RemoteTenantMemberProvider(tenantMemberFeignClient);
    }

    private static class RemoteAuthIdentitySecurityProvider implements AuthIdentitySecurityProvider {

        private final ObjectProvider<AuthIdentityFeignClient> authIdentityFeignClient;

        RemoteAuthIdentitySecurityProvider(ObjectProvider<AuthIdentityFeignClient> authIdentityFeignClient) {
            this.authIdentityFeignClient = authIdentityFeignClient;
        }

        @Override
        public void assertLoginAllowed(AuthUserInfo user) {
            if (user == null || user.getLockedUntil() == null
                    || !user.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
                return;
            }
            throw new io.mango.common.exception.BizException(1429, "账号已被临时锁定，请稍后再试或联系管理员");
        }

        @Override
        public void recordLoginFailure(Long userId) {
            requireSuccess(authIdentityFeignClient.getObject().recordLoginFailure(userId));
        }

        @Override
        public void recordLoginSuccess(Long userId) {
            requireSuccess(authIdentityFeignClient.getObject().recordLoginSuccess(userId));
        }

        @Override
        public void changeRequiredPassword(ChangeRequiredPasswordCommand command) {
            requireSuccess(authIdentityFeignClient.getObject().changeRequiredPassword(command));
        }

        private void requireSuccess(R<Boolean> response) {
            if (response == null || !response.isSuccess() || !Boolean.TRUE.equals(response.getData())) {
                String message = response == null ? "Remote identity security operation failed" : response.getMsg();
                throw new IllegalStateException(message);
            }
        }
    }

    private static class RemoteAuthUserProvider implements AuthUserProvider {

        private final ObjectProvider<AuthIdentityFeignClient> authIdentityFeignClient;

        RemoteAuthUserProvider(ObjectProvider<AuthIdentityFeignClient> authIdentityFeignClient) {
            this.authIdentityFeignClient = authIdentityFeignClient;
        }

        @Override
        public AuthUserInfo getByUsernameForAuth(String username) {
            AuthUsernameQuery query = new AuthUsernameQuery();
            query.setUsername(username);
            return unwrap(authIdentityFeignClient.getObject().getByUsernameForAuth(query));
        }

        @Override
        public AuthUserInfo getByUsernameForAuth(String username, String realm) {
            AuthUsernameQuery query = new AuthUsernameQuery();
            query.setUsername(username);
            query.setRealm(realm);
            return unwrap(authIdentityFeignClient.getObject().getByUsernameForAuth(query));
        }

        @Override
        public AuthUserInfo getByIdForAuth(Long userId) {
            return unwrap(authIdentityFeignClient.getObject().getByIdForAuth(userId));
        }

        private AuthUserInfo unwrap(R<AuthUserInfo> response) {
            return response != null && response.isSuccess() ? response.getData() : null;
        }
    }

    private static class RemoteTenantMemberProvider implements TenantMemberProvider {

        private final ObjectProvider<TenantMemberFeignClient> tenantMemberFeignClient;

        RemoteTenantMemberProvider(ObjectProvider<TenantMemberFeignClient> tenantMemberFeignClient) {
            this.tenantMemberFeignClient = tenantMemberFeignClient;
        }

        @Override
        public TenantMemberInfo getEnabledMember(Long userId, Long tenantId) {
            return unwrap(tenantMemberFeignClient.getObject().getEnabledMember(userId, tenantId));
        }

        @Override
        public List<TenantMemberInfo> listEnabledMembers(Long userId) {
            return unwrapList(tenantMemberFeignClient.getObject().listEnabledMembers(userId));
        }

        @Override
        public TenantMemberInfo getMember(Long memberId) {
            return unwrap(tenantMemberFeignClient.getObject().getMember(memberId));
        }

        @Override
        public List<TenantMemberOrgRelationInfo> listOrgRelations(Long tenantId, Long orgId) {
            return unwrapList(tenantMemberFeignClient.getObject().listOrgRelations(tenantId, orgId));
        }

        @Override
        public TenantMemberOrgRelationInfo getOrgRelation(Long relationId) {
            return unwrap(tenantMemberFeignClient.getObject().getOrgRelation(relationId));
        }

        @Override
        public boolean existsOrgRelation(Long tenantId, Long memberId, Long orgId) {
            Boolean result = unwrap(tenantMemberFeignClient.getObject().existsOrgRelation(tenantId, memberId, orgId));
            return Boolean.TRUE.equals(result);
        }

        @Override
        public void addOrgRelation(AddTenantMemberOrgCommand command) {
            requireSuccess(tenantMemberFeignClient.getObject().addOrgRelation(command));
        }

        @Override
        public void updateOrgRelation(UpdateTenantMemberOrgCommand command) {
            requireSuccess(tenantMemberFeignClient.getObject().updateOrgRelation(command));
        }

        @Override
        public void removeOrgRelation(Long relationId) {
            requireSuccess(tenantMemberFeignClient.getObject().removeOrgRelation(relationId));
        }

        @Override
        public long countOtherOrgRelations(Long tenantId, Long memberId, Long excludedRelationId) {
            Long count = unwrap(tenantMemberFeignClient.getObject()
                    .countOtherOrgRelations(tenantId, memberId, excludedRelationId));
            return count == null ? 0L : count;
        }

        @Override
        public List<TenantMemberInfo> listMembers(Collection<Long> memberIds) {
            List<Long> ids = memberIds == null ? List.of() : List.copyOf(memberIds);
            return unwrapList(tenantMemberFeignClient.getObject().listMembers(ids));
        }

        private <T> T unwrap(R<T> response) {
            return response != null && response.isSuccess() ? response.getData() : null;
        }

        private <T> List<T> unwrapList(R<List<T>> response) {
            List<T> data = unwrap(response);
            return data == null ? List.of() : data;
        }

        private void requireSuccess(R<Boolean> response) {
            if (response == null || !response.isSuccess() || !Boolean.TRUE.equals(response.getData())) {
                String message = response == null ? "Remote tenant member operation failed" : response.getMsg();
                throw new IllegalStateException(message);
            }
        }
    }
}
