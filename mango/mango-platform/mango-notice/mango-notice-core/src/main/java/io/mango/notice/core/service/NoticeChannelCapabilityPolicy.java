package io.mango.notice.core.service;

import io.mango.notice.api.enums.NoticeChannelCapabilityMode;
import io.mango.notice.api.enums.NoticeChannelType;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

/** Shared completeness policy for the explicitly selected channel capability mode. */
public final class NoticeChannelCapabilityPolicy {
    private NoticeChannelCapabilityPolicy() {}

    public static NoticeChannelCapabilityMode normalize(NoticeChannelCapabilityMode mode) {
        return mode == null ? NoticeChannelCapabilityMode.SEND : mode;
    }

    public static boolean supportsMode(NoticeChannelType channelType, NoticeChannelCapabilityMode mode) {
        NoticeChannelCapabilityMode normalized = normalize(mode);
        return normalized == NoticeChannelCapabilityMode.SEND
                || channelType == NoticeChannelType.EMAIL
                || channelType == NoticeChannelType.WECOM;
    }

    public static List<String> missingSecretKeys(
            NoticeChannelType channelType,
            String providerCode,
            NoticeChannelCapabilityMode mode,
            Map<String, Object> config) {
        List<String> missing = new ArrayList<>();
        NoticeChannelCapabilityMode normalized = normalize(mode);
        if (normalized.supportsSend()) {
            addMissingSendSecrets(channelType, providerCode, config, missing);
        }
        if (normalized.supportsReceive()) {
            if (channelType == NoticeChannelType.EMAIL && !hasAny(config, "inboundPassword")) {
                missing.add("inboundPassword");
            }
            if (channelType == NoticeChannelType.WECOM) {
                if (!hasAny(config, "callbackToken")) {
                    missing.add("callbackToken");
                }
                if (!hasAny(config, "encodingAesKey", "callbackEncodingAesKey")) {
                    missing.add("encodingAesKey");
                }
            }
        }
        return missing.stream().distinct().toList();
    }

    public static boolean isConfigComplete(
            NoticeChannelType channelType,
            String providerCode,
            NoticeChannelCapabilityMode mode,
            Map<String, Object> config) {
        NoticeChannelCapabilityMode normalized = normalize(mode);
        return (!normalized.supportsSend() || isSendConfigComplete(channelType, providerCode, config))
                && (!normalized.supportsReceive() || isReceiveConfigComplete(channelType, config));
    }

