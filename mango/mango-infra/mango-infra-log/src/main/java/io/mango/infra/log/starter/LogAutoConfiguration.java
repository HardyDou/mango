package io.mango.infra.log.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Log 自动配置。
 * 日志模块只提供日志属性、格式模板和审计注解能力；请求上下文与 MDC 写入由 web/context 模块负责。
 */
@AutoConfiguration
@EnableConfigurationProperties(LogProperties.class)
public class LogAutoConfiguration {
}
