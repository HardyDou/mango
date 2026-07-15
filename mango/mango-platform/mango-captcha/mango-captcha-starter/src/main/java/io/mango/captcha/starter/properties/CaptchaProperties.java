package io.mango.captcha.starter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证码配置属性
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.captcha")
public class CaptchaProperties {

    private static final long DEFAULT_TTL_SECONDS = 300L;
    private static final int DEFAULT_ARITHMETIC_WIDTH = 120;
    private static final int DEFAULT_ARITHMETIC_HEIGHT = 40;
    private static final int DEFAULT_PUZZLE_WIDTH = 280;
    private static final int DEFAULT_PUZZLE_HEIGHT = 160;
    private static final int DEFAULT_SLIDER_SIZE = 50;
    private static final int DEFAULT_CLICK_WIDTH = 320;
    private static final int DEFAULT_CLICK_HEIGHT = 180;
    private static final int DEFAULT_WORD_COUNT = 4;
    private static final int DEFAULT_TARGET_COUNT = 3;
    private static final int DEFAULT_TOLERANCE = 24;
    private static final int DEFAULT_CODE_LENGTH = 6;
    private static final int DEFAULT_SMS_PERIOD_SECONDS = 60;

    /**
     * 兼容保留的存储策略配置；实际存储实现由应用注入的 IKvStore 决定。
     */
    private String storage = "auto";

    /**
     * 验证码有效期（秒）
     */
    private long ttl = DEFAULT_TTL_SECONDS;

    /**
     * 算术验证码配置
     */
    private Arithmetic arithmetic = new Arithmetic();

    /**
     * 滑块验证码配置
     */
    private BlockPuzzle blockPuzzle = new BlockPuzzle();

    /**
     * 点选文字验证码配置
     */
    private ClickWord clickWord = new ClickWord();

    /**
     * 短信验证码配置
     */
    private Sms sms = new Sms();

    /**
     * 邮件验证码配置
     */
    private Email email = new Email();

    @Data
    public static class Arithmetic {
        private boolean enabled = true;
        private int width = DEFAULT_ARITHMETIC_WIDTH;
        private int height = DEFAULT_ARITHMETIC_HEIGHT;
    }

    @Data
    public static class BlockPuzzle {
        private boolean enabled = true;
        private int width = DEFAULT_PUZZLE_WIDTH;
        private int height = DEFAULT_PUZZLE_HEIGHT;
        private int sliderSize = DEFAULT_SLIDER_SIZE;
        /**
         * 滑块验证码图库。支持 classpath:/file:/http(s) 路径。
         * 为空时使用组件内置图库。
         */
        private List<String> imageLocations = new ArrayList<>();
    }

    @Data
    public static class ClickWord {
        private boolean enabled = true;
        private int width = DEFAULT_CLICK_WIDTH;
        private int height = DEFAULT_CLICK_HEIGHT;
        private int wordCount = DEFAULT_WORD_COUNT;
        private int targetCount = DEFAULT_TARGET_COUNT;
        private int tolerance = DEFAULT_TOLERANCE;
    }

    @Data
    public static class Sms {
        private boolean enabled = true;
        private int length = DEFAULT_CODE_LENGTH;
        private int period = DEFAULT_SMS_PERIOD_SECONDS;
        private String provider = "default";
    }

    @Data
    public static class Email {
        private boolean enabled = true;
        private int length = DEFAULT_CODE_LENGTH;
        private String provider = "default";
    }
}
