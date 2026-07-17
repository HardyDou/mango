package io.mango.cms.starter.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * CMS 远程调用自动配置。
 */
@AutoConfiguration
@EnableFeignClients(basePackageClasses = CmsSiteFeignClient.class)
public class CmsRemoteAutoConfiguration {
}
