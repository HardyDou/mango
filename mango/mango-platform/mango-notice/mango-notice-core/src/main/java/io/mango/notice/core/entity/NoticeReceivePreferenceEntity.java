package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeReceivePreferenceScopeType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_receive_preference", excludeProperty = "orgId")
public class NoticeReceivePreferenceEntity extends NoticeBaseEntity {

    private Long userId;

    private NoticeReceivePreferenceScopeType scopeType;

    private String scopeValue;

    private NoticeChannelType channelType;

    private Boolean enabled;

    private Long accountId;

}
