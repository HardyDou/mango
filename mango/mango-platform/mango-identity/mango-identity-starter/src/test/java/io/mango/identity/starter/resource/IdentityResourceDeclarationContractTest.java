package io.mango.identity.starter.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityResourceDeclarationContractTest {

    private static final Path FORMAL = Path.of(
            "src/main/resources/META-INF/mango/resources/identity-common-bootstrap.yml");
    private static final Path DOMAIN = Path.of(
            "src/main/resources/META-INF/mango/resources/identity-common-domain.yml");
    private static final Path DEMO = Path.of(
            "src/main/resources/META-INF/mango/demo/identity-demo-members.yml");

    @Test
    void requiredLoginIdentityAndDemoMembersAreSeparated() throws IOException {
        String formal = Files.readString(FORMAL);
        String demo = Files.readString(DEMO);

        assertThat(formal)
                .contains("module-code: identity")
                .contains("biz-key: identity.user.admin")
                .contains("memberId: { type: LONG, value: 1001 }")
                .contains("encodedPassword: { type: STRING, value: \"$2a$10$")
                .doesNotContain("password: { type: STRING")
                .doesNotContain("value: admin123")
                .doesNotContain("ORG_MEMBER_BINDING");
        assertThat(demo)
                .contains("biz-key: identity.member.tenant-2.admin")
                .contains("biz-key: identity.member.tenant-3.admin")
                .contains("biz-key: identity.member.tenant-4.admin")
                .contains("ORG_MEMBER_BINDING")
                .contains("memberId: { type: LONG, value: 1002 }")
                .contains("memberId: { type: LONG, value: 1003 }")
                .contains("memberId: { type: LONG, value: 1004 }")
                .doesNotContain("password:");
        assertThat(count(formal, "sync-mode: INIT_ONLY")).isEqualTo(1);
        assertThat(count(demo, "sync-mode: INIT_ONLY")).isEqualTo(6);
    }

    @Test
    void identityBusinessDomainIsFormallyRegistered() throws IOException {
        String domain = Files.readString(DOMAIN);

        assertThat(domain)
                .contains("module-code: identity")
                .contains("BUSINESS_DOMAIN:")
                .contains("biz-key: identity.domain.identity")
                .contains("value: IDENTITY")
                .contains("value: 身份管理")
                .contains("target-module: domain");
    }

    private int count(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
