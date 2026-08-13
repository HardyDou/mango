package io.mango.notice.core.convert;

import io.mango.notice.api.enums.NoticeChannelCapabilityMode;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeChannelConfigConvertTest {

    @Test
    void toVoMasksWecomInboundCallbackSecrets() {
        NoticeChannelConfigEntity entity = new NoticeChannelConfigEntity();
        entity.setConfigJson("{\"corpId\":\"corp\",\"callbackToken\":\"token-value\","
                + "\"encodingAesKey\":\"aes-key-value\",\"callbackEncodingAesKey\":\"legacy-value\"}");

        String masked = NoticeChannelConfigConvert.toVO(entity).getConfigJson();

        assertThat(masked).contains("\"corpId\":\"corp\"");
        assertThat(masked).doesNotContain("token-value", "aes-key-value", "legacy-value");
        assertThat(masked).contains("\"callbackToken\":\"***\"")
                .contains("\"encodingAesKey\":\"***\"")
                .contains("\"callbackEncodingAesKey\":\"***\"");
    }

    @Test
    void toVoExposesExplicitCapabilityMode() {
        NoticeChannelConfigEntity entity = new NoticeChannelConfigEntity();
        entity.setCapabilityMode(NoticeChannelCapabilityMode.BOTH);

        assertThat(NoticeChannelConfigConvert.toVO(entity).getCapabilityMode())
                .isEqualTo(NoticeChannelCapabilityMode.BOTH);
    }
}
