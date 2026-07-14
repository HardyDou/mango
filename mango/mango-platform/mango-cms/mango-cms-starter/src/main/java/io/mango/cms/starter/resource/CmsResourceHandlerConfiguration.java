package io.mango.cms.starter.resource;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.cms.core.entity.CmsAdDeliveryEntity;
import io.mango.cms.core.entity.CmsAdvertisementEntity;
import io.mango.cms.core.entity.CmsBannerEntity;
import io.mango.cms.core.entity.CmsBaseTenantEntity;
import io.mango.cms.core.entity.CmsContentEntity;
import io.mango.cms.core.entity.CmsContentPublishEntity;
import io.mango.cms.core.entity.CmsNavigationEntity;
import io.mango.cms.core.entity.CmsSiteCategoryEntity;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.entity.CmsSiteSettingEntity;
import io.mango.cms.core.mapper.CmsAdDeliveryMapper;
import io.mango.cms.core.mapper.CmsAdvertisementMapper;
import io.mango.cms.core.mapper.CmsBannerMapper;
import io.mango.cms.core.mapper.CmsContentMapper;
import io.mango.cms.core.mapper.CmsContentPublishMapper;
import io.mango.cms.core.mapper.CmsNavigationMapper;
import io.mango.cms.core.mapper.CmsSiteCategoryMapper;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.cms.core.mapper.CmsSiteSettingMapper;
import io.mango.resource.api.ResourceHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.mango.cms.starter.resource.CmsResourceTypes.ADVERTISEMENT;
import static io.mango.cms.starter.resource.CmsResourceTypes.AD_DELIVERY;
import static io.mango.cms.starter.resource.CmsResourceTypes.BANNER;
import static io.mango.cms.starter.resource.CmsResourceTypes.CONTENT;
import static io.mango.cms.starter.resource.CmsResourceTypes.CONTENT_PUBLISH;
import static io.mango.cms.starter.resource.CmsResourceTypes.NAVIGATION;
import static io.mango.cms.starter.resource.CmsResourceTypes.SITE;
import static io.mango.cms.starter.resource.CmsResourceTypes.SITE_CATEGORY;
import static io.mango.cms.starter.resource.CmsResourceTypes.SITE_SETTING;

/**
 * CMS-owned Resource Registry handlers.
 */
@Configuration(proxyBeanMethods = false)
public class CmsResourceHandlerConfiguration {

