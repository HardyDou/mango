package io.mango.app.microservice.gateway;

import io.mango.gateway.core.config.DynamicWhiteListConfig;
import io.mango.gateway.core.config.GatewayProperties;
import io.mango.gateway.starter.remote.filter.AuthGlobalFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Mango 网关部署入口。
 *
 * @author hardy
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
@Import({
        DynamicWhiteListConfig.class,
        AuthGlobalFilter.class
})
public class MangoGatewayAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoGatewayAppApplication.class, args);
    }
}
