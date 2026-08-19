package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(
        value = "notice_audit_log",
        excludeProperty = {"orgId", "createdBy", "updatedBy", "updatedAt"})
public class NoticeAuditLogEntity extends NoticeBaseEntity {
    private String actionType;

    private String targetType;

    private Long targetId;

    private Long operatorId;

    private String auditSnapshot;

}
