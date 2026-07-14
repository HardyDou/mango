package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_recipient", excludeProperty = {
        "orgId", "createdBy", "updatedBy", "updatedAt"
})
public class NoticeRecipientEntity extends NoticeBaseEntity {

    private Long taskId;

    private Long userId;

    private String recipientName;

    private String mobile;

    private String email;

    private String wechatOpenid;

    private String wecomUserId;

    private String dingtalkUserId;

    private String externalId;

}
