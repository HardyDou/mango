package {{basePackage}}.{{modulePackage}}.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * {{moduleName}}远程调用自动配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackageClasses = {{modulePascal}}FeignClient.class)
public class {{modulePascal}}RemoteAutoConfiguration {
}
