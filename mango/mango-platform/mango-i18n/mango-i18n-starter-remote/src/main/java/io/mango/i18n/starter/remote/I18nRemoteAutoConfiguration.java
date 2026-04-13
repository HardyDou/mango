package io.mango.i18n.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * I18n remote auto configuration - enables Feign clients for i18n service
 *
 * @author Mango
 */
@Configuration
@EnableFeignClients(basePackages = "io.mango.i18n.starter.remote")
public class I18nRemoteAutoConfiguration {
}
