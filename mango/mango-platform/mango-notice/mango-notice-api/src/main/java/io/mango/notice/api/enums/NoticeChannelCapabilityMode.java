package io.mango.notice.api.enums;

/** Explicitly declares whether a channel account is used for sending, receiving, or both. */
public enum NoticeChannelCapabilityMode {
    SEND,
    RECEIVE,
    BOTH;

    public boolean supportsSend() {
        return this == SEND || this == BOTH;
    }

    public boolean supportsReceive() {
        return this == RECEIVE || this == BOTH;
    }
}
