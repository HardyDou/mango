package io.mango.area.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Area remote auto configuration
 *
 * @author Mango
 */
@Configuration
@EnableFeignClients(basePackages = "io.mango.area.starter.remote")
public class AreaRemoteAutoConfiguration {
}
