package io.mango.integration.support.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证规范解析结果。
 *
 * <p>认证方式与参数来自连接器 authSchema，字段值来自凭证声明，业务域只使用本对象，不自行解析 JSON。</p>
 */
public final class IntegrationAuthConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String authMode;
    private final Map<String, String> params;
    private final Map<String, String> credentials;

    private IntegrationAuthConfig(String authMode, Map<String, String> params, Map<String, String> credentials) {
        this.authMode = authMode;
        this.params = params;
        this.credentials = credentials;
    }

    /**
     * 由认证规范与凭证值构建配置。
     *
     * @param authSchemaJson 连接器认证规范
     * @param credentials 解密后的凭证字段
     * @return 认证配置
     */
    public static IntegrationAuthConfig of(String authSchemaJson, Map<String, String> credentials) {
        Map<String, String> params = new LinkedHashMap<>();
        String authMode = "NONE";
        if (authSchemaJson != null && !authSchemaJson.isBlank()) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(authSchemaJson);
                authMode = root.path("authMode").asText("NONE");
                JsonNode paramsNode = root.get("params");
                if (paramsNode != null && paramsNode.isObject()) {
                    paramsNode.fields().forEachRemaining(entry ->
                            params.put(entry.getKey(), entry.getValue().asText()));
                }
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("认证规范不是合法JSON");
            }
        }
        return new IntegrationAuthConfig(authMode, params, credentials == null ? Map.of() : credentials);
    }

    public String authMode() {
        return authMode;
    }

    /**
     * 读取认证参数，未配置时返回默认值。
     *
     * @param name 参数名
     * @param defaultValue 默认值
     * @return 参数值
     */
    public String param(String name, String defaultValue) {
        String value = params.get(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 读取凭证字段，优先按字段名，其次取第一个非空值。
     *
     * @param fieldName 字段名，可为空
     * @return 凭证值
     */
    public String credential(String fieldName) {
        if (fieldName != null && !fieldName.isBlank()) {
            String value = credentials.get(fieldName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return credentials.values().stream().filter(value -> value != null && !value.isBlank()).findFirst()
                .orElseThrow(() -> new IllegalStateException("凭证未配置"));
    }

    /**
     * 读取指定凭证字段，不存在时返回空。
     *
     * @param fieldName 字段名
     * @return 凭证值
     */
    public String optionalCredential(String fieldName) {
        return credentials.get(fieldName);
    }
}
