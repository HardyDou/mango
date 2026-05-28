package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_business_config_version")
public class NoticeBusinessConfigVersionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long businessTypeId;

    private String bizType;

    private String paramsSchema;

    private NoticePriority defaultPriority;

    private String idempotentStrategy;

    private Integer version;

    private NoticeTemplateVersionStatus versionStatus;

    private LocalDateTime publishTime;

    private Long publishBy;

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
