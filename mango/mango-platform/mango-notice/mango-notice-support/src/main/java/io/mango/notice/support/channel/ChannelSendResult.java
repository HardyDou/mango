package io.mango.notice.support.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelSendResult {

    private boolean success;

    private Long siteMessageId;

    private String providerMessageId;

    private String failCode;

    private String failReason;

    private boolean retryable;

    private String responseSnapshot;

    public static ChannelSendResult success(Long siteMessageId) {
        return new ChannelSendResult(true, siteMessageId, null, null, null, false, null);
    }

    public static ChannelSendResult providerSuccess(String providerMessageId, String responseSnapshot) {
        return new ChannelSendResult(true, null, providerMessageId, null, null, false, responseSnapshot);
    }

    public static ChannelSendResult failed(String failReason) {
        return new ChannelSendResult(false, null, null, null, failReason, true, null);
    }

    public static ChannelSendResult failed(String failCode, String failReason, boolean retryable) {
        return new ChannelSendResult(false, null, null, failCode, failReason, retryable, null);
    }
}
