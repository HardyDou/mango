package io.mango.notice.channel.wecom;

public record WecomTextMessageRequest(String toUser, int agentId, String content) {
}
