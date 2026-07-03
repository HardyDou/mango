package io.mango.infra.kv.api;

/**
 * Well-known outbox ownership topics.
 */
public final class OutboxTopics {

    public static final String DOMAIN_EVENT = "domain-event";
    public static final String NOTICE = "notice";
    public static final String REALTIME = "realtime";
    public static final String NOTICE_SEND_EVENT_TYPE = "notice.send";
    public static final String REALTIME_DISPATCH_EVENT_TYPE = "realtime.message.dispatch";

    private OutboxTopics() {
    }
}
