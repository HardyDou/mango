package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeReceivePreferenceScopeType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_receive_preference")
public class NoticeReceivePreferenceEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private NoticeReceivePreferenceScopeType scopeType;

    private String scopeValue;

    private NoticeChannelType channelType;

    private Boolean enabled;

    private Long accountId;

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
