package io.mango.captcha.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.spi.EmailProvider;
import io.mango.captcha.api.spi.SmsProvider;
import io.mango.captcha.core.service.ArithmeticCaptchaService;
import io.mango.captcha.core.service.BlockPuzzleCaptchaService;
import io.mango.captcha.core.service.ClickWordCaptchaService;
import io.mango.captcha.core.service.ICaptchaService;
import io.mango.captcha.core.service.impl.ArithmeticCaptchaServiceImpl;
import io.mango.captcha.core.service.impl.BlockPuzzleCaptchaServiceImpl;
import io.mango.captcha.core.service.impl.CaptchaServiceImpl;
import io.mango.captcha.core.service.impl.ClickWordCaptchaServiceImpl;
import io.mango.infra.kv.api.IKvStore;
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

import java.util.List;

/**
 * 验证码自动配置
 *
 * @author Mango
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(CaptchaProperties.class)
@RequiredArgsConstructor
@Import({CaptchaController.class})
public class CaptchaAutoConfiguration {

    private final CaptchaProperties properties;

    @Bean
    @ConditionalOnMissingBean(ArithmeticCaptchaService.class)
    public ArithmeticCaptchaService arithmeticCaptchaService() {
        return new ArithmeticCaptchaServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(BlockPuzzleCaptchaService.class)
    public BlockPuzzleCaptchaService blockPuzzleCaptchaService() {
        return new BlockPuzzleCaptchaServiceImpl(properties.getBlockPuzzle().getImageLocations());
    }

    @Bean
    @ConditionalOnMissingBean(ClickWordCaptchaService.class)
    public ClickWordCaptchaService clickWordCaptchaService() {
        return new ClickWordCaptchaServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(CaptchaApi.class)
    public ICaptchaService captchaApi(IKvStore kvStore,
                                      ArithmeticCaptchaService arithmeticCaptchaService,
                                      BlockPuzzleCaptchaService blockPuzzleCaptchaService,
                                      ClickWordCaptchaService clickWordCaptchaService,
                                      List<SmsProvider> smsProviders,
                                      List<EmailProvider> emailProviders,
                                      ObjectMapper objectMapper) {
        return new CaptchaServiceImpl(
                kvStore,
                arithmeticCaptchaService,
                blockPuzzleCaptchaService,
                clickWordCaptchaService,
                smsProviders,
                emailProviders,
                objectMapper
        );
    }

    /**
     * 默认短信供应商（仅打印日志，实际需配置第三方短信服务）
     */
    @Bean
    @ConditionalOnMissingBean(SmsProvider.class)
    public SmsProvider defaultSmsProvider() {
        return new DefaultSmsProvider();
    }

    /**
     * 默认邮件供应商（仅打印日志，实际需配置SMTP服务）
     */
    @Bean
    @ConditionalOnMissingBean(EmailProvider.class)
    public EmailProvider defaultEmailProvider() {
        return new DefaultEmailProvider();
    }
}
