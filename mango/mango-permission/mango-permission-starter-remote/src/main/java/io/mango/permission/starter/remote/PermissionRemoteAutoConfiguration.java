package io.mango.permission.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Permission remote auto configuration - enables Feign clients
 *
 * @author Mango
 */
@Configuration
@EnableFeignClients(basePackages = "io.mango.permission.starter.remote")
public class PermissionRemoteAutoConfiguration {
}
