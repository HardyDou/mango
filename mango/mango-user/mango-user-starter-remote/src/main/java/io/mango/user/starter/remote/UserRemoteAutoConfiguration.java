package io.mango.user.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * User remote auto configuration - enables Feign clients for user service
 *
 * @author Mango
 */
@Configuration
@EnableFeignClients(basePackages = "io.mango.user.starter.remote")
public class UserRemoteAutoConfiguration {
}
