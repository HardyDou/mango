package io.mango.notice.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.api.LockLease;
import io.mango.notice.api.InboundReceiveResult;
import io.mango.notice.api.NoticeInboundReceiver;
import io.mango.notice.api.enums.NoticeInboundProtocol;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeInboundReceiveCursorEntity;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.service.NoticeChannelSecretMaterializer;
import io.mango.notice.core.service.NoticeInboundMailCursorService;
import io.mango.notice.support.channel.NoticeInboundMailAccount;
import io.mango.notice.support.channel.NoticeInboundMailClient;
import io.mango.notice.support.channel.NoticeInboundMailItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Polls enabled mailbox accounts under a tenant-scoped owner-safe lease. */
@Component
@ConditionalOnProperty(prefix = "mango.notice.inbound", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class NoticeInboundMailPoller {

    private static final long DEFAULT_POLL_INTERVAL_SECONDS = 60L;
    private static final int IMAP_SSL_PORT = 993;
    private static final int IMAP_PLAIN_PORT = 143;
    private static final int POP3_SSL_PORT = 995;
    private static final int POP3_PLAIN_PORT = 110;
    private static final int MAX_PORT = 65535;

    private final NoticeChannelConfigMapper channelConfigMapper;
    private final NoticeChannelSecretMaterializer secretMaterializer;
    private final NoticeInboundMailCursorService cursorService;
    private final NoticeInboundReceiver receiver;
    private final List<NoticeInboundMailClient> clients;
    private final ILeaseLocker leaseLocker;
    private final NoticeProperties properties;
    private final ObjectMapper objectMapper;

    @Scheduled(
            initialDelayString = "${mango.notice.inbound.poll-initial-delay-millis:5000}",
            fixedDelayString = "${mango.notice.inbound.poll-fixed-delay-millis:60000}")
    public void poll() {
        List<NoticeChannelConfigEntity> configs = channelConfigMapper.selectEnabledEmailConfigs();
        for (NoticeChannelConfigEntity config : configs) {
            try {
                pollIfEnabled(config);
            } catch (RuntimeException failure) {
                log.warn("Inbound mailbox poll failed: channelConfigId={}, tenantId={}, provider={}",
                        config.getId(), config.getTenantId(), config.getProviderCode(), failure);
            }
        }
    }

    private void pollIfEnabled(NoticeChannelConfigEntity config) {
        Map<String, String> values = jsonMap(secretMaterializer.materialize(config));
        NoticeInboundMailAccount account = account(config, values);
        String lockKey = "notice:inbound:mail:" + config.getTenantId() + ":" + config.getId();
        NoticeProperties.Inbound inbound = properties.getInbound();
        LockLease lease = leaseLocker.tryAcquire(lockKey, inbound.getWorkerId(), inbound.getLockTtlSeconds())
                .orElse(null);
        if (lease == null) {
            return;
        }
        try {
            withTenant(config.getTenantId(), () -> pollAccount(account, values));
        } finally {
            leaseLocker.release(lease);
        }
    }

    private void pollAccount(NoticeInboundMailAccount account, Map<String, String> values) {
        long intervalSeconds = positiveLong(values.get("inboundPollIntervalSeconds"), DEFAULT_POLL_INTERVAL_SECONDS);
        LocalDateTime nextPollAt = LocalDateTime.now().plusSeconds(intervalSeconds);
        NoticeInboundReceiveCursorEntity cursor = cursorService.find(account.channelConfigId());
        if (cursor != null && cursor.getNextPollAt() != null && cursor.getNextPollAt().isAfter(LocalDateTime.now())) {
            return;
        }
        NoticeInboundMailClient client = clients.stream()
                .filter(candidate -> candidate.supports(account.protocol()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("邮箱协议未注册: " + account.protocol()));
        String cursorValue = cursor == null || cursor.getProtocol() != account.protocol()
                ? null : cursor.getCursorValue();
        String cursorVersion = cursor == null || cursor.getProtocol() != account.protocol()
                ? null : cursor.getCursorVersion();
        try {
            List<NoticeInboundMailItem> messages = client.fetch(account, cursorValue, cursorVersion);
            int processed = 0;
            for (NoticeInboundMailItem item : messages) {
                if (processed >= properties.getInbound().getBatchSize()) {
                    break;
                }
                InboundReceiveResult result = receiver.receive(item.message());
                if (!result.accepted()) {
                    throw new IllegalStateException("邮箱消息未完成可靠接收");
                }
                cursorService.advance(account.channelConfigId(), account.protocol(),
                        item.cursorValue(), item.cursorVersion(), nextPollAt);
                processed++;
            }
            if (processed == 0) {
                cursorService.recordPoll(account.channelConfigId(), account.protocol(), nextPollAt);
            }
        } catch (RuntimeException failure) {
            cursorService.recordFailure(account.channelConfigId(), account.protocol(),
                    failure.getClass().getSimpleName(), failure.getMessage(), nextPollAt);
            throw failure;
        }
    }

    private NoticeInboundMailAccount account(NoticeChannelConfigEntity config, Map<String, String> values) {
        NoticeInboundProtocol protocol = protocol(values.get("inboundProtocol"));
        boolean ssl = Boolean.parseBoolean(values.getOrDefault("inboundSsl", "true"));
        String host = required(values, "inboundHost");
        String username = required(values, "inboundUsername");
        String password = required(values, "inboundPassword");
        String clientName = optional(values, "inboundClientName");
        int port = positiveInt(values.get("inboundPort"), protocol == NoticeInboundProtocol.IMAP
                ? (ssl ? IMAP_SSL_PORT : IMAP_PLAIN_PORT) : (ssl ? POP3_SSL_PORT : POP3_PLAIN_PORT));
        return new NoticeInboundMailAccount(config.getTenantId(), config.getId(), config.getConfigCode(),
                host, port, ssl, username, password, protocol, clientName);
    }

    private NoticeInboundProtocol protocol(String value) {
        Require.notBlank(value, "邮箱 inboundProtocol 未配置");
        try {
            return NoticeInboundProtocol.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("邮箱 inboundProtocol 仅支持 IMAP 或 POP3", failure);
        }
    }

    private Map<String, String> jsonMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("邮箱渠道配置 JSON 格式错误", failure);
        }
    }

    private String required(Map<String, String> values, String key) {
        String value = values.get(key);
        Require.notBlank(value, "邮箱 " + key + " 未配置");
        return value.trim();
    }

    private int positiveInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        int parsed = Integer.parseInt(value);
        Require.isTrue(parsed > 0 && parsed <= MAX_PORT, "邮箱端口配置非法");
        return parsed;
    }

    private String optional(Map<String, String> values, String key) {
        String value = values.get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private long positiveLong(String value, long defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        long parsed = Long.parseLong(value);
        Require.isTrue(parsed > 0L, "邮箱轮询周期必须大于 0");
        return parsed;
    }

    private void withTenant(String tenantId, Runnable action) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(new MangoContextSnapshot(
                    previous.requestId(), previous.traceId(), tenantId, previous.userId(), previous.memberId(),
                    previous.principalName(), previous.realm(), previous.actorType(), previous.partyType(),
                    previous.partyId(), previous.appCode(), previous.clientIp()));
            action.run();
        } finally {
            MangoContextHolder.set(previous);
        }
    }
}
