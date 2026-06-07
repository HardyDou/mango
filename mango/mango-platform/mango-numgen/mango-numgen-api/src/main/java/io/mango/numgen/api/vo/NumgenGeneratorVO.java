package io.mango.numgen.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "编号生成器视图")
public class NumgenGeneratorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "生成器 ID")
    private Long id;

    @Schema(description = "业务 Key")
    private String genKey;

    @Schema(description = "名称")
    private String genName;

    @Schema(description = "业务域编码")
    private String domainCode;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "当前规则版本")
    private Integer currentRuleVersion;

    @Schema(description = "当前发布状态")
    private Integer currentPublishStatus;

    @Schema(description = "是否存在未发布修改")
    private Boolean hasUnpublishedChanges;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
