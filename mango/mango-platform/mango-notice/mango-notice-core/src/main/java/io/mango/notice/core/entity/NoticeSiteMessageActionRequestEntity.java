package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeSiteMessageActionRequestStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_site_message_action_request")
public class NoticeSiteMessageActionRequestEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long messageId;

    private Long actionId;

    private String actionCode;

    private Long actorUserId;

    private String requestId;

    private String inputJson;

    private NoticeSiteMessageActionRequestStatus status;

    private String failCode;

    private String failReason;

    private String resultJson;

    private String eventId;

    private String tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime finishedAt;
}
