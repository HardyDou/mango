package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知渠道配置分页查询")
public class NoticeChannelConfigPageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private long pageNum = 1;

    @Schema(description = "每页数量")
    private long pageSize = 10;

    @Schema(description = "渠道类型")
    private NoticeChannelType channelType;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
