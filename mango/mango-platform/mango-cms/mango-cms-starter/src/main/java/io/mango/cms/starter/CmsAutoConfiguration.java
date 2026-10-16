package io.mango.cms.starter;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(prefix = "mango.cms", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan(basePackages = "io.mango.cms.core.mapper", annotationClass = Mapper.class)
@ComponentScan({
    "io.mango.cms.core.service",
    "io.mango.cms.starter.controller",
    "io.mango.cms.starter.endpoint",
    "io.mango.cms.starter.resource"
})
public class CmsAutoConfiguration {

    @Bean
    public static CmsPermitPathBeanPostProcessor cmsPermitPathBeanPostProcessor() {
        return new CmsPermitPathBeanPostProcessor();
    }

}
