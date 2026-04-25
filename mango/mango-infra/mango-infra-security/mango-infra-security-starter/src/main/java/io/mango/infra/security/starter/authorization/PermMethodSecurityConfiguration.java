package io.mango.infra.security.starter.authorization;

import io.mango.infra.security.api.IPermissionService;
import io.mango.infra.security.api.Perm;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;

/**
 * Method security configuration for {@link Perm}.
 */
@Configuration(proxyBeanMethods = false)
public class PermMethodSecurityConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor permAuthorizationAdvisor(IPermissionService permissionService) {
        AuthorizationManagerBeforeMethodInterceptor interceptor = new AuthorizationManagerBeforeMethodInterceptor(
                new AnnotationMatchingPointcut(null, Perm.class, true),
                new PermAuthorizationManager(permissionService));
        interceptor.setOrder(AuthorizationInterceptorsOrder.PRE_AUTHORIZE.getOrder());
        return interceptor;
    }
}
