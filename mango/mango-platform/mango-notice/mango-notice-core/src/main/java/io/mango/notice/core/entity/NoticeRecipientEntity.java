package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_recipient")
public class NoticeRecipientEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskId;

    private Long userId;

    private String recipientName;

    private String mobile;

    private String email;

    private String wechatOpenid;

    private String wecomUserId;

    private String dingtalkUserId;

    private String externalId;

    private String tenantId;

    private LocalDateTime createdAt;
}
