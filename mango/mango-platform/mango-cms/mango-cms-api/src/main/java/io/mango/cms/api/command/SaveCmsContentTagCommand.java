package io.mango.cms.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsContentTagCommand {

    private Long id;

    @NotBlank(message = "标签编码不能为空")
    @Size(max = 64, message = "标签编码最多64个字符")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "标签编码只能包含字母、数字、点、下划线、冒号和短横线")
    private String tagCode;

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 128, message = "标签名称最多128个字符")
    private String tagName;

    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    private String status;

    @Size(max = 512, message = "备注最多512个字符")
    private String remark;
}
