package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeSendMode;
import io.mango.notice.api.enums.NoticeTaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_task")
public class NoticeTaskEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskCode;

    private String bizType;

    private String bizId;

    private String idempotentKey;

    private String paramsSnapshot;

    private String recipientTargetsSnapshot;

    private String channelTypes;

    private NoticeSendMode sendMode;

    private LocalDateTime scheduledTime;

    private NoticeTaskStatus status;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private String tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
