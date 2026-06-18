package io.mango.authorization.starter;

import io.mango.authorization.core.config.FrontendRuntimeProperties;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.service.IRoleDataScopeService;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.org.api.SysOrgApi;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Authorization service auto configuration
 *
 * @author Mango
 */
@AutoConfiguration
@ConditionalOnClass(RoleMapper.class)
@MapperScan("io.mango.authorization.core.mapper")
@EnableConfigurationProperties(FrontendRuntimeProperties.class)
@ComponentScan({
    "io.mango.authorization.core.service",
    "io.mango.authorization.starter"
})
public class AuthorizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataScopeProvider.class)
    public DataScopeProvider authorizationDataScopeProvider(IRoleDataScopeService roleDataScopeService,
                                                            TenantMemberProvider tenantMemberProvider,
                                                            ObjectProvider<SysOrgApi> sysOrgApiProvider) {
        return new AuthorizationDataScopeProvider(roleDataScopeService, tenantMemberProvider, sysOrgApiProvider);
    }
}
