package io.mango.information.starter;

import io.mango.information.core.mapper.InformationFormMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/** 信息采集自动配置。 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mango.information", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.information.core.mapper")
@ComponentScan({"io.mango.information.core.service", "io.mango.information.starter.controller"})
public class InformationAutoConfiguration {
}