    @Bean
    ResourceHandler cmsSiteResourceHandler(CmsSiteMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, CmsSiteEntity.class, SITE, "cms_site",
                fields("targetId", "tenantId", "orgId", "siteName", "siteCode", "logoFileId", "description",
                        "domain", "status", "defaultLanguage", "seoTitle", "seoKeywords", "seoDescription",
                        "footerCopyright", "icpRecord", "contactInfo"),
                required("targetId", "tenantId", "siteName", "siteCode", "status"), List.of());
    }

    @Bean
    ResourceHandler cmsSiteSettingResourceHandler(CmsSiteSettingMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, CmsSiteSettingEntity.class, SITE_SETTING, "cms_site_setting",
                fields("targetId", "tenantId", "orgId", "siteId", "seoTitle", "seoKeywords", "seoDescription",
                        "footerCopyright", "icpRecord", "contactInfo"),
                required("targetId", "tenantId", "siteId"), List.of(SITE));
    }

    @Bean
    ResourceHandler cmsSiteCategoryResourceHandler(CmsSiteCategoryMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, CmsSiteCategoryEntity.class, SITE_CATEGORY, "cms_site_category",
                fields("targetId", "tenantId", "orgId", "siteId", "parentId", "categoryName", "categoryCode",
                        "categoryType", "accessPath", "externalUrl", "sort", "visibleStatus", "accessType",
                        "roleCodes", "seoTitle", "seoKeywords", "seoDescription"),
                required("targetId", "tenantId", "siteId", "parentId", "categoryName", "categoryCode",
                        "categoryType", "sort", "visibleStatus", "accessType"), List.of(SITE));
    }

    @Bean
    ResourceHandler cmsContentResourceHandler(CmsContentMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, CmsContentEntity.class, CONTENT, "cms_content",
                fields("targetId", "tenantId", "orgId", "title", "subtitle", "summary", "contentType",
                        "coverFileId", "body", "externalUrl", "attachmentFileId", "videoFileId", "source",
                        "author", "categoryId", "seoTitle", "seoKeywords", "seoDescription", "status",
                        "publishTime", "offlineTime", "reviewComment"),
                required("targetId", "tenantId", "title", "contentType", "status"), List.of(SITE_CATEGORY));
    }

    @Bean
    ResourceHandler cmsContentPublishResourceHandler(CmsContentPublishMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, CmsContentPublishEntity.class, CONTENT_PUBLISH, "cms_content_publish",
                fields("targetId", "tenantId", "orgId", "contentId", "siteId", "categoryId", "publishStatus",
                        "publishTime", "scheduledPublishTime", "offlineTime", "top", "topScope", "recommended",
                        "recommendationType", "sort"),
                required("targetId", "tenantId", "contentId", "siteId", "categoryId", "publishStatus", "top",
                        "recommended", "sort"), List.of(CONTENT, SITE, SITE_CATEGORY));
    }

    @Bean
    ResourceHandler cmsNavigationResourceHandler(CmsNavigationMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, CmsNavigationEntity.class, NAVIGATION, "cms_navigation",
                fields("targetId", "tenantId", "orgId", "siteId", "navType", "navName", "jumpType",
                        "categoryId", "contentId", "externalUrl", "openTarget", "sort", "status"),
                required("targetId", "tenantId", "siteId", "navType", "navName", "jumpType", "sort", "status"),
                List.of(SITE, SITE_CATEGORY, CONTENT));
    }

    @Bean
    ResourceHandler cmsBannerResourceHandler(CmsBannerMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, CmsBannerEntity.class, BANNER, "cms_banner",
                fields("targetId", "tenantId", "orgId", "siteId", "position", "title", "subtitle", "mediaType",
                        "mediaFileId", "jumpUrl", "startTime", "endTime", "sort", "status"),
                required("targetId", "tenantId", "siteId", "position", "title", "mediaType", "sort", "status"),
                List.of(SITE));
    }

    @Bean
    ResourceHandler cmsAdvertisementResourceHandler(CmsAdvertisementMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, CmsAdvertisementEntity.class, ADVERTISEMENT, "cms_advertisement",
                fields("targetId", "tenantId", "orgId", "siteId", "adCode", "adName", "position",
                        "positionType", "supportedMaterialTypes", "width", "height", "remark", "adType",
                        "materialFileId", "jumpUrl", "startTime", "endTime", "sort", "status"),
                required("targetId", "tenantId", "siteId", "adCode", "adName", "position", "sort", "status"),
                List.of(SITE));
    }

    @Bean
    ResourceHandler cmsAdDeliveryResourceHandler(CmsAdDeliveryMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, CmsAdDeliveryEntity.class, AD_DELIVERY, "cms_ad_delivery",
                fields("targetId", "tenantId", "orgId", "siteId", "adId", "deliveryName", "materialType",
                        "title", "textContent", "richContent", "htmlContent", "imageFileId", "imageFileIds",
                        "videoFileId", "coverFileId", "jumpUrl", "openTarget", "startTime", "endTime", "sort",
                        "status"),
                required("targetId", "tenantId", "siteId", "adId", "deliveryName", "materialType", "sort",
                        "status"), List.of(SITE, ADVERTISEMENT));
    }

    private <E extends CmsBaseTenantEntity> ResourceHandler handler(
            BaseMapper<E> mapper,
            ObjectMapper objectMapper,
            Class<E> entityType,
            String resourceType,
            String table,
            Map<String, String> fields,
            Set<String> requiredFields,
            List<String> dependencies) {
        return new CmsTableResourceHandler<>(mapper, objectMapper,
                new CmsTableResourceHandler.Definition<>(
                        resourceType, table, entityType, fields, requiredFields, dependencies));
    }

    private static Map<String, String> fields(String... fieldNames) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String fieldName : fieldNames) {
            if ("targetId".equals(fieldName)) {
                fields.put(fieldName, "id");
            } else {
                fields.put(fieldName, snakeCase(fieldName));
            }
        }
        return fields;
    }

    private static Set<String> required(String... fieldNames) {
        return Set.of(fieldNames);
    }

    private static String snakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
