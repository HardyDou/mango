package io.mango.notice.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.mango.notice.api.enums.NoticeChannelCapabilityMode;
import io.mango.notice.api.enums.NoticeChannelType;
import org.junit.jupiter.api.Test;

import java.util.Map;

class NoticeChannelCapabilityPolicyTest {
    @Test
    void receiveOnlyWecomDoesNotRequireSendSecrets() {
        Map<String, Object> config = Map.of(
                "callbackToken", "token-value",
                "encodingAesKey", "aes-key-value");

        assertThat(NoticeChannelCapabilityPolicy.missingSecretKeys(
                        NoticeChannelType.WECOM,
                        "WECOM",
                        NoticeChannelCapabilityMode.RECEIVE,
                        config))
                .isEmpty();
        assertThat(NoticeChannelCapabilityPolicy.isConfigComplete(
                        NoticeChannelType.WECOM,
                        "WECOM",
                        NoticeChannelCapabilityMode.RECEIVE,
                        config))
                .isTrue();
    }

    @Test
    void sendOnlyWecomDoesNotRequireCallbackEncryptionSecrets() {
        Map<String, Object> config = Map.of(
                "corpId", "corp",
                "agentId", "agent",
                "secret", "send-secret");

        assertThat(NoticeChannelCapabilityPolicy.missingSecretKeys(
                        NoticeChannelType.WECOM,
                        "WECOM",
                        NoticeChannelCapabilityMode.SEND,
                        config))
                .isEmpty();
        assertThat(NoticeChannelCapabilityPolicy.isConfigComplete(
                        NoticeChannelType.WECOM,
                        "WECOM",
                        NoticeChannelCapabilityMode.SEND,
                        config))
                .isTrue();
    }

    @Test
    void bothRequiresSendAndReceiveSides() {
        Map<String, Object> config = Map.of(
                "corpId", "corp",
                "agentId", "agent",
                "secret", "send-secret",
                "callbackToken", "callback-token");

        assertThat(NoticeChannelCapabilityPolicy.missingSecretKeys(
                        NoticeChannelType.WECOM,
                        "WECOM",
                        NoticeChannelCapabilityMode.BOTH,
                        config))
                .containsExactly("encodingAesKey");
        assertThat(NoticeChannelCapabilityPolicy.isConfigComplete(
                        NoticeChannelType.WECOM,
                        "WECOM",
                        NoticeChannelCapabilityMode.BOTH,
                        config))
                .isFalse();
    }

    @Test
    void receiveOnlyMailboxRequiresInboundAccountOnly() {
        Map<String, Object> config = Map.of(
                "inboundProtocol", "IMAP",
                "inboundHost", "imap.example.com",
                "inboundUsername", "receiver@example.com",
                "inboundPassword", "authorization-code");

        assertThat(NoticeChannelCapabilityPolicy.missingSecretKeys(
                        NoticeChannelType.EMAIL,
                        "CUSTOM_SMTP",
                        NoticeChannelCapabilityMode.RECEIVE,
                        config))
                .isEmpty();
        assertThat(NoticeChannelCapabilityPolicy.isConfigComplete(
                        NoticeChannelType.EMAIL,
                        "CUSTOM_SMTP",
                        NoticeChannelCapabilityMode.RECEIVE,
                        config))
                .isTrue();
    }

    @Test
    void unsupportedInboundChannelsRemainSendOnly() {
        assertThat(NoticeChannelCapabilityPolicy.supportsMode(
                        NoticeChannelType.SMS, NoticeChannelCapabilityMode.RECEIVE))
                .isFalse();
        assertThat(NoticeChannelCapabilityPolicy.supportsMode(
                        NoticeChannelType.EMAIL, NoticeChannelCapabilityMode.BOTH))
                .isTrue();
    }

    @Test
    void dingtalkOnlyAllowsTheConfiguredAppSecretFieldToBeRevealed() {
        assertThat(
                        NoticeChannelCapabilityPolicy.supportedSecretKeys(
                                NoticeChannelType.DINGTALK,
                                "DINGTALK",
                                NoticeChannelCapabilityMode.SEND))
                .containsExactly("appSecret");
    }
}
