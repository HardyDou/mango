package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import io.mango.infra.persistence.api.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice_audit_log")
public class NoticeAuditLogEntity extends BaseEntity {
    private String actionType;

    private String targetType;

    private Long targetId;

    private Long operatorId;

    private String auditSnapshot;

    private String tenantId;

    private LocalDateTime createdAt;
}
