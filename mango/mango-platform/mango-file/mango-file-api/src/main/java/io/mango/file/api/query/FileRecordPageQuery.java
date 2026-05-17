package io.mango.file.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件记录分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件记录分页查询")
public class FileRecordPageQuery extends PageQuery {

    @Schema(description = "关键词。支持文件名、业务类型、业务ID模糊搜索")
    private String keyword;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;

    @Schema(description = "文件用途")
    private String purpose;

    @Schema(description = "逻辑目录ID。根目录为0")
    private Long directoryId;

    @Schema(description = "访问级别：PRIVATE、PUBLIC_READ、INTERNAL")
    private String accessLevel;

    @Schema(description = "状态：0-上传中，1-完成，2-失败，9-归档")
    private Integer status;

    @Schema(description = "是否包含已归档文件")
    private Boolean includeArchived;
}
