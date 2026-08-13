package io.mango.notice.channel.wecom;

public record WecomInboundRequest(
        String signature,
        String timestamp,
        String nonce,
        String echoString,
        String body) {
}
