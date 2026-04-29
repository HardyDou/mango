package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API 资源注册结果。
 *
 * @param scanned 扫描到的资源数量
 * @param created 新增资源数量
 * @param updated 更新资源数量
 *
 * @author hardy
 */
@Schema(description = "API 资源注册结果")
public record ApiResourceRegisterResultVO(
        @Schema(description = "扫描到的资源数量") int scanned,
        @Schema(description = "新增资源数量") int created,
        @Schema(description = "更新资源数量") int updated) {

    public static ApiResourceRegisterResultVO empty() {
        return new ApiResourceRegisterResultVO(0, 0, 0);
    }
}
