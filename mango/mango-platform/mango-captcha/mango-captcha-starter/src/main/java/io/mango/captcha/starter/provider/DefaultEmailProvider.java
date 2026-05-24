package io.mango.captcha.starter.provider;

import io.mango.captcha.api.spi.EmailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 默认邮件供应商。
 * 未配置真实邮件供应商时拒绝发送，避免验证码链路误判为发送成功。
 *
 * @author Mango
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mango.captcha.email.provider", havingValue = "default")
public class DefaultEmailProvider implements EmailProvider {

    @Override
    public boolean send(String to, String subject, String content) {
        log.warn("Email provider is not configured, captcha email was not sent: to={}, subject={}", to, subject);
        return false;
    }

    @Override
    public String getName() {
        return "default";
    }
}
