package io.mango.notice.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.support.channel.NoticeChannelSecretResolver;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class NoticeChannelSecretMaterializerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void materializeUsesReferenceBeforeManualSecretAndKeepsUnmanagedManualSecret()
            throws Exception {
        NoticeChannelSecretMaterializer materializer =
                new NoticeChannelSecretMaterializer(
                        objectMapper,
                        List.of(
                                new MapSecretResolver(
                                        Map.of("property:mail.password", "from-reference"))));
        NoticeChannelConfigEntity entity =
                entity(
                        "{\"host\":\"smtp.example.com\",\"password\":\"unsafe-config-value\"}",
                        "{\"password\":\"property:mail.password\"}",
                        "{\"password\":\"manual-value\",\"username\":\"manual-user\"}");

        JsonNode result = objectMapper.readTree(materializer.materialize(entity));

        assertThat(result.get("password").asText()).isEqualTo("from-reference");
        assertThat(result.get("username").asText()).isEqualTo("manual-user");
        assertThat(result.get("host").asText()).isEqualTo("smtp.example.com");
    }

    @Test
    void materializeRejectsUnsupportedOrUnresolvedReferenceWithoutLeakingReferenceValue() {
        NoticeChannelSecretMaterializer materializer =
                new NoticeChannelSecretMaterializer(
                        objectMapper, List.of(new MapSecretResolver(Map.of())));

        assertThatThrownBy(
                        () ->
                                materializer.materialize(
                                        entity("{}", "{\"password\":\"file:/tmp/secret\"}", null)))
                .isInstanceOf(NoticeChannelSecretResolutionException.class)
                .hasMessageContaining("不受支持")
                .hasMessageNotContaining("/tmp/secret");
        assertThatThrownBy(
                        () ->
                                materializer.materialize(
                                        entity("{}", "{\"password\":\"property:missing\"}", null)))
                .isInstanceOf(NoticeChannelSecretResolutionException.class)
                .hasMessageContaining("未解析")
                .hasMessageNotContaining("property:missing");
    }

    private static NoticeChannelConfigEntity entity(String config, String refs, String manual) {
        NoticeChannelConfigEntity entity = new NoticeChannelConfigEntity();
        entity.setConfigJson(config);
        entity.setSecretRefsJson(refs);
        entity.setSecretConfigJson(manual);
        return entity;
    }

    private record MapSecretResolver(Map<String, String> values)
            implements NoticeChannelSecretResolver {
        @Override
        public boolean supports(String reference) {
            return reference != null && reference.startsWith("property:");
        }

        @Override
        public String resolve(String reference) {
            return values.get(reference);
        }
    }
}
