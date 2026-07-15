package io.mango.captcha.core.generator;

import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.constant.CaptchaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

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
public class DefaultBlockPuzzleCaptchaGenerator implements BlockPuzzleCaptchaGenerator {

    private static final int DEFAULT_WIDTH = 280;
    private static final int DEFAULT_HEIGHT = 160;
    private static final int DEFAULT_SLIDER_SIZE = 50;
    private static final int X_RANDOM_MARGIN = 30;
    private static final int X_OFFSET = 24;
    private static final int Y_RANDOM_MARGIN = 24;
    private static final int Y_OFFSET = 12;
    private static final int KNOB_DIVISOR = 5;
    private static final int MIN_KNOB_SIZE = 8;
    private static final int RADIUS_DIVISOR = 8;
    private static final int MIN_RADIUS = 5;
    private static final int SHAPE_BORDER = 4;
    private static final float HOLE_ALPHA = 0.58F;
    private static final int SLIDER_SHADOW_ALPHA = 55;
    private static final int SLIDER_BORDER_ALPHA = 190;
    private static final int BORDER_ALPHA = 150;
    private static final int FALLBACK_DECORATION_ALPHA = 90;
    private static final int FALLBACK_LEFT = 28;
    private static final int FALLBACK_TOP = 30;
    private static final int FALLBACK_HORIZONTAL_MARGIN = 56;
    private static final int FALLBACK_VERTICAL_MARGIN = 60;
    private static final int FALLBACK_ARC = 18;
    private static final int FALLBACK_LINE_STEP = 28;
    private static final Color FALLBACK_BACKGROUND = Color.decode("#ebf1f7");
    private static final Color FALLBACK_PRIMARY = Color.decode("#409eff");

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
    private static final String URI_SCHEME_SEPARATOR = ":";

    private final Random random = new Random();
    private final ResourceLoader resourceLoader;
    private final List<String> imageLocations;

    @Value("${mango.captcha.block-puzzle.width:280}")
    private int width = DEFAULT_WIDTH;

    @Value("${mango.captcha.block-puzzle.height:160}")
    private int height = DEFAULT_HEIGHT;

    @Value("${mango.captcha.block-puzzle.slider-size:50}")
    private int sliderSize = DEFAULT_SLIDER_SIZE;

    public DefaultBlockPuzzleCaptchaGenerator() {
        this(Collections.emptyList());
    }

    public DefaultBlockPuzzleCaptchaGenerator(List<String> imageLocations) {
        this(new DefaultResourceLoader(), imageLocations);
    }

    DefaultBlockPuzzleCaptchaGenerator(ResourceLoader resourceLoader, List<String> imageLocations) {
        this.resourceLoader = resourceLoader;
        this.imageLocations = normalizeImageLocations(imageLocations);
    }

    @Override
    public CaptchaResponse generate() {
        CaptchaResponse response = new CaptchaResponse();
        response.setType(CaptchaType.BLOCK_PUZZLE);
        applyDimensionMetadata(response);

        try {
            BufferedImage source = loadRandomBackground();
            BufferedImage background = resize(source, width, height);
            int slipX = random.nextInt(width - sliderSize - X_RANDOM_MARGIN) + X_OFFSET;
            int slipY = random.nextInt(height - sliderSize - Y_RANDOM_MARGIN) + Y_OFFSET;
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

    private void applyDimensionMetadata(CaptchaResponse response) {
        response.setBackgroundWidth(width);
        response.setBackgroundHeight(height);
        response.setSliderSize(sliderSize);
    }

    private BufferedImage loadRandomBackground() throws IOException {
        List<String> locations = imageLocations;
        if (locations.isEmpty()) {
            locations = defaultImageLocations();
        }
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
                .map(DefaultBlockPuzzleCaptchaGenerator::normalizeImageLocation)
                .toList();
    }

    private static String normalizeImageLocation(String location) {
        if (location.contains(URI_SCHEME_SEPARATOR)) {
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
        int knob = Math.max(sliderSize / KNOB_DIVISOR, MIN_KNOB_SIZE);
        int radius = Math.max(sliderSize / RADIUS_DIVISOR, MIN_RADIUS);
        Area area = new Area(new RoundRectangle2D.Double(x + 2, y + 2,
                sliderSize - SHAPE_BORDER, sliderSize - SHAPE_BORDER, radius, radius));
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
            graphics.setColor(new Color(Color.WHITE.getRed(), Color.WHITE.getGreen(), Color.WHITE.getBlue(),
                    SLIDER_BORDER_ALPHA));
            graphics.draw(puzzleShape);
            graphics.setColor(new Color(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK.getBlue(), SLIDER_SHADOW_ALPHA));
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
            graphics.setComposite(AlphaComposite.SrcOver.derive(HOLE_ALPHA));
            graphics.setColor(Color.BLACK);
            graphics.fill(puzzleShape);
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setColor(new Color(Color.WHITE.getRed(), Color.WHITE.getGreen(), Color.WHITE.getBlue(), BORDER_ALPHA));
            graphics.draw(puzzleShape);
        } finally {
            graphics.dispose();
        }
    }

    private BufferedImage createFallbackBackground() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(FALLBACK_BACKGROUND);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(FALLBACK_PRIMARY);
            graphics.fillRoundRect(FALLBACK_LEFT, FALLBACK_TOP, width - FALLBACK_HORIZONTAL_MARGIN,
                    height - FALLBACK_VERTICAL_MARGIN, FALLBACK_ARC, FALLBACK_ARC);
            graphics.setColor(new Color(Color.WHITE.getRed(), Color.WHITE.getGreen(), Color.WHITE.getBlue(),
                    FALLBACK_DECORATION_ALPHA));
            for (int i = 0; i < width; i += FALLBACK_LINE_STEP) {
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
