package io.mango.infra.realtime.api;

import java.util.List;

/**
 * HTTP polling-side access for clients that cannot keep a push connection.
 */
public interface RealtimePollingService {

    void append(String subscriberId, RealtimeMessage envelope);

    List<RealtimeMessage> poll(String subscriberId, int maxSize);
}
