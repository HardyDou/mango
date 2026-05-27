package {{basePackage}}.{{modulePackage}}.starter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * {{moduleName}}自动配置。
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "{{basePackage}}.{{modulePackage}}")
public class {{modulePascal}}AutoConfiguration {
}
