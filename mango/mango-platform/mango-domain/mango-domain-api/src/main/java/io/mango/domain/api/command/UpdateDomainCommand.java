package io.mango.domain.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改业务域命令。
 */
@Data
@Schema(description = "修改业务域命令")
public class UpdateDomainCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "业务域ID")
    private Long id;

    @NotBlank
    @Schema(description = "业务域编码简写")
    private String domainShortCode;

    @NotBlank
    @Schema(description = "业务域名称")
    private String domainName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：0停用，1启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
