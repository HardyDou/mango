package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeChannelSecretStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知渠道配置分页查询")
public class NoticeChannelConfigPageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    @jakarta.validation.constraints.Positive
    private long pageNum = 1;

    @Schema(description = "每页数量")
    @jakarta.validation.constraints.Positive
    private long pageSize = 10;

    @Schema(description = "渠道类型")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private NoticeChannelType channelType;

    @Schema(description = "是否启用")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private Boolean enabled;

    @Schema(description = "渠道配置稳定编码")
    private String configCode;

    @Schema(description = "配置来源：MANUAL 或 RESOURCE")
    private String resourceSource;

    @Schema(description = "Secret 完整性状态")
    private NoticeChannelSecretStatus secretStatus;
}
