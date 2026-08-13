package io.mango.notice.starter.resource;

import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeInboundPublicResourceProviderTest {

    @Test
    void provide_declaresBothWecomMethodsAndMailPostAsPublic() {
        var declarations = new NoticeInboundPublicResourceProvider().provide();

        assertThat(declarations).hasSize(3);
        assertThat(declarations).allSatisfy(this::assertPublic);
        assertThat(declarations.stream().map(item -> value(item, "resourceCode")))
                .containsExactlyInAnyOrder(
                        "GET:" + NoticeInboundPublicResourceProvider.WECOM_PATH,
                        "POST:" + NoticeInboundPublicResourceProvider.WECOM_PATH,
                        "POST:" + NoticeInboundPublicResourceProvider.MAIL_PATH);
    }

    private void assertPublic(ResourceDeclaration declaration) {
        assertThat(declaration.getTargetModule()).isEqualTo("authorization");
        assertThat(value(declaration, "accessMode")).isEqualTo("PUBLIC");
        assertThat(value(declaration, "handlerClass"))
                .isEqualTo("io.mango.notice.starter.endpoint.NoticeInboundPublicEndpoint");
    }

    private String value(ResourceDeclaration declaration, String name) {
        Map<String, ResourceField> fields = declaration.getFields();
        ResourceField field = fields.get(name);
        assertThat(field).isNotNull();
        assertThat(field.getType()).isEqualTo(ResourceFieldType.STRING);
        return String.valueOf(field.getValue());
    }
}
