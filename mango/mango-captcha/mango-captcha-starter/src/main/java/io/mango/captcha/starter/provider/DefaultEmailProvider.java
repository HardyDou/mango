package io.mango.captcha.starter.provider;

import io.mango.captcha.api.spi.EmailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 默认邮件供应商
 * 仅打印日志，实际使用需配置SMTP服务
 *
 * @author Mango
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mango.captcha.email.provider", havingValue = "default", matchIfMissing = true)
public class DefaultEmailProvider implements EmailProvider {

    @Override
    public boolean send(String to, String subject, String content) {
        log.info("【模拟邮件发送】收件人: {}, 主题: {}, 内容: {}", to, subject, content);
        // 实际生产环境应调用SMTP服务
        return true;
    }

    @Override
    public String getName() {
        return "default";
    }
}
