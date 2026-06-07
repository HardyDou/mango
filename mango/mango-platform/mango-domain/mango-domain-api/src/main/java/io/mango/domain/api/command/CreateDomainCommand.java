package io.mango.domain.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增业务域命令。
 */
@Data
@Schema(description = "新增业务域命令")
public class CreateDomainCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "本层业务域编码，顶级域直接作为完整编码，子域会拼接父级编码")
    private String domainCode;

    @NotBlank
    @Schema(description = "业务域编码简写")
    private String domainShortCode;

    @NotBlank
    @Schema(description = "业务域名称")
    private String domainName;

    @Schema(description = "父业务域ID，0表示顶级")
    private Long parentId;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：0停用，1启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
