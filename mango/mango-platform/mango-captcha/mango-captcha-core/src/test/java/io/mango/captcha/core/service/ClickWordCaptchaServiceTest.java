package io.mango.captcha.core.service;

import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.core.service.impl.ClickWordCaptchaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 点选文字验证码服务单元测试
 */
class ClickWordCaptchaServiceTest {

    private ClickWordCaptchaService clickWordCaptchaService;

    @BeforeEach
    void setUp() {
        clickWordCaptchaService = new ClickWordCaptchaServiceImpl();
        ReflectionTestUtils.setField(clickWordCaptchaService, "width", 320);
        ReflectionTestUtils.setField(clickWordCaptchaService, "height", 180);
        ReflectionTestUtils.setField(clickWordCaptchaService, "wordCount", 4);
        ReflectionTestUtils.setField(clickWordCaptchaService, "targetCount", 3);
        ReflectionTestUtils.setField(clickWordCaptchaService, "tolerance", 24);
    }

    @Test
    void generate_returnsClickWordCaptcha() {
        CaptchaResponse response = clickWordCaptchaService.generate();

        assertNotNull(response);
        assertEquals(CaptchaType.CLICK_WORD, response.getType());
        assertNotNull(response.getImage());
        assertTrue(response.getImage().startsWith("data:image/png;base64,"));
        assertNotNull(response.getTarget());
        assertEquals(3, response.getTarget().split(",").length);
        assertNotNull(response.getExtra());
        assertTrue(response.getExtra().contains("\"points\""));
    }
}
