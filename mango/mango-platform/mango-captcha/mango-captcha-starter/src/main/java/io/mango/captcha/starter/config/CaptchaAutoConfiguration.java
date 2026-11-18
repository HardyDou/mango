package io.mango.captcha.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.spi.EmailProvider;
import io.mango.captcha.api.spi.SmsProvider;
import io.mango.captcha.core.generator.ArithmeticCaptchaGenerator;
import io.mango.captcha.core.generator.BehaviorCaptchaEngine;
import io.mango.captcha.core.generator.BlockPuzzleCaptchaGenerator;
import io.mango.captcha.core.generator.ClickWordCaptchaGenerator;
import io.mango.captcha.core.generator.DefaultArithmeticCaptchaGenerator;
import io.mango.captcha.core.generator.DefaultBehaviorCaptchaEngine;
import io.mango.captcha.core.generator.DefaultBlockPuzzleCaptchaGenerator;
import io.mango.captcha.core.generator.DefaultClickWordCaptchaGenerator;
import io.mango.captcha.core.generator.DefaultGraphicCaptchaGenerator;
import io.mango.captcha.core.generator.GraphicCaptchaGenerator;
import io.mango.captcha.core.properties.GraphicCaptchaProperties;
import io.mango.captcha.core.service.impl.CaptchaService;
import io.mango.captcha.starter.controller.CaptchaController;
import io.mango.captcha.starter.properties.CaptchaProperties;
import io.mango.captcha.starter.provider.AliyunSmsProvider;
import io.mango.captcha.starter.provider.DefaultEmailProvider;
import io.mango.captcha.starter.provider.DefaultSmsProvider;
import io.mango.captcha.starter.provider.TencentSmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;


