package io.mango.identity.starter.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Identity remote auto configuration.
 */
@AutoConfiguration
@EnableFeignClients(basePackages = "io.mango.identity.starter.remote")
public class IdentityRemoteAutoConfiguration {
}
