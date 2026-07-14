package io.mango.notice.channel.wecom;

public final class WecomTextMessage {

    private final String toUser;
    private final int agentId;
    private final String content;

    public WecomTextMessage(String toUser, int agentId, String content) {
        this.toUser = toUser;
        this.agentId = agentId;
        this.content = content;
    }

    public String toUser() {
        return toUser;
    }

    public int agentId() {
        return agentId;
    }

    public String content() {
        return content;
    }
}
