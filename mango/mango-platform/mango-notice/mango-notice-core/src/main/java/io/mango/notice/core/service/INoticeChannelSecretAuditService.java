package io.mango.notice.core.service;

public interface INoticeChannelSecretAuditService {

    void record(AuditEntry entry);

    record AuditEntry(Long channelConfigId, String secretKey, String source, String result) {}
}
