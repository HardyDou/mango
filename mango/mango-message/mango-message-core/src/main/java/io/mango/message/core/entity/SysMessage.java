package io.mango.message.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.mango.message.api.enums.MessageType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_message")
public class SysMessage {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private MessageType messageType;
    private String title;
    private String content;
    private Long userId;
    private Integer priority;
    private Integer readStatus;
    private LocalDateTime readTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
