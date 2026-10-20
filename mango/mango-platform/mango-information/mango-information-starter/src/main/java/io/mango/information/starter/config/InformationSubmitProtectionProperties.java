package io.mango.information.starter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 公开提交防刷配置。 */
@ConfigurationProperties(prefix = "mango.information.public-submit.protection")
@Getter
@Setter
public class InformationSubmitProtectionProperties {

    /** 是否启用公开提交防刷。 */
    private boolean enabled = true;

    /** 固定窗口长度，单位秒。 */
    private long windowSeconds = 60;

    /** 达到该次数后要求一次性验证码。 */
    private int riskThreshold = 10;

    /** 固定窗口内最大请求数，超过后返回 429。 */
    private int maxRequests = 30;

    /** 限流维度：IP 或 IP_AND_FORM。 */
    private KeyDimension keyDimension = KeyDimension.IP_AND_FORM;

    /** KV 不可用时是否保持可用性优先。 */
    private boolean failOpen = true;

    public enum KeyDimension {
        IP,
        IP_AND_FORM
    }
}