    public static Set<String> supportedSecretKeys(
            NoticeChannelType channelType, String providerCode, NoticeChannelCapabilityMode mode) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        secretKeyGroups(channelType, providerCode, mode)
                .forEach(group -> keys.add(group.getFirst()));
        return Set.copyOf(keys);
    }

    public static List<String> secretKeyAliases(
            NoticeChannelType channelType,
            String providerCode,
            NoticeChannelCapabilityMode mode,
            String canonicalKey) {
        return secretKeyGroups(channelType, providerCode, mode).stream()
                .filter(group -> group.getFirst().equalsIgnoreCase(canonicalKey))
                .findFirst()
                .map(List::copyOf)
                .orElseGet(List::of);
    }

    private static List<List<String>> secretKeyGroups(
            NoticeChannelType channelType, String providerCode, NoticeChannelCapabilityMode mode) {
        List<List<String>> groups = new ArrayList<>();
        NoticeChannelCapabilityMode normalized = normalize(mode);
        if (normalized.supportsSend()) {
            switch (channelType) {
                case SITE -> { }
                case EMAIL -> {
                    if ("ALIYUN_DM".equalsIgnoreCase(providerCode)) {
                        groups.add(List.of("accessKeySecret", "accessSecret"));
                    } else {
                        groups.add(List.of("password", "smtpPassword"));
                    }
                }
                case SMS -> {
                    if ("TENCENT_SMS".equalsIgnoreCase(providerCode)) {
                        groups.add(List.of("secretKey"));
                    } else {
                        groups.add(List.of("accessKeySecret", "accessSecret", "secretKey"));
                    }
                }
                case WECHAT_OFFICIAL -> groups.add(List.of("appSecret", "secret"));
                case WECOM -> groups.add(List.of("secret", "corpSecret"));
                case DINGTALK -> groups.add(List.of("appSecret", "secret"));
                default -> throw new IllegalArgumentException("不支持的通知渠道: " + channelType);
            }
        }
        if (normalized.supportsReceive()) {
            if (channelType == NoticeChannelType.EMAIL) {
                groups.add(List.of("inboundPassword"));
            } else if (channelType == NoticeChannelType.WECOM) {
                groups.add(List.of("callbackToken"));
                groups.add(List.of("encodingAesKey", "callbackEncodingAesKey"));
            }
        }
        return List.copyOf(groups);
    }

    private static void addMissingSendSecrets(
            NoticeChannelType channelType,
            String providerCode,
            Map<String, Object> config,
            List<String> missing) {
        switch (channelType) {
            case SITE -> { }
            case EMAIL -> {
                if ("ALIYUN_DM".equalsIgnoreCase(providerCode)) {
                    addIfMissing(config, missing, "accessKeySecret", "accessKeySecret", "accessSecret");
                } else {
                    addIfMissing(config, missing, "password", "password", "smtpPassword");
                }
            }
            case SMS -> {
                if ("TENCENT_SMS".equalsIgnoreCase(providerCode)) {
                    addIfMissing(config, missing, "secretKey", "secretKey");
                } else {
                    addIfMissing(config, missing, "accessKeySecret", "accessKeySecret", "accessSecret");
                }
            }
            case WECHAT_OFFICIAL, DINGTALK ->
                    addIfMissing(config, missing, "appSecret", "appSecret", "secret", "webhookUrl");
            case WECOM ->
                    addIfMissing(config, missing, "secret", "secret", "corpSecret", "webhookUrl");
            default -> throw new IllegalArgumentException("不支持的通知渠道: " + channelType);
        }
    }

    private static boolean isSendConfigComplete(
            NoticeChannelType channelType, String providerCode, Map<String, Object> config) {
        return switch (channelType) {
            case SITE -> true;
            case EMAIL -> isSendEmailComplete(providerCode, config);
            case SMS -> isSendSmsComplete(providerCode, config);
            case WECHAT_OFFICIAL -> hasAny(config, "appId") && hasAny(config, "appSecret", "secret");
            case WECOM -> hasAny(config, "corpId")
                    && hasAny(config, "agentId", "webhookUrl")
                    && hasAny(config, "secret", "corpSecret", "webhookUrl");
            case DINGTALK -> hasAny(config, "appKey", "webhookUrl")
                    && hasAny(config, "appSecret", "webhookUrl");
            default -> throw new IllegalArgumentException("不支持的通知渠道: " + channelType);
        };
    }

    private static boolean isSendSmsComplete(String providerCode, Map<String, Object> config) {
        if ("TENCENT_SMS".equalsIgnoreCase(providerCode)) {
            return hasAny(config, "secretId")
                    && hasAny(config, "secretKey")
                    && hasAny(config, "smsSdkAppId", "appId")
                    && hasAny(config, "signName", "sign");
        }
        return hasAny(config, "accessKeyId", "accessKey", "secretId")
                && hasAny(config, "accessKeySecret", "accessSecret", "secretKey")
                && hasAny(config, "signName", "sign");
    }

    private static boolean isSendEmailComplete(String providerCode, Map<String, Object> config) {
        if ("ALIYUN_DM".equalsIgnoreCase(providerCode)) {
            return hasAny(config, "accessKeyId", "accessKey")
                    && hasAny(config, "accessKeySecret", "accessSecret")
                    && hasAny(config, "regionId", "region")
                    && hasAny(config, "endpoint")
                    && hasAny(config, "accountName", "from", "fromAddress");
        }
        return hasAny(config, "host", "smtpHost")
                && hasAny(config, "username", "account")
                && hasAny(config, "password", "smtpPassword")
                && hasAny(config, "from", "fromAddress");
    }

    private static boolean isReceiveConfigComplete(
            NoticeChannelType channelType, Map<String, Object> config) {
        if (channelType == NoticeChannelType.EMAIL) {
            String protocol = text(config, "inboundProtocol").toUpperCase(Locale.ROOT);
            return ("IMAP".equals(protocol) || "POP3".equals(protocol))
                    && hasAny(config, "inboundHost")
                    && hasAny(config, "inboundUsername")
                    && hasAny(config, "inboundPassword");
        }
        return channelType == NoticeChannelType.WECOM
                && hasAny(config, "callbackToken")
                && hasAny(config, "encodingAesKey", "callbackEncodingAesKey");
    }

    private static void addIfMissing(
            Map<String, Object> config, List<String> missing, String label, String... keys) {
        if (!hasAny(config, keys)) {
            missing.add(label);
        }
    }

    private static String text(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static boolean hasAny(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            Object value = config.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return true;
            }
        }
        return false;
    }
}
