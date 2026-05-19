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

    /**
     * 存储策略: redis, db, memory
     * 默认自动检测: 有redis用redis，否则用memory
     */
    private String storage = "auto";

    /**
     * 验证码有效期（秒）
     */
    private long ttl = 300;

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
        private int width = 120;
        private int height = 40;
    }

    @Data
    public static class BlockPuzzle {
        private boolean enabled = true;
        private int width = 280;
        private int height = 160;
        private int sliderSize = 50;
        /**
         * 滑块验证码图库。支持 classpath:/file:/http(s) 路径。
         * 为空时使用组件内置图库。
         */
        private List<String> imageLocations = new ArrayList<>();
    }

    @Data
    public static class ClickWord {
        private boolean enabled = true;
        private int width = 320;
        private int height = 180;
        private int wordCount = 4;
        private int targetCount = 3;
        private int tolerance = 24;
    }

    @Data
    public static class Sms {
        private boolean enabled = true;
        private int length = 6;
        private int period = 60;
        private String provider = "default";
    }

    @Data
    public static class Email {
        private boolean enabled = true;
        private int length = 6;
        private String provider = "default";
    }
}
