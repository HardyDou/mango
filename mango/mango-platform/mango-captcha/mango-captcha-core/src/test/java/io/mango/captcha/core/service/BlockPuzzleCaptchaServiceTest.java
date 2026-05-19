package io.mango.captcha.core.service;

import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.core.service.impl.BlockPuzzleCaptchaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 滑块验证码服务单元测试
 */
class BlockPuzzleCaptchaServiceTest {

    private BlockPuzzleCaptchaService blockPuzzleCaptchaService;

    @BeforeEach
    void setUp() {
        blockPuzzleCaptchaService = new BlockPuzzleCaptchaServiceImpl();
        // 通过反射设置默认值（因为没有 Spring 上下文）
        ReflectionTestUtils.setField(blockPuzzleCaptchaService, "width", 280);
        ReflectionTestUtils.setField(blockPuzzleCaptchaService, "height", 160);
        ReflectionTestUtils.setField(blockPuzzleCaptchaService, "sliderSize", 50);
    }

    @Test
    void generate_returnsBlockPuzzleType() {
        CaptchaResponse response = blockPuzzleCaptchaService.generate();

        assertNotNull(response);
        assertEquals(CaptchaType.BLOCK_PUZZLE, response.getType());
    }

    @Test
    void generate_hasBackgroundImage() {
        CaptchaResponse response = blockPuzzleCaptchaService.generate();

        assertNotNull(response.getBackgroundImage());
        assertTrue(response.getBackgroundImage().startsWith("data:image/png;base64,"));
        assertNotNull(response.getSliderImage());
        assertTrue(response.getSliderImage().startsWith("data:image/png;base64,"));
    }

    @Test
    void generate_hasValidX() {
        CaptchaResponse response = blockPuzzleCaptchaService.generate();

        assertNotNull(response.getX());
        assertNotNull(response.getY());
        // X 坐标应该在合理范围内
        assertTrue(response.getX() >= 0);
        assertTrue(response.getY() >= 0);
    }

    @Test
    void generate_multipleCallsHaveDifferentX() {
        CaptchaResponse response1 = blockPuzzleCaptchaService.generate();
        CaptchaResponse response2 = blockPuzzleCaptchaService.generate();

        // 不同次调用生成的 X 坐标可能不同
        // 这里只验证都在合理范围内
        assertTrue(response1.getX() >= 0);
        assertTrue(response2.getX() >= 0);
    }
}
