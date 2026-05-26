package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeDeleteStatus;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_site_message")
public class NoticeSiteMessageEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskId;

    private Long sendRecordId;

    private Long userId;

    private String title;

    private String content;

    private NoticePriority priority;

    private NoticeReadStatus readStatus;

    private LocalDateTime readTime;

    private NoticeDeleteStatus deleteStatus;

    private Boolean revokeStatus;

    private Boolean topStatus;

    private String bizType;

    private String bizId;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private String tenantId;
}
