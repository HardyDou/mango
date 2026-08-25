package io.mango.ai.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 租户级 AI 服务调用审计和用量记录。 */
@Getter
@Setter
@TableName("ai_invocation_audit")
public class AiInvocationAuditEntity extends TenantEntity {
    private String requestId;
    private Long userId;
    private String traceId;
    private String serviceCode;
    private String serviceType;
    private String capability;
    private String providerCode;
    private String modelName;
    private String resultStatus;
    private Integer errorCode;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long inputBytes;
    private Long outputBytes;
    private Long latencyMs;
    private LocalDateTime completedAt;
}
