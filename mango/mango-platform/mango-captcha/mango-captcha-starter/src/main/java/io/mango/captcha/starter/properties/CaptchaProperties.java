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
    private static final boolean DEFAULT_SMS_FIXED_CODE_ENABLED = false;
    private static final String DEFAULT_SMS_PROVIDER = "default";
    private static final String DEFAULT_ALIYUN_ENDPOINT = "dysmsapi.aliyuncs.com";
    private static final String DEFAULT_ALIYUN_REGION = "cn-hangzhou";
    private static final String DEFAULT_TENCENT_ENDPOINT = "sms.tencentcloudapi.com";
    private static final String DEFAULT_TENCENT_REGION = "ap-guangzhou";
    private static final String DEFAULT_TENCENT_COUNTRY_CODE = "+86";

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
        /**
         * 短信供应商：{@code default}（空实现，发送明确失败）、{@code aliyun}、{@code tencent}。
         * 取值不匹配任何内置供应商时不会注册 {@code SmsProvider}，发送同样明确失败。
         */
        private String provider = DEFAULT_SMS_PROVIDER;
        /**
         * 第三方短信模板编码；{@code provider=aliyun|tencent} 时必填，由 {@code CaptchaService} 传给供应商。
         */
        private String templateCode;
        /**
         * 是否启用固定短信验证码；仅适用于开发和测试环境。
         */
        private boolean fixedCodeEnabled = DEFAULT_SMS_FIXED_CODE_ENABLED;
        /**
         * 固定短信验证码值。启用固定验证码时必须为纯数字。
         */
        private String fixedCode;
        /**
         * 阿里云短信账号配置；凭证只允许通过部署环境的 {@code ${ENV}} 占位符注入。
         */
        private Aliyun aliyun = new Aliyun();
        /**
         * 腾讯云短信账号配置；凭证只允许通过部署环境的 {@code ${ENV}} 占位符注入。
         */
        private Tencent tencent = new Tencent();
    }

    @Data
    public static class Aliyun {
        private String accessKeyId;
        private String accessKeySecret;
        private String signName;
        private String endpoint = DEFAULT_ALIYUN_ENDPOINT;
        private String region = DEFAULT_ALIYUN_REGION;
    }

    @Data
    public static class Tencent {
        private String secretId;
        private String secretKey;
        private String signName;
        private String sdkAppId;
        private String region = DEFAULT_TENCENT_REGION;
        private String endpoint = DEFAULT_TENCENT_ENDPOINT;
        private String countryCode = DEFAULT_TENCENT_COUNTRY_CODE;
    }

    @Data
    public static class Email {
        private boolean enabled = true;
        private int length = DEFAULT_CODE_LENGTH;
        private String provider = "default";
    }
}
