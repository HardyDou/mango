package io.mango.notice.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeResourceDeclarationContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void formalDeclarationsBelongToNoticeAndContainOnlyFormalResources() throws IOException {
        String domain = resourceText("META-INF/mango/resources/notice-common-domain.yml");
        String message = resourceText("META-INF/mango/resources/notice-common-message.yml");
        String menu = resourceText("META-INF/mango/resources/notice-common-menu.json");
        String all = domain + message + menu;

        assertThat(domain)
                .contains("module-code: notice")
                .contains("biz-key: notice.domain.notice");
        assertThat(message)
                .contains("module-code: notice")
                .contains("notice.channel.site-internal-default")
                .contains("notice.channel.site-internal-tenant-1");
        assertThat(menu)
                .contains("\"moduleCode\": \"notice\"")
                .contains("notice.menu.internal-admin");
        assertThat(all)
                .doesNotContain("1012404303@qq.com")
                .doesNotContain("18701445644")
                .doesNotContain("notice_recipient_account")
                .doesNotContain("notice_task")
                .doesNotContain("notice_send_record")
                .doesNotContain("notice_site_message")
                .doesNotContain("notice_announcement_recipient");
    }

    @Test
    void loginRoleReceivesPersonalNoticeMenusAndPermissions() throws IOException {
        JsonNode declaration = objectMapper.readTree(
                resourceText("META-INF/mango/resources/notice-common-menu.json"));

        JsonNode loginBasic = findMenu(declaration, "notice:basic-login");
        assertThat(loginBasic).isNotNull();
        assertThat(stringValues(loginBasic.get("roleCodes"))).containsExactly("ROLE_LOGIN");
        assertThat(stringValues(loginBasic.get("apiCodes"))).containsExactlyInAnyOrder(
                "notice:site:view",
                "notice:site:edit",
                "notice:business:view",
                "notice:receive-setting:view",
                "notice:receive-setting:edit");

        JsonNode siteMessages = findMenu(declaration, "notice:site-message");
        assertThat(siteMessages).isNotNull();
        assertThat(stringValues(siteMessages.get("roleCodes"))).containsExactly("ROLE_LOGIN");
        assertThat(stringValues(siteMessages.get("apiCodes"))).containsExactlyInAnyOrder(
                "notice:site:view", "notice:site:edit");

        JsonNode announcements = findMenu(declaration, "notice:announcement-user");
        assertThat(announcements).isNotNull();
        assertThat(stringValues(announcements.get("roleCodes"))).containsExactly("ROLE_LOGIN");
        assertThat(stringValues(announcements.get("apiCodes"))).containsExactlyInAnyOrder(
                "notice:site:view", "notice:site:edit");
        assertThat(declaration.toString()).doesNotContain("ROLE_ANONYMOUS");
    }

    private JsonNode findMenu(JsonNode node, String menuCode) {
        if (node.isObject() && menuCode.equals(node.path("menuCode").asText())) {
            return node;
        }
        for (JsonNode child : node) {
            JsonNode match = findMenu(child, menuCode);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private List<String> stringValues(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }

    private String resourceText(String path) throws IOException {
        try (var input = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
