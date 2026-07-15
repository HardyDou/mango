package io.mango.captcha.core.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 点选文字验证码实现
 *
 * @author Mango
 */
@Slf4j
public class DefaultClickWordCaptchaGenerator implements ClickWordCaptchaGenerator {

    private static final String IMAGE_PREFIX = "data:image/png;base64,";
    private static final int DEFAULT_WIDTH = 320;
    private static final int DEFAULT_HEIGHT = 180;
    private static final int DEFAULT_WORD_COUNT = 4;
    private static final int DEFAULT_TARGET_COUNT = 3;
    private static final int DEFAULT_TOLERANCE = 24;
    private static final int BACKGROUND_GRID_STEP = 36;
    private static final int LEFT_OVAL_X = -40;
    private static final int LEFT_OVAL_BOTTOM_OFFSET = 72;
    private static final int LEFT_OVAL_HEIGHT = 118;
    private static final int RIGHT_OVAL_RIGHT_OFFSET = 128;
    private static final int RIGHT_OVAL_Y = -36;
    private static final int RIGHT_OVAL_WIDTH = 178;
    private static final int RIGHT_OVAL_HEIGHT = 112;
    private static final int LEFT_CARD_X = 34;
    private static final int LEFT_CARD_Y = 34;
    private static final int LEFT_CARD_WIDTH = 92;
    private static final int LEFT_CARD_HEIGHT = 48;
    private static final int LEFT_CARD_ARC = 18;
    private static final int RIGHT_CARD_RIGHT_OFFSET = 132;
    private static final int RIGHT_CARD_BOTTOM_OFFSET = 76;
    private static final int RIGHT_CARD_WIDTH = 104;
    private static final int RIGHT_CARD_HEIGHT = 42;
    private static final int RIGHT_CARD_ARC = 16;
    private static final float NOISE_STROKE_WIDTH = 1.2F;
    private static final int NOISE_LINE_COUNT = 24;
    private static final int NOISE_RED_BASE = 120;
    private static final int NOISE_GREEN_BASE = 130;
    private static final int NOISE_BLUE_BASE = 140;
    private static final int NOISE_COLOR_RANGE = 80;
    private static final int NOISE_ALPHA = 58;
    private static final int NOISE_MAX_X_GAP = 54;
    private static final int NOISE_MAX_Y_GAP = 34;
    private static final int FONT_SIZE_BASE = 28;
    private static final int FONT_SIZE_RANGE = 7;
    private static final int ROTATION_RANGE = 31;
    private static final int ROTATION_OFFSET = 15;
    private static final int TEXT_SHADOW_ALPHA = 145;
    private static final int PLACEMENT_ATTEMPTS = 30;
    private static final int WORD_X_OFFSET = 18;
    private static final int WORD_X_MARGIN = 36;
    private static final int WORD_Y_OFFSET = 32;
    private static final int WORD_Y_MARGIN = 64;
    private static final int FALLBACK_X_OFFSET = 24;
    private static final int FALLBACK_HORIZONTAL_MARGIN = 48;
    private static final int FALLBACK_MIN_COLUMN_WIDTH = 36;
    private static final int WORD_BASELINE_OFFSET = 18;
    private static final int WORD_VERTICAL_MARGIN = 36;
    private static final int FALLBACK_Y_STEP = 31;
    private static final int FALLBACK_BOTTOM_MARGIN = 32;
    private static final double MIN_WORD_DISTANCE_FACTOR = 2.2D;
    private static final int FALLBACK_FONT_SIZE = 32;
    private static final int FALLBACK_CLOUD_X = 62;
    private static final int FALLBACK_CLOUD_Y = 78;
    private static final int FALLBACK_MOUNTAIN_X = 150;
    private static final int FALLBACK_MOUNTAIN_Y = 126;
    private static final int FALLBACK_MOON_X = 244;
    private static final int FALLBACK_MOON_Y = 82;
    private static final Color WORD_BLUE = Color.decode("#274f8e");
    private static final Color WORD_RED = Color.decode("#98432f");
    private static final Color WORD_GREEN = Color.decode("#267a57");
    private static final Color WORD_PURPLE = Color.decode("#6f499b");
    private static final Color WORD_ORANGE = Color.decode("#ae671f");
    private static final Color BACKGROUND_COLOR = Color.decode("#f1f6fa");
    private static final Color GRID_COLOR = Color.decode("#d6e5ef");
    private static final Color LEFT_OVAL_COLOR = Color.decode("#d2e2d5");
    private static final Color RIGHT_OVAL_COLOR = Color.decode("#e6d5be");
    private static final Color LEFT_CARD_COLOR = Color.decode("#c3dae8");
    private static final Color RIGHT_CARD_COLOR = Color.decode("#e1dbec");
    private static final String[] WORD_POOL = {
        "云", "山", "月", "河", "竹", "星", "海", "风", "林", "桥",
        "春", "秋", "雨", "雪", "松", "石", "舟", "花", "城", "光"
    };
    private static final Color[] WORD_COLORS = {
        WORD_BLUE,
        WORD_RED,
        WORD_GREEN,
        WORD_PURPLE,
        WORD_ORANGE
    };

    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mango.captcha.click-word.width:320}")
    private int width = DEFAULT_WIDTH;

    @Value("${mango.captcha.click-word.height:180}")
    private int height = DEFAULT_HEIGHT;

    @Value("${mango.captcha.click-word.word-count:4}")
    private int wordCount = DEFAULT_WORD_COUNT;

    @Value("${mango.captcha.click-word.target-count:3}")
    private int targetCount = DEFAULT_TARGET_COUNT;

    @Value("${mango.captcha.click-word.tolerance:24}")
    private int tolerance = DEFAULT_TOLERANCE;

    @Override
    public CaptchaResponse generate() {
        CaptchaResponse response = new CaptchaResponse();
        response.setType(CaptchaType.CLICK_WORD);

        try {
            BufferedImage image = createBackground();
            List<WordPoint> points = drawWords(image);
            List<WordPoint> targets = pickTargets(points);
            response.setImage(IMAGE_PREFIX + toPngBase64(image));
            response.setTarget(joinWords(targets));
            response.setExtra(toAnswerJson(targets));
        } catch (Exception e) {
            log.error("生成点选文字验证码失败", e);
            BufferedImage image = createFallbackImage();
            response.setImage(toSafeImage(image));
            response.setTarget("云,山,月");
            response.setExtra(fallbackAnswerJson());
        }

        return response;
    }

    private BufferedImage createBackground() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(BACKGROUND_COLOR);
            graphics.fillRect(0, 0, width, height);

            graphics.setColor(GRID_COLOR);
            for (int i = 0; i < width; i += BACKGROUND_GRID_STEP) {
                graphics.drawLine(i, 0, i + height, height);
            }

            graphics.setColor(LEFT_OVAL_COLOR);
            graphics.fillOval(LEFT_OVAL_X, height - LEFT_OVAL_BOTTOM_OFFSET, width / 2, LEFT_OVAL_HEIGHT);
            graphics.setColor(RIGHT_OVAL_COLOR);
            graphics.fillOval(width - RIGHT_OVAL_RIGHT_OFFSET, RIGHT_OVAL_Y, RIGHT_OVAL_WIDTH, RIGHT_OVAL_HEIGHT);
            graphics.setColor(LEFT_CARD_COLOR);
            graphics.fillRoundRect(LEFT_CARD_X, LEFT_CARD_Y, LEFT_CARD_WIDTH, LEFT_CARD_HEIGHT,
                    LEFT_CARD_ARC, LEFT_CARD_ARC);
            graphics.setColor(RIGHT_CARD_COLOR);
            graphics.fillRoundRect(width - RIGHT_CARD_RIGHT_OFFSET, height - RIGHT_CARD_BOTTOM_OFFSET,
                    RIGHT_CARD_WIDTH, RIGHT_CARD_HEIGHT, RIGHT_CARD_ARC, RIGHT_CARD_ARC);

            graphics.setStroke(new BasicStroke(NOISE_STROKE_WIDTH));
            for (int i = 0; i < NOISE_LINE_COUNT; i++) {
                graphics.setColor(new Color(
                    NOISE_RED_BASE + random.nextInt(NOISE_COLOR_RANGE),
                    NOISE_GREEN_BASE + random.nextInt(NOISE_COLOR_RANGE),
                    NOISE_BLUE_BASE + random.nextInt(NOISE_COLOR_RANGE),
                    NOISE_ALPHA
                ));
                int x1 = random.nextInt(width);
                int y1 = random.nextInt(height);
                graphics.drawLine(x1, y1, Math.min(width, x1 + random.nextInt(NOISE_MAX_X_GAP)),
                        Math.min(height, y1 + random.nextInt(NOISE_MAX_Y_GAP)));
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private List<WordPoint> drawWords(BufferedImage image) {
        List<String> words = new ArrayList<>(List.of(WORD_POOL));
        Collections.shuffle(words, random);
        int count = Math.max(targetCount, wordCount);
        List<WordPoint> points = new ArrayList<>(count);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < count; i++) {
                String word = words.get(i % words.size());
                int fontSize = FONT_SIZE_BASE + random.nextInt(FONT_SIZE_RANGE);
                Font font = new Font("SansSerif", Font.BOLD, fontSize);
                graphics.setFont(font);
                FontMetrics metrics = graphics.getFontMetrics(font);
                int textWidth = metrics.stringWidth(word);
                int textHeight = metrics.getAscent();
                int x = nextWordX(points, textWidth);
                int y = nextWordY(points, textHeight);
                double rotate = Math.toRadians(random.nextInt(ROTATION_RANGE) - ROTATION_OFFSET);

                AffineTransform oldTransform = graphics.getTransform();
                graphics.rotate(rotate, x + textWidth / 2.0, y - textHeight / 2.0);
                graphics.setColor(new Color(Color.WHITE.getRed(), Color.WHITE.getGreen(), Color.WHITE.getBlue(),
                        TEXT_SHADOW_ALPHA));
                graphics.drawString(word, x + 2, y + 2);
                graphics.setColor(WORD_COLORS[random.nextInt(WORD_COLORS.length)]);
                graphics.drawString(word, x, y);
                graphics.setTransform(oldTransform);
                points.add(new WordPoint(word, x + textWidth / 2, y - textHeight / 2));
            }
        } finally {
            graphics.dispose();
        }
        return points;
    }

    private int nextWordX(List<WordPoint> points, int textWidth) {
        for (int i = 0; i < PLACEMENT_ATTEMPTS; i++) {
            int x = WORD_X_OFFSET + random.nextInt(Math.max(width - textWidth - WORD_X_MARGIN, 1));
            int y = WORD_Y_OFFSET + random.nextInt(Math.max(height - WORD_Y_MARGIN, 1));
            if (isFarEnough(points, x + textWidth / 2, y)) {
                return x;
            }
        }
        return FALLBACK_X_OFFSET + points.size()
                * Math.max((width - FALLBACK_HORIZONTAL_MARGIN) / Math.max(wordCount, 1), FALLBACK_MIN_COLUMN_WIDTH)
                % Math.max(width - textWidth - FALLBACK_X_OFFSET, 1);
    }

    private int nextWordY(List<WordPoint> points, int textHeight) {
        for (int i = 0; i < PLACEMENT_ATTEMPTS; i++) {
            int x = WORD_Y_OFFSET + random.nextInt(Math.max(width - WORD_Y_MARGIN, 1));
            int y = textHeight + WORD_BASELINE_OFFSET
                    + random.nextInt(Math.max(height - textHeight - WORD_VERTICAL_MARGIN, 1));
            if (isFarEnough(points, x, y - textHeight / 2)) {
                return y;
            }
        }
        return textHeight + DEFAULT_TOLERANCE + points.size() * FALLBACK_Y_STEP
                % Math.max(height - textHeight - FALLBACK_BOTTOM_MARGIN, 1);
    }

    private boolean isFarEnough(List<WordPoint> points, int x, int y) {
        for (WordPoint point : points) {
            double distance = Math.hypot(point.x() - x, point.y() - y);
            if (distance < tolerance * MIN_WORD_DISTANCE_FACTOR) {
                return false;
            }
        }
        return true;
    }

    private List<WordPoint> pickTargets(List<WordPoint> points) {
        List<WordPoint> shuffled = new ArrayList<>(points);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, Math.min(targetCount, shuffled.size()));
    }

    private String joinWords(List<WordPoint> targets) {
        return targets.stream().map(WordPoint::word).reduce((left, right) -> left + "," + right).orElse("");
    }

    private String toAnswerJson(List<WordPoint> targets) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("width", width);
        root.put("height", height);
        root.put("tolerance", tolerance);
        ArrayNode array = root.putArray("points");
        for (WordPoint target : targets) {
            ObjectNode node = array.addObject();
            node.put("word", target.word());
            node.put("x", target.x());
            node.put("y", target.y());
        }
        return objectMapper.writeValueAsString(root);
    }

    private BufferedImage createFallbackImage() {
        BufferedImage image = createBackground();
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(WORD_BLUE);
            graphics.setFont(new Font("SansSerif", Font.BOLD, FALLBACK_FONT_SIZE));
            graphics.drawString("云", FALLBACK_CLOUD_X, FALLBACK_CLOUD_Y);
            graphics.drawString("山", FALLBACK_MOUNTAIN_X, FALLBACK_MOUNTAIN_Y);
            graphics.drawString("月", FALLBACK_MOON_X, FALLBACK_MOON_Y);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private String fallbackAnswerJson() {
        return "{\"width\":320,\"height\":180,\"tolerance\":24,\"points\":[{\"word\":\"云\",\"x\":78,\"y\":54},{\"word\":\"山\",\"x\":166,\"y\":102},{\"word\":\"月\",\"x\":260,\"y\":58}]}";
    }

    private String toSafeImage(BufferedImage image) {
        try {
            return IMAGE_PREFIX + toPngBase64(image);
        } catch (IOException e) {
            return null;
        }
    }

    private String toPngBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return java.util.Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private record WordPoint(String word, int x, int y) {
    }
}
