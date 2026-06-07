package io.mango.domain.starter.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 业务域远程调用自动配置。
 */
@AutoConfiguration
@EnableFeignClients(basePackageClasses = DomainFeignClient.class)
public class DomainRemoteAutoConfiguration {
}
