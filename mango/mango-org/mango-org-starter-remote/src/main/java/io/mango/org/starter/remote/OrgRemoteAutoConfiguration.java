package io.mango.org.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Org remote auto configuration
 *
 * @author Mango
 */
@Configuration
@EnableFeignClients(basePackages = "io.mango.org.starter.remote")
public class OrgRemoteAutoConfiguration {
}
