package io.mango.captcha.starter.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 验证码远程调用自动配置。
 */
@AutoConfiguration
@EnableFeignClients(basePackageClasses = CaptchaFeignClient.class)
public class CaptchaRemoteAutoConfiguration {
}
