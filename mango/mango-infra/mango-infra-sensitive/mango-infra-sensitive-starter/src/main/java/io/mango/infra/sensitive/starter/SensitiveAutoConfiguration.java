package io.mango.infra.sensitive.starter;

import com.fasterxml.jackson.databind.Module;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import io.mango.infra.sensitive.api.ISensitiveMaskingService;
import io.mango.infra.sensitive.api.ISensitiveRawAccessProvider;
import io.mango.infra.sensitive.api.ISensitiveWordProvider;
import io.mango.infra.sensitive.api.SensitiveMaskingContext;
import io.mango.infra.sensitive.core.SensitiveMaskingRuntime;
import io.mango.infra.sensitive.core.jackson.SensitiveJacksonModule;
import io.mango.infra.sensitive.core.word.SensitiveWordCustomizer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

/**
 * Spring Boot auto-configuration for Mango sensitive masking.
 */
@AutoConfiguration
@EnableConfigurationProperties(SensitiveProperties.class)
public class SensitiveAutoConfiguration {

    @Bean
    public Module sensitiveJacksonModule() {
        return new SensitiveJacksonModule();
    }

    @Bean
    @ConditionalOnMissingBean
    public ISensitiveMaskingService sensitiveMaskingService(SensitiveProperties properties,
                                                            ObjectProvider<ISensitiveRawAccessProvider>
                                                                    rawAccessProviders) {
        return sensitive -> !SensitiveMaskingContext.isMaskingDisabled()
                && rawAccessProviders.orderedStream()
                .noneMatch(provider -> provider.canViewRaw(properties.getMasking().getRawAuthority()));
    }

    @Bean
    public InitializingBean sensitiveMaskingRuntimeInitializer(ISensitiveMaskingService maskingService) {
        return () -> SensitiveMaskingRuntime.setMaskingService(maskingService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SensitiveWordCustomizer sensitiveWordCustomizer(ObjectProvider<ISensitiveWordProvider> providers) {
        return new SensitiveWordCustomizer(providers.orderedStream().toList());
    }

    @Bean
    @Lazy
    @ConditionalOnClass(SensitiveWordBs.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.sensitive.word", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SensitiveWordBs sensitiveWordBs(SensitiveWordCustomizer sensitiveWordCustomizer,
                                           SensitiveProperties properties) {
        SensitiveProperties.Word word = properties.getWord();
        return SensitiveWordBs.newInstance()
                .wordAllow(sensitiveWordCustomizer)
                .wordDeny(WordDenys.chains(WordDenys.defaults(), sensitiveWordCustomizer))
                .ignoreCase(word.isIgnoreCase())
                .ignoreWidth(word.isIgnoreWidth())
                .ignoreNumStyle(word.isIgnoreNumStyle())
                .ignoreChineseStyle(word.isIgnoreChineseStyle())
                .ignoreEnglishStyle(word.isIgnoreEnglishStyle())
                .ignoreRepeat(word.isIgnoreRepeat())
                .enableNumCheck(word.isEnableNumCheck())
                .enableEmailCheck(word.isEnableEmailCheck())
                .enableUrlCheck(word.isEnableUrlCheck())
                .numCheckLen(word.getNumCheckLen())
                .init();
    }
}
