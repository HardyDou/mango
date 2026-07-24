package io.mango.notice.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notice_channel_config_route_tag", excludeProperty = "orgId")
public class NoticeChannelConfigRouteTagEntity extends NoticeBaseEntity {
    private Long channelConfigId;

    private Long routeTagId;
}
