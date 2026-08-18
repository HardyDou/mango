package io.mango.notice.core.convert;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;

import org.junit.jupiter.api.Test;

class NoticeChannelConfigConvertTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void keepsAccountIdentifiersAndNonSensitiveFieldsWhileMaskingLegacySecrets()
            throws Exception {
        NoticeChannelConfigEntity entity = new NoticeChannelConfigEntity();
        entity.setConfigJson(
                """
                {
                  "accessKeyId":"account-access-key",
                  "secretId":"tencent-secret-id",
                  "appKey":"dingtalk-app-key",
                  "webhookUrl":"https://callback.example.com",
                  "loginEnabled":true,
                  "loginRedirectUri":"https://admin.example.com/login",
                  "password":"legacy-password",
                  "callbackToken":"legacy-token"
                }
                """);

        NoticeChannelConfigVO result = NoticeChannelConfigConvert.toVO(entity);
        JsonNode config = objectMapper.readTree(result.getConfigJson());

        assertThat(config.get("accessKeyId").asText()).isEqualTo("account-access-key");
        assertThat(config.get("secretId").asText()).isEqualTo("tencent-secret-id");
        assertThat(config.get("appKey").asText()).isEqualTo("dingtalk-app-key");
        assertThat(config.get("webhookUrl").asText())
                .isEqualTo("https://callback.example.com");
        assertThat(config.get("loginEnabled").asBoolean()).isTrue();
        assertThat(config.get("loginRedirectUri").asText())
                .isEqualTo("https://admin.example.com/login");
        assertThat(config.get("password").asText()).isEqualTo("***");
        assertThat(config.get("callbackToken").asText()).isEqualTo("***");
    }
}
