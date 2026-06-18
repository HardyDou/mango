package io.mango.resource.api.builder;

import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.api.model.ResourceDeclaration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDeclarationBuilderTest {

    @Test
    void build_withBlankFields_createsDeclarationAndSkipsBlankFields() {
        ResourceDeclaration declaration = ResourceDeclarationBuilder.create(ResourceTypes.MESSAGE_TEMPLATE)
                .id("2026061801000000001")
                .version(2)
                .module("guarantee", "担保")
                .bizKey("guarantee.apply.submit")
                .name("担保申请提交消息")
                .targetModule("notice")
                .syncMode(ResourceSyncMode.MANUAL)
                .string("title", "申请已提交")
                .string("blank", " ")
                .longValue("tenantId", 100L)
                .list("channels", List.of("site", "sms"))
                .file("body", "classpath:/templates/guarantee-submit.txt", "UTF-8", "text/plain")
                .build();

        assertThat(declaration.getId()).isEqualTo("2026061801000000001");
        assertThat(declaration.getVersion()).isEqualTo(2);
        assertThat(declaration.getResourceType()).isEqualTo(ResourceTypes.MESSAGE_TEMPLATE);
        assertThat(declaration.getModuleCode()).isEqualTo("guarantee");
        assertThat(declaration.getModuleName()).isEqualTo("担保");
        assertThat(declaration.getBizKey()).isEqualTo("guarantee.apply.submit");
        assertThat(declaration.getTargetModule()).isEqualTo("notice");
        assertThat(declaration.getSyncMode()).isEqualTo(ResourceSyncMode.MANUAL);
        assertThat(declaration.getFields()).containsOnlyKeys("title", "tenantId", "channels", "body");
        assertThat(declaration.getFields().get("title").getType()).isEqualTo(ResourceFieldType.STRING);
        assertThat(declaration.getFields().get("tenantId").getValue()).isEqualTo(100L);
        assertThat(declaration.getFields().get("channels").getType()).isEqualTo(ResourceFieldType.LIST);
        assertThat(declaration.getFields().get("body").getType()).isEqualTo(ResourceFieldType.FILE);
        assertThat(declaration.getFields().get("body").getLocation())
                .isEqualTo("classpath:/templates/guarantee-submit.txt");
        assertThat(declaration.getFields().get("body").getEncoding()).isEqualTo("UTF-8");
        assertThat(declaration.getFields().get("body").getMediaType()).isEqualTo("text/plain");
    }
}
