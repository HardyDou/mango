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

        JsonNode noticeCenter = findMenu(declaration, "notice");
        JsonNode messageCenter = findMenu(declaration, "message-center");
        assertThat(noticeCenter).isNotNull();
        assertThat(messageCenter).isNotNull();

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
        assertThat(announcements.path("menuName").asText()).isEqualTo("系统公告");
        assertThat(stringValues(announcements.get("roleCodes"))).containsExactly("ROLE_LOGIN");
        assertThat(stringValues(announcements.get("apiCodes"))).containsExactlyInAnyOrder(
                "notice:site:view", "notice:site:edit");

        JsonNode receiveSetting = findMenu(declaration, "notice:receive-setting");
        assertThat(receiveSetting).isNotNull();
        assertThat(receiveSetting.path("menuName").asText()).isEqualTo("接收配置");
        assertThat(receiveSetting.path("path").asText()).isEqualTo("/message-center/receive-setting");
        assertThat(receiveSetting.path("component").asText()).isEqualTo("notice/receive-setting/index");
        assertThat(receiveSetting.path("sort").asInt()).isEqualTo(3);
        assertThat(stringValues(receiveSetting.get("roleCodes"))).containsExactly("ROLE_LOGIN");
        assertThat(stringValues(receiveSetting.get("apiCodes"))).containsExactlyInAnyOrder(
                "notice:receive-setting:view", "notice:receive-setting:edit");
        assertThat(directChildMenuCodes(noticeCenter)).doesNotContain("notice:receive-setting");
        assertThat(directChildMenuCodes(messageCenter)).contains("notice:receive-setting");
        assertThat(countMenus(declaration, "notice:receive-setting")).isEqualTo(1);
        assertThat(declaration.toString()).doesNotContain("ROLE_ANONYMOUS");
    }

    private List<String> directChildMenuCodes(JsonNode menu) {
        return StreamSupport.stream(menu.path("children").spliterator(), false)
                .map(child -> child.path("menuCode").asText())
                .toList();
    }

    private long countMenus(JsonNode node, String menuCode) {
        long current = node.isObject() && menuCode.equals(node.path("menuCode").asText()) ? 1 : 0;
        return current + StreamSupport.stream(node.spliterator(), false)
                .mapToLong(child -> countMenus(child, menuCode))
                .sum();
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
