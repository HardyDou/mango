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
import io.mango.captcha.core.service.impl.CaptchaService;
import io.mango.captcha.starter.controller.CaptchaController;
import io.mango.captcha.starter.properties.CaptchaProperties;
import io.mango.captcha.starter.provider.DefaultEmailProvider;
import io.mango.captcha.starter.provider.DefaultSmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;


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

    @Bean
    @ConditionalOnMissingBean(SmsProvider.class)
    public SmsProvider defaultSmsProvider() {
        return new DefaultSmsProvider();
    }

    @Bean
    @ConditionalOnMissingBean(EmailProvider.class)
    public EmailProvider defaultEmailProvider() {
        return new DefaultEmailProvider();
    }
}
