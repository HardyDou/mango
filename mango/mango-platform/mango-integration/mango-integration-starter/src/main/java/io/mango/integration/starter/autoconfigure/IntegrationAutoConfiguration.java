package io.mango.integration.starter.autoconfigure;

import io.mango.integration.core.mapper.IntegrationConnectorMapper;
import io.mango.integration.core.support.IntegrationTestProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

/**
 * 集成应用模块自动装配。
 */
@AutoConfiguration
@ConditionalOnClass(IntegrationConnectorMapper.class)
@MapperScan("io.mango.integration.core.mapper")
@EnableConfigurationProperties(IntegrationTestProperties.class)
@ComponentScan({"io.mango.integration.core.service", "io.mango.integration.starter"})
public class IntegrationAutoConfiguration {
}
