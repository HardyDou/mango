package io.mango.notice.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.mango.notice.core.entity.NoticeChannelConfigEntity;
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

    public String materialize(NoticeChannelConfigEntity entity) {
        Map<String, Object> config = new LinkedHashMap<>(readMap(entity.getConfigJson()));
        Map<String, Object> refs = readMap(entity.getSecretRefsJson());
        Map<String, Object> manual = readMap(entity.getSecretConfigJson());
        manual.forEach(
                (key, value) -> {
                    if (!refs.containsKey(key)) {
                        config.put(key, value);
                    }
                });
        refs.forEach((key, value) -> config.put(key, resolve(key, value)));
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException ex) {
            throw new NoticeChannelSecretResolutionException("渠道配置无法物化");
        }
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
