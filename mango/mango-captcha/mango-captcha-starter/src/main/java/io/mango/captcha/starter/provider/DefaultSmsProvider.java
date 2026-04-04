package io.mango.captcha.starter.provider;

import io.mango.captcha.api.spi.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 默认短信供应商
 * 仅打印日志，实际使用需配置第三方短信服务（如阿里云、腾讯云等）
 *
 * @author Mango
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mango.captcha.sms.provider", havingValue = "default", matchIfMissing = true)
public class DefaultSmsProvider implements SmsProvider {

    @Override
    public boolean send(String mobile, String templateCode, String... params) {
        if (params.length > 0) {
            log.info("【模拟短信发送】手机号: {}, 验证码: {}, 模板: {}", mobile, params[0], templateCode);
        } else {
            log.info("【模拟短信发送】手机号: {}, 模板: {}", mobile, templateCode);
        }
        // 实际生产环境应调用第三方短信API
        return true;
    }

    @Override
    public String getName() {
        return "default";
    }
}
