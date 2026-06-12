package io.mango.infra.event.api.vo;

import io.mango.infra.kv.api.OutboxStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统事件返回对象。
 */
@Data
@Schema(description = "系统事件返回对象")
public class SystemEventVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息 ID")
    private String messageId;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "聚合 ID")
    private String aggregateId;

    @Schema(description = "发生时间")
    private Instant occurredAt;

    @Schema(description = "状态")
    private OutboxStatus status;

    @Schema(description = "投递尝试次数")
    private int attemptCount;

    @Schema(description = "下次投递时间")
    private Instant nextAttemptAt;

    @Schema(description = "锁定时间")
    private Instant lockedAt;

    @Schema(description = "锁定 Worker")
    private String lockedBy;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "事件载荷")
    private Map<String, Object> payload = new HashMap<>();

    @Schema(description = "事件头")
    private Map<String, String> headers = new HashMap<>();
}
