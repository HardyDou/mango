package io.mango.notice.core.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;

import java.util.Map;
import java.util.Set;

public final class NoticeChannelConfigConvert {

 private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
 private static final Set<String> SECRET_KEYS = Set.of("secret", "password", "token", "key", "appSecret", "accessKey", "secretKey", "smtpPassword");

 private NoticeChannelConfigConvert() {
 }

 public static NoticeChannelConfigVO toVO(NoticeChannelConfigEntity entity) {
 NoticeChannelConfigVO vo = new NoticeChannelConfigVO();
 vo.setId(entity.getId());
 vo.setChannelType(entity.getChannelType());
 vo.setProviderCode(entity.getProviderCode());
 vo.setConfigName(entity.getConfigName());
 vo.setConfigJson(mask(entity.getConfigJson()));
 vo.setEnabled(entity.getEnabled());
 vo.setPriority(entity.getPriority());
 vo.setWeight(entity.getWeight());
 vo.setConfigStatus(entity.getConfigStatus());
 vo.setLastSendStatus(entity.getLastSendStatus());
 vo.setLastSendTime(entity.getLastSendTime());
 vo.setLastFailureCode(entity.getLastFailureCode());
 vo.setLastFailureReason(entity.getLastFailureReason());
 vo.setRateLimitConfig(entity.getRateLimitConfig());
 vo.setUpdatedAt(entity.getUpdatedAt());
 return vo;
 }

 private static String mask(String configJson) {
 if (configJson == null || configJson.isBlank()) {
 return configJson;
 }
 try {
 Map<String, Object> config = OBJECT_MAPPER.readValue(configJson, Map.class);
 maskMap(config);
 return OBJECT_MAPPER.writeValueAsString(config);
 } catch (JsonProcessingException ex) {
 return "***";
 }
 }

 private static void maskMap(Map<String, Object> config) {
 config.replaceAll((key, value) -> {
 if (isSecretKey(key)) {
 return "***";
 }
 if (value instanceof Map<?, ?> nested) {
 maskMap((Map<String, Object>) nested);
 }
 return value;
 });
 }

 private static boolean isSecretKey(String key) {
 return SECRET_KEYS.stream().anyMatch(secretKey -> secretKey.equalsIgnoreCase(key) || key.toLowerCase().contains(secretKey.toLowerCase()));
 }
}
