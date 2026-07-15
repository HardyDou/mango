package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户应用开通关系 VO。
 */
@Data
@Schema(description = "租户应用开通关系")
public class TenantAppBindingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "开通关系ID")
    private Long bindingId;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;
    @Schema(description = "失效时间")
    private LocalDateTime expireTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
