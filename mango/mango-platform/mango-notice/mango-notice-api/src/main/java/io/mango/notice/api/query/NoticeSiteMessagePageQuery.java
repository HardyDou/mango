package io.mango.notice.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "站内信分页查询")
public class NoticeSiteMessagePageQuery {

    @Schema(description = "当前页，从 1 开始")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "是否只查询未读站内信")
    private Boolean unreadOnly;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务对象ID")
    private String bizId;
}
