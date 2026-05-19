package io.mango.captcha.core.service;

import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.core.service.impl.BlockPuzzleCaptchaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

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

    @Test
    void generate_usesConfiguredImageGallery(@TempDir Path tempDir) throws Exception {
        File galleryImage = tempDir.resolve("captcha-gallery.png").toFile();
        BufferedImage image = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(36, 116, 94));
            graphics.fillRect(0, 0, 320, 180);
            graphics.setColor(new Color(242, 198, 87));
            graphics.fillOval(212, 28, 48, 48);
            graphics.setColor(new Color(82, 154, 204));
            graphics.fillRoundRect(42, 72, 118, 70, 12, 12);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", galleryImage);

        BlockPuzzleCaptchaServiceImpl service = new BlockPuzzleCaptchaServiceImpl(
                List.of(galleryImage.toURI().toString())
        );
        ReflectionTestUtils.setField(service, "width", 280);
        ReflectionTestUtils.setField(service, "height", 160);
        ReflectionTestUtils.setField(service, "sliderSize", 50);

        CaptchaResponse response = service.generate();

        assertEquals(CaptchaType.BLOCK_PUZZLE, response.getType());
        assertTrue(response.getBackgroundImage().startsWith("data:image/png;base64,"));
        assertTrue(response.getSliderImage().startsWith("data:image/png;base64,"));
        assertNotNull(response.getX());
        assertNotNull(response.getY());
    }
}
