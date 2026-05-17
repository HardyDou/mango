package io.mango.infra.event.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 通用领域事件。
 */
@Value
@Builder(toBuilder = true)
@Schema(description = "通用领域事件")
public class DomainEvent {

    @Builder.Default
    @Schema(description = "事件ID")
    String eventId = UUID.randomUUID().toString();

    @Schema(description = "事件类型")
    String eventType;

    @Schema(description = "业务类型")
    String businessType;

    @Schema(description = "业务主键")
    String businessKey;

    @Schema(description = "聚合ID或申请ID")
    String aggregateId;

    @Builder.Default
    @Schema(description = "发生时间")
    Instant occurredAt = Instant.now();

    @Singular("payload")
    @Schema(description = "事件载荷")
    Map<String, Object> payload;

    @Singular("header")
    @Schema(description = "事件头")
    Map<String, String> headers;
}
