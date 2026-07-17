package io.mango.link.starter.resource;

import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LinkPublicRedirectResourceProviderTest {

    @Test
    void declaresPublicAndAuthenticatedFunctionalRoutesWithDistinctAccessModes() {
        LinkPublicRedirectResourceProvider provider = new LinkPublicRedirectResourceProvider();

        assertThat(provider.moduleCodes()).containsExactly(LinkPublicRedirectResourceProvider.MODULE_CODE);
        assertThat(provider.provide())
                .hasSize(4)
                .allSatisfy(resource -> {
                    assertThat(resource.getResourceType()).isEqualTo("API_RESOURCE");
                    assertThat(resource.getTargetModule()).isEqualTo("authorization");
                    assertThat(value(resource, "httpMethod")).isEqualTo("GET");
                })
                .extracting(resource -> value(resource, "pathPattern"))
                .containsExactly(LinkPublicRedirectResourceProvider.REDIRECT_PATH,
                        LinkPublicRedirectResourceProvider.JUMP_PATH,
                        LinkPublicRedirectResourceProvider.VISIBLE_REDIRECT_PATH,
                        LinkPublicRedirectResourceProvider.VISIBLE_JUMP_PATH);
        assertThat(provider.provide())
                .extracting(resource -> value(resource, "accessMode"))
                .containsExactly("PUBLIC", "PUBLIC", "LOGIN", "LOGIN");
    }

    private String value(ResourceDeclaration declaration, String name) {
        Map<String, ResourceField> fields = declaration.getFields();
        ResourceField field = fields.get(name);
        assertThat(field).isNotNull();
        assertThat(field.getType()).isEqualTo(ResourceFieldType.STRING);
        return String.valueOf(field.getValue());
    }
}
