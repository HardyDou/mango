package io.mango.authorization.api.query;

import io.mango.authorization.api.enums.ApiResourceSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * API 资源分页查询。
 */
@Data
@Schema(description = "API 资源分页查询")
public class ApiResourcePageQuery {

    @Min(value = 1, message = "当前页必须大于等于1")
    @Schema(description = "当前页，从1开始")
    private long page = 1L;

    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 500, message = "每页大小不能超过500")
    @Schema(description = "每页大小，最大500")
    private long size = 10L;

    @Size(max = 128, message = "模块名不能超过128个字符")
    @Schema(description = "来源模块名，例如 mango-auth")
    private String moduleName;

    @Size(max = 128, message = "领域不能超过128个字符")
    @Schema(description = "接口一级路径段所属领域，例如 auth")
    private String domain;

    @Schema(description = "登记来源：固定声明、Controller 扫描或人工登记")
    private ApiResourceSource source;

    @Size(max = 32, message = "访问模式不能超过32个字符")
    @Schema(description = "访问模式：PUBLIC/LOGIN/PERMISSION/INTERNAL")
    private String accessMode;

    @Schema(description = "资源状态：1-启用，0-禁用")
    private Integer status;

    @Size(max = 128, message = "关键词不能超过128个字符")
    @Schema(description = "关键词，支持路径、权限码、资源编码和处理类")
    private String keyword;
}
