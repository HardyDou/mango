package io.mango.notice.api;

/** Core entry point used by schedulers and public callback adapters. */
public interface NoticeInboundReceiver {

    InboundReceiveResultResponse receive(InboundNoticeMessageRequest message);
}
