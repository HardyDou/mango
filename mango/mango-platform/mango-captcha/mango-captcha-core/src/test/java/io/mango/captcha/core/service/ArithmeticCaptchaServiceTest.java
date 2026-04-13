package io.mango.captcha.core.service;

import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.core.service.impl.ArithmeticCaptchaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 算术验证码服务单元测试
 */
class ArithmeticCaptchaServiceTest {

    private ArithmeticCaptchaService arithmeticCaptchaService;

    @BeforeEach
    void setUp() {
        arithmeticCaptchaService = new ArithmeticCaptchaServiceImpl();
        // 通过反射设置默认值（因为没有 Spring 上下文）
        ReflectionTestUtils.setField(arithmeticCaptchaService, "defaultTtl", 300L);
        ReflectionTestUtils.setField(arithmeticCaptchaService, "width", 120);
        ReflectionTestUtils.setField(arithmeticCaptchaService, "height", 40);
    }

    @Test
    void generate_returnsArithmeticType() {
        CaptchaResponse response = arithmeticCaptchaService.generate();

        assertNotNull(response);
        assertEquals(CaptchaType.ARITHMETIC, response.getType());
        assertNotNull(response.getImage());
        assertTrue(response.getImage().startsWith("data:image/png;base64,"));
        assertNotNull(response.getExtra());
    }

    @Test
    void generate_imageBase64IsValid() {
        CaptchaResponse response = arithmeticCaptchaService.generate();
        String imageData = response.getImage().replace("data:image/png;base64,", "");

        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(imageData));
    }

    @Test
    void generate_extraContainsAnswer() {
        CaptchaResponse response = arithmeticCaptchaService.generate();

        // extra 应该是一个数字字符串（答案）
        assertNotNull(response.getExtra());
        assertDoesNotThrow(() -> Integer.parseInt(response.getExtra()));
    }

    @Test
    void generate_expireTimeIsSet() {
        CaptchaResponse response = arithmeticCaptchaService.generate();

        assertNotNull(response.getExpireTime());
        assertTrue(response.getExpireTime() > 0);
    }
}
