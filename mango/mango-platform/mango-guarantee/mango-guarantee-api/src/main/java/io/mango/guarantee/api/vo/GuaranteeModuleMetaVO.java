package io.mango.guarantee.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "保函协同模块元信息")
public class GuaranteeModuleMetaVO {

    @Schema(description = "模块名称")
    private String moduleName;

    @Schema(description = "模块路径")
    private String modulePath;

    @Schema(description = "模块阶段")
    private String stage;

    @Schema(description = "当前已装配能力")
    private List<String> capabilities;
}
