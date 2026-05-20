package io.mango.captcha.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.core.service.ClickWordCaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
@Service
public class ClickWordCaptchaServiceImpl implements ClickWordCaptchaService {

    private static final String IMAGE_PREFIX = "data:image/png;base64,";
    private static final String[] WORD_POOL = {
        "云", "山", "月", "河", "竹", "星", "海", "风", "林", "桥",
        "春", "秋", "雨", "雪", "松", "石", "舟", "花", "城", "光"
    };
    private static final Color[] WORD_COLORS = {
        new Color(39, 79, 142),
        new Color(152, 67, 47),
        new Color(38, 122, 87),
        new Color(111, 73, 155),
        new Color(174, 103, 31)
    };

    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mango.captcha.click-word.width:320}")
    private int width = 320;

    @Value("${mango.captcha.click-word.height:180}")
    private int height = 180;

    @Value("${mango.captcha.click-word.word-count:4}")
    private int wordCount = 4;

    @Value("${mango.captcha.click-word.target-count:3}")
    private int targetCount = 3;

    @Value("${mango.captcha.click-word.tolerance:24}")
    private int tolerance = 24;

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
            graphics.setColor(new Color(241, 246, 250));
            graphics.fillRect(0, 0, width, height);

            graphics.setColor(new Color(214, 229, 239));
            for (int i = 0; i < width; i += 36) {
                graphics.drawLine(i, 0, i + height, height);
            }

            graphics.setColor(new Color(210, 226, 213));
            graphics.fillOval(-40, height - 72, width / 2, 118);
            graphics.setColor(new Color(230, 213, 190));
            graphics.fillOval(width - 128, -36, 178, 112);
            graphics.setColor(new Color(195, 218, 232));
            graphics.fillRoundRect(34, 34, 92, 48, 18, 18);
            graphics.setColor(new Color(225, 219, 236));
            graphics.fillRoundRect(width - 132, height - 76, 104, 42, 16, 16);

            graphics.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 24; i++) {
                graphics.setColor(new Color(
                    120 + random.nextInt(80),
                    130 + random.nextInt(80),
                    140 + random.nextInt(80),
                    58
                ));
                int x1 = random.nextInt(width);
                int y1 = random.nextInt(height);
                graphics.drawLine(x1, y1, Math.min(width, x1 + random.nextInt(54)), Math.min(height, y1 + random.nextInt(34)));
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
                int fontSize = 28 + random.nextInt(7);
                Font font = new Font("SansSerif", Font.BOLD, fontSize);
                graphics.setFont(font);
                FontMetrics metrics = graphics.getFontMetrics(font);
                int textWidth = metrics.stringWidth(word);
                int textHeight = metrics.getAscent();
                int x = nextWordX(points, textWidth);
                int y = nextWordY(points, textHeight);
                double rotate = Math.toRadians(random.nextInt(31) - 15);

                AffineTransform oldTransform = graphics.getTransform();
                graphics.rotate(rotate, x + textWidth / 2.0, y - textHeight / 2.0);
                graphics.setColor(new Color(255, 255, 255, 145));
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
        for (int i = 0; i < 30; i++) {
            int x = 18 + random.nextInt(Math.max(width - textWidth - 36, 1));
            int y = 32 + random.nextInt(Math.max(height - 64, 1));
            if (isFarEnough(points, x + textWidth / 2, y)) {
                return x;
            }
        }
        return 24 + points.size() * Math.max((width - 48) / Math.max(wordCount, 1), 36) % Math.max(width - textWidth - 24, 1);
    }

    private int nextWordY(List<WordPoint> points, int textHeight) {
        for (int i = 0; i < 30; i++) {
            int x = 32 + random.nextInt(Math.max(width - 64, 1));
            int y = textHeight + 18 + random.nextInt(Math.max(height - textHeight - 36, 1));
            if (isFarEnough(points, x, y - textHeight / 2)) {
                return y;
            }
        }
        return textHeight + 24 + points.size() * 31 % Math.max(height - textHeight - 32, 1);
    }

    private boolean isFarEnough(List<WordPoint> points, int x, int y) {
        for (WordPoint point : points) {
            double distance = Math.hypot(point.x() - x, point.y() - y);
            if (distance < tolerance * 2.2) {
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
            graphics.setColor(new Color(39, 79, 142));
            graphics.setFont(new Font("SansSerif", Font.BOLD, 32));
            graphics.drawString("云", 62, 78);
            graphics.drawString("山", 150, 126);
            graphics.drawString("月", 244, 82);
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
