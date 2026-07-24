package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知渠道或路由标签引用影响查询")
public class NoticeChannelReferenceImpactQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long configId;

    private NoticeChannelType channelType;

    private String routeTagCode;
}
