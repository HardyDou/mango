package io.mango.system.starter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SystemMenuResourceContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void wildcardPermissionBelongsOnlyToPlatformPackage() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/META-INF/mango/resources/system-common-menu.json")) {
            assertThat(input).isNotNull();
            JsonNode declarations = objectMapper.readTree(input)
                    .path("mango").path("resource").path("declarations").path("AUTH_MENU");

            JsonNode systemRoot = findMenu(declarations, "system");
            JsonNode wildcard = findMenu(declarations, "system:all-permissions");

            assertThat(stringValues(systemRoot.path("apiCodes"))).doesNotContain("*:*");
            assertThat(wildcard.path("menuType").asInt()).isEqualTo(3);
            assertThat(stringValues(wildcard.path("packageCodes"))).containsExactly("platform_admin");
            assertThat(stringValues(wildcard.path("apiCodes"))).containsExactly("*:*");
        }
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
