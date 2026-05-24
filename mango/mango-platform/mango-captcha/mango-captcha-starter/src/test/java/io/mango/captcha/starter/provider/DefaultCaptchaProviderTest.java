package io.mango.captcha.starter.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DefaultCaptchaProviderTest {

    @Test
    void defaultSmsProvider_send_returnsFalse() {
        DefaultSmsProvider provider = new DefaultSmsProvider();

        boolean sent = provider.send("13800138000", "LOGIN", "123456");

        assertFalse(sent);
    }

    @Test
    void defaultEmailProvider_send_returnsFalse() {
        DefaultEmailProvider provider = new DefaultEmailProvider();

        boolean sent = provider.send("test@example.com", "验证码", "123456");

        assertFalse(sent);
    }
}
