package io.mango.notice.support.channel;

/** Resolves an external Secret reference without exposing the resolved value to management APIs. */
public interface NoticeChannelSecretResolver {
    boolean supports(String reference);

    String resolve(String reference);
}
