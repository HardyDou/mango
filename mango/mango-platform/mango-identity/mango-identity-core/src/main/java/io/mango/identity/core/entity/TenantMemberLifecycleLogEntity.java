package io.mango.identity.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** Append-only tenant member lifecycle event. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_member_lifecycle_log")
public class TenantMemberLifecycleLogEntity extends TenantEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long memberId;
    private String eventType;
    private Long operatorUserId;
    private LocalDateTime occurredAt;
}
