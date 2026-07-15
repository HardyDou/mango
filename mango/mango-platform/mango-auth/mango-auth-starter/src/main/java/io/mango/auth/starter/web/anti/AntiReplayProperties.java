package io.mango.auth.starter.web.anti;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 防重放配置属性。
 */
@Data
@ConfigurationProperties(prefix = "mango.auth.anti-replay")
public class AntiReplayProperties {

    /**
     * 应用签名密钥，key 为 appKey，value 为 secret。
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Spring Boot configuration binding requires this mutable getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring Boot configuration binding requires this mutable setter"))
    private Map<String, String> appSecrets = new HashMap<>();

    /**
     * 默认签名密钥，仅在 allowFallback=true 时作为兜底密钥。
     */
    private String defaultSecret;

    /**
     * 是否允许未知 appKey 使用默认签名密钥兜底。
     */
    private boolean allowFallback = false;
}
