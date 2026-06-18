package io.mango.resource.starter;

import io.mango.resource.core.config.ResourceRegistryCoreConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * 资源注册中心自动配置。
 */
@AutoConfiguration
@MapperScan("io.mango.resource.core.mapper")
@ComponentScan({
        "io.mango.resource.starter.controller",
        "io.mango.resource.starter.service"
})
@Import(ResourceRegistryCoreConfiguration.class)
public class ResourceRegistryAutoConfiguration {
}
