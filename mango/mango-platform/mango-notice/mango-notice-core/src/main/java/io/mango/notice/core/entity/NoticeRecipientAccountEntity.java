package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeRecipientAccountStatus;
import io.mango.notice.api.enums.NoticeRecipientAccountType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_recipient_account", excludeProperty = "orgId")
public class NoticeRecipientAccountEntity extends NoticeBaseEntity {

    private Long userId;

    private NoticeRecipientAccountType accountType;

    private String accountValue;

    private String displayName;

    private NoticeRecipientAccountStatus verifiedStatus;

    private Boolean defaultAccount;

    private Boolean enabled;

}
