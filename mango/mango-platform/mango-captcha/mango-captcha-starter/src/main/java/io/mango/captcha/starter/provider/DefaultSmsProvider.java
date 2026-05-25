package io.mango.captcha.starter.provider;

import io.mango.captcha.api.spi.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 默认短信供应商。
 * 未配置真实短信供应商时拒绝发送，避免验证码链路误判为发送成功。
 *
 * @author Mango
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mango.captcha.sms.provider", havingValue = "default")
public class DefaultSmsProvider implements SmsProvider {

    @Override
    public boolean send(String mobile, String templateCode, String... params) {
        log.warn("SMS provider is not configured, captcha SMS was not sent: mobile={}, template={}", mobile, templateCode);
        return false;
    }

    @Override
    public String getName() {
        return "default";
    }
}
