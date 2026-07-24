package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.mango.notice.api.enums.NoticeChannelRouteMode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_business_channel_template", excludeProperty = "orgId")
public class NoticeBusinessChannelTemplateEntity extends NoticeBaseEntity {

    private Long businessTypeId;

    private String bizType;

    private NoticeChannelType channelType;

    private String templateName;

    private String titleTemplate;

    private String contentTemplate;

    private String channelTemplateId;

    private String variableMapping;

    private Integer version;

    private NoticeTemplateVersionStatus versionStatus;

    private Boolean enabled;

    private Long channelConfigId;

    private NoticeChannelRouteMode routeMode;

    private String routeTagCode;

    private LocalDateTime publishTime;

    private Long publishBy;

}
