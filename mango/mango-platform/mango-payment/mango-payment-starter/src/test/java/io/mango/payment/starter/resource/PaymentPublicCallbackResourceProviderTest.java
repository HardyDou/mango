package io.mango.payment.starter.resource;

import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPublicCallbackResourceProviderTest {

    @Test
    void declaresBothCallbackMethodsAsPublicApiResources() {
        PaymentPublicCallbackResourceProvider provider = new PaymentPublicCallbackResourceProvider();

        assertThat(provider.moduleCodes()).containsExactly(PaymentPublicCallbackResourceProvider.MODULE_CODE);
        assertThat(provider.provide())
                .hasSize(2)
                .allSatisfy(resource -> {
                    assertThat(resource.getResourceType()).isEqualTo("API_RESOURCE");
                    assertThat(resource.getTargetModule()).isEqualTo("authorization");
                    assertThat(value(resource, "pathPattern"))
                            .isEqualTo(PaymentPublicCallbackResourceProvider.CALLBACK_PATH);
                    assertThat(value(resource, "accessMode")).isEqualTo("PUBLIC");
                })
                .extracting(resource -> value(resource, "httpMethod"))
                .containsExactly("GET", "POST");
    }

    private String value(ResourceDeclaration declaration, String name) {
        Map<String, ResourceField> fields = declaration.getFields();
        ResourceField field = fields.get(name);
        assertThat(field).isNotNull();
        assertThat(field.getType()).isEqualTo(ResourceFieldType.STRING);
        return String.valueOf(field.getValue());
    }
}
