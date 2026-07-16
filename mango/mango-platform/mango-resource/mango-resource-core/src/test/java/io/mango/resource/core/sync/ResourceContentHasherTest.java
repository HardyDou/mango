package io.mango.resource.core.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceContentHasherTest {

    private final ResourceContentHasher hasher = new ResourceContentHasher(new ObjectMapper());

    @Test
    void hashChangesWhenVersionChanges() {
        ResourceDeclaration first = declaration(1);
        ResourceDeclaration second = declaration(2);

        assertThat(hasher.hash(first)).isNotEqualTo(hasher.hash(second));
    }

    @Test
    void hashChangesWhenSyncModeChanges() {
        ResourceDeclaration first = declaration(1);
        ResourceDeclaration second = declaration(1);
        second.setSyncMode(ResourceSyncMode.MANUAL);

        assertThat(hasher.hash(first)).isNotEqualTo(hasher.hash(second));
    }

    @Test
    void hashIgnoresFieldDeclarationOrder() {
        ResourceDeclaration first = declaration(1);
        ResourceDeclaration second = declaration(1);
        second.clearFields();
        ResourceField body = new ResourceField();
        body.setType(ResourceFieldType.STRING);
        body.setValue("正文");
        second.putField("body", body);
        ResourceField title = new ResourceField();
        title.setType(ResourceFieldType.STRING);
        title.setValue("提交申请");
        second.putField("title", title);
        ResourceField firstBody = new ResourceField();
        firstBody.setType(ResourceFieldType.STRING);
        firstBody.setValue("正文");
        first.putField("body", firstBody);

        assertThat(hasher.hash(first)).isEqualTo(hasher.hash(second));
    }

    @Test
    void hashIncludesClasspathFileContent() {
        ResourceDeclaration declaration = declaration(1);
        ResourceField body = new ResourceField();
        body.setType(ResourceFieldType.FILE);
        body.setLocation("classpath:templates/guarantee-submit.txt");
        declaration.putField("body", body);

        String hash = hasher.hash(declaration);

        assertThat(hash).hasSize(32);
    }

    @Test
    void fileFieldOnlyAllowsClasspathLocation() {
        ResourceDeclaration declaration = declaration(1);
        ResourceField body = new ResourceField();
        body.setType(ResourceFieldType.FILE);
        body.setLocation("file:/tmp/body.txt");
        declaration.putField("body", body);

        assertThatThrownBy(() -> hasher.hash(declaration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only supports classpath");
    }

    private ResourceDeclaration declaration(int version) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("1900000000000000001");
        declaration.setVersion(version);
        declaration.setResourceType("MESSAGE_TEMPLATE");
        declaration.setModuleCode("guarantee");
        declaration.setBizKey("guarantee.apply.submit");
        declaration.setName("提交申请通知");
        declaration.setTargetModule("notice");
        declaration.setFields(new LinkedHashMap<>());
        ResourceField title = new ResourceField();
        title.setType(ResourceFieldType.STRING);
        title.setValue("提交申请");
        declaration.putField("title", title);
        return declaration;
    }
}
