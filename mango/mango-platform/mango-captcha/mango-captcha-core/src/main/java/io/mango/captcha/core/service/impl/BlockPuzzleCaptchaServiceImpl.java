package io.mango.captcha.core.service.impl;

import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.core.service.BlockPuzzleCaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 滑块验证码实现
 *
 * @author Mango
 */
@Slf4j
@Service
public class BlockPuzzleCaptchaServiceImpl implements BlockPuzzleCaptchaService {

    private static final String[] BACKGROUND_IMAGES = {
        "classpath:captcha/block-puzzle/workspace.jpg",
        "classpath:captcha/block-puzzle/city.jpg",
        "classpath:captcha/block-puzzle/garden.jpg",
        "classpath:captcha/block-puzzle/pears.jpg",
        "classpath:captcha/block-puzzle/village.jpg",
        "classpath:captcha/block-puzzle/mountain.jpg",
        "classpath:captcha/block-puzzle/courtyard.jpg"
    };

    private static final String IMAGE_PREFIX = "data:image/png;base64,";

    private final Random random = new Random();
    private final ResourceLoader resourceLoader;
    private final List<String> imageLocations;

    public BlockPuzzleCaptchaServiceImpl() {
        this(Collections.emptyList());
    }

    public BlockPuzzleCaptchaServiceImpl(List<String> imageLocations) {
        this(new DefaultResourceLoader(), imageLocations);
    }

    BlockPuzzleCaptchaServiceImpl(ResourceLoader resourceLoader, List<String> imageLocations) {
        this.resourceLoader = resourceLoader;
        this.imageLocations = normalizeImageLocations(imageLocations);
    }

    @Value("${mango.captcha.block-puzzle.width:280}")
    private int width = 280;

    @Value("${mango.captcha.block-puzzle.height:160}")
    private int height = 160;

    @Value("${mango.captcha.block-puzzle.slider-size:50}")
    private int sliderSize = 50;

    @Override
    public CaptchaResponse generate() {
        CaptchaResponse response = new CaptchaResponse();
        response.setType(CaptchaType.BLOCK_PUZZLE);

        try {
            BufferedImage source = loadRandomBackground();
            BufferedImage background = resize(source, width, height);
            int slipX = random.nextInt(width - sliderSize - 30) + 24;
            int slipY = random.nextInt(height - sliderSize - 24) + 12;
            Area puzzleShape = createPuzzleShape(0, 0);

            BufferedImage slider = cutSliderImage(background, puzzleShape, slipX, slipY);
            drawPuzzleHole(background, puzzleShape, slipX, slipY);

            response.setBackgroundImage(IMAGE_PREFIX + toPngBase64(background));
            response.setSliderImage(IMAGE_PREFIX + toPngBase64(slider));
            response.setX(slipX);
            response.setY(slipY);

            log.debug("生成滑块验证码: slipX={}, slipY={}", slipX, slipY);

        } catch (Exception e) {
            log.error("生成滑块验证码失败", e);
            BufferedImage background = createFallbackBackground();
            int slipX = width / 2;
            int slipY = Math.max((height - sliderSize) / 2, 0);
            Area puzzleShape = createPuzzleShape(0, 0);
            try {
                BufferedImage slider = cutSliderImage(background, puzzleShape, slipX, slipY);
                drawPuzzleHole(background, puzzleShape, slipX, slipY);
                response.setBackgroundImage(IMAGE_PREFIX + toPngBase64(background));
                response.setSliderImage(IMAGE_PREFIX + toPngBase64(slider));
            } catch (IOException ioException) {
                log.error("生成滑块验证码降级图失败", ioException);
            }
            response.setX(width / 2);
            response.setY(slipY);
        }

        return response;
    }

    private BufferedImage loadRandomBackground() throws IOException {
        List<String> locations = imageLocations.isEmpty() ? defaultImageLocations() : imageLocations;
        List<String> candidates = new ArrayList<>(locations);
        Collections.shuffle(candidates, random);
        IOException lastException = null;

        for (String location : candidates) {
            try {
                Resource resource = resourceLoader.getResource(location);
                BufferedImage image = ImageIO.read(resource.getInputStream());
                if (image != null) {
                    return image;
                }
                lastException = new IOException("Unsupported captcha image format: " + location);
            } catch (IOException e) {
                lastException = e;
                log.warn("加载滑块验证码图库图片失败: location={}", location, e);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new IOException("No captcha block puzzle image available");
    }

    private static List<String> defaultImageLocations() {
        return List.of(BACKGROUND_IMAGES);
    }

    private static List<String> normalizeImageLocations(List<String> locations) {
        if (locations == null || locations.isEmpty()) {
            return Collections.emptyList();
        }
        return locations.stream()
                .filter(location -> location != null && !location.isBlank())
                .map(String::trim)
                .map(BlockPuzzleCaptchaServiceImpl::normalizeImageLocation)
                .toList();
    }

    private static String normalizeImageLocation(String location) {
        if (location.contains(":")) {
            return location;
        }
        return "classpath:" + location;
    }

    private BufferedImage resize(BufferedImage source, int targetWidth, int targetHeight) {
        BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private Area createPuzzleShape(int x, int y) {
        int knob = Math.max(sliderSize / 5, 8);
        int radius = Math.max(sliderSize / 8, 5);
        Area area = new Area(new RoundRectangle2D.Double(x + 2, y + 2, sliderSize - 4, sliderSize - 4, radius, radius));
        area.add(new Area(new Ellipse2D.Double(x + sliderSize / 2.0 - knob / 2.0, y - knob / 2.0, knob, knob)));
        area.subtract(new Area(new Ellipse2D.Double(x - knob / 2.0, y + sliderSize / 2.0 - knob / 2.0, knob, knob)));
        return area;
    }

    private BufferedImage cutSliderImage(BufferedImage background, Area puzzleShape, int x, int y) {
        BufferedImage slider = new BufferedImage(sliderSize, sliderSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = slider.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setClip(puzzleShape);
            graphics.drawImage(background, -x, -y, null);
            graphics.setClip(null);
            graphics.setColor(new Color(255, 255, 255, 190));
            graphics.draw(puzzleShape);
            graphics.setColor(new Color(0, 0, 0, 55));
            graphics.draw(createPuzzleShape(1, 1));
        } finally {
            graphics.dispose();
        }
        return slider;
    }

    private void drawPuzzleHole(BufferedImage background, Area puzzleShape, int x, int y) {
        Graphics2D graphics = background.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.translate(x, y);
            graphics.setComposite(AlphaComposite.SrcOver.derive(0.58f));
            graphics.setColor(Color.BLACK);
            graphics.fill(puzzleShape);
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setColor(new Color(255, 255, 255, 150));
            graphics.draw(puzzleShape);
        } finally {
            graphics.dispose();
        }
    }

    private BufferedImage createFallbackBackground() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(235, 241, 247));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(new Color(64, 158, 255));
            graphics.fillRoundRect(28, 30, width - 56, height - 60, 18, 18);
            graphics.setColor(new Color(255, 255, 255, 90));
            for (int i = 0; i < width; i += 28) {
                graphics.drawLine(i, 0, i + height, height);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private String toPngBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return java.util.Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}
