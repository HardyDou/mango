package io.mango.file.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
    @Size(max = 255, message = "关键词不能超过255个字符")
    private String keyword;

    @Schema(description = "业务类型")
    @Size(max = 64, message = "业务类型不能超过64个字符")
    private String bizType;

    @Schema(description = "业务ID")
    @Size(max = 128, message = "业务ID不能超过128个字符")
    private String bizId;

    @Schema(description = "文件用途")
    @Size(max = 64, message = "文件用途不能超过64个字符")
    private String purpose;

    @Schema(description = "逻辑目录ID。根目录为0")
    @PositiveOrZero(message = "逻辑目录ID不能小于0")
    private Long directoryId;

    @Schema(description = "访问级别：PRIVATE、PUBLIC_READ、INTERNAL")
    @Size(max = 32, message = "访问级别不能超过32个字符")
    private String accessLevel;

    @Schema(description = "状态：0-上传中，1-完成，2-失败，9-归档")
    @Min(value = 0, message = "状态值不能小于0")
    @Max(value = 9, message = "状态值不能大于9")
    private Integer status;

    @Schema(description = "是否包含已归档文件")
    @NotNull(message = "是否包含已归档文件不能为空")
    private Boolean includeArchived = false;
}
