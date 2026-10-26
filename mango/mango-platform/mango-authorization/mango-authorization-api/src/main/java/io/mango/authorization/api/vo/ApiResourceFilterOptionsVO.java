package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * API 资源筛选维度。
 */
@Data
@Schema(description = "API 资源筛选维度")
public class ApiResourceFilterOptionsVO {

    @Schema(description = "已登记资源的来源模块名")
    private List<String> modules;

    @Schema(description = "已登记资源的接口领域")
    private List<String> domains;
}
