package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.notice.api.enums.NoticeChannelType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_channel_route_tag", excludeProperty = "orgId")
public class NoticeChannelRouteTagEntity extends NoticeBaseEntity {

    private NoticeChannelType channelType;

    private String tagCode;

    private String tagName;

    private String description;
}
