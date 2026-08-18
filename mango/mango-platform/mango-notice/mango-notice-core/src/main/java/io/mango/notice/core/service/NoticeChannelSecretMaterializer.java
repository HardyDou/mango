package io.mango.notice.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.support.channel.NoticeChannelSecretResolver;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring singleton collaborators are injected and intentionally shared")
public class NoticeChannelSecretMaterializer {
    private final ObjectMapper objectMapper;
    private final List<NoticeChannelSecretResolver> resolvers;
    private final NoticeChannelSecretCodec secretCodec;
    private final NoticeChannelConfigMapper channelConfigMapper;

    public String materialize(NoticeChannelConfigEntity entity) {
        Map<String, Object> config = new LinkedHashMap<>(readMap(entity.getConfigJson()));
        Map<String, Object> refs = readMap(entity.getSecretRefsJson());
        Map<String, Object> manual = new LinkedHashMap<>(readMap(entity.getSecretConfigJson()));
        boolean migrated = encryptLegacyValues(manual);
        manual.forEach(
                (key, value) -> {
                    if (!refs.containsKey(key)) {
                        config.put(key, decryptStoredValue(value));
                    }
                });
        refs.forEach((key, value) -> config.put(key, resolve(key, value)));
        try {
            if (migrated && entity.getId() != null) {
                entity.setSecretConfigJson(objectMapper.writeValueAsString(manual));
                channelConfigMapper.updateById(entity);
            }
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException ex) {
            throw new NoticeChannelSecretResolutionException("渠道配置无法物化");
        }
    }

    private boolean encryptLegacyValues(Map<String, Object> values) {
        boolean migrated = false;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                migrated |= encryptLegacyValues((Map<String, Object>) nested);
            } else if (value != null) {
                String stored = String.valueOf(value);
                if (!secretCodec.isEncrypted(stored)) {
                    entry.setValue(secretCodec.encrypt(stored));
                    migrated = true;
                }
            }
        }
        return migrated;
    }

    private Object decryptStoredValue(Object value) {
        if (value instanceof Map<?, ?> nested) {
            Map<String, Object> decrypted = new LinkedHashMap<>();
            nested.forEach((key, nestedValue) -> decrypted.put(String.valueOf(key), decryptStoredValue(nestedValue)));
            return decrypted;
        }
        return value == null ? null : secretCodec.decryptCompatible(String.valueOf(value));
    }

    private String resolve(String key, Object value) {
        String reference = value == null ? null : String.valueOf(value);
        NoticeChannelSecretResolver resolver =
                resolvers.stream()
                        .filter(candidate -> candidate.supports(reference))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new NoticeChannelSecretResolutionException(
                                                "Secret 引用不受支持，key=" + key));
        String resolved = resolver.resolve(reference);
        if (!StringUtils.hasText(resolved)) {
            throw new NoticeChannelSecretResolutionException("Secret 引用未解析，key=" + key);
        }
        return resolved;
    }

    private Map<String, Object> readMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new NoticeChannelSecretResolutionException("渠道 Secret 配置格式错误");
        }
    }
}
