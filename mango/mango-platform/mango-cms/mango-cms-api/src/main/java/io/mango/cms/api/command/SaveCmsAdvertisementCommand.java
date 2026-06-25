package io.mango.cms.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsAdvertisementCommand {

    private Long id;

    @NotNull(message = "站点 ID 不能为空")
    private Long siteId;

    @NotBlank(message = "广告位编码不能为空")
    @Size(max = 64, message = "广告位编码最多64个字符")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "广告位编码只能包含字母、数字、点、下划线、冒号和短横线")
    private String adCode;

    @NotBlank(message = "广告位名称不能为空")
    @Size(max = 128, message = "广告位名称最多128个字符")
    private String adName;

    @NotBlank(message = "广告位位置不能为空")
    @Size(max = 64, message = "广告位置最多64个字符")
    private String position;

    @NotBlank(message = "位置类型不能为空")
    @Pattern(regexp = "BANNER|RECOMMEND|SIDEBAR|FOOTER|POPUP|CUSTOM", message = "位置类型不合法")
    private String positionType;

    @Size(max = 255, message = "支持物料类型最多255个字符")
    private String supportedMaterialTypes;

    private Integer width;

    private Integer height;

    @Size(max = 512, message = "备注最多512个字符")
    private String remark;

    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    private String status;
}
