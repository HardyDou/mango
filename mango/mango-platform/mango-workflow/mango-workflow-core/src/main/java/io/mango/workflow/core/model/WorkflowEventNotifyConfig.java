package io.mango.workflow.core.model;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 节点事件通知配置。
 */
@Data
public class WorkflowEventNotifyConfig {

    private Boolean enabled;
    private String type;
    private String url;
    private String eventName;
    private String method;
    private Integer timeoutMillis;
    private String payloadTemplate;

    public boolean enabled() {
        return Boolean.TRUE.equals(enabled)
                && (StringUtils.hasText(url) || StringUtils.hasText(eventName));
    }
}
