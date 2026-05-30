package {{basePackage}}.{{modulePackage}}.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * {{moduleName}}自动配置。
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "{{basePackage}}.{{modulePackage}}")
@MapperScan("{{basePackage}}.{{modulePackage}}.core.mapper")
public class {{modulePascal}}AutoConfiguration {
}
