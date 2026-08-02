package io.mango.infra.bootstrap.starter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = BootstrapAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ServletContextInitializer.class)
@ConditionalOnBean(RuntimeLeaseManager.class)
public class BootstrapServletRuntimeAutoConfiguration {

    @Bean
    RuntimeReceiptServletContextInitializer runtimeReceiptServletContextInitializer(
            ObjectProvider<RuntimeLeaseManager> leaseManagerProvider) {
        return new RuntimeReceiptServletContextInitializer(leaseManagerProvider);
    }
}
