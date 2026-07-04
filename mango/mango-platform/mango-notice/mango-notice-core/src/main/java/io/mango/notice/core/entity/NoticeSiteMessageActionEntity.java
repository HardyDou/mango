package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageActionStatus;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_site_message_action")
public class NoticeSiteMessageActionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long messageId;

    private String actionCode;

    private String actionLabel;

    private NoticeSiteMessageActionInteractionType interactionType;

    private String eventType;

    private NoticeSiteMessageTargetType targetType;

    private String targetKey;

    private String targetParamsJson;

    private String targetOpenMode;

    private Boolean confirmRequired;

    private String inputSchema;

    private NoticeSiteMessageActionStatus status;

    private String failureReason;

    private Integer sortOrder;

    private LocalDateTime expireTime;

    private String tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
