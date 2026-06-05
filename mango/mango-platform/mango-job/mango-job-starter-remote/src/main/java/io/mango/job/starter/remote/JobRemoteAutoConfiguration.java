package io.mango.job.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Mango Job 远程调用自动配置。
 */
@Configuration
@EnableFeignClients(basePackages = "io.mango.job.starter.remote")
public class JobRemoteAutoConfiguration {
}
