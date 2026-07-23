package io.mango.authorization.starter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleDiagnosticPermissionResourceContractTest {

    private static final String RESOURCE =
            "META-INF/mango/resources/authorization-common-menu.json";

    @Test
    void diagnosticPermissionIsGovernedByAnInvisibleAssignableMenuItem() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
            assertThat(input).isNotNull();
            JsonNode root = new ObjectMapper().readTree(input);
            JsonNode item = findMenu(root, "system:module-diagnostic:read");

            assertThat(item).isNotNull();
            assertThat(item.path("menuType").asInt()).isEqualTo(3);
            assertThat(textValues(item.path("apiCodes"))).containsExactly("diagnostic:read");
            assertThat(textValues(item.path("packageCodes"))).containsExactly("platform_admin");
            assertThat(item.hasNonNull("path")).isFalse();
            assertThat(item.hasNonNull("component")).isFalse();
        }
    }

    private JsonNode findMenu(JsonNode node, String menuCode) {
        if (node.isObject() && menuCode.equals(node.path("menuCode").asText())) {
            return node;
        }
        for (JsonNode child : node) {
            JsonNode found = findMenu(child, menuCode);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private java.util.List<String> textValues(JsonNode values) {
        return StreamSupport.stream(values.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }
}
