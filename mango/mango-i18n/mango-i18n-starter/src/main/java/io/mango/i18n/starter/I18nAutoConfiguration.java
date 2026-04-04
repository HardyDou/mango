package io.mango.i18n.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * I18n service auto configuration
 *
 * @author Mango
 */
@Configuration
@MapperScan("io.mango.i18n.core.mapper")
@ComponentScan({
        "io.mango.i18n.core.service",
        "io.mango.i18n.core.service.impl",
        "io.mango.i18n.starter.controller"
})
public class I18nAutoConfiguration {
}
