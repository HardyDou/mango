package io.mango.link.starter.resource;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.persistence.api.entity.TenantEntity;
import io.mango.link.core.entity.LinkCategoryEntity;
import io.mango.link.core.entity.LinkFavoriteEntity;
import io.mango.link.core.entity.LinkItemEntity;
import io.mango.link.core.mapper.LinkCategoryMapper;
import io.mango.link.core.mapper.LinkFavoriteMapper;
import io.mango.link.core.mapper.LinkItemMapper;
import io.mango.resource.support.ResourceHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.mango.link.starter.resource.LinkResourceTypes.CATEGORY;
import static io.mango.link.starter.resource.LinkResourceTypes.FAVORITE;
import static io.mango.link.starter.resource.LinkResourceTypes.ITEM;

/** Link 模块 Resource Registry 处理器配置。 */
@Configuration(proxyBeanMethods = false)
public class LinkResourceHandlerConfiguration {

    @Bean
    ResourceHandler linkCategoryResourceHandler(LinkCategoryMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, LinkCategoryEntity.class, CATEGORY, "link_category",
                fields("targetId", "tenantId", "orgId", "scope", "ownerUserId", "name", "sortNo",
                        "status", "remark"),
                required("targetId", "tenantId", "scope", "ownerUserId", "name", "sortNo", "status"),
                List.of(), LinkTableResourceHandler.DisableMode.STATUS);
    }

    @Bean
    ResourceHandler linkItemResourceHandler(LinkItemMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, LinkItemEntity.class, ITEM, "link_item",
                fields("targetId", "tenantId", "orgId", "categoryId", "name", "url", "summary", "iconUrl",
                        "tags", "visibilityScope", "ownerUserId", "openMode", "recommended", "sortNo",
                        "status", "remark"),
                required("targetId", "tenantId", "categoryId", "name", "url", "visibilityScope",
                        "ownerUserId", "openMode", "recommended", "sortNo", "status"),
                List.of(CATEGORY), LinkTableResourceHandler.DisableMode.STATUS);
    }

    @Bean
    ResourceHandler linkFavoriteResourceHandler(LinkFavoriteMapper mapper, ObjectMapper objectMapper) {
        return handler(mapper, objectMapper, LinkFavoriteEntity.class, FAVORITE, "link_favorite",
                fields("targetId", "tenantId", "orgId", "userId", "linkId"),
                required("targetId", "tenantId", "userId", "linkId"),
                List.of(ITEM), LinkTableResourceHandler.DisableMode.DELETE);
    }

    private <E extends TenantEntity> ResourceHandler handler(
            BaseMapper<E> mapper,
            ObjectMapper objectMapper,
            Class<E> entityType,
            String resourceType,
            String table,
            Map<String, String> fields,
            Set<String> requiredFields,
            List<String> dependencies,
            LinkTableResourceHandler.DisableMode disableMode) {
        return new LinkTableResourceHandler<>(mapper, objectMapper,
                new LinkTableResourceHandler.Definition<>(resourceType, table, entityType,
                        fields, requiredFields, dependencies, disableMode));
    }

    private static Map<String, String> fields(String... fieldNames) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("createdBy", "created_by");
        fields.put("updatedBy", "updated_by");
        for (String fieldName : fieldNames) {
            fields.put(fieldName, "targetId".equals(fieldName) ? "id" : snakeCase(fieldName));
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
