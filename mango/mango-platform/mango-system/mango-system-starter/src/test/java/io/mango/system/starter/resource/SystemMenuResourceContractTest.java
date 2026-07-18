package io.mango.system.starter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SystemMenuResourceContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void menuResourcesUseExplicitPermissions() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/META-INF/mango/resources/system-common-menu.json")) {
            assertThat(input).isNotNull();
            JsonNode declarations = objectMapper.readTree(input)
                    .path("mango").path("resource").path("declarations").path("AUTH_MENU");

            JsonNode memberMenu = findMenu(declarations, "system:user");

            assertThat(stringValues(memberMenu.path("apiCodes"))).contains("system:user:list");
            assertThat(containsApiCode(declarations, "*:*")).isFalse();
        }
    }

    private boolean containsApiCode(JsonNode node, String apiCode) {
        if (node.isObject() && stringValues(node.path("apiCodes")).contains(apiCode)) {
            return true;
        }
        for (JsonNode child : node) {
            if (containsApiCode(child, apiCode)) {
                return true;
            }
        }
        return false;
    }

    private JsonNode findMenu(JsonNode declarations, String menuCode) {
        for (JsonNode declaration : declarations) {
            JsonNode found = findMenuInList(
                    declaration.path("fields").path("menus").path("value"), menuCode);
            if (found != null) {
                return found;
            }
        }
        throw new AssertionError("Menu not found: " + menuCode);
    }

    private JsonNode findMenuInList(JsonNode menus, String menuCode) {
        for (JsonNode menu : menus) {
            if (menuCode.equals(menu.path("menuCode").asText())) {
                return menu;
            }
            JsonNode child = findMenuInList(menu.path("children"), menuCode);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private java.util.List<String> stringValues(JsonNode values) {
        if (!values.isArray()) {
            return java.util.List.of();
        }
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }
}
