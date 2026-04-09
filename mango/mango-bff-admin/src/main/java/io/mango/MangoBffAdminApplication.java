package io.mango;

import io.mango.ai.starter.MangoAiAutoConfiguration;
import io.mango.area.starter.MangoAreaAutoConfiguration;
import io.mango.captcha.starter.config.CaptchaAutoConfiguration;
import io.mango.i18n.starter.I18nAutoConfiguration;
import io.mango.org.starter.MangoOrgAutoConfiguration;
import io.mango.rbac.starter.RbacAutoConfiguration;
import io.mango.user.starter.UserAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Mango BFF Admin Application
 * Entry point for the BFF layer that aggregates user, permission, i18n services
 *
 * @author Mango
 */
@SpringBootApplication(exclude = {
        // 排除Spring Security自动配置，使用AuthAutoConfiguration中的AuthSecurityConfig
        SecurityAutoConfiguration.class,
        // 排除actuator的安全自动配置，避免与AuthSecurityConfig冲突
        org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class
})
@Import({
        // AuthAutoConfiguration excluded - auth controllers need Feign clients that don't work in BFF monolith
        // AuthSecurityConfig is loaded via @ComponentScan from io.mango package
        MangoOrgAutoConfiguration.class,
        MangoAreaAutoConfiguration.class,
        MangoAiAutoConfiguration.class,
        I18nAutoConfiguration.class,
        UserAutoConfiguration.class,
        RbacAutoConfiguration.class,
        CaptchaAutoConfiguration.class
})
@ComponentScan(basePackages = {"io.mango"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io\\.mango\\.auth\\.starter\\.controller\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io\\.mango\\.auth\\.core\\.init\\..*")
        })
@MapperScan({
        "io.mango.user.core.mapper",
        "io.mango.rbac.core.mapper",
        "io.mango.i18n.core.mapper",
        "io.mango.org.core.mapper",
        "io.mango.area.core.mapper"
})
public class MangoBffAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoBffAdminApplication.class, args);
    }
}
