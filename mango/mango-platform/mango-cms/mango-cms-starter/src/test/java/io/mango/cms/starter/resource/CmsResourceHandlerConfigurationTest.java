package io.mango.cms.starter.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.cms.core.mapper.CmsContentCategoryMapper;
import io.mango.resource.support.ResourceHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CmsResourceHandlerConfigurationTest {

    @Test
    void exposesContentCategoryAsAFormalResourceType() {
        CmsResourceHandlerConfiguration configuration = new CmsResourceHandlerConfiguration();
        ResourceHandler handler = configuration.cmsContentCategoryResourceHandler(
                mock(CmsContentCategoryMapper.class), new ObjectMapper().findAndRegisterModules());

        assertThat(handler.resourceType()).isEqualTo(CmsResourceTypes.CONTENT_CATEGORY);
        assertThat(handler.dependsOnResourceTypes()).isEmpty();
        assertThat(handler.spec().getRequiredFields())
                .containsExactlyInAnyOrder("targetId", "tenantId", "parentId", "categoryCode", "categoryName", "sort", "status");
        assertThat(handler.spec().getFieldDescriptions())
                .containsKeys("targetId", "tenantId", "parentId", "categoryCode", "categoryName", "sort", "status", "remark");
    }
}
