package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeRecipientAccountStatus;
import io.mango.notice.api.enums.NoticeRecipientAccountType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_recipient_account")
public class NoticeRecipientAccountEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private NoticeRecipientAccountType accountType;

    private String accountValue;

    private String displayName;

    private NoticeRecipientAccountStatus verifiedStatus;

    private Boolean defaultAccount;

    private Boolean enabled;

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
