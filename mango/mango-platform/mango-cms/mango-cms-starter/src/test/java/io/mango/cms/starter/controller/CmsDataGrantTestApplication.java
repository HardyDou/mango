package io.mango.cms.starter.controller;

import io.mango.authorization.data.grant.DataGrantAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Grant 接入测试的最小装配。
 *
 * <p>CMS 的业务服务依赖持久层，因此这里不开启完整自动配置，只装配 Web MVC、Jackson 与 Grant 自动配置，
 * 业务服务由测试替身提供。</p>
 */
@SpringBootConfiguration
@Import({
        JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        DataGrantAutoConfiguration.class,
        CmsSiteAdminController.class,
        CmsSiteSettingController.class,
        CmsContentController.class,
        CmsContentCategoryController.class,
        CmsContentTagController.class,
        CmsAdvertisementController.class,
        CmsAdDeliveryController.class,
        CmsNavigationController.class
})
class CmsDataGrantTestApplication {
}
