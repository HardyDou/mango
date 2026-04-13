package io.mango.auth.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Auth remote auto configuration - enables Feign clients for auth service
 *
 * @author Mango
 */
@Configuration
@EnableFeignClients(basePackages = "io.mango.auth.starter.remote")
public class AuthRemoteAutoConfiguration {
}
