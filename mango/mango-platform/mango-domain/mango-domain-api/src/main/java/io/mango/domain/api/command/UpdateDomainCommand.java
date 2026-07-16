package io.mango.domain.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改业务域命令。
 */
@Data
@Schema(description = "修改业务域命令")
public class UpdateDomainCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "业务域ID不能为空")
    @Positive(message = "业务域ID必须大于0")
    @Schema(description = "业务域ID")
    private Long id;

    @NotBlank(message = "业务域简写不能为空")
    @Size(max = 64, message = "业务域简写长度不能超过64个字符")
    @Schema(description = "业务域编码简写")
    private String domainShortCode;

    @NotBlank(message = "业务域名称不能为空")
    @Size(max = 128, message = "业务域名称长度不能超过128个字符")
    @Schema(description = "业务域名称")
    private String domainName;

    @PositiveOrZero(message = "排序不能小于0")
    @Schema(description = "排序")
    private Integer sort;

    @Min(value = 0, message = "业务域状态非法")
    @Max(value = 1, message = "业务域状态非法")
    @Schema(description = "状态：0停用，1启用")
    private Integer status;

    @Size(max = 512, message = "备注长度不能超过512个字符")
    @Schema(description = "备注")
    private String remark;
}
