package io.mango.cms.starter.resource;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.cms.core.entity.CmsSiteEntity;
import io.mango.cms.core.mapper.CmsSiteMapper;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CmsTableResourceHandlerTest {

    private final CmsSiteMapper mapper = mock(CmsSiteMapper.class);
    private final CmsTableResourceHandler<CmsSiteEntity> handler = new CmsTableResourceHandler<>(
            mapper,
            new ObjectMapper().findAndRegisterModules(),
            new CmsTableResourceHandler.Definition<>(
                    CmsResourceTypes.SITE,
                    "cms_site",
                    CmsSiteEntity.class,
                    Map.of("targetId", "id", "tenantId", "tenant_id", "siteCode", "site_code",
                            "siteName", "site_name", "status", "status", "createdBy", "created_by",
                            "updatedBy", "updated_by"),
                    Set.of("targetId", "tenantId", "siteCode", "siteName", "status"),
                    List.of()));

    @Test
    void upsertMapsOnlyDeclaredFieldsAndCreatesAnActiveRecord() {
        when(mapper.selectById(anyLong())).thenReturn(null);

        handler.upsert(declaration(Map.of(
                "targetId", field(ResourceFieldType.LONG, "2070000000000000001"),
                "tenantId", field(ResourceFieldType.STRING, "1"),
                "createdBy", field(ResourceFieldType.LONG, "1"),
                "updatedBy", field(ResourceFieldType.LONG, "1"),
                "siteCode", field(ResourceFieldType.STRING, "demo"),
                "siteName", field(ResourceFieldType.STRING, "演示站点"),
                "status", field(ResourceFieldType.STRING, "ENABLED"))));

        ArgumentCaptor<CmsSiteEntity> entity = ArgumentCaptor.forClass(CmsSiteEntity.class);
        verify(mapper).insert(entity.capture());
        assertThat(entity.getValue().getId()).isEqualTo(2070000000000000001L);
        assertThat(entity.getValue().getTenantId()).isEqualTo("1");
        assertThat(entity.getValue().getCreatedBy()).isEqualTo(1L);
        assertThat(entity.getValue().getUpdatedBy()).isEqualTo(1L);
        assertThat(entity.getValue().getSiteCode()).isEqualTo("demo");
        assertThat(entity.getValue().getDeleted()).isZero();
    }

    @Test
    void upsertRejectsUnknownAndMissingFields() {
        Map<String, ResourceField> unknownFields = new LinkedHashMap<>(validFields());
        unknownFields.put("unexpected", field(ResourceFieldType.STRING, "value"));

        assertThatThrownBy(() -> handler.upsert(declaration(unknownFields)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported CMS resource fields");

        Map<String, ResourceField> missingFields = new LinkedHashMap<>(validFields());
        missingFields.remove("siteCode");
        assertThatThrownBy(() -> handler.upsert(declaration(missingFields)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing CMS resource field: siteCode");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void disableUsesTheCmsLogicalDeleteContract() {
        handler.disable(declaration(validFields()));

        ArgumentCaptor<UpdateWrapper<CmsSiteEntity>> update =
                (ArgumentCaptor) ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(mapper).update(isNull(), update.capture());
        assertThat(update.getValue().getSqlSet()).contains("deleted=");
        assertThat(update.getValue().getCustomSqlSegment()).contains("id");
        assertThat(update.getValue().getParamNameValuePairs().values())
                .contains(1, 2070000000000000001L);
    }

    private Map<String, ResourceField> validFields() {
        return Map.of(
                "targetId", field(ResourceFieldType.LONG, "2070000000000000001"),
                "tenantId", field(ResourceFieldType.STRING, "1"),
                "createdBy", field(ResourceFieldType.LONG, "1"),
                "updatedBy", field(ResourceFieldType.LONG, "1"),
                "siteCode", field(ResourceFieldType.STRING, "demo"),
                "siteName", field(ResourceFieldType.STRING, "演示站点"),
                "status", field(ResourceFieldType.STRING, "ENABLED"));
    }

    private ResourceDeclaration declaration(Map<String, ResourceField> fields) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setResourceType(CmsResourceTypes.SITE);
        declaration.setBizKey("cms.site.demo");
        declaration.setFields(new LinkedHashMap<>(fields));
        return declaration;
    }

    private ResourceField field(ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        return field;
    }
}
