package io.mango.captcha.starter.provider;

import io.mango.captcha.api.spi.SmsProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认短信供应商。
 * 未配置 {@code mango.captcha.sms.provider}（或显式配置为 {@code default}）时由自动配置注册，拒绝发送，
 * 避免验证码链路误判为发送成功。
 *
 * @author Mango
 */
@Slf4j
public class DefaultSmsProvider implements SmsProvider {

    @Override
    public boolean send(String mobile, String templateCode, String... params) {
        log.warn("SMS provider is not configured, captcha SMS was not sent: template={}", templateCode);
        return false;
    }

    @Override
    public String getName() {
        return "default";
    }
}
