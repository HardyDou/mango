package io.mango.cms.starter.remote;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CmsRemoteAutoConfigurationTest {

    private static final List<Class<?>> FEIGN_TYPES = List.of(
            CmsContentCategoryFeignClient.class,
            CmsContentTagFeignClient.class,
            CmsSiteAdminFeignClient.class,
            CmsSiteCategoryFeignClient.class,
            CmsContentFeignClient.class,
            CmsContentPublishFeignClient.class,
            CmsNavigationFeignClient.class,
            CmsBannerFeignClient.class,
            CmsAdvertisementFeignClient.class,
            CmsAdDeliveryFeignClient.class,
            CmsSiteSettingFeignClient.class,
            CmsSiteFeignClient.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class, CmsRemoteAutoConfiguration.class))
            .withPropertyValues(FEIGN_TYPES.stream()
                    .map(type -> "spring.cloud.openfeign.client.config."
                            + type.getAnnotation(FeignClient.class).contextId()
                            + ".url=http://localhost")
                    .toArray(String[]::new));

    @Test
    void autoConfiguration_registersAllCmsFeignAdapters() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            FEIGN_TYPES.forEach(type -> assertThat(context.getBeansOfType(type))
                    .as(type.getSimpleName())
                    .hasSize(1));
        });
    }
}
