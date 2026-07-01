package io.mango.link.starter;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.link", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan(basePackages = "io.mango.link.core.mapper", annotationClass = Mapper.class)
@ComponentScan({
        "io.mango.link.core.service",
        "io.mango.link.starter.controller"
})
public class LinkAutoConfiguration {

    @Bean
    public static LinkPermitPathBeanPostProcessor linkPermitPathBeanPostProcessor() {
        return new LinkPermitPathBeanPostProcessor();
    }
}
