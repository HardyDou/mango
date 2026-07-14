package io.mango.notice.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeResourceDeclarationContractTest {

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

    private String resourceText(String path) throws IOException {
        try (var input = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