/**
 * 验证码自动配置
 *
 * @author Mango
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(CaptchaProperties.class)
@RequiredArgsConstructor
@Import({CaptchaController.class, CaptchaService.class})
public class CaptchaAutoConfiguration {

    private final CaptchaProperties properties;

    @Bean
    @ConditionalOnMissingBean(ArithmeticCaptchaGenerator.class)
    public ArithmeticCaptchaGenerator arithmeticCaptchaService() {
        return new DefaultArithmeticCaptchaGenerator();
    }

    @Bean
    @ConfigurationProperties(prefix = "mango.captcha.graphic")
    @ConditionalOnMissingBean(GraphicCaptchaProperties.class)
    public GraphicCaptchaProperties graphicCaptchaProperties() {
        return new GraphicCaptchaProperties();
    }

    @Bean
    @ConditionalOnMissingBean(GraphicCaptchaGenerator.class)
    public GraphicCaptchaGenerator graphicCaptchaService(GraphicCaptchaProperties properties) {
        return new DefaultGraphicCaptchaGenerator(properties);
    }

    @Bean
    @ConditionalOnMissingBean(BlockPuzzleCaptchaGenerator.class)
    public BlockPuzzleCaptchaGenerator blockPuzzleCaptchaService() {
        return new DefaultBlockPuzzleCaptchaGenerator(properties.getBlockPuzzle().getImageLocations());
    }

    @Bean
    @ConditionalOnMissingBean(ClickWordCaptchaGenerator.class)
    public ClickWordCaptchaGenerator clickWordCaptchaService() {
        return new DefaultClickWordCaptchaGenerator();
    }

    @Bean
    @ConditionalOnMissingBean(BehaviorCaptchaEngine.class)
    public BehaviorCaptchaEngine behaviorCaptchaService(ObjectMapper objectMapper) {
        return new DefaultBehaviorCaptchaEngine(objectMapper);
    }

    /**
     * 阿里云短信供应商：{@code mango.captcha.sms.provider=aliyun} 时生效。
     *
     * <p>装配期不因配置缺失抛异常（应用必须能启动），只记录缺失的配置项键名；发送期统一返回
     * {@code CAPTCHA_SEND_FAILED}(2410)。{@code @ConditionalOnClass} 保证下游排除 SDK 依赖时可正常启动。</p>
     *
     * @param properties 验证码配置属性
     * @return 阿里云短信供应商
     */
    @Bean
    @ConditionalOnProperty(name = "mango.captcha.sms.provider", havingValue = "aliyun")
    @ConditionalOnMissingBean(SmsProvider.class)
    @ConditionalOnClass(name = "com.aliyun.dysmsapi20170525.Client")
    public SmsProvider aliyunSmsProvider(CaptchaProperties properties) {
        CaptchaProperties.Sms sms = sms(properties);
        CaptchaProperties.Aliyun aliyun = sms.getAliyun() == null ? new CaptchaProperties.Aliyun() : sms.getAliyun();
        warnMissing(AliyunSmsProvider.ACCESS_KEY_ID_PROPERTY, aliyun.getAccessKeyId());
        warnMissing(AliyunSmsProvider.ACCESS_KEY_SECRET_PROPERTY, aliyun.getAccessKeySecret());
        warnMissing(AliyunSmsProvider.SIGN_NAME_PROPERTY, aliyun.getSignName());
        warnMissing(AliyunSmsProvider.TEMPLATE_CODE_PROPERTY, sms.getTemplateCode());
        return new AliyunSmsProvider(sms);
    }

    /**
     * 腾讯云短信供应商：{@code mango.captcha.sms.provider=tencent} 时生效。
     *
     * <p>装配期与校验行为同 {@link #aliyunSmsProvider(CaptchaProperties)}。</p>
     *
     * @param properties 验证码配置属性
     * @return 腾讯云短信供应商
     */
    @Bean
    @ConditionalOnProperty(name = "mango.captcha.sms.provider", havingValue = "tencent")
    @ConditionalOnMissingBean(SmsProvider.class)
    @ConditionalOnClass(name = "com.tencentcloudapi.sms.v20210111.SmsClient")
    public SmsProvider tencentSmsProvider(CaptchaProperties properties) {
        CaptchaProperties.Sms sms = sms(properties);
        CaptchaProperties.Tencent tencent = sms.getTencent() == null
                ? new CaptchaProperties.Tencent() : sms.getTencent();
        warnMissing(TencentSmsProvider.SECRET_ID_PROPERTY, tencent.getSecretId());
        warnMissing(TencentSmsProvider.SECRET_KEY_PROPERTY, tencent.getSecretKey());
        warnMissing(TencentSmsProvider.SIGN_NAME_PROPERTY, tencent.getSignName());
        warnMissing(TencentSmsProvider.SDK_APP_ID_PROPERTY, tencent.getSdkAppId());
        warnMissing(TencentSmsProvider.TEMPLATE_CODE_PROPERTY, sms.getTemplateCode());
        return new TencentSmsProvider(sms);
    }

    /**
     * 空实现短信供应商：{@code mango.captcha.sms.provider} 未配置或取值为 {@code default} 时生效，
     * 发送时记录告警并返回失败，避免验证码链路误判为发送成功。
     *
     * <p>固定验证码（开发期）在 {@code CaptchaService} 中短路，此供应商不会被调用。</p>
     *
     * @return 空实现短信供应商
     */
    @Bean
    @ConditionalOnProperty(name = "mango.captcha.sms.provider", havingValue = "default", matchIfMissing = true)
    @ConditionalOnMissingBean(SmsProvider.class)
    public SmsProvider defaultSmsProvider() {
        return new DefaultSmsProvider();
    }

    private static CaptchaProperties.Sms sms(CaptchaProperties properties) {
        return properties.getSms() == null ? new CaptchaProperties.Sms() : properties.getSms();
    }

    private static void warnMissing(String property, String value) {
        if (!StringUtils.hasText(value)) {
            log.error("验证码短信配置缺失，发送时将返回 2410: property={}", property);
        }
    }

    @Bean
    @ConditionalOnMissingBean(EmailProvider.class)
    public EmailProvider defaultEmailProvider() {
        return new DefaultEmailProvider();
    }
}
