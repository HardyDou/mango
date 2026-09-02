package io.mango.auth.starter.resource;

import io.mango.resource.support.model.ResourceDeclaration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthMessageTemplateResourceProviderTest {

    @Test
    void providesOnlyLoginLockSecurityTemplates() {
        List<ResourceDeclaration> declarations = new AuthMessageTemplateResourceProvider().provide();

        assertThat(declarations).hasSize(4);
        assertThat(declarations)
                .extracting(declaration -> field(declaration, "bizType"))
                .containsOnly("auth.login.locked");
    }

    private Object field(ResourceDeclaration declaration, String name) {
        return declaration.getFields().get(name).getValue();
    }
}
